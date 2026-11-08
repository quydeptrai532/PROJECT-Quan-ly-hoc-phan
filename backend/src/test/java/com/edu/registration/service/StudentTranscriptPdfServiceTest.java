package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentTranscriptPdfServiceTest {

    @Mock
    private CourseRegistrationRepository dangKyHocPhanRepository;
    @Mock
    private GradeRepository diemRepository;
    @Mock
    private CourseClassRepository lopHocPhanRepository;
    @Mock
    private StudentRepository sinhVienRepository;
    @Mock
    private LecturerRepository giangVienRepository;
    @Mock
    private SemesterRepository hocKyRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private StudentAcademicProfileService studentAcademicProfileService;

    private GradeService diemService;
    private Clock fixedClock;
    private User user;
    private Student sinhVien;
    private Lecturer giangVien;

    @BeforeEach
    void setUp() {
        diemService = new GradeService(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository,
                sinhVienRepository, giangVienRepository, hocKyRepository, currentUser, new DtoMapper());
        fixedClock = Clock.fixed(Instant.parse("2026-11-05T00:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

        user = new User();
        user.setId(10L);
        user.setUsername("sv01");

        sinhVien = new Student();
        sinhVien.setId(1L);
        sinhVien.setUser(user);
        sinhVien.setMa("SV001");
        sinhVien.setHoTen("Trần Minh Anh");
        sinhVien.setEmail("anh.tm@student.edu.vn");

        giangVien = new Lecturer();
        giangVien.setId(7L);
        giangVien.setMa("GV01");
        giangVien.setHoTen("TS. Nguyễn Văn An");
    }

    private void dangNhapSinhVien() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findWithAcademicProfileByUserId(10L)).thenReturn(Optional.of(sinhVien));
        when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "K2024", "CNPM01", "Kỹ thuật phần mềm 01", "K2024", "2024.1", "Chưa cập nhật"));
    }

    @Test
    void xuatBangDiemPdfChiChuaDiemDaCongBoVaKhopGpaTiengViet() throws Exception {
        dangNhapSinhVien();

        Semester hk1 = taoHocKy(1L, "Học kỳ 1", "2025-2026", LocalDate.of(2025, 9, 1));
        Semester hk2 = taoHocKy(2L, "Học kỳ 2", "2025-2026", LocalDate.of(2026, 2, 1));
        Semester hk3 = taoHocKy(3L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));

        Subject int101 = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        Subject mat101 = taoMon(12L, "MAT101", "Toán rời rạc", 3);
        Subject eco101 = taoMon(13L, "ECO101", "Kinh tế vi mô", 3);

        CourseClass lopLan1 = taoLop(201L, "INT101.01-HK1", int101, hk1);
        CourseClass lopLan2 = taoLop(202L, "INT101.01-HK2", int101, hk2);
        CourseClass lopNhapChuaCongBo = taoLop(203L, "MAT101.01-HK3", mat101, hk3);
        CourseClass lopChuaCoDiem = taoLop(204L, "ECO101.01-HK3", eco101, hk3);

        CourseRegistration dkLan1 = taoDangKy(401L, lopLan1, LocalDateTime.of(2025, 8, 20, 8, 0));
        CourseRegistration dkLan2 = taoDangKy(402L, lopLan2, LocalDateTime.of(2026, 1, 20, 8, 0));
        CourseRegistration dkNhap = taoDangKy(403L, lopNhapChuaCongBo, LocalDateTime.of(2026, 8, 20, 8, 0));
        CourseRegistration dkChuaDiem = taoDangKy(404L, lopChuaCoDiem, LocalDateTime.of(2026, 8, 21, 8, 0));

        Grade diemTruot = taoDiem(dkLan1, 4.0, 4.0, 4.0);
        Grade diemDat = taoDiem(dkLan2, 7.5, 8.0, 7.8);
        Grade diemNhap = taoDiem(dkNhap, 9.5, null, null);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dkChuaDiem, dkNhap, dkLan2, dkLan1));
        when(diemRepository.findByDangKyIdIn(any()))
                .thenReturn(List.of(diemTruot, diemDat, diemNhap));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(null);

        assertEquals("bang-diem-SV001.pdf", file.tenTep());
        assertEquals(2, file.soMonDaCongBo());
        assertNotNull(file.noiDung());
        assertTrue(file.noiDung().length > 1000);
        assertEquals("%PDF-", new String(file.noiDung(), 0, 5, StandardCharsets.US_ASCII));

        RegistrationDtos.BangDiemPdfDto duLieu = file.duLieu();
        assertNull(duLieu.donViPhatHanh());
        assertEquals(2, duLieu.hocKy().size());
        assertEquals(6, duLieu.tongTinChiCoDiem());
        assertEquals(3, duLieu.tinChiTichLuy());
        assertEquals(5.9, duLieu.gpa10());
        assertEquals(2.0, duLieu.gpa4());
        assertTrue(duLieu.coNhieuLanHoc());

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            assertEquals(1, reader.getNumberOfPages());
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");

            assertTrue(text.contains("BẢNG ĐIỂM HỌC TẬP CÁ NHÂN"), text);
            assertTrue(text.contains("Trần Minh Anh"), text);
            assertTrue(text.contains("SV001"), text);
            assertTrue(text.contains("Công nghệ thông tin (CNTT)"), text);
            assertTrue(text.contains("Kỹ thuật phần mềm (CNPM)"), text);
            assertTrue(text.contains("Lập trình cơ bản"), text);
            assertTrue(text.contains("INT101.01-HK1 (Lần 1/2)"), text);
            assertTrue(text.contains("INT101.01-HK2 (Lần 2/2)"), text);
            assertTrue(text.contains("Chưa đạt"), text);
            assertTrue(text.contains("Đạt"), text);
            assertTrue(text.contains("5.90"), text);
            assertTrue(text.contains("2.00"), text);
            assertTrue(text.contains("05/11/2026 07:00:00"), text);

            // Điểm nháp/chưa công bố tuyệt đối không xuất hiện trong PDF
            assertFalse(text.contains("MAT101"), text);
            assertFalse(text.contains("Toán rời rạc"), text);
            assertFalse(text.contains("9.5"), text);
            assertFalse(text.contains("ECO101"), text);
            assertFalse(text.contains("Kinh tế vi mô"), text);
            assertFalse(text.contains("Học kỳ 1 - Năm học 2026-2027"), text);
        }
    }

    @Test
    void xuatBangDiemPdfHienThiDonViPhatHanhKhiDaCauHinhVaNgatTrangNhieuMon() throws Exception {
        dangNhapSinhVien();

        Semester hk1 = taoHocKy(1L, "Học kỳ 1", "2025-2026", LocalDate.of(2025, 9, 1));
        List<CourseRegistration> danhSachDangKy = new ArrayList<>();
        List<Grade> danhSachDiem = new ArrayList<>();

        for (int i = 1; i <= 38; i++) {
            Subject mon = taoMon((long) i, String.format("INT%03d", i),
                    "Chuyên đề công nghệ phần mềm nâng cao số " + i, 3);
            CourseClass lop = taoLop((long) (100 + i), String.format("INT%03d.01", i), mon, hk1);
            CourseRegistration dk = taoDangKy((long) (500 + i), lop, LocalDateTime.of(2025, 8, 15, 8, 0).plusMinutes(i));
            Grade diem = taoDiem(dk, 8.0, 8.5, 8.3);
            danhSachDangKy.add(dk);
            danhSachDiem.add(diem);
        }

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(danhSachDangKy);
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(danhSachDiem);

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService,
                "Trường Đại học Công nghệ", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(null);

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            assertTrue(reader.getNumberOfPages() >= 2);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String page1 = extractor.getTextFromPage(1).replaceAll("\\s+", " ");
            String page2 = extractor.getTextFromPage(2).replaceAll("\\s+", " ");

            assertTrue(page1.contains("TRƯỜNG ĐẠI HỌC CÔNG NGHỆ"), page1);
            assertTrue(page1.contains("Chuyên đề công nghệ phần mềm nâng cao số 1"), page1);
            assertTrue(page2.contains("Tên môn học"), page2);
            assertTrue(page2.contains("Chuyên đề công nghệ phần mềm nâng cao số 38"), page2);
        }
    }

    @Test
    void xuatBangDiemPdfBaoLoiRoRangKhiChuaCoKetQuaDaCongBo() {
        when(currentUser.get()).thenReturn(user);
        when(sinhVienRepository.findWithAcademicProfileByUserId(10L)).thenReturn(Optional.of(sinhVien));
        when(sinhVienRepository.findByUser(user)).thenReturn(Optional.of(sinhVien));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "Chưa cập nhật", "CNPM01", "Kỹ thuật phần mềm 01", "Chưa cập nhật", "Chưa cập nhật", "Chưa cập nhật"));

        Semester hk1 = taoHocKy(1L, "Học kỳ 1", "2026-2027", LocalDate.of(2026, 9, 1));
        Subject mon = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        CourseClass lop = taoLop(201L, "INT101.01", mon, hk1);
        CourseRegistration dk = taoDangKy(401L, lop, LocalDateTime.of(2026, 8, 20, 8, 0));
        Grade diemNhap = taoDiem(dk, 8.0, null, null);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(1L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dk));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(diemNhap));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        ApiException ex = assertThrows(ApiException.class, () -> service.xuatBangDiemPdf(null));
        assertEquals("Chưa có kết quả học tập đã công bố để xuất bảng điểm PDF", ex.getMessage());
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void xuatBangDiemPdfTheoHocKyVaChiLayDuLieuCuaSinhVienHienTai() throws Exception {
        User userSv2 = new User();
        userSv2.setId(20L);
        userSv2.setUsername("sv02");

        Student sv2 = new Student();
        sv2.setId(2L);
        sv2.setUser(userSv2);
        sv2.setMa("SV002");
        sv2.setHoTen("Phạm Thu Hà");
        sv2.setEmail("ha.pt@student.edu.vn");

        when(currentUser.get()).thenReturn(userSv2);
        when(sinhVienRepository.findWithAcademicProfileByUserId(20L)).thenReturn(Optional.of(sv2));
        when(sinhVienRepository.findByUser(userSv2)).thenReturn(Optional.of(sv2));
        when(studentAcademicProfileService.getMyProfile()).thenReturn(new StudentAcademicProfileDto(
                "CNTT", "Công nghệ thông tin", "CNPM", "Kỹ thuật phần mềm",
                "Chưa cập nhật", "CNPM01", "Kỹ thuật phần mềm 01", "Chưa cập nhật", "Chưa cập nhật", "Chưa cập nhật"));
        when(hocKyRepository.existsById(2L)).thenReturn(true);

        Semester hk2 = taoHocKy(2L, "Học kỳ 2", "2025-2026", LocalDate.of(2026, 2, 1));
        Subject int101 = taoMon(11L, "INT101", "Lập trình cơ bản", 3);
        CourseClass lop = taoLop(202L, "INT101.01-HK2", int101, hk2);
        CourseRegistration dkSv2 = new CourseRegistration();
        dkSv2.setId(501L);
        dkSv2.setSinhVien(sv2);
        dkSv2.setLopHocPhan(lop);
        dkSv2.setThoiGianDangKy(LocalDateTime.of(2026, 1, 20, 8, 0));
        dkSv2.setTrangThai(RegistrationStatus.DA_DANG_KY);
        Grade diemSv2 = taoDiem(dkSv2, 7.0, 8.0, 7.6);

        when(dangKyHocPhanRepository.findAllActiveOfStudent(2L, RegistrationStatus.DA_DANG_KY))
                .thenReturn(List.of(dkSv2));
        when(diemRepository.findByDangKyIdIn(any())).thenReturn(List.of(diemSv2));

        StudentTranscriptPdfService service = new StudentTranscriptPdfService(
                currentUser, sinhVienRepository, studentAcademicProfileService, diemService, "", fixedClock);

        StudentTranscriptPdfService.PdfFile file = service.xuatBangDiemPdf(2L);
        assertEquals("bang-diem-SV002-hk2.pdf", file.tenTep());

        try (PdfReader reader = new PdfReader(file.noiDung())) {
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            assertTrue(text.contains("Phạm Thu Hà"), text);
            assertTrue(text.contains("SV002"), text);
            assertFalse(text.contains("Trần Minh Anh"), text);
            assertFalse(text.contains("SV001"), text);
        }
    }

    private Semester taoHocKy(Long id, String ten, String namHoc, LocalDate ngayBatDau) {
        Semester hocKy = new Semester();
        hocKy.setId(id);
        hocKy.setTen(ten);
        hocKy.setNamHoc(namHoc);
        hocKy.setNgayBatDau(ngayBatDau);
        return hocKy;
    }

    private Subject taoMon(Long id, String ma, String ten, int soTinChi) {
        Subject mon = new Subject();
        mon.setId(id);
        mon.setMa(ma);
        mon.setTen(ten);
        mon.setSoTinChi(soTinChi);
        return mon;
    }

    private CourseClass taoLop(Long id, String ma, Subject mon, Semester hocKy) {
        CourseClass lop = new CourseClass();
        lop.setId(id);
        lop.setMa(ma);
        lop.setMonHoc(mon);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        return lop;
    }

    private CourseRegistration taoDangKy(Long id, CourseClass lop, LocalDateTime thoiGian) {
        CourseRegistration dk = new CourseRegistration();
        dk.setId(id);
        dk.setSinhVien(sinhVien);
        dk.setLopHocPhan(lop);
        dk.setThoiGianDangKy(thoiGian);
        dk.setTrangThai(RegistrationStatus.DA_DANG_KY);
        return dk;
    }

    private Grade taoDiem(CourseRegistration dk, Double qt, Double ck, Double tk) {
        Grade diem = new Grade();
        diem.setDangKy(dk);
        diem.setDiemQuaTrinh(qt);
        diem.setDiemCuoiKy(ck);
        diem.setDiemTongKet(tk);
        return diem;
    }
}
