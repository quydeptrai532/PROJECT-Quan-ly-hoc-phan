package com.edu.registration.controller;

import com.edu.registration.dto.BackgroundJobDtos;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.service.job.BackgroundJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminBackgroundJobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BackgroundJobService backgroundJobService;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
    }

    @Test
    void unauthenticatedRequests_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/background-jobs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/background-jobs/stats"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/admin/background-jobs/1/retry"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentAndLecturerRequests_shouldReturn403() throws Exception {
        String svToken = login("sv01", "123456");
        String gvToken = login("gv01", "123456");

        mockMvc.perform(get("/api/admin/background-jobs")
                        .header("Authorization", "Bearer " + svToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/background-jobs/stats")
                        .header("Authorization", "Bearer " + svToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/background-jobs")
                        .header("Authorization", "Bearer " + gvToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/background-jobs/stats")
                        .header("Authorization", "Bearer " + gvToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUser_canViewListStatsAndManageJobs() throws Exception {
        String adminToken = login("admin", "admin123");

        // 1. Tạo một job mẫu qua service
        BackgroundJob job = backgroundJobService.enqueue(
                "TEST_INTEGRATION_JOB", "integration-key-" + System.currentTimeMillis(),
                "{\"info\":\"admin test\"}", 3, 30, null);

        // 2. Tra cứu danh sách phân trang
        mockMvc.perform(get("/api/admin/background-jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());

        // 3. Tra cứu thống kê
        mockMvc.perform(get("/api/admin/background-jobs/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").isNumber())
                .andExpect(jsonPath("$.running").isNumber())
                .andExpect(jsonPath("$.succeeded").isNumber())
                .andExpect(jsonPath("$.failed").isNumber())
                .andExpect(jsonPath("$.total").isNumber());

        // 4. Tra cứu chi tiết job
        mockMvc.perform(get("/api/admin/background-jobs/" + job.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(job.getId()))
                .andExpect(jsonPath("$.jobType").value("TEST_INTEGRATION_JOB"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 5. Hủy job
        BackgroundJobDtos.CancelJobRequest cancelReq = new BackgroundJobDtos.CancelJobRequest("Test hủy từ integration test");
        mockMvc.perform(post("/api/admin/background-jobs/" + job.getId() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.lastError").exists());

        // 6. Thử lại job
        mockMvc.perform(post("/api/admin/background-jobs/" + job.getId() + "/retry")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
