package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.dto.MonitoringCreateRequest;
import com.cmbchina.orgperformance.entity.*;
import com.cmbchina.orgperformance.excel.DataCollectionSheetParser;
import com.cmbchina.orgperformance.excel.ExcelGenerator;
import com.cmbchina.orgperformance.mapper.*;
import com.cmbchina.orgperformance.minio.MinioService;
import com.cmbchina.orgperformance.notify.NotificationService;
import com.cmbchina.orgperformance.vo.ConfirmationTaskVO;
import com.cmbchina.orgperformance.vo.MonitoringVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class MonitoringService {

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private AssessmentSystemMapper systemMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private IndicatorMapper indicatorMapper;

    @Autowired
    private CollectionTaskMapper taskMapper;

    @Autowired
    private MonthlyIndicatorDataMapper indicatorDataMapper;

    @Autowired
    private InstitutionLeaderMapper leaderMapper;

    @Autowired
    private ConfirmationRecordMapper confirmMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DataProcessingService dataProcessingService;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ExcelGenerator excelGenerator;

    @Autowired
    private DataCollectionSheetParser dataCollectionSheetParser;

    public List<MonitoringVO> getMonitoringList(Long systemId, String status, Integer year, Integer month,
                                                 Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<MonthlyMonitoring> monitorings = monitoringMapper.selectList(systemId, status, year, month, offset, pageSize);
        return convertToVO(monitorings);
    }

    public int count(Long systemId, String status) {
        return monitoringMapper.count(systemId, status);
    }

    public MonitoringVO getMonitoringById(Long id) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(id);
        return convertToVO(monitoring);
    }

    @Transactional
    public Long createMonitoring(MonitoringCreateRequest request, String createdBy) throws IOException {
        MonthlyMonitoring existing = monitoringMapper.selectBySystemIdAndYearMonth(
                request.getSystemId(), request.getYear(), request.getMonth());
        if (existing != null) {
            throw new RuntimeException("同一体系同一月份只能发起一次监测");
        }

        AssessmentSystem system = systemMapper.selectById(request.getSystemId());
        if (system == null) {
            throw new RuntimeException("体系不存在");
        }

        MonthlyMonitoring monitoring = new MonthlyMonitoring();
        monitoring.setSystemId(request.getSystemId());
        monitoring.setYear(request.getYear());
        monitoring.setMonth(request.getMonth());
        monitoring.setStatus(MonthlyMonitoring.STATUS_COLLECTING);
        monitoring.setDeadline(request.getDeadline());
        monitoring.setApprovalRequired(request.getApprovalRequired() != null ?
                request.getApprovalRequired() : system.getNeedApproval());
        monitoring.setProcessPercent(0);
        monitoring.setProcessStatus(MonthlyMonitoring.PROCESS_IDLE);
        monitoring.setCreatedBy(createdBy);

        monitoringMapper.insert(monitoring);

        createCollectionTasks(monitoring);

        return monitoring.getId();
    }

    private void createCollectionTasks(MonthlyMonitoring monitoring) throws IOException {
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        List<Indicator> allIndicators = indicatorMapper.selectBySystemId(monitoring.getSystemId());

        // 获取原始Excel模板
        AssessmentSystem system = systemMapper.selectById(monitoring.getSystemId());
        if (system == null || system.getTemplateFileKey() == null) {
            throw new RuntimeException("体系模板文件不存在");
        }

        byte[] templateData = minioService.downloadAsBytes(
                minioService.getBucketTemplates(), system.getTemplateFileKey());

        // 解析"数据收集页"获取收数指标和收数人分配
        DataCollectionSheetParser.ParsedDataCollectionSheet parsedSheet =
                dataCollectionSheetParser.parse(templateData);
        dataCollectionSheetParser.matchWithAssessmentIndicators(parsedSheet, allIndicators);

        // 按收数人分组指标
        Map<String, List<DataCollectionSheetParser.CollectionIndicator>> collectorIndicators = new HashMap<>();
        for (DataCollectionSheetParser.CollectionIndicator colInd : parsedSheet.getIndicators()) {
            String collectorName = colInd.getCollectorName();
            if (collectorName == null || collectorName.isEmpty()) continue;
            collectorIndicators.computeIfAbsent(collectorName, k -> new ArrayList<>()).add(colInd);
        }

        // 为每个收数人创建Excel文件
        Map<String, String> collectorFileKeys = new HashMap<>();
        for (Map.Entry<String, List<DataCollectionSheetParser.CollectionIndicator>> entry : collectorIndicators.entrySet()) {
            String collectorName = entry.getKey();
            List<DataCollectionSheetParser.CollectionIndicator> colInds = entry.getValue();

            // 根据收数人名称找到对应的用户
            SysUser collector = null;
            for (SysUser u : userMapper.selectByRoleCode("collector")) {
                if (u.getName().equals(collectorName) || u.getEmpNo().equals(colInds.get(0).getCollectorEmpNo())) {
                    collector = u;
                    break;
                }
            }
            if (collector == null) {
                // 尝试按工号匹配
                for (SysUser u : userMapper.selectByRoleCode("collector")) {
                    if (u.getEmpNo() != null && colInds.get(0).getCollectorEmpNo() != null &&
                            u.getEmpNo().equals(colInds.get(0).getCollectorEmpNo())) {
                        collector = u;
                        break;
                    }
                }
            }
            if (collector == null) continue;

            byte[] collectorExcel = excelGenerator.createCollectorExcel(
                    templateData, colInds, institutions,
                    collector.getName(), monitoring.getYear(), monitoring.getMonth());

            String fileKey = minioService.uploadCollectorDocument(
                    collectorExcel, monitoring.getId(), collector.getId(), null);
            collectorFileKeys.put(collectorName, fileKey);
        }

        // 创建任务，每个收数员的任务关联其专属的Excel文件
        List<CollectionTask> tasks = new ArrayList<>();

        for (Institution inst : institutions) {
            // 遍历数据收集页中的每个指标
            for (DataCollectionSheetParser.CollectionIndicator colInd : parsedSheet.getIndicators()) {
                String collectorName = colInd.getCollectorName();
                if (collectorName == null || collectorName.isEmpty()) continue;

                // 找到对应的收数员
                SysUser collector = null;
                for (SysUser u : userMapper.selectByRoleCode("collector")) {
                    if (u.getName().equals(collectorName) || u.getEmpNo().equals(colInd.getCollectorEmpNo())) {
                        collector = u;
                        break;
                    }
                }
                if (collector == null) {
                    for (SysUser u : userMapper.selectByRoleCode("collector")) {
                        if (u.getEmpNo() != null && colInd.getCollectorEmpNo() != null &&
                                u.getEmpNo().equals(colInd.getCollectorEmpNo())) {
                            collector = u;
                            break;
                        }
                    }
                }
                if (collector == null) continue;

                // 找到对应的考核指标
                Long indicatorId = null;
                for (Indicator ind : allIndicators) {
                    String level2Name = ind.getLevel2Name() != null ? ind.getLevel2Name() : "";
                    if (colInd.getIndicatorName().equals(level2Name) ||
                            colInd.getIndicatorName().equals(ind.getLevel1Name())) {
                        indicatorId = ind.getId();
                        break;
                    }
                }

                String fileKey = collectorFileKeys.get(collectorName);
                if (fileKey == null) continue;

                CollectionTask task = new CollectionTask();
                task.setMonitoringId(monitoring.getId());
                task.setIndicatorId(indicatorId);
                task.setInstitutionId(inst.getId());
                task.setCollectorName(collector.getName());
                task.setCollectorEmpNo(collector.getEmpNo());
                task.setCollectorUserId(collector.getId());
                task.setStatus(CollectionTask.STATUS_PENDING);
                task.setFileKey(fileKey);

                // 保存数据收集页的收数指标信息
                task.setCollectionIndicatorName(colInd.getIndicatorName());
                task.setCollectionUnit(colInd.getUnit());
                // 维度分类尝试从考核指标获取，如果没有则使用数据收集页的值
                if (indicatorId != null) {
                    Indicator assIndicator = null;
                    for (Indicator ind : allIndicators) {
                        if (ind.getId().equals(indicatorId)) {
                            assIndicator = ind;
                            break;
                        }
                    }
                    if (assIndicator != null) {
                        task.setCollectionDimension(assIndicator.getDimension());
                        task.setCollectionCategory(assIndicator.getCategory());
                    }
                }

                tasks.add(task);
            }
        }

        if (!tasks.isEmpty()) {
            taskMapper.batchInsert(tasks);
        }
    }

    @Transactional
    public int fixCollectorTasks(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }

        List<SysUser> collectors = userMapper.selectByRoleCode("collector");
        if (collectors == null || collectors.isEmpty()) {
            throw new RuntimeException("没有收数员");
        }

        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        int count = 0;
        int collectorIndex = 0;

        for (Institution inst : institutions) {
            SysUser collector = collectors.get(collectorIndex % collectors.size());
            collectorIndex++;
            int updated = taskMapper.updateCollectorByInstitutionId(
                    inst.getId(), monitoringId,
                    collector.getName(), collector.getEmpNo(), collector.getId());
            count += updated;
        }
        return count;
    }

    @Transactional
    public void closeMonitoring(Long id) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(id);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_COLLECTING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许截止");
        }

        monitoringMapper.updateStatus(id, MonthlyMonitoring.STATUS_CLOSED);
    }

    @Transactional
    public void startConfirming(Long id) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(id);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_PROCESSING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }

        monitoringMapper.updateStatus(id, MonthlyMonitoring.STATUS_CONFIRMING);

        notifyLeadersForConfirmation(id);
    }

    private void notifyLeadersForConfirmation(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());

        for (Institution inst : institutions) {
            Set<Long> notifiedUserIds = new HashSet<>();
            
            // 1. 从 institution_leader 表获取负责人
            List<InstitutionLeader> leaders = leaderMapper.selectByInstitutionId(inst.getId());
            for (InstitutionLeader leader : leaders) {
                if (leader.getUserId() != null) {
                    createConfirmationRecord(monitoringId, monitoring, inst, leader.getUserId());
                    notifiedUserIds.add(leader.getUserId());
                }
            }
            
            // 2. 如果 institution_leader 表没有数据，从 Institution 表的 leaderEmpNo 查找
            if (leaders.isEmpty() && inst.getLeaderEmpNo() != null && !inst.getLeaderEmpNo().isEmpty()) {
                List<SysUser> users = userMapper.selectByRoleCode("leader");
                for (SysUser u : users) {
                    if (u.getEmpNo() != null && u.getEmpNo().equals(inst.getLeaderEmpNo()) && !notifiedUserIds.contains(u.getId())) {
                        createConfirmationRecord(monitoringId, monitoring, inst, u.getId());
                        notifiedUserIds.add(u.getId());
                        break;
                    }
                }
            }
        }
    }
    
    private void createConfirmationRecord(Long monitoringId, MonthlyMonitoring monitoring, Institution inst, Long userId) {
        String content = String.format(
                "您有新的绩效数据待确认。体系：%s，月份：%d年%d月，机构：%s",
                systemMapper.selectById(monitoring.getSystemId()).getName(),
                monitoring.getYear(), monitoring.getMonth(), inst.getOrgName());

        ConfirmationRecord record = new ConfirmationRecord();
        record.setMonitoringId(monitoringId);
        record.setInstitutionId(inst.getId());
        record.setUserId(userId);
        record.setStatus(ConfirmationRecord.STATUS_PENDING);
        confirmMapper.insert(record);

        notificationService.sendNotification(userId,
                "绩效数据待确认", content, "site");
    }

    @Transactional
    public void confirmInstitution(Long monitoringId, Long institutionId, Long userId, String remark) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null || !MonthlyMonitoring.STATUS_CONFIRMING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }

        ConfirmationRecord record = confirmMapper.selectByMonitoringIdAndUserId(monitoringId, userId);
        if (record == null) {
            throw new RuntimeException("您没有该监测的确认权限");
        }

        record.setStatus(ConfirmationRecord.STATUS_CONFIRMED);
        record.setRemark(remark);
        confirmMapper.updateStatus(record.getId(), ConfirmationRecord.STATUS_CONFIRMED, remark);

        leaderMapper.updateConfirmed(institutionId, userId, true);
    }

    @Transactional
    public void publishMonitoring(Long id) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(id);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_CONFIRMING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许发布");
        }

        monitoringMapper.updateStatus(id, MonthlyMonitoring.STATUS_PUBLISHED);

        notifyPublication(id);
    }

    private void notifyPublication(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());

        for (Institution inst : institutions) {
            List<InstitutionLeader> leaders = leaderMapper.selectByInstitutionId(inst.getId());
            for (InstitutionLeader leader : leaders) {
                String content = String.format(
                        "绩效结果已发布。体系：%s，月份：%d年%d月，请查看报表。",
                        systemMapper.selectById(monitoring.getSystemId()).getName(),
                        monitoring.getYear(), monitoring.getMonth());

                notificationService.sendNotification(leader.getUserId(),
                        "绩效结果发布通知", content, "site");
            }
        }
    }

    public boolean isAllConfirmed(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());

        for (Institution inst : institutions) {
            int confirmed = leaderMapper.countConfirmedByInstitutionId(inst.getId());
            int total = leaderMapper.countByInstitutionId(inst.getId());
            if (total > 0 && confirmed < total) {
                return false;
            }
        }
        return true;
    }

    private MonitoringVO convertToVO(MonthlyMonitoring monitoring) {
        if (monitoring == null) return null;

        MonitoringVO vo = new MonitoringVO();
        vo.setId(monitoring.getId());
        vo.setSystemId(monitoring.getSystemId());
        vo.setYear(monitoring.getYear());
        vo.setMonth(monitoring.getMonth());
        vo.setPeriod(monitoring.getYear() + "年" + String.format("%02d", monitoring.getMonth()) + "月");
        vo.setStatus(monitoring.getStatus());
        vo.setDeadline(monitoring.getDeadline());
        vo.setApprovalRequired(monitoring.getApprovalRequired());
        vo.setProcessPercent(monitoring.getProcessPercent());
        vo.setProcessStatus(monitoring.getProcessStatus());
        vo.setProcessMsg(monitoring.getProcessMsg());
        vo.setCreatedBy(monitoring.getCreatedBy());
        vo.setCreatedAt(monitoring.getCreatedAt());
        vo.setUpdatedAt(monitoring.getUpdatedAt());

        AssessmentSystem system = systemMapper.selectById(monitoring.getSystemId());
        if (system != null) {
            vo.setSystemName(system.getName());
        }

        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        vo.setTotalInstitutions(institutions.size());

        int confirmedCount = 0;
        for (Institution inst : institutions) {
            int confirmed = leaderMapper.countConfirmedByInstitutionId(inst.getId());
            if (confirmed > 0) {
                confirmedCount++;
            }
        }
        vo.setConfirmedInstitutions(confirmedCount);
        vo.setPendingInstitutions(vo.getTotalInstitutions() - confirmedCount);

        vo.setTotalTasks(taskMapper.countByMonitoringId(monitoring.getId()));
        vo.setSubmittedTasks(taskMapper.countByMonitoringIdAndStatus(monitoring.getId(), CollectionTask.STATUS_SUBMITTED));
        vo.setPendingTasks(vo.getTotalTasks() - vo.getSubmittedTasks());

        return vo;
    }

    private List<MonitoringVO> convertToVO(List<MonthlyMonitoring> monitorings) {
        List<MonitoringVO> result = new ArrayList<>();
        for (MonthlyMonitoring m : monitorings) {
            result.add(convertToVO(m));
        }
        return result;
    }

    @Transactional
    public void batchGenerateReports(Long monitoringId) throws IOException {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_COLLECTING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许生成报告");
        }

        // Move to PROCESSING status
        monitoringMapper.updateStatus(monitoringId, MonthlyMonitoring.STATUS_PROCESSING);

        // Generate reports (this is a synchronous call, could be made async in future)
        dataProcessingService.generateInstitutionReports(monitoringId);

        // Move to CONFIRMING status
        monitoringMapper.updateStatus(monitoringId, MonthlyMonitoring.STATUS_CONFIRMING);

        // Create confirmation records for institution leaders
        notifyLeadersForConfirmation(monitoringId);
    }

    public List<ConfirmationTaskVO> getConfirmationTasks(Long monitoringId) {
        List<ConfirmationRecord> records = confirmMapper.selectByMonitoringId(monitoringId);
        List<ConfirmationTaskVO> result = new ArrayList<>();

        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);

        for (ConfirmationRecord record : records) {
            ConfirmationTaskVO vo = new ConfirmationTaskVO();
            vo.setId(record.getId());
            vo.setMonitoringId(record.getMonitoringId());
            vo.setInstitutionId(record.getInstitutionId());
            vo.setUserId(record.getUserId());
            vo.setStatus(record.getStatus());
            vo.setRemark(record.getRemark());
            if (record.getConfirmedAt() != null) {
                vo.setConfirmedAt(record.getConfirmedAt().toString());
            }

            Institution inst = institutionMapper.selectById(record.getInstitutionId());
            if (inst != null) {
                vo.setInstitutionName(inst.getOrgName());
            }

            SysUser user = userMapper.selectById(record.getUserId());
            if (user != null) {
                vo.setLeaderName(user.getName());
                vo.setLeaderEmpNo(user.getEmpNo());
            }

            result.add(vo);
        }

        return result;
    }

    @Transactional
    public void regenerateConfirmationTasks(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_CONFIRMING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不是确认中");
        }
        
        // 删除现有的确认记录
        confirmMapper.deleteByMonitoringId(monitoringId);
        
        // 重新生成确认记录
        notifyLeadersForConfirmation(monitoringId);
    }

    @Transactional
    public void rollbackToCollecting(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_CONFIRMING.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许回退");
        }
        
        // 将监测状态回退到收数中
        monitoringMapper.updateStatus(monitoringId, MonthlyMonitoring.STATUS_COLLECTING);
        
        // 删除所有确认记录
        confirmMapper.deleteByMonitoringId(monitoringId);
        
        // 重置机构负责人的确认状态
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        for (Institution inst : institutions) {
            List<InstitutionLeader> leaders = leaderMapper.selectByInstitutionId(inst.getId());
            for (InstitutionLeader leader : leaders) {
                leaderMapper.updateConfirmed(inst.getId(), leader.getUserId(), false);
            }
        }
    }

    @Transactional
    public void rollbackToConfirming(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }
        if (!MonthlyMonitoring.STATUS_PUBLISHED.equals(monitoring.getStatus())) {
            throw new RuntimeException("当前状态不允许回退到确认中");
        }
        
        // 将监测状态回退到确认中
        monitoringMapper.updateStatus(monitoringId, MonthlyMonitoring.STATUS_CONFIRMING);
    }

    @Transactional
    public void deleteMonitoring(Long monitoringId) throws IOException {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }

        // 1. 删除所有收数文档 (MinIO collectors bucket)
        List<CollectionTask> tasks = taskMapper.selectByMonitoringId(monitoringId);
        for (CollectionTask task : tasks) {
            if (task.getFileKey() != null && !task.getFileKey().isEmpty()) {
                try {
                    minioService.deleteFile(minioService.getBucketCollectors(), task.getFileKey());
                } catch (Exception e) {
                    // 忽略删除失败，继续执行
                }
            }
        }

        // 2. 删除所有绩效报告 (MinIO reports bucket)
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        for (Institution inst : institutions) {
            String reportFileKey = String.format("%d/%d/%02d/%s/report_%d%02d.xlsx",
                    monitoring.getSystemId(), monitoring.getYear(), monitoring.getMonth(),
                    inst.getOrgId(), monitoring.getYear(), monitoring.getMonth());
            try {
                minioService.deleteFile(minioService.getBucketReports(), reportFileKey);
            } catch (Exception e) {
                // 忽略删除失败，继续执行
            }
        }

        // 3. 删除数据库记录 (按依赖关系顺序删除)
        // 先删 monthly_indicator_data (通过 monitoringId)
        List<MonthlyIndicatorData> indicatorDataList = indicatorDataMapper.selectByMonitoringId(monitoringId);
        indicatorDataMapper.deleteByMonitoringId(monitoringId);

        // 删除 confirmation_record
        confirmMapper.deleteByMonitoringId(monitoringId);

        // 删除 collection_task
        taskMapper.deleteByMonitoringId(monitoringId);

        // 最后删除 monthly_monitoring
        monitoringMapper.deleteById(monitoringId);
    }

    // ==================== Notification methods for MCP ====================

    public List<Notification> getNotifications(Long userId) {
        return notificationService.getUserNotifications(userId);
    }

    public int getUnreadNotificationCount(Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    public void markNotificationRead(Long notificationId) {
        notificationService.markAsRead(notificationId);
    }
}
