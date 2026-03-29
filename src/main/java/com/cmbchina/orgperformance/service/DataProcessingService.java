package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.entity.*;
import com.cmbchina.orgperformance.excel.ExcelDataReader;
import com.cmbchina.orgperformance.excel.ExcelGenerator;
import com.cmbchina.orgperformance.excel.ExcelTemplateParser;
import com.cmbchina.orgperformance.mapper.*;
import com.cmbchina.orgperformance.minio.MinioService;
import com.cmbchina.orgperformance.vo.CollectionTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DataProcessingService {

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private IndicatorMapper indicatorMapper;

    @Autowired
    private MonthlyIndicatorDataMapper indicatorDataMapper;

    @Autowired
    private CollectionTaskMapper taskMapper;

    @Autowired
    private MinioService minioService;

    @Autowired
    private ExcelDataReader excelDataReader;

    @Autowired
    private ExcelTemplateParser excelTemplateParser;

    @Autowired
    private ExcelGenerator excelGenerator;

    @Autowired
    private AssessmentSystemMapper systemMapper;

    public String getCollectorFileKey(Long monitoringId, Long collectorUserId) {
        CollectionTask task = taskMapper.selectByMonitoringIdAndCollectorUserId(monitoringId, collectorUserId)
                .stream().findFirst().orElse(null);
        return task != null ? task.getFileKey() : null;
    }

    public String getCollectorFileUrl(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        return minioService.getPresignedUrl(minioService.getBucketCollectors(), fileKey, 60);
    }

    public byte[] downloadCollectorFile(Long monitoringId, Long collectorUserId) {
        String fileKey = getCollectorFileKey(monitoringId, collectorUserId);
        if (fileKey == null) {
            throw new RuntimeException("Collector file not found");
        }
        return minioService.downloadAsBytes(minioService.getBucketCollectors(), fileKey);
    }

    private final Map<Long, ProcessingProgress> progressMap = new HashMap<>();

    public void processMonitoringData(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("Monitoring not found");
        }

        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        List<Indicator> indicators = indicatorMapper.selectBySystemId(monitoring.getSystemId());

        ProcessingProgress progress = new ProcessingProgress();
        progress.setTotal(institutions.size());
        progressMap.put(monitoringId, progress);

        for (Institution institution : institutions) {
            try {
                processInstitutionData(monitoring, institution, indicators);
                progress.incrementProcessed();
                int percent = (int) ((progress.getProcessed() * 100.0) / progress.getTotal());
                monitoringMapper.updateProcessStatus(monitoringId,
                        MonthlyMonitoring.PROCESS_PROCESSING, percent, "Processing...");
            } catch (Exception e) {
                monitoringMapper.updateProcessStatus(monitoringId,
                        MonthlyMonitoring.PROCESS_FAILED, 0, "Error processing " + institution.getOrgName() + ": " + e.getMessage());
                return;
            }
        }

        monitoringMapper.updateProcessStatus(monitoringId,
                MonthlyMonitoring.PROCESS_DONE, 100, "Processing completed");
        progressMap.remove(monitoringId);
    }

    private void processInstitutionData(MonthlyMonitoring monitoring, Institution institution,
                                        List<Indicator> indicators) throws IOException {
        ByteArrayOutputStream templateStream = downloadTemplate(monitoring.getSystemId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("institutionName", institution.getOrgName());
        metadata.put("year", monitoring.getYear());
        metadata.put("month", monitoring.getMonth());

        byte[] reportData = generateReportFromTemplate(templateStream.toByteArray(), institution,
                monitoring.getYear(), monitoring.getMonth(), metadata);

        String fileKey = minioService.uploadReport(reportData, monitoring.getSystemId(),
                monitoring.getYear(), monitoring.getMonth(), institution.getOrgId());

        saveIndicatorData(monitoring.getId(), institution.getId(), fileKey, indicators, reportData);
    }

    private ByteArrayOutputStream downloadTemplate(Long systemId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return out;
    }

    private byte[] generateReportFromTemplate(byte[] templateData, Institution institution,
                                              Integer year, Integer month,
                                              Map<String, Object> metadata) throws IOException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(templateData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        return outputStream.toByteArray();
    }

    private void saveIndicatorData(Long monitoringId, Long institutionId, String fileKey,
                                    List<Indicator> indicators, byte[] reportData) throws IOException {
        List<MonthlyIndicatorData> dataList = new ArrayList<>();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(reportData);
        Map<String, Object> templateData = excelDataReader.readTemplateData(inputStream);
        List<Map<String, Object>> indicatorList = (List<Map<String, Object>>) templateData.get("indicators");

        for (int i = 0; i < indicatorList.size() && i < indicators.size(); i++) {
            Map<String, Object> indData = indicatorList.get(i);
            Indicator indicator = indicators.get(i);

            MonthlyIndicatorData data = new MonthlyIndicatorData();
            data.setMonitoringId(monitoringId);
            data.setIndicatorId(indicator.getId());
            data.setInstitutionId(institutionId);
            data.setFileKey(fileKey);

            Object actualValue = indData.get("actualValue");
            if (actualValue instanceof BigDecimal) {
                data.setActualValue((BigDecimal) actualValue);
            } else if (actualValue instanceof Number) {
                data.setActualValue(BigDecimal.valueOf(((Number) actualValue).doubleValue()));
            }

            Object annualTarget = indData.get("annualTarget");
            if (annualTarget instanceof BigDecimal) {
                data.setAnnualTarget((BigDecimal) annualTarget);
            } else if (annualTarget instanceof Number) {
                data.setAnnualTarget(BigDecimal.valueOf(((Number) annualTarget).doubleValue()));
            }

            Object progressTarget = indData.get("progressTarget");
            if (progressTarget instanceof BigDecimal) {
                data.setProgressTarget((BigDecimal) progressTarget);
            } else if (progressTarget instanceof Number) {
                data.setProgressTarget(BigDecimal.valueOf(((Number) progressTarget).doubleValue()));
            }

            Object annualRate = indData.get("annualCompletionRate");
            if (annualRate instanceof BigDecimal) {
                data.setAnnualCompletionRate((BigDecimal) annualRate);
            } else if (annualRate instanceof Number) {
                data.setAnnualCompletionRate(BigDecimal.valueOf(((Number) annualRate).doubleValue()));
            }

            Object progressRate = indData.get("progressCompletionRate");
            if (progressRate instanceof BigDecimal) {
                data.setProgressCompletionRate((BigDecimal) progressRate);
            } else if (progressRate instanceof Number) {
                data.setProgressCompletionRate(BigDecimal.valueOf(((Number) progressRate).doubleValue()));
            }

            Object score100 = indData.get("score100");
            if (score100 instanceof BigDecimal) {
                data.setScore100((BigDecimal) score100);
            } else if (score100 instanceof Number) {
                data.setScore100(BigDecimal.valueOf(((Number) score100).doubleValue()));
            }

            Object scoreWeighted = indData.get("scoreWeighted");
            if (scoreWeighted instanceof BigDecimal) {
                data.setScoreWeighted((BigDecimal) scoreWeighted);
            } else if (scoreWeighted instanceof Number) {
                data.setScoreWeighted(BigDecimal.valueOf(((Number) scoreWeighted).doubleValue()));
            }

            Object scoreCategory = indData.get("scoreCategory");
            if (scoreCategory instanceof BigDecimal) {
                data.setScoreCategory((BigDecimal) scoreCategory);
            } else if (scoreCategory instanceof Number) {
                data.setScoreCategory(BigDecimal.valueOf(((Number) scoreCategory).doubleValue()));
            }

            Object scoreDimension = indData.get("scoreDimension");
            if (scoreDimension instanceof BigDecimal) {
                data.setScoreDimension((BigDecimal) scoreDimension);
            } else if (scoreDimension instanceof Number) {
                data.setScoreDimension(BigDecimal.valueOf(((Number) scoreDimension).doubleValue()));
            }

            Object totalScore = indData.get("totalScore");
            if (totalScore instanceof BigDecimal) {
                data.setTotalScore((BigDecimal) totalScore);
            } else if (totalScore instanceof Number) {
                data.setTotalScore(BigDecimal.valueOf(((Number) totalScore).doubleValue()));
            }

            dataList.add(data);
        }

        if (!dataList.isEmpty()) {
            indicatorDataMapper.batchInsert(dataList);
        }
    }

    public void updateProgress(Long monitoringId) {
        ProcessingProgress progress = progressMap.get(monitoringId);
        if (progress != null) {
            int percent = (int) ((progress.getProcessed() * 100.0) / progress.getTotal());
            monitoringMapper.updateProcessStatus(monitoringId,
                    MonthlyMonitoring.PROCESS_PROCESSING, percent, "Processing...");
        }
    }

    @Transactional
    public void submitTaskData(Long taskId, BigDecimal actualValue) {
        CollectionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found");
        }
        if (!CollectionTask.STATUS_PENDING.equals(task.getStatus())) {
            throw new RuntimeException("Task already submitted");
        }

        taskMapper.submitTask(taskId, actualValue, CollectionTask.STATUS_SUBMITTED);
    }

    @Transactional
    public int batchSubmitTasks(List<com.cmbchina.orgperformance.dto.TaskUpdateRequest> updates) {
        int count = 0;
        for (com.cmbchina.orgperformance.dto.TaskUpdateRequest update : updates) {
            CollectionTask task = taskMapper.selectById(update.getTaskId());
            if (task != null && CollectionTask.STATUS_PENDING.equals(task.getStatus())) {
                taskMapper.submitTask(update.getTaskId(), update.getActualValue(), CollectionTask.STATUS_SUBMITTED);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public void uploadDataCollection(Long monitoringId, MultipartFile file) throws IOException {
        // 解析上传的"数据收集页"
        Map<String, Map<String, BigDecimal>> data = excelDataReader.readDataCollectionFromFile(file.getInputStream());

        // 获取该监测的所有任务
        List<CollectionTask> allTasks = taskMapper.selectByMonitoringId(monitoringId);

        int updatedCount = 0;
        for (CollectionTask task : allTasks) {
            String institutionName = null;
            Institution inst = institutionMapper.selectById(task.getInstitutionId());
            if (inst != null) {
                institutionName = inst.getOrgName();
            }
            if (institutionName == null) continue;

            // 根据 collection_indicator_name 查找对应的值
            String collectionIndicatorName = task.getCollectionIndicatorName();
            if (collectionIndicatorName == null || collectionIndicatorName.isEmpty()) continue;

            Map<String, BigDecimal> instData = data.get(institutionName);
            if (instData == null) continue;

            BigDecimal value = instData.get(collectionIndicatorName);
            if (value != null) {
                taskMapper.submitTask(task.getId(), value, CollectionTask.STATUS_SUBMITTED);
                updatedCount++;
            }
        }

        // 上传文件到MinIO保存
        minioService.uploadDataCollection(file, monitoringId, null, null, null);
    }

    public List<CollectionTaskVO> getTasksByMonitoringId(Long monitoringId) {
        List<CollectionTask> tasks = taskMapper.selectByMonitoringId(monitoringId);
        return convertTasksToVO(tasks);
    }

    public List<CollectionTaskVO> getTasksByMonitoringId(Long monitoringId, String status) {
        List<CollectionTask> tasks;
        if (status != null && !status.isEmpty()) {
            tasks = taskMapper.selectByMonitoringIdAndStatus(monitoringId, status);
        } else {
            tasks = taskMapper.selectByMonitoringId(monitoringId);
        }
        return convertTasksToVO(tasks);
    }

    public List<CollectionTaskVO> getTasksByCollector(Long monitoringId, Long collectorUserId) {
        List<CollectionTask> tasks = taskMapper.selectByMonitoringIdAndCollectorUserId(monitoringId, collectorUserId);
        return convertTasksToVO(tasks);
    }

    private List<CollectionTaskVO> convertTasksToVO(List<CollectionTask> tasks) {
        List<CollectionTaskVO> result = new ArrayList<>();
        Map<Long, CollectionTaskVO> taskMap = new LinkedHashMap<>();

        for (CollectionTask task : tasks) {
            CollectionTaskVO vo = new CollectionTaskVO();
            vo.setId(task.getId());
            vo.setMonitoringId(task.getMonitoringId());
            vo.setIndicatorId(task.getIndicatorId());
            vo.setInstitutionId(task.getInstitutionId());
            vo.setCollectorName(task.getCollectorName());
            vo.setCollectorEmpNo(task.getCollectorEmpNo());
            vo.setCollectorUserId(task.getCollectorUserId());
            vo.setActualValue(task.getActualValue());
            vo.setStatus(task.getStatus());
            vo.setFileKey(task.getFileKey());

            // 数据收集页的收数指标信息
            vo.setCollectionIndicatorName(task.getCollectionIndicatorName());
            vo.setCollectionUnit(task.getCollectionUnit());
            vo.setCollectionDimension(task.getCollectionDimension());
            vo.setCollectionCategory(task.getCollectionCategory());

            Indicator indicator = indicatorMapper.selectById(task.getIndicatorId());
            if (indicator != null) {
                vo.setDimension(indicator.getDimension());
                vo.setCategory(indicator.getCategory());
                vo.setLevel1Name(indicator.getLevel1Name());
                vo.setLevel2Name(indicator.getLevel2Name());
                vo.setUnit(indicator.getUnit());
            }

            Institution institution = institutionMapper.selectById(task.getInstitutionId());
            if (institution != null) {
                vo.setInstitutionName(institution.getOrgName());
            }

            result.add(vo);
        }

        return result;
    }

    @Transactional
    public int generateInstitutionReports(Long monitoringId) throws IOException {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("Monitoring not found");
        }

        AssessmentSystem system = systemMapper.selectById(monitoring.getSystemId());
        if (system == null || system.getTemplateFileKey() == null) {
            throw new RuntimeException("System template not found");
        }

        byte[] templateData = minioService.downloadAsBytes(
                minioService.getBucketTemplates(), system.getTemplateFileKey());

        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());
        List<CollectionTask> allTasks = taskMapper.selectByMonitoringId(monitoringId);

        int count = 0;
        for (Institution institution : institutions) {
            Map<String, BigDecimal> indicatorData = new HashMap<>();
            List<CollectionTask> institutionTasks = new ArrayList<>();

            for (CollectionTask task : allTasks) {
                if (task.getInstitutionId() != null && task.getInstitutionId().equals(institution.getId()) && task.getActualValue() != null) {
                    // 使用collectionIndicatorName作为key，因为数据收集页的表头使用的是这个名称
                    String indicatorName = task.getCollectionIndicatorName();
                    if (indicatorName != null && !indicatorName.isEmpty()) {
                        indicatorData.put(indicatorName, task.getActualValue());
                    }
                    institutionTasks.add(task);
                }
            }

            byte[] reportData = excelGenerator.generateInstitutionReport(
                    templateData, institution, indicatorData, monitoring.getYear(), monitoring.getMonth(),
                    monitoring.getSystemId(), institutionTasks);

            String fileKey = minioService.uploadReport(reportData, monitoring.getSystemId(),
                    monitoring.getYear(), monitoring.getMonth(), institution.getOrgId());

            saveIndicatorDataFromReport(monitoringId, institution.getId(), institution.getOrgName(), monitoring.getSystemId(), reportData, fileKey);
            count++;
        }

        return count;
    }

    private void saveIndicatorDataFromReport(Long monitoringId, Long institutionId, String institutionName, Long systemId, byte[] reportData, String fileKey) {
        // Delete existing data for this monitoring and institution to avoid duplicate key errors
        indicatorDataMapper.deleteByMonitoringIdAndInstitutionId(monitoringId, institutionId);

        // Read actual values from data collection sheet
        Map<String, BigDecimal> actualValues;
        try (InputStream is = new ByteArrayInputStream(reportData)) {
            actualValues = excelDataReader.readIndicatorDataFromDataCollectionSheet(is, institutionName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data collection sheet", e);
        }

        // Read template data (scores) from the report
        Map<String, Object> templateData;
        try (InputStream is = new ByteArrayInputStream(reportData)) {
            templateData = excelDataReader.readTemplateData(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read template data", e);
        }
        List<Map<String, Object>> templateIndicators = (List<Map<String, Object>>) templateData.get("indicators");

        List<MonthlyIndicatorData> dataList = new ArrayList<>();
        List<Indicator> dbIndicators = indicatorMapper.selectBySystemId(systemId);

        for (int i = 0; i < dbIndicators.size(); i++) {
            Indicator dbIndicator = dbIndicators.get(i);
            MonthlyIndicatorData data = new MonthlyIndicatorData();
            data.setMonitoringId(monitoringId);
            data.setInstitutionId(institutionId);
            data.setIndicatorId(dbIndicator.getId());
            data.setFileKey(fileKey);

            // Set actual value
            String level2Name = dbIndicator.getLevel2Name() != null ? dbIndicator.getLevel2Name() : "";
            BigDecimal actualValue = actualValues.get(level2Name);
            if (actualValue == null) {
                actualValue = actualValues.get(dbIndicator.getLevel1Name());
            }
            data.setActualValue(actualValue);

            // Set scores and targets from template data
            if (i < templateIndicators.size()) {
                Map<String, Object> tmplInd = templateIndicators.get(i);
                data.setAnnualTarget(getBigDecimal(tmplInd.get("annualTarget")));
                data.setProgressTarget(getBigDecimal(tmplInd.get("progressTarget")));
                data.setAnnualCompletionRate(getBigDecimal(tmplInd.get("annualCompletionRate")));
                data.setProgressCompletionRate(getBigDecimal(tmplInd.get("progressCompletionRate")));
                data.setScore100(getBigDecimal(tmplInd.get("score100")));
                data.setScoreWeighted(getBigDecimal(tmplInd.get("scoreWeighted")));
                data.setScoreCategory(getBigDecimal(tmplInd.get("scoreCategory")));
                data.setScoreDimension(getBigDecimal(tmplInd.get("scoreDimension")));
                data.setTotalScore(getBigDecimal(tmplInd.get("totalScore")));
            }

            dataList.add(data);
        }

        if (!dataList.isEmpty()) {
            indicatorDataMapper.batchInsert(dataList);
        }
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static class ProcessingProgress {
        private int total;
        private AtomicInteger processed = new AtomicInteger(0);

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getProcessed() { return processed.get(); }
        public void incrementProcessed() { processed.incrementAndGet(); }
    }
}
