package com.cmbchina.orgperformance.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request, @AuthenticationPrincipal UserDetails user) {
        String sessionId = request.getSessionId() != null ? request.getSessionId() : "default";
        String message = request.getMessage();
        String userId = "0";
        if (user != null && user.getUsername() != null) {
            String username = user.getUsername();
            userId = username.contains(",") ? username.split(",")[0] : username;
        }
        String templateFileKey = request.getTemplateFileKey();
        Long modelProviderId = request.getModelProviderId();

        try {
            AgentService.AgentResponse response = agentService.chat(sessionId, userId, message, templateFileKey, modelProviderId);

            if (response.isSuccess()) {
                return Map.of("success", true, "sessionId", sessionId, "message", response.getContent());
            } else {
                return Map.of("success", false, "sessionId", sessionId, "error", response.getError());
            }
        } catch (Exception e) {
            return Map.of("success", false, "sessionId", sessionId, "error", e.getMessage());
        }
    }

    @PostMapping("/clear")
    public Map<String, Object> clearSession(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        if (sessionId != null) {
            agentService.clearSession(sessionId);
        }
        return Map.of("success", true);
    }

    public static class ChatRequest {
        private String sessionId;
        private String message;
        private String templateFileKey;
        private Long modelProviderId;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTemplateFileKey() { return templateFileKey; }
        public void setTemplateFileKey(String templateFileKey) { this.templateFileKey = templateFileKey; }
        public Long getModelProviderId() { return modelProviderId; }
        public void setModelProviderId(Long modelProviderId) { this.modelProviderId = modelProviderId; }
    }
}
