package com.edu.registration.controller;

import com.edu.registration.dto.AuditAlertDtos;
import com.edu.registration.service.AuditAnomalyService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-alerts")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminAuditAlertController {

    private final AuditAnomalyService service;

    public AdminAuditAlertController(
            AuditAnomalyService service) {
        this.service = service;
    }

    // GET: Danh sách cảnh báo
    @GetMapping
    public Page<AuditAlertDtos.AlertDto> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return service.list(status, page, size);
    }

    // POST: Đánh dấu cảnh báo đã xử lý
    @PostMapping("/{id}/resolve")
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
    public List<AuditAlertDtos.RuleDto> rules() {
        return service.getRules();
    }

    // PUT: Cập nhật luật cảnh báo
    @PutMapping("/rules/{code}")
    public AuditAlertDtos.RuleDto updateRule(
            @PathVariable String code,
            @Valid @RequestBody AuditAlertDtos.RuleDto request) {

        return service.updateRule(code, request);
    }
}