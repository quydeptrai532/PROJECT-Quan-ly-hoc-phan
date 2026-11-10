package com.edu.registration.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử tích hợp OpenAPI và Swagger UI trên môi trường phát triển (QLSV-101 / TASK-OP-07A).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"h2", "dev"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("OpenAPI JSON docs phải trả về HTTP 200 kèm đặc tả chuẩn OpenAPI 3.0")
    void openApiDocs_shouldReturnValidOpenApiSpec() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(json);

        // Kiểm tra phiên bản OpenAPI và thông tin metadata
        assertTrue(root.has("openapi"), "Phải chứa thuộc tính openapi");
        assertTrue(root.get("openapi").asText().startsWith("3."), "Phiên bản OpenAPI phải là 3.x");
        assertEquals("Hệ thống Quản lý Đào tạo & Đăng ký Học phần (QLSV) API", root.path("info").path("title").asText());
        assertEquals("1.0.0", root.path("info").path("version").asText());

        // Kiểm tra Security Scheme: bearerAuth (JWT)
        JsonNode securitySchemes = root.path("components").path("securitySchemes");
        assertTrue(securitySchemes.has("bearerAuth"), "Phải cấu hình security scheme bearerAuth");
        assertEquals("http", securitySchemes.path("bearerAuth").path("type").asText());
        assertEquals("bearer", securitySchemes.path("bearerAuth").path("scheme").asText());
        assertEquals("JWT", securitySchemes.path("bearerAuth").path("bearerFormat").asText());

        // Kiểm tra danh sách API paths chính yếu được tài liệu hóa
        JsonNode paths = root.path("paths");
        assertTrue(paths.has("/api/auth/login"), "Phải có endpoint /api/auth/login");
        assertTrue(paths.has("/api/auth/me"), "Phải có endpoint /api/auth/me");
        assertTrue(paths.has("/api/health"), "Phải có endpoint /api/health");
        assertTrue(paths.has("/api/dang-ky"), "Phải có endpoint /api/dang-ky");
        assertTrue(paths.has("/api/sinh-vien/me/diem"), "Phải có endpoint /api/sinh-vien/me/diem");
        assertTrue(paths.has("/api/giang-vien/me/ho-so"), "Phải có endpoint /api/giang-vien/me/ho-so");
        assertTrue(paths.has("/api/admin/metrics"), "Phải có endpoint /api/admin/metrics");
        assertTrue(paths.has("/api/public/bang-diem/xac-thuc/{code}"), "Phải có endpoint xác thực bảng điểm công khai");

        // Không được để lộ endpoint nội bộ h2-console
        assertFalse(paths.has("/h2-console"), "Không được tài liệu hóa h2-console nội bộ");
    }

    @Test
    @DisplayName("Swagger UI HTML endpoint phải phản hồi thành công hoặc chuyển hướng hợp lệ")
    void swaggerUi_shouldBeAccessibleOnDev() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    @DisplayName("Bật Swagger UI trên DEV không làm lộ API nhạy cảm nếu chưa xác thực JWT")
    void sensitiveEndpoints_shouldStillRequireJwtAuthentication() throws Exception {
        // Gọi API admin khi chưa đăng nhập -> bắt buộc 401 Unauthorized
        mockMvc.perform(get("/api/admin/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập không hợp lệ"));

        // Gọi API sinh viên khi chưa đăng nhập -> bắt buộc 401 Unauthorized
        mockMvc.perform(get("/api/sinh-vien/me/thoi-khoa-bieu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập không hợp lệ"));

        // Gọi API giảng viên khi chưa đăng nhập -> bắt buộc 401 Unauthorized
        mockMvc.perform(get("/api/giang-vien/me/ho-so"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Phiên đăng nhập không hợp lệ"));
    }
}
