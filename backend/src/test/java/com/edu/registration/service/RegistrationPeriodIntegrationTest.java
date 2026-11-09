package com.edu.registration.service;

import com.edu.registration.entity.AcademicCohort;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Curriculum;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.RegistrationPeriod;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.repository.AcademicCohortRepository;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.RegistrationPeriodRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalDateTime;
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
class RegistrationPeriodIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationPeriodRepository registrationPeriodRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private AcademicCohortRepository academicCohortRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private AdministrativeClassRepository administrativeClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CourseClassRepository courseClassRepository;

    @Autowired
    private LecturerRepository lecturerRepository;

    @Autowired
    private CourseRegistrationRepository courseRegistrationRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

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
    void testRegistrationPeriodFlow_CohortTargeting_Adjustment_AndDirectRejection() throws Exception {
        String tokenAdmin = login("admin", "admin123");
        String tokenSv01 = login("sv01", "123456");
        String tokenSv02 = login("sv02", "123456");

        Student sv01 = studentRepository.findByMa("SV001").orElseThrow();
        Student sv02 = studentRepository.findByMa("SV002").orElseThrow();

        // 1. Tạo học kỳ thử nghiệm
        Semester testSemester = new Semester();
        testSemester.setTen("Học kỳ Test QLSV-77");
        testSemester.setNamHoc("2026-2027");
        testSemester.setNgayBatDau(LocalDate.now().minusDays(10));
        testSemester.setNgayKetThuc(LocalDate.now().plusMonths(4));
        testSemester.setDangKyTu(LocalDate.now().minusDays(5));
        testSemester.setDangKyDen(LocalDate.now().plusDays(10));
        testSemester.setSoTinChiToiThieu(2);
        testSemester.setSoTinChiToiDa(24);
        testSemester = semesterRepository.save(testSemester);

        // Tạo môn học & lớp học phần
        Faculty faculty = facultyRepository.findAll().get(0);
        Subject subject = new Subject();
        subject.setMa("QLSV77_SUBJ");
        subject.setTen("Môn học thử nghiệm QLSV-77");
        subject.setSoTinChi(3);
        subject.setKhoa(faculty);
        subject = subjectRepository.save(subject);

        CourseClass section = new CourseClass();
        section.setMa("QLSV77_CLASS_01");
        section.setMonHoc(subject);
        section.setHocKy(testSemester);
        section.setPhong("P77");
        section.setThu(2);
        section.setTietBatDau(1);
        section.setSoTiet(3);
        section.setSiSoToiDa(40);
        section.setSiSoHienTai(0);
        section.setGiangVien(lecturerRepository.findAll().get(0));
        section = courseClassRepository.save(section);

        // Đảm bảo sv01 có chương trình gắn với cohort K2024
        AcademicCohort cohortK24 = academicCohortRepository.findByMaIgnoreCase("K2024").orElse(null);
        if (cohortK24 == null) {
            cohortK24 = new AcademicCohort();
            cohortK24.setMa("K2024");
            cohortK24.setTen("Khóa 2024");
            cohortK24.setNienKhoa("2024-2028");
            cohortK24.setNamBatDau(2024);
            cohortK24.setNamKetThuc(2028);
            cohortK24 = academicCohortRepository.save(cohortK24);
        }

        Major majorSv1 = sv01.getNganh() != null ? sv01.getNganh() : majorRepository.findAll().get(0);
        sv01.setNganh(majorSv1);
        Curriculum ctSv1 = curriculumRepository.findAll().stream()
                .filter(c -> c.getNganh().getId().equals(majorSv1.getId()) && "K2024".equalsIgnoreCase(c.getKhoaNhapHoc()))
                .findFirst().orElse(null);
        if (ctSv1 == null) {
            ctSv1 = new Curriculum();
            ctSv1.setNganh(majorSv1);
            ctSv1.setKhoaNhapHoc("K2024");
            ctSv1.setPhienBan("v77-test");
            ctSv1 = curriculumRepository.save(ctSv1);
        }
        sv01.setChuongTrinh(ctSv1);
        sv01 = studentRepository.save(sv01);

        // sv02 gắn với cohort khác: K2026
        Major majorSv2 = sv02.getNganh() != null ? sv02.getNganh() : majorRepository.findAll().get(0);
        sv02.setNganh(majorSv2);
        Curriculum ctSv2 = curriculumRepository.findAll().stream()
                .filter(c -> c.getNganh().getId().equals(majorSv2.getId()) && "K2026".equalsIgnoreCase(c.getKhoaNhapHoc()))
                .findFirst().orElse(null);
        if (ctSv2 == null) {
            ctSv2 = new Curriculum();
            ctSv2.setNganh(majorSv2);
            ctSv2.setKhoaNhapHoc("K2026");
            ctSv2.setPhienBan("v77-test-26");
            ctSv2 = curriculumRepository.save(ctSv2);
        }
        sv02.setChuongTrinh(ctSv2);
        sv02 = studentRepository.save(sv02);

        // 2. Admin cấu hình đợt đăng ký CHỈ DÀNH CHO K2024 (bắt đầu từ hôm qua, kết thúc ngày mai)
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> createPayload = Map.of(
                "hocKyId", testSemester.getId(),
                "maDot", "DOT_K24_CHINH_THUC",
                "tenDot", "Đợt 1 - Khóa 2024",
                "loaiDot", "CHINH_THUC",
                "khoaHocId", cohortK24.getId(),
                "khoaNhapHoc", "K2024",
                "thuTuUuTien", 1,
                "thoiGianBatDau", now.minusDays(1).toString(),
                "thoiGianKetThuc", now.plusDays(2).toString(),
                "ghiChu", "Cấu hình thử nghiệm QLSV-77"
        );

        MvcResult createRes = mockMvc.perform(post("/api/admin/dot-dang-ky")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maDot").value("DOT_K24_CHINH_THUC"))
                .andExpect(jsonPath("$.dangMo").value(true))
                .andReturn();

        long dotId = json(createRes).get("id").asLong();

        // Kiểm tra audit log đã ghi nhận REGISTRATION_PERIOD_CREATED
        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> "REGISTRATION_PERIOD_CREATED".equals(a.getAction())
                        && String.valueOf(dotId).equals(a.getEntityId())));

        // 3. Sinh viên K2024 (sv001) kiểm tra trạng thái đợt đăng ký của mình
        mockMvc.perform(get("/api/dang-ky/dot-hien-tai")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .param("hocKyId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thuocDoiTuong").value(true))
                .andExpect(jsonPath("$.dangMoChoSinhVien").value(true))
                .andExpect(jsonPath("$.maDot").value("DOT_K24_CHINH_THUC"))
                .andExpect(jsonPath("$.timezone").value("Asia/Ho_Chi_Minh (UTC+7)"));

        // 4. Sinh viên K2026 (sv002) tra cứu trạng thái đợt đăng ký -> KHONG_THUOC_DOI_TUONG
        mockMvc.perform(get("/api/dang-ky/dot-hien-tai")
                        .header("Authorization", "Bearer " + tokenSv02)
                        .param("hocKyId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thuocDoiTuong").value(false))
                .andExpect(jsonPath("$.dangMoChoSinhVien").value(false))
                .andExpect(jsonPath("$.trangThai").value("KHONG_THUOC_DOI_TUONG"));

        // 5. Backend Validation: sv002 gọi API POST /api/dang-ky trực tiếp -> BỊ TỪ CHỐI BỞI BACKEND
        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenSv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", section.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("không thuộc đối tượng được đăng ký")));

        // 6. sv001 (thuộc đối tượng) gọi API đăng ký -> THÀNH CÔNG
        MvcResult regRes = mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", section.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maLop").value("QLSV77_CLASS_01"))
                .andReturn();

        long regId = json(regRes).get("id").asLong();

        // 7. Test điều chỉnh đợt: Gia hạn (Extend)
        LocalDateTime newEndTime = now.plusDays(5);
        mockMvc.perform(post("/api/admin/dot-dang-ky/" + dotId + "/gia-han")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "thoiGianKetThucMoi", newEndTime.toString(),
                                "lyDo", "Gia hạn thêm 3 ngày cho sinh viên K2024 hoàn tất"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lyDoDieuChinh").value(org.hamcrest.Matchers.containsString("Gia hạn thêm 3 ngày")));

        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> "REGISTRATION_PERIOD_EXTENDED".equals(a.getAction())
                        && String.valueOf(dotId).equals(a.getEntityId())));

        // 8. Test điều chỉnh đợt: Đóng sớm (Close early)
        mockMvc.perform(post("/api/admin/dot-dang-ky/" + dotId + "/dong-som")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lyDo", "Đóng sớm do bảo trì hệ thống cổng đào tạo"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dongSom").value(true))
                .andExpect(jsonPath("$.trangThaiHienThi").value("DONG_SOM"));

        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> "REGISTRATION_PERIOD_CLOSED_EARLY".equals(a.getAction())
                        && String.valueOf(dotId).equals(a.getEntityId())));

        // Khi đã đóng sớm, sv001 không thể đăng ký thêm môn khác hoặc hủy đăng ký
        Subject subject2 = new Subject();
        subject2.setMa("QLSV77_SUBJ2");
        subject2.setTen("Môn học 2");
        subject2.setSoTinChi(3);
        subject2.setKhoa(faculty);
        subject2 = subjectRepository.save(subject2);

        CourseClass section2 = new CourseClass();
        section2.setMa("QLSV77_CLASS_02");
        section2.setMonHoc(subject2);
        section2.setHocKy(testSemester);
        section2.setPhong("P77-B");
        section2.setThu(3);
        section2.setTietBatDau(1);
        section2.setSoTiet(3);
        section2.setSiSoToiDa(40);
        section2.setSiSoHienTai(0);
        section2.setGiangVien(lecturerRepository.findAll().get(0));
        section2 = courseClassRepository.save(section2);

        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", section2.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã đóng sớm")));

        // 9. Bảo toàn lịch sử đăng ký cũ: sv001 vẫn xem được danh sách lớp đã đăng ký và lịch sử đăng ký bình thường
        mockMvc.perform(get("/api/dang-ky/me")
                        .header("Authorization", "Bearer " + tokenSv01)
                        .param("hocKyId", String.valueOf(testSemester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].maLop").value("QLSV77_CLASS_01"));

        // 10. Mở đợt bổ sung (Supplemental registration window)
        mockMvc.perform(post("/api/admin/dot-dang-ky/mo-bo-sung")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "hocKyId", testSemester.getId(),
                                "maDot", "DOT_BO_SUNG_TOAN_TRUONG",
                                "tenDot", "Đợt bổ sung toàn trường",
                                "thuTuUuTien", 2,
                                "thoiGianBatDau", now.minusMinutes(30).toString(),
                                "thoiGianKetThuc", now.plusDays(1).toString(),
                                "lyDo", "Mở bổ sung cho sinh viên toàn trường đăng ký vét học phần"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaiDot").value("BO_SUNG"))
                .andExpect(jsonPath("$.dangMo").value(true));

        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(a -> "REGISTRATION_PERIOD_SUPPLEMENTAL_OPENED".equals(a.getAction())));

        // Bây giờ sv002 (K2026) cũng thuộc đợt bổ sung toàn trường và có thể đăng ký được!
        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenSv02)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", section2.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maLop").value("QLSV77_CLASS_02"));
    }
}
