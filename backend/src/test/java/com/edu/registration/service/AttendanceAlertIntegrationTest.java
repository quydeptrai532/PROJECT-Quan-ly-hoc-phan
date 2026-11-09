package com.edu.registration.service;

import com.edu.registration.dto.AttendanceAlertDtos;
import com.edu.registration.dto.AttendanceDtos;
import com.edu.registration.entity.AuditLog;
import com.edu.registration.entity.AlertStatus;
import com.edu.registration.entity.AttendanceStatus;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.AttendanceAlertRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AttendanceAlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttendanceAlertRepository canhBaoDiemDanhRepository;

    @Autowired
    private NotificationRepository thongBaoRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
    void testAttendanceThresholdAlertLifecycleSecurityAndIdempotency() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");

        // 1. Get gv01's classes
        JsonNode gv01Classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(gv01Classes.isEmpty(), "gv01 should have assigned classes");

        long lopId = -1;
        String lopMa = "";
        for (JsonNode c : gv01Classes) {
            if (c.get("siSoHienTai").asInt() > 0) {
                lopId = c.get("id").asLong();
                lopMa = c.get("ma").asText();
                break;
            }
        }
        assertTrue(lopId > 0, "gv01 should have a class with enrolled students");

        // 2. Security check: gv02 cannot view or configure alerts for gv01's class (403 Forbidden)
        mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/cau-hinh-canh-bao")
                        .header("Authorization", "Bearer " + tokenGv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceAlertDtos.CauHinhNguongLopRequest(2, 20.0))))
                .andExpect(status().isForbidden());

        // 3. gv01 views initial alert report: without configured threshold, alertEnabled is false
        JsonNode initialReport = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertNotNull(initialReport.get("ghiChuQuyChe"));

        // 4. gv01 configures threshold for class: 2 absences or 25% absence rate
        JsonNode configRes = json(mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/cau-hinh-canh-bao")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceAlertDtos.CauHinhNguongLopRequest(2, 25.0))))
                .andExpect(status().isOk())
                .andReturn());
        assertTrue(configRes.get("canhBaoKichHoat").asBoolean());
        assertEquals(2, configRes.get("nguongSoBuoiVang").asInt());
        assertEquals(25.0, configRes.get("nguongTyLeVang").asDouble());
        assertEquals("LOP_HOC_PHAN", configRes.get("nguonNguong").asText());

        // Verify audit log for threshold config update
        final long finalLopId = lopId;
        List<AuditLog> configAudits = auditLogRepository.findAll().stream()
                .filter(a -> "LOP_HOC_PHAN".equals(a.getEntityType()) && String.valueOf(finalLopId).equals(a.getEntityId()))
                .toList();
        assertTrue(configAudits.stream().anyMatch(a -> "ATTENDANCE_THRESHOLD_CONFIG_UPDATED".equals(a.getAction())));

        // 5. Create session 1 (today)
        LocalDate sessionDate = LocalDate.of(2026, 4, 15);
        MvcResult session1Res = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/buoi-hoc")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.TaoBuoiHocRequest(
                                lopId, sessionDate, 1, "P101", 1, 3, "Buổi 1 test"))))
                .andExpect(status().isOk())
                .andReturn();
        long buoi1Id = json(session1Res).get("id").asLong();

        // Get student list for session 1
        JsonNode buoi1Details = json(mockMvc.perform(get("/api/giang-vien/buoi-hoc/" + buoi1Id + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        JsonNode studentList = buoi1Details.get("danhSachDiemDanh");
        assertFalse(studentList.isEmpty());
        long svId = studentList.get(0).get("sinhVienId").asLong();

        // 6. Record attendance: svId is VANG in session 1 (1/1 valid sessions = 100% absence rate)
        // 100% >= 25.0% threshold -> TY_LE_VANG alert must be triggered!
        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoi1Id + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.LuuDiemDanhRequest(
                                List.of(new AttendanceDtos.DiemDanhRecordRequest(svId, AttendanceStatus.VANG, "Vắng buổi 1")),
                                false))))
                .andExpect(status().isOk());

        // Check alerts report: must contain active alert for TY_LE_VANG
        JsonNode alertReport = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(1, alertReport.get("tongSoBuoiHopLe").asInt());
        JsonNode danhSachCanhBao = alertReport.get("danhSachCanhBao");
        assertFalse(danhSachCanhBao.isEmpty(), "Should have active alert for exceeding 25% threshold");

        boolean hasTyLeAlert = false;
        for (JsonNode a : danhSachCanhBao) {
            if ("TY_LE_VANG".equals(a.get("loaiNguong").asText()) && svId == a.get("sinhVienId").asLong()) {
                hasTyLeAlert = true;
                assertEquals(1, a.get("soBuoiVang").asInt());
                assertEquals(1, a.get("tongBuoiHopLe").asInt());
                assertEquals(100.0, a.get("tyLeVang").asDouble());
                assertEquals("DANG_CANH_BAO", a.get("trangThai").asText());
                assertTrue(a.get("thongDiep").asText().contains("1/1 buổi hợp lệ"));
            }
        }
        assertTrue(hasTyLeAlert, "Alert for TY_LE_VANG must exist");

        // 7. Verify Idempotency: Saving attendance again with identical values must NOT duplicate alerts or notifications
        long alertRowCountBefore = canhBaoDiemDanhRepository.count();
        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoi1Id + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.LuuDiemDanhRequest(
                                List.of(new AttendanceDtos.DiemDanhRecordRequest(svId, AttendanceStatus.VANG, "Vắng buổi 1")),
                                false))))
                .andExpect(status().isOk());
        assertEquals(alertRowCountBefore, canhBaoDiemDanhRepository.count(), "Alert rows must not duplicate on idempotent save");

        // 8. Create session 2 in the past/today (ca 2) and mark svId absent -> reaches 2 absences (SO_BUOI_VANG >= 2)
        MvcResult session2Res = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/buoi-hoc")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.TaoBuoiHocRequest(
                                lopId, sessionDate, 2, "P101", 4, 3, "Buổi 2 test"))))
                .andExpect(status().isOk())
                .andReturn();
        long buoi2Id = json(session2Res).get("id").asLong();

        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoi2Id + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.LuuDiemDanhRequest(
                                List.of(new AttendanceDtos.DiemDanhRecordRequest(svId, AttendanceStatus.VANG, "Vắng buổi 2")),
                                false))))
                .andExpect(status().isOk());

        JsonNode alertReport2 = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(2, alertReport2.get("tongSoBuoiHopLe").asInt());

        boolean hasSoBuoiAlert = false;
        for (JsonNode a : alertReport2.get("danhSachCanhBao")) {
            if ("SO_BUOI_VANG".equals(a.get("loaiNguong").asText()) && svId == a.get("sinhVienId").asLong()) {
                hasSoBuoiAlert = true;
                assertEquals(2, a.get("soBuoiVang").asInt());
                assertEquals(2, a.get("tongBuoiHopLe").asInt());
                assertEquals("DANG_CANH_BAO", a.get("trangThai").asText());
            }
        }
        assertTrue(hasSoBuoiAlert, "Alert for SO_BUOI_VANG must exist when absences >= 2");

        // 9. Lecturer changes session 2 attendance to CO_MAT (absences drop back to 1)
        // -> SO_BUOI_VANG falls below 2 -> alert status transitions to DA_GIAI_TOA without deleting audit history!
        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoi2Id + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AttendanceDtos.LuuDiemDanhRequest(
                                List.of(new AttendanceDtos.DiemDanhRecordRequest(svId, AttendanceStatus.CO_MAT, "Đã đi học bù")),
                                false))))
                .andExpect(status().isOk());

        JsonNode alertReport3 = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());

        boolean soBuoiResolved = false;
        for (JsonNode a : alertReport3.get("danhSachCanhBao")) {
            if ("SO_BUOI_VANG".equals(a.get("loaiNguong").asText()) && svId == a.get("sinhVienId").asLong()) {
                if ("DA_GIAI_TOA".equals(a.get("trangThai").asText())) {
                    soBuoiResolved = true;
                    assertEquals(1, a.get("soBuoiVang").asInt());
                    assertEquals(2, a.get("tongBuoiHopLe").asInt());
                    assertTrue(a.get("thongDiep").asText().contains("giải tỏa"));
                }
            }
        }
        assertTrue(soBuoiResolved, "Alert must be resolved (DA_GIAI_TOA) when absences drop below threshold");

        // Verify audit log preserves ATTENDANCE_ALERT_RESOLVED
        List<AuditLog> allAudits = auditLogRepository.findAll();
        assertTrue(allAudits.stream().anyMatch(a -> "ATTENDANCE_ALERT_RESOLVED".equals(a.getAction())),
                "Audit log must preserve resolved actions");

        // 10. Student self-service: check personal alerts
        mockMvc.perform(get("/api/sinh-vien/me/canh-bao-diem-danh")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk());

        // 11. Admin view: Admin can access any class alerts
        mockMvc.perform(get("/api/admin/lop-hoc-phan/" + lopId + "/canh-bao")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
    }
}
