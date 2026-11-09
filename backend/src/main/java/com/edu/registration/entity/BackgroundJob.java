package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bản ghi tác vụ nền bền vững hỗ trợ hàng đợi, retry và chống trùng lặp (QLSV-93 / TASK-OP-06).
 */
@Entity
@Table(name = "background_jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_background_jobs_idempotency_key", columnNames = {"idempotency_key"})
}, indexes = {
        @Index(name = "idx_background_jobs_status_next_run", columnList = "status,next_run_at"),
        @Index(name = "idx_background_jobs_lease_expires", columnList = "status,lease_expires_at"),
        @Index(name = "idx_background_jobs_created_at", columnList = "created_at")
})
public class BackgroundJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_type", nullable = false, length = 64)
    private String jobType;

    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BackgroundJobStatus status = BackgroundJobStatus.PENDING;

    @Column(length = 4000)
    private String payload;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "backoff_seconds", nullable = false)
    private int backoffSeconds = 30;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "lease_expires_at")
    private LocalDateTime leaseExpiresAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public BackgroundJob() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.nextRunAt == null) {
            this.nextRunAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public BackgroundJobStatus getStatus() {
        return status;
    }

    public void setStatus(BackgroundJobStatus status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBackoffSeconds() {
        return backoffSeconds;
    }

    public void setBackoffSeconds(int backoffSeconds) {
        this.backoffSeconds = backoffSeconds;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
