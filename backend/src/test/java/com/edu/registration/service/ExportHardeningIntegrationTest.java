package com.edu.registration.service;

import com.edu.registration.entity.AuditLog;
import com.edu.registration.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ExportHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("token").asText();
    }

    @Test
    void lecturerStudentListExport_shouldEnforceSecurityHeadersAndAudit() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // 1. Lấy danh sách lớp của GV01
        MvcResult classesRes = mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode classes = objectMapper.readTree(classesRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertTrue(classes.size() > 0, "gv01 must have at least one assigned class");
        long lopId = classes.get(0).get("id").asLong();

        // 2. Xuất danh sách sinh viên có kèm filter tuKhoa và sapXep
        MvcResult exportRes = mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien/csv")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .param("tuKhoa", "")
                        .param("sapXep", "hoTen"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] bytes = exportRes.getResponse().getContentAsByteArray();
        assertTrue(bytes.length > 3);
        // Kiểm tra UTF-8 BOM
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String cacheControl = exportRes.getResponse().getHeader(HttpHeaders.CACHE_CONTROL);
        assertNotNull(cacheControl);
        assertTrue(cacheControl.contains("no-store"));

        // 3. Kiểm tra AuditLog được ghi lại
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundAudit = auditLogs.stream().anyMatch(a ->
                "COURSE_STUDENT_LIST_EXPORTED".equals(a.getAction()) && "gv01".equals(a.getActorUsername())
        );
        assertTrue(foundAudit, "COURSE_STUDENT_LIST_EXPORTED audit log must be recorded for gv01");
    }

    @Test
    void lecturerStudentListExport_shouldDenyUnauthorizedLecturer() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");

        // Lấy lớp của gv01
        MvcResult classesRes = mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode classes = objectMapper.readTree(classesRes.getResponse().getContentAsString(StandardCharsets.UTF_8));
        long lopId = classes.get(0).get("id").asLong();

        // GV02 không được phép xuất lớp của GV01
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien/csv")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void adminStatisticsExport_shouldIncludeTimezoneAndAudit() throws Exception {
        String adminToken = login("admin", "admin123");

        MvcResult csvResult = mockMvc.perform(get("/api/admin/thong-ke/export")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] csvBytes = csvResult.getResponse().getContentAsByteArray();
        // UTF-8 BOM
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvString = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("(GMT+7)"), "CSV report timestamp must contain (GMT+7)");

        // Kiểm tra AuditLog
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundAudit = auditLogs.stream().anyMatch(a ->
                "ADMIN_STATISTICS_EXPORTED".equals(a.getAction()) && "admin".equals(a.getActorUsername())
        );
        assertTrue(foundAudit, "ADMIN_STATISTICS_EXPORTED audit log must be recorded for admin");
    }

    @Test
    void studentTranscriptPdfExport_shouldIncludeSafeHeadersAndAudit() throws Exception {
        String studentToken = login("sv01", "123456");

        MvcResult pdfResult = mockMvc.perform(get("/api/sinh-vien/me/bang-diem/pdf")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Content-Disposition"))
                .andReturn();

        byte[] pdfBytes = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdfBytes.length > 100);
        // PDF magic bytes %PDF-
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);

        String disposition = pdfResult.getResponse().getHeader("Content-Disposition");
        assertTrue(disposition.contains("bang-diem"));

        // Kiểm tra AuditLog
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundAudit = auditLogs.stream().anyMatch(a ->
                "STUDENT_TRANSCRIPT_EXPORTED".equals(a.getAction()) && "sv01".equals(a.getActorUsername())
        );
        assertTrue(foundAudit, "STUDENT_TRANSCRIPT_EXPORTED audit log must be recorded for sv01");
    }
}
