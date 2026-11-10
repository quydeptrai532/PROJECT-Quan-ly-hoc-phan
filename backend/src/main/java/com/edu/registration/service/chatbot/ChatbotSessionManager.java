package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotAiProperties;
import com.edu.registration.dto.ChatbotDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý phiên hội thoại và ngữ cảnh Chatbot với chính sách TTL tự dọn dẹp và giới hạn lịch sử (QLSV-106).
 */
@Component
public class ChatbotSessionManager {

    private static final Logger log = LoggerFactory.getLogger(ChatbotSessionManager.class);

    private final ChatbotAiProperties properties;
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public ChatbotSessionManager(ChatbotAiProperties properties) {
        this.properties = properties;
    }

    public static class UserSession {
        private final String sessionId;
        private final String username;
        private final String role;
        private Long currentHocKyId;
        private String currentHocKyTen;
        private Long currentMonHocId;
        private String currentMonHocMa;
        private String currentMonHocTen;
        private Long currentLopHocPhanId;
        private String currentLopHocPhanMa;
        private List<ChatbotDtos.ClassCardDto> candidateClasses = new ArrayList<>();
        private ChatbotDtos.ClassCardDto pendingRegistrationClass;
        private boolean awaitingConfirmation = false;
        private final List<SessionMessage> messages = new ArrayList<>();
        private LocalDateTime lastActive;

        public UserSession(String sessionId, String username, String role) {
            this.sessionId = sessionId;
            this.username = username;
            this.role = role;
            this.lastActive = LocalDateTime.now();
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public Long getCurrentHocKyId() {
            return currentHocKyId;
        }

        public void setCurrentHocKyId(Long currentHocKyId) {
            this.currentHocKyId = currentHocKyId;
        }

        public String getCurrentHocKyTen() {
            return currentHocKyTen;
        }

        public void setCurrentHocKyTen(String currentHocKyTen) {
            this.currentHocKyTen = currentHocKyTen;
        }

        public Long getCurrentMonHocId() {
            return currentMonHocId;
        }

        public void setCurrentMonHocId(Long currentMonHocId) {
            this.currentMonHocId = currentMonHocId;
        }

        public String getCurrentMonHocMa() {
            return currentMonHocMa;
        }

        public void setCurrentMonHocMa(String currentMonHocMa) {
            this.currentMonHocMa = currentMonHocMa;
        }

        public String getCurrentMonHocTen() {
            return currentMonHocTen;
        }

        public void setCurrentMonHocTen(String currentMonHocTen) {
            this.currentMonHocTen = currentMonHocTen;
        }

        public Long getCurrentLopHocPhanId() {
            return currentLopHocPhanId;
        }

        public void setCurrentLopHocPhanId(Long currentLopHocPhanId) {
            this.currentLopHocPhanId = currentLopHocPhanId;
        }

        public String getCurrentLopHocPhanMa() {
            return currentLopHocPhanMa;
        }

        public void setCurrentLopHocPhanMa(String currentLopHocPhanMa) {
            this.currentLopHocPhanMa = currentLopHocPhanMa;
        }

        public List<ChatbotDtos.ClassCardDto> getCandidateClasses() {
            return candidateClasses;
        }

        public void setCandidateClasses(List<ChatbotDtos.ClassCardDto> candidateClasses) {
            this.candidateClasses = candidateClasses != null ? candidateClasses : new ArrayList<>();
        }

        public ChatbotDtos.ClassCardDto getPendingRegistrationClass() {
            return pendingRegistrationClass;
        }

        public void setPendingRegistrationClass(ChatbotDtos.ClassCardDto pendingRegistrationClass) {
            this.pendingRegistrationClass = pendingRegistrationClass;
            this.awaitingConfirmation = (pendingRegistrationClass != null);
        }

        public boolean isAwaitingConfirmation() {
            return awaitingConfirmation;
        }

        public void clearPendingRegistration() {
            this.pendingRegistrationClass = null;
            this.awaitingConfirmation = false;
        }

        public List<SessionMessage> getMessages() {
            return new ArrayList<>(messages);
        }

        public synchronized void addMessage(String sender, String text, int maxHistory) {
            messages.add(new SessionMessage(UUID.randomUUID().toString(), sender, text, LocalDateTime.now()));
            while (messages.size() > maxHistory) {
                messages.remove(0);
            }
            this.lastActive = LocalDateTime.now();
        }

        public LocalDateTime getLastActive() {
            return lastActive;
        }

        public void touch() {
            this.lastActive = LocalDateTime.now();
        }
    }

    public record SessionMessage(String id, String sender, String content, LocalDateTime timestamp) {
    }

    public UserSession getOrCreateSession(String sessionId, String username, String role) {
        String key = (sessionId != null && !sessionId.isBlank()) ? sessionId : "user:" + username;
        return sessions.compute(key, (k, existing) -> {
            if (existing == null) {
                return new UserSession(key, username, role);
            }
            existing.touch();
            return existing;
        });
    }

    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public void resetUserSession(String username) {
        sessions.remove("user:" + username);
        sessions.entrySet().removeIf(e -> username.equals(e.getValue().getUsername()));
    }

    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupExpiredSessions() {
        int ttlMinutes = properties.getSessionTtlMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(ttlMinutes);
        int initialSize = sessions.size();
        sessions.entrySet().removeIf(entry -> entry.getValue().getLastActive().isBefore(cutoff));
        int removed = initialSize - sessions.size();
        if (removed > 0) {
            log.info("ChatbotSessionManager dọn dẹp {} phiên hội thoại hết hạn sau {} phút không hoạt động", removed, ttlMinutes);
        }
    }
}
