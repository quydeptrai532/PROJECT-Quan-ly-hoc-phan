package com.edu.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AdminStatisticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
    }

    @Test
    void adminKpiDashboardAndExport_shouldWorkWithCorrectPermissions() throws Exception {
        String adminToken = login("admin", "admin123");

        // 1. Lấy Dashboard KPI tổng hợp
        MvcResult result = mockMvc.perform(get("/api/admin/thong-ke/kpi-dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boLoc").exists())
                .andExpect(jsonPath("$.kpi").exists())
                .andExpect(jsonPath("$.kpi.tongDangKy").isNumber())
                .andExpect(jsonPath("$.kpi.tiLeLapDay").isNumber())
                .andExpect(jsonPath("$.phanBoDiem").isArray())
                .andExpect(jsonPath("$.phanBoGpa").isArray())
                .andExpect(jsonPath("$.danhSachMon").isArray())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertEquals("DA_CONG_BO", json.get("boLoc").get("phamViDiem").asText());
        assertEquals(5, json.get("phanBoDiem").size());
        assertEquals(5, json.get("phanBoGpa").size());

        // 2. Xuất báo cáo CSV
        MvcResult csvResult = mockMvc.perform(get("/api/admin/thong-ke/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] csvBytes = csvResult.getResponse().getContentAsByteArray();
        assertTrue(csvBytes.length > 0);
        // Kiểm tra UTF-8 BOM
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvString = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("BÁO CÁO THỐNG KÊ VÀ DASHBOARD ĐÀO TẠO"));
        assertTrue(csvString.contains("Tên chỉ số,Tử số,Mẫu số,Giá trị,Đơn vị"));
        assertTrue(csvString.contains("Lượt đăng ký hiệu lực"));
        assertTrue(csvString.contains("Tỷ lệ lấp đầy"));
        assertTrue(csvString.contains("Tỷ lệ Đạt"));
    }

    @Test
    void studentAndLecturer_shouldBeForbiddenFromAccessingAdminStatistics() throws Exception {
        String studentToken = login("sv01", "123456");
        String lecturerToken = login("gv01", "123456");

        // Sinh viên bị chặn 403
        mockMvc.perform(get("/api/admin/thong-ke/kpi-dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/thong-ke/export")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Giảng viên bị chặn 403
        mockMvc.perform(get("/api/admin/thong-ke/kpi-dashboard")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/thong-ke/export")
                        .header("Authorization", "Bearer " + lecturerToken))
                .andExpect(status().isForbidden());
    }
}
