package com.edu.registration.controller;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Người dùng & Import CSV", description = "Quản lý tài khoản sinh viên, giảng viên và nhập liệu danh sách người dùng hàng loạt từ CSV")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/sinh-vien")
    @Operation(summary = "Danh sách sinh viên", description = "Tìm kiếm và liệt kê danh sách tài khoản sinh viên")
    public List<UserDtos.SinhVienDto> danhSachSinhVien(@RequestParam(required = false) String keyword) {
        return userService.danhSachSinhVien(keyword);
    }

    @PostMapping("/sinh-vien")
    @Operation(summary = "Tạo mới sinh viên", description = "Tạo mới tài khoản và hồ sơ học vụ cho sinh viên")
    public UserDtos.SinhVienDto taoSinhVien(@Valid @RequestBody UserDtos.SinhVienRequest request) {
        return userService.taoSinhVien(request);
    }

    @PutMapping("/sinh-vien/{id}")
    @Operation(summary = "Cập nhật thông tin sinh viên", description = "Chỉnh sửa họ tên, email, lớp hành chính hoặc ngành của sinh viên")
    public UserDtos.SinhVienDto suaSinhVien(@PathVariable Long id,
                                            @Valid @RequestBody UserDtos.SinhVienRequest request) {
        return userService.suaSinhVien(id, request);
    }

    @PatchMapping("/sinh-vien/{id}/trang-thai")
    @Operation(summary = "Kích hoạt / Khóa tài khoản sinh viên", description = "Thay đổi trạng thái tài khoản sinh viên (HOAT_DONG, KHOA)")
    public UserDtos.SinhVienDto doiTrangThaiSinhVien(@PathVariable Long id,
                                                     @RequestBody UserDtos.TrangThaiRequest request) {
        return userService.doiTrangThaiSinhVien(id, request.trangThai());
    }

    @PatchMapping("/sinh-vien/{id}/trang-thai-hoc-tap")
    @Operation(summary = "Cập nhật trạng thái học vụ sinh viên", description = "Cập nhật trạng thái học tập (Bình thường, Buộc thôi học, Bảo lưu...)")
    public UserDtos.SinhVienDto capNhatTrangThaiHocTap(@PathVariable Long id,
                                                      @Valid @RequestBody UserDtos.CapNhatTrangThaiHocTapRequest request) {
        return userService.capNhatTrangThaiHocTap(id, request);
    }

    @GetMapping("/sinh-vien/{id}/lich-su-trang-thai-hoc-tap")
    public List<UserDtos.StudentAcademicStatusHistoryDto> layLichSuTrangThaiHocTap(@PathVariable Long id) {
        return userService.layLichSuTrangThaiHocTap(id);
    }

    @GetMapping({"/sinh-vien/import/cau-hinh", "/sinh-vien/import/config"})
    public UserDtos.UserImportConfigDto cauHinhImportSinhVien() {
        return userService.layCauHinhImportSinhVien();
    }

    @GetMapping("/sinh-vien/import/template")
    public ResponseEntity<byte[]> mauImportSinhVien() {
        byte[] csv = userService.getSinhVienTemplateCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau-import-sinh-vien.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/sinh-vien/import")
    public UserDtos.ImportResultDto importSinhVien(@RequestPart("file") MultipartFile file) {
        return userService.importSinhVien(file);
    }

    @PostMapping("/sinh-vien/import/preview")
    public UserDtos.UserImportPreviewResponse previewImportSinhVien(@RequestPart("file") MultipartFile file) {
        return userService.previewImportSinhVien(file);
    }

    @PostMapping("/sinh-vien/import/confirm")
    public UserDtos.ImportResultDto confirmImportSinhVien(@Valid @RequestBody UserDtos.UserConfirmImportRequest request) {
        return userService.confirmImportSinhVien(request);
    }

    @PostMapping("/sinh-vien/import/export-errors")
    public ResponseEntity<byte[]> xuatLoiImportSinhVien(@RequestBody List<UserDtos.UserImportRowDetailDto> danhSachLoi) {
        byte[] csv = userService.exportErrorReport(danhSachLoi);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loi-import-sinh-vien.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/giang-vien")
    public List<UserDtos.GiangVienDto> danhSachGiangVien(@RequestParam(required = false) String keyword) {
        return userService.danhSachGiangVien(keyword);
    }

    @PostMapping("/giang-vien")
    public UserDtos.GiangVienDto taoGiangVien(@Valid @RequestBody UserDtos.GiangVienRequest request) {
        return userService.taoGiangVien(request);
    }

    @PutMapping("/giang-vien/{id}")
    public UserDtos.GiangVienDto suaGiangVien(@PathVariable Long id,
                                              @Valid @RequestBody UserDtos.GiangVienRequest request) {
        return userService.suaGiangVien(id, request);
    }

    @PatchMapping("/giang-vien/{id}/trang-thai")
    public UserDtos.GiangVienDto doiTrangThaiGiangVien(@PathVariable Long id,
                                                       @RequestBody UserDtos.TrangThaiRequest request) {
        return userService.doiTrangThaiGiangVien(id, request.trangThai());
    }

    @GetMapping({"/giang-vien/import/cau-hinh", "/giang-vien/import/config"})
    public UserDtos.UserImportConfigDto cauHinhImportGiangVien() {
        return userService.layCauHinhImportGiangVien();
    }

    @GetMapping("/giang-vien/import/template")
    public ResponseEntity<byte[]> mauImportGiangVien() {
        byte[] csv = userService.getGiangVienTemplateCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=mau-import-giang-vien.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/giang-vien/import")
    public UserDtos.ImportResultDto importGiangVien(@RequestPart("file") MultipartFile file) {
        return userService.importGiangVien(file);
    }

    @PostMapping("/giang-vien/import/preview")
    public UserDtos.UserImportPreviewResponse previewImportGiangVien(@RequestPart("file") MultipartFile file) {
        return userService.previewImportGiangVien(file);
    }

    @PostMapping("/giang-vien/import/confirm")
    public UserDtos.ImportResultDto confirmImportGiangVien(@Valid @RequestBody UserDtos.UserConfirmImportRequest request) {
        return userService.confirmImportGiangVien(request);
    }

    @PostMapping("/giang-vien/import/export-errors")
    public ResponseEntity<byte[]> xuatLoiImportGiangVien(@RequestBody List<UserDtos.UserImportRowDetailDto> danhSachLoi) {
        byte[] csv = userService.exportErrorReport(danhSachLoi);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=loi-import-giang-vien.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}