package com.edu.registration.ratelimit;

import com.edu.registration.config.RateLimitingProperties;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ điều phối Rate Limiting trung tâm theo cấu hình (QLSV-102 / TASK-OP-07B).
 * Hỗ trợ đa nhóm điểm cuối (login, verify, chatbot, heavy, api), phân định tài khoản vs IP để tránh DoS người dùng hợp lệ.
 */
@Service
public class RateLimitService {

    private final RateLimitingProperties properties;
    private final InMemoryRateLimiter inMemoryRateLimiter;
    private final DatabaseDistributedRateLimiter databaseDistributedRateLimiter;

    public RateLimitService(RateLimitingProperties properties,
                            InMemoryRateLimiter inMemoryRateLimiter,
                            DatabaseDistributedRateLimiter databaseDistributedRateLimiter) {
        this.properties = properties;
        this.inMemoryRateLimiter = inMemoryRateLimiter;
        this.databaseDistributedRateLimiter = databaseDistributedRateLimiter;
    }

    /**
     * Kiểm tra giới hạn theo tài khoản đăng nhập (Chống Brute-force mật khẩu có chủ đích).
     */
    public RateLimitResult checkLoginAccountLimit(String username) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        if (username == null || username.isBlank()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String normalized = username.trim().toLowerCase();
        String key = "login:account:" + normalized;
        RateLimitingProperties.EndpointLimit config = properties.getLogin().getAccount();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    /**
     * Kiểm tra giới hạn theo IP gửi request đăng nhập (Chống tràn lụt DoS cổng xác thực).
     */
    public RateLimitResult checkLoginIpLimit(String clientIp) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "login:ip:" + (clientIp != null ? clientIp : "127.0.0.1");
        RateLimitingProperties.EndpointLimit config = properties.getLogin().getIp();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    /**
     * Cung cấp tương thích ngược với QLSV-94: kiểm tra login theo IP.
     */
    public RateLimitResult checkLoginLimit(String clientIp) {
        return checkLoginIpLimit(clientIp);
    }

    /**
     * Kiểm tra giới hạn cho điểm cuối tra cứu / xác thực văn bằng bảng điểm công khai (/api/public/bang-diem/xac-thuc/**).
     */
    public RateLimitResult checkVerifyLimit(String clientIp) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "verify:ip:" + (clientIp != null ? clientIp : "127.0.0.1");
        RateLimitingProperties.EndpointLimit config = properties.getVerify();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    /**
     * Kiểm tra giới hạn cho điểm cuối Chatbot AI (/api/chatbot/**).
     */
    public RateLimitResult checkChatbotLimit(String clientIdentifier) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "chatbot:" + clientIdentifier;
        RateLimitingProperties.EndpointLimit config = properties.getChatbot();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    /**
     * Kiểm tra giới hạn cho điểm cuối tính toán nặng / tốn tài nguyên CPU/RAM (xuất PDF bảng điểm, import/export CSV/Excel...).
     */
    public RateLimitResult checkHeavyLimit(String clientIdentifier) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "heavy:" + clientIdentifier;
        RateLimitingProperties.EndpointLimit config = properties.getHeavy();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    /**
     * Kiểm tra giới hạn cho các API nghiệp vụ thông thường (/api/**).
     */
    public RateLimitResult checkApiLimit(String clientIdentifier) {
        if (!isRateLimitingActive()) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "api:" + clientIdentifier;
        RateLimitingProperties.EndpointLimit config = clientIdentifier.startsWith("user:")
                ? properties.getApi()
                : properties.getApiAnonymous();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    private boolean isRateLimitingActive() {
        return properties.isEnabled() && properties.getStrategy() != RateLimitingProperties.Strategy.DISABLED;
    }

    private RateLimiter getActiveLimiter() {
        if (properties.getStrategy() == RateLimitingProperties.Strategy.DATABASE) {
            return databaseDistributedRateLimiter;
        }
        return inMemoryRateLimiter;
    }

    public RateLimitingProperties getProperties() {
        return properties;
    }
}
