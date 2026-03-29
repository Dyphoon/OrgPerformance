package com.cmbchina.orgperformance.service;

import com.cmbchina.orgperformance.entity.*;
import com.cmbchina.orgperformance.mapper.*;
import com.cmbchina.orgperformance.minio.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private MonthlyMonitoringMapper monitoringMapper;

    @Autowired
    private AssessmentSystemMapper systemMapper;

    @Autowired
    private InstitutionMapper institutionMapper;

    @Autowired
    private IndicatorMapper indicatorMapper;

    @Autowired
    private MonthlyIndicatorDataMapper dataMapper;

    @Autowired
    private MinioService minioService;

    @Value("${minio.bucket-reports}")
    private String bucketReports;

    public Map<String, Object> getOverview(Long monitoringId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }

        AssessmentSystem system = systemMapper.selectById(monitoring.getSystemId());
        List<Institution> institutions = institutionMapper.selectBySystemId(monitoring.getSystemId());

        List<Map<String, Object>> institutionRanks = new ArrayList<>();
        Map<String, List<Map<String, Object>>> groupMap = new HashMap<>();
        Set<String> dimensions = new LinkedHashSet<>();

        for (Institution inst : institutions) {
            List<MonthlyIndicatorData> dataList = dataMapper.selectByMonitoringIdAndInstitutionId(monitoringId, inst.getId());

            double totalScore = dataList.stream()
                    .filter(d -> d.getTotalScore() != null)
                    .mapToDouble(d -> d.getTotalScore().doubleValue())
                    .sum();

            Set<String> instDimensions = dataList.stream()
                    .map(d -> {
                        Indicator ind = indicatorMapper.selectById(d.getIndicatorId());
                        return ind != null ? ind.getDimension() : "";
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
            dimensions.addAll(instDimensions);

            Map<String, Object> rankItem = new HashMap<>();
            rankItem.put("institutionId", inst.getId());
            rankItem.put("institutionName", inst.getOrgName());
            rankItem.put("groupName", inst.getGroupName());
            rankItem.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
            institutionRanks.add(rankItem);

            groupMap.computeIfAbsent(inst.getGroupName(), k -> new ArrayList<>()).add(rankItem);
        }

        institutionRanks.sort((a, b) -> Double.compare((Double) b.get("totalScore"), (Double) a.get("totalScore")));
        for (int i = 0; i < institutionRanks.size(); i++) {
            institutionRanks.get(i).put("rank", i + 1);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupMap.entrySet()) {
            entry.getValue().sort((a, b) -> Double.compare((Double) b.get("totalScore"), (Double) a.get("totalScore")));
            for (int i = 0; i < entry.getValue().size(); i++) {
                entry.getValue().get(i).put("groupRank", i + 1);
            }
        }

        List<Map<String, Object>> groupOverviews = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : groupMap.entrySet()) {
            List<Map<String, Object>> instList = entry.getValue();
            double avg = instList.stream()
                    .mapToDouble(m -> ((Number) m.get("totalScore")).doubleValue())
                    .average().orElse(0);
            double max = instList.stream()
                    .mapToDouble(m -> ((Number) m.get("totalScore")).doubleValue())
                    .max().orElse(0);
            double min = instList.stream()
                    .mapToDouble(m -> ((Number) m.get("totalScore")).doubleValue())
                    .min().orElse(0);
            Map<String, Object> go = new HashMap<>();
            go.put("groupName", entry.getKey());
            go.put("institutionCount", instList.size());
            go.put("avgScore", Math.round(avg * 100.0) / 100.0);
            go.put("maxScore", Math.round(max * 100.0) / 100.0);
            go.put("minScore", Math.round(min * 100.0) / 100.0);
            go.put("institutions", entry.getValue());
            groupOverviews.add(go);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("monitoringId", monitoringId);
        result.put("systemName", system != null ? system.getName() : "");
        result.put("year", monitoring.getYear());
        result.put("month", monitoring.getMonth());
        result.put("institutionRanks", institutionRanks);
        result.put("dimensions", new ArrayList<>(dimensions));
        result.put("groupOverviews", groupOverviews);

        return result;
    }

    public Map<String, Object> getInstitutionReport(Long monitoringId, Long institutionId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        if (monitoring == null) {
            throw new RuntimeException("监测不存在");
        }

        Institution institution = institutionMapper.selectById(institutionId);
        if (institution == null) {
            throw new RuntimeException("机构不存在");
        }

        AssessmentSystem system = systemMapper.selectById(monitoring.getSystemId());

        List<MonthlyIndicatorData> dataList = dataMapper.selectDetailByInstitution(monitoringId, institutionId);

        double totalScore = dataList.stream()
                .filter(d -> d.getTotalScore() != null)
                .mapToDouble(d -> d.getTotalScore().doubleValue())
                .sum();

        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        Map<String, Double> categoryScores = new LinkedHashMap<>();
        for (MonthlyIndicatorData data : dataList) {
            Indicator ind = indicatorMapper.selectById(data.getIndicatorId());
            if (ind != null) {
                double score = data.getTotalScore() != null ? data.getTotalScore().doubleValue() : 0.0;
                dimensionScores.merge(ind.getDimension(), score, Double::sum);
                categoryScores.merge(ind.getCategory(), score, Double::sum);
            }
        }

        List<Map<String, Object>> dimensionScoreList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : dimensionScores.entrySet()) {
            Map<String, Object> ds = new HashMap<>();
            ds.put("dimension", entry.getKey());
            ds.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
            dimensionScoreList.add(ds);
        }

        List<Map<String, Object>> categoryScoreList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryScores.entrySet()) {
            Map<String, Object> cs = new HashMap<>();
            cs.put("category", entry.getKey());
            cs.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
            categoryScoreList.add(cs);
        }

        List<Map<String, Object>> indicators = new ArrayList<>();
        for (MonthlyIndicatorData data : dataList) {
            Indicator ind = indicatorMapper.selectById(data.getIndicatorId());
            if (ind == null) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("dimension", ind.getDimension());
            item.put("category", ind.getCategory());
            item.put("level1Name", ind.getLevel1Name());
            item.put("level2Name", ind.getLevel2Name());
            item.put("unit", ind.getUnit());
            item.put("actualValue", data.getActualValue());
            item.put("target", data.getAnnualTarget());
            item.put("progressTarget", data.getProgressTarget());
            item.put("completionRate", data.getProgressCompletionRate());
            item.put("score", data.getScore100());
            item.put("weight", ind.getWeight());
            item.put("weightedScore", data.getScoreWeighted());
            indicators.add(item);
        }

        List<Map<String, Object>> allRanks = (List<Map<String, Object>>) getOverview(monitoringId).get("institutionRanks");
        int totalRank = 1;
        int groupRank = 1;
        for (Map<String, Object> r : allRanks) {
            if (((Number) r.get("institutionId")).longValue() == institutionId) {
                totalRank = (Integer) r.get("rank");
                groupRank = (Integer) r.get("groupRank");
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("monitoringId", monitoringId);
        result.put("systemName", system != null ? system.getName() : "");
        result.put("year", monitoring.getYear());
        result.put("month", monitoring.getMonth());
        result.put("institutionId", institutionId);
        result.put("institutionName", institution.getOrgName());
        result.put("groupName", institution.getGroupName());
        result.put("totalScore", Math.round(totalScore * 100.0) / 100.0);
        result.put("totalRank", totalRank);
        result.put("groupRank", groupRank);
        result.put("dimensionScores", dimensionScoreList);
        result.put("categoryScores", categoryScoreList);
        result.put("indicators", indicators);

        return result;
    }

    public InputStream downloadInstitutionReport(Long monitoringId, Long institutionId) {
        String fileKey = dataMapper.selectFileKeyByMonitoringAndInstitution(monitoringId, institutionId);
        if (fileKey == null || fileKey.isEmpty()) {
            throw new RuntimeException("绩效报告文件不存在，请先生成报表");
        }
        try {
            return minioService.downloadFile(bucketReports, fileKey);
        } catch (Exception e) {
            throw new RuntimeException("绩效报告文件在存储中不存在或已损坏: " + fileKey, e);
        }
    }

    public String getInstitutionReportFileName(Long monitoringId, Long institutionId) {
        MonthlyMonitoring monitoring = monitoringMapper.selectById(monitoringId);
        Institution institution = institutionMapper.selectById(institutionId);
        if (monitoring == null || institution == null) {
            return "report.xlsx";
        }
        return String.format("report_%d_%d_%d%02d.xlsx", 
                institution.getId(),
                monitoring.getSystemId(),
                monitoring.getYear(), 
                monitoring.getMonth());
    }

    public String getFileKey(Long monitoringId, Long institutionId) {
        return dataMapper.selectFileKeyByMonitoringAndInstitution(monitoringId, institutionId);
    }

    public Map<String, String> getBucketConfig() {
        return Map.of("bucketReports", bucketReports);
    }

    public List<Map<String, Object>> getMonitoringsWithReports(String status) {
        List<MonthlyMonitoring> monitorings;
        if (status != null && !status.isEmpty()) {
            monitorings = monitoringMapper.selectList(null, status, null, null, null, null);
        } else {
            monitorings = monitoringMapper.selectAll();
        }
        return monitorings.stream().map(m -> {
            String fileKey = dataMapper.selectFileKeyByMonitoringAndInstitution(m.getId(), null);
            return Map.<String, Object>of(
                "id", m.getId(),
                "systemId", m.getSystemId(),
                "year", m.getYear(),
                "month", m.getMonth(),
                "status", m.getStatus(),
                "hasFileKey", fileKey != null && !fileKey.isEmpty()
            );
        }).toList();
    }

    public Map<String, Object> testMinioConnection() {
        try {
            boolean bucketExists = minioService.bucketExists(bucketReports);
            return Map.of(
                "bucketReports", bucketReports,
                "bucketExists", bucketExists,
                "status", "OK"
            );
        } catch (Exception e) {
            return Map.of(
                "bucketReports", bucketReports,
                "error", e.getMessage(),
                "status", "ERROR"
            );
        }
    }
}
