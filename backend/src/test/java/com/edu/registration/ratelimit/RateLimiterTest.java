package com.edu.registration.ratelimit;

import com.edu.registration.config.RateLimitingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private InMemoryRateLimiter inMemoryRateLimiter;
    private RateLimitingProperties properties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        inMemoryRateLimiter = new InMemoryRateLimiter();
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setStrategy(RateLimitingProperties.Strategy.IN_MEMORY);

        RateLimitingProperties.LoginLimits loginLimits = new RateLimitingProperties.LoginLimits();
        loginLimits.setIp(new RateLimitingProperties.EndpointLimit(5, 60));
        loginLimits.setAccount(new RateLimitingProperties.EndpointLimit(3, 60));
        properties.setLogin(loginLimits);

        properties.setVerify(new RateLimitingProperties.EndpointLimit(4, 60));
        properties.setChatbot(new RateLimitingProperties.EndpointLimit(3, 60));
        properties.setHeavy(new RateLimitingProperties.EndpointLimit(2, 60));
        properties.setApi(new RateLimitingProperties.EndpointLimit(10, 60));
        properties.setApiAnonymous(new RateLimitingProperties.EndpointLimit(5, 60));

        rateLimitService = new RateLimitService(properties, inMemoryRateLimiter, null);
    }

    @Test
    @DisplayName("Cho phép các yêu cầu trong hạn ngạch theo IP")
    void shouldAllowRequestsWithinCapacity() {
        String clientIp = "192.168.1.100";

        RateLimitResult r1 = rateLimitService.checkLoginIpLimit(clientIp);
        assertTrue(r1.allowed());
        assertEquals(4, r1.remaining());

        RateLimitResult r2 = rateLimitService.checkLoginIpLimit(clientIp);
        assertTrue(r2.allowed());
        assertEquals(3, r2.remaining());
    }

    @Test
    @DisplayName("Chặn khi vượt quá hạn ngạch và trả về thời gian chờ Retry-After hợp lệ")
    void shouldRejectWhenExceedingCapacityAndProvideRetryAfter() {
        String username = "student_01";

        rateLimitService.checkLoginAccountLimit(username); // 1
        rateLimitService.checkLoginAccountLimit(username); // 2
        rateLimitService.checkLoginAccountLimit(username); // 3

        RateLimitResult r4 = rateLimitService.checkLoginAccountLimit(username);
        assertFalse(r4.allowed());
        assertEquals(0, r4.remaining());
        assertTrue(r4.retryAfterSeconds() > 0 && r4.retryAfterSeconds() <= 60);
    }

    @Test
    @DisplayName("IP dùng chung (NAT/Ký túc xá) không bị DoS toàn hệ thống khi 1 tài khoản bị brute-force")
    void shouldNotLockEntireSharedIpWhenSingleAccountIsExceeded() {
        String sharedCampusIp = "203.0.113.195";
        String attackerTargetUser = "target_victim";
        String legitimateStudent = "legitimate_student";

        // Kẻ tấn công thử mật khẩu liên tục cho target_victim
        rateLimitService.checkLoginAccountLimit(attackerTargetUser);
        rateLimitService.checkLoginAccountLimit(attackerTargetUser);
        rateLimitService.checkLoginAccountLimit(attackerTargetUser);

        // Tài khoản target_victim bị giới hạn (429)
        RateLimitResult blockedAccount = rateLimitService.checkLoginAccountLimit(attackerTargetUser);
        assertFalse(blockedAccount.allowed());

        // Sinh viên hợp lệ khác từ CÙNG địa chỉ IP dùng chung vẫn đăng nhập bình thường!
        RateLimitResult normalStudent = rateLimitService.checkLoginAccountLimit(legitimateStudent);
        assertTrue(normalStudent.allowed(), "Sinh viên hợp lệ không được phép bị khóa khi dùng chung IP với tài khoản bị tấn công");

        // IP dùng chung vẫn trong ngưỡng 5 req/min
        RateLimitResult ipCheck = rateLimitService.checkLoginIpLimit(sharedCampusIp);
        assertTrue(ipCheck.allowed());
    }

    @Test
    @DisplayName("Phân lập độc lập hạn mức cho các nhóm endpoint nhạy cảm (verify, chatbot, heavy, api)")
    void shouldIsolateLimitsByEndpointGroup() {
        String clientIp = "10.0.0.5";
        String user = "user:lecturer01";

        // Kiểm tra verify endpoint
        for (int i = 0; i < 4; i++) {
            assertTrue(rateLimitService.checkVerifyLimit(clientIp).allowed());
        }
        assertFalse(rateLimitService.checkVerifyLimit(clientIp).allowed());

        // Chatbot endpoint cho user vẫn còn nguyên hạn ngạch (không bị ảnh hưởng bởi verify)
        assertTrue(rateLimitService.checkChatbotLimit(user).allowed());

        // Heavy endpoint cho user
        assertTrue(rateLimitService.checkHeavyLimit(user).allowed());
        assertTrue(rateLimitService.checkHeavyLimit(user).allowed());
        assertFalse(rateLimitService.checkHeavyLimit(user).allowed());

        // API thông thường vẫn hoạt động bình thường
        assertTrue(rateLimitService.checkApiLimit(user).allowed());
    }

    @Test
    @DisplayName("Cho phép tất cả khi Rate Limiting bị vô hiệu hóa (disabled)")
    void shouldAllowAllWhenDisabled() {
        properties.setEnabled(false);
        String clientIp = "1.2.3.4";

        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitService.checkLoginIpLimit(clientIp).allowed());
            assertTrue(rateLimitService.checkLoginAccountLimit("any_user").allowed());
        }
    }
}
