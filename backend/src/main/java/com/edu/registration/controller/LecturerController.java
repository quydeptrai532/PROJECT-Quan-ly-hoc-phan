package com.edu.registration.controller;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.GradeImportDtos;
import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.service.ClassAbsenceNoticeService;
import com.edu.registration.service.ClassStudentExportService;
import com.edu.registration.service.CourseClassAnnouncementService;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.GradeImportService;
import com.edu.registration.service.GradeService;
import com.edu.registration.service.LecturerDashboardService;
import com.edu.registration.service.StudentAcademicProfileService;
import com.edu.registration.util.XlsxWorkbookSupport;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('GIANG_VIEN')")
public class LecturerController {

    private final CourseClassService lopHocPhanService;
    private final GradeService diemService;
    private final ClassAbsenceNoticeService thongBaoNghiService;
    private final ClassStudentExportService lopSinhVienExportService;
    private final LecturerDashboardService giangVienDashboardService;
    private final StudentAcademicProfileService studentAcademicProfileService;
    private final CourseClassAnnouncementService thongBaoLopHocPhanService;
    private final GradeImportService gradeImportService;

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, null, null, null);
    }

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, studentAcademicProfileService, null, null);
    }

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService,
                               CourseClassAnnouncementService thongBaoLopHocPhanService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, studentAcademicProfileService, thongBaoLopHocPhanService, null);
    }

    @Autowired
    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService,
                               CourseClassAnnouncementService thongBaoLopHocPhanService,
                               GradeImportService gradeImportService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
        this.thongBaoNghiService = thongBaoNghiService;
        this.lopSinhVienExportService = lopSinhVienExportService;
        this.giangVienDashboardService = giangVienDashboardService;
        this.studentAcademicProfileService = studentAcademicProfileService;
        this.thongBaoLopHocPhanService = thongBaoLopHocPhanService;
        this.gradeImportService = gradeImportService;
    }

    @GetMapping("/giang-vien/me/ho-so")
    public LecturerDtos.LecturerProfileDto hoSoCuaToi() {
        return giangVienDashboardService.hoSo();
    }

    @GetMapping("/giang-vien/me/dashboard")
    public LecturerDtos.LecturerDashboardDto dashboard(@RequestParam(required = false) Long hocKyId) {
        return giangVienDashboardService.dashboard(hocKyId);
    }

    @GetMapping("/giang-vien/me/lop-hoc-phan")
    public List<CourseClassDtos.LopHocPhanDto> lopCuaToi(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thoi-khoa-bieu")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.thoiKhoaBieuCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thong-bao-nghi")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaGiangVien(hocKyId);
    }

    @PostMapping("/giang-vien/me/thong-bao-nghi")
    public RegistrationDtos.ThongBaoNghiDto taoThongBaoNghi(
            @Valid @RequestBody RegistrationDtos.ThongBaoNghiRequest request) {
        return thongBaoNghiService.tao(request);
    }

    @DeleteMapping("/giang-vien/me/thong-bao-nghi/{id}")
    public void xoaThongBaoNghi(@PathVariable Long id) {
        thongBaoNghiService.xoa(id);
    }

    // --- Class Announcements (QLSV-68 / TASK-LC-03) ---

    @PostMapping("/giang-vien/lop-hoc-phan/{id}/thong-bao")
    public LecturerDtos.ThongBaoLopDto taoThongBaoLop(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody LecturerDtos.TaoThongBaoLopRequest request) {
        return thongBaoLopHocPhanService.taoThongBao(id, request, idempotencyKeyHeader);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/thong-bao")
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoLop(@PathVariable Long id) {
        return thongBaoLopHocPhanService.danhSachThongBaoLop(id);
    }

    @GetMapping("/giang-vien/me/thong-bao-lop")
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoCuaToi(@RequestParam(required = false) Long lopHocPhanId) {
        return thongBaoLopHocPhanService.danhSachThongBaoCuaGiangVien(lopHocPhanId);
    }

    // --------------------------------------------------

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public List<RegistrationDtos.DiemDto> danhSachSinhVien(
            @PathVariable Long id,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return diemService.danhSachDiemLop(id, kw);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}")
    public LecturerDtos.StudentMinimalProfileDto chiTietSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, lopId);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so",
                 "/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so-dao-tao"})
    public StudentAcademicProfileDto hoSoDaoTaoSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, lopId);
    }

    @GetMapping("/giang-vien/sinh-vien")
    public List<LecturerDtos.StudentMinimalProfileDto> danhSachSinhVienPhuTrach(
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return studentAcademicProfileService.getAssignedStudentsForLecturer(lopHocPhanId, kw);
    }

    @GetMapping("/giang-vien/sinh-vien/{studentId}")
    public LecturerDtos.StudentMinimalProfileDto hoSoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, null);
    }

    @GetMapping({"/giang-vien/sinh-vien/{studentId}/ho-so", "/giang-vien/sinh-vien/{studentId}/ho-so-dao-tao"})
    public StudentAcademicProfileDto hoSoDaoTaoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, null);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv")
    public ResponseEntity<byte[]> xuatDanhSachSinhVien(@PathVariable Long id,
                                                        @RequestParam(required = false) String tuKhoa,
                                                        @RequestParam(required = false) String sapXep) {
        ClassStudentExportService.CsvFile tep = lopSinhVienExportService.xuatDanhSach(id, tuKhoa, sapXep);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(tep.tenTep(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @PutMapping("/diem/{dangKyId}")
    public RegistrationDtos.DiemDto nhapDiem(@PathVariable Long dangKyId,
                                            @Valid @RequestBody RegistrationDtos.NhapDiemRequest request) {
        return diemService.nhapDiem(dangKyId, request);
    }

    @PutMapping({"/giang-vien/lop-hoc-phan/{id}/diem", "/diem/lop-hoc-phan/{id}"})
    public List<RegistrationDtos.DiemDto> capNhatDiemHangLoat(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationDtos.CapNhatDiemHangLoatRequest request) {
        return diemService.capNhatDiemHangLoat(id, request);
    }

    // --- Grade Excel (.xlsx) Import (QLSV-73 / TASK-LC-08) ---

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/cau-hinh",
                 "/diem/lop-hoc-phan/{id}/import-xlsx/cau-hinh"})
    public GradeImportDtos.GradeImportConfigDto cauHinhImportDiemXlsx(@PathVariable Long id) {
        return gradeImportService.layCauHinhImport(id);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx",
                 "/giang-vien/lop-hoc-phan/{id}/diem/template",
                 "/diem/lop-hoc-phan/{id}/template-xlsx"})
    public ResponseEntity<byte[]> taiTemplateDiemXlsx(@PathVariable Long id) {
        GradeImportService.XlsxFile tep = gradeImportService.xuatTemplateXlsx(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(XlsxWorkbookSupport.XLSX_MEDIA_TYPE));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(tep.tenTep(), StandardCharsets.UTF_8).build());
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @PostMapping(
            value = {"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview",
                     "/diem/lop-hoc-phan/{id}/import-xlsx/preview"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public GradeImportDtos.PreviewResponse xemTruocImportDiemXlsx(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return gradeImportService.xemTruocImport(id, file);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm",
                  "/diem/lop-hoc-phan/{id}/import-xlsx/confirm"})
    public GradeImportDtos.ConfirmImportResponse xacNhanImportDiemXlsx(
            @PathVariable Long id,
            @Valid @RequestBody GradeImportDtos.ConfirmImportRequest request) {
        return gradeImportService.xacNhanImport(id, request);
    }

    // --- Grade Sheet Approval & Locking Workflow (QLSV-74 / TASK-LC-09) ---

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/trang-thai-bang-diem", "/diem/lop-hoc-phan/{id}/trang-thai"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/lich-su-bang-diem", "/diem/lop-hoc-phan/{id}/lich-su-trang-thai"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public List<RegistrationDtos.LichSuTrangThaiBangDiemDto> lichSuTrangThaiBangDiem(@PathVariable Long id) {
        return diemService.lichSuTrangThaiBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem", "/diem/lop-hoc-phan/{id}/gui-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto guiDuyetBangDiem(@PathVariable Long id) {
        return diemService.guiDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/tra-ve-diem", "/diem/lop-hoc-phan/{id}/tra-ve"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto traVeBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.TraVeBangDiemRequest request) {
        return diemService.traVeBangDiem(id, request != null ? request.lyDo() : null);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/phe-duyet-diem", "/diem/lop-hoc-phan/{id}/phe-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/cong-bo-diem", "/diem/lop-hoc-phan/{id}/cong-bo"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/khoa-diem", "/diem/lop-hoc-phan/{id}/khoa"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto khoaBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.KhoaBangDiemRequest request) {
        return diemService.khoaBangDiem(id, request != null ? request.lyDo() : null);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/mo-khoa-diem",
                  "/giang-vien/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem",
                  "/diem/lop-hoc-phan/{id}/mo-khoa",
                  "/diem/lop-hoc-phan/{id}/thu-hoi-cong-bo"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public RegistrationDtos.TrangThaiBangDiemDto moKhoaBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.MoKhoaBangDiemRequest request) {
        return diemService.moKhoaBangDiem(id, request != null ? request.lyDo() : null);
    }
}
