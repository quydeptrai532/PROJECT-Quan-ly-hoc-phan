package com.edu.registration.service;

import com.edu.registration.config.GradeAppealProperties;
import com.edu.registration.dto.GradeAppealDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeAppeal;
import com.edu.registration.entity.GradeAppealHistory;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeAppealHistoryRepository;
import com.edu.registration.repository.GradeAppealRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeAppealServiceTest {

    @Mock
    private GradeAppealRepository appealRepository;

    @Mock
    private GradeAppealHistoryRepository historyRepository;

    @Mock
    private CourseRegistrationRepository registrationRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private GradeService gradeService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    private GradeAppealProperties properties;
    private GradeAppealService service;

    private User studentUser;
    private Student student;
    private User adminUser;
    private CourseClass courseClass;
    private Subject subject;
    private Semester semester;
    private CourseRegistration registration;
    private Grade grade;

    @BeforeEach
    void setUp() {
        properties = new GradeAppealProperties();
        properties.setSubmissionWindowDays(15);
        properties.setRequirePublishedGrade(true);
        properties.setAutoApplyGradeChange(true);

        service = new GradeAppealService(
                appealRepository,
                historyRepository,
                registrationRepository,
                gradeRepository,
                studentRepository,
                currentUser,
                gradeService,
                auditLogService,
                notificationService,
                properties
        );

        studentUser = new User();
        studentUser.setId(101L);
        studentUser.setUsername("sv01");
        studentUser.setRole(Role.SINH_VIEN);

        student = new Student();
        student.setId(201L);
        student.setMa("SV001");
        student.setHoTen("Nguyễn Văn A");
        student.setUser(studentUser);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);

        subject = new Subject();
        subject.setId(301L);
        subject.setMa("INT1340");
        subject.setTen("Nhập môn công nghệ phần mềm");
        subject.setSoTinChi(3);

        semester = new Semester();
        semester.setId(401L);
        semester.setTen("Học kỳ 1");
        semester.setNamHoc("2026-2027");

        courseClass = new CourseClass();
        courseClass.setId(501L);
        courseClass.setMa("INT1340_01");
        courseClass.setMonHoc(subject);
        courseClass.setHocKy(semester);
        courseClass.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
        courseClass.setCongBoBangDiemLuc(LocalDateTime.now().minusDays(2));

        registration = new CourseRegistration();
        registration.setId(601L);
        registration.setSinhVien(student);
        registration.setLopHocPhan(courseClass);
        registration.setTrangThai(RegistrationStatus.DA_DANG_KY);

        grade = new Grade();
        grade.setId(701L);
        grade.setDangKy(registration);
        grade.setDiemQuaTrinh(7.0);
        grade.setDiemCuoiKy(6.0);
        grade.setDiemTongKet(6.4);
    }

    @Test
    void danhMuc_thanhCongChoSinhVien() {
        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findAllActiveOfStudent(student.getId(), RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(registration));
        when(gradeRepository.findByDangKyId(registration.getId())).thenReturn(Optional.of(grade));
        when(appealRepository.findActiveByDangKyId(eq(registration.getId()), any()))
                .thenReturn(Collections.emptyList());

        GradeAppealDtos.DanhMucPhucKhaoDto danhMuc = service.danhMuc();

        assertNotNull(danhMuc);
        assertEquals(1, danhMuc.danhSachDiem().size());
        assertEquals(15, danhMuc.submissionWindowDays());
        GradeAppealDtos.DiemKhaDungPhucKhaoDto item = danhMuc.danhSachDiem().get(0);
        assertEquals("INT1340_01", item.maLop());
        assertEquals(6.4, item.diemTongKet());
        assertFalse(item.quaHanNop());
        assertFalse(item.daCoDonDangXuLy());
    }

    @Test
    void danhMuc_chanNguoiDungKhongPhaiSinhVien() {
        when(currentUser.get()).thenReturn(adminUser);

        ApiException ex = assertThrows(ApiException.class, () -> service.danhMuc());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void taoHoSo_thanhCongVaLuuVetLichSu() {
        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(gradeRepository.findByDangKyId(registration.getId())).thenReturn(Optional.of(grade));
        when(appealRepository.findActiveByDangKyId(eq(registration.getId()), any())).thenReturn(Collections.emptyList());
        when(appealRepository.findByMaHoSo(any())).thenReturn(Optional.empty());
        when(appealRepository.save(any(GradeAppeal.class))).thenAnswer(inv -> {
            GradeAppeal a = inv.getArgument(0);
            a.setId(999L);
            return a;
        });

        GradeAppealDtos.TaoHoSoPhucKhaoRequest request = new GradeAppealDtos.TaoHoSoPhucKhaoRequest(
                registration.getId(),
                "DIEM_CUOI_KY",
                8.0,
                "Chấm sót câu 3 phần lập trình web, bài làm đầy đủ các bước"
        );

        GradeAppealDtos.HoSoPhucKhaoDto result = service.taoHoSo(request);

        assertNotNull(result);
        assertEquals(999L, result.id());
        assertEquals("SV001", result.maSinhVien());
        assertEquals("INT1340_01", result.maLopHocPhan());
        assertEquals(7.0, result.diemQuaTrinhGoc());
        assertEquals(6.0, result.diemCuoiKyGoc());
        assertEquals(6.4, result.diemTongKetGoc());
        assertEquals("MOI_TAO", result.trangThai());

        verify(historyRepository).save(any(GradeAppealHistory.class));
    }

    @Test
    void taoHoSo_chanKhiBangDiemChuaCongBo() {
        courseClass.setTrangThaiBangDiem(GradeSheetStatus.CHO_DUYET);
        courseClass.setCongBoBangDiemLuc(null);

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        GradeAppealDtos.TaoHoSoPhucKhaoRequest request = new GradeAppealDtos.TaoHoSoPhucKhaoRequest(
                registration.getId(),
                "DIEM_CUOI_KY",
                8.0,
                "Chấm sót câu 3 phần lập trình web, bài làm đầy đủ các bước"
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.taoHoSo(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("chưa được công bố chính thức"));
    }

    @Test
    void taoHoSo_chanKhiQuaHanNop() {
        // Công bố từ 20 ngày trước (quá hạn 15 ngày)
        courseClass.setCongBoBangDiemLuc(LocalDateTime.now().minusDays(20));

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(gradeRepository.findByDangKyId(registration.getId())).thenReturn(Optional.of(grade));

        GradeAppealDtos.TaoHoSoPhucKhaoRequest request = new GradeAppealDtos.TaoHoSoPhucKhaoRequest(
                registration.getId(),
                "DIEM_CUOI_KY",
                8.0,
                "Chấm sót câu 3 phần lập trình web, bài làm đầy đủ các bước"
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.taoHoSo(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("Đã quá thời hạn nộp hồ sơ phúc khảo"));
    }

    @Test
    void taoHoSo_chanKhiDaCoDonDangXuLy() {
        GradeAppeal existing = new GradeAppeal();
        existing.setId(888L);
        existing.setMaHoSo("PK-20261109-001");
        existing.setTrangThai(GradeAppealStatus.MOI_TAO);

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(gradeRepository.findByDangKyId(registration.getId())).thenReturn(Optional.of(grade));
        when(appealRepository.findActiveByDangKyId(eq(registration.getId()), any()))
                .thenReturn(List.of(existing));

        GradeAppealDtos.TaoHoSoPhucKhaoRequest request = new GradeAppealDtos.TaoHoSoPhucKhaoRequest(
                registration.getId(),
                "DIEM_CUOI_KY",
                8.0,
                "Chấm sót câu 3 phần lập trình web, bài làm đầy đủ các bước"
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.taoHoSo(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("đang chờ hoặc đang được xử lý"));
    }

    @Test
    void taoHoSo_chanIDORDangKyCuaSinhVienKhac() {
        Student svKhac = new Student();
        svKhac.setId(202L);
        svKhac.setMa("SV002");
        registration.setSinhVien(svKhac);

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        GradeAppealDtos.TaoHoSoPhucKhaoRequest request = new GradeAppealDtos.TaoHoSoPhucKhaoRequest(
                registration.getId(),
                "DIEM_CUOI_KY",
                8.0,
                "Chấm sót câu 3 phần lập trình web, bài làm đầy đủ các bước"
        );

        ApiException ex = assertThrows(ApiException.class, () -> service.taoHoSo(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void huyHoSo_thanhCongKhiMoiTao() {
        GradeAppeal appeal = new GradeAppeal();
        appeal.setId(10L);
        appeal.setMaHoSo("PK-001");
        appeal.setSinhVien(student);
        appeal.setTrangThai(GradeAppealStatus.MOI_TAO);

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(appealRepository.findWithDetailsById(10L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(GradeAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeAppealDtos.HoSoPhucKhaoDto result = service.huyHoSo(10L, "Tôi muốn rút đơn");

        assertNotNull(result);
        assertEquals("DA_HUY", result.trangThai());
        verify(historyRepository).save(any(GradeAppealHistory.class));
    }

    @Test
    void huyHoSo_thatBaiKhiDaDangXuLy() {
        GradeAppeal appeal = new GradeAppeal();
        appeal.setId(10L);
        appeal.setMaHoSo("PK-001");
        appeal.setSinhVien(student);
        appeal.setTrangThai(GradeAppealStatus.DANG_XU_LY);

        when(currentUser.get()).thenReturn(studentUser);
        when(studentRepository.findByUser(studentUser)).thenReturn(Optional.of(student));
        when(appealRepository.findWithDetailsById(10L)).thenReturn(Optional.of(appeal));

        ApiException ex = assertThrows(ApiException.class, () -> service.huyHoSo(10L, "Rút đơn"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void tiepNhan_thanhCongChoQuanTri() {
        GradeAppeal appeal = new GradeAppeal();
        appeal.setId(10L);
        appeal.setMaHoSo("PK-001");
        appeal.setSinhVien(student);
        appeal.setLopHocPhan(courseClass);
        appeal.setMonHoc(subject);
        appeal.setTrangThai(GradeAppealStatus.MOI_TAO);

        when(currentUser.get()).thenReturn(adminUser);
        when(appealRepository.findWithDetailsById(10L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(GradeAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeAppealDtos.HoSoPhucKhaoDto result = service.tiepNhan(10L, "Phòng Đào tạo chuyển tổ chấm lại bài thi");

        assertNotNull(result);
        assertEquals("DANG_XU_LY", result.trangThai());
        verify(historyRepository).save(any(GradeAppealHistory.class));
    }

    @Test
    void xuLy_tuChoi_khongThayDoiDiemGocVaGhiNhanKetQua() {
        GradeAppeal appeal = new GradeAppeal();
        appeal.setId(10L);
        appeal.setMaHoSo("PK-001");
        appeal.setSinhVien(student);
        appeal.setLopHocPhan(courseClass);
        appeal.setMonHoc(subject);
        appeal.setDiem(grade);
        appeal.setDiemQuaTrinhGoc(7.0);
        appeal.setDiemCuoiKyGoc(6.0);
        appeal.setDiemTongKetGoc(6.4);
        appeal.setTrangThai(GradeAppealStatus.DANG_XU_LY);

        when(currentUser.get()).thenReturn(adminUser);
        when(appealRepository.findWithDetailsById(10L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(GradeAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeAppealDtos.XuLyPhucKhaoRequest request = new GradeAppealDtos.XuLyPhucKhaoRequest(
                "TU_CHOI",
                "Đã chấm kiểm tra lại, bài thi chấm chính xác theo barem đáp án, giữ nguyên điểm",
                null,
                null
        );

        GradeAppealDtos.HoSoPhucKhaoDto result = service.xuLy(10L, request);

        assertNotNull(result);
        assertEquals("TU_CHOI", result.trangThai());
        assertEquals(7.0, result.diemQuaTrinhGoc());
        assertEquals(6.0, result.diemCuoiKyGoc());
        assertEquals(6.4, result.diemTongKetGoc());
        assertNull(result.diemQuaTrinhMoi());
        assertNull(result.diemCuoiKyMoi());

        verify(gradeService, never()).dieuChinhDiemPhucKhao(any(), any(), any(), any(), any(), any());
        verify(historyRepository).save(any(GradeAppealHistory.class));
        verify(notificationService).gradeAppealProcessed(eq(studentUser), eq("PK-001"), eq(subject.getTen()), eq("TU_CHOI"), any(), any());
    }

    @Test
    void xuLy_chapThuan_dieuChinhDiemQuaLuongAudit() {
        GradeAppeal appeal = new GradeAppeal();
        appeal.setId(10L);
        appeal.setMaHoSo("PK-001");
        appeal.setSinhVien(student);
        appeal.setLopHocPhan(courseClass);
        appeal.setMonHoc(subject);
        appeal.setDiem(grade);
        appeal.setDiemQuaTrinhGoc(7.0);
        appeal.setDiemCuoiKyGoc(6.0);
        appeal.setDiemTongKetGoc(6.4);
        appeal.setTrangThai(GradeAppealStatus.DANG_XU_LY);

        when(currentUser.get()).thenReturn(adminUser);
        when(appealRepository.findWithDetailsById(10L)).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any(GradeAppeal.class))).thenAnswer(inv -> inv.getArgument(0));

        GradeAppealDtos.XuLyPhucKhaoRequest request = new GradeAppealDtos.XuLyPhucKhaoRequest(
                "CHAP_THUAN",
                "Chấm sót câu 3, tăng điểm thi cuối kỳ từ 6.0 lên 8.0",
                7.0,
                8.0
        );

        GradeAppealDtos.HoSoPhucKhaoDto result = service.xuLy(10L, request);

        assertNotNull(result);
        assertEquals("CHAP_THUAN", result.trangThai());
        assertEquals(7.0, result.diemQuaTrinhMoi());
        assertEquals(8.0, result.diemCuoiKyMoi());
        assertEquals(7.6, result.diemTongKetMoi());

        // Đảm bảo gọi qua luồng điều chỉnh có audit của GradeService
        verify(gradeService).dieuChinhDiemPhucKhao(
                eq(grade.getId()),
                eq(10L),
                eq(7.0),
                eq(8.0),
                any(),
                eq(adminUser)
        );
        verify(historyRepository).save(any(GradeAppealHistory.class));
        verify(notificationService).gradeAppealProcessed(eq(studentUser), eq("PK-001"), eq(subject.getTen()), eq("CHAP_THUAN"), any(), any());
    }
}
