package com.cmbchina.orgperformance.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    private String name = "OrgPerformance Assistant";
    private String sysPrompt = """
        你是一个专业的组织绩效管理系统助手，帮助用户完成以下操作：
        1. 创建考核体系 - 通过upload_system_template和create_system工具
        2. 发起监测任务 - 通过create_monitoring工具
        3. 录入和收集数据 - 通过submit_task和batch_submit_tasks工具
        4. 获取分析报告 - 通过get_overview、get_institution_report等工具
        
        重要规则：
        - 当用户询问"有多少绩效体系"、"有哪些体系"、"列出所有体系"时，必须调用 list_systems 工具
        - 当用户询问某个具体体系的详情时，调用 get_system 工具
        - 当用户询问监测任务时，调用 list_monitorings 或 get_monitoring 工具
        请根据用户需求，调用相应工具完成任务。如果用户请求不明确，请询问具体信息。
        
        重要提示：
        - 所有金额单位为元
        - 日期格式为YYYY-MM-DD
        - 在执行操作前，先查询相关信息（如体系列表、监测任务列表等）确认目标
        """;

    private String baseUrl = "https://api.minimaxi.com/v1";
    
    private String apiKey;
    
    private String modelName = "MiniMax-M2.7";

    private int maxTokens = 196608;

    private double temperature = 0.7;

    private int conversationTimeoutSeconds = 120;

    private String modelProviderCode = "minimax";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSysPrompt() {
        return sysPrompt;
    }

    public void setSysPrompt(String sysPrompt) {
        this.sysPrompt = sysPrompt;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getConversationTimeoutSeconds() {
        return conversationTimeoutSeconds;
    }

    public void setConversationTimeoutSeconds(int conversationTimeoutSeconds) {
        this.conversationTimeoutSeconds = conversationTimeoutSeconds;
    }

    public String getModelProviderCode() {
        return modelProviderCode;
    }

    public void setModelProviderCode(String modelProviderCode) {
        this.modelProviderCode = modelProviderCode;
    }
}
