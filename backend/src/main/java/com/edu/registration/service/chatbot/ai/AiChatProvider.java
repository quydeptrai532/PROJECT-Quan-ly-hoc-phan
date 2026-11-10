package com.edu.registration.service.chatbot.ai;

import com.edu.registration.dto.ChatbotDtos;
import com.edu.registration.service.chatbot.ChatbotSessionManager;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction tầng giao tiếp với External AI Provider (Google Gemini, OpenAI, ...) (QLSV-106).
 */
public interface AiChatProvider {

    /**
     * Lấy thông tin trạng thái cấu hình của External AI Provider và danh sách dependency còn thiếu nếu có.
     */
    ChatbotDtos.ChatbotStatusDto getStatus();

    /**
     * Sinh phản hồi hội thoại dựa trên context nếu Provider đã được cấu hình và sẵn sàng.
     */
    Optional<String> generateText(String systemPrompt, String userMessage, List<ChatbotSessionManager.SessionMessage> history);
}
