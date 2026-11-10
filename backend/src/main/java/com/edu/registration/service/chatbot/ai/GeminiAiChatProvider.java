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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Triển khai giao tiếp với Google Gemini API theo abstraction an toàn (QLSV-106 / TASK-AI-01).
 * Nếu chưa cấu hình API Key, không giả vờ AI hoạt động mà báo rõ dependency còn thiếu.
 */
@Component
public class GeminiAiChatProvider implements AiChatProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiChatProvider.class);

    private final ChatbotAiProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAiChatProvider(ChatbotAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ChatbotDtos.ChatbotStatusDto getStatus() {
        boolean configured = properties.isProviderConfigured();
        List<String> missing = new ArrayList<>();
        if (!configured) {
            missing.add("app.chatbot.ai.api-key (Biến môi trường CHATBOT_AI_API_KEY)");
        }

        String statusMessage = configured
                ? "AI Provider (" + properties.getProvider() + " - model: " + properties.getModel() + ") đã được kết nối."
                : "AI Provider chưa được cấu hình API Key. Trợ lý sử dụng động cơ phân tích quy chuẩn an toàn từ CSDL thời gian thực.";

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
        if (!properties.isProviderConfigured()) {
            return Optional.empty();
        }

        try {
            String baseUrl = (properties.getEndpoint() != null && !properties.getEndpoint().isBlank())
                    ? properties.getEndpoint()
                    : "https://generativelanguage.googleapis.com/v1beta/models/" + properties.getModel() + ":generateContent?key=" + properties.getApiKey();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, Object>> contents = new ArrayList<>();

            // Đưa lịch sử hội thoại vào context (giới hạn 6 tin gần nhất để tiết kiệm token và chi phí)
            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                ChatbotSessionManager.SessionMessage msg = history.get(i);
                String role = "USER".equalsIgnoreCase(msg.sender()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.content()))
                ));
            }

            contents.add(Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userMessage))
            ));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", contents);

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                requestBody.put("systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ));
            }

            requestBody.put("generationConfig", Map.of(
                    "temperature", properties.getTemperature(),
                    "maxOutputTokens", properties.getMaxTokens()
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(baseUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                    if (!textNode.isMissingNode()) {
                        return Optional.of(textNode.asText());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi gọi external AI provider (Gemini): {}. Tự động fallback sang chế độ quy chuẩn an toàn.", e.getMessage());
        }

        return Optional.empty();
    }
}
