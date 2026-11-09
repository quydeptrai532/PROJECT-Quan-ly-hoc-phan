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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class LecturerDashboardIntegrationTest {

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
    void dashboardIsScopedToAuthenticatedLecturerAndIgnoresClientIds() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");

        JsonNode own = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());

        // Spoofed lecturer identifiers in the query string must have no effect.
        JsonNode dashboard = json(mockMvc.perform(get("/api/giang-vien/me/dashboard")
                        .param("giangVienId", "999")
                        .param("userId", "999")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hocKy.id").isNumber())
                .andReturn());

        long hocKyId = dashboard.get("hocKy").get("id").asLong();
        Set<String> expected = new HashSet<>();
        own.forEach(lop -> {
            if (lop.get("hocKyId").asLong() == hocKyId) {
                assertEquals("TS. Nguyễn Văn An", lop.get("tenGiangVien").asText());
                expected.add(lop.get("ma").asText());
            }
        });
        Set<String> actual = new HashSet<>();
        int tongSinhVien = 0;
        for (JsonNode lop : dashboard.get("lopPhuTrach")) {
            actual.add(lop.get("ma").asText());
            tongSinhVien += lop.get("siSoHienTai").asInt();
        }
        assertFalse(actual.isEmpty());
        assertEquals(expected, actual);
        assertEquals(actual.size(), dashboard.get("tongQuan").get("soLop").asInt());
        assertEquals(tongSinhVien, dashboard.get("tongQuan").get("tongSinhVien").asInt());
        assertEquals(actual.size(), dashboard.get("lichDay").size());

        JsonNode otherDashboard = json(mockMvc.perform(get("/api/giang-vien/me/dashboard")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk()).andReturn());
        for (JsonNode lop : otherDashboard.get("lopPhuTrach")) {
            assertFalse(actual.contains(lop.get("ma").asText()), "gv02 must not see gv01 classes");
        }
    }

    @Test
    void nonLecturersCannotAccessLecturerDashboardOrProfile() throws Exception {
        String tokenSv01 = login("sv01", "123456");
        mockMvc.perform(get("/api/giang-vien/me/dashboard").header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/giang-vien/me/ho-so").header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/giang-vien/me/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lecturerCanOnlyUpdateAllowedPersonalFields() throws Exception {
        String token = login("gv01", "123456");

        JsonNode before = json(mockMvc.perform(get("/api/giang-vien/me/ho-so")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ma").value("GV01"))
                .andExpect(jsonPath("$.role").value("GIANG_VIEN"))
                .andExpect(jsonPath("$.truongDuocPhepSua.length()").value(3))
                .andReturn());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("hoTen", before.get("hoTen").asText());
        payload.put("email", "gv01.moi@university.edu.vn");
        payload.put("soDienThoai", "0901234567");
        payload.put("ma", "HACKED");
        payload.put("role", "QUAN_TRI");
        payload.put("khoaId", 999);
        payload.put("trangThai", false);

        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ma").value("GV01"))
                .andExpect(jsonPath("$.role").value("GIANG_VIEN"));

        JsonNode after = json(mockMvc.perform(get("/api/giang-vien/me/ho-so")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
        assertEquals("GV01", after.get("ma").asText());
        assertEquals("GIANG_VIEN", after.get("role").asText());
        assertEquals(before.get("khoaMa").asText(), after.get("khoaMa").asText());
        assertTrue(after.get("taiKhoanHoatDong").asBoolean());
        assertEquals("gv01.moi@university.edu.vn", after.get("email").asText());
        assertEquals("0901234567", after.get("soDienThoai").asText());

        // Field length limits mirror the database columns.
        payload.put("soDienThoai", "0".repeat(21));
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        // Restore seed values for other tests sharing the context.
        Map<String, Object> restore = new LinkedHashMap<>();
        restore.put("hoTen", before.get("hoTen").asText());
        restore.put("email", before.get("email").isNull() ? null : before.get("email").asText());
        restore.put("soDienThoai", before.get("soDienThoai").isNull() ? null : before.get("soDienThoai").asText());
        mockMvc.perform(put("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restore)))
                .andExpect(status().isOk());
    }
}
