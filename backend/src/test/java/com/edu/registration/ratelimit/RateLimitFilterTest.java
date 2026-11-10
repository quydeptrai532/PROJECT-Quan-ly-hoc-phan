package com.edu.registration.ratelimit;

import com.edu.registration.config.RateLimitingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private InMemoryRateLimiter inMemoryRateLimiter;
    private RateLimitingProperties properties;
    private RateLimitService rateLimitService;
    private ObjectMapper objectMapper;
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        inMemoryRateLimiter = new InMemoryRateLimiter();
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setStrategy(RateLimitingProperties.Strategy.IN_MEMORY);
        properties.setTrustedProxies(List.of("127.0.0.1", "::1"));

        RateLimitingProperties.LoginLimits loginLimits = new RateLimitingProperties.LoginLimits();
        loginLimits.setIp(new RateLimitingProperties.EndpointLimit(5, 60));
        loginLimits.setAccount(new RateLimitingProperties.EndpointLimit(2, 60));
        properties.setLogin(loginLimits);

        properties.setVerify(new RateLimitingProperties.EndpointLimit(3, 60));
        properties.setApi(new RateLimitingProperties.EndpointLimit(10, 60));
        properties.setApiAnonymous(new RateLimitingProperties.EndpointLimit(5, 60));

        rateLimitService = new RateLimitService(properties, inMemoryRateLimiter, null);
        objectMapper = new ObjectMapper();
        rateLimitFilter = new RateLimitFilter(rateLimitService, objectMapper);
    }

    @Test
    @DisplayName("Các điểm cuối được miễn trừ (health check, Swagger UI) được bỏ qua hoàn toàn")
    void shouldBypassExcludedPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertNull(response.getHeader("X-RateLimit-Limit"));
    }

    @Test
    @DisplayName("Đăng nhập vượt ngưỡng tài khoản trả về HTTP 429 và đầy đủ headers Retry-After")
    void shouldReturn429WhenLoginAccountExceedsLimit() throws Exception {
        String loginPayload = "{\"username\":\"sv_test\",\"password\":\"MatKhauBiMat123!\"}";

        // Lượt 1: Thành công
        MockHttpServletRequest req1 = createLoginRequest(loginPayload, "192.168.1.50");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        rateLimitFilter.doFilter(req1, res1, new MockFilterChain());
        assertEquals(HttpServletResponse.SC_OK, res1.getStatus());

        // Lượt 2: Thành công
        MockHttpServletRequest req2 = createLoginRequest(loginPayload, "192.168.1.50");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        rateLimitFilter.doFilter(req2, res2, new MockFilterChain());
        assertEquals(HttpServletResponse.SC_OK, res2.getStatus());

        // Lượt 3: Vượt quá ngưỡng tài khoản (2 req/60s) -> 429
        MockHttpServletRequest req3 = createLoginRequest(loginPayload, "192.168.1.50");
        MockHttpServletResponse res3 = new MockHttpServletResponse();
        rateLimitFilter.doFilter(req3, res3, new MockFilterChain());

        assertEquals(429, res3.getStatus());
        assertTrue(res3.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE));
        assertNotNull(res3.getHeader("Retry-After"));
        assertEquals("2", res3.getHeader("X-RateLimit-Limit"));
        assertEquals("0", res3.getHeader("X-RateLimit-Remaining"));

        JsonNode body = objectMapper.readTree(res3.getContentAsString());
        assertTrue(body.has("message"));
        assertTrue(body.has("retryAfterSeconds"));
        assertTrue(body.has("timestamp"));
        assertTrue(body.get("retryAfterSeconds").asLong() > 0);
    }

    @Test
    @DisplayName("Người dùng khác trên cùng IP vẫn đăng nhập được khi một tài khoản bị 429")
    void shouldNotBlockOtherUserOnSameIp() throws Exception {
        String attackedUser = "{\"username\":\"victim_user\",\"password\":\"WrongPassword!\"}";
        String validUser = "{\"username\":\"normal_student\",\"password\":\"CorrectPassword!\"}";
        String sharedIp = "203.0.113.88";

        // Gửi 2 lần cho victim_user
        for (int i = 0; i < 2; i++) {
            MockHttpServletRequest req = createLoginRequest(attackedUser, sharedIp);
            rateLimitFilter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        // Lần 3 cho victim_user bị chặn 429
        MockHttpServletResponse resBlocked = new MockHttpServletResponse();
        rateLimitFilter.doFilter(createLoginRequest(attackedUser, sharedIp), resBlocked, new MockFilterChain());
        assertEquals(429, resBlocked.getStatus());

        // normal_student từ CÙNG sharedIp vẫn được qua filter bình thường!
        MockHttpServletResponse resAllowed = new MockHttpServletResponse();
        rateLimitFilter.doFilter(createLoginRequest(validUser, sharedIp), resAllowed, new MockFilterChain());
        assertEquals(HttpServletResponse.SC_OK, resAllowed.getStatus());
    }

    @Test
    @DisplayName("Bỏ qua X-Forwarded-For giả mạo khi request đến từ IP không tin cậy")
    void shouldIgnoreSpoofedIpFromUntrustedClient() throws Exception {
        String loginPayload = "{\"username\":\"user_spoof\",\"password\":\"123456\"}";

        // Untrusted client (203.0.113.99) cố tình thay đổi X-Forwarded-For trong mỗi request
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest req = createLoginRequest(loginPayload, "203.0.113.99");
            req.addHeader("X-Forwarded-For", "10.0.0." + i); // Fake IP khác nhau
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitFilter.doFilter(req, res, new MockFilterChain());

            if (i > 2) {
                // Đã bị chặn do account limit = 2 (dù client đổi IP fake liên tục)
                assertEquals(429, res.getStatus());
            } else {
                assertEquals(HttpServletResponse.SC_OK, res.getStatus());
            }
        }
    }

    @Test
    @DisplayName("Điểm cuối xác thực văn bằng công khai bị giới hạn theo chuẩn 429")
    void shouldRateLimitPublicVerificationEndpoint() throws Exception {
        String verifyPath = "/api/public/bang-diem/xac-thuc/VB-2026-ABCD";

        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", verifyPath);
            req.setRemoteAddr("192.168.1.77");
            MockHttpServletResponse res = new MockHttpServletResponse();
            rateLimitFilter.doFilter(req, res, new MockFilterChain());
            assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        }

        // Lượt thứ 4 -> Vượt ngưỡng verify (3 req/60s)
        MockHttpServletRequest req4 = new MockHttpServletRequest("GET", verifyPath);
        req4.setRemoteAddr("192.168.1.77");
        MockHttpServletResponse res4 = new MockHttpServletResponse();
        rateLimitFilter.doFilter(req4, res4, new MockFilterChain());

        assertEquals(429, res4.getStatus());
        assertEquals("0", res4.getHeader("X-RateLimit-Remaining"));
        assertNotNull(res4.getHeader("Retry-After"));
    }

    private MockHttpServletRequest createLoginRequest(String jsonPayload, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(jsonPayload.getBytes());
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
