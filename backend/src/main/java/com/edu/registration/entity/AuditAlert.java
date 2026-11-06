package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "audit_alerts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_audit_alert_fingerprint",
                columnNames = "fingerprint"
        ),
        indexes = @Index(
                name = "idx_audit_alert_status_time",
                columnList = "status,occurred_at"
        )
)
public class AuditAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(name = "subject", nullable = false, length = 160)
    private String subject;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(nullable = false, length = 300)
    private String fingerprint;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "source_audit_ids", nullable = false, length = 4000)
    private String sourceAuditIds;

    @Column(name = "resolved_by", length = 50)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    public Long getId() { return id; }

    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public LocalDateTime getWindowStart() { return windowStart; }
    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getWindowEnd() { return windowEnd; }
    public void setWindowEnd(LocalDateTime windowEnd) {
        this.windowEnd = windowEnd;
    }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceAuditIds() { return sourceAuditIds; }
    public void setSourceAuditIds(String sourceAuditIds) {
        this.sourceAuditIds = sourceAuditIds;
    }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }
}