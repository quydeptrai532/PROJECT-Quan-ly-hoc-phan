package com.edu.registration.service;

import com.edu.registration.entity.AuditLog;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.NotificationRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class LecturerClassAnnouncementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository thongBaoRepository;

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
    void lecturerCanSendAnnouncementToAssignedClassAndEnrolledStudentsReceiveNotification() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // 1. Get gv01's assigned classes
        JsonNode classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        assertFalse(classes.isEmpty(), "gv01 should have assigned classes");
        long assignedClassId = -1;
        String assignedClassCode = "";
        for (JsonNode c : classes) {
            if (c.get("siSoHienTai").asInt() > 0) {
                assignedClassId = c.get("id").asLong();
                assignedClassCode = c.get("ma").asText();
                break;
            }
        }
        assertTrue(assignedClassId > 0, "gv01 should have at least one class with registered students");

        // 2. sv01 is enrolled in gv01's classes (INT201.01, INT303.01, INT101.01-HK2)
        String studentUsername = "sv01";

        // 3. Lecturer sends class announcement
        String idempotencyKey = "key-" + UUID.randomUUID();
        String title = "Kiểm tra giữa kỳ tuần 8";
        String content = "Cả lớp mang theo máy tính cầm tay và thẻ sinh viên khi dự thi.";

        MvcResult sendRes = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + assignedClassId + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", title,
                                "noiDung", content,
                                "idempotencyKey", idempotencyKey
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tieuDe").value(title))
                .andExpect(jsonPath("$.noiDung").value(content))
                .andExpect(jsonPath("$.soNguoiNhan").isNumber())
                .andExpect(jsonPath("$.trangThaiGui").value("DA_GUI"))
                .andReturn();

        JsonNode created = json(sendRes);
        long announcementId = created.get("id").asLong();
        assertTrue(created.get("soNguoiNhan").asInt() > 0, "At least one enrolled student received the announcement");

        // 4. Verify Audit Log recorded
        List<AuditLog> auditLogs = auditLogRepository.findAll().stream()
                .filter(a -> "CLASS_ANNOUNCEMENT_CREATED".equals(a.getAction()))
                .toList();
        assertFalse(auditLogs.isEmpty(), "Audit log for CLASS_ANNOUNCEMENT_CREATED should be recorded");
        AuditLog lastAudit = auditLogs.get(auditLogs.size() - 1);
        assertEquals("GIANG_VIEN", lastAudit.getActorRole());
        assertEquals(String.valueOf(assignedClassId), lastAudit.getEntityId());
        assertEquals("COURSE_SECTION", lastAudit.getEntityType());
        assertEquals("SUCCESS", lastAudit.getResult());

        // 5. Verify enrolled student sees notification in notification center
        String tokenStudent = login(studentUsername, "123456");
        JsonNode notifications = json(mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenStudent))
                .andExpect(status().isOk())
                .andReturn());

        boolean foundInNotificationCenter = false;
        for (JsonNode item : notifications.get("content")) {
            if ("CLASS_ANNOUNCEMENT".equals(item.get("eventType").asText())
                    && title.equals(item.get("title").asText())) {
                foundInNotificationCenter = true;
                assertTrue(item.get("message").asText().contains(assignedClassCode));
                assertTrue(item.get("message").asText().contains(content));
                assertEquals("/hoc-phan-cua-toi", item.get("targetUrl").asText());
                break;
            }
        }
        assertTrue(foundInNotificationCenter, "Enrolled student must see class announcement in notification center");

        // 6. Enrolled student can view announcements via class announcement endpoint
        JsonNode studentClassAnnouncements = json(mockMvc.perform(get("/api/sinh-vien/lop-hoc-phan/" + assignedClassId + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenStudent))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(studentClassAnnouncements.isEmpty(), "Student should be able to view announcements of registered class");
        assertEquals(announcementId, studentClassAnnouncements.get(0).get("id").asLong());

        // 7. Lecturer can view announcement history
        JsonNode history = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + assignedClassId + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(history.isEmpty());
        assertEquals(announcementId, history.get(0).get("id").asLong());

        // 8. Retry with same idempotencyKey returns existing without duplicating
        MvcResult retryRes = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + assignedClassId + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", title,
                                "noiDung", content,
                                "idempotencyKey", idempotencyKey
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(announcementId, json(retryRes).get("id").asLong());
    }

    @Test
    void lecturerCannotSendAnnouncementToClassOfAnotherLecturer() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Find a class not assigned to gv01
        JsonNode allClasses = json(mockMvc.perform(get("/api/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        long otherClassId = -1;
        for (JsonNode c : allClasses) {
            JsonNode gv = c.get("giangVien");
            if (gv == null || !"gv01".equalsIgnoreCase(gv.get("ma").asText())) {
                otherClassId = c.get("id").asLong();
                break;
            }
        }

        if (otherClassId > 0) {
            mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + otherClassId + "/thong-bao")
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "tieuDe", "Thông báo thử nghiệm",
                                    "noiDung", "Nội dung thử nghiệm"
                            ))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void nonExistentClassReturns404() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/999999/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "Tiêu đề hợp lệ",
                                "noiDung", "Nội dung hợp lệ"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentCannotSendClassAnnouncement() throws Exception {
        String tokenStudent = login("sv01", "123456");

        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/1/thong-bao")
                        .header("Authorization", "Bearer " + tokenStudent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "Mạo danh",
                                "noiDung", "Thử nghiệm"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentNotInClassCannotAccessClassAnnouncements() throws Exception {
        // sv02 is not enrolled in INT201.01 (which is class with INT201.01)
        // Find INT201.01 class id
        String tokenGv01 = login("gv01", "123456");
        String tokenSv02 = login("sv02", "123456");

        JsonNode classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        long int201ClassId = -1;
        for (JsonNode c : classes) {
            if ("INT201.01".equals(c.get("ma").asText())) {
                int201ClassId = c.get("id").asLong();
                break;
            }
        }
        assertTrue(int201ClassId > 0, "INT201.01 should exist");

        // sv02 attempts to view announcements of INT201.01 which they did not register
        mockMvc.perform(get("/api/sinh-vien/lop-hoc-phan/" + int201ClassId + "/thong-bao")
                        .header("Authorization", "Bearer " + tokenSv02))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationRejectsBlankOrOversizedFields() throws Exception {
        String tokenGv01 = login("gv01", "123456");

        // Blank title
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/1/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "   ",
                                "noiDung", "Nội dung hợp lệ"
                        ))))
                .andExpect(status().isBadRequest());

        // Blank content
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/1/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "Tiêu đề hợp lệ",
                                "noiDung", "   "
                        ))))
                .andExpect(status().isBadRequest());

        // Oversized title (> 120 chars)
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/1/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "A".repeat(125),
                                "noiDung", "Nội dung hợp lệ"
                        ))))
                .andExpect(status().isBadRequest());

        // Oversized content (> 1000 chars)
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/1/thong-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tieuDe", "Tiêu đề hợp lệ",
                                "noiDung", "A".repeat(1005)
                        ))))
                .andExpect(status().isBadRequest());
    }
}
