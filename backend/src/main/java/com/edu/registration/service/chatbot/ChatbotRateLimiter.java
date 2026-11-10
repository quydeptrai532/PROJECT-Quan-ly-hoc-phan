package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotAiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn tần suất gửi tin nhắn tới Chatbot và bảo vệ chi phí gọi External AI Provider (QLSV-106).
 */
@Component
public class ChatbotRateLimiter {

    private final ChatbotAiProperties properties;
    private final Map<String, Deque<Long>> userRequestTimestamps = new ConcurrentHashMap<>();

    public ChatbotRateLimiter(ChatbotAiProperties properties) {
        this.properties = properties;
    }

    public synchronized boolean isAllowed(String key) {
        int maxRequests = properties.getRateLimitPerMinute();
        if (maxRequests <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        Deque<Long> timestamps = userRequestTimestamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            return false;
        }

        timestamps.addLast(now);
        return true;
    }

    public void clear(String key) {
        userRequestTimestamps.remove(key);
    }
}
