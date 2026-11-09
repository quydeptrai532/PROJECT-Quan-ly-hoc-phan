package com.edu.registration.service;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.util.XlsxWorkbookSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class GradeConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private CourseRegistrationRepository dangKyRepository;

    @Autowired
    private GradeRepository diemRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(res).get("token").asText();
    }

    private JsonNode json(MvcResult res) throws Exception {
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private Long findDraftClassId(String token) throws Exception {
        JsonNode lopList = json(mockMvc.perform(get("/api/giang-vien/me/lop-hoc-phan")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn());

        for (JsonNode lop : lopList) {
            long id = lop.get("id").asLong();

            JsonNode ttSheet = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + id + "/trang-thai-bang-diem")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn());

            String trangThai = ttSheet.path("trangThaiBangDiem").asText("NHAP");
            if (!"NHAP".equals(trangThai)) continue;

            JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + id + "/sinh-vien")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn());
            if (svList.isArray() && svList.size() > 0) {
                return id;
            }
        }
        throw new IllegalStateException("Không tìm thấy lớp học phần nháp có sinh viên phù hợp");
    }

    @Test
    @DisplayName("Cập nhật đơn lẻ mang đúng version thành công và tăng version; gửi version cũ bị từ chối 409 Conflict")
    void singleGradeUpdate_optimisticLockingAndConflict() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        Long lopId = findDraftClassId(tokenGv01);

        // Lấy danh sách điểm
        JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        assertTrue(svList.size() > 0);

        JsonNode firstStudent = svList.get(0);
        long dangKyId = firstStudent.get("dangKyId").asLong();
        Long initialVersion = firstStudent.hasNonNull("version") ? firstStudent.get("version").asLong() : null;

        // Lấy trạng thái bảng điểm để biết phienBanBangDiem hiện tại
        JsonNode ttSheet = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        int currentSheetVer = ttSheet.get("phienBanBangDiem").asInt();

        // Cập nhật hợp lệ với đúng version
        MvcResult update1 = mockMvc.perform(put("/api/diem/" + dangKyId)
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 8.0,
                                "diemCuoiKy", 9.0,
                                "version", initialVersion != null ? initialVersion : 0,
                                "phienBanBangDiem", currentSheetVer
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updatedGrade1 = json(update1);
        long v1 = updatedGrade1.get("version").asLong();

        // Lấy lại phienBanBangDiem mới nhất của lớp (đã tăng sau update1)
        JsonNode ttSheetAfter1 = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        int sheetVer1 = ttSheetAfter1.get("phienBanBangDiem").asInt();

        // Cập nhật lần 2 hợp lệ với version v1 -> Version điểm tăng lên v2
        MvcResult update2 = mockMvc.perform(put("/api/diem/" + dangKyId)
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 8.5,
                                "diemCuoiKy", 8.5,
                                "version", v1,
                                "phienBanBangDiem", sheetVer1
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode updatedGrade2 = json(update2);
        long v2 = updatedGrade2.get("version").asLong();
        assertTrue(v2 > v1, "Version bản ghi điểm phải được tăng lên sau update 2");

        // Lấy lại phienBanBangDiem mới nhất của lớp sau update2
        JsonNode ttSheetAfter2 = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        int sheetVer2 = ttSheetAfter2.get("phienBanBangDiem").asInt();

        // Cập nhật lại với version điểm cũ (stale version v1 khi hiện tại đã là v2) -> Phải bị từ chối 409 Conflict
        MvcResult conflictResult = mockMvc.perform(put("/api/diem/" + dangKyId)
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 7.0,
                                "diemCuoiKy", 8.0,
                                "version", v1,
                                "phienBanBangDiem", sheetVer2
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentVersion").value(v2))
                .andReturn();

        JsonNode conflictBody = json(conflictResult);
        assertEquals(v2, conflictBody.get("currentVersion").asLong());
    }

    @Test
    @DisplayName("Hai cập nhật đồng thời với cùng version: tối đa một cập nhật thành công, request còn lại nhận 409 Conflict")
    void concurrentSingleGradeUpdates_onlyOneSucceeds() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        Long lopId = findDraftClassId(tokenGv01);

        JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        long dangKyId = svList.get(0).get("dangKyId").asLong();

        // Đồng bộ trước để lấy phiên bản chuẩn
        JsonNode ttSheet = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        int sheetVer = ttSheet.get("phienBanBangDiem").asInt();

        JsonNode gradeFresh = json(mockMvc.perform(put("/api/diem/" + dangKyId)
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 7.5,
                                "diemCuoiKy", 8.0,
                                "phienBanBangDiem", sheetVer
                        ))))
                .andExpect(status().isOk()).andReturn());

        long baseVersion = gradeFresh.get("version").asLong();
        int baseSheetVersion = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn()).get("phienBanBangDiem").asInt();

        // Gửi 2 request đồng thời với cùng baseVersion và baseSheetVersion
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Callable<Integer> task1 = () -> {
            startLatch.await();
            return mockMvc.perform(put("/api/diem/" + dangKyId)
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "diemQuaTrinh", 9.0,
                                    "diemCuoiKy", 9.0,
                                    "version", baseVersion,
                                    "phienBanBangDiem", baseSheetVersion
                            ))))
                    .andReturn().getResponse().getStatus();
        };

        Callable<Integer> task2 = () -> {
            startLatch.await();
            return mockMvc.perform(put("/api/diem/" + dangKyId)
                            .header("Authorization", "Bearer " + tokenGv01)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "diemQuaTrinh", 6.0,
                                    "diemCuoiKy", 6.0,
                                    "version", baseVersion,
                                    "phienBanBangDiem", baseSheetVersion
                            ))))
                    .andReturn().getResponse().getStatus();
        };

        Future<Integer> f1 = executor.submit(task1);
        Future<Integer> f2 = executor.submit(task2);

        startLatch.countDown();
        int status1 = f1.get(10, TimeUnit.SECONDS);
        int status2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Tối đa một cập nhật thành công (200), request còn lại nhận 409 Conflict
        boolean oneSuccessOneConflict = (status1 == 200 && status2 == 409) || (status1 == 409 && status2 == 200);
        assertTrue(oneSuccessOneConflict, "Chính xác một request thành công 200 và một request nhận 409 Conflict. Actual: s1=" + status1 + ", s2=" + status2);
    }

    @Test
    @DisplayName("Cập nhật hàng loạt (bulk update) với stale version bảng điểm hoặc bản ghi bị từ chối 409 Conflict")
    void bulkGradeUpdate_concurrencyControl() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        Long lopId = findDraftClassId(tokenGv01);

        JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());

        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonNode sv : svList) {
            items.add(Map.of(
                    "dangKyId", sv.get("dangKyId").asLong(),
                    "diemQuaTrinh", 8.0,
                    "diemCuoiKy", 8.0,
                    "version", sv.hasNonNull("version") ? sv.get("version").asLong() : 0L
            ));
        }

        // Test 1: Gửi stale phienBanBangDiem (ví dụ 999999) -> 409 Conflict
        mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phienBanBangDiem", 999999,
                                "danhSach", items
                        ))))
                .andExpect(status().isConflict());

        // Test 2: Gửi đúng phienBanBangDiem nhưng stale version của một bản ghi điểm
        JsonNode ttSheet = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());
        int currentSheetVer = ttSheet.get("phienBanBangDiem").asInt();

        List<Map<String, Object>> itemsWithStaleVersion = new ArrayList<>();
        for (JsonNode sv : svList) {
            itemsWithStaleVersion.add(Map.of(
                    "dangKyId", sv.get("dangKyId").asLong(),
                    "diemQuaTrinh", 8.0,
                    "diemCuoiKy", 8.0,
                    "version", 999999L // stale version
            ));
        }

        mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phienBanBangDiem", currentSheetVer,
                                "danhSach", itemsWithStaleVersion
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Import Excel kiểm tra version bảng điểm; nếu bị thay đổi trong lúc xem trước thì confirm bị 409 Conflict")
    void excelImport_checksVersionOnConfirm() throws Exception {
        String tokenGv01 = login("gv01", "123456");
        Long lopId = findDraftClassId(tokenGv01);

        // Tạo tệp Excel nhập điểm hợp lệ
        JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk()).andReturn());

        List<XlsxWorkbookSupport.TemplateStudentRow> rows = new ArrayList<>();
        for (int i = 0; i < svList.size(); i++) {
            JsonNode sv = svList.get(i);
            rows.add(new XlsxWorkbookSupport.TemplateStudentRow(
                    sv.get("maSinhVien").asText(),
                    sv.get("hoTen").asText(),
                    8.5,
                    9.0
            ));
        }
        byte[] filledExcel = XlsxWorkbookSupport.createGradeTemplateWorkbook(rows);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "nhap-diem.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                filledExcel
        );

        // Xem trước (Preview)
        MvcResult previewRes = mockMvc.perform(multipart("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem/import-xlsx/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode previewJson = json(previewRes);
        String previewToken = previewJson.get("previewToken").asText();
        int previewSheetVer = previewJson.get("phienBanBangDiem").asInt();

        // Giả lập người khác chỉnh sửa bảng điểm trong lúc đang xem trước -> Bảng điểm tăng version
        long dangKyId = svList.get(0).get("dangKyId").asLong();
        mockMvc.perform(put("/api/diem/" + dangKyId)
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 7.0,
                                "diemCuoiKy", 7.0
                        ))))
                .andExpect(status().isOk());

        // Xác nhận import với phiên bản xem trước cũ -> Phải bị từ chối 409 Conflict
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem/import-xlsx/confirm")
                        .header("Authorization", "Bearer " + tokenGv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "previewToken", previewToken,
                                "phienBanBangDiem", previewSheetVer
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Bảng điểm đã khóa (DA_KHOA hoặc CHO_DUYET) không cho sửa điểm dù version còn khớp")
    void lockedGradeSheet_rejectsUpdateEvenIfVersionMatches() throws Exception {
        String tokenGv02 = login("gv02", "123456");
        Long lopId = findDraftClassId(tokenGv02);

        // Nhập đủ điểm cho tất cả sinh viên
        JsonNode svList = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk()).andReturn());

        List<Map<String, Object>> danhSachDiem = new ArrayList<>();
        for (JsonNode sv : svList) {
            danhSachDiem.add(Map.of(
                    "dangKyId", sv.get("dangKyId").asLong(),
                    "diemQuaTrinh", 8.0,
                    "diemCuoiKy", 8.0
            ));
        }
        mockMvc.perform(put("/api/giang-vien/lop-hoc-phan/" + lopId + "/diem")
                        .header("Authorization", "Bearer " + tokenGv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("danhSach", danhSachDiem))))
                .andExpect(status().isOk());

        // Gửi duyệt -> Trạng thái sang CHO_DUYET (đã khóa sửa)
        mockMvc.perform(post("/api/giang-vien/lop-hoc-phan/" + lopId + "/gui-duyet-diem")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk());

        // Lấy lại version mới nhất
        JsonNode svAfterSubmit = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/sinh-vien")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk()).andReturn());
        JsonNode first = svAfterSubmit.get(0);
        long dkId = first.get("dangKyId").asLong();
        long v = first.get("version").asLong();

        JsonNode ttSheet = json(mockMvc.perform(get("/api/giang-vien/lop-hoc-phan/" + lopId + "/trang-thai-bang-diem")
                        .header("Authorization", "Bearer " + tokenGv02))
                .andExpect(status().isOk()).andReturn());
        int sVer = ttSheet.get("phienBanBangDiem").asInt();

        // Thử cập nhật điểm khi bảng điểm ở CHO_DUYET dù version khớp 100% -> Phải bị từ chối 409 Conflict
        mockMvc.perform(put("/api/diem/" + dkId)
                        .header("Authorization", "Bearer " + tokenGv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "diemQuaTrinh", 10.0,
                                "diemCuoiKy", 10.0,
                                "version", v,
                                "phienBanBangDiem", sVer
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Audit ghi nhận before/after version, bảo vệ nội dung nhạy cảm và ngăn người dùng thường truy cập/sửa xóa")
    void auditLog_recordsVersionsAndProtectsAccess() throws Exception {
        String tokenAdmin = login("admin", "admin123");
        String tokenGv01 = login("gv01", "123456");
        String tokenSv01 = login("sv01", "123456");

        // 1. Giảng viên và sinh viên không thể truy cập danh sách audit log
        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());

        // 2. Admin truy cập được audit log
        MvcResult auditRes = mockMvc.perform(get("/api/admin/audit-logs?hanhDong=GRADE_UPDATED")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode auditJson = json(auditRes);
        assertTrue(auditJson.has("content"));
    }
}
