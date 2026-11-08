package com.edu.registration.service;

import com.edu.registration.config.GradeAnalyticsProperties;
import com.edu.registration.dto.GradeAnalyticsDtos;
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
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GradeAnalyticsServiceTest {

    @Mock
    private CourseClassRepository lopHocPhanRepository;

    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;

    @Mock
    private LecturerRepository giangVienRepository;

    @Mock
    private SubjectRepository monHocRepository;

    @Mock
    private FacultyRepository khoaRepository;

    @Mock
    private SemesterService hocKyService;

    @Mock
    private CurrentUser currentUser;

    private GradeAnalyticsProperties properties;
    private GradeAnalyticsService service;

    @BeforeEach
    void setUp() {
        properties = new GradeAnalyticsProperties();
        service = new GradeAnalyticsService(
                lopHocPhanRepository,
                dangKyHocPhanRepository,
                giangVienRepository,
                monHocRepository,
                khoaRepository,
                hocKyService,
                currentUser,
                properties
        );
    }

    @Test
    void phanBoKhoangDiemChinhXacTaiGiaTriBienKhongChongLanKhongBoSot() {
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setRole(Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(admin);

        CourseClass lopCongBo = taoLop(10L, "IT101-01", GradeSheetStatus.DA_CONG_BO, 100L, "GV01");
        when(lopHocPhanRepository.findForGradeAnalytics(null, null, null, null, null))
                .thenReturn(List.of(lopCongBo));

        // 10 sinh viên ở đúng các điểm biên: 0.0, 3.99, 4.0, 4.99, 5.0, 6.49, 6.5, 7.99, 8.0, 10.0
        List<CourseRegistrationRepository.DiemTheoLopProjection> rows = List.of(
                row(10L, 0.0, 0.0, 0.0),
                row(10L, 3.99, 3.99, 3.99),
                row(10L, 4.0, 4.0, 4.0),
                row(10L, 4.99, 4.99, 4.99),
                row(10L, 5.0, 5.0, 5.0),
                row(10L, 6.49, 6.49, 6.49),
                row(10L, 6.5, 6.5, 6.5),
                row(10L, 7.99, 7.99, 7.99),
                row(10L, 8.0, 8.0, 8.0),
                row(10L, 10.0, 10.0, 10.0)
        );
        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(eq(List.of(10L)), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(rows);

        GradeAnalyticsDtos.BaoCaoThongKeDiemDto baoCao = service.thongKeChoQuanTri(null, null, null, null);
        GradeAnalyticsDtos.ChiSoThongKeDiemDto tk = baoCao.tongHopCongBo();

        assertEquals(10L, tk.tongSoDangKyHieuLuc());
        assertEquals(10L, tk.soBanGhiDuocTinh());
        assertEquals(0L, tk.soSinhVienChuaCoDiem());
        assertEquals(6L, tk.soSinhVienDat()); // >= 5.0: 5.0, 6.49, 6.5, 7.99, 8.0, 10.0
        assertEquals(4L, tk.soSinhVienTruot()); // < 5.0: 0.0, 3.99, 4.0, 4.99
        assertEquals(60.0, tk.tyLeDat());
        assertEquals(40.0, tk.tyLeTruot());
        assertEquals(0.0, tk.diemThapNhat());
        assertEquals(10.0, tk.diemCaoNhat());
        // Tổng = 0 + 3.99 + 4.0 + 4.99 + 5.0 + 6.49 + 6.5 + 7.99 + 8.0 + 10.0 = 56.96 -> TB = 5.696 -> làm tròn 5.70
        assertEquals(5.7, tk.diemTrungBinh());

        assertEquals(10L, tk.tongSoTrongPhanBo());
        assertEquals(5, tk.phanBoDiem().size());
        // Mỗi khoảng nhận đúng 2 bản ghi biên
        for (GradeAnalyticsDtos.KhoangPhanBoDiemDto bucket : tk.phanBoDiem()) {
            assertEquals(2L, bucket.soLuong(), "Khoảng " + bucket.maKhoang() + " phải có đúng 2 bản ghi");
            assertEquals(20.0, bucket.tyLePhanTram());
        }
    }

    @Test
    void sinhVienChuaCoDiemKhongTinhThanh0VaLopChuaCongBoKhongTronVaoThongKeCongBo() {
        User gvUser = new User();
        gvUser.setId(2L);
        gvUser.setUsername("gv01");
        gvUser.setRole(Role.GIANG_VIEN);

        Lecturer gv = new Lecturer();
        gv.setId(100L);
        gv.setMa("GV01");
        gv.setHoTen("Nguyen Van A");
        gv.setUser(gvUser);

        when(currentUser.get()).thenReturn(gvUser);
        when(giangVienRepository.findByUser(gvUser)).thenReturn(Optional.of(gv));

        CourseClass lopDaCongBo = taoLop(11L, "IT101-01", GradeSheetStatus.DA_CONG_BO, 100L, "GV01");
        CourseClass lopNhap = taoLop(12L, "IT101-02", GradeSheetStatus.NHAP, 100L, "GV01");

        when(lopHocPhanRepository.findForGradeAnalytics(null, null, null, 100L, null))
                .thenReturn(List.of(lopDaCongBo, lopNhap));

        // Lớp 11 (Đã công bố): 2 SV có điểm (8.0 và 6.0), 1 SV thiếu điểm cuối kỳ (null)
        // Lớp 12 (Nháp - chưa công bố): 2 SV đã nhập điểm nháp (9.0 và 3.0), 1 SV chưa nhập điểm
        List<CourseRegistrationRepository.DiemTheoLopProjection> rows = List.of(
                row(11L, 8.0, 8.0, 8.0),
                row(11L, 6.0, 6.0, 6.0),
                row(11L, 7.0, null, null),
                row(12L, 9.0, 9.0, 9.0),
                row(12L, 3.0, 3.0, 3.0),
                row(12L, null, null, null)
        );
        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(eq(List.of(11L, 12L)), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(rows);

        GradeAnalyticsDtos.BaoCaoThongKeDiemDto baoCao = service.thongKeChoGiangVien(null, null, null);
        GradeAnalyticsDtos.ChiSoThongKeDiemDto tongHop = baoCao.tongHopCongBo();

        // Tổng đăng ký hiệu lực của cả 2 lớp = 6, nhưng chỉ 2 bản ghi thuộc lớp đã công bố và đủ điểm được đưa vào mẫu số
        assertEquals(6L, tongHop.tongSoDangKyHieuLuc());
        assertEquals(2L, tongHop.soBanGhiDuocTinh());
        assertEquals(4L, tongHop.soSinhVienChuaCoDiem());
        assertEquals(2L, tongHop.soSinhVienChuaNhapDuDiem()); // 2 SV chưa nhập đủ điểm (1 ở lớp công bố + 1 ở lớp nháp)
        assertEquals(2L, tongHop.soSinhVienChuaCongBoDiem()); // 2 SV đã có điểm ở lớp nháp nhưng chưa công bố
        // Điểm trung bình chỉ tính trên (8.0 + 6.0) / 2 = 7.0 (không tính SV thiếu điểm thành 0, không trộn lớp nháp)
        assertEquals(7.0, tongHop.diemTrungBinh());
        assertEquals(6.0, tongHop.diemThapNhat());
        assertEquals(8.0, tongHop.diemCaoNhat());
        assertEquals(2L, tongHop.soSinhVienDat());
        assertEquals(0L, tongHop.soSinhVienTruot());
        assertEquals(100.0, tongHop.tyLeDat());
        assertEquals(2L, tongHop.tongSoTrongPhanBo());

        // Kiểm tra chi tiết từng lớp
        assertEquals(2, baoCao.theoLop().size());
        GradeAnalyticsDtos.ThongKeDiemLopItemDto itemCongBo = baoCao.theoLop().stream()
                .filter(i -> i.lopHocPhanId().equals(11L)).findFirst().orElseThrow();
        assertTrue(itemCongBo.daCongBo());
        assertNull(itemCongBo.thongKeTamTinhChuaCongBo());
        assertEquals(2L, itemCongBo.thongKeCongBo().soBanGhiDuocTinh());
        assertEquals(1L, itemCongBo.thongKeCongBo().soSinhVienChuaCoDiem());

        GradeAnalyticsDtos.ThongKeDiemLopItemDto itemNhap = baoCao.theoLop().stream()
                .filter(i -> i.lopHocPhanId().equals(12L)).findFirst().orElseThrow();
        assertFalse(itemNhap.daCongBo());
        // Thống kê công bố của lớp nháp phải có mẫu số = 0
        assertEquals(0L, itemNhap.thongKeCongBo().soBanGhiDuocTinh());
        assertNull(itemNhap.thongKeCongBo().diemTrungBinh());
        // Nhưng giảng viên phụ trách được xem khối tạm tính riêng biệt có gắn nhãn cảnh báo
        assertNotNull(itemNhap.thongKeTamTinhChuaCongBo());
        assertEquals(GradeAnalyticsService.PHAM_VI_TAM_TINH, itemNhap.thongKeTamTinhChuaCongBo().phamViDuLieu());
        assertEquals(2L, itemNhap.thongKeTamTinhChuaCongBo().soBanGhiDuocTinh());
        assertEquals(6.0, itemNhap.thongKeTamTinhChuaCongBo().diemTrungBinh());
    }

    @Test
    void giangVienTruyCapLopKhongPhuTrachBiChan403Forbidden() {
        User gvUser = new User();
        gvUser.setId(2L);
        gvUser.setUsername("gv01");
        gvUser.setRole(Role.GIANG_VIEN);

        Lecturer gv01 = new Lecturer();
        gv01.setId(100L);
        gv01.setMa("GV01");
        gv01.setHoTen("GV 01");
        gv01.setUser(gvUser);

        when(currentUser.get()).thenReturn(gvUser);
        when(giangVienRepository.findByUser(gvUser)).thenReturn(Optional.of(gv01));

        CourseClass lopCuaGv02 = taoLop(99L, "IT999-01", GradeSheetStatus.DA_CONG_BO, 200L, "GV02");
        when(lopHocPhanRepository.findWithDetailsById(99L)).thenReturn(Optional.of(lopCuaGv02));

        ApiException ex1 = assertThrows(ApiException.class, () -> service.thongKeMotLop(99L));
        assertEquals(HttpStatus.FORBIDDEN, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () -> service.thongKeChoGiangVien(null, null, 99L));
        assertEquals(HttpStatus.FORBIDDEN, ex2.getStatus());
    }

    @Test
    void validateCauHinhKhoangDiemPhatHienChongLanHoacHo() {
        GradeAnalyticsProperties badProps = new GradeAnalyticsProperties();
        badProps.setBuckets(List.of(
                new GradeAnalyticsProperties.BucketDef(
                        "F", "Kém", "[0.0, 4.0)",
                        new BigDecimal("0.0"), new BigDecimal("4.0"), true, false),
                new GradeAnalyticsProperties.BucketDef(
                        "D", "Yếu", "[4.5, 10.0]",
                        new BigDecimal("4.5"), new BigDecimal("10.0"), true, true) // Hở [4.0, 4.5)
        ));
        assertThrows(IllegalArgumentException.class, () -> new GradeAnalyticsService(
                lopHocPhanRepository,
                dangKyHocPhanRepository,
                giangVienRepository,
                monHocRepository,
                khoaRepository,
                hocKyService,
                currentUser,
                badProps
        ));
    }

    private CourseClass taoLop(Long id, String maLop, GradeSheetStatus status, Long gvId, String maGv) {
        Faculty khoa = new Faculty();
        khoa.setId(1L);
        khoa.setMa("CNTT");
        khoa.setTen("Công nghệ thông tin");

        Subject mon = new Subject();
        mon.setId(50L);
        mon.setMa("IT101");
        mon.setTen("Lập trình Java");
        mon.setSoTinChi(3);
        mon.setKhoa(khoa);

        Semester hk = new Semester();
        hk.setId(5L);
        hk.setTen("Học kỳ 1");
        hk.setNamHoc("2025-2026");

        Lecturer gv = new Lecturer();
        gv.setId(gvId);
        gv.setMa(maGv);
        gv.setHoTen("Giảng viên " + maGv);
        gv.setKhoa(khoa);

        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(maLop);
        lop.setMonHoc(mon);
        lop.setHocKy(hk);
        lop.setGiangVien(gv);
        lop.setSiSoToiDa(40);
        lop.setTrangThaiBangDiem(status);
        return lop;
    }

    private CourseRegistrationRepository.DiemTheoLopProjection row(Long lopId, Double qt, Double ck, Double tk) {
        return new CourseRegistrationRepository.DiemTheoLopProjection() {
            @Override
            public Long getLopHocPhanId() {
                return lopId;
            }

            @Override
            public Double getDiemQuaTrinh() {
                return qt;
            }

            @Override
            public Double getDiemCuoiKy() {
                return ck;
            }

            @Override
            public Double getDiemTongKet() {
                return tk;
            }
        };
    }
}
