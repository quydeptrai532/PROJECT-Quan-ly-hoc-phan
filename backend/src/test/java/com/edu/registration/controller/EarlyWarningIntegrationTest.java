package com.edu.registration.controller;

import com.edu.registration.dto.AcademicAlertDtos.CapNhatTrangThaiRequest;
import com.edu.registration.entity.AcademicAlertStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class EarlyWarningIntegrationTest {

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
    @DisplayName("Bảo mật phân quyền API: Quản trị viên, Giảng viên, Sinh viên và Unauthenticated")
    void kiemTraPhanQuyenVaBaoMat() throws Exception {
        String tokenAdmin = login("admin", "admin123");
        String tokenSv = login("sv01", "123456");
        String tokenGv = login("gv01", "123456");

        // 1. Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/canh-bao-hoc-tap/cau-hinh"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sinh-vien/me/canh-bao-hoc-tap"))
                .andExpect(status().isUnauthorized());

        // 2. Sinh viên không được vào trang quản trị -> 403
        mockMvc.perform(get("/api/admin/canh-bao-hoc-tap/cau-hinh")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/canh-bao-hoc-tap/quet-du-lieu")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isForbidden());

        // 3. Quản trị viên truy cập cấu hình thành công -> 200
        MvcResult configRes = mockMvc.perform(get("/api/admin/canh-bao-hoc-tap/cau-hinh")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode configJson = json(configRes);
        assertTrue(configJson.get("enabled").asBoolean());
        assertFalse(configJson.get("officialRegulation").asBoolean(), "Chưa có quy chế chính thức");
        assertEquals(4, configJson.get("danhSachLuat").size());
        assertTrue(configJson.get("giaTriChoXacNhan").size() > 0);

        // 4. Quản trị viên kích hoạt quét toàn bộ hệ thống -> 200
        MvcResult scanRes = mockMvc.perform(post("/api/admin/canh-bao-hoc-tap/quet-du-lieu")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode scanJson = json(scanRes);
        assertTrue(scanJson.get("soSinhVienQuet").asInt() > 0);

        // 5. Quản trị viên lấy danh sách cảnh báo -> 200
        MvcResult listRes = mockMvc.perform(get("/api/admin/canh-bao-hoc-tap")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode listJson = json(listRes);
        assertNotNull(listJson.get("content"));

        // 6. Sinh viên truy cập cảnh báo của mình -> 200
        MvcResult svRes = mockMvc.perform(get("/api/sinh-vien/me/canh-bao-hoc-tap")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode svJson = json(svRes);
        assertNotNull(svJson.get("content"));

        // Alias URL /api/sinh-vien/me/canh-bao-hoc-vu
        mockMvc.perform(get("/api/sinh-vien/me/canh-bao-hoc-vu")
                        .header("Authorization", "Bearer " + tokenSv))
                .andExpect(status().isOk());

        // 7. Giảng viên truy cập cảnh báo lớp phụ trách -> 200
        MvcResult gvRes = mockMvc.perform(get("/api/giang-vien/canh-bao-hoc-tap")
                        .header("Authorization", "Bearer " + tokenGv))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode gvJson = json(gvRes);
        assertNotNull(gvJson.get("content"));

        // 8. Cập nhật trạng thái xử lý: nếu có bản ghi cảnh báo
        if (listJson.get("content").size() > 0) {
            long alertId = listJson.get("content").get(0).get("id").asLong();

            // Sinh viên thử cập nhật -> 403 Forbidden
            CapNhatTrangThaiRequest req = new CapNhatTrangThaiRequest(AcademicAlertStatus.DANG_XU_LY, "Cần trao đổi");
            mockMvc.perform(patch("/api/admin/canh-bao-hoc-tap/" + alertId + "/xu-ly")
                            .header("Authorization", "Bearer " + tokenSv)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden());

            // Quản trị viên cập nhật -> 200 OK
            MvcResult patchRes = mockMvc.perform(patch("/api/admin/canh-bao-hoc-tap/" + alertId + "/xu-ly")
                            .header("Authorization", "Bearer " + tokenAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode patchJson = json(patchRes);
            assertEquals("DANG_XU_LY", patchJson.get("trangThai").asText());
            assertEquals("Cần trao đổi", patchJson.get("ghiChuXuLy").asText());
            assertEquals("admin", patchJson.get("nguoiXuLy").asText());
        }
    }
}
