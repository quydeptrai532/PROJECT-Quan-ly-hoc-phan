package com.edu.registration.controller;

import com.edu.registration.dto.AttendanceAlertDtos;
import com.edu.registration.dto.AttendanceDtos;
import com.edu.registration.entity.AlertStatus;
import com.edu.registration.service.AttendanceAlertService;
import com.edu.registration.service.AttendanceService;
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
    public ResponseEntity<List<AttendanceDtos.BuoiHocDto>> danhSachBuoiHoc(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.danhSachBuoiHocCuaLop(lopId));
    }

    @PutMapping("/giang-vien/buoi-hoc/{id}")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceDtos.BuoiHocDto> capNhatBuoiHoc(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttendanceDtos.CapNhatBuoiHocRequest request) {
        return ResponseEntity.ok(attendanceService.capNhatBuoiHoc(id, request));
    }

    @DeleteMapping("/giang-vien/buoi-hoc/{id}")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<Void> xoaBuoiHoc(@PathVariable("id") Long id) {
        attendanceService.xoaBuoiHoc(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/giang-vien/buoi-hoc/{id}/khoa", "/admin/buoi-hoc/{id}/khoa"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceDtos.BuoiHocDto> khoaBuoiHoc(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AttendanceDtos.KhoaBuoiHocRequest request) {
        boolean khoa = request == null || request.khoa();
        return ResponseEntity.ok(attendanceService.khoaBuoiHoc(id, khoa));
    }

    @GetMapping("/giang-vien/buoi-hoc/{id}/diem-danh")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceDtos.BuoiHocChiTietDto> chiTietDiemDanh(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(attendanceService.chiTietBuoiHocVaDiemDanh(id));
    }

    @PutMapping("/giang-vien/buoi-hoc/{id}/diem-danh")
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceDtos.BuoiHocChiTietDto> luuDiemDanh(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttendanceDtos.LuuDiemDanhRequest request) {
        return ResponseEntity.ok(attendanceService.luuDiemDanh(id, request));
    }

    @GetMapping({"/giang-vien/lop-hoc-phan/{lopId}/chuyen-can", "/admin/lop-hoc-phan/{lopId}/chuyen-can"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceDtos.BaoCaoChuyenCanLopDto> baoCaoChuyenCanLop(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.baoCaoChuyenCanLop(lopId));
    }

    @GetMapping({"/giang-vien/sinh-vien/{studentId}/chuyen-can", "/admin/sinh-vien/{studentId}/chuyen-can"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
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
    public ResponseEntity<AttendanceAlertDtos.BaoCaoCanhBaoLopDto> layCanhBaoLop(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceAlertService.layBaoCaoCanhBaoLop(lopId));
    }

    @PutMapping({"/giang-vien/lop-hoc-phan/{lopId}/cau-hinh-canh-bao", "/admin/lop-hoc-phan/{lopId}/cau-hinh-canh-bao"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<AttendanceAlertDtos.BaoCaoCanhBaoLopDto> capNhatCauHinhCanhBao(
            @PathVariable("lopId") Long lopId,
            @Valid @RequestBody AttendanceAlertDtos.CauHinhNguongLopRequest request) {
        return ResponseEntity.ok(attendanceAlertService.capNhatCauHinhNguongLop(lopId, request));
    }

    @GetMapping({"/giang-vien/canh-bao-diem-danh", "/admin/canh-bao-diem-danh"})
    @PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
    public ResponseEntity<List<AttendanceAlertDtos.CanhBaoDiemDanhDto>> danhSachCanhBaoGiangVien(
            @RequestParam(value = "trangThai", required = false) AlertStatus trangThai) {
        return ResponseEntity.ok(attendanceAlertService.danhSachCanhBaoCuaGiangVien(trangThai));
    }

    // =========================================================================
    // Endpoints cho Sinh viên
    // =========================================================================

    @GetMapping("/sinh-vien/lop-hoc-phan/{lopId}/buoi-hoc")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public ResponseEntity<List<AttendanceDtos.BuoiHocDto>> danhSachBuoiHocChoSinhVien(
            @PathVariable("lopId") Long lopId) {
        return ResponseEntity.ok(attendanceService.danhSachBuoiHocCuaLop(lopId));
    }

    @GetMapping("/sinh-vien/me/chuyen-can")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public ResponseEntity<List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto>> baoCaoChuyenCanCuaToi(
            @RequestParam(value = "hocKyId", required = false) Long hocKyId) {
        return ResponseEntity.ok(attendanceService.baoCaoChuyenCanCuaSinhVienHienTai(hocKyId));
    }

    @GetMapping("/sinh-vien/me/canh-bao-diem-danh")
    @PreAuthorize("hasRole('SINH_VIEN')")
    public ResponseEntity<List<AttendanceAlertDtos.CanhBaoDiemDanhDto>> danhSachCanhBaoCuaToi(
            @RequestParam(value = "trangThai", required = false) AlertStatus trangThai) {
        return ResponseEntity.ok(attendanceAlertService.danhSachCanhBaoCuaSinhVien(trangThai));
    }
}
