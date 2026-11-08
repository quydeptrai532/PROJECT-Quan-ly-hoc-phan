package com.edu.registration.service;

import com.edu.registration.dto.AuditAlertDtos;
import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditAnomalyService {

    private final AuditAlertRepository alerts;
    private final AuditAlertRuleRepository rules;
    private final AuditLogRepository logs;
    private final ObjectMapper mapper;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    public AuditAnomalyService(
            AuditAlertRepository alerts,
            AuditAlertRuleRepository rules,
            AuditLogRepository logs,
            ObjectMapper mapper,
            AuditLogService auditLogService,
            CurrentUser currentUser) {

        this.alerts = alerts;
        this.rules = rules;
        this.logs = logs;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
    }

    // Khởi tạo luật mặc định
    @PostConstruct
    @Transactional
    public void initializeRules() {
        addRule("LOGIN_FAILURES", "Nhiều lần đăng nhập thất bại",
                true, 5, 10, "HIGH");

        addRule("GRADE_BULK", "Thao tác bảng điểm số lượng lớn",
                true, 10, 15, "HIGH");

        addRule("GRADE_AFTER_HOURS", "Thay đổi bảng điểm ngoài giờ",
                true, 1, 60, "MEDIUM");

        addRule("REGISTRATION_REPEAT", "Đăng ký/hủy học phần lặp bất thường",
                true, 6, 10, "MEDIUM");
    }

    private void addRule(
            String code, String name, boolean enabled,
            int threshold, int minutes, String severity) {

        if (!rules.existsById(code)) {
            AuditAlertRule rule = new AuditAlertRule();

            rule.setCode(code);
            rule.setName(name);
            rule.setEnabled(enabled);
            rule.setThreshold(threshold);
            rule.setWindowMinutes(minutes);
            rule.setSeverity(severity);

            rules.save(rule);
        }
    }

    // Quét audit log mỗi 60 giây
    @Scheduled(fixedDelayString =
            "${app.audit-anomaly.scan-delay-ms:60000}")
    @Transactional
    public void scan() {

        LocalDateTime now = LocalDateTime.now();

        List<AuditLog> recent =
                logs.findAllByOccurredAtAfterOrderByOccurredAtAsc(
                        now.minusHours(24));

        Map<String, AuditAlertRule> config =
                rules.findAll().stream().collect(
                        Collectors.toMap(
                                AuditAlertRule::getCode,
                                rule -> rule
                        ));

        detectGrouped(
                recent,
                config.get("LOGIN_FAILURES"),
                "LOGIN_FAILED",
                true
        );

        detectGrouped(
                recent,
                config.get("REGISTRATION_REPEAT"),
                null,
                false
        );

        AuditAlertRule bulk = config.get("GRADE_BULK");
        AuditAlertRule after = config.get("GRADE_AFTER_HOURS");

        detectGradeBulk(recent, bulk);

        for (AuditLog row : recent) {

            if (!isGradeAction(row.getAction())) {
                continue;
            }

            if (after != null && after.isEnabled()
                    && (row.getOccurredAt().getHour() >= 20
                    || row.getOccurredAt().getHour() < 6)) {

                createAlert(
                        after,
                        row.getActorUsername(),
                        List.of(row),
                        row.getOccurredAt(),
                        "Bảng điểm được thao tác ngoài khung giờ 06:00–20:00"
                );
            }

            int count = detailCount(row.getDetailsJson());

            if (bulk != null && bulk.isEnabled()
                    && count >= bulk.getThreshold()) {

                createAlert(
                        bulk,
                        row.getActorUsername(),
                        List.of(row),
                        row.getOccurredAt(),
                        "Thao tác bảng điểm ảnh hưởng "
                                + count + " bản ghi"
                );
            }
        }
    }

    private boolean isGradeAction(String action) {
        return action != null
                && (action.startsWith("GRADE_")
                || action.contains("DIEM"));
    }

    // Phát hiện cập nhật điểm nhiều lần
    private void detectGradeBulk(
            List<AuditLog> recent,
            AuditAlertRule rule) {

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        Map<String, List<AuditLog>> grouped =
                recent.stream()
                        .filter(a -> "GRADE_UPDATED".equals(a.getAction())
                                && "SUCCESS".equals(a.getResult()))
                        .collect(Collectors.groupingBy(
                                AuditLog::getActorUsername
                        ));

        for (var entry : grouped.entrySet()) {

            List<AuditLog> events = entry.getValue()
                    .stream()
                    .sorted(Comparator.comparing(
                            AuditLog::getOccurredAt))
                    .toList();

            for (int i = 0; i < events.size(); i++) {

                LocalDateTime anchor =
                        events.get(i).getOccurredAt();

                LocalDateTime end =
                        anchor.plusMinutes(rule.getWindowMinutes());

                List<AuditLog> window = events.stream()
                        .filter(e ->
                                !e.getOccurredAt().isBefore(anchor)
                                        && !e.getOccurredAt().isAfter(end))
                        .toList();

                if (window.size() >= rule.getThreshold()) {

                    createAlert(
                            rule,
                            entry.getKey(),
                            window,
                            window.get(window.size() - 1).getOccurredAt(),
                            "Cập nhật điểm nhiều lần trong một cửa sổ thời gian"
                    );

                    break;
                }
            }
        }
    }

    private int detailCount(String json) {
        try {
            JsonNode node = mapper.readTree(json);

            for (String key : List.of(
                    "soBanGhiDiem", "soBanGhi")) {

                if (node.path("after").has(key)) {
                    return node.path("after")
                            .path(key).asInt(0);
                }

                if (node.has(key)) {
                    return node.path(key).asInt(0);
                }
            }

        } catch (Exception ignored) {
            // Không có dữ liệu chi tiết thì bỏ qua luật này
        }

        return 0;
    }

    // Phát hiện đăng nhập thất bại và đăng ký/hủy lặp
    private void detectGrouped(
            List<AuditLog> recent,
            AuditAlertRule rule,
            String exactAction,
            boolean login) {

        if (rule == null || !rule.isEnabled()) {
            return;
        }

        Map<String, List<AuditLog>> grouped =
                recent.stream()
                        .filter(a -> {
                            if (exactAction != null) {
                                return exactAction.equals(a.getAction());
                            }

                            return Set.of(
                                    "COURSE_REGISTRATION",
                                    "COURSE_REGISTRATION_CANCELLED"
                            ).contains(a.getAction())
                                    && "SUCCESS".equals(a.getResult());
                        })
                        .collect(Collectors.groupingBy(
                                a -> a.getActorUsername() == null
                                        ? "unknown"
                                        : a.getActorUsername()
                        ));

        for (var entry : grouped.entrySet()) {

            List<AuditLog> events = entry.getValue()
                    .stream()
                    .sorted(Comparator.comparing(
                            AuditLog::getOccurredAt))
                    .toList();

            for (int i = 0; i < events.size(); i++) {

                AuditLog first = events.get(i);

                LocalDateTime end =
                        first.getOccurredAt()
                                .plusMinutes(rule.getWindowMinutes());

                List<AuditLog> window = events.stream()
                        .filter(e ->
                                !e.getOccurredAt()
                                        .isBefore(first.getOccurredAt())
                                        && !e.getOccurredAt().isAfter(end))
                        .toList();

                if (window.size() >= rule.getThreshold()) {

                    createAlert(
                            rule,
                            entry.getKey(),
                            window,
                            window.get(window.size() - 1).getOccurredAt(),
                            login
                                    ? "Tài khoản có nhiều lần đăng nhập thất bại"
                                    : "Tài khoản đăng ký/hủy học phần lặp trong thời gian ngắn"
                    );

                    break;
                }
            }
        }
    }

    // Tạo cảnh báo và chống trùng lặp
    private void createAlert(
            AuditAlertRule rule,
            String subject,
            List<AuditLog> source,
            LocalDateTime occurred,
            String description) {

        if (source.isEmpty()) {
            return;
        }

        String fingerprint =
                rule.getCode() + ":"
                        + subject + ":"
                        + source.get(0).getOccurredAt() + ":"
                        + source.get(0).getId();

        if (alerts.findByFingerprint(fingerprint).isPresent()) {
            return;
        }

        AuditAlert alert = new AuditAlert();

        alert.setRuleCode(rule.getCode());
        alert.setTitle(description);
        alert.setSeverity(rule.getSeverity());
        alert.setSubject(subject);
        alert.setOccurredAt(occurred);

        alert.setWindowStart(source.get(0).getOccurredAt());

        alert.setWindowEnd(
                source.get(source.size() - 1).getOccurredAt());

        alert.setFingerprint(fingerprint);

        alert.setSourceAuditIds(
                source.stream()
                        .limit(100)
                        .map(AuditLog::getId)
                        .map(String::valueOf)
                        .collect(Collectors.joining(","))
        );

        alert.setStatus("OPEN");

        alerts.save(alert);
    }

    // Danh sách cảnh báo
    @Transactional(readOnly = true)
    public Page<AuditAlertDtos.AlertDto> list(
            String status, int page, int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "occurredAt")
        );

        Page<AuditAlert> result =
                status == null || status.isBlank()
                        ? alerts.findAllByOrderByOccurredAtDesc(pageable)
                        : alerts.findAllByStatusOrderByOccurredAtDesc(
                        status, pageable);

        return result.map(this::dto);
    }

    // Đánh dấu đã xử lý
    @Transactional
    public AuditAlertDtos.AlertDto resolve(
            Long id, String note) {

        AuditAlert alert = alerts.findById(id)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Không tìm thấy cảnh báo"));

        if ("RESOLVED".equals(alert.getStatus())) {
            throw ApiException.badRequest(
                    "Cảnh báo đã được xử lý trước đó");
        }

        User actor = currentUser.get();

        return auditLogService.execute(
                "AUDIT_ALERT_RESOLVED",
                "AUDIT_ALERT",
                id,
                audit -> {

                    String before = alert.getStatus();

                    alert.setStatus("RESOLVED");
                    alert.setResolvedBy(actor.getUsername());
                    alert.setResolvedAt(LocalDateTime.now());
                    alert.setResolutionNote(
                            note == null ? null : note.trim());

                    audit.changes(
                            Map.of("trangThaiCanhBao", before),
                            Map.of("trangThaiCanhBao", "RESOLVED")
                    );

                    return dto(alerts.save(alert));
                }
        );
    }

    // Lấy cấu hình luật
    @Transactional(readOnly = true)
    public List<AuditAlertDtos.RuleDto> getRules() {

        return rules.findAll(Sort.by("code"))
                .stream()
                .map(this::ruleDto)
                .toList();
    }

    // Cập nhật cấu hình luật
    @Transactional
    public AuditAlertDtos.RuleDto updateRule(
            String code,
            AuditAlertDtos.RuleDto request) {

        AuditAlertRule rule = rules.findById(code)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Không tìm thấy luật cảnh báo"));

        if (request.threshold() < 1
                || request.threshold() > 10000
                || request.windowMinutes() < 1
                || request.windowMinutes() > 10080) {

            throw ApiException.badRequest(
                    "Ngưỡng hoặc cửa sổ thời gian không hợp lệ");
        }

        if (request.severity() == null
                || !Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL")
                .contains(request.severity())) {

            throw ApiException.badRequest(
                    "Mức độ cảnh báo không hợp lệ");
        }

        return auditLogService.execute(
                "AUDIT_ALERT_RULE_UPDATED",
                "AUDIT_ALERT_RULE",
                code,
                audit -> {

                    audit.changes(
                            Map.of("kichHoat", rule.isEnabled(),
                                    "nguong", rule.getThreshold(),
                                    "cuaSoPhut", rule.getWindowMinutes(),
                                    "mucDo", rule.getSeverity()),
                            Map.of("kichHoat", request.enabled(),
                                    "nguong", request.threshold(),
                                    "cuaSoPhut", request.windowMinutes(),
                                    "mucDo", request.severity())
                    );

                    rule.setEnabled(request.enabled());
                    rule.setThreshold(request.threshold());
                    rule.setWindowMinutes(request.windowMinutes());
                    rule.setSeverity(request.severity());

                    if (request.name() != null && !request.name().isBlank()) {
                        rule.setName(request.name().trim());
                    }

                    return ruleDto(rules.save(rule));
                }
        );
    }

    private AuditAlertDtos.RuleDto ruleDto(
            AuditAlertRule rule) {

        return new AuditAlertDtos.RuleDto(
                rule.getCode(),
                rule.getName(),
                rule.isEnabled(),
                rule.getThreshold(),
                rule.getWindowMinutes(),
                rule.getSeverity()
        );
    }

    private AuditAlertDtos.AlertDto dto(AuditAlert alert) {

        List<Long> ids = Arrays.stream(
                        alert.getSourceAuditIds().split(","))
                .filter(x -> !x.isBlank())
                .map(Long::valueOf)
                .toList();

        return new AuditAlertDtos.AlertDto(
                alert.getId(),
                alert.getRuleCode(),
                alert.getTitle(),
                alert.getSeverity(),
                alert.getSubject(),
                alert.getOccurredAt(),
                alert.getWindowStart(),
                alert.getWindowEnd(),
                alert.getStatus(),
                ids,
                alert.getResolvedBy(),
                alert.getResolvedAt(),
                alert.getResolutionNote()
        );
    }
}