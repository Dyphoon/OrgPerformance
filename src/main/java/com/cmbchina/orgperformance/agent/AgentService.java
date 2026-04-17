package com.cmbchina.orgperformance.agent;

import com.cmbchina.orgperformance.entity.Skill;
import com.cmbchina.orgperformance.service.FileContextService;
import com.cmbchina.orgperformance.service.SkillService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentService {

    private static final Logger logger = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    private AgentConfig agentConfig;

    @Autowired
    private AgentTools agentTools;

    @Autowired
    private SkillService skillService;

    @Autowired
    private FileContextService fileContextService;

    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();
    private final Map<String, List<Msg>> sessionHistories = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserIds = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 30;

    @PostConstruct
    public void init() {
        logger.info("Initializing AgentService with model: {}, baseUrl: {}", 
            agentConfig.getModelName(), agentConfig.getBaseUrl());
    }

    private ReActAgent createAgent(String sessionId, Long userId) {
        logger.info("Creating new agent for session: {}, userId: {}", sessionId, userId);

        GenerateOptions generateOptions = GenerateOptions.builder()
                .maxTokens(agentConfig.getMaxTokens())
                .temperature(agentConfig.getTemperature())
                .build();

        OpenAIChatModel model = OpenAIChatModel.builder()
                .baseUrl(agentConfig.getBaseUrl())
                .apiKey(agentConfig.getApiKey())
                .modelName(agentConfig.getModelName())
                .generateOptions(generateOptions)
                .build();

        logger.info("Model created with baseUrl: {}, modelName: {}", 
            agentConfig.getBaseUrl(), agentConfig.getModelName());

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(agentTools);
        
        logger.info("Tools registered: {}, count: {}", toolkit.getToolNames(), toolkit.getToolNames().size());
        logger.info("Tool schemas: {}", toolkit.getToolSchemas().size());

        String sysPrompt = buildSystemPrompt(userId);

        ReActAgent agent = ReActAgent.builder()
                .name(agentConfig.getName())
                .sysPrompt(sysPrompt)
                .model(model)
                .toolkit(toolkit)
                .maxIters(20)
                .build();

        logger.info("Agent created successfully for session: {}", sessionId);
        return agent;
    }

    private String buildSystemPrompt(Long userId) {
        String basePrompt = agentConfig.getSysPrompt();
        
        if (userId == null || userId == 0) {
            return basePrompt;
        }

        try {
            List<Skill> installedSkills = skillService.getInstalledSkills(userId);
            if (installedSkills.isEmpty()) {
                return basePrompt;
            }

            StringBuilder skillSection = new StringBuilder();
            skillSection.append("\n\n【已激活的技能】\n");
            for (Skill skill : installedSkills) {
                skillSection.append(String.format("\n【%s】%s\n%s\n", 
                    skill.getName(), 
                    skill.getDescription(),
                    skill.getPrompt()));
            }
            
            return basePrompt + skillSection.toString();
        } catch (Exception e) {
            logger.warn("Failed to load user skills: {}", e.getMessage());
            return basePrompt;
        }
    }

    public AgentResponse chat(String sessionId, String userId, String userMessage) {
        return chat(sessionId, userId, userMessage, null);
    }

    public AgentResponse chat(String sessionId, String userId, String userMessage, String templateFileKey) {
        logger.info("Processing chat request for session: {}, userId: {}, message: {}, templateFileKey: {}", 
            sessionId, userId, userMessage, templateFileKey);
        
        try {
            Long parsedUserId = 0L;
            try {
                if (userId != null && !userId.isEmpty()) {
                    parsedUserId = Long.parseLong(userId);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid userId format: {}", userId);
            }

            ReActAgent agent;
            boolean isNewAgent = false;
            if (!sessionAgents.containsKey(sessionId)) {
                agent = createAgent(sessionId, parsedUserId);
                sessionAgents.put(sessionId, agent);
                sessionHistories.put(sessionId, new ArrayList<>());
                sessionUserIds.put(sessionId, parsedUserId);
                isNewAgent = true;
            } else {
                Long currentUserId = sessionUserIds.get(sessionId);
                if (currentUserId == null || !currentUserId.equals(parsedUserId)) {
                    agent = createAgent(sessionId, parsedUserId);
                    sessionAgents.put(sessionId, agent);
                    sessionHistories.put(sessionId, new ArrayList<>());
                    sessionUserIds.put(sessionId, parsedUserId);
                    isNewAgent = true;
                } else {
                    agent = sessionAgents.get(sessionId);
                }
            }
            
            List<Msg> history = sessionHistories.computeIfAbsent(sessionId, k -> new ArrayList<>());

            String fileContext = fileContextService.buildFileContext(sessionId);
            
            StringBuilder fullMessageBuilder = new StringBuilder(userMessage);
            if (fileContext != null && !fileContext.isEmpty()) {
                fullMessageBuilder.append(fileContext);
            }
            
            if (templateFileKey != null && !templateFileKey.isEmpty()) {
                fullMessageBuilder.append("\n\n【模板文件信息】\n");
                fullMessageBuilder.append("用户已上传了评估体系模板文件，该模板已通过验证。\n");
                fullMessageBuilder.append("如需使用此模板创建评估体系，请调用 create_system 工具，参数 templateFileKey=\"").append(templateFileKey).append("\"\n");
            }
            
            String fullMessage = fullMessageBuilder.toString();

            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(fullMessage)
                    .build();

            history.add(userMsg);
            logger.info("User message added to history, history size: {}, fileContext length: {}", 
                history.size(), fileContext != null ? fileContext.length() : 0);

            logger.info("Calling agent...");
            Msg response = agent.call(userMsg).block();
            logger.info("Agent call completed");

            if (response != null) {
                history.add(response);
                
                while (history.size() > MAX_HISTORY_SIZE) {
                    history.remove(0);
                }

                String content = response.getTextContent();
                logger.info("Agent response: {}", content);
                
                return new AgentResponse(true, content, null);
            } else {
                logger.warn("Empty response from agent");
                return new AgentResponse(false, null, "Empty response from agent");
            }
        } catch (Exception e) {
            logger.error("Error processing chat: {}", e.getMessage(), e);
            return new AgentResponse(false, null, e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        sessionAgents.remove(sessionId);
        sessionHistories.remove(sessionId);
        sessionUserIds.remove(sessionId);
        fileContextService.clearSessionFiles(sessionId);
        logger.info("Cleared session: {}", sessionId);
    }

    public static class AgentResponse {
        private final boolean success;
        private final String content;
        private final String error;

        public AgentResponse(boolean success, String content, String error) {
            this.success = success;
            this.content = content;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public String getContent() { return content; }
        public String getError() { return error; }
    }
}
