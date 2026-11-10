package com.edu.registration.controller;

import com.edu.registration.dto.AuditAlertDtos;
import com.edu.registration.service.AuditAnomalyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-alerts")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Cảnh báo kiểm toán", description = "Phát hiện và xử lý các hành vi bất thường hoặc vi phạm chính sách bảo mật hệ thống")
public class AdminAuditAlertController {

    private final AuditAnomalyService service;

    public AdminAuditAlertController(
            AuditAnomalyService service) {
        this.service = service;
    }

    // GET: Danh sách cảnh báo
    @GetMapping
    @Operation(summary = "Danh sách cảnh báo kiểm toán", description = "Lấy danh sách các cảnh báo bất thường trong hệ thống")
    public Page<AuditAlertDtos.AlertDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return service.list(status, page, size);
    }

    // POST: Đánh dấu cảnh báo đã xử lý
    @PostMapping("/{id}/resolve")
    @Operation(summary = "Xử lý cảnh báo kiểm toán", description = "Đánh dấu cảnh báo kiểm toán đã được xem xét và ghi chú kết quả xử lý")
    public AuditAlertDtos.AlertDto resolve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false)
            AuditAlertDtos.ResolveRequest request) {

        return service.resolve(
                id,
                request == null ? null : request.note()
        );
    }

    // GET: Danh sách luật cảnh báo
    @GetMapping("/rules")
    @Operation(summary = "Danh sách quy tắc cảnh báo", description = "Xem các quy tắc cảnh báo bất thường đang hoạt động")
    public List<AuditAlertDtos.RuleDto> rules() {
        return service.getRules();
    }

    // PUT: Cập nhật luật cảnh báo
    @PutMapping("/rules/{code}")
    @Operation(summary = "Cập nhật quy tắc cảnh báo", description = "Điều chỉnh ngưỡng kích hoạt hoặc bật/tắt quy tắc cảnh báo")
    public AuditAlertDtos.RuleDto updateRule(
            @PathVariable String code,
            @Valid @RequestBody AuditAlertDtos.RuleDto request) {

        return service.updateRule(code, request);
    }
}