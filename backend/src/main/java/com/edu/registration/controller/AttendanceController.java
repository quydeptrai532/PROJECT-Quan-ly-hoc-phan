package com.edu.registration.controller;

import com.edu.registration.dto.AttendanceAlertDtos;
import com.edu.registration.dto.AttendanceDtos;
import com.edu.registration.entity.AlertStatus;
import com.edu.registration.service.AttendanceAlertService;
import com.edu.registration.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Điểm danh & Chuyên cần", description = "Quản lý buổi học, điểm danh sinh viên, báo cáo chuyên cần và cảnh báo vắng vượt ngưỡng")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceAlertService attendanceAlertService;

    public AttendanceController(AttendanceService attendanceService,
                                AttendanceAlertService attendanceAlertService) {
        this.attendanceService = attendanceService;
        this.attendanceAlertService = attendanceAlertService;
    }

    // =========================================================================
    // Endpoints cho Giảng viên & Quản trị viên (QLSV-70)
    // =========================================================================

    @PostMapping({"/giang-vien/lop-hoc-phan/{lopId}/buoi-hoc", "/giang-vien/buoi-hoc"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Tạo mới buổi học", description = "Tạo một buổi học mới cho lớp học phần với ngày học, ca học và phòng học")
    public ResponseEntity<AttendanceDtos.BuoiHocDto> taoBuoiHoc(
            @PathVariable(value = "lopId", required = false) Long lopId,
            @Valid @RequestBody AttendanceDtos.TaoBuoiHocRequest request) {
        AttendanceDtos.TaoBuoiHocRequest effectiveRequest = request;
        if (lopId != null && (request.lopHocPhanId() == null || !request.lopHocPhanId().equals(lopId))) {
            effectiveRequest = new AttendanceDtos.TaoBuoiHocRequest(
                    lopId, request.ngayHoc(), request.caHoc(), request.phongHoc(),
                    request.tietBatDau(), request.soTiet(), request.noiDung());
        }
        return ResponseEntity.ok(attendanceService.taoBuoiHoc(effectiveRequest));
    }

    @GetMapping("/giang-vien/lop-hoc-phan/{lopId}/buoi-hoc")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem danh sách buổi học của lớp", description = "Lấy tất cả các buổi học đã được thiết lập cho lớp học phần")
    public ResponseEntity<List<AttendanceDtos.BuoiHocDto>> danhSachBuoiHoc(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.danhSachBuoiHocCuaLop(lopId));
    }

    @PutMapping("/giang-vien/buoi-hoc/{id}")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Cập nhật thông tin buổi học", description = "Chỉnh sửa ngày học, ca học, phòng học hoặc nội dung buổi học")
    public ResponseEntity<AttendanceDtos.BuoiHocDto> capNhatBuoiHoc(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttendanceDtos.CapNhatBuoiHocRequest request) {
        return ResponseEntity.ok(attendanceService.capNhatBuoiHoc(id, request));
    }

    @DeleteMapping("/giang-vien/buoi-hoc/{id}")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xóa buổi học", description = "Xóa một buổi học chưa điểm danh khỏi danh sách")
    public ResponseEntity<Void> xoaBuoiHoc(@PathVariable("id") Long id) {
        attendanceService.xoaBuoiHoc(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/giang-vien/buoi-hoc/{id}/khoa", "/admin/buoi-hoc/{id}/khoa"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Khóa hoặc mở khóa dữ liệu buổi học", description = "Khóa buổi học để không thể sửa đổi kết quả điểm danh")
    public ResponseEntity<AttendanceDtos.BuoiHocDto> khoaBuoiHoc(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AttendanceDtos.KhoaBuoiHocRequest request) {
        boolean khoa = request == null || request.khoa();
        return ResponseEntity.ok(attendanceService.khoaBuoiHoc(id, khoa));
    }

    @GetMapping("/giang-vien/buoi-hoc/{id}/diem-danh")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem chi tiết điểm danh buổi học", description = "Lấy danh sách sinh viên lớp và trạng thái điểm danh trong buổi học")
    public ResponseEntity<AttendanceDtos.BuoiHocChiTietDto> chiTietDiemDanh(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(attendanceService.chiTietBuoiHocVaDiemDanh(id));
    }

    @PutMapping("/giang-vien/buoi-hoc/{id}/diem-danh")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Lưu kết quả điểm danh", description = "Ghi nhận trạng thái có mặt, vắng, muộn hoặc có phép cho từng sinh viên")
    public ResponseEntity<AttendanceDtos.BuoiHocChiTietDto> luuDiemDanh(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttendanceDtos.LuuDiemDanhRequest request) {
        return ResponseEntity.ok(attendanceService.luuDiemDanh(id, request));
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/chuyen-can", "/admin/lop-hoc-phan/{lopId}/chuyen-can"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Báo cáo chuyên cần cả lớp", description = "Xem bảng tổng hợp số buổi có mặt, vắng và tỷ lệ chuyên cần của cả lớp")
    public ResponseEntity<AttendanceDtos.BaoCaoChuyenCanLopDto> baoCaoChuyenCanLop(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.baoCaoChuyenCanLop(lopId));
    }

    @GetMapping({"/giang-vien/sinh-vien/{studentId}/chuyen-can", "/admin/sinh-vien/{studentId}/chuyen-can"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Báo cáo chuyên cần của một sinh viên", description = "Tra cứu tỷ lệ chuyên cần của một sinh viên trên tất cả các môn đã học")
    public ResponseEntity<List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto>> baoCaoChuyenCanSinhVien(
            @PathVariable("studentId") Long studentId,
            @RequestParam(value = "hocKyId", required = false) Long hocKyId) {
        return ResponseEntity.ok(attendanceService.baoCaoChuyenCanTheoSinhVien(studentId, hocKyId));
    }

    // =========================================================================
    // Endpoints Cảnh báo quá ngưỡng (QLSV-71 / TASK-LC-06)
    // =========================================================================

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/canh-bao", "/admin/lop-hoc-phan/{lopId}/canh-bao"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Xem báo cáo cảnh báo vắng của lớp", description = "Danh sách sinh viên chạm hoặc vượt ngưỡng vắng tối đa quy định của lớp")
    public ResponseEntity<AttendanceAlertDtos.BaoCaoCanhBaoLopDto> layCanhBaoLop(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceAlertService.layBaoCaoCanhBaoLop(lopId));
    }

    @PutMapping({"/giang-vien/lop-hoc-phan/{lopId}/cau-hinh-canh-bao", "/admin/lop-hoc-phan/{lopId}/cau-hinh-canh-bao"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Cấu hình ngưỡng cảnh báo vắng cho lớp", description = "Thiết lập số buổi vắng tối đa hoặc tỷ lệ vắng phần trăm kích hoạt cảnh báo")
    public ResponseEntity<AttendanceAlertDtos.BaoCaoCanhBaoLopDto> capNhatCauHinhCanhBao(
            @PathVariable("lopId") Long lopId,
            @Valid @RequestBody AttendanceAlertDtos.CauHinhNguongLopRequest request) {
        return ResponseEntity.ok(attendanceAlertService.capNhatCauHinhNguongLop(lopId, request));
    }

    @GetMapping({"/giang-vien/canh-bao-diem-danh", "/admin/canh-bao-diem-danh"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    @Operation(summary = "Danh sách cảnh báo chuyên cần toàn diện", description = "Tra cứu toàn bộ danh sách cảnh báo chuyên cần được kích hoạt")
    public ResponseEntity<List<AttendanceAlertDtos.CanhBaoDiemDanhDto>> danhSachCanhBaoGiangVien(
            @RequestParam(value = "trangThai", required = false) AlertStatus trangThai) {
        return ResponseEntity.ok(attendanceAlertService.danhSachCanhBaoCuaGiangVien(trangThai));
    }

    // =========================================================================
    // Endpoints cho Sinh viên
    // =========================================================================

    @GetMapping("/sinh-vien/lop-hoc-phan/{lopId}/buoi-hoc")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem danh sách buổi học của lớp cho sinh viên", description = "Sinh viên xem lịch các buổi học trong lớp mình tham gia")
    public ResponseEntity<List<AttendanceDtos.BuoiHocDto>> danhSachBuoiHocChoSinhVien(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.danhSachBuoiHocCuaLop(lopId));
    }

    @GetMapping("/sinh-vien/me/chuyen-can")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem tỷ lệ chuyên cần cá nhân", description = "Sinh viên tra cứu số buổi đã học, số buổi vắng và tỷ lệ chuyên cần của mình")
    public ResponseEntity<List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto>> baoCaoChuyenCanCuaToi(
            @RequestParam(value = "hocKyId", required = false) Long hocKyId) {
        return ResponseEntity.ok(attendanceService.baoCaoChuyenCanCuaSinhVienHienTai(hocKyId));
    }

    @GetMapping("/sinh-vien/me/canh-bao-diem-danh")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xem các cảnh báo vắng cá nhân", description = "Sinh viên theo dõi các cảnh báo chuyên cần nguy cơ cấm thi của mình")
    public ResponseEntity<List<AttendanceAlertDtos.CanhBaoDiemDanhDto>> danhSachCanhBaoCuaToi(
            @RequestParam(value = "trangThai", required = false) AlertStatus trangThai) {
        return ResponseEntity.ok(attendanceAlertService.danhSachCanhBaoCuaSinhVien(trangThai));
    }
}
