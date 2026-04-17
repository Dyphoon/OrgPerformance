package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.vo.ApiResponse;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    @GetMapping("/tools")
    public ApiResponse<List<McpToolInfo>> getTools() {
        List<McpToolInfo> tools = scanMcpTools();
        return ApiResponse.success(tools);
    }

    private List<McpToolInfo> scanMcpTools() {
        List<McpToolInfo> tools = new ArrayList<>();
        
        try {
            Class<?> mcpToolsClass = Class.forName("com.cmbchina.orgperformance.mcp.McpTools");
            Method[] methods = mcpToolsClass.getDeclaredMethods();
            
            for (Method method : methods) {
                McpTool mcpTool = method.getAnnotation(McpTool.class);
                if (mcpTool != null) {
                    McpToolInfo info = new McpToolInfo();
                    info.setName(mcpTool.name());
                    info.setDescription(mcpTool.description());
                    info.setCategory(getCategory(mcpTool.name()));
                    info.setParameters(extractParameters(method));
                    tools.add(info);
                }
            }
        } catch (ClassNotFoundException e) {
            // MCP tools class not found
        }
        
        // Sort by category, then by name
        tools.sort(Comparator.comparing(McpToolInfo::getCategory)
                .thenComparing(McpToolInfo::getName));
        return tools;
    }

    private List<ParameterInfo> extractParameters(Method method) {
        List<ParameterInfo> params = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        
        for (Parameter param : parameters) {
            org.springaicommunity.mcp.annotation.McpToolParam mcpParam = 
                param.getAnnotation(org.springaicommunity.mcp.annotation.McpToolParam.class);
            
            if (mcpParam != null) {
                ParameterInfo paramInfo = new ParameterInfo();
                paramInfo.setName(param.getName());
                paramInfo.setDescription(mcpParam.description());
                paramInfo.setRequired(mcpParam.required());
                paramInfo.setType(param.getType().getSimpleName());
                params.add(paramInfo);
            }
        }
        
        return params;
    }

    public static class McpToolInfo {
        private String name;
        private String description;
        private String category;
        private List<ParameterInfo> parameters;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<ParameterInfo> getParameters() { return parameters; }
        public void setParameters(List<ParameterInfo> parameters) { this.parameters = parameters; }
    }

    private String getCategory(String name) {
        if (name.contains("system")) return "系统管理";
        if (name.contains("monitoring")) return "监测管理";
        if (name.contains("task") && !name.contains("confirm")) return "任务管理";
        if (name.contains("confirm")) return "确认管理";
        if (name.contains("report") || name.contains("overview")) return "报表管理";
        if (name.contains("visualization") || name.contains("performance_data")) return "数据分析";
        if (name.contains("template")) return "模板管理";
        return "其他";
    }

    public static class ParameterInfo {
        private String name;
        private String description;
        private boolean required;
        private String type;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
