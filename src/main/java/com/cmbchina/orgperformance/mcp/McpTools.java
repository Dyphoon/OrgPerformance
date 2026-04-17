package com.cmbchina.orgperformance.mcp;

import com.cmbchina.orgperformance.service.*;
import com.cmbchina.orgperformance.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class McpTools {

    private static final Logger logger = LoggerFactory.getLogger(McpTools.class);

    @Autowired
    private AssessmentSystemService systemService;

    @Autowired
    private TemplateValidationService templateValidationService;

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private DataProcessingService dataProcessingService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private FileContextService fileContextService;

    @McpTool(name = "list_systems", description = "查询考核体系列表，支持按名称、状态筛选和分页")
    public List<SystemVO> listSystems(
            @McpToolParam(description = "按体系名称筛选") String name,
            @McpToolParam(description = "按状态筛选（1=启用，0=停用）") Integer status,
            @McpToolParam(description = "页码（默认1）", required = false) Integer page,
            @McpToolParam(description = "每页数量（默认10）", required = false) Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int pageSizeNum = pageSize != null ? pageSize : 10;
        return systemService.getSystemList(name, status, pageNum, pageSizeNum);
    }

    @McpTool(name = "get_system", description = "获取指定考核体系的详细信息")
    public SystemVO getSystem(@McpToolParam(description = "体系ID", required = true) Long id) {
        return systemService.getSystemById(id);
    }

    @McpTool(name = "create_system", description = "创建新的考核体系")
    public Map<String, Object> createSystem(
            @McpToolParam(description = "体系名称", required = true) String name,
            @McpToolParam(description = "体系描述", required = false) String description,
            @McpToolParam(description = "是否需要审批", required = false) Boolean needApproval,
            @McpToolParam(description = "模板文件key", required = true) String templateFileKey) {
        try {
            Long systemId = systemService.createSystemWithTemplate(name, description,
                    needApproval != null ? needApproval : false, templateFileKey);
            return Map.of("success", true, "systemId", systemId);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @McpTool(name = "upload_system_template", description = "上传并验证考核体系的Excel模板文件")
    public Map<String, Object> uploadSystemTemplate(
            @McpToolParam(description = "原始文件名，需包含 .xlsx 扩展名", required = true) String fileName,
            @McpToolParam(description = "Base64编码的Excel文件内容", required = true) String fileContent) {
        try {
            byte[] content = java.util.Base64.getDecoder().decode(fileContent);
            var validationResult = templateValidationService.validate(new java.io.ByteArrayInputStream(content));
            if (!validationResult.isValid()) {
                return Map.of("success", false, "error", "Template validation failed", "details", validationResult.getErrors());
            }
            org.springframework.web.multipart.MultipartFile springFile = createMultipartFile(fileName, content);
            String fileKey = systemService.uploadTemplate(springFile);
            return Map.of("success", true, "fileKey", fileKey, "message", "Template uploaded and validated successfully");
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @McpTool(name = "parse_system_template", description = "解析并验证已上传的Excel模板文件")
    public Map<String, Object> parseSystemTemplate(
            @McpToolParam(description = "模板文件key", required = true) String templateFileKey) {
        try {
            return systemService.parseTemplatePreview(templateFileKey);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @McpTool(name = "get_system_institutions", description = "获取指定体系下的所有机构")
    public List<?> getSystemInstitutions(
            @McpToolParam(description = "体系ID", required = true) Long systemId) {
        return systemService.getInstitutionsBySystemId(systemId);
    }

    @McpTool(name = "get_system_indicators", description = "获取指定体系下的所有指标")
    public List<?> getSystemIndicators(
            @McpToolParam(description = "体系ID", required = true) Long systemId) {
        return systemService.getIndicatorsBySystemId(systemId);
    }

    @McpTool(name = "get_system_groups", description = "获取指定体系下的所有分组名称")
    public List<String> getSystemGroups(
            @McpToolParam(description = "体系ID", required = true) Long systemId) {
        try {
            return systemService.getGroupNamesBySystemId(systemId);
        } catch (Exception e) {
            logger.error("Error getting system groups", e);
            return List.of();
        }
    }

    @McpTool(name = "validate_template", description = "验证模板数据是否符合体系格式要求")
    public Map<String, Object> validateTemplate(
            @McpToolParam(description = "体系ID", required = true) Long systemId,
            @McpToolParam(description = "机构数据列表", required = true) List<Map<String, Object>> institutions,
            @McpToolParam(description = "指标数据列表", required = true) List<Map<String, Object>> indicators) {
        var system = systemService.getSystemById(systemId);
        List<String> errors = new java.util.ArrayList<>();
        if (system == null) {
            errors.add("System not found");
            return Map.of("valid", false, "errors", errors);
        }
        if (institutions == null || institutions.isEmpty()) {
            errors.add("No institutions provided");
        }
        if (indicators == null || indicators.isEmpty()) {
            errors.add("No indicators provided");
        }
        return Map.of("valid", errors.isEmpty(), "errors", errors);
    }

    @McpTool(name = "list_monitorings", description = "查询监测任务列表，支持按体系、状态、年份、月份筛选和分页")
    public List<MonitoringVO> listMonitorings(
            @McpToolParam(description = "按体系ID筛选", required = false) Long systemId,
            @McpToolParam(description = "按状态筛选", required = false) String status,
            @McpToolParam(description = "按年份筛选", required = false) Integer year,
            @McpToolParam(description = "按月份筛选（1-12）", required = false) Integer month,
            @McpToolParam(description = "页码", required = false) Integer page,
            @McpToolParam(description = "每页数量", required = false) Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int pageSizeNum = pageSize != null ? pageSize : 10;
        return monitoringService.getMonitoringList(systemId, status, year, month, pageNum, pageSizeNum);
    }

    @McpTool(name = "get_monitoring", description = "获取指定监测任务的详细信息")
    public MonitoringVO getMonitoring(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        return monitoringService.getMonitoringById(id);
    }

    @McpTool(name = "create_monitoring", description = "创建新的监测任务用于数据采集")
    public Map<String, Object> createMonitoring(
            @McpToolParam(description = "考核体系ID", required = true) Long systemId,
            @McpToolParam(description = "年份", required = true) Integer year,
            @McpToolParam(description = "月份（1-12）", required = true) Integer month,
            @McpToolParam(description = "截止日期", required = false) String deadline,
            @McpToolParam(description = "是否需要审批", required = false) Boolean approvalRequired) {
        try {
            var request = new com.cmbchina.orgperformance.dto.MonitoringCreateRequest();
            request.setSystemId(systemId);
            request.setYear(year);
            request.setMonth(month);
            request.setApprovalRequired(approvalRequired != null ? approvalRequired : false);
            if (deadline != null && !deadline.isEmpty()) {
                request.setDeadline(java.time.LocalDateTime.parse(deadline));
            }
            Long monitoringId = monitoringService.createMonitoring(request, "admin");
            return Map.of("success", true, "monitoringId", monitoringId);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @McpTool(name = "start_monitoring", description = "启动监测任务的数据采集阶段")
    public Map<String, Object> startMonitoring(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        monitoringService.startConfirming(id);
        return Map.of("success", true, "message", "Monitoring started");
    }

    @McpTool(name = "close_monitoring", description = "关闭监测任务，停止数据采集")
    public Map<String, Object> closeMonitoring(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        monitoringService.closeMonitoring(id);
        return Map.of("success", true, "message", "Monitoring closed");
    }

    @McpTool(name = "start_confirming", description = "启动监测任务的确认阶段")
    public Map<String, Object> startConfirming(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        monitoringService.startConfirming(id);
        return Map.of("success", true, "message", "Confirmation phase started");
    }

    @McpTool(name = "publish_monitoring", description = "发布监测任务（使结果可见）- 必须所有机构都确认后才能发布")
    public Map<String, Object> publishMonitoring(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        if (!monitoringService.isAllConfirmed(id)) {
            return Map.of("success", false, "message", "Cannot publish: not all institutions have confirmed");
        }
        monitoringService.publishMonitoring(id);
        return Map.of("success", true, "message", "Monitoring published");
    }

    @McpTool(name = "rollback_monitoring", description = "回滚监测任务到数据采集阶段")
    public Map<String, Object> rollbackMonitoring(@McpToolParam(description = "监测任务ID", required = true) Long id) {
        monitoringService.rollbackToCollecting(id);
        return Map.of("success", true, "message", "Monitoring rolled back to collecting phase");
    }

    @McpTool(name = "list_tasks", description = "查询监测任务下的数据采集任务")
    public List<CollectionTaskVO> listTasks(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "按任务状态筛选", required = false) String status) {
        return dataProcessingService.getTasksByMonitoringId(monitoringId, status);
    }

    @McpTool(name = "get_my_tasks", description = "获取指定采集员的任务列表")
    public List<CollectionTaskVO> getMyTasks(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "采集员用户ID", required = true) Long collectorUserId) {
        return dataProcessingService.getTasksByCollector(monitoringId, collectorUserId);
    }

    @McpTool(name = "submit_task", description = "提交数据采集任务的实际值")
    public Map<String, Object> submitTask(
            @McpToolParam(description = "任务ID", required = true) Long taskId,
            @McpToolParam(description = "实际采集值", required = true) BigDecimal actualValue) {
        dataProcessingService.submitTaskData(taskId, actualValue);
        return Map.of("success", true, "message", "Task submitted");
    }

    @McpTool(name = "batch_submit_tasks", description = "批量提交多个采集任务")
    public Map<String, Object> batchSubmitTasks(
            @McpToolParam(description = "更新列表 [{taskId, actualValue}]", required = true) List<Map<String, Object>> updates) {
        var requests = updates.stream().map(m -> {
            var req = new com.cmbchina.orgperformance.dto.TaskUpdateRequest();
            req.setTaskId(((Number) m.get("taskId")).longValue());
            req.setActualValue(new BigDecimal(m.get("actualValue").toString()));
            return req;
        }).toList();
        int count = dataProcessingService.batchSubmitTasks(requests);
        return Map.of("success", true, "updated", count);
    }

    @McpTool(name = "confirm_institution", description = "确认指定机构的数据")
    public Map<String, Object> confirmInstitution(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "机构ID", required = true) Long institutionId,
            @McpToolParam(description = "确认备注", required = false) String remark) {
        monitoringService.confirmInstitution(monitoringId, institutionId, 1L, remark);
        return Map.of("success", true, "message", "Institution confirmed");
    }

    @McpTool(name = "get_confirmation_tasks", description = "获取监测任务的确认任务列表")
    public List<?> getConfirmationTasks(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId) {
        return monitoringService.getConfirmationTasks(monitoringId);
    }

    @McpTool(name = "get_overview", description = "获取监测任务的概览统计信息")
    public Map<String, Object> getOverview(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId) {
        return reportService.getOverview(monitoringId);
    }

    @McpTool(name = "get_institution_report", description = "获取指定机构在监测任务中的详细报告")
    public Map<String, Object> getInstitutionReport(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "机构ID", required = true) Long institutionId) {
        try {
            return reportService.getInstitutionReport(monitoringId, institutionId);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @McpTool(name = "generate_reports", description = "为监测任务下的所有机构生成Excel报告")
    public Map<String, Object> generateReports(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId) {
        try {
            int count = dataProcessingService.generateInstitutionReports(monitoringId);
            return Map.of("success", true, "generatedCount", count);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @McpTool(name = "get_performance_data", description = "获取可自定义指标和维度的绩效数据")
    public Map<String, Object> getPerformanceData(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "分组维度", required = false) List<String> dimensions,
            @McpToolParam(description = "指标列表", required = false) List<String> metrics) {
        var overview = reportService.getOverview(monitoringId);
        return Map.of(
                "monitoringId", monitoringId,
                "dimensions", dimensions != null ? dimensions : List.of("institution", "indicator"),
                "metrics", metrics != null ? metrics : List.of("actualValue", "targetValue", "completionRate"),
                "data", overview);
    }

    @McpTool(name = "get_visualization_data", description = "获取用于生成可视化图表的数据")
    public Map<String, Object> getVisualizationData(
            @McpToolParam(description = "监测任务ID", required = true) Long monitoringId,
            @McpToolParam(description = "图表类型：bar, line, pie, radar", required = false) String chartType,
            @McpToolParam(description = "按机构ID列表筛选", required = false) List<Long> institutionIds) {
        var overview = reportService.getOverview(monitoringId);
        return Map.of(
                "chartType", chartType != null ? chartType : "bar",
                "monitoringId", monitoringId,
                "data", overview);
    }

    private org.springframework.web.multipart.MultipartFile createMultipartFile(String filename, byte[] content) {
        return new org.springframework.web.multipart.MultipartFile() {
            @Override public String getName() { return "file"; }
            @Override public String getOriginalFilename() { return filename; }
            @Override public String getContentType() { return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"; }
            @Override public boolean isEmpty() { return content == null || content.length == 0; }
            @Override public long getSize() { return content.length; }
            @Override public byte[] getBytes() { return content; }
            @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(content); }
            @Override public void transferTo(java.io.File dest) throws java.io.IOException {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(dest)) { fos.write(content); }
            }
        };
    }
}
