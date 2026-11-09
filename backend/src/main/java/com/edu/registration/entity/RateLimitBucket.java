package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bản ghi lưu trạng thái rate limit dùng chung giữa các instance/worker khi chạy cụm phân tán (QLSV-94 / TASK-OP-07 Work Package B).
 */
@Entity
@Table(name = "rate_limit_bucket", indexes = {
        @Index(name = "idx_rl_reset_at", columnList = "reset_at")
})
public class RateLimitBucket {

    @Id
    @Column(name = "bucket_key", length = 120, nullable = false)
    private String bucketKey;

    @Column(name = "request_count", nullable = false)
    private long requestCount;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "reset_at", nullable = false)
    private LocalDateTime resetAt;

    public RateLimitBucket() {
    }

    public RateLimitBucket(String bucketKey, long requestCount, LocalDateTime windowStart, LocalDateTime resetAt) {
        this.bucketKey = bucketKey;
        this.requestCount = requestCount;
        this.windowStart = windowStart;
        this.resetAt = resetAt;
    }

    public String getBucketKey() {
        return bucketKey;
    }

    public void setBucketKey(String bucketKey) {
        this.bucketKey = bucketKey;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public LocalDateTime getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(LocalDateTime windowStart) {
        this.windowStart = windowStart;
    }

    public LocalDateTime getResetAt() {
        return resetAt;
    }

    public void setResetAt(LocalDateTime resetAt) {
        this.resetAt = resetAt;
    }
}
