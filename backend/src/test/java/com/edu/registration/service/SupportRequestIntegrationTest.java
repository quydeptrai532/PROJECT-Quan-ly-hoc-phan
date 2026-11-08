package com.edu.registration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class SupportRequestIntegrationTest {

    @TempDir
    static Path tempStorageDir;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.support-request.storage-dir", () -> tempStorageDir.toString());
        registry.add("app.support-request.max-file-size-bytes", () -> 1048576L); // 1 MB
    }

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
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("token").asText();
    }

    @Test
    void fullLifecycleAndSecurityIsolationBetweenStudentsLecturersAndAdmin() throws Exception {
        String tokenSv01 = login("sv01", "123456");
        String tokenSv02 = login("sv02", "123456");
        String tokenGv01 = login("gv01", "123456");
        String tokenAdmin = login("admin", "admin123");

        // 1. Fetch catalog as sv01
        MvcResult catalogRes = mockMvc.perform(get("/api/yeu-cau-ho-tro/danh-muc")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaiYeuCau").isArray())
                .andExpect(jsonPath("$.trangThai").isArray())
                .andExpect(jsonPath("$.diemCuaToi").isArray())
                .andReturn();
        JsonNode catalogJson = objectMapper.readTree(catalogRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        Long sv01DiemId = catalogJson.get("diemCuaToi").get(0).get("id").asLong();

        // 2. sv01 creates support request with PDF attachment and spoofed studentId in body (must use principal sv01)
        byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        MockMultipartFile pdfPart = new MockMultipartFile(
                "files", "bang-diem-minh-chung.pdf", "application/pdf", pdfBytes);

        String payloadJson = objectMapper.writeValueAsString(Map.of(
                "studentId", 999999L,
                "sinhVienId", 999999L,
                "loaiYeuCau", "DIEM_SO",
                "tieuDe", "Khiếu nại điểm môn học kỳ trước",
                "noiDung", "Em đề nghị kiểm tra lại điểm quá trình và cuối kỳ của học phần này.",
                "diemId", sv01DiemId
        ));
        MockMultipartFile payloadPart = new MockMultipartFile(
                "payload", "", "application/json", payloadJson.getBytes(StandardCharsets.UTF_8));

        MvcResult createRes = mockMvc.perform(multipart("/api/yeu-cau-ho-tro")
                        .file(payloadPart)
                        .file(pdfPart)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maSinhVien").value("SV001"))
                .andExpect(jsonPath("$.trangThai").value("MOI_TAO"))
                .andExpect(jsonPath("$.tepDinhKem.length()").value(1))
                .andExpect(jsonPath("$.lichSuTrangThai.length()").value(1))
                .andReturn();

        JsonNode createdTicket = objectMapper.readTree(createRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long ticketId = createdTicket.get("id").asLong();
        long attachmentId = createdTicket.get("tepDinhKem").get(0).get("id").asLong();

        // 3. Original content cannot be edited via PUT
        mockMvc.perform(put("/api/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieuDe\":\"Sửa tiêu đề\",\"noiDung\":\"Sửa nội dung gốc\"}"))
                .andExpect(status().isBadRequest());

        // 4. Student B (sv02) cannot view, comment, or download attachment of Student A's ticket (Anti-IDOR)
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/yeu-cau-ho-tro/" + ticketId + "/trao-doi")
                        .header("Authorization", "Bearer " + tokenSv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("noiDung", "Truy cập trái phép"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + attachmentId)
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isForbidden());

        // 5. Unauthenticated request cannot download attachment (not public)
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + attachmentId))
                .andExpect(status().isUnauthorized());

        // 6. Lecturer (gv01) cannot access support requests or admin endpoints
        mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        // 7. Student A cannot change ticket status
        mockMvc.perform(patch("/api/yeu-cau-ho-tro/" + ticketId + "/trang-thai")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("trangThai", "DA_GIAI_QUYET"))))
                .andExpect(status().isForbidden());

        // 8. Admin updates status to DANG_XU_LY and adds a comment
        mockMvc.perform(patch("/api/admin/yeu-cau-ho-tro/" + ticketId + "/trang-thai")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "trangThai", "DANG_XU_LY",
                                "ghiChu", "Phòng đào tạo đang rà soát biên bản chấm thi"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trangThai").value("DANG_XU_LY"))
                .andExpect(jsonPath("$.lichSuTrangThai.length()").value(2))
                .andExpect(jsonPath("$.lichSuTrangThai[1].tenNguoiThucHien").value("admin"));

        mockMvc.perform(post("/api/yeu-cau-ho-tro/" + ticketId + "/trao-doi")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "noiDung", "Chào em, phòng đào tạo đã nhận được minh chứng và đang đối chiếu."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vaiTroNguoiGui").value("QUAN_TRI"));

        // 9. Student A replies and downloads own attachment with security headers
        mockMvc.perform(post("/api/yeu-cau-ho-tro/" + ticketId + "/trao-doi")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "noiDung", "Dạ em cảm ơn thầy/cô ạ."
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vaiTroNguoiGui").value("SINH_VIEN"));

        MvcResult downloadRes = mockMvc.perform(get("/api/yeu-cau-ho-tro/" + ticketId + "/tep-dinh-kem/" + attachmentId)
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();
        assertArrayEquals(pdfBytes, downloadRes.getResponse().getContentAsByteArray());
        assertTrue(downloadRes.getResponse().getHeader("Content-Disposition").startsWith("attachment;"));

        // 10. Student B's list does NOT contain Student A's ticket
        mockMvc.perform(get("/api/yeu-cau-ho-tro/me")
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
