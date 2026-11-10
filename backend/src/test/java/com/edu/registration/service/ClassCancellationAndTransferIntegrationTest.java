package com.edu.registration.service;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.entity.ClassTransferHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.StudentAcademicStatus;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SubjectRelationType;
import com.edu.registration.repository.ClassTransferHistoryRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
class ClassCancellationAndTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseClassRepository lopHocPhanRepository;

    @Autowired
    private SemesterRepository hocKyRepository;

    @Autowired
    private SubjectRepository monHocRepository;

    @Autowired
    private LecturerRepository giangVienRepository;

    @Autowired
    private StudentRepository sinhVienRepository;

    @Autowired
    private CourseRegistrationRepository dangKyRepository;

    @Autowired
    private RegistrationHistoryRepository lichSuDangKyRepository;

    @Autowired
    private GradeRepository diemRepository;

    @Autowired
    private PrerequisiteSubjectRepository monTienQuyetRepository;

    @Autowired
    private ClassTransferHistoryRepository classTransferHistoryRepository;

    private String tokenAdmin;
    private String tokenStudent1;
    private Semester hocKy;
    private Subject monHoc1;
    private Subject monHoc2;
    private Lecturer gv1;
    private Lecturer gv2;
    private Student sv1;
    private Student sv2;

    private final List<Long> createdClassIds = new ArrayList<>();
    private final List<Long> createdRegistrationIds = new ArrayList<>();
    private final List<Long> createdPrerequisiteIds = new ArrayList<>();

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

    @BeforeEach
    void setUp() throws Exception {
        tokenAdmin = login("admin", "admin123");
        hocKy = hocKyRepository.findAll().get(0);

        List<Lecturer> gvs = giangVienRepository.findAll();
        gv1 = gvs.get(0);
        gv2 = gvs.size() > 1 ? gvs.get(1) : gv1;

        List<Student> students = sinhVienRepository.findAllByOrderByMaAsc();
        sv1 = students.get(0);
        sv2 = students.size() > 1 ? students.get(1) : sv1;
        sv1.setTrangThaiHocTap(StudentAcademicStatus.DANG_HOC);
        sinhVienRepository.save(sv1);
        tokenStudent1 = login(sv1.getUser().getUsername(), "123456");

        List<Long> enrolledSubjectIds = dangKyRepository.findBySinhVienIdAndLopHocPhanHocKyIdOrderByIdAsc(sv1.getId(), hocKy.getId())
                .stream()
                .map(dk -> dk.getLopHocPhan().getMonHoc().getId())
                .toList();
        List<Subject> subjects = monHocRepository.findAll().stream()
                .filter(m -> !enrolledSubjectIds.contains(m.getId()))
                .toList();
        monHoc1 = subjects.get(0);
        monHoc2 = subjects.size() > 1 ? subjects.get(1) : monHoc1;
    }

    @AfterEach
    void tearDown() {
        for (Long id : createdPrerequisiteIds) {
            monTienQuyetRepository.findById(id).ifPresent(monTienQuyetRepository::delete);
        }
        createdPrerequisiteIds.clear();

        for (Long classId : createdClassIds) {
            classTransferHistoryRepository.findByLopId(classId).forEach(classTransferHistoryRepository::delete);
        }

        for (Long regId : createdRegistrationIds) {
            diemRepository.findByDangKyId(regId).ifPresent(diemRepository::delete);
            dangKyRepository.findById(regId).ifPresent(dangKyRepository::delete);
        }

        for (Long classId : createdClassIds) {
            dangKyRepository.findByLopHocPhan(classId, RegistrationStatus.DA_DANG_KY).forEach(dk -> {
                diemRepository.findByDangKyId(dk.getId()).ifPresent(diemRepository::delete);
                dangKyRepository.delete(dk);
            });
            dangKyRepository.findByLopHocPhan(classId, RegistrationStatus.DA_HUY).forEach(dk -> {
                diemRepository.findByDangKyId(dk.getId()).ifPresent(diemRepository::delete);
                dangKyRepository.delete(dk);
            });
        }

        lichSuDangKyRepository.findAll().stream()
                .filter(h -> h.getLopHocPhan() != null && createdClassIds.contains(h.getLopHocPhan().getId()))
                .forEach(lichSuDangKyRepository::delete);

        for (Long id : createdClassIds) {
            lopHocPhanRepository.findById(id).ifPresent(lopHocPhanRepository::delete);
        }
        createdClassIds.clear();
        createdRegistrationIds.clear();
    }

    private CourseClass taoLopMau(String ma, Subject mon, Lecturer gv, String phong, int thu, int tietBatDau, int soTiet, int siSoToiDa) {
        CourseClass lop = new CourseClass();
        lop.setMa(ma);
        lop.setHocKy(hocKy);
        lop.setMonHoc(mon);
        lop.setGiangVien(gv);
        lop.setPhong(phong);
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(siSoToiDa);
        lop.setSiSoHienTai(0);
        lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
        CourseClass saved = lopHocPhanRepository.save(lop);
        createdClassIds.add(saved.getId());
        return saved;
    }

    private CourseRegistration dangKySinhVien(Student sv, CourseClass lop) {
        CourseRegistration reg = new CourseRegistration();
        reg.setSinhVien(sv);
        reg.setLopHocPhan(lop);
        reg.setTrangThai(RegistrationStatus.DA_DANG_KY);
        reg.setThoiGianDangKy(LocalDateTime.now());
        CourseRegistration saved = dangKyRepository.save(reg);
        createdRegistrationIds.add(saved.getId());

        lop.setSiSoHienTai(lop.getSiSoHienTai() + 1);
        lopHocPhanRepository.save(lop);

        RegistrationHistory history = new RegistrationHistory();
        history.setSinhVien(sv);
        history.setLopHocPhan(lop);
        history.setHocKyId(lop.getHocKy().getId());
        history.setDangKyId(saved.getId());
        history.setTrangThai(RegistrationStatus.DA_DANG_KY);
        history.setThoiGianDangKy(saved.getThoiGianDangKy());
        history.setThoiDiem(LocalDateTime.now());
        lichSuDangKyRepository.save(history);

        return saved;
    }

    @Test
    void khaoSatHuyLopTraVeDayDuTacDongVaLopDichKhaDi() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_KS_01", monHoc1, gv1, "P.101", 2, 1, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_KS_02", monHoc1, gv2, "P.102", 3, 1, 3, 30);
        dangKySinhVien(sv1, lopNguon);

        MvcResult res = mockMvc.perform(get("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/khao-sat-huy")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(res);
        assertEquals(lopNguon.getId().longValue(), json.get("lopHocPhanId").asLong());
        assertEquals("QLSV79_KS_01", json.get("maLop").asText());
        assertEquals(1, json.get("siSoHienTai").asInt());
        assertFalse(json.get("coDiem").asBoolean());
        assertTrue(json.get("choPhepPhaoHuy").asBoolean());
        assertEquals(1, json.get("danhSachSinhVien").size());
        assertTrue(json.get("danhSachLopDichKhaDi").size() >= 1);
    }

    @Test
    void huyLopHocPhanThanhCong_KhongXoaDuLieu_VaChanDangKyMoi() throws Exception {
        CourseClass lop = taoLopMau("QLSV79_HUY_01", monHoc1, gv1, "P.201", 4, 1, 3, 30);
        CourseRegistration reg = dangKySinhVien(sv1, lop);

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lop.getId() + "/huy")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Lớp không đủ sĩ số mở lớp"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soDangKyBiHuy").value(1))
                .andExpect(jsonPath("$.lyDo").value("Lớp không đủ sĩ số mở lớp"))
                .andReturn();

        // 1. Kiểm tra lớp đã bị hủy
        CourseClass updatedLop = lopHocPhanRepository.findById(lop.getId()).orElseThrow();
        assertTrue(updatedLop.isDaHuy());
        assertEquals(0, updatedLop.getSiSoHienTai());
        assertEquals("Lớp không đủ sĩ số mở lớp", updatedLop.getLyDoHuy());
        assertNotNull(updatedLop.getThoiGianHuy());

        // 2. Không xóa bản ghi đăng ký cũ; trạng thái chuyển thành DA_HUY
        CourseRegistration updatedReg = dangKyRepository.findById(reg.getId()).orElseThrow();
        assertEquals(RegistrationStatus.DA_HUY, updatedReg.getTrangThai());

        // 3. Lớp đã hủy không nhận đăng ký mới
        mockMvc.perform(post("/api/dang-ky")
                        .header("Authorization", "Bearer " + tokenStudent1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lopHocPhanId", lop.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã bị hủy, không tiếp nhận đăng ký mới")));
    }

    @Test
    void chanHuyLopKhiLopDaNhapDiem() throws Exception {
        CourseClass lop = taoLopMau("QLSV79_HUY_DIEM_01", monHoc1, gv1, "P.202", 5, 1, 3, 30);
        CourseRegistration reg = dangKySinhVien(sv1, lop);

        // Nhập điểm cho sinh viên
        Grade grade = new Grade();
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(8.5);
        grade.setDiemCuoiKy(9.0);
        grade.setDiemTongKet(8.8);
        diemRepository.save(grade);

        // Thao tác hủy lớp bị từ chối an toàn với 409 Conflict
        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lop.getId() + "/huy")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("lyDo", "Hủy lớp đột xuất"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã có điểm đã nhập")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Quyết định cần chốt")));

        // Dữ liệu lớp và đăng ký giữ nguyên 100%
        CourseClass lopSau = lopHocPhanRepository.findById(lop.getId()).orElseThrow();
        assertFalse(lopSau.isDaHuy());
        assertEquals(1, lopSau.getSiSoHienTai());
    }

    @Test
    void chuyenSinhVienCungMonThanhCong_CapNhatSiSoVaLuuLichSu() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_SRC_01", monHoc1, gv1, "P.301", 6, 10, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_TGT_01", monHoc1, gv2, "P.302", 7, 10, 3, 30);
        CourseRegistration regNguon = dangKySinhVien(sv1, lopNguon);

        MvcResult res = mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId(),
                                "lyDo", "Điều chỉnh sĩ số lớp học phần"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maLopNguon").value("QLSV79_SRC_01"))
                .andExpect(jsonPath("$.maLopDich").value("QLSV79_TGT_01"))
                .andExpect(jsonPath("$.loaiQuanHeMon").value("CUNG_MON"))
                .andReturn();

        // 1. Kiểm tra sĩ số hai lớp
        CourseClass updatedNguon = lopHocPhanRepository.findById(lopNguon.getId()).orElseThrow();
        CourseClass updatedDich = lopHocPhanRepository.findById(lopDich.getId()).orElseThrow();
        assertEquals(0, updatedNguon.getSiSoHienTai());
        assertEquals(1, updatedDich.getSiSoHienTai());

        // 2. Kiểm tra trạng thái đăng ký nguồn và đích
        CourseRegistration regSauNguon = dangKyRepository.findById(regNguon.getId()).orElseThrow();
        assertEquals(RegistrationStatus.DA_HUY, regSauNguon.getTrangThai());

        CourseRegistration regSauDich = dangKyRepository.findBySinhVienIdAndLopHocPhanId(sv1.getId(), lopDich.getId()).orElseThrow();
        assertEquals(RegistrationStatus.DA_DANG_KY, regSauDich.getTrangThai());

        // 3. Kiểm tra lưu lịch sử chuyển lớp
        List<ClassTransferHistory> histories = classTransferHistoryRepository.findByLopId(lopNguon.getId());
        assertFalse(histories.isEmpty());
        assertEquals("Điều chỉnh sĩ số lớp học phần", histories.get(0).getLyDo());
        assertEquals("CUNG_MON", histories.get(0).getLoaiQuanHeMon());
    }

    @Test
    void chuyenSinhVienSangMonTuongDuongThanhCong() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_TD_SRC", monHoc1, gv1, "P.401", 6, 7, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_TD_TGT", monHoc2, gv2, "P.402", 7, 7, 3, 30);
        dangKySinhVien(sv1, lopNguon);

        // Thiết lập quan hệ TUONG_DUONG giữa monHoc1 và monHoc2
        PrerequisiteSubject relation = new PrerequisiteSubject();
        relation.setMonHoc(monHoc1);
        relation.setMonTienQuyet(monHoc2);
        relation.setLoaiQuanHe(SubjectRelationType.TUONG_DUONG);
        relation.setDangHieuLuc(true);
        relation.setHocKyHieuLucTu(hocKy);
        PrerequisiteSubject savedRelation = monTienQuyetRepository.save(relation);
        createdPrerequisiteIds.add(savedRelation.getId());

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId(),
                                "lyDo", "Chuyển sang môn tương đương tương thích"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaiQuanHeMon").value("TUONG_DUONG"));

        CourseClass updatedNguon = lopHocPhanRepository.findById(lopNguon.getId()).orElseThrow();
        CourseClass updatedDich = lopHocPhanRepository.findById(lopDich.getId()).orElseThrow();
        assertEquals(0, updatedNguon.getSiSoHienTai());
        assertEquals(1, updatedDich.getSiSoHienTai());
    }

    @Test
    void chanChuyenKhiLopDichKhongCungMonVaKhongTuongDuong() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_ERR_SRC", monHoc1, gv1, "P.501", 4, 1, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_ERR_TGT", monHoc2, gv2, "P.502", 5, 1, 3, 30);
        dangKySinhVien(sv1, lopNguon);

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("không cùng môn và không phải môn tương đương")));
    }

    @Test
    void chanChuyenKhiLopDichHetCho() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_CAP_SRC", monHoc1, gv1, "P.601", 2, 4, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_CAP_TGT", monHoc1, gv2, "P.602", 3, 4, 3, 1);
        dangKySinhVien(sv1, lopNguon);
        dangKySinhVien(sv2, lopDich); // Lớp đích đã đủ sĩ số 1/1

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã đủ sĩ số")));
    }

    @Test
    void chanChuyenKhiSinhVienBiTrungLich() throws Exception {
        // Sinh viên sv1 đang học lớp nguồn thứ 2 tiết 1-3
        CourseClass lopNguon = taoLopMau("QLSV79_CONFL_SRC", monHoc1, gv1, "P.701", 2, 1, 3, 30);
        // Sinh viên sv1 cũng đang có một lớp môn khác vào thứ 3 tiết 1-3
        CourseClass lopKhac = taoLopMau("QLSV79_CONFL_OTHER", monHoc2, gv1, "P.702", 3, 1, 3, 30);
        // Lớp đích cùng môn 1 nhưng trùng khung giờ thứ 3 tiết 1-3
        CourseClass lopDich = taoLopMau("QLSV79_CONFL_TGT", monHoc1, gv2, "P.703", 3, 1, 3, 30);

        dangKySinhVien(sv1, lopNguon);
        dangKySinhVien(sv1, lopKhac);

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("trùng lịch")));
    }

    @Test
    void chanChuyenKhiSinhVienDaCoDiemTaiLopNguon() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_GRD_SRC", monHoc1, gv1, "P.801", 2, 10, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_GRD_TGT", monHoc1, gv2, "P.802", 3, 10, 3, 30);
        CourseRegistration reg = dangKySinhVien(sv1, lopNguon);

        Grade grade = new Grade();
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(7.5);
        diemRepository.save(grade);

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId()
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("đã có điểm tại lớp nguồn")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Quyết định cần chốt")));
    }

    @Test
    void traCuuLichSuChuyenLop() throws Exception {
        CourseClass lopNguon = taoLopMau("QLSV79_HIST_SRC", monHoc1, gv1, "P.901", 2, 7, 3, 30);
        CourseClass lopDich = taoLopMau("QLSV79_HIST_TGT", monHoc1, gv2, "P.902", 3, 7, 3, 30);
        dangKySinhVien(sv1, lopNguon);

        mockMvc.perform(post("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/chuyen-sinh-vien")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sinhVienId", sv1.getId(),
                                "lopDichId", lopDich.getId(),
                                "lyDo", "Lịch sử test tra cứu"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/lop-hoc-phan/" + lopNguon.getId() + "/lich-su-chuyen")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].lyDo").value("Lịch sử test tra cứu"));
    }
}
