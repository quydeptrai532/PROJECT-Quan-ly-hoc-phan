package com.edu.registration.controller;

import com.edu.registration.dto.AcademicAlertDtos.*;
import com.edu.registration.entity.AcademicAlertRuleType;
import com.edu.registration.entity.AcademicAlertSeverity;
import com.edu.registration.entity.AcademicAlertStatus;
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
@RequestMapping("/api/admin/canh-bao-hoc-tap")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Admin Early Warning", description = "Quản lý hệ thống cảnh báo sớm học tập theo quy tắc (QLSV-97 / TASK-OP-10)")
public class AdminEarlyWarningController {

    private final EarlyWarningService earlyWarningService;

    public AdminEarlyWarningController(EarlyWarningService earlyWarningService) {
        this.earlyWarningService = earlyWarningService;
    }

    @GetMapping
    @Operation(summary = "Danh sách cảnh báo học tập phân trang và lọc đa tiêu chí")
    public ResponseEntity<Page<AcademicAlertDto>> danhSachCanhBao(
            @RequestParam(required = false) AcademicAlertStatus trangThai,
            @RequestParam(required = false) AcademicAlertRuleType loaiLuat,
            @RequestParam(required = false) AcademicAlertSeverity mucDo,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(earlyWarningService.danhSachCanhBaoAdmin(trangThai, loaiLuat, mucDo, active, keyword, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một cảnh báo kèm công thức tính và dữ liệu dẫn chứng")
    public ResponseEntity<AcademicAlertDto> chiTietCanhBao(@PathVariable Long id) {
        return ResponseEntity.ok(earlyWarningService.layChiTiet(id));
    }

    @PostMapping("/quet-du-lieu")
    @Operation(summary = "Kích hoạt quét và đánh giá lại cảnh báo trên toàn hệ thống")
    public ResponseEntity<ScanAlertsResultDto> quetDuLieuToanTruong() {
        return ResponseEntity.ok(earlyWarningService.quetToanBoCanhBao());
    }

    @PatchMapping("/{id}/xu-ly")
    @Operation(summary = "Đánh dấu trạng thái xử lý cảnh báo (Đang xử lý, Đã xử lý, Bỏ qua) kèm ghi chú")
    public ResponseEntity<AcademicAlertDto> capNhatXuLy(
            @PathVariable Long id,
            @RequestBody CapNhatTrangThaiRequest request
    ) {
        return ResponseEntity.ok(earlyWarningService.capNhatTrangThaiXuLy(id, request));
    }

    @GetMapping("/cau-hinh")
    @Operation(summary = "Xem cấu hình tập trung, danh sách quy tắc và các giá trị chờ Hội đồng Đào tạo xác nhận")
    public ResponseEntity<EarlyWarningConfigDto> xemCauHinh() {
        return ResponseEntity.ok(earlyWarningService.layCauHinh());
    }
}
