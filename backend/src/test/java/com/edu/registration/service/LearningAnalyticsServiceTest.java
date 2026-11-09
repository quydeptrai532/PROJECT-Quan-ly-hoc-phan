package com.edu.registration.service;

import com.edu.registration.dto.LearningAnalyticsDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningAnalyticsServiceTest {

    @Mock
    private CourseClassRepository lopHocPhanRepository;

    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;

    @Mock
    private SemesterRepository hocKyRepository;

    @Mock
    private FacultyRepository khoaRepository;

    @Mock
    private MajorRepository nganhRepository;

    @Mock
    private SubjectRepository monHocRepository;

    @Mock
    private LecturerRepository giangVienRepository;

    @Mock
    private SemesterService hocKyService;

    @Mock
    private CurrentUser currentUser;

    private LearningAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new LearningAnalyticsService(
                lopHocPhanRepository,
                dangKyHocPhanRepository,
                hocKyRepository,
                khoaRepository,
                nganhRepository,
                monHocRepository,
                giangVienRepository,
                hocKyService,
                currentUser
        );
    }

    @Test
    void tinhKpiPhanBoDiemVaDatTruotKhopTaskAd11DiemThieuKhongThanhKhong() {
        User admin = taoUser(1L, "admin", Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(admin);

        Semester hk1 = taoHocKy(1L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));
        Subject mon1 = taoMon(10L, "CS101", "Nhập môn Lập trình", 3);

        CourseClass lopDaCongBo = taoLop(101L, "CS101.01", mon1, hk1, GradeSheetStatus.DA_CONG_BO, 10);
        CourseClass lopChuaCongBo = taoLop(102L, "CS101.02", mon1, hk1, GradeSheetStatus.CHO_DUYET, 5);

        when(lopHocPhanRepository.findForAdminAnalytics(null, null, null, null))
                .thenReturn(List.of(lopDaCongBo, lopChuaCongBo));

        // 7 SV có điểm công bố của lớp 101, 3 SV chưa có điểm (tổng 10 ĐK). Lớp 102 có 5 ĐK chưa công bố.
        // Điểm: 3.5 (trượt, kém), 4.5 (trượt, yếu), 5.5 (đạt, TB), 6.0 (đạt, TB), 7.5 (đạt, khá), 8.5 (đạt, giỏi), 10.0 (đạt, giỏi)
        List<CourseRegistrationRepository.DiemTheoLopProjection> diemLop101 = List.of(
                row(101L, 3.5),
                row(101L, 4.5),
                row(101L, 5.5),
                row(101L, 6.0),
                row(101L, 7.5),
                row(101L, 8.5),
                row(101L, 10.0)
        );

        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(101L), RegistrationStatus.DA_DANG_KY))
                .thenReturn(diemLop101);
        when(hocKyRepository.findAllByOrderByNgayBatDauAsc()).thenReturn(List.of(hk1));

        LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto baoCao = service.layBaoCaoChoQuanTri(
                null, null, null, null, null);

        assertNotNull(baoCao);
        LearningAnalyticsDtos.ChiSoKpiHocTapDto kpi = baoCao.kpi();

        assertEquals(2, kpi.tongSoLop());
        assertEquals(1, kpi.soLopDaCongBo());
        assertEquals(1, kpi.soLopChuaCongBo());
        assertEquals(15, kpi.tongSoDangKyHieuLuc());
        // Mẫu số M chỉ tính 7 SV của lớp đã công bố
        assertEquals(7, kpi.mauSoDuocTinh());
        // 15 - 7 = 8 SV chưa có điểm công bố (không bị gán 0.0)
        assertEquals(8, kpi.soSinhVienChuaCoDiem());

        // Điểm TB: (3.5 + 4.5 + 5.5 + 6.0 + 7.5 + 8.5 + 10.0) / 7 = 45.5 / 7 = 6.50
        assertEquals(6.50, kpi.diemTrungBinh());
        assertEquals(3.5, kpi.diemThapNhat());
        assertEquals(10.0, kpi.diemCaoNhat());

        // Đạt (>= 5.0): 5 SV, Trượt (< 5.0): 2 SV
        assertEquals(5, kpi.soSinhVienDat());
        assertEquals(2, kpi.soSinhVienTruot());
        assertEquals(71.43, kpi.tyLeDat());
        assertEquals(28.57, kpi.tyLeTruot());

        // 5 khoảng phân bố điểm
        assertEquals(5, baoCao.phanBoDiem().size());
        assertEquals(1, baoCao.phanBoDiem().get(0).soLuong()); // [0.0, 4.0): 3.5
        assertEquals(1, baoCao.phanBoDiem().get(1).soLuong()); // [4.0, 5.0): 4.5
        assertEquals(2, baoCao.phanBoDiem().get(2).soLuong()); // [5.0, 6.5): 5.5, 6.0
        assertEquals(1, baoCao.phanBoDiem().get(3).soLuong()); // [6.5, 8.0): 7.5
        assertEquals(2, baoCao.phanBoDiem().get(4).soLuong()); // [8.0, 10.0]: 8.5, 10.0

        assertNotNull(baoCao.canhBaoNhanQua());
        assertTrue(baoCao.canhBaoNhanQua().contains("không suy diễn kết luận nhân quả"));
    }

    @Test
    void xuHuongGpaTheoHocKyDungThuTuThoiGianVaXuLyLopChuaCoDiem() {
        User admin = taoUser(1L, "admin", Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(admin);

        Semester hk1 = taoHocKy(1L, "Học kỳ 1", "2025-2026", LocalDate.of(2025, 9, 1));
        Semester hk2 = taoHocKy(2L, "Học kỳ 2", "2025-2026", LocalDate.of(2026, 2, 1));
        Semester hk3 = taoHocKy(3L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));

        Subject mon = taoMon(10L, "CS101", "Lập trình", 3);
        CourseClass lopHk1 = taoLop(201L, "CS101.01", mon, hk1, GradeSheetStatus.DA_CONG_BO, 10);
        CourseClass lopHk2 = taoLop(202L, "CS101.02", mon, hk2, GradeSheetStatus.NHAP, 8); // Chưa công bố
        CourseClass lopHk3 = taoLop(203L, "CS101.03", mon, hk3, GradeSheetStatus.DA_KHOA, 12);

        when(hocKyRepository.findAllByOrderByNgayBatDauAsc()).thenReturn(List.of(hk1, hk2, hk3));

        when(lopHocPhanRepository.findForAdminAnalytics(eq(1L), any(), any(), any())).thenReturn(List.of(lopHk1));
        when(lopHocPhanRepository.findForAdminAnalytics(eq(2L), any(), any(), any())).thenReturn(List.of(lopHk2));
        when(lopHocPhanRepository.findForAdminAnalytics(eq(3L), any(), any(), any())).thenReturn(List.of(lopHk3));

        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(201L), RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(row(201L, 7.0), row(201L, 8.0)));
        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(203L), RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(row(203L, 9.0), row(203L, 8.5), row(203L, 9.5)));

        List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> xuHuong = service.layXuHuongGpaChoQuanTri(null, null, null);

        assertEquals(3, xuHuong.size());

        // HK1: có điểm công bố
        LearningAnalyticsDtos.XuHuongGpaHocKyDto x1 = xuHuong.get(0);
        assertEquals("Học kỳ 1", x1.tenHocKy());
        assertEquals(2, x1.mauSoDuocTinh());
        assertEquals(7.50, x1.diemTrungBinh());
        assertTrue(x1.coDiemCongBo());

        // HK2: chưa công bố, M = 0, GPA = null (không bị ép thành 0.0)
        LearningAnalyticsDtos.XuHuongGpaHocKyDto x2 = xuHuong.get(1);
        assertEquals("Học kỳ 2", x2.tenHocKy());
        assertEquals(0, x2.mauSoDuocTinh());
        assertNull(x2.diemTrungBinh());
        assertNull(x2.gpaThang4());
        assertFalse(x2.coDiemCongBo());
        assertEquals("Chưa công bố bảng điểm", x2.ghiChu());

        // HK3: đã khóa/công bố
        LearningAnalyticsDtos.XuHuongGpaHocKyDto x3 = xuHuong.get(2);
        assertEquals(3, x3.mauSoDuocTinh());
        assertEquals(9.00, x3.diemTrungBinh());
        assertTrue(x3.coDiemCongBo());
    }

    @Test
    void soSanhLopCungMonPhanBietTrangThaiCongBoVaCanhBaoCoMauNho() {
        User admin = taoUser(1L, "admin", Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(admin);

        Semester hk = taoHocKy(1L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));
        Subject mon = taoMon(10L, "MATH101", "Giải tích", 4);

        when(monHocRepository.findById(10L)).thenReturn(Optional.of(mon));
        when(hocKyRepository.findById(1L)).thenReturn(Optional.of(hk));

        // Lớp 1: Đã công bố, cỡ mẫu M = 6 (>= 5, đủ điều kiện)
        CourseClass lop1 = taoLop(301L, "MATH101.01", mon, hk, GradeSheetStatus.DA_CONG_BO, 10);
        // Lớp 2: Đã công bố, cỡ mẫu M = 3 (< 5, cảnh báo cỡ mẫu nhỏ)
        CourseClass lop2 = taoLop(302L, "MATH101.02", mon, hk, GradeSheetStatus.DA_CONG_BO, 5);
        // Lớp 3: Chưa công bố (Bản nháp)
        CourseClass lop3 = taoLop(303L, "MATH101.03", mon, hk, GradeSheetStatus.NHAP, 8);

        when(lopHocPhanRepository.findForAdminAnalytics(1L, null, 10L, null))
                .thenReturn(List.of(lop1, lop2, lop3));

        List<CourseRegistrationRepository.DiemTheoLopProjection> allGrades = List.of(
                row(301L, 6.0), row(301L, 7.0), row(301L, 8.0), row(301L, 6.5), row(301L, 7.5), row(301L, 8.5),
                row(302L, 7.0), row(302L, 8.0), row(302L, 9.0)
        );

        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(301L, 302L, 303L), RegistrationStatus.DA_DANG_KY))
                .thenReturn(allGrades);

        LearningAnalyticsDtos.SoSanhCungMonDto soSanh = service.laySoSanhCungMonChoQuanTri(10L, 1L);

        assertNotNull(soSanh);
        assertEquals(3, soSanh.tongSoLop());
        assertEquals(2, soSanh.soLopDaCongBo());
        // Chỉ lớp 1 có M >= 5
        assertEquals(1, soSanh.soLopDuDieuKienSoSanh());
        assertFalse(soSanh.coTheSoSanh()); // cần >= 2 lớp đủ điều kiện để so sánh chính thức

        List<LearningAnalyticsDtos.SoSanhLopItemDto> lops = soSanh.danhSachLop();
        assertEquals(3, lops.size());

        // Lớp 1: đủ điều kiện
        assertTrue(lops.get(0).duDieuKienSoSanhChinhThuc());
        assertTrue(lops.get(0).duSoMauToiThieu());

        // Lớp 2: đã công bố nhưng cỡ mẫu nhỏ (M=3 < 5)
        assertTrue(lops.get(1).duDieuKienSoSanhChinhThuc());
        assertFalse(lops.get(1).duSoMauToiThieu());
        assertTrue(lops.get(1).ghiChuDieuKien().contains("Cỡ mẫu nhỏ"));

        // Lớp 3: chưa công bố
        assertFalse(lops.get(2).duDieuKienSoSanhChinhThuc());
        assertTrue(lops.get(2).ghiChuDieuKien().contains("Bảng điểm chưa công bố"));

        assertNotNull(soSanh.canhBaoNhanQua());
    }

    @Test
    void phanQuyenBackendTuChoiSinhVienVaKiemSoatPhamViGiangVien() {
        // Sinh viên bị cấm
        User sinhVien = taoUser(2L, "student", Role.SINH_VIEN);
        when(currentUser.get()).thenReturn(sinhVien);

        assertThrows(ApiException.class, () -> service.layBaoCaoChoQuanTri(null, null, null, null, null));
        assertThrows(ApiException.class, () -> service.layBaoCaoChoGiangVien(null, null, null));

        // Giảng viên xem môn mình không phụ trách bị 403
        User gvUser = taoUser(3L, "lecturer", Role.GIANG_VIEN);
        when(currentUser.get()).thenReturn(gvUser);

        Lecturer gv = new Lecturer();
        gv.setId(30L);
        gv.setUser(gvUser);
        when(giangVienRepository.findByUser(gvUser)).thenReturn(Optional.of(gv));

        Subject monKhongDay = taoMon(99L, "PHYS101", "Vật lý", 3);
        when(monHocRepository.findById(99L)).thenReturn(Optional.of(monKhongDay));
        when(lopHocPhanRepository.findByGiangVienIdOrderByMaAsc(30L)).thenReturn(List.of());

        assertThrows(ApiException.class, () -> service.layBaoCaoChoGiangVien(null, 99L, null));
    }

    @Test
    void xuatCsvChuanUtf8BomMetadataVaKhongChuaPii() {
        User admin = taoUser(1L, "admin", Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(admin);

        Semester hk = taoHocKy(1L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));
        Subject mon = taoMon(10L, "CS101", "Lập trình", 3);
        CourseClass lop = taoLop(101L, "CS101.01", mon, hk, GradeSheetStatus.DA_CONG_BO, 10);

        when(lopHocPhanRepository.findForAdminAnalytics(null, null, null, null)).thenReturn(List.of(lop));
        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(101L), RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(row(101L, 8.0)));
        when(hocKyRepository.findAllByOrderByNgayBatDauAsc()).thenReturn(List.of(hk));

        byte[] csv = service.xuatCsvChoQuanTri(null, null, null, null);

        assertNotNull(csv);
        assertTrue(csv.length > 3);
        // Kiểm tra UTF-8 BOM: 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, csv[0]);
        assertEquals((byte) 0xBB, csv[1]);
        assertEquals((byte) 0xBF, csv[2]);

        String csvString = new String(csv, StandardCharsets.UTF_8);
        assertTrue(csvString.contains("BÁO CÁO PHÂN TÍCH HỌC TẬP (LEARNING ANALYTICS - QLSV)"));
        assertTrue(csvString.contains("Chính sách điểm thiếu"));
        assertTrue(csvString.contains("Cảnh báo nhân quả"));
        assertTrue(csvString.contains("Zero PII"));
        assertTrue(csvString.contains("Tỷ lệ Đạt"));
        assertFalse(csvString.contains("maSinhVien"));
        assertFalse(csvString.contains("hoTenSinhVien"));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User taoUser(Long id, String username, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setRole(role);
        return u;
    }

    private Semester taoHocKy(Long id, String ten, String namHoc, LocalDate ngayBatDau) {
        Semester s = new Semester();
        s.setId(id);
        s.setTen(ten);
        s.setNamHoc(namHoc);
        s.setNgayBatDau(ngayBatDau);
        s.setNgayKetThuc(ngayBatDau.plusMonths(4));
        s.setDangKyTu(ngayBatDau.minusWeeks(2));
        s.setDangKyDen(ngayBatDau.plusWeeks(1));
        return s;
    }

    private Subject taoMon(Long id, String ma, String ten, int soTinChi) {
        Subject s = new Subject();
        s.setId(id);
        s.setMa(ma);
        s.setTen(ten);
        s.setSoTinChi(soTinChi);
        return s;
    }

    private CourseClass taoLop(Long id, String ma, Subject mon, Semester hk, GradeSheetStatus status, int siSoHienTai) {
        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hk);
        lop.setTrangThaiBangDiem(status);
        lop.setSiSoToiDa(40);
        lop.setSiSoHienTai(siSoHienTai);
        return lop;
    }

    private CourseRegistrationRepository.DiemTheoLopProjection row(Long lopId, Double diemTongKet) {
        return new CourseRegistrationRepository.DiemTheoLopProjection() {
            @Override
            public Long getLopHocPhanId() {
                return lopId;
            }

            @Override
            public Double getDiemQuaTrinh() {
                return diemTongKet;
            }

            @Override
            public Double getDiemCuoiKy() {
                return diemTongKet;
            }

            @Override
            public Double getDiemTongKet() {
                return diemTongKet;
            }
        };
    }
}
