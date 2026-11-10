package com.edu.registration.service;

import com.edu.registration.config.EarlyWarningProperties;
import com.edu.registration.dto.AcademicAlertDtos.*;
import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EarlyWarningServiceTest {

    @Mock
    private AcademicAlertRepository alertRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRegistrationRepository registrationRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private ClassSessionRepository sessionRepository;
    @Mock
    private AttendanceRecordRepository attendanceRepository;
    @Mock
    private ClassAbsenceNoticeRepository absenceNoticeRepository;
    @Mock
    private CourseClassRepository courseClassRepository;
    @Mock
    private LecturerRepository lecturerRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditLogService auditLogService;

    private EarlyWarningProperties properties;
    private ObjectMapper objectMapper;
    private EarlyWarningService service;

    private Student testStudent;
    private User studentUser;
    private User adminUser;
    private User lecturerUser;
    private Lecturer testLecturer;

    @BeforeEach
    void setUp() {
        properties = new EarlyWarningProperties();
        objectMapper = new ObjectMapper();
        service = new EarlyWarningService(
                alertRepository,
                studentRepository,
                registrationRepository,
                gradeRepository,
                sessionRepository,
                attendanceRepository,
                absenceNoticeRepository,
                courseClassRepository,
                lecturerRepository,
                currentUser,
                properties,
                auditLogService,
                objectMapper
        );

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setUsername("sv01");
        studentUser.setRole(Role.SINH_VIEN);

        testStudent = new Student();
        testStudent.setId(1L);
        testStudent.setMa("SV001");
        testStudent.setHoTen("Nguyễn Văn A");
        testStudent.setUser(studentUser);
        testStudent.setTrangThaiHocTap(StudentAcademicStatus.DANG_HOC);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);

        lecturerUser = new User();
        lecturerUser.setId(50L);
        lecturerUser.setUsername("gv01");
        lecturerUser.setRole(Role.GIANG_VIEN);

        testLecturer = new Lecturer();
        testLecturer.setId(5L);
        testLecturer.setMa("GV001");
        testLecturer.setHoTen("TS. Trần Văn B");
        testLecturer.setUser(lecturerUser);
    }

    @Test
    @DisplayName("Cấu hình tập trung trả về danh sách quy tắc và danh sách chờ Hội đồng Đào tạo xác nhận")
    void layCauHinhTraVeDayDuGiaTriVaQuyCheChoXacNhan() {
        EarlyWarningConfigDto config = service.layCauHinh();

        assertNotNull(config);
        assertTrue(config.enabled());
        assertFalse(config.officialRegulation(), "Quy chế chính thức phải là false chờ Hội đồng phê duyệt");
        assertNotNull(config.policyNote());
        assertEquals(4, config.danhSachLuat().size());

        List<String> choXacNhan = config.giaTriChoXacNhan();
        assertFalse(choXacNhan.isEmpty());
        assertTrue(choXacNhan.stream().anyMatch(s -> s.contains("officialRegulation = false")));
        assertTrue(choXacNhan.stream().anyMatch(s -> s.contains("Điểm quá trình")));
        assertTrue(choXacNhan.stream().anyMatch(s -> s.contains("Điểm danh")));
        assertTrue(choXacNhan.stream().anyMatch(s -> s.contains("GPA giảm")));
        assertTrue(choXacNhan.stream().anyMatch(s -> s.contains("Tiến độ tín chỉ")));
    }

    @Test
    @DisplayName("Luật 1: Điểm quá trình < 5.0 kích hoạt cảnh báo minh bạch; >= 5.0 không kích hoạt")
    void luatDiemQuaTrinhKichHoatKhiDuoiNguong() {
        properties.getDiemDanh().setEnabled(false);
        properties.getGpaGiam().setEnabled(false);
        properties.getTienDoTinChi().setEnabled(false);

        CourseClass lop = taoLopHocPhan(101L, "CS101-01", "Lập trình cơ sở", 3);
        CourseRegistration reg = taoDangKy(201L, testStudent, lop);
        Grade grade = new Grade();
        grade.setId(301L);
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(3.5); // < 5.0

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(grade));
        when(alertRepository.findByKhoaChongTrung(anyString()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertEquals(1, summary.created);
        ArgumentCaptor<AcademicAlert> captor = ArgumentCaptor.forClass(AcademicAlert.class);
        verify(alertRepository).save(captor.capture());

        AcademicAlert alert = captor.getValue();
        assertEquals(AcademicAlertRuleType.DIEM_QUA_TRINH, alert.getLoaiLuat());
        assertEquals("RULE_MIDTERM_LOW", alert.getMaLuat());
        assertEquals(AcademicAlertSeverity.CANH_BAO, alert.getMucDo());
        assertTrue(alert.getLyDo().contains("3.50/10.0"));
        assertTrue(alert.getCongThucGiaiThich().contains("diemQuaTrinh < nguongDiem"));
        assertTrue(alert.getDuLieuDanChung().contains("\"diemQuaTrinh\":3.5"));
        assertTrue(alert.isChoPhepSinhVienXem());
    }

    @Test
    @DisplayName("Luật 2: Số buổi vắng hoặc tỷ lệ vắng vượt ngưỡng kích hoạt cảnh báo chuyên cần")
    void luatDiemDanhKichHoatKhiVangVuotNguong() {
        CourseClass lop = taoLopHocPhan(102L, "MATH101-01", "Giải tích 1", 3);
        CourseRegistration reg = taoDangKy(202L, testStudent, lop);

        LocalDate d1 = LocalDate.now().minusDays(14);
        LocalDate d2 = LocalDate.now().minusDays(7);
        LocalDate d3 = LocalDate.now().minusDays(2);
        ClassSession s1 = taoBuoiHoc(1L, lop, d1);
        ClassSession s2 = taoBuoiHoc(2L, lop, d2);
        ClassSession s3 = taoBuoiHoc(3L, lop, d3);

        AttendanceRecord r1 = taoDiemDanh(s1, testStudent, AttendanceStatus.VANG);
        AttendanceRecord r2 = taoDiemDanh(s2, testStudent, AttendanceStatus.VANG);
        AttendanceRecord r3 = taoDiemDanh(s3, testStudent, AttendanceStatus.VANG);

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg));
        when(sessionRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(eq(102L)))
                .thenReturn(List.of(s1, s2, s3));
        when(absenceNoticeRepository.findByLopHocPhanId(eq(102L)))
                .thenReturn(List.of());
        when(attendanceRepository.findBySinhVienAndLopHocPhan(eq(1L), eq(102L)))
                .thenReturn(List.of(r1, r2, r3));
        when(alertRepository.findByKhoaChongTrung(anyString()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertTrue(summary.created >= 1);
        ArgumentCaptor<AcademicAlert> captor = ArgumentCaptor.forClass(AcademicAlert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());

        AcademicAlert alert = captor.getAllValues().stream()
                .filter(a -> a.getLoaiLuat() == AcademicAlertRuleType.DIEM_DANH)
                .findFirst()
                .orElseThrow();

        assertEquals("RULE_ATTENDANCE_EXCESSIVE", alert.getMaLuat());
        assertEquals(AcademicAlertSeverity.NGHIEM_TRONG, alert.getMucDo());
        assertTrue(alert.getLyDo().contains("3/3 buổi"));
        assertTrue(alert.getCongThucGiaiThich().contains("soBuoiVang >= 3 HOẶC tyLeVang >= 20.00%"));
    }

    @Test
    @DisplayName("Luật 3: GPA giảm qua 2 học kỳ liên tiếp >= 0.3 kích hoạt cảnh báo; chính sách riêng tư ẩn đối với sinh viên")
    void luatGpaGiamKichHoatKhiGpaGiamLienTiep() {
        Semester sem1 = new Semester();
        sem1.setId(1L);
        sem1.setTen("Học kỳ 1 2024-2025");

        Semester sem2 = new Semester();
        sem2.setId(2L);
        sem2.setTen("Học kỳ 2 2024-2025");

        CourseClass lop1 = taoLopHocPhan(11L, "CS1", "Môn 1", 3);
        lop1.setHocKy(sem1);
        lop1.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);

        CourseClass lop2 = taoLopHocPhan(12L, "CS2", "Môn 2", 3);
        lop2.setHocKy(sem2);
        lop2.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);

        CourseRegistration reg1 = taoDangKy(21L, testStudent, lop1);
        CourseRegistration reg2 = taoDangKy(22L, testStudent, lop2);

        Grade g1 = new Grade();
        g1.setId(31L);
        g1.setDangKy(reg1);
        g1.setDiemTongKet(8.5); // GPA hệ 4: 4.0

        Grade g2 = new Grade();
        g2.setId(32L);
        g2.setDangKy(reg2);
        g2.setDiemTongKet(5.0); // GPA hệ 4: 1.5 -> Sụt giảm 2.5 >= 0.3

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg1, reg2));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(g1, g2));
        when(alertRepository.findByKhoaChongTrung(anyString()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertTrue(summary.created >= 1);
        ArgumentCaptor<AcademicAlert> captor = ArgumentCaptor.forClass(AcademicAlert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());

        AcademicAlert alert = captor.getAllValues().stream()
                .filter(a -> a.getLoaiLuat() == AcademicAlertRuleType.GPA_GIAM)
                .findFirst()
                .orElseThrow();

        assertEquals("RULE_GPA_DECLINE", alert.getMaLuat());
        assertTrue(alert.getLyDo().contains("GPA hệ 4 giảm"));
        assertFalse(alert.isChoPhepSinhVienXem(), "Mặc định quyền riêng tư: sinh viên chưa được xem cảnh báo GPA giảm");
    }

    @Test
    @DisplayName("Luật 4: Thiếu tín chỉ so với tiến độ chuẩn (thiếu >= 8 TC) kích hoạt cảnh báo")
    void luatTienDoTinChiKichHoatKhiThieuTinChi() {
        Semester s1 = new Semester(); s1.setId(1L); s1.setTen("HK1");
        Semester s2 = new Semester(); s2.setId(2L); s2.setTen("HK2");

        CourseClass l1 = taoLopHocPhan(101L, "C1", "Môn 1", 3); l1.setHocKy(s1); l1.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        CourseClass l2 = taoLopHocPhan(102L, "C2", "Môn 2", 3); l2.setHocKy(s2); l2.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);

        CourseRegistration r1 = taoDangKy(201L, testStudent, l1);
        CourseRegistration r2 = taoDangKy(202L, testStudent, l2);

        // Sinh viên qua 2 kỳ nhưng chỉ đạt 1 môn 3 TC (kỳ vọng 2 kỳ * 14 = 28 TC -> thiếu 25 TC >= 8 TC)
        Grade g1 = new Grade(); g1.setId(301L); g1.setDangKy(r1); g1.setDiemTongKet(6.0); // Đạt 3 TC
        Grade g2 = new Grade(); g2.setId(302L); g2.setDangKy(r2); g2.setDiemTongKet(4.0); // Trượt

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(r1, r2));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(g1, g2));
        when(alertRepository.findByKhoaChongTrung(anyString()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertTrue(summary.created >= 1);
        ArgumentCaptor<AcademicAlert> captor = ArgumentCaptor.forClass(AcademicAlert.class);
        verify(alertRepository, atLeastOnce()).save(captor.capture());

        AcademicAlert alert = captor.getAllValues().stream()
                .filter(a -> a.getLoaiLuat() == AcademicAlertRuleType.TIEN_DO_TIN_CHI)
                .findFirst()
                .orElseThrow();

        assertEquals("RULE_CREDIT_DEFICIT", alert.getMaLuat());
        assertTrue(alert.getLyDo().contains("tích lũy 3/28 tín chỉ"));
        assertTrue(alert.getCongThucGiaiThich().contains("Tín chỉ kỳ vọng chuẩn: 2 kỳ * 14 TC = 28 TC"));
    }

    @Test
    @DisplayName("Cơ chế chống trùng: Chạy lại khi điều kiện vi phạm vẫn tồn tại sẽ cập nhật bản ghi cũ, không tạo bản ghi mới")
    void chayLaiKhongTaoCanhBaoTrung() {
        properties.getDiemDanh().setEnabled(false);
        properties.getGpaGiam().setEnabled(false);
        properties.getTienDoTinChi().setEnabled(false);

        CourseClass lop = taoLopHocPhan(101L, "CS101-01", "Lập trình", 3);
        CourseRegistration reg = taoDangKy(201L, testStudent, lop);
        Grade grade = new Grade();
        grade.setId(301L);
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(4.0);

        AcademicAlert existingAlert = new AcademicAlert();
        existingAlert.setId(555L);
        existingAlert.setSinhVien(testStudent);
        existingAlert.setMaLuat("RULE_MIDTERM_LOW");
        existingAlert.setKhoaChongTrung("sv:1#RULE_MIDTERM_LOW#lop:101");
        existingAlert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);
        existingAlert.setActive(true);

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(grade));
        when(alertRepository.findByKhoaChongTrung(eq("sv:1#RULE_MIDTERM_LOW#lop:101")))
                .thenReturn(Optional.of(existingAlert));
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertEquals(0, summary.created, "Không được tạo bản ghi mới khi đã có bản ghi cùng khóa chống trùng");
        assertEquals(1, summary.updated, "Phải cập nhật bản ghi hiện tại");
        assertEquals(555L, existingAlert.getId());
    }

    @Test
    @DisplayName("Tự động giải tỏa có truy vết: Khi điểm quá trình cải thiện >= 5.0, cảnh báo tự động chuyển DA_GIAI_TOA")
    void tuDongGiaiToaKhiDuLieuNguonCaiThien() {
        properties.getDiemDanh().setEnabled(false);
        properties.getGpaGiam().setEnabled(false);
        properties.getTienDoTinChi().setEnabled(false);

        CourseClass lop = taoLopHocPhan(101L, "CS101-01", "Lập trình", 3);
        CourseRegistration reg = taoDangKy(201L, testStudent, lop);
        Grade grade = new Grade();
        grade.setId(301L);
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(7.5); // Điểm đã cải thiện đạt yêu cầu

        AcademicAlert activeAlert = new AcademicAlert();
        activeAlert.setId(777L);
        activeAlert.setSinhVien(testStudent);
        activeAlert.setMaLuat("RULE_MIDTERM_LOW");
        activeAlert.setKhoaChongTrung("sv:1#RULE_MIDTERM_LOW#lop:101");
        activeAlert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);
        activeAlert.setActive(true);

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(grade));
        when(alertRepository.findByKhoaChongTrung(eq("sv:1#RULE_MIDTERM_LOW#lop:101")))
                .thenReturn(Optional.of(activeAlert));
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        EarlyWarningService.ScanSummary summary = service.quyetToanCanhBaoChoSinhVien(testStudent);

        assertEquals(1, summary.autoResolved);
        assertEquals(AcademicAlertStatus.DA_GIAI_TOA, activeAlert.getTrangThai());
        assertFalse(activeAlert.isActive());
        assertNotNull(activeAlert.getNguyenNhanGiaiToa());
        assertNotNull(activeAlert.getNgayXuLy());
    }

    @Test
    @DisplayName("An toàn tuyệt đối: Đánh giá cảnh báo KHÔNG BAO GIỜ tự sửa trạng thái học vụ sinh viên hoặc khóa tài khoản")
    void anToanTuyetDoiKhongSuaDuLieuNghiepVu() {
        CourseClass lop = taoLopHocPhan(101L, "CS101-01", "Lập trình cơ sở", 3);
        CourseRegistration reg = taoDangKy(201L, testStudent, lop);
        Grade grade = new Grade();
        grade.setId(301L);
        grade.setDangKy(reg);
        grade.setDiemQuaTrinh(1.0); // Rất thấp

        when(registrationRepository.findAllActiveOfStudent(eq(1L), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(reg));
        when(gradeRepository.findByDangKyIdIn(anyList()))
                .thenReturn(List.of(grade));
        when(alertRepository.findByKhoaChongTrung(anyString()))
                .thenReturn(Optional.empty());
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        service.quyetToanCanhBaoChoSinhVien(testStudent);

        // Kiểm tra chắc chắn trạng thái học vụ và tài khoản sinh viên được bảo toàn nguyên vẹn
        assertEquals(StudentAcademicStatus.DANG_HOC, testStudent.getTrangThaiHocTap());
        assertTrue(studentUser.isTrangThai());
    }

    @Test
    @DisplayName("Phân quyền: Quản trị viên có thể cập nhật trạng thái xử lý cảnh báo kèm ghi chú")
    void quanTriCapNhatTrangThaiXuLyThanhCong() {
        when(currentUser.get()).thenReturn(adminUser);
        AcademicAlert alert = new AcademicAlert();
        alert.setId(10L);
        alert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);
        alert.setSinhVien(testStudent);
        alert.setLoaiLuat(AcademicAlertRuleType.DIEM_QUA_TRINH);
        alert.setMucDo(AcademicAlertSeverity.CANH_BAO);

        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(AcademicAlert.class))).thenAnswer(i -> i.getArgument(0));

        CapNhatTrangThaiRequest req = new CapNhatTrangThaiRequest(AcademicAlertStatus.DANG_XU_LY, "Đã hẹn gặp cố vấn học tập");
        AcademicAlertDto dto = service.capNhatTrangThaiXuLy(10L, req);

        assertEquals(AcademicAlertStatus.DANG_XU_LY, dto.trangThai());
        assertEquals("Đã hẹn gặp cố vấn học tập", dto.ghiChuXuLy());
        assertEquals("admin", dto.nguoiXuLy());
    }

    @Test
    @DisplayName("Phân quyền: Sinh viên bị chặn (403) khi cố gắng cập nhật trạng thái xử lý")
    void sinhVienBiChanKhiCapNhatTrangThaiXuLy() {
        when(currentUser.get()).thenReturn(studentUser);
        AcademicAlert alert = new AcademicAlert();
        alert.setId(10L);
        when(alertRepository.findById(10L)).thenReturn(Optional.of(alert));

        CapNhatTrangThaiRequest req = new CapNhatTrangThaiRequest(AcademicAlertStatus.DA_XU_LY, "Tự đánh dấu");
        assertThrows(ApiException.class, () -> service.capNhatTrangThaiXuLy(10L, req));
    }

    @Test
    @DisplayName("Phân quyền: Sinh viên chỉ xem được các cảnh báo có cờ cho phép sinh viên xem")
    void sinhVienChiXemCanhBaoDuocPhep() {
        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(testStudent));

        AcademicAlert allowedAlert = new AcademicAlert();
        allowedAlert.setId(1L);
        allowedAlert.setSinhVien(testStudent);
        allowedAlert.setLoaiLuat(AcademicAlertRuleType.DIEM_QUA_TRINH);
        allowedAlert.setMaLuat("RULE_MIDTERM_LOW");
        allowedAlert.setTenLuat("Cảnh báo điểm quá trình thấp");
        allowedAlert.setMucDo(AcademicAlertSeverity.CANH_BAO);
        allowedAlert.setLyDo("Điểm thấp");
        allowedAlert.setCongThucGiaiThich("Giải thích");
        allowedAlert.setDuLieuDanChung("{}");
        allowedAlert.setNguCanh("lop:1");
        allowedAlert.setChoPhepSinhVienXem(true);
        allowedAlert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);

        Page<AcademicAlert> page = new PageImpl<>(List.of(allowedAlert));
        when(alertRepository.findForStudent(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<AcademicAlertDto> result = service.danhSachCanhBaoSinhVien(PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).choPhepSinhVienXem());
    }

    // Helper builders
    private CourseClass taoLopHocPhan(Long id, String ma, String tenMon, int soTinChi) {
        Subject s = new Subject();
        s.setId(id + 1000);
        s.setMa(ma + "-M");
        s.setTen(tenMon);
        s.setSoTinChi(soTinChi);

        Semester sem = new Semester();
        sem.setId(1L);
        sem.setTen("Học kỳ 1");

        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(s);
        lop.setHocKy(sem);
        return lop;
    }

    private CourseRegistration taoDangKy(Long id, Student sv, CourseClass lop) {
        CourseRegistration r = new CourseRegistration();
        r.setId(id);
        r.setSinhVien(sv);
        r.setLopHocPhan(lop);
        r.setTrangThai(RegistrationStatus.DA_DANG_KY);
        return r;
    }

    private ClassSession taoBuoiHoc(Long id, CourseClass lop, LocalDate ngayHoc) {
        ClassSession s = new ClassSession();
        s.setId(id);
        s.setLopHocPhan(lop);
        s.setNgayHoc(ngayHoc);
        s.setCaHoc(1);
        return s;
    }

    private AttendanceRecord taoDiemDanh(ClassSession s, Student sv, AttendanceStatus status) {
        AttendanceRecord r = new AttendanceRecord();
        r.setBuoiHoc(s);
        r.setSinhVien(sv);
        r.setTrangThai(status);
        return r;
    }
}
