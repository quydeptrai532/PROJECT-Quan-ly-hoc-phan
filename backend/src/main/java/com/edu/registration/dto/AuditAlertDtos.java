package com.edu.registration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AuditAlertDtos {

    private AuditAlertDtos() {}

    public record AlertDto(
            Long id,
            String ruleCode,
            String title,
            String severity,
            String subject,
            LocalDateTime occurredAt,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            String status,
            List<Long> sourceAuditIds,
            String resolvedBy,
            LocalDateTime resolvedAt,
            String resolutionNote
    ) {}

    public record ResolveRequest(
            @Size(max = 500)
            String note
    ) {}

    public record RuleDto(
            String code,
            String name,
            boolean enabled,

            @Min(1)
            @Max(10000)
            int threshold,

            @Min(1)
            @Max(10080)
            int windowMinutes,

            String severity
    ) {}
}