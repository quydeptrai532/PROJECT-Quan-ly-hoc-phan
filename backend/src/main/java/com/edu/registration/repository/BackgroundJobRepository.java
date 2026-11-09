package com.edu.registration.repository;

import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    Optional<BackgroundJob> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<BackgroundJob> findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
            BackgroundJobStatus status, LocalDateTime now, Pageable pageable);

    List<BackgroundJob> findByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAsc(
            BackgroundJobStatus status, LocalDateTime now, Pageable pageable);

    Page<BackgroundJob> findByStatus(BackgroundJobStatus status, Pageable pageable);

    Page<BackgroundJob> findByJobType(String jobType, Pageable pageable);

    Page<BackgroundJob> findByStatusAndJobType(BackgroundJobStatus status, String jobType, Pageable pageable);

    long countByStatus(BackgroundJobStatus status);

    @Modifying
    @Query("UPDATE BackgroundJob j SET j.status = :newStatus, j.lockedBy = :workerId, j.lockedAt = :now, " +
            "j.leaseExpiresAt = :leaseExpires, j.updatedAt = :now " +
            "WHERE j.id = :id AND j.status = :currentStatus")
    int claimJobLease(@Param("id") Long id,
                      @Param("currentStatus") BackgroundJobStatus currentStatus,
                      @Param("newStatus") BackgroundJobStatus newStatus,
                      @Param("workerId") String workerId,
                      @Param("now") LocalDateTime now,
                      @Param("leaseExpires") LocalDateTime leaseExpires);

    @Modifying
    @Query("UPDATE BackgroundJob j SET j.status = :newStatus, j.retryCount = j.retryCount + 1, " +
            "j.nextRunAt = :nextRunAt, j.lockedBy = NULL, j.lockedAt = NULL, j.leaseExpiresAt = NULL, " +
            "j.lastError = :lastError, j.updatedAt = :now " +
            "WHERE j.id = :id AND j.status = :currentStatus AND j.leaseExpiresAt <= :now")
    int recoverStuckJob(@Param("id") Long id,
                        @Param("currentStatus") BackgroundJobStatus currentStatus,
                        @Param("newStatus") BackgroundJobStatus newStatus,
                        @Param("nextRunAt") LocalDateTime nextRunAt,
                        @Param("lastError") String lastError,
                        @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM BackgroundJob j WHERE j.status IN ('SUCCEEDED', 'FAILED') AND j.completedAt <= :cutoff")
    int deleteOldCompletedJobs(@Param("cutoff") LocalDateTime cutoff);
}
