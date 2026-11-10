package com.edu.registration.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Bản ghi khóa phân tán trong cơ sở dữ liệu để chống chạy trùng giữa nhiều instance/worker (QLSV-93 / TASK-OP-06).
 */
@Entity
@Table(name = "distributed_lock", indexes = {
        @Index(name = "idx_dist_lock_expires", columnList = "expires_at")
})
public class DistributedLock {

    @Id
    @Column(name = "lock_name", length = 100, nullable = false)
    private String lockName;

    @Column(name = "locked_by", length = 100, nullable = false)
    private String lockedBy;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public DistributedLock() {
    }

    public DistributedLock(String lockName, String lockedBy, LocalDateTime lockedAt, LocalDateTime expiresAt) {
        this.lockName = lockName;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.expiresAt = expiresAt;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
