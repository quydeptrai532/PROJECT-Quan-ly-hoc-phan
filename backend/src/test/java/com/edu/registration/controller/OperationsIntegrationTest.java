package com.edu.registration.controller;

import com.edu.registration.config.RateLimitingProperties;
import com.edu.registration.ratelimit.InMemoryRateLimiter;
import com.edu.registration.ratelimit.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OperationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private InMemoryRateLimiter inMemoryRateLimiter;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
    }

    @Test
    void healthEndpoint_shouldBePublicAndReturnUpWithoutExposingSecrets() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("quan-ly-dao-tao"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.url").doesNotExist())
                .andExpect(jsonPath("$.secret").doesNotExist());
    }

    @Test
    void openApiDocs_shouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", startsWith("3.")))
                .andExpect(jsonPath("$.info.title", startsWith("Hệ thống Quản lý Đào tạo & Đăng ký Học phần")));
    }

    @Test
    void adminMetrics_shouldRequireAuthenticationAndQuanTriRole() throws Exception {
        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isUnauthorized());

        // 2. Authenticated as admin (QUAN_TRI) -> 200 with operational metrics
        String adminToken = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jvm.uptimeSeconds").isNumber())
                .andExpect(jsonPath("$.jvm.memoryUsedBytes").isNumber())
                .andExpect(jsonPath("$.business.totalUsers").isNumber())
                .andExpect(jsonPath("$.cache.activeCaches").isArray());
    }

    @Test
    void adminCache_shouldSupportListingAndClearing() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/cache"))
                .andExpect(status().isUnauthorized());

        // Admin -> 200
        String adminToken = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/cache")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caches").isArray());

        // Clear specific cache
        mockMvc.perform(post("/api/admin/cache/clear?name=catalog_subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.target").value("catalog_subjects"));
    }

    @Test
    void rateLimiter_shouldRejectWhenExceedingLoginQuota() throws Exception {
        String testIp = "10.99.88.77";
        RateLimitingProperties props = rateLimitService.getProperties();
        long originalCapacity = props.getLogin().getCapacity();

        try {
            // Đặt tạm capacity = 2 để kiểm thử rate limiting
            props.getLogin().setCapacity(2);
            inMemoryRateLimiter.reset("login:" + testIp);

            // Request 1: OK
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Remaining", "1"));

            // Request 2: OK
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Remaining", "0"));

            // Request 3: Exceeded -> 429 Too Many Requests
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> {
                                request.setRemoteAddr(testIp);
                                return request;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", "admin", "password", "admin123"))))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(jsonPath("$.message", containsString("Quá nhiều yêu cầu")))
                    .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        } finally {
            props.getLogin().setCapacity(originalCapacity);
            inMemoryRateLimiter.clear();
        }
    }
}
