package com.cmbchina.orgperformance.agent;

import com.cmbchina.orgperformance.entity.ModelProvider;
import com.cmbchina.orgperformance.entity.Skill;
import com.cmbchina.orgperformance.service.FileContextService;
import com.cmbchina.orgperformance.service.ModelProviderService;
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
import java.time.Duration;
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

    @Autowired
    private ModelProviderService modelProviderService;

    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();
    private final Map<String, List<Msg>> sessionHistories = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionUserIds = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionModelProviderIds = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 30;

    @PostConstruct
    public void init() {
        logger.info("Initializing AgentService with model: {}, baseUrl: {}",
            agentConfig.getModelName(), agentConfig.getBaseUrl());
    }

    private ReActAgent createAgent(String sessionId, Long userId, Long modelProviderId) {
        logger.info("Creating new agent for session: {}, userId: {}, modelProviderId: {}", sessionId, userId, modelProviderId);

        ModelProvider provider = null;
        if (modelProviderId != null) {
            provider = modelProviderService.getById(modelProviderId);
        }
        if (provider == null) {
            List<ModelProvider> activeProviders = modelProviderService.getAllActive();
            if (!activeProviders.isEmpty()) {
                provider = activeProviders.get(0);
            }
        }
        if (provider == null) {
            provider = new ModelProvider();
            provider.setBaseUrl(agentConfig.getBaseUrl());
            provider.setApiKey(agentConfig.getApiKey());
            String configModelName = agentConfig.getModelName();
            provider.setModelName(configModelName != null && !configModelName.isEmpty() ? configModelName : "MiniMax-M2.7");
            provider.setMaxTokens(4096);
            provider.setTemperature(0.7);
            logger.warn("No active model provider found, using config defaults");
        } else {
            logger.info("Using model provider: {}, model: {}", provider.getName(), provider.getModelName());
        }

        String baseUrl = provider.getBaseUrl();
        String apiKey = provider.getApiKey();
        String modelName = provider.getModelName();
        Integer maxTokens = provider.getMaxTokens() != null ? provider.getMaxTokens() : 4096;
        Double temperature = provider.getTemperature() != null ? provider.getTemperature() : 0.7;

        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = agentConfig.getBaseUrl();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = agentConfig.getApiKey();
        }
        if (modelName == null || modelName.isEmpty()) {
            modelName = agentConfig.getModelName() != null ? agentConfig.getModelName() : "MiniMax-M2.7";
        }

        GenerateOptions generateOptions = GenerateOptions.builder()
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();

        OpenAIChatModel model = OpenAIChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .generateOptions(generateOptions)
                .build();

        logger.info("Model created with baseUrl: {}, modelName: {}",
            baseUrl, modelName);

        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(agentTools);
        
        logger.info("Tools registered: {}, count: {}", toolkit.getToolNames(), toolkit.getToolNames().size());
        logger.info("Tool schemas: {}", toolkit.getToolSchemas().size());
        if (!toolkit.getToolSchemas().isEmpty()) {
            logger.info("First tool schema: {}", toolkit.getToolSchemas().get(0));
        }

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
        return chat(sessionId, userId, userMessage, null, null);
    }

    public AgentResponse chat(String sessionId, String userId, String userMessage, String templateFileKey) {
        return chat(sessionId, userId, userMessage, templateFileKey, null);
    }

    public AgentResponse chat(String sessionId, String userId, String userMessage, String templateFileKey, Long modelProviderId) {
        logger.info("========== Processing chat request ==========");
        logger.info("sessionId: {}, userId: {}, message: {}, templateFileKey: {}, modelProviderId: {}",
            sessionId, userId, userMessage, templateFileKey, modelProviderId);

        try {
            Long parsedUserId = 0L;
            try {
                if (userId != null && !userId.isEmpty()) {
                    parsedUserId = Long.parseLong(userId);
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid userId format: {}, defaulting to 0", userId);
            }
            logger.info("parsedUserId: {}", parsedUserId);

            ReActAgent agent;
            boolean isNewAgent = false;
            logger.info("Checking if agent exists for sessionId: {}", sessionId);
            logger.info("sessionAgents.containsKey: {}", sessionAgents.containsKey(sessionId));

            if (!sessionAgents.containsKey(sessionId) ||
                !Objects.equals(sessionModelProviderIds.get(sessionId), modelProviderId)) {
                logger.info("Creating new agent for session: {}", sessionId);
                agent = createAgent(sessionId, parsedUserId, modelProviderId);
                sessionAgents.put(sessionId, agent);
                sessionHistories.put(sessionId, new ArrayList<>());
                sessionUserIds.put(sessionId, parsedUserId);
                if (modelProviderId != null) {
                    sessionModelProviderIds.put(sessionId, modelProviderId);
                }
                isNewAgent = true;
            } else {
                Long currentUserId = sessionUserIds.get(sessionId);
                if (currentUserId == null || !currentUserId.equals(parsedUserId)) {
                    logger.info("User changed, creating new agent");
                    agent = createAgent(sessionId, parsedUserId, modelProviderId);
                    sessionAgents.put(sessionId, agent);
                    sessionHistories.put(sessionId, new ArrayList<>());
                    sessionUserIds.put(sessionId, parsedUserId);
                    if (modelProviderId != null) {
                        sessionModelProviderIds.put(sessionId, modelProviderId);
                    }
                    isNewAgent = true;
                } else {
                    logger.info("Reusing existing agent");
                    agent = sessionAgents.get(sessionId);
                }
            }
            
            logger.info("Agent created successfully for session: {}", sessionId);
            logger.info("sessionAgents size: {}, sessionHistories size: {}", sessionAgents.size(), sessionHistories.size());
            
            logger.info("Step 1: Getting history for sessionId: {}", sessionId);
            List<Msg> history = sessionHistories.get(sessionId);
            if (history == null) {
                history = new ArrayList<>();
                sessionHistories.put(sessionId, history);
            }
            logger.info("Step 2: History size: {}", history.size());

            logger.info("Step 3: Building file context...");
            String fileContext = fileContextService.buildFileContext(sessionId);
            logger.info("Step 4: File context length: {}", fileContext != null ? fileContext.length() : 0);
            
            StringBuilder fullMessageBuilder = new StringBuilder(userMessage);
            if (fileContext != null && !fileContext.isEmpty()) {
                fullMessageBuilder.append(fileContext);
            }
            
            if (templateFileKey != null && !templateFileKey.isEmpty()) {
                fullMessageBuilder.append("\n\n【模板文件信息】\n");
                fullMessageBuilder.append("用户已上传了考核体系模板文件，该模板已通过验证。\n");
                fullMessageBuilder.append("如需使用此模板创建考核体系，请调用 create_system 工具，参数 templateFileKey=\"").append(templateFileKey).append("\"\n");
            }
            
            String fullMessage = fullMessageBuilder.toString();
            logger.info("Step 5: Full message length: {}", fullMessage.length());

            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(fullMessage)
                    .build();
            logger.info("Step 6: UserMsg created");

            history.add(userMsg);
            logger.info("Step 7: Message added to history, history size: {}", history.size());

            logger.info("Step 8: About to call agent.chat()...");
            Msg response = null;
            Exception agentException = null;
            try {
                logger.info("Calling agent.call()...");
                response = agent.call(userMsg).block(Duration.ofSeconds(agentConfig.getConversationTimeoutSeconds()));
                logger.info("Agent call completed, response: {}", response);
            } catch (Exception e) {
                logger.error("Agent call failed with exception: {}", e.getClass().getName(), e);
                agentException = e;
            }
            
            if (agentException != null) {
                return new AgentResponse(false, null, "AI服务调用失败: " + agentException.getMessage());
            }
            logger.info("Agent call succeeded");

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
            logger.error("Error processing chat: {} | Exception class: {} | Stack trace:",
                e.getMessage(), e.getClass().getName(), e);
            return new AgentResponse(false, null, e.getMessage());
        }
    }

    public void clearSession(String sessionId) {
        sessionAgents.remove(sessionId);
        sessionHistories.remove(sessionId);
        sessionUserIds.remove(sessionId);
        sessionModelProviderIds.remove(sessionId);
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
