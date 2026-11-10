package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình Chatbot AI tư vấn và hỗ trợ đăng ký học phần (QLSV-106 / TASK-AI-01).
 */
@Component
@ConfigurationProperties(prefix = "app.chatbot.ai")
public class ChatbotAiProperties {

    private boolean enabled = true;
    private String provider = "GEMINI"; // GEMINI, COMMAND_CODE, NONE
    private String apiKey = "";
    private String commandCodeApiKey = "";
    private String model = "gemini-3.5-flash-lite";
    private String endpoint = "";
    private double temperature = 0.2;
    private int maxTokens = 1000;
    private int rateLimitPerMinute = 30;
    private int sessionTtlMinutes = 60;
    private int maxHistoryMessages = 20;
    private String policyNote = "Chatbot chỉ là giao diện hội thoại; dữ liệu lớp và đăng ký do API nghiệp vụ có phân quyền quyết định. " +
            "Khi AI Provider chưa cấu hình API key, hệ thống hoạt động ở chế độ quy chuẩn an toàn từ CSDL đào tạo.";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getSessionTtlMinutes() {
        return sessionTtlMinutes;
    }

    public void setSessionTtlMinutes(int sessionTtlMinutes) {
        this.sessionTtlMinutes = sessionTtlMinutes;
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public void setPolicyNote(String policyNote) {
        this.policyNote = policyNote;
    }

    public String getCommandCodeApiKey() {
        return commandCodeApiKey;
    }

    public void setCommandCodeApiKey(String commandCodeApiKey) {
        this.commandCodeApiKey = commandCodeApiKey;
    }

    public boolean isCommandCodeProvider() {
        return provider != null
                && ("COMMAND_CODE".equalsIgnoreCase(provider) || "COMMANDCODE".equalsIgnoreCase(provider));
    }

    public boolean isGeminiProvider() {
        return !isCommandCodeProvider() && !"NONE".equalsIgnoreCase(provider);
    }

    public boolean isGeminiConfigured() {
        return apiKey != null && !apiKey.trim().isBlank();
    }

    public boolean isCommandCodeConfigured() {
        return commandCodeApiKey != null && !commandCodeApiKey.trim().isBlank();
    }

    public boolean isProviderConfigured() {
        if ("NONE".equalsIgnoreCase(provider)) {
            return false;
        }
        return isCommandCodeProvider() ? isCommandCodeConfigured() : isGeminiConfigured();
    }
}
