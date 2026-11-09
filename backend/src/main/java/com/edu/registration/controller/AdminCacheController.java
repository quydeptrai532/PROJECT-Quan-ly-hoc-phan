package com.edu.registration.controller;

import com.edu.registration.service.AuditLogService;
import com.edu.registration.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * API quản trị và hủy bộ nhớ đệm (Cache Invalidation) (QLSV-94 / TASK-OP-07 Work Package C).
 */
@RestController
@RequestMapping("/api/admin/cache")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Admin Cache", description = "Quản lý và vô hiệu hóa bộ đệm hệ thống")
public class AdminCacheController {

    private final CacheManagementService cacheManagementService;
    private final AuditLogService auditLogService;

    public AdminCacheController(CacheManagementService cacheManagementService,
                                AuditLogService auditLogService) {
        this.cacheManagementService = cacheManagementService;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách các vùng cache đang hoạt động")
    public ResponseEntity<Map<String, Object>> listCaches() {
        return ResponseEntity.ok(Map.of(
                "caches", cacheManagementService.getCacheNames(),
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/clear")
    @Operation(summary = "Xóa toàn bộ hoặc một vùng cache cụ thể")
    public ResponseEntity<Map<String, Object>> clearCache(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return auditLogService.execute("CACHE_CLEAR", "CACHE", name, ctx -> {
                boolean success = cacheManagementService.clearCache(name);
                return ResponseEntity.ok(Map.of(
                        "success", success,
                        "target", name,
                        "message", success ? "Đã xóa cache: " + name : "Không tìm thấy cache: " + name,
                        "timestamp", Instant.now().toString()
                ));
            });
        } else {
            return auditLogService.execute("CACHE_CLEAR_ALL", "CACHE", "ALL", ctx -> {
                cacheManagementService.clearAllCaches();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "target", "ALL",
                        "message", "Đã xóa toàn bộ bộ đệm trong hệ thống",
                        "timestamp", Instant.now().toString()
                ));
            });
        }
    }
}
