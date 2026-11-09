package com.edu.registration.service;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.repository.AcademicCohortRepository;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SubjectRepository;
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
import java.util.HashMap;
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
class CourseAndSubjectRelationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcademicCohortRepository academicCohortRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PrerequisiteSubjectRepository prerequisiteSubjectRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private CourseClassRepository courseClassRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

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
    void quanTriKhoaHocVaQuanHeMon_CrudLocChongTuThamChieuChuTrinhBaoVeXoaAuditVaKiemTraDangKy() throws Exception {
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");
        String tokenGv01 = login("gv01", "123456");

        // 1. Phân quyền backend: Sinh viên và Giảng viên không được phép truy cập API quản trị khóa học & quan hệ môn
        mockMvc.perform(get("/api/admin/khoa-hoc")
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenGv01))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", 1,
                                "monLienQuanId", 2,
                                "loaiQuanHe", "TIEN_QUYET"
                        ))))
                .andExpect(status().isForbidden());

        // 2. Danh mục loại quan hệ môn học trả về đầy đủ 5 loại và thông tin quy chế đã chốt / chờ chốt
        JsonNode loaiQuanHe = json(mockMvc.perform(get("/api/admin/loai-quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(5, loaiQuanHe.size());

        // 3. Quản lý Khóa học / Niên khóa: Validate năm bắt đầu > năm kết thúc bị từ chối 400
        Semester hk1 = semesterRepository.findAll().get(0);
        Map<String, Object> invalidCohortPayload = Map.of(
                "ma", "K2099",
                "ten", "Khóa 2099",
                "nienKhoa", "2099-2095",
                "namBatDau", 2099,
                "namKetThuc", 2095,
                "dangHieuLuc", true
        );
        mockMvc.perform(post("/api/admin/khoa-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCohortPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Năm bắt đầu không được lớn hơn năm kết thúc"));

        // Tạo khóa học hợp lệ và kiểm tra metadata người tạo + audit log
        Map<String, Object> validCohortPayload = new HashMap<>();
        validCohortPayload.put("ma", "K2030");
        validCohortPayload.put("ten", "Khóa 2030");
        validCohortPayload.put("nienKhoa", "2030-2034");
        validCohortPayload.put("namBatDau", 2030);
        validCohortPayload.put("namKetThuc", 2034);
        validCohortPayload.put("hocKyBatDauId", hk1.getId());
        validCohortPayload.put("hieuLucTu", "2030-09-01");
        validCohortPayload.put("hieuLucDen", "2035-06-30");
        validCohortPayload.put("dangHieuLuc", true);
        validCohortPayload.put("moTa", "Khóa thử nghiệm QLSV-76");

        JsonNode createdCohort = json(mockMvc.perform(post("/api/admin/khoa-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCohortPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ma").value("K2030"))
                .andExpect(jsonPath("$.nguoiTaoUsername").value("admin"))
                .andReturn());
        long cohortId = createdCohort.get("id").asLong();
        assertNotNull(createdCohort.get("taoLuc").asText());

        // Cập nhật khóa học và kiểm tra người cập nhật
        validCohortPayload.put("ten", "Khóa 2030 (Đã cập nhật)");
        mockMvc.perform(put("/api/admin/khoa-hoc/" + cohortId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCohortPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ten").value("Khóa 2030 (Đã cập nhật)"))
                .andExpect(jsonPath("$.nguoiCapNhatUsername").value("admin"));

        // Tìm kiếm/lọc khóa học
        JsonNode filteredCohorts = json(mockMvc.perform(get("/api/admin/khoa-hoc")
                        .param("q", "K2030")
                        .param("dangHieuLuc", "true")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn());
        assertEquals(1, filteredCohorts.size());

        // 4. Tạo 3 môn học phục vụ kiểm thử quan hệ môn học (A, B, C) và kiểm tra metadata người tạo
        Faculty khoa = facultyRepository.findAll().get(0);
        long monAId = json(mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", "REL76A",
                                "ten", "Môn QLSV76 A",
                                "soTinChi", 3,
                                "khoaId", khoa.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nguoiTaoUsername").value("admin"))
                .andReturn()).get("id").asLong();

        long monBId = json(mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", "REL76B",
                                "ten", "Môn QLSV76 B",
                                "soTinChi", 3,
                                "khoaId", khoa.getId()
                        ))))
                .andExpect(status().isOk())
                .andReturn()).get("id").asLong();

        long monCId = json(mockMvc.perform(post("/api/admin/mon-hoc")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "ma", "REL76C",
                                "ten", "Môn QLSV76 C",
                                "soTinChi", 3,
                                "khoaId", khoa.getId()
                        ))))
                .andExpect(status().isOk())
                .andReturn()).get("id").asLong();

        // 5. Không cho môn tự tham chiếu (monHocId == monLienQuanId)
        mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monAId,
                                "monLienQuanId", monAId,
                                "loaiQuanHe", "TIEN_QUYET"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Môn học không thể là tiên quyết của chính nó (không được tự tham chiếu)"));

        // 6. Kiểm tra tạo quan hệ TIEN_QUYET và chặn chu trình nhiều bước (C -> B -> A, rồi thêm A -> C phải bị chặn)
        // C yêu cầu B (B là tiên quyết của C)
        long relCBId = json(mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monCId,
                                "monLienQuanId", monBId,
                                "loaiQuanHe", "TIEN_QUYET",
                                "dieuKienDat", 6.0,
                                "yeuCauDat", true,
                                "khoaNhapHoc", "K2030",
                                "dangHieuLuc", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaiQuanHe").value("TIEN_QUYET"))
                .andExpect(jsonPath("$.dieuKienDat").value(6.0))
                .andExpect(jsonPath("$.nguoiTaoUsername").value("admin"))
                .andReturn()).get("id").asLong();

        // B yêu cầu A (A là tiên quyết của B)
        long relBAId = json(mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monBId,
                                "monLienQuanId", monAId,
                                "loaiQuanHe", "TIEN_QUYET",
                                "dieuKienDat", 5.0,
                                "yeuCauDat", true,
                                "dangHieuLuc", true
                        ))))
                .andExpect(status().isOk())
                .andReturn()).get("id").asLong();

        // Thử tạo A yêu cầu B (chu trình 2 đỉnh A <-> B) -> phải bị chặn 400
        mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monAId,
                                "monLienQuanId", monBId,
                                "loaiQuanHe", "TIEN_QUYET"
                        ))))
                .andExpect(status().isBadRequest());

        // Thử tạo A yêu cầu C (chu trình 3 đỉnh C -> B -> A -> C) -> phải bị chặn 400
        MvcResult cycleRes = mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monAId,
                                "monLienQuanId", monCId,
                                "loaiQuanHe", "TIEN_QUYET"
                        ))))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(json(cycleRes).path("message").asText().contains("chu trình"));

        // 7. Tạo đầy đủ các loại quan hệ khác: SONG_HANH, NHOM_TU_CHON
        long relSongHanhId = json(mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monCId,
                                "monLienQuanId", monAId,
                                "loaiQuanHe", "SONG_HANH",
                                "yeuCauDat", false,
                                "dangHieuLuc", true,
                                "ghiChu", "Học song hành cùng kỳ hoặc đã học trước"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quyCheDaChot").value(false))
                .andReturn()).get("id").asLong();

        long relNhomTuChonId = json(mockMvc.perform(post("/api/admin/quan-he-mon")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monCId,
                                "monLienQuanId", monAId,
                                "loaiQuanHe", "NHOM_TU_CHON",
                                "maNhomTuChon", "TC-76",
                                "tinChiTuChonYeuCau", 6,
                                "dieuKienDat", 5.0,
                                "yeuCauDat", true,
                                "dangHieuLuc", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maNhomTuChon").value("TC-76"))
                .andExpect(jsonPath("$.tinChiTuChonYeuCau").value(6))
                .andReturn()).get("id").asLong();

        // 8. Kiểm tra bảo vệ xóa (409 Conflict) khi quan hệ đang được Chương trình đào tạo hoặc Lớp học phần sử dụng
        Major nganh = majorRepository.findAll().get(0);
        long ctdtId = json(mockMvc.perform(post("/api/admin/chuong-trinh")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "majorId", nganh.getId(),
                                "cohort", "K2030",
                                "version", "CTDT-76-TEST",
                                "groups", List.of(Map.of(
                                        "name", "Nhóm bắt buộc QLSV-76",
                                        "type", "BAT_BUOC",
                                        "requiredCredits", 6,
                                        "subjectIds", List.of(monBId, monCId)
                                ))
                        ))))
                .andExpect(status().isOk())
                .andReturn()).get("id").asLong();

        // Thử xóa quan hệ C -> B đang nằm trong chương trình đào tạo -> phải trả về 409 Conflict và giữ nguyên dữ liệu
        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relCBId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());
        assertTrue(prerequisiteSubjectRepository.findById(relCBId).isPresent(),
                "Quan hệ đang được CTĐT sử dụng phải được giữ nguyên sau khi từ chối xóa");

        // Thử xóa khóa học K2030 đang được CTĐT và quan hệ môn sử dụng -> phải trả về 409 Conflict
        mockMvc.perform(delete("/api/admin/khoa-hoc/" + cohortId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());
        assertTrue(academicCohortRepository.findById(cohortId).isPresent(),
                "Khóa học đang được tham chiếu phải được giữ nguyên");

        // Mở lớp học phần cho môn B và thử xóa quan hệ B -> A -> phải trả về 409 Conflict vì lớp học phần đang sử dụng
        Subject subjectB = subjectRepository.findById(monBId).orElseThrow();
        Subject subjectC = subjectRepository.findById(monCId).orElseThrow();
        Semester openSem = semesterRepository.findAll().stream()
                .filter(Semester::dangMoDangKy)
                .findFirst()
                .orElseGet(() -> semesterRepository.findAll().get(0));
        Lecturer gv = lecturerRepository.findAll().get(0);
        CourseClass lopB = new CourseClass();
        lopB.setMa("REL76B.01");
        lopB.setMonHoc(subjectB);
        lopB.setHocKy(openSem);
        lopB.setGiangVien(gv);
        lopB.setPhong("P761");
        lopB.setThu(4);
        lopB.setTietBatDau(7);
        lopB.setSoTiet(3);
        lopB.setSiSoToiDa(40);
        lopB = courseClassRepository.save(lopB);

        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relBAId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isConflict());
        assertTrue(prerequisiteSubjectRepository.findById(relBAId).isPresent(),
                "Quan hệ đang được lớp học phần sử dụng phải được giữ nguyên");

        // 9. Kiểm tra đăng ký học phần với quan hệ đã cấu hình:
        // Mở lớp cho môn C (có TIEN_QUYET là B với ngưỡng 6.0 cho khóa K2030, và SONG_HANH + NHOM_TU_CHON với A)
        CourseClass lopC = new CourseClass();
        lopC.setMa("REL76C.01");
        lopC.setMonHoc(subjectC);
        lopC.setHocKy(openSem);
        lopC.setGiangVien(gv);
        lopC.setPhong("P762");
        lopC.setThu(5);
        lopC.setTietBatDau(7);
        lopC.setSoTiet(3);
        lopC.setSiSoToiDa(40);
        lopC = courseClassRepository.save(lopC);

        // Sinh viên sv01 thuộc khóa K2024, trong khi quan hệ TIEN_QUYET C->B chỉ áp dụng cho K2030:
        // Kiểm tra API /api/dang-ky/kiem-tra-quan-he cho sv01 trên lớp C:
        // Quan hệ C->B (K2030) không áp dụng cho sv01 (K2024), còn SONG_HANH và NHOM_TU_CHON áp dụng mọi khóa nhưng không tự chặn!
        JsonNode checkForSv01 = json(mockMvc.perform(get("/api/dang-ky/kiem-tra-quan-he")
                        .param("lopHocPhanId", String.valueOf(lopC.getId()))
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duDieuKienTienQuyet").value(true))
                .andReturn());
        assertTrue(checkForSv01.path("danhSachQuanHe").isArray());
        assertEquals(3, checkForSv01.path("danhSachQuanHe").size(),
                "Phải trả về danh sách các quan hệ đã cấu hình kèm trạng thái quy chế");

        // Bây giờ cập nhật quan hệ TIEN_QUYET C->B sang áp dụng cho mọi khóa (khoaNhapHoc = null) với ngưỡng đạt 6.5:
        mockMvc.perform(put("/api/admin/quan-he-mon/" + relCBId)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "monHocId", monCId,
                                "monLienQuanId", monBId,
                                "loaiQuanHe", "TIEN_QUYET",
                                "dieuKienDat", 6.5,
                                "yeuCauDat", true,
                                "dangHieuLuc", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dieuKienDat").value(6.5))
                .andExpect(jsonPath("$.nguoiCapNhatUsername").value("admin"));

        // Kiểm tra lại /api/dang-ky/kiem-tra-quan-he: lúc này sv01 chưa học môn B nên duDieuKienTienQuyet = false
        mockMvc.perform(get("/api/dang-ky/kiem-tra-quan-he")
                        .param("lopHocPhanId", String.valueOf(lopC.getId()))
                        .header("Authorization", "Bearer " + tokenSv01))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duDieuKienTienQuyet").value(false));

        // Sinh viên sv01 thử đăng ký lớp C -> bị chặn 400 vì chưa đạt môn tiên quyết REL76B (yêu cầu >= 6.5)
        MvcResult regBlocked = mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", lopC.getId()))))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertTrue(json(regBlocked).path("message").asText().contains("REL76B"));

        // Dọn dữ liệu kiểm thử tạm để không ảnh hưởng các test khác
        courseClassRepository.delete(lopC);
        courseClassRepository.delete(lopB);
        curriculumRepository.deleteById(ctdtId);

        // Sau khi không còn CTĐT hay lớp học phần sử dụng, xóa quan hệ và khóa học thành công
        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relSongHanhId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relNhomTuChonId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relCBId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/quan-he-mon/" + relBAId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/khoa-hoc/" + cohortId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        // Kiểm tra Audit Log đã ghi nhận các hành động COHORT_CREATED, COHORT_UPDATED, COHORT_DELETED, SUBJECT_RELATION_UPDATED
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a -> "COHORT_CREATED".equals(a.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a -> "COHORT_UPDATED".equals(a.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a -> "COHORT_DELETED".equals(a.getAction())));
        assertTrue(auditLogRepository.findAll().stream().anyMatch(a -> "SUBJECT_RELATION_UPDATED".equals(a.getAction())));
    }
}
