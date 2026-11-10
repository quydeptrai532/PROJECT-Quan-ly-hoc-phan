package com.edu.registration.controller;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.GradeAnalyticsDtos;
import com.edu.registration.dto.GradeImportDtos;
import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.service.ClassAbsenceNoticeService;
import com.edu.registration.service.ClassStudentExportService;
import com.edu.registration.service.CourseClassAnnouncementService;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.GradeAnalyticsService;
import com.edu.registration.service.GradeImportService;
import com.edu.registration.service.GradeService;
import com.edu.registration.service.LecturerDashboardService;
import com.edu.registration.service.StudentAcademicProfileService;
import com.edu.registration.util.ExportSecuritySupport;
import com.edu.registration.util.XlsxWorkbookSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Giảng viên - Giảng dạy & Đánh giá", description = "Các API dành cho Giảng viên: quản lý lớp phụ trách, thời khóa biểu, nhập điểm, import điểm Excel, phê duyệt bảng điểm và thông báo lớp")
public class LecturerController {

    private final CourseClassService lopHocPhanService;
    private final GradeService diemService;
    private final ClassAbsenceNoticeService thongBaoNghiService;
    private final ClassStudentExportService lopSinhVienExportService;
    private final LecturerDashboardService giangVienDashboardService;
    private final StudentAcademicProfileService studentAcademicProfileService;
    private final CourseClassAnnouncementService thongBaoLopHocPhanService;
    private final GradeImportService gradeImportService;
    private final GradeAnalyticsService gradeAnalyticsService;

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, null, null, null, null);
    }

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, studentAcademicProfileService, null, null, null);
    }

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService,
                               CourseClassAnnouncementService thongBaoLopHocPhanService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, studentAcademicProfileService, thongBaoLopHocPhanService, null, null);
    }

    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService,
                               CourseClassAnnouncementService thongBaoLopHocPhanService,
                               GradeImportService gradeImportService) {
        this(lopHocPhanService, diemService, thongBaoNghiService, lopSinhVienExportService,
                giangVienDashboardService, studentAcademicProfileService, thongBaoLopHocPhanService, gradeImportService, null);
    }

    @Autowired
    public LecturerController(CourseClassService lopHocPhanService, GradeService diemService,
                               ClassAbsenceNoticeService thongBaoNghiService, ClassStudentExportService lopSinhVienExportService,
                               LecturerDashboardService giangVienDashboardService,
                               StudentAcademicProfileService studentAcademicProfileService,
                               CourseClassAnnouncementService thongBaoLopHocPhanService,
                               GradeImportService gradeImportService,
                               GradeAnalyticsService gradeAnalyticsService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
        this.thongBaoNghiService = thongBaoNghiService;
        this.lopSinhVienExportService = lopSinhVienExportService;
        this.giangVienDashboardService = giangVienDashboardService;
        this.studentAcademicProfileService = studentAcademicProfileService;
        this.thongBaoLopHocPhanService = thongBaoLopHocPhanService;
        this.gradeImportService = gradeImportService;
        this.gradeAnalyticsService = gradeAnalyticsService;
    }

    @GetMapping("/giang-vien/me/ho-so")
    @Operation(summary = "Xem hồ sơ giảng viên", description = "Lấy thông tin cá nhân, học vị, khoa bộ môn của giảng viên hiện tại")
    public LecturerDtos.LecturerProfileDto hoSoCuaToi() {
        return giangVienDashboardService.hoSo();
    }

    @GetMapping("/giang-vien/me/dashboard")
    @Operation(summary = "Xem tổng quan Dashboard giảng viên", description = "Tổng hợp số lớp phụ trách, số sinh viên, tiến độ nhập điểm và cảnh báo")
    public LecturerDtos.LecturerDashboardDto dashboard(@RequestParam(required = false) Long hocKyId) {
        return giangVienDashboardService.dashboard(hocKyId);
    }

    @GetMapping("/giang-vien/me/lop-hoc-phan")
    @Operation(summary = "Xem danh sách lớp học phần phụ trách", description = "Lấy các lớp học phần được phân công giảng dạy theo học kỳ")
    public List<CourseClassDtos.LopHocPhanDto> lopCuaToi(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thoi-khoa-bieu")
    @Operation(summary = "Xem lịch giảng dạy", description = "Xem thời khóa biểu giảng dạy các lớp học phần trong tuần hoặc học kỳ")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.thoiKhoaBieuCuaToi(hocKyId);
    }

    @GetMapping("/giang-vien/me/thong-bao-nghi")
    @Operation(summary = "Xem danh sách thông báo báo nghỉ", description = "Tra cứu các thông báo báo nghỉ/báo bận do giảng viên đã tạo")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaGiangVien(hocKyId);
    }

    @PostMapping("/giang-vien/me/thong-bao-nghi")
    @Operation(summary = "Tạo thông báo báo nghỉ và học bù", description = "Đăng ký báo nghỉ cho buổi học và lên lịch dạy bù tương ứng")
    public RegistrationDtos.ThongBaoNghiDto taoThongBaoNghi(
            @Valid @RequestBody RegistrationDtos.ThongBaoNghiRequest request) {
        return thongBaoNghiService.tao(request);
    }

    @DeleteMapping("/giang-vien/me/thong-bao-nghi/{id}")
    @Operation(summary = "Hủy thông báo báo nghỉ", description = "Xóa thông báo báo nghỉ khi có thay đổi kế hoạch")
    public void xoaThongBaoNghi(@PathVariable Long id) {
        thongBaoNghiService.xoa(id);
    }

    // --- Class Announcements (QLSV-68 / TASK-LC-03) ---

    @PostMapping("/giang-vien/lop-hoc-phan/{id}/thong-bao")
    @Operation(summary = "Đăng thông báo lớp học phần", description = "Gửi thông báo mới đến toàn thể sinh viên trong lớp học phần")
    public LecturerDtos.ThongBaoLopDto taoThongBaoLop(
            @PathVariable Long id,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody LecturerDtos.TaoThongBaoLopRequest request) {
        return thongBaoLopHocPhanService.taoThongBao(id, request, idempotencyKeyHeader);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/thong-bao")
    @Operation(summary = "Xem danh sách thông báo lớp học phần", description = "Lấy các thông báo đã gửi trong một lớp học phần cụ thể")
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoLop(@PathVariable Long id) {
        return thongBaoLopHocPhanService.danhSachThongBaoLop(id);
    }

    @GetMapping("/giang-vien/me/thong-bao-lop")
    @Operation(summary = "Xem tất cả thông báo lớp do giảng viên tạo", description = "Lấy toàn bộ thông báo lớp học phần do chính giảng viên hiện tại ban hành")
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoCuaToi(@RequestParam(required = false) Long lopHocPhanId) {
        return thongBaoLopHocPhanService.danhSachThongBaoCuaGiangVien(lopHocPhanId);
    }

    // --------------------------------------------------

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem danh sách sinh viên và bảng điểm lớp", description = "Xem danh sách sinh viên đăng ký lớp kèm các cột điểm quá trình, giữa kỳ, cuối kỳ")
    public List<RegistrationDtos.DiemDto> danhSachSinhVien(
            @PathVariable Long id,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return diemService.danhSachDiemLop(id, kw);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}")
    @Operation(summary = "Xem hồ sơ rút gọn sinh viên trong lớp", description = "Xem thông tin họ tên, mã sinh viên và lớp hành chính trong phạm vi lớp giảng dạy")
    public LecturerDtos.StudentMinimalProfileDto chiTietSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, lopId);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so",
                 "/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}/ho-so-dao-tao"})
    @Operation(summary = "Xem hồ sơ học vụ sinh viên trong lớp", description = "Xem thông tin chương trình đào tạo của sinh viên trong phạm vi lớp phụ trách")
    public StudentAcademicProfileDto hoSoDaoTaoSinhVienLop(
            @PathVariable Long lopId,
            @PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, lopId);
    }

    @GetMapping("/giang-vien/sinh-vien")
    @Operation(summary = "Xem danh sách sinh viên tất cả lớp phụ trách", description = "Tra cứu sinh viên theo từ khóa hoặc lọc theo từng lớp phụ trách")
    public List<LecturerDtos.StudentMinimalProfileDto> danhSachSinhVienPhuTrach(
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String keyword) {
        String kw = tuKhoa != null ? tuKhoa : keyword;
        return studentAcademicProfileService.getAssignedStudentsForLecturer(lopHocPhanId, kw);
    }

    @GetMapping("/giang-vien/sinh-vien/{studentId}")
    @Operation(summary = "Xem hồ sơ sinh viên phụ trách", description = "Tra cứu hồ sơ rút gọn của sinh viên thuộc diện giảng dạy")
    public LecturerDtos.StudentMinimalProfileDto hoSoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getMinimalProfileForLecturer(studentId, null);
    }

    @GetMapping({"/giang-vien/sinh-vien/{studentId}/ho-so", "/giang-vien/sinh-vien/{studentId}/ho-so-dao-tao"})
    @Operation(summary = "Xem hồ sơ đào tạo sinh viên phụ trách", description = "Tra cứu hồ sơ đào tạo của sinh viên thuộc diện giảng dạy")
    public StudentAcademicProfileDto hoSoDaoTaoSinhVien(@PathVariable Long studentId) {
        return studentAcademicProfileService.getProfileDtoForLecturer(studentId, null);
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv")
    @Operation(summary = "Xuất danh sách sinh viên lớp ra CSV", description = "Tải về tệp CSV danh sách sinh viên lớp phục vụ lưu trữ ngoại tuyến")
    public ResponseEntity<byte[]> xuatDanhSachSinhVien(@PathVariable Long id,
                                                        @RequestParam(required = false) String tuKhoa,
                                                        @RequestParam(required = false) String sapXep) {
        ClassStudentExportService.CsvFile tep = lopSinhVienExportService.xuatDanhSach(id, tuKhoa, sapXep);
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @PutMapping("/diem/{dangKyId}")
    @Operation(summary = "Nhập điểm cho một sinh viên", description = "Cập nhật điểm chuyên cần, giữa kỳ hoặc cuối kỳ cho một sinh viên trong lớp")
    public RegistrationDtos.DiemDto nhapDiem(@PathVariable Long dangKyId,
                                            @Valid @RequestBody RegistrationDtos.NhapDiemRequest request) {
        return diemService.nhapDiem(dangKyId, request);
    }

    @PutMapping({"/giang-vien/lop-hoc-phan/{id}/diem", "/diem/lop-hoc-phan/{id}"})
    @Operation(summary = "Cập nhật điểm hàng loạt cho lớp", description = "Lưu danh sách điểm của nhiều sinh viên cùng lúc kèm kiểm soát xung đột dữ liệu Optimistic Locking")
    public List<RegistrationDtos.DiemDto> capNhatDiemHangLoat(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationDtos.CapNhatDiemHangLoatRequest request) {
        return diemService.capNhatDiemHangLoat(id, request);
    }

    // --- Grade Excel (.xlsx) Import (QLSV-73 / TASK-LC-08) ---

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/cau-hinh",
                 "/diem/lop-hoc-phan/{id}/import-xlsx/cau-hinh"})
    @Operation(summary = "Lấy cấu hình import điểm Excel", description = "Xem các giới hạn dung lượng tệp, số dòng tối đa và chính sách giao dịch import điểm")
    public GradeImportDtos.GradeImportConfigDto cauHinhImportDiemXlsx(@PathVariable Long id) {
        return gradeImportService.layCauHinhImport(id);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx",
                 "/giang-vien/lop-hoc-phan/{id}/diem/template",
                 "/diem/lop-hoc-phan/{id}/template-xlsx"})
    @Operation(summary = "Tải tệp mẫu Excel nhập điểm", description = "Tải tệp bảng tính .xlsx chuẩn gồm mã sinh viên, họ tên và các cột điểm để nhập dữ liệu")
    public ResponseEntity<byte[]> taiTemplateDiemXlsx(@PathVariable Long id) {
        GradeImportService.XlsxFile tep = gradeImportService.xuatTemplateXlsx(id);
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.parseMediaType(XlsxWorkbookSupport.XLSX_MEDIA_TYPE));
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @PostMapping(
            value = {"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview",
                     "/diem/lop-hoc-phan/{id}/import-xlsx/preview"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Tải lên và xem trước dữ liệu import điểm", description = "Đọc và kiểm tra hợp lệ tệp Excel điểm, trả về danh sách dòng hợp lệ và cảnh báo lỗi trước khi lưu")
    public GradeImportDtos.PreviewResponse xemTruocImportDiemXlsx(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return gradeImportService.xemTruocImport(id, file);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm",
                  "/diem/lop-hoc-phan/{id}/import-xlsx/confirm"})
    @Operation(summary = "Xác nhận ghi dữ liệu import điểm vào hệ thống", description = "Ghi nhận toàn bộ dữ liệu điểm đã xem trước thành công vào cơ sở dữ liệu")
    public GradeImportDtos.ConfirmImportResponse xacNhanImportDiemXlsx(
            @PathVariable Long id,
            @Valid @RequestBody GradeImportDtos.ConfirmImportRequest request) {
        return gradeImportService.xacNhanImport(id, request);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/export-errors",
                  "/diem/lop-hoc-phan/{id}/import-xlsx/export-errors"})
    @Operation(summary = "Xuất danh sách lỗi import ra tệp CSV", description = "Tải về báo cáo chi tiết các dòng dữ liệu không hợp lệ trong tệp import")
    public ResponseEntity<byte[]> xuatLoiImportDiemXlsx(
            @PathVariable Long id,
            @RequestBody List<com.edu.registration.dto.ValidationErrorDetail> danhSachLoi) {
        byte[] csv = gradeImportService.xuatFileLoiCsv(id, danhSachLoi);
        String filename = "loi-import-diem-lop-" + id + ".csv";
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(filename, MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    // --- Grade Sheet Approval & Locking Workflow (QLSV-74 / TASK-LC-09) ---

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/trang-thai-bang-diem", "/diem/lop-hoc-phan/{id}/trang-thai"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem trạng thái bảng điểm lớp", description = "Xem bảng điểm đang ở trạng thái nháp, chờ duyệt, đã duyệt, đã công bố hay đã khóa")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/lich-su-bang-diem", "/diem/lop-hoc-phan/{id}/lich-su-trang-thai"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem lịch sử trạng thái bảng điểm", description = "Xem dòng thời gian các thao tác gửi duyệt, trả về, phê duyệt, công bố bảng điểm")
    public List<RegistrationDtos.LichSuTrangThaiBangDiemDto> lichSuTrangThaiBangDiem(@PathVariable Long id) {
        return diemService.lichSuTrangThaiBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem", "/diem/lop-hoc-phan/{id}/gui-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Gửi duyệt bảng điểm", description = "Giảng viên chuyển bảng điểm từ trạng thái Nháp sang Chờ phê duyệt")
    public RegistrationDtos.TrangThaiBangDiemDto guiDuyetBangDiem(@PathVariable Long id) {
        return diemService.guiDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/tra-ve-diem", "/diem/lop-hoc-phan/{id}/tra-ve"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Trả về bảng điểm yêu cầu chỉnh sửa", description = "Từ chối phê duyệt bảng điểm và yêu cầu giảng viên cập nhật lại")
    public RegistrationDtos.TrangThaiBangDiemDto traVeBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.TraVeBangDiemRequest request) {
        return diemService.traVeBangDiem(id, request != null ? request.lyDo() : null);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/phe-duyet-diem", "/diem/lop-hoc-phan/{id}/phe-duyet"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Phê duyệt bảng điểm", description = "Xác nhận bảng điểm hợp lệ và sẵn sàng công bố cho sinh viên")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/cong-bo-diem", "/diem/lop-hoc-phan/{id}/cong-bo"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Công bố bảng điểm cho sinh viên", description = "Cho phép sinh viên trong lớp tra cứu kết quả điểm chính thức")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping({"/giang-vien/lop-hoc-phan/{id}/khoa-diem", "/diem/lop-hoc-phan/{id}/khoa"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Khóa bảng điểm học phần", description = "Đóng hoàn toàn bảng điểm, không cho phép chỉnh sửa thêm")
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
    @Operation(summary = "Mở khóa hoặc thu hồi công bố điểm", description = "Mở lại quyền chỉnh sửa bảng điểm khi cần điều chỉnh sau phúc khảo hoặc có sai sót")
    public RegistrationDtos.TrangThaiBangDiemDto moKhoaBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.MoKhoaBangDiemRequest request) {
        return diemService.moKhoaBangDiem(id, request != null ? request.lyDo() : null);
    }

    // --- Class Grade Analytics & Distribution (QLSV-75 / TASK-LC-10) ---

    @GetMapping({"/giang-vien/thong-ke-diem", "/giang-vien/me/thong-ke-diem"})
    @PreAuthorize("hasRole('GIANG_VIEN')")
    @Operation(summary = "Báo cáo thống kê điểm giảng viên", description = "Xem phân tích điểm trung bình, tỷ lệ đạt/trượt trên toàn bộ các lớp giảng viên phụ trách")
    public GradeAnalyticsDtos.BaoCaoThongKeDiemDto thongKeDiemGiangVien(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId) {
        return gradeAnalyticsService.thongKeChoGiangVien(hocKyId, monHocId, lopHocPhanId);
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{id}/thong-ke-diem", "/diem/lop-hoc-phan/{id}/thong-ke"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Thống kê phổ điểm của một lớp học phần", description = "Xem biểu đồ phổ điểm, điểm cao nhất, thấp nhất, trung bình và độ lệch chuẩn của lớp")
    public GradeAnalyticsDtos.ThongKeDiemLopItemDto thongKeDiemMotLop(@PathVariable Long id) {
        return gradeAnalyticsService.thongKeMotLop(id);
    }
}
