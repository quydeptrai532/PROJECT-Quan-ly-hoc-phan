package com.edu.registration.dto;

import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;

import java.time.LocalDateTime;

/**
 * Các DTO phục vụ tra cứu và quản trị tác vụ nền (QLSV-93 / TASK-OP-06).
 */
public final class BackgroundJobDtos {

    private BackgroundJobDtos() {
    }

    public record JobSummaryDto(
            Long id,
            String jobType,
            String idempotencyKey,
            BackgroundJobStatus status,
            int retryCount,
            int maxRetries,
            int backoffSeconds,
            LocalDateTime nextRunAt,
            String lockedBy,
            LocalDateTime lockedAt,
            LocalDateTime leaseExpiresAt,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {
        public static JobSummaryDto fromEntity(BackgroundJob job) {
            return new JobSummaryDto(
                    job.getId(),
                    job.getJobType(),
                    job.getIdempotencyKey(),
                    job.getStatus(),
                    job.getRetryCount(),
                    job.getMaxRetries(),
                    job.getBackoffSeconds(),
                    job.getNextRunAt(),
                    job.getLockedBy(),
                    job.getLockedAt(),
                    job.getLeaseExpiresAt(),
                    job.getLastError(),
                    job.getCreatedAt(),
                    job.getUpdatedAt(),
                    job.getCompletedAt()
            );
        }
    }

    public record JobDetailDto(
            Long id,
            String jobType,
            String idempotencyKey,
            BackgroundJobStatus status,
            String payload,
            int retryCount,
            int maxRetries,
            int backoffSeconds,
            LocalDateTime nextRunAt,
            String lockedBy,
            LocalDateTime lockedAt,
            LocalDateTime leaseExpiresAt,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime completedAt
    ) {
        public static JobDetailDto fromEntity(BackgroundJob job) {
            return new JobDetailDto(
                    job.getId(),
                    job.getJobType(),
                    job.getIdempotencyKey(),
                    job.getStatus(),
                    job.getPayload(),
                    job.getRetryCount(),
                    job.getMaxRetries(),
                    job.getBackoffSeconds(),
                    job.getNextRunAt(),
                    job.getLockedBy(),
                    job.getLockedAt(),
                    job.getLeaseExpiresAt(),
                    job.getLastError(),
                    job.getCreatedAt(),
                    job.getUpdatedAt(),
                    job.getCompletedAt()
            );
        }
    }

    public record CancelJobRequest(String reason) {}

    public record JobStatsDto(
            long pending,
            long running,
            long succeeded,
            long failed,
            long total
    ) {}
}
