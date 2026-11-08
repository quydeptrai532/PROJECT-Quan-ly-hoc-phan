package com.edu.registration.service;

import com.edu.registration.dto.AttendanceDtos;
import com.edu.registration.entity.AuditLog;
import com.edu.registration.entity.AttendanceStatus;
import com.edu.registration.repository.AuditLogRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AttendanceIntegrationTest {

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
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void fullAttendanceWorkflowAndSecurityVerification() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        String tokenGv02 = login("gv02", "123456");
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");

        // 1. Get gv01's assigned classes in current semester
        JsonNode gv01Classes = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn());
        assertFalse(gv01Classes.isEmpty(), "gv01 should have classes");

        long lopId = -1;
        String lopMa = "";
        LocalDate testDate = null;
        for (JsonNode c : gv01Classes) {
            if ("INT201.01".equals(c.get("ma").asText()) || "INT303.01".equals(c.get("ma").asText())) {
                lopId = c.get("id").asLong();
                lopMa = c.get("ma").asText();
                testDate = LocalDate.now();
                break;
            }
        }
        if (lopId == -1) {
            // fallback: find any class with enrolled students and choose a date inside its semester
            for (JsonNode c : gv01Classes) {
                if (c.get("siSoHienTai").asInt() > 0) {
                    lopId = c.get("id").asLong();
                    lopMa = c.get("ma").asText();
                    break;
                }
            }
            testDate = LocalDate.of(2026, 4, 15);
        }
        assertTrue(lopId > 0, "gv01 should have a class with enrolled students");
        assertNotNull(testDate, "testDate should be determined");
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/buoi-hoc")
                        .header("Authorization", "Bearer " + tokenGv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lopHocPhanId", lopId,
                                "ngayHoc", testDate.toString(),
                                "caHoc", 1,
                                "noiDung", "Buổi 1 nhập môn"
                        ))))
                .andExpect(status().isForbidden());

        // 3. gv01 creates session for their class -> 200 OK
        MvcResult taoRes = mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/buoi-hoc")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lopHocPhanId", lopId,
                                "ngayHoc", testDate.toString(),
                                "caHoc", 1,
                                "noiDung", "Buổi 1 giới thiệu môn học"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.daKhoa").value(false))
                .andReturn();

        JsonNode buoi1 = json(taoRes);
        long buoiId = buoi1.get("id").asLong();

        // 4. Duplicate prevention: creating duplicate (lop, ngay, ca) -> 409 Conflict
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/buoi-hoc")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lopHocPhanId", lopId,
                                "ngayHoc", testDate.toString(),
                                "caHoc", 1,
                                "noiDung", "Buổi trùng lặp"
                        ))))
                .andExpect(status().isConflict());

        // 5. Get attendance detail for buoi1
        MvcResult chiTietRes = mockMvc.perform(get("/api/giang-vien/buoi-hoc/" + buoiId + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode chiTiet = json(chiTietRes);
        JsonNode dsSv = chiTiet.get("danhSachDiemDanh");
        assertFalse(dsSv.isEmpty(), "Enrolled students should appear in attendance sheet");

        long sv1Id = dsSv.get(0).get("sinhVienId").asLong();
        long sv2Id = dsSv.size() > 1 ? dsSv.get(1).get("sinhVienId").asLong() : -1;

        // 6. Record attendance (CO_MAT, VANG, MUON, CO_PHEP)
        List<Map<String, Object>> attendanceList;
        if (sv2Id > 0) {
            attendanceList = List.of(
                    Map.of("sinhVienId", sv1Id, "trangThai", "CO_MAT", "ghiChu", "Đúng giờ"),
                    Map.of("sinhVienId", sv2Id, "trangThai", "MUON", "ghiChu", "Đi muộn 15p")
            );
        } else {
            attendanceList = List.of(
                    Map.of("sinhVienId", sv1Id, "trangThai", "CO_MAT", "ghiChu", "Đúng giờ")
            );
        }

        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoiId + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "danhSach", attendanceList,
                                "khoaBuoi", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buoiHoc.soCoMat").value(1));

        // 7. Update attendance before lock (editing status to CO_PHEP)
        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoiId + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "danhSach", List.of(
                                        Map.of("sinhVienId", sv1Id, "trangThai", "CO_PHEP", "ghiChu", "Có đơn xin phép")
                                ),
                                "khoaBuoi", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buoiHoc.soCoPhep").value(1));

        // 8. Lock session
        mockMvc.perform(post("/api/giang-vien/buoi-hoc/" + buoiId + "/khoa")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("khoa", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daKhoa").value(true));

        // 9. When locked, regular lecturer cannot edit attendance -> 400 Bad Request
        mockMvc.perform(put("/api/giang-vien/buoi-hoc/" + buoiId + "/diem-danh")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "danhSach", List.of(
                                        Map.of("sinhVienId", sv1Id, "trangThai", "VANG", "ghiChu", "Sửa khi đã khóa")
                                )
                        ))))
                .andExpect(status().isBadRequest());

        // 10. When locked, regular lecturer cannot unlock -> 403 Forbidden
        mockMvc.perform(post("/api/giang-vien/buoi-hoc/" + buoiId + "/khoa")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("khoa", false))))
                .andExpect(status().isForbidden());

        // 11. Admin uses special permission to edit locked session / unlock session -> 200 OK
        mockMvc.perform(post("/api/admin/buoi-hoc/" + buoiId + "/khoa")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("khoa", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daKhoa").value(false));

        // 12. Class attendance report endpoint
        MvcResult reportRes = mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/chuyen-can")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lopHocPhanId").value(lopId))
                .andExpect(jsonPath("$.tongSoBuoi").value(1))
                .andExpect(jsonPath("$.congThucApDung").isString())
                .andExpect(jsonPath("$.ghiChuQuyChe").isString())
                .andReturn();
        JsonNode report = json(reportRes);
        assertFalse(report.get("danhSachSinhVien").isEmpty());

        // 13. Student accesses attendance report for current user
        mockMvc.perform(get("/api/sinh-vien/me/chuyen-can")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk());

        // 14. Verify Audit Logs were recorded
        List<AuditLog> auditLogs = auditLogRepository.findAll();
        boolean foundCreated = auditLogs.stream()
                .anyMatch(a -> "ATTENDANCE_SESSION_CREATED".equals(a.getAction()));
        boolean foundRecorded = auditLogs.stream()
                .anyMatch(a -> "ATTENDANCE_RECORDED".equals(a.getAction()));
        boolean foundLocked = auditLogs.stream()
                .anyMatch(a -> "ATTENDANCE_SESSION_LOCKED".equals(a.getAction()));

        assertTrue(foundCreated, "ATTENDANCE_SESSION_CREATED audit log should be saved");
        assertTrue(foundRecorded, "ATTENDANCE_RECORDED audit log should be saved");
        assertTrue(foundLocked, "ATTENDANCE_SESSION_LOCKED audit log should be saved");
    }
}
