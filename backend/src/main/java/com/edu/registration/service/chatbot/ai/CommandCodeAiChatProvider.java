package com.edu.registration.service.chatbot.ai;

import com.edu.registration.config.ChatbotAiProperties;
import com.edu.registration.dto.ChatbotDtos;
import com.edu.registration.service.chatbot.ChatbotSessionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gọi Command Code Provider API (OpenAI Chat Completions tương thích) để sinh câu trả lời hội thoại.
 * Endpoint mặc định: https://api.commandcode.ai/provider/v1/chat/completions
 */
@Component
public class CommandCodeAiChatProvider implements AiChatProvider {

    private static final Logger log = LoggerFactory.getLogger(CommandCodeAiChatProvider.class);
    private static final String DEFAULT_ENDPOINT = "https://api.commandcode.ai/provider/v1/chat/completions";

    private final ChatbotAiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CommandCodeAiChatProvider(ChatbotAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public ChatbotDtos.ChatbotStatusDto getStatus() {
        boolean configured = properties.isCommandCodeProvider() && properties.isCommandCodeConfigured();
        List<String> missing = new ArrayList<>();
        if (!configured) {
            missing.add("app.chatbot.ai.command-code-api-key (Biến môi trường CHATBOT_AI_COMMAND_CODE_API_KEY)");
        }
        String statusMessage = configured
                ? "Command Code Provider API đã được kết nối (model: " + properties.getModel() + ")."
                : "Command Code Provider API chưa được cấu hình API key.";
        return new ChatbotDtos.ChatbotStatusDto(
                properties.isEnabled(),
                properties.getProvider(),
                properties.getModel(),
                configured,
                missing,
                statusMessage,
                properties.getPolicyNote(),
                properties.getRateLimitPerMinute(),
                properties.getSessionTtlMinutes()
        );
    }

    @Override
    public Optional<String> generateText(String systemPrompt, String userMessage, List<ChatbotSessionManager.SessionMessage> history) {
        if (!properties.isCommandCodeProvider() || !properties.isCommandCodeConfigured()) {
            return Optional.empty();
        }

        try {
            String endpoint = (properties.getEndpoint() != null && !properties.getEndpoint().isBlank())
                    ? properties.getEndpoint()
                    : DEFAULT_ENDPOINT;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getCommandCodeApiKey());
            headers.set("User-Agent", "command-code/backend-chatbot");

            List<Map<String, Object>> messages = new ArrayList<>();
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }
            int startIdx = Math.max(0, history.size() - 10);
            for (int i = startIdx; i < history.size(); i++) {
                ChatbotSessionManager.SessionMessage msg = history.get(i);
                String role = "USER".equalsIgnoreCase(msg.sender()) ? "user" : "assistant";
                messages.add(Map.of("role", role, "content", msg.content()));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", properties.getTemperature());
            requestBody.put("max_tokens", properties.getMaxTokens());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                String text = extractContent(content);
                if (text != null && !text.isBlank()) {
                    return Optional.of(text.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi gọi Command Code Provider API: {}. Tự động fallback sang chế độ quy chuẩn an toàn.", e.getMessage());
        }

        return Optional.empty();
    }

    private String extractContent(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return null;
        }
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : content) {
                JsonNode textNode = part.path("text");
                if (!textNode.isMissingNode() && textNode.isTextual()) {
                    sb.append(textNode.asText());
                }
            }
            return sb.toString();
        }
        return null;
    }
}
