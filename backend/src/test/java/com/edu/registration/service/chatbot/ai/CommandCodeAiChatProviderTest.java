package com.edu.registration.service.chatbot.ai;

import com.edu.registration.config.ChatbotAiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCodeAiChatProviderTest {

    @Test
    @DisplayName("Gọi đúng endpoint OpenAI-compatible và đọc nội dung trả lời")
    void generateTextParsesOpenAiCompatibleResponse() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"Xin chào từ Command Code\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            ChatbotAiProperties properties = new ChatbotAiProperties();
            properties.setProvider("COMMAND_CODE");
            properties.setCommandCodeApiKey("test-key");
            properties.setModel("stealth/crouton-alpha");
            properties.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions");
            properties.setTemperature(0.3);
            properties.setMaxTokens(200);

            CommandCodeAiChatProvider provider = new CommandCodeAiChatProvider(properties, new ObjectMapper());
            Optional<String> result = provider.generateText("system prompt", "Xin chào", List.of());

            assertTrue(result.isPresent());
            assertEquals("Xin chào từ Command Code", result.get());
            assertEquals("Bearer test-key", authorization.get());
            assertTrue(requestBody.get().contains("stealth/crouton-alpha"));
            assertTrue(requestBody.get().contains("system prompt"));
            assertTrue(requestBody.get().contains("Xin chào"));
            assertTrue(provider.getStatus().configured());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Không gọi Command Code khi provider đang là GEMINI")
    void generateTextReturnsEmptyWhenProviderIsGemini() {
        ChatbotAiProperties properties = new ChatbotAiProperties();
        properties.setProvider("GEMINI");
        properties.setCommandCodeApiKey("test-key");

        CommandCodeAiChatProvider provider = new CommandCodeAiChatProvider(properties, new ObjectMapper());

        assertTrue(provider.generateText("system", "user", List.of()).isEmpty());
        assertFalse(provider.getStatus().configured());
    }
}
