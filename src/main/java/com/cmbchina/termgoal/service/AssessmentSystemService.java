package com.cmbchina.termgoal.service;

import com.cmbchina.termgoal.dto.SystemCreateRequest;
import com.cmbchina.termgoal.entity.AssessmentSystem;
import com.cmbchina.termgoal.entity.CollectionTask;
import com.cmbchina.termgoal.entity.Institution;
import com.cmbchina.termgoal.entity.Indicator;
import com.cmbchina.termgoal.entity.MonthlyMonitoring;
import com.cmbchina.termgoal.excel.ExcelTemplateParser;
import com.cmbchina.termgoal.mapper.AssessmentSystemMapper;
import com.cmbchina.termgoal.mapper.CollectionTaskMapper;
import com.cmbchina.termgoal.mapper.MonthlyMonitoringMapper;
import com.cmbchina.termgoal.mapper.InstitutionMapper;
import com.cmbchina.termgoal.mapper.IndicatorMapper;
import com.cmbchina.termgoal.minio.MinioService;
import com.cmbchina.termgoal.vo.SystemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssessmentSystemService {

    @Autowired
    private AssessmentSystemMapper systemMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private IndicatorMapper indicatorMapper;

    @Autowired
    private ExcelTemplateParser templateParser;

    @Autowired
    private MinioService minioService;

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private CollectionTaskMapper taskMapper;

    public List<SystemVO> getSystemList(String name, Integer status, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        List<AssessmentSystem> systems = systemMapper.selectList(name, status, offset, pageSize);
        return convertToVO(systems);
    }

    public int count(String name, Integer status) {
        return systemMapper.count(name, status);
    }

    public SystemVO getSystemById(Long id) {
        AssessmentSystem system = systemMapper.selectById(id);
        return convertToVO(system);
    }

    @Transactional
    public Long createSystem(SystemCreateRequest request, MultipartFile file) throws IOException {
        AssessmentSystem system = new AssessmentSystem();
        system.setName(request.getName());
        system.setDescription(request.getDescription());
        system.setNeedApproval(request.getNeedApproval());
        system.setStatus(1);
        system.setCreatedBy("admin");

        String fileKey = minioService.uploadTemplate(file, null);
        system.setTemplateFileKey(fileKey);

        systemMapper.insert(system);

        fileKey = minioService.uploadTemplate(file, system.getId());
        systemMapper.updateTemplateFileKey(system.getId(), fileKey);

        return system.getId();
    }

    @Transactional
    public Long createSystemWithParsedData(SystemCreateRequest request) {
        AssessmentSystem system = new AssessmentSystem();
        system.setName(request.getName());
        system.setDescription(request.getDescription());
        system.setNeedApproval(request.getNeedApproval());
        system.setStatus(1);
        system.setCreatedBy("admin");

        systemMapper.insert(system);

        List<Institution> institutions = new ArrayList<>();
        if (request.getInstitutions() != null) {
            for (SystemCreateRequest.InstitutionDTO dto : request.getInstitutions()) {
                Institution inst = new Institution();
                inst.setSystemId(system.getId());
                inst.setOrgName(dto.getOrgName());
                inst.setOrgId(dto.getOrgId());
                inst.setGroupName(dto.getGroupName());
                inst.setLeaderName(dto.getLeaderName());
                inst.setLeaderEmpNo(dto.getLeaderEmpNo());
                institutions.add(inst);
            }
            if (!institutions.isEmpty()) {
                institutionMapper.batchInsert(institutions);
            }
        }

        List<Indicator> indicators = new ArrayList<>();
        if (request.getIndicators() != null) {
            for (SystemCreateRequest.IndicatorDTO dto : request.getIndicators()) {
                Indicator ind = new Indicator();
                ind.setSystemId(system.getId());
                ind.setDimension(dto.getDimension());
                ind.setCategory(dto.getCategory());
                ind.setLevel1Name(dto.getLevel1Name());
                ind.setLevel2Name(dto.getLevel2Name());
                ind.setWeight(dto.getWeight() != null ?
                        java.math.BigDecimal.valueOf(dto.getWeight()) : null);
                ind.setUnit(dto.getUnit());
                ind.setAnnualTarget(dto.getAnnualTarget() != null ?
                        java.math.BigDecimal.valueOf(dto.getAnnualTarget()) : null);
                ind.setProgressTarget(dto.getProgressTarget() != null ?
                        java.math.BigDecimal.valueOf(dto.getProgressTarget()) : null);
                ind.setRowIndex(dto.getRowIndex());
                indicators.add(ind);
            }
            if (!indicators.isEmpty()) {
                indicatorMapper.batchInsert(indicators);
            }
        }

        return system.getId();
    }

    @Transactional
    public Long createSystemWithExcel(String name, String description, Boolean needApproval, MultipartFile file) throws IOException {
        // 1. 创建体系记录
        AssessmentSystem system = new AssessmentSystem();
        system.setName(name);
        system.setDescription(description);
        system.setNeedApproval(needApproval != null ? needApproval : false);
        system.setStatus(1);
        system.setCreatedBy("admin");
        systemMapper.insert(system);

        // 2. 上传Excel模板到MinIO
        String fileKey = minioService.uploadTemplate(file, system.getId());
        system.setTemplateFileKey(fileKey);
        systemMapper.updateTemplateFileKey(system.getId(), fileKey);

        // 3. 解析Excel模板，保存机构和指标信息
        try (java.io.InputStream inputStream = file.getInputStream()) {
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(inputStream);

            // 解析机构页
            List<Institution> institutions = templateParser.parseInstitutionSheet(workbook);
            for (Institution inst : institutions) {
                inst.setSystemId(system.getId());
                inst.setCreatedAt(java.time.LocalDateTime.now());
            }
            if (!institutions.isEmpty()) {
                institutionMapper.batchInsert(institutions);
            }

            // 解析模版页（指标）
            List<Indicator> indicators = templateParser.parseTemplateSheet(workbook);
            for (Indicator ind : indicators) {
                ind.setSystemId(system.getId());
            }
            if (!indicators.isEmpty()) {
                indicatorMapper.batchInsert(indicators);
            }

            workbook.close();
        }

        return system.getId();
    }

    @Transactional
    public void updateSystem(Long id, SystemCreateRequest request) {
        AssessmentSystem system = systemMapper.selectById(id);
        if (system == null) {
            throw new RuntimeException("System not found");
        }

        system.setName(request.getName());
        system.setDescription(request.getDescription());
        system.setNeedApproval(request.getNeedApproval());
        systemMapper.update(system);
    }

    @Transactional
    public void deleteSystem(Long id) {
        // 1. 查询体系信息获取模板文件key
        AssessmentSystem system = systemMapper.selectById(id);
        if (system == null) {
            throw new RuntimeException("System not found");
        }

        // 2. 查询该体系的所有监测记录
        List<MonthlyMonitoring> monitorings = monitoringMapper.selectBySystemId(id);

        // 3. 删除每个监测的MinIO文件和数据库记录
        for (MonthlyMonitoring monitoring : monitorings) {
            Long monitoringId = monitoring.getId();

            // 删除该监测的收集任务记录
            List<CollectionTask> tasks = taskMapper.selectByMonitoringId(monitoringId);

            // 删除收集文档（MinIO）
            for (CollectionTask task : tasks) {
                if (task.getFileKey() != null && !task.getFileKey().isEmpty()) {
                    try {
                        minioService.deleteFile(minioService.getBucketCollectors(), task.getFileKey());
                    } catch (Exception e) {
                        // 忽略删除失败
                    }
                }
            }

            // 删除该监测的收集任务
            taskMapper.deleteByMonitoringId(monitoringId);

            // 删除该监测的MinIO报告文件
            try {
                String reportsPrefix = String.format("%d/%d/", id, monitoring.getYear());
                // 删除该监测的所有报告文件（简化处理，实际可以列出并删除）
            } catch (Exception e) {
                // 忽略
            }

            // 删除监测记录
            monitoringMapper.deleteById(monitoringId);
        }

        // 4. 删除体系模板文件（MinIO）
        if (system.getTemplateFileKey() != null && !system.getTemplateFileKey().isEmpty()) {
            try {
                minioService.deleteFile(minioService.getBucketTemplates(), system.getTemplateFileKey());
            } catch (Exception e) {
                // 忽略删除失败
            }
        }

        // 5. 删除机构和指标
        institutionMapper.deleteBySystemId(id);
        indicatorMapper.deleteBySystemId(id);

        // 6. 删除体系记录
        systemMapper.deleteById(id);
    }

    public List<Institution> getInstitutionsBySystemId(Long systemId) {
        return institutionMapper.selectBySystemId(systemId);
    }

    public List<Indicator> getIndicatorsBySystemId(Long systemId) {
        return indicatorMapper.selectBySystemId(systemId);
    }

    public List<String> getGroupNamesBySystemId(Long systemId) {
        return institutionMapper.selectGroupNamesBySystemId(systemId);
    }

    public String getTemplateDownloadUrl(Long systemId) {
        AssessmentSystem system = systemMapper.selectById(systemId);
        if (system == null || system.getTemplateFileKey() == null) {
            throw new RuntimeException("Template not found");
        }
        return minioService.getPresignedUrl(minioService.getBucketTemplates(), system.getTemplateFileKey(), 30);
    }

    private SystemVO convertToVO(AssessmentSystem system) {
        if (system == null) return null;

        SystemVO vo = new SystemVO();
        vo.setId(system.getId());
        vo.setName(system.getName());
        vo.setDescription(system.getDescription());
        vo.setTemplateFileKey(system.getTemplateFileKey());
        vo.setNeedApproval(system.getNeedApproval());
        vo.setStatus(system.getStatus());
        vo.setCreatedBy(system.getCreatedBy());
        vo.setCreatedAt(system.getCreatedAt());

        List<Institution> institutions = institutionMapper.selectBySystemId(system.getId());
        vo.setInstitutionCount(institutions.size());
        vo.setGroupNames(institutionMapper.selectGroupNamesBySystemId(system.getId()));

        List<Indicator> indicators = indicatorMapper.selectBySystemId(system.getId());
        vo.setIndicatorCount(indicators.size());

        return vo;
    }

    private List<SystemVO> convertToVO(List<AssessmentSystem> systems) {
        List<SystemVO> result = new ArrayList<>();
        for (AssessmentSystem system : systems) {
            result.add(convertToVO(system));
        }
        return result;
    }
}
