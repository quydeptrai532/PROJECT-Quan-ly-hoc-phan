package com.edu.registration.service;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.service.AuditLogService.AuditContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;
    @Mock
    private RegistrationHistoryRepository lichSuDangKyRepository;
    @Mock
    private CourseClassRepository lopHocPhanRepository;
    @Mock
    private GradeRepository diemRepository;
    @Mock
    private PrerequisiteSubjectRepository monTienQuyetRepository;
    @Mock
    private StudentRepository sinhVienRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private SemesterService hocKyService;
    @Mock
    private DtoMapper mapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService thongBaoService;

    @InjectMocks
    private RegistrationService dangKyService;

    private User user;
    private Student sinhVien;
    private Semester hocKy;
    private Subject monHoc;
    private CourseClass lopHocPhan;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogService.execute(any(), any(), any(), any())).thenAnswer(invocation -> {
            Function<AuditContext, Object> operation = invocation.getArgument(3);
            return operation.apply(new AuditContext(null));
        });
        user = new User();
        user.setId(1L);
        user.setUsername("sv01");

        sinhVien = new Student();
        sinhVien.setId(1L);
        sinhVien.setUser(user);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");

        hocKy = new Semester();
        hocKy.setId(1L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2026-2027");
        hocKy.setNgayBatDau(LocalDate.now().minusDays(14));
        hocKy.setNgayKetThuc(LocalDate.now().plusDays(90));
        hocKy.setDangKyTu(LocalDate.now().minusDays(5));
        hocKy.setDangKyDen(LocalDate.now().plusDays(20));
        hocKy.setSoTinChiToiThieu(12);
        hocKy.setSoTinChiToiDa(24);

        monHoc = new Subject();
        monHoc.setId(10L);
        monHoc.setMa("INT201");
        monHoc.setTen("Cấu trúc dữ liệu và giải thuật");
        monHoc.setSoTinChi(3);

        lopHocPhan = new CourseClass();
        lopHocPhan.setId(100L);
        lopHocPhan.setMa("INT201.01");
        lopHocPhan.setMonHoc(monHoc);
        lopHocPhan.setHocKy(hocKy);
        lopHocPhan.setGiangVien(new Lecturer());
        lopHocPhan.setPhong("A101");
        lopHocPhan.setThu(2);
        lopHocPhan.setTietBatDau(1);
        lopHocPhan.setSoTiet(3);
        lopHocPhan.setSiSoToiDa(40);
        lopHocPhan.setSiSoHienTai(0);
    }

    private void dangNhapVoiSinhVien() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findByUserIdForUpdate(1L)).thenReturn(Optional.of(sinhVien));
    }

    private void stubLopHocPhan() {
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
    }

    private CourseClass taoLopKhac(long id, String ma, Subject mon, int thu, int tietBatDau, int soTiet) {
        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hocKy);
        lop.setGiangVien(new Lecturer());
        lop.setPhong("B202");
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(40);
        return lop;
    }

    @Test
    void dangKyThanhCongTangSiSoVaTaoDangKyMoi() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L)).thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(CourseRegistration.class)))
                .thenAnswer(invocation -> {
                    CourseRegistration saved = invocation.getArgument(0);
                    saved.setId(501L);
                    return saved;
                });

        dangKyService.dangKy(100L);

        ArgumentCaptor<CourseRegistration> captor = ArgumentCaptor.forClass(CourseRegistration.class);
        verify(dangKyHocPhanRepository).save(captor.capture());
        assertEquals(RegistrationStatus.DA_DANG_KY, captor.getValue().getTrangThai());
        assertEquals(sinhVien, captor.getValue().getSinhVien());
        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void dangKyLaiKichHoatLaiDangKyDaHuy() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        CourseRegistration daHuy = new CourseRegistration();
        daHuy.setId(200L);
        daHuy.setSinhVien(sinhVien);
        daHuy.setLopHocPhan(lopHocPhan);
        daHuy.setTrangThai(RegistrationStatus.DA_HUY);

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L))
                .thenReturn(Optional.of(daHuy));
        when(dangKyHocPhanRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        dangKyService.dangKy(100L);

        assertEquals(RegistrationStatus.DA_DANG_KY, daHuy.getTrangThai());
        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void dangKyThatBaiKhiNgoaiThoiGianDangKy() {
        hocKy.setDangKyTu(LocalDate.now().minusDays(10));
        hocKy.setDangKyDen(LocalDate.now().minusDays(1));
        dangNhapVoiSinhVien();
        stubLopHocPhan();

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("thời gian đăng ký"));
        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void dangKyThatBaiKhiKhongTimThayLopHocPhan() {
        dangNhapVoiSinhVien();

        when(lopHocPhanRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.dangKy(999L));

        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void dangKyThatBaiKhiSinhVienChuaCoHoSo() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findByUserIdForUpdate(1L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.dangKy(100L));

        verify(dangKyHocPhanRepository, never()).save(any());
    }

    @Test
    void huyDangKyThatBaiKhiDangKyKhongThuocSinhVien() {
        dangNhapVoiSinhVien();

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ApiException.class,
                () -> dangKyService.huyDangKy(200L, null));

        verify(lopHocPhanRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void huyDangKyThatBaiKhiDangKyDaHuy() {
        dangNhapVoiSinhVien();

        CourseRegistration dangKy = new CourseRegistration();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(RegistrationStatus.DA_HUY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L))
                .thenReturn(Optional.of(dangKy));

        assertThrows(ApiException.class,
                () -> dangKyService.huyDangKy(200L, null));

        assertEquals(RegistrationStatus.DA_HUY, dangKy.getTrangThai());
        verify(lopHocPhanRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void dangKyThatBaiKhiDaDangKyMonTrongHocKy() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        CourseRegistration daDangKy = new CourseRegistration();
        daDangKy.setLopHocPhan(lopHocPhan);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("đã đăng ký môn"));
    }

    @Test
    void dangKyThatBaiKhiTrungLichHoc() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        Subject monKhac = new Subject();
        monKhac.setId(11L);
        monKhac.setMa("INT303");
        monKhac.setTen("Mạng máy tính");
        monKhac.setSoTinChi(3);
        CourseClass trungLich = taoLopKhac(101L, "INT303.01", monKhac, 2, 2, 3);

        CourseRegistration daDangKy = new CourseRegistration();
        daDangKy.setLopHocPhan(trungLich);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("Trùng lịch học"));
    }

    @Test
    void dangKyThatBaiKhiLopDay() {
        lopHocPhan.setSiSoHienTai(40);
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("đủ sĩ số"));
    }

    @Test
    void dangKyThatBaiKhiVuotQuaTinChiToiDa() {
        hocKy.setSoTinChiToiDa(5);
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        Subject monKhac = new Subject();
        monKhac.setId(11L);
        monKhac.setMa("INT303");
        monKhac.setTen("Mạng máy tính");
        monKhac.setSoTinChi(3);
        CourseClass lopKhac = taoLopKhac(101L, "INT303.01", monKhac, 3, 1, 3);

        CourseRegistration daDangKy = new CourseRegistration();
        daDangKy.setLopHocPhan(lopKhac);
        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(daDangKy));
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("tín chỉ tối đa"));
    }


    @Test
    void dangKyThanhCongKhiTongTinChiBangMucToiDa() {
        hocKy.setSoTinChiToiDa(3);
        dangNhapVoiSinhVien();
        stubLopHocPhan();

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L))
                .thenReturn(List.of());
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L))
                .thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(CourseRegistration.class)))
                .thenAnswer(inv -> {
                    CourseRegistration saved = inv.getArgument(0);
                    saved.setId(501L);
                    return saved;
                });

        dangKyService.dangKy(100L);

        assertEquals(1, lopHocPhan.getSiSoHienTai());
        verify(dangKyHocPhanRepository).save(any(CourseRegistration.class));
    }

    @Test
    void dangKyThatBaiKhiChuaDatMonTienQuyet() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        Subject tienQuyet = new Subject();
        tienQuyet.setId(9L);
        tienQuyet.setMa("INT101");
        tienQuyet.setTen("Lập trình cơ bản");
        PrerequisiteSubject lienKet = new PrerequisiteSubject(monHoc, tienQuyet);

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L)).thenReturn(List.of(lienKet));
        when(diemRepository.findMonDaDat(1L, 5.0)).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.dangKy(100L));

        assertTrue(ex.getMessage().contains("INT101"));
        assertTrue(ex.getMessage().contains("tiên quyết"));
    }

    @Test
    void dangKyThanhCongKhiDaDatMonTienQuyet() {
        dangNhapVoiSinhVien();
        stubLopHocPhan();
        Subject tienQuyet = new Subject();
        tienQuyet.setId(9L);
        tienQuyet.setMa("INT101");
        tienQuyet.setTen("Lập trình cơ bản");

        when(dangKyHocPhanRepository.findTheoHocKy(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(List.of());
        when(monTienQuyetRepository.findByMonHocIdOrderByIdAsc(10L))
                .thenReturn(List.of(new PrerequisiteSubject(monHoc, tienQuyet)));
        when(diemRepository.findMonDaDat(1L, 5.0)).thenReturn(List.of(9L));
        when(dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(1L, 100L)).thenReturn(Optional.empty());
        when(dangKyHocPhanRepository.save(any(CourseRegistration.class))).thenAnswer(inv -> {
            CourseRegistration saved = inv.getArgument(0);
            saved.setId(501L);
            return saved;
        });

        dangKyService.dangKy(100L);

        assertEquals(1, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void huyDangKyThanhCongHoanLaiCho() {
        lopHocPhan.setSiSoHienTai(5);
        dangNhapVoiSinhVien();
        CourseRegistration dangKy = new CourseRegistration();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
        when(dangKyHocPhanRepository.tinhTongTinChi(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(3L);

        dangKyService.huyDangKy(200L, null);

        assertEquals(RegistrationStatus.DA_HUY, dangKy.getTrangThai());
        assertEquals(4, lopHocPhan.getSiSoHienTai());
    }

    @Test
    void huyDangKyThatBaiKhiConLaiDuoiTinChiToiThieu() {
        dangNhapVoiSinhVien();
        CourseRegistration dangKy = new CourseRegistration();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));
        when(dangKyHocPhanRepository.tinhTongTinChi(1L, 1L, RegistrationStatus.DA_DANG_KY)).thenReturn(6L);

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.huyDangKy(200L, null));

        assertTrue(ex.getMessage().contains("tối thiểu"));
        assertEquals(RegistrationStatus.DA_DANG_KY, dangKy.getTrangThai());
    }

    @Test
    void huyDangKyThatBaiKhiHetThoiGian() {
        hocKy.setDangKyDen(LocalDate.now().minusDays(1));
        dangNhapVoiSinhVien();
        CourseRegistration dangKy = new CourseRegistration();
        dangKy.setId(200L);
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lopHocPhan);
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);

        when(dangKyHocPhanRepository.findByIdAndSinhVienId(200L, 1L)).thenReturn(Optional.of(dangKy));
        when(lopHocPhanRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(lopHocPhan));

        ApiException ex = assertThrows(ApiException.class, () -> dangKyService.huyDangKy(200L, null));

        assertTrue(ex.getMessage().contains("hết thời gian"));
        assertEquals(RegistrationStatus.DA_DANG_KY, dangKy.getTrangThai());
    }
}
