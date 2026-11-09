package com.edu.registration.controller;

import com.edu.registration.dto.AcademicAlertDtos.*;
import com.edu.registration.service.EarlyWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/giang-vien/canh-bao-hoc-tap")
@PreAuthorize("hasAnyRole('GIANG_VIEN', 'QUAN_TRI')")
@Tag(name = "Lecturer Early Warning", description = "Xem và tư vấn cảnh báo học tập của sinh viên trong lớp phụ trách")
public class LecturerEarlyWarningController {

    private final EarlyWarningService earlyWarningService;

    public LecturerEarlyWarningController(EarlyWarningService earlyWarningService) {
        this.earlyWarningService = earlyWarningService;
    }

    @GetMapping
    @Operation(summary = "Danh sách cảnh báo học tập của sinh viên thuộc các lớp giảng viên phụ trách")
    public ResponseEntity<Page<AcademicAlertDto>> danhSachCanhBaoCuaLop(
            @RequestParam(required = false) Long lopHocPhanId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(earlyWarningService.danhSachCanhBaoGiangVien(lopHocPhanId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một cảnh báo học tập của sinh viên trong phạm vi phụ trách")
    public ResponseEntity<AcademicAlertDto> chiTietCanhBao(@PathVariable Long id) {
        return ResponseEntity.ok(earlyWarningService.layChiTiet(id));
    }

    @PatchMapping("/{id}/xu-ly")
    @Operation(summary = "Giảng viên ghi nhận tư vấn học vụ hoặc đánh dấu đang xử lý/đã xử lý")
    public ResponseEntity<AcademicAlertDto> capNhatXuLy(
            @PathVariable Long id,
            @RequestBody CapNhatTrangThaiRequest request
    ) {
        return ResponseEntity.ok(earlyWarningService.capNhatTrangThaiXuLy(id, request));
    }
}
