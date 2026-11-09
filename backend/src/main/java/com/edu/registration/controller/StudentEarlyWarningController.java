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
@RequestMapping("/api/sinh-vien/me")
@PreAuthorize("hasRole('SINH_VIEN')")
@Tag(name = "Student Early Warning", description = "Sinh viên xem cảnh báo học tập của chính mình theo chính sách cho phép")
public class StudentEarlyWarningController {

    private final EarlyWarningService earlyWarningService;

    public StudentEarlyWarningController(EarlyWarningService earlyWarningService) {
        this.earlyWarningService = earlyWarningService;
    }

    @GetMapping("/canh-bao-hoc-tap")
    @Operation(summary = "Sinh viên xem danh sách cảnh báo học tập của bản thân được chính sách cho phép")
    public ResponseEntity<Page<AcademicAlertDto>> danhSachCanhBaoCuaToi(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(earlyWarningService.danhSachCanhBaoSinhVien(pageable));
    }

    @GetMapping("/canh-bao-hoc-vu")
    @Operation(summary = "Xem cảnh báo học vụ của sinh viên (chuẩn hóa theo ma trận phân quyền)")
    public ResponseEntity<Page<AcademicAlertDto>> danhSachCanhBaoHocVu(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(earlyWarningService.danhSachCanhBaoSinhVien(pageable));
    }

    @GetMapping("/canh-bao-hoc-tap/tom-tat")
    @Operation(summary = "Tóm tắt nhanh số lượng cảnh báo học tập đang mở của sinh viên")
    public ResponseEntity<StudentAlertSummaryDto> tomTatCanhBao() {
        return ResponseEntity.ok(earlyWarningService.tomTatCanhBaoSinhVien());
    }

    @GetMapping("/canh-bao-hoc-tap/{id}")
    @Operation(summary = "Xem chi tiết một cảnh báo học tập của chính mình")
    public ResponseEntity<AcademicAlertDto> chiTietCanhBaoCuaToi(@PathVariable Long id) {
        return ResponseEntity.ok(earlyWarningService.layChiTiet(id));
    }
}
