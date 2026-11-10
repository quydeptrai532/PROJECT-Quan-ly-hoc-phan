package com.edu.registration.controller;

import com.edu.registration.dto.AuditLogDtos;
import com.edu.registration.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Nhật ký kiểm toán", description = "Truy vết nhật ký thay đổi dữ liệu, người thực hiện, thời gian và địa chỉ IP")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm nhật ký kiểm toán", description = "Tìm kiếm log kiểm toán theo khoảng thời gian, người thao tác, loại thực thể và hành động")
    public Page<AuditLogDtos.AuditLogDto> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return auditLogService.search(from, to, actor, action, entityType, entityId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết nhật ký kiểm toán", description = "Lấy chi tiết payload trước và sau thay đổi của một sự kiện kiểm toán")
    public AuditLogDtos.AuditLogDto get(@PathVariable Long id) {
        return auditLogService.get(id);
    }
}