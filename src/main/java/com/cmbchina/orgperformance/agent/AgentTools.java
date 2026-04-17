package com.cmbchina.orgperformance.agent;

import com.cmbchina.orgperformance.service.*;
import com.cmbchina.orgperformance.vo.*;
import io.agentscope.core.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class AgentTools {

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

    @Tool(name = "list_systems", description = "查询评估系统列表，支持按名称、状态筛选和分页")
    public List<SystemVO> listSystems(
            String name,
            Integer status,
            Integer page,
            Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int pageSizeNum = pageSize != null ? pageSize : 10;
        return systemService.getSystemList(name, status, pageNum, pageSizeNum);
    }

    @Tool(name = "get_system", description = "获取指定评估系统的详细信息")
    public SystemVO getSystem(Long id) {
        return systemService.getSystemById(id);
    }

    @Tool(name = "create_system", description = "创建新的评估系统")
    public Map<String, Object> createSystem(
            String name,
            String description,
            Boolean needApproval,
            String templateFileKey) {
        try {
            Long systemId = systemService.createSystemWithTemplate(name, description,
                    needApproval != null ? needApproval : false, templateFileKey);
            return Map.of("success", true, "systemId", systemId);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "upload_system_template", description = "上传并验证评估系统的Excel模板文件")
    public Map<String, Object> uploadSystemTemplate(String fileName, String fileContent) {
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

    @Tool(name = "parse_system_template", description = "解析并验证已上传的Excel模板文件")
    public Map<String, Object> parseSystemTemplate(String templateFileKey) {
        try {
            return systemService.parseTemplatePreview(templateFileKey);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "get_system_institutions", description = "获取指定系统下的所有机构")
    public String getSystemInstitutions(Long systemId) {
        try {
            var list = systemService.getInstitutionsBySystemId(systemId);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(name = "get_system_indicators", description = "获取指定系统下的所有指标")
    public String getSystemIndicators(Long systemId) {
        try {
            var list = systemService.getIndicatorsBySystemId(systemId);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(name = "get_system_groups", description = "获取指定系统下的所有分组名称")
    public List<String> getSystemGroups(Long systemId) {
        try {
            return systemService.getGroupNamesBySystemId(systemId);
        } catch (Exception e) {
            return List.of();
        }
    }

    @Tool(name = "validate_template", description = "验证模板数据是否符合系统格式要求")
    public Map<String, Object> validateTemplate(
            Long systemId,
            String institutionsJson,
            String indicatorsJson) {
        var system = systemService.getSystemById(systemId);
        List<String> errors = new java.util.ArrayList<>();
        if (system == null) {
            errors.add("System not found");
            return Map.of("valid", false, "errors", errors);
        }
        if (institutionsJson == null || institutionsJson.isEmpty()) {
            errors.add("No institutions provided");
        }
        if (indicatorsJson == null || indicatorsJson.isEmpty()) {
            errors.add("No indicators provided");
        }
        return Map.of("valid", errors.isEmpty(), "errors", errors);
    }

    @Tool(name = "list_monitorings", description = "查询监测任务列表，支持按系统、状态、年份、月份筛选和分页")
    public List<MonitoringVO> listMonitorings(
            Long systemId,
            String status,
            Integer year,
            Integer month,
            Integer page,
            Integer pageSize) {
        int pageNum = page != null ? page : 1;
        int pageSizeNum = pageSize != null ? pageSize : 10;
        return monitoringService.getMonitoringList(systemId, status, year, month, pageNum, pageSizeNum);
    }

    @Tool(name = "get_monitoring", description = "获取指定监测任务的详细信息")
    public MonitoringVO getMonitoring(Long id) {
        return monitoringService.getMonitoringById(id);
    }

    @Tool(name = "create_monitoring", description = "创建新的监测任务用于数据采集")
    public Map<String, Object> createMonitoring(
            Long systemId,
            Integer year,
            Integer month,
            String deadline,
            Boolean approvalRequired) {
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

    @Tool(name = "start_monitoring", description = "启动监测任务的数据采集阶段")
    public Map<String, Object> startMonitoring(Long id) {
        monitoringService.startConfirming(id);
        return Map.of("success", true, "message", "Monitoring started");
    }

    @Tool(name = "close_monitoring", description = "关闭监测任务，停止数据采集")
    public Map<String, Object> closeMonitoring(Long id) {
        monitoringService.closeMonitoring(id);
        return Map.of("success", true, "message", "Monitoring closed");
    }

    @Tool(name = "start_confirming", description = "启动监测任务的确认阶段")
    public Map<String, Object> startConfirming(Long id) {
        monitoringService.startConfirming(id);
        return Map.of("success", true, "message", "Confirmation phase started");
    }

    @Tool(name = "publish_monitoring", description = "发布监测任务（使结果可见）- 必须所有机构都确认后才能发布")
    public Map<String, Object> publishMonitoring(Long id) {
        if (!monitoringService.isAllConfirmed(id)) {
            return Map.of("success", false, "message", "Cannot publish: not all institutions have confirmed");
        }
        monitoringService.publishMonitoring(id);
        return Map.of("success", true, "message", "Monitoring published");
    }

    @Tool(name = "rollback_monitoring", description = "回滚监测任务到数据采集阶段")
    public Map<String, Object> rollbackMonitoring(Long id) {
        monitoringService.rollbackToCollecting(id);
        return Map.of("success", true, "message", "Monitoring rolled back to collecting phase");
    }

    @Tool(name = "list_tasks", description = "查询监测任务下的数据采集任务")
    public String listTasks(Long monitoringId, String status) {
        try {
            var list = dataProcessingService.getTasksByMonitoringId(monitoringId, status);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(name = "get_my_tasks", description = "获取指定采集员的任务列表")
    public String getMyTasks(Long monitoringId, Long collectorUserId) {
        try {
            var list = dataProcessingService.getTasksByCollector(monitoringId, collectorUserId);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(name = "submit_task", description = "提交数据采集任务的实际值")
    public Map<String, Object> submitTask(Long taskId, BigDecimal actualValue) {
        dataProcessingService.submitTaskData(taskId, actualValue);
        return Map.of("success", true, "message", "Task submitted");
    }

    @Tool(name = "batch_submit_tasks", description = "批量提交多个采集任务")
    public Map<String, Object> batchSubmitTasks(String updatesJson) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var updates = mapper.readValue(updatesJson, java.util.List.class);
            var requests = ((java.util.List<?>) updates).stream().map(m -> {
                var req = new com.cmbchina.orgperformance.dto.TaskUpdateRequest();
                req.setTaskId(((Number) ((java.util.Map) m).get("taskId")).longValue());
                req.setActualValue(new BigDecimal(String.valueOf(((java.util.Map) m).get("actualValue"))));
                return req;
            }).toList();
            int count = dataProcessingService.batchSubmitTasks(requests);
            return Map.of("success", true, "updated", count);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "confirm_institution", description = "确认指定机构的数据")
    public Map<String, Object> confirmInstitution(Long monitoringId, Long institutionId, String remark) {
        monitoringService.confirmInstitution(monitoringId, institutionId, 1L, remark);
        return Map.of("success", true, "message", "Institution confirmed");
    }

    @Tool(name = "get_confirmation_tasks", description = "获取监测任务的确认任务列表")
    public String getConfirmationTasks(Long monitoringId) {
        try {
            var list = monitoringService.getConfirmationTasks(monitoringId);
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Tool(name = "get_overview", description = "获取监测任务的概览统计信息")
    public Map<String, Object> getOverview(Long monitoringId) {
        return reportService.getOverview(monitoringId);
    }

    @Tool(name = "get_institution_report", description = "获取指定机构在监测任务中的详细报告")
    public Map<String, Object> getInstitutionReport(Long monitoringId, Long institutionId) {
        try {
            return reportService.getInstitutionReport(monitoringId, institutionId);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Tool(name = "generate_reports", description = "为监测任务下的所有机构生成Excel报告")
    public Map<String, Object> generateReports(Long monitoringId) {
        try {
            int count = dataProcessingService.generateInstitutionReports(monitoringId);
            return Map.of("success", true, "generatedCount", count);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "get_performance_data", description = "获取可自定义指标和维度的绩效数据")
    public Map<String, Object> getPerformanceData(
            Long monitoringId,
            List<String> dimensions,
            List<String> metrics) {
        var overview = reportService.getOverview(monitoringId);
        return Map.of(
                "monitoringId", monitoringId,
                "dimensions", dimensions != null ? dimensions : List.of("institution", "indicator"),
                "metrics", metrics != null ? metrics : List.of("actualValue", "targetValue", "completionRate"),
                "data", overview);
    }

    @Tool(name = "get_visualization_data", description = "获取用于生成可视化图表的数据")
    public Map<String, Object> getVisualizationData(
            Long monitoringId,
            String chartType,
            List<Long> institutionIds) {
        var overview = reportService.getOverview(monitoringId);
        return Map.of(
                "chartType", chartType != null ? chartType : "bar",
                "monitoringId", monitoringId,
                "data", overview);
    }

    @Tool(name = "execute_command", description = "执行 shell 命令，返回命令输出结果")
    public Map<String, Object> executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            String result = output.toString();

            return Map.of(
                    "success", exitCode == 0,
                    "exitCode", exitCode,
                    "output", result,
                    "command", command);
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "command", command);
        }
    }

    @Tool(name = "mcporter_list", description = "列出所有已配置的 MCP 服务器，或查看指定服务器的详细信息")
    public Map<String, Object> mcporterList(String serverName) {
        try {
            String cmd = serverName != null && !serverName.isEmpty()
                    ? "npx mcporter list " + serverName
                    : "npx mcporter list";
            return executeCommand(cmd);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "mcporter_call", description = "调用指定 MCP 服务器的工具，toolCall 格式：server.tool key:value key2:value2")
    public Map<String, Object> mcporterCall(String toolCall) {
        try {
            String cmd = "npx mcporter call " + toolCall;
            return executeCommand(cmd);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "mcporter_generate_cli", description = "将 MCP 服务器生成为独立的 CLI 工具")
    public Map<String, Object> mcporterGenerateCli(String serverUrl, String outputPath) {
        try {
            String cmd = "npx mcporter generate-cli --command " + serverUrl;
            if (outputPath != null && !outputPath.isEmpty()) {
                cmd += " --output " + outputPath;
            }
            return executeCommand(cmd);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    @Tool(name = "mcporter_emit_ts", description = "生成 TypeScript 类型定义文件或客户端包装器")
    public Map<String, Object> mcporterEmitTs(String server, String outputPath) {
        try {
            String cmd = "npx mcporter emit-ts " + server;
            if (outputPath != null && !outputPath.isEmpty()) {
                cmd += " --out " + outputPath;
            }
            return executeCommand(cmd);
        } catch (Exception e) {
            return Map.of("success", false, "error", e.getMessage());
        }
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
