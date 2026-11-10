package com.edu.registration.controller;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.dto.StudentAcademicProfileDto;
import com.edu.registration.dto.UserDtos;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import com.edu.registration.util.ExportSecuritySupport;
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
import com.edu.registration.dto.VerifiableTranscriptDtos;
import com.edu.registration.service.VerifiableTranscriptService;
import com.edu.registration.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Sinh viên - Đăng ký & Học vụ", description = "Các API phục vụ Sinh viên: đăng ký học phần, xem thời khóa biểu, điểm, bảng điểm điện tử, lịch sử học tập")
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
    private final VerifiableTranscriptService verifiableTranscriptService;

    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService) {
        this(lopHocPhanService, dangKyService, diemService, thongBaoNghiService,
                studentAcademicProfileService, sinhVienDashboardService, hocLaiCaiThienService, null, null, null, null);
    }

    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService,
                              StudentTranscriptPdfService studentTranscriptPdfService) {
        this(lopHocPhanService, dangKyService, diemService, thongBaoNghiService,
                studentAcademicProfileService, sinhVienDashboardService, hocLaiCaiThienService,
                studentTranscriptPdfService, null, null, null);
    }

    @Autowired
    public StudentController(CourseClassService lopHocPhanService, RegistrationService dangKyService,
                              GradeService diemService, ClassAbsenceNoticeService thongBaoNghiService,
                              StudentAcademicProfileService studentAcademicProfileService,
                              StudentDashboardService sinhVienDashboardService,
                              RetakeCourseService hocLaiCaiThienService,
                              StudentTranscriptPdfService studentTranscriptPdfService,
                              CourseClassAnnouncementService thongBaoLopHocPhanService,
                              @Autowired(required = false) RegistrationPeriodService registrationPeriodService,
                              @Autowired(required = false) VerifiableTranscriptService verifiableTranscriptService) {
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
        this.verifiableTranscriptService = verifiableTranscriptService;
    }

    @GetMapping("/lop-hoc-phan")
    @Operation(summary = "Xem danh sách lớp học phần mở", description = "Tra cứu các lớp học phần đang mở theo học kỳ và từ khóa tìm kiếm")
    public List<CourseClassDtos.LopHocPhanDto> danhSachLopHocPhan(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String keyword) {
        return lopHocPhanService.danhSach(hocKyId, keyword);
    }

    @GetMapping("/lop-hoc-phan/{id}")
    @Operation(summary = "Xem chi tiết lớp học phần", description = "Lấy thông tin chi tiết của một lớp học phần theo ID")
    public CourseClassDtos.LopHocPhanDto chiTietLopHocPhan(@PathVariable Long id) {
        return lopHocPhanService.chiTiet(id);
    }

    @PostMapping("/dang-ky")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Đăng ký lớp học phần", description = "Sinh viên gửi yêu cầu đăng ký lớp học phần, hệ thống kiểm tra điều kiện tiên quyết, trùng lịch và sĩ số")
    public RegistrationDtos.DangKyDto dangKy(@Valid @RequestBody RegistrationDtos.DangKyRequest request) {
        return dangKyService.dangKy(request.lopHocPhanId(), request.loaiDangKy());
    }

    @GetMapping("/dang-ky/kiem-tra-quan-he")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Kiểm tra điều kiện quan hệ môn học", description = "Kiểm tra xem sinh viên có đủ điều kiện tiên quyết/song hành/học trước để đăng ký lớp học phần hay không")
    public RegistrationDtos.KetQuaKiemTraQuanHeDangKyDto kiemTraQuanHeDangKy(
            @RequestParam Long lopHocPhanId) {
        return dangKyService.kiemTraQuanHeDangKy(lopHocPhanId);
    }

    @PostMapping("/dang-ky/xac-nhan-ke-hoach")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Đăng ký học phần theo kế hoạch", description = "Xác nhận đăng ký đồng thời danh sách các lớp học phần đã lên kế hoạch từ trước")
    public List<RegistrationDtos.DangKyDto> xacNhanKeHoach(
            @Valid @RequestBody RegistrationDtos.XacNhanKeHoachRequest request) {
        return dangKyService.xacNhanKeHoach(request.lopHocPhanIds());
    }

    @DeleteMapping("/dang-ky/{id}")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Hủy đăng ký lớp học phần", description = "Sinh viên hủy một lớp học phần đã đăng ký trong đợt mở đăng ký")
    public void huyDangKy(@PathVariable Long id,
                          @RequestParam(required = false) @Size(max = 255) String lyDo) {
        dangKyService.huyDangKy(id, lyDo);
    }

    @GetMapping("/dang-ky/me")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem danh sách lớp đã đăng ký", description = "Lấy danh sách các lớp học phần sinh viên hiện tại đã đăng ký trong học kỳ")
    public List<RegistrationDtos.DangKyDto> dangKyCuaToi(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.danhSachCuaToi(hocKyId);
    }

    @GetMapping("/dang-ky/dot-hien-tai")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Lấy thông tin đợt đăng ký hiện tại", description = "Kiểm tra xem hiện tại có đợt đăng ký nào đang mở áp dụng cho sinh viên hay không")
    public RegistrationPeriodDtos.TrangThaiDangKySinhVienDto layTrangThaiDotDangKyCuaToi(
            @RequestParam(required = false) Long hocKyId) {
        if (registrationPeriodService == null) {
            return null;
        }
        return registrationPeriodService.layTrangThaiDangKySinhVien(hocKyId);
    }

    @GetMapping("/dang-ky/me/lich-su")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Lịch sử thao tác đăng ký", description = "Xem lịch sử các lần đăng ký hoặc hủy đăng ký học phần kèm lý do")
    public Page<RegistrationDtos.LichSuDangKyDto> lichSuDangKyCuaToi(
            @RequestParam Long hocKyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return dangKyService.lichSuCuaToi(hocKyId, page, size);
    }

    @GetMapping("/sinh-vien/me/thoi-khoa-bieu")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem thời khóa biểu cá nhân", description = "Lấy thời khóa biểu học tập của sinh viên theo từng học kỳ kèm thông tin phòng học và giảng viên")
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(@RequestParam(required = false) Long hocKyId) {
        return dangKyService.thoiKhoaBieu(hocKyId);
    }

    @GetMapping("/sinh-vien/lop-hoc-phan/{id}/thong-bao")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem thông báo lớp học phần", description = "Lấy danh sách thông báo của một lớp học phần dành cho sinh viên tham gia lớp")
    public List<LecturerDtos.ThongBaoLopDto> thongBaoLop(@PathVariable Long id) {
        if (thongBaoLopHocPhanService == null) {
            return List.of();
        }
        return thongBaoLopHocPhanService.danhSachThongBaoChoSinhVien(id);
    }

    @GetMapping("/sinh-vien/me/thong-bao-nghi")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem thông báo báo nghỉ và học bù", description = "Tra cứu các thông báo báo nghỉ hoặc bố trí học bù của các lớp sinh viên đang theo học")
    public List<RegistrationDtos.ThongBaoNghiDto> thongBaoNghi(@RequestParam(required = false) Long hocKyId) {
        return thongBaoNghiService.cuaSinhVien(hocKyId);
    }

    @GetMapping("/sinh-vien/me/diem")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem kết quả học tập", description = "Xem bảng điểm quá trình, điểm kết thúc và điểm tổng kết các môn học của sinh viên")
    public RegistrationDtos.KetQuaHocTapDto ketQuaHocTap() {
        return diemService.ketQuaHocTap();
    }

    @GetMapping("/sinh-vien/me/lich-su-hoc-tap")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem lịch sử học tập toàn khóa", description = "Tra cứu lịch sử học tập qua các kỳ, số tín chỉ tích lũy và điểm trung bình tích lũy GPA")
    public RegistrationDtos.LichSuHocTapDto lichSuHocTap(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String trangThai) {
        return diemService.lichSuHocTap(hocKyId, trangThai);
    }

    @GetMapping("/sinh-vien/me/bang-diem/pdf")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Tải bảng điểm định dạng PDF", description = "Xuất bảng điểm chính thức của sinh viên dưới dạng tệp PDF có chữ ký điện tử hoặc mã xác thực")
    public ResponseEntity<byte[]> xuatBangDiemPdf(@RequestParam(required = false) Long hocKyId) {
        StudentTranscriptPdfService.PdfFile tep;
        if (verifiableTranscriptService != null) {
            tep = verifiableTranscriptService.xuatBangDiemDienTuSinhVien(hocKyId);
        } else {
            tep = studentTranscriptPdfService.xuatBangDiemPdf(hocKyId);
        }
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.APPLICATION_PDF);
        if (tep.maXacMinh() != null && !tep.maXacMinh().isBlank()) {
            headers.set("X-Verification-Code", tep.maXacMinh());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Verification-Code");
        }
        if (tep.verificationUrl() != null && !tep.verificationUrl().isBlank()) {
            headers.set("X-Verification-Url", tep.verificationUrl());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Verification-Url");
        }
        if (tep.phienBan() != null) {
            headers.set("X-Transcript-Version", String.valueOf(tep.phienBan()));
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Transcript-Version");
        }
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @GetMapping("/sinh-vien/me/bang-diem-xac-thuc")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Danh sách bảng điểm điện tử đã cấp", description = "Xem danh sách các bản phát hành bảng điểm điện tử có mã QR xác minh của sinh viên")
    public List<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> danhSachBangDiemXacThucMe() {
        if (verifiableTranscriptService == null) {
            return List.of();
        }
        return verifiableTranscriptService.danhSachCuaSinhVienHienTai();
    }

    @GetMapping("/sinh-vien/me/bang-diem-xac-thuc/{id}/pdf")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Tải lại PDF bảng điểm điện tử đã phát hành", description = "Tải tệp PDF của một bản phát hành bảng điểm điện tử xác thực cụ thể theo ID")
    public ResponseEntity<byte[]> taiBangDiemDaCapMe(@PathVariable Long id) {
        if (verifiableTranscriptService == null) {
            throw ApiException.notFound("Tính năng bảng điểm xác thực chưa được kích hoạt");
        }
        StudentTranscriptPdfService.PdfFile tep = verifiableTranscriptService.taiPdfDaPhatHanh(id);
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @GetMapping("/sinh-vien/me/ho-so-dao-tao")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem hồ sơ học vụ sinh viên", description = "Lấy thông tin chương trình đào tạo, khoa, ngành, khóa và trạng thái học vụ của sinh viên")
    public StudentAcademicProfileDto hoSoDaoTao() {
        return studentAcademicProfileService.getMyProfile();
    }

    @GetMapping("/sinh-vien/me/lich-su-trang-thai-hoc-tap")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Lịch sử biến động trạng thái học vụ", description = "Xem lịch sử thay đổi trạng thái học vụ (bình thường, cảnh báo học tập, buộc thôi học, bảo lưu...)")
    public List<UserDtos.StudentAcademicStatusHistoryDto> layLichSuTrangThaiHocTapMe() {
        return studentAcademicProfileService.getMyStatusHistory();
    }

    @GetMapping("/sinh-vien/me/canh-bao-dashboard")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Tổng hợp cảnh báo học tập hiển thị Dashboard", description = "Lấy các thông điệp cảnh báo học tập sớm (điểm thấp, vắng nhiều) dành cho sinh viên")
    public List<StudentDashboardService.CanhBaoDto> canhBaoDashboard() {
        return sinhVienDashboardService.canhBao();
    }

    @GetMapping("/sinh-vien/me/dang-ky-hoc-lai-cai-thien")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Tra cứu môn đủ điều kiện học lại/cải thiện", description = "Liệt kê các môn học có điểm F (học lại) hoặc điểm D/D+ (học cải thiện nâng điểm)")
    public List<RegistrationDtos.DangKyHocLaiDto> danhSachHocLaiCaiThien(
            @RequestParam(required = false) Long hocKyId) {
        return hocLaiCaiThienService.danhSach(hocKyId);
    }
}
