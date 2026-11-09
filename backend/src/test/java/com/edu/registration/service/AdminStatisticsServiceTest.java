package com.edu.registration.service;

import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Curriculum;
import com.edu.registration.entity.CurriculumGroup;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminStatisticsServiceTest {

    private StudentRepository sinhVienRepository;
    private LecturerRepository giangVienRepository;
    private SubjectRepository monHocRepository;
    private CourseClassRepository lopHocPhanRepository;
    private CourseRegistrationRepository dangKyHocPhanRepository;
    private GradeRepository diemRepository;
    private SemesterService hocKyService;
    private FacultyRepository khoaRepository;
    private MajorRepository nganhRepository;
    private CurriculumRepository chuongTrinhRepository;
    private CurrentUser currentUser;

    private StatisticsService service;

    private Semester hocKy;
    private Faculty khoa;
    private Major nganh;
    private Subject monHoc;
    private CourseClass lopDaCongBo;
    private CourseClass lopChuaCongBo;

    @BeforeEach
    void setUp() {
        sinhVienRepository = mock(StudentRepository.class);
        giangVienRepository = mock(LecturerRepository.class);
        monHocRepository = mock(SubjectRepository.class);
        lopHocPhanRepository = mock(CourseClassRepository.class);
        dangKyHocPhanRepository = mock(CourseRegistrationRepository.class);
        diemRepository = mock(GradeRepository.class);
        hocKyService = mock(SemesterService.class);
        khoaRepository = mock(FacultyRepository.class);
        nganhRepository = mock(MajorRepository.class);
        chuongTrinhRepository = mock(CurriculumRepository.class);
        currentUser = mock(CurrentUser.class);

        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setRole(Role.QUAN_TRI);
        when(currentUser.get()).thenReturn(adminUser);

        service = new StatisticsService(sinhVienRepository, giangVienRepository, monHocRepository,
                lopHocPhanRepository, dangKyHocPhanRepository, diemRepository, hocKyService,
                khoaRepository, nganhRepository, chuongTrinhRepository, currentUser);

        khoa = new Faculty();
        khoa.setId(1L);
        khoa.setMa("CNTT");
        khoa.setTen("Công nghệ thông tin");

        nganh = new Major();
        nganh.setId(10L);
        nganh.setMa("CNPM");
        nganh.setTen("Kỹ thuật phần mềm");
        nganh.setKhoa(khoa);

        monHoc = new Subject();
        monHoc.setId(100L);
        monHoc.setMa("INT101");
        monHoc.setTen("Lập trình căn bản");
        monHoc.setSoTinChi(3);
        monHoc.setKhoa(khoa);

        hocKy = new Semester();
        hocKy.setId(1000L);
        hocKy.setTen("Học kỳ 1");
        hocKy.setNamHoc("2025-2026");
        hocKy.setDangKyTu(LocalDate.now().minusDays(10));
        hocKy.setDangKyDen(LocalDate.now().plusDays(10));
        hocKy.setNgayKetThuc(LocalDate.now().plusMonths(3));

        when(hocKyService.hocKyHienTai()).thenReturn(hocKy);
        when(hocKyService.lay(1000L)).thenReturn(hocKy);
        when(khoaRepository.findById(1L)).thenReturn(Optional.of(khoa));
        when(nganhRepository.findWithKhoaById(10L)).thenReturn(Optional.of(nganh));
        when(monHocRepository.findById(100L)).thenReturn(Optional.of(monHoc));

        lopDaCongBo = new CourseClass();
        lopDaCongBo.setId(2001L);
        lopDaCongBo.setMa("INT101_01");
        lopDaCongBo.setMonHoc(monHoc);
        lopDaCongBo.setHocKy(hocKy);
        lopDaCongBo.setSiSoToiDa(50);
        lopDaCongBo.setSiSoHienTai(40);
        lopDaCongBo.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);

        lopChuaCongBo = new CourseClass();
        lopChuaCongBo.setId(2002L);
        lopChuaCongBo.setMa("INT101_02");
        lopChuaCongBo.setMonHoc(monHoc);
        lopChuaCongBo.setHocKy(hocKy);
        lopChuaCongBo.setSiSoToiDa(50);
        lopChuaCongBo.setSiSoHienTai(30);
        lopChuaCongBo.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
    }

    @Test
    void validateFilters_shouldRejectInvalidInputs() {
        assertThrows(ApiException.class, () -> service.layDashboardKpi(-1L, null, null, null, null));
        assertThrows(ApiException.class, () -> service.layDashboardKpi(null, -1L, null, null, null));
        assertThrows(ApiException.class, () -> service.layDashboardKpi(null, null, -1L, null, null));

        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(2L);
        otherFaculty.setTen("Kinh tế");
        when(khoaRepository.findById(2L)).thenReturn(Optional.of(otherFaculty));

        // Ngành 10 thuộc khoa 1, nhưng filter truyền khoa 2 -> badRequest
        assertThrows(ApiException.class, () -> service.layDashboardKpi(null, 2L, 10L, null, null));
    }

    @Test
    void layDashboardKpi_shouldCalculateSingleSourceOfTruthCorrectly() {
        when(lopHocPhanRepository.findForAdminAnalytics(eq(1000L), eq(1L), eq(null), eq(null)))
                .thenReturn(List.of(lopDaCongBo, lopChuaCongBo));

        // Mock 40 sinh viên có điểm của lớp đã công bố
        // 30 đạt (>= 5.0), 10 trượt (< 5.0)
        CourseRegistrationRepository.DiemTheoLopProjection pDat1 = createProjection(2001L, 8.5, 9.0, 8.8);
        CourseRegistrationRepository.DiemTheoLopProjection pDat2 = createProjection(2001L, 7.0, 7.5, 7.3);
        CourseRegistrationRepository.DiemTheoLopProjection pTruot = createProjection(2001L, 3.0, 4.0, 3.5);

        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(eq(List.of(2001L)), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(pDat1, pDat2, pTruot));

        // Mock tiến độ học tập
        Student sv = new Student();
        sv.setId(11L);
        sv.setKhoa(khoa);
        sv.setNganh(nganh);
        Curriculum ct = new Curriculum();
        ct.setId(50L);
        CurriculumGroup g = new CurriculumGroup();
        g.setTinChiYeuCau(3);
        g.getMonHoc().add(monHoc);
        ct.getNhom().add(g);
        sv.setChuongTrinh(ct);

        when(sinhVienRepository.findByKhoaIdAndNganhIdForAnalytics(1L, null))
                .thenReturn(List.of(sv));
        when(diemRepository.findMonDaDat(11L, AcademicRules.DIEM_DAT))
                .thenReturn(List.of(100L));

        StatDtos.DashboardKpiResponseDto result = service.layDashboardKpi(1000L, 1L, null, null, null);

        assertNotNull(result);
        assertEquals(2, result.kpi().tongSoLop());
        assertEquals(100, result.kpi().tongChiTieu()); // 50 + 50
        assertEquals(70, result.kpi().tongDangKy());   // 40 + 30
        assertEquals(70.0, result.kpi().tiLeLapDay()); // 70 / 100 * 100%
        assertEquals(30, result.kpi().conTrong());

        // Điểm chỉ tính từ lớp đã công bố (lopDaCongBo: 3 bản ghi)
        assertEquals(3, result.kpi().soSinhVienCoDiem());
        assertEquals(2, result.kpi().soSinhVienDat());   // 8.8, 7.3
        assertEquals(1, result.kpi().soSinhVienTruot()); // 3.5
        assertEquals(66.67, result.kpi().tyLeDat());     // 2 / 3 * 100%
        assertEquals(33.33, result.kpi().tyLeTruot());   // 1 / 3 * 100%
        assertEquals(67, result.kpi().soSinhVienChuaCoDiem()); // 70 - 3 = 67

        // Phân bố điểm 5 buckets
        assertEquals(5, result.phanBoDiem().size());
        assertEquals(1, result.phanBoDiem().get(0).soLuong()); // [0, 4) -> 3.5
        assertEquals(1, result.phanBoDiem().get(3).soLuong()); // [6.5, 8.0) -> 7.3
        assertEquals(1, result.phanBoDiem().get(4).soLuong()); // [8.0, 10.0] -> 8.8

        // Danh sách môn
        assertEquals(1, result.danhSachMon().size());
        StatDtos.ThongKeMonChiTietDto monStat = result.danhSachMon().getFirst();
        assertEquals("INT101", monStat.maMon());
        assertEquals(2, monStat.soLop());
        assertEquals(70, monStat.daDangKy());
        assertEquals(3, monStat.soSinhVienCoDiem());
    }

    @Test
    void xuatBaoCaoThongKeCsv_shouldProduceProperCsvWithBOMAndNoPII() {
        when(lopHocPhanRepository.findForAdminAnalytics(eq(1000L), eq(1L), eq(null), eq(null)))
                .thenReturn(List.of(lopDaCongBo));

        CourseRegistrationRepository.DiemTheoLopProjection pDat = createProjection(2001L, 8.0, 8.0, 8.0);
        when(dangKyHocPhanRepository.findDiemTheoDanhSachLop(eq(List.of(2001L)), eq(RegistrationStatus.DA_DANG_KY)))
                .thenReturn(List.of(pDat));
        when(sinhVienRepository.findByKhoaIdAndNganhIdForAnalytics(1L, null))
                .thenReturn(List.of());

        byte[] csvBytes = service.xuatBaoCaoThongKeCsv(1000L, 1L, null, null, null);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 3);

        // Kiểm tra UTF-8 BOM: 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, csvBytes[0]);
        assertEquals((byte) 0xBB, csvBytes[1]);
        assertEquals((byte) 0xBF, csvBytes[2]);

        String csvText = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("BÁO CÁO THỐNG KÊ VÀ DASHBOARD ĐÀO TẠO"));
        assertTrue(csvText.contains("Công nghệ thông tin"));
        assertTrue(csvText.contains("Học kỳ 1 - 2025-2026"));
        assertTrue(csvText.contains("ĐÃ CÔNG BỐ (Chính thức)"));
        assertTrue(csvText.contains("INT101"));
        assertTrue(csvText.contains("Lập trình căn bản"));

        // Bảo vệ Zero PII: Tuyệt đối không chứa MSSV hoặc họ tên sinh viên
        assertFalse(csvText.contains("SV001"));
        assertFalse(csvText.contains("Nguyễn Văn A"));
    }

    private CourseRegistrationRepository.DiemTheoLopProjection createProjection(Long lopId, Double qt, Double ck, Double tk) {
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
