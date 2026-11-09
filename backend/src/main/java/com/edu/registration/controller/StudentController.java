package com.edu.registration.controller;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.service.RegistrationService;
import com.edu.registration.service.RegistrationPeriodService;
import com.edu.registration.service.GradeService;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.CourseClassAnnouncementService;
import com.edu.registration.service.ClassAbsenceNoticeService;
import com.edu.registration.service.StudentAcademicProfileService;
import com.edu.registration.service.StudentDashboardService;
import com.edu.registration.service.RetakeCourseService;
import com.edu.registration.service.StudentTranscriptPdfService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StudentController {

    private final CourseClassService lopHocPhanService;
    private final RegistrationService dangKyService;
    private final GradeService diemService;
    private final ClassAbsenceNoticeService thongBaoNghiService;
    private final StudentAcademicProfileService studentAcademicProfileService;
    private final StudentDashboardService sinhVienDashboardService;
    private final RetakeCourseService hocLaiCaiThienService;
    private final StudentTranscriptPdfService studentTranscriptPdfService;
    private final CourseClassAnnouncementService thongBaoLopHocPhanService;
    private final RegistrationPeriodService registrationPeriodService;

    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService) {
        this(lopHocPhanService, dangKyService, diemService, thongBaoNghiService,
                studentAcademicProfileService, sinhVienDashboardService, hocLaiCaiThienService, null, null, null);
    }

    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService,
                              StudentTranscriptPdfService studentTranscriptPdfService) {
        this(lopHocPhanService, dangKyService, diemService, thongBaoNghiService,
                studentAcademicProfileService, sinhVienDashboardService, hocLaiCaiThienService,
                studentTranscriptPdfService, null, null);
    }

    @Autowired
    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService,
                              StudentTranscriptPdfService studentTranscriptPdfService,
                              CourseClassAnnouncementService thongBaoLopHocPhanService,
                              @Autowired(required = false) RegistrationPeriodService registrationPeriodService) {
        this.lopHocPhanService = lopHocPhanService;
        this.dangKyService = dangKyService;
        this.diemService = diemService;
        this.thongBaoNghiService = thongBaoNghiService;
        this.studentAcademicProfileService = studentAcademicProfileService;
        this.sinhVienDashboardService = sinhVienDashboardService;
        this.hocLaiCaiThienService = hocLaiCaiThienService;
        this.studentTranscriptPdfService = studentTranscriptPdfService;
        this.thongBaoLopHocPhanService = thongBaoLopHocPhanService;
        this.registrationPeriodService = registrationPeriodService;
    }

    @GetMapping("/lop-hoc-phan")
    public List<CourseClassDtos.LopHocPhanDto> danhSachLopHocPhan(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String keyword) {
        return lopHocPhanService.danhSach(hocKyId, keyword);
    }

    @GetMapping("/lop-hoc-phan/{id}")
    public CourseClassDtos.LopHocPhanDto chiTietLopHocPhan(@PathVariable Long id) {
        return lopHocPhanService.chiTiet(id);
    }

    @PostMapping("/dang-ky")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.DangKyDto dangKy(@Valid @RequestBody RegistrationDtos.DangKyRequest request) {
        return dangKyService.dangKy(request.lopHocPhanId(), request.loaiDangKy());
    }

    @GetMapping("/dang-ky/kiem-tra-quan-he")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.KetQuaKiemTraQuanHeDangKyDto kiemTraQuanHeDangKy(
            @RequestParam Long lopHocPhanId) {
        return dangKyService.kiemTraQuanHeDangKy(lopHocPhanId);
    }

    @PostMapping("/dang-ky/xac-nhan-ke-hoach")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.DangKyDto> xacNhanKeHoach(
            @Valid @RequestBody RegistrationDtos.XacNhanKeHoachRequest request) {
        return dangKyService.xacNhanKeHoach(request.lopHocPhanIds());
    }

    @DeleteMapping("/dang-ky/{id}")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public void huyDangKy(@PathVariable Long id,
                          @RequestParam(required = false) @Size(max = 255) String lyDo) {
        dangKyService.huyDangKy(id, lyDo);
    }

    @GetMapping("/dang-ky/me")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.DangKyDto> dangKyCuaToi(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/dang-ky/dot-hien-tai")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationPeriodDtos.TrangThaiDangKySinhVienDto layTrangThaiDotDangKyCuaToi(
            @RequestParam(required = false) Long hocKyId) {
        if (registrationPeriodService == null) {
            return null;
        }
        return registrationPeriodService.layTrangThaiDangKySinhVien(hocKyId);
    }

    @GetMapping("/dang-ky/me/lich-su")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public Page<RegistrationDtos.LichSuDangKyDto> lichSuDangKyCuaToi(
            @RequestParam Long hocKyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return dangKyService.lichSuCuaToi(hocKyId, page, size);
    }

    @GetMapping("/sinh-vien/me/thoi-khoa-bieu")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.thoiKhoaBieu(hocKyId);
    }


    @GetMapping("/sinh-vien/lop-hoc-phan/{id}/thong-bao")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<LecturerDtos.ThongBaoLopDto> thongBaoLop(@PathVariable Long id) {
        if (thongBaoLopHocPhanService == null) {
            return List.of();
        }
        return thongBaoLopHocPhanService.danhSachThongBaoChoSinhVien(id);
    }

    @GetMapping("/sinh-vien/me/thong-bao-nghi")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaSinhVien(hocKyId);
    }

    @GetMapping("/sinh-vien/me/diem")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.KetQuaHocTapDto ketQuaHocTap() {
        return diemService.ketQuaHocTap();
    }

    @GetMapping("/sinh-vien/me/lich-su-hoc-tap")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public RegistrationDtos.LichSuHocTapDto lichSuHocTap(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String trangThai) {
        return diemService.lichSuHocTap(hocKyId, trangThai);
    }

    @GetMapping("/sinh-vien/me/bang-diem/pdf")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public ResponseEntity<byte[]> xuatBangDiemPdf(@RequestParam(required = false) Long hocKyId) {
        StudentTranscriptPdfService.PdfFile tep = studentTranscriptPdfService.xuatBangDiemPdf(hocKyId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(tep.tenTep(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @GetMapping("/sinh-vien/me/ho-so-dao-tao")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public StudentAcademicProfileDto hoSoDaoTao() {
        return studentAcademicProfileService.getMyProfile();
    }

    @GetMapping("/sinh-vien/me/canh-bao-dashboard")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<StudentDashboardService.CanhBaoDto> canhBaoDashboard() {
        return sinhVienDashboardService.canhBao();
    }

    @GetMapping("/sinh-vien/me/dang-ky-hoc-lai-cai-thien")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public List<RegistrationDtos.DangKyHocLaiDto> danhSachHocLaiCaiThien(
            @RequestParam(required = false) Long hocKyId) {
        return hocLaiCaiThienService.danhSach(hocKyId);
    }
}
