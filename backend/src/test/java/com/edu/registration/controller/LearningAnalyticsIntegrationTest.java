package com.edu.registration.controller;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class LearningAnalyticsIntegrationTest {

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
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void testPhanQuyenVaZeroPiiVaBaoCaoTongHopLearningAnalytics() throws Exception {
        String tokenAdmin = login("admin", "admin123");
        String tokenGv01 = login("gv01", "123456");
        String tokenSv01 = login("sv01", "123456");

        // 1. Sinh viên gọi API Admin hoặc Giảng viên bị chặn 403
        mockMvc.perform(get("/api/admin/learning-analytics")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/giang-vien/learning-analytics")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        // 2. Giảng viên gọi API Admin bị chặn 403
        mockMvc.perform(get("/api/admin/learning-analytics")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 3. Quản trị viên gọi API tổng hợp thành công
        MvcResult resAdmin = mockMvc.perform(get("/api/admin/learning-analytics")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi").exists())
                .andExpect(jsonPath("$.phanBoDiem").isArray())
                .andExpect(jsonPath("$.xuHuongTheoHocKy").isArray())
                .andExpect(jsonPath("$.canhBaoNhanQua").exists())
                .andReturn();

        JsonNode adminJson = json(resAdmin);
        assertNotNull(adminJson.get("kpi"));
        assertTrue(adminJson.get("canhBaoNhanQua").asText().contains("không suy diễn kết luận nhân quả"));

        // Xác nhận Zero Student PII
        String responseContent = resAdmin.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(responseContent.contains("maSinhVien"));
        assertFalse(responseContent.contains("hoTenSinhVien"));

        // 4. Xu hướng GPA theo học kỳ
        mockMvc.perform(get("/api/admin/learning-analytics/xu-huong-gpa")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 5. Xuất CSV có UTF-8 BOM
        MvcResult csvRes = mockMvc.perform(get("/api/admin/learning-analytics/xuat-csv")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andReturn();

        byte[] csvBytes = csvRes.getResponse().getContentAsByteArray();
        assertTrue(csvBytes.length > 3);
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        // 6. Giảng viên gọi API của mình thành công
        mockMvc.perform(get("/api/giang-vien/learning-analytics")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi").exists())
                .andExpect(jsonPath("$.boLoc.vaiTroNguoiXem").value("GIANG_VIEN"));
    }
}
