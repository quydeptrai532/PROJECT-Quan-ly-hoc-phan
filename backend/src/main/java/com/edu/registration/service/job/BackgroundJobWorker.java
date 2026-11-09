package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.repository.BackgroundJobRepository;
import com.edu.registration.util.SecretSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Worker xử lý tác vụ nền: quét hàng đợi, tranh chấp lease bằng atomic update,
 * phục hồi tác vụ kẹt do worker restart, tính toán backoff và ghi nhận kết quả bền vững (QLSV-93 / TASK-OP-06).
 */
@Component
public class BackgroundJobWorker {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobWorker.class);

    private final BackgroundJobRepository jobRepository;
    private final BackgroundJobProperties properties;
    private final DistributedLockService lockService;
    private final Map<String, BackgroundJobHandler> handlers = new HashMap<>();
    private final TransactionTemplate requiresNewTemplate;

    public BackgroundJobWorker(BackgroundJobRepository jobRepository,
                               BackgroundJobProperties properties,
                               DistributedLockService lockService,
                               List<BackgroundJobHandler> handlerList,
                               PlatformTransactionManager transactionManager) {
        this.jobRepository = jobRepository;
        this.properties = properties;
        this.lockService = lockService;
        if (handlerList != null) {
            for (BackgroundJobHandler handler : handlerList) {
                this.handlers.put(handler.getJobType(), handler);
            }
        }
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    /**
     * Chu kỳ quét định kỳ các tác vụ nền pending và phục hồi tác vụ kẹt.
     */
    @Scheduled(fixedDelayString = "${app.background-jobs.poll-interval-ms:5000}")
    public void processJobsCycle() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            recoverStuckJobs();
            processPendingJobs();
        } catch (Exception ex) {
            log.error("Lỗi trong chu kỳ quét worker tác vụ nền: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Phục hồi các tác vụ bị kẹt ở trạng thái RUNNING khi worker trước đó bị crash, restart hoặc mất kết nối.
     */
    public void recoverStuckJobs() {
        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        List<BackgroundJob> stuckJobs = jobRepository.findByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAsc(
                BackgroundJobStatus.RUNNING, now, PageRequest.of(0, properties.getBatchSize()));

        for (BackgroundJob job : stuckJobs) {
            int newRetry = job.getRetryCount() + 1;
            String errorMsg = SecretSanitizer.sanitize("Quá hạn lease thực thi (" + properties.getLeaseTimeoutSeconds() +
                    "s). Worker sở hữu [" + job.getLockedBy() + "] có thể đã restart hoặc dừng đột ngột.");

            if (newRetry < job.getMaxRetries()) {
                long backoffSeconds = calculateBackoff(job.getBackoffSeconds(), newRetry,
                        properties.getBackoffMultiplier(), properties.getMaxBackoffSeconds());
                LocalDateTime nextRunAt = now.plusSeconds(backoffSeconds);

                int updated = executeInNewTx(() -> jobRepository.recoverStuckJob(
                        job.getId(), BackgroundJobStatus.RUNNING, BackgroundJobStatus.PENDING,
                        nextRunAt, errorMsg, now));

                if (updated > 0) {
                    log.warn("Đã phục hồi tác vụ kẹt [id={}, type={}] về PENDING để thử lại lần {}/{} vào {}",
                            job.getId(), job.getJobType(), newRetry, job.getMaxRetries(), nextRunAt);
                }
            } else {
                int updated = executeInNewTx(() -> jobRepository.recoverStuckJob(
                        job.getId(), BackgroundJobStatus.RUNNING, BackgroundJobStatus.FAILED,
                        now, errorMsg + " Đã hết số lần retry tối đa (" + job.getMaxRetries() + ").", now));

                if (updated > 0) {
                    log.error("Tác vụ kẹt [id={}, type={}] đã hết số lần retry ({}/{}), chuyển sang FAILED",
                            job.getId(), job.getJobType(), newRetry, job.getMaxRetries());
                }
            }
        }
    }

    /**
     * Quét và tranh chấp lease để xử lý các tác vụ PENDING đến hạn.
     */
    public void processPendingJobs() {
        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        List<BackgroundJob> pendingJobs = jobRepository.findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(
                BackgroundJobStatus.PENDING, now, PageRequest.of(0, properties.getBatchSize()));

        if (pendingJobs.isEmpty()) {
            return;
        }

        String workerId = properties.getWorkerId();
        LocalDateTime leaseExpires = now.plusSeconds(properties.getLeaseTimeoutSeconds());

        for (BackgroundJob candidate : pendingJobs) {
            // Tranh chấp khóa lease độc quyền nguyên tử bằng DB conditional update
            int claimed = executeInNewTx(() -> jobRepository.claimJobLease(
                    candidate.getId(), BackgroundJobStatus.PENDING, BackgroundJobStatus.RUNNING,
                    workerId, now, leaseExpires));

            if (claimed == 0) {
                // Instance khác đã tranh chấp trước tác vụ này
                log.debug("Tác vụ [id={}] đã được worker khác nhận lease, bỏ qua", candidate.getId());
                continue;
            }

            log.info("Worker [{}] đã nhận lease tác vụ [id={}, type={}] đến {}",
                    workerId, candidate.getId(), candidate.getJobType(), leaseExpires);

            executeClaimedJob(candidate.getId());
        }
    }

    /**
     * Thực thi tác vụ đã nhận lease.
     */
    private void executeClaimedJob(Long jobId) {
        BackgroundJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != BackgroundJobStatus.RUNNING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        BackgroundJobHandler handler = handlers.get(job.getJobType());

        if (handler == null) {
            String error = SecretSanitizer.sanitize("Không tìm thấy BackgroundJobHandler hỗ trợ cho loại tác vụ: " + job.getJobType());
            log.error("Tác vụ [id={}, type={}] thất bại: {}", job.getId(), job.getJobType(), error);
            markJobFailedPermanently(job, error, now);
            return;
        }

        try {
            handler.execute(job);
            markJobSucceeded(job, now);
        } catch (Exception ex) {
            handleJobExecutionFailure(job, ex, now);
        }
    }

    private void markJobSucceeded(BackgroundJob job, LocalDateTime now) {
        executeInNewTxVoid(() -> {
            BackgroundJob entity = jobRepository.findById(job.getId()).orElse(job);
            entity.setStatus(BackgroundJobStatus.SUCCEEDED);
            entity.setCompletedAt(now);
            entity.setLockedBy(null);
            entity.setLockedAt(null);
            entity.setLeaseExpiresAt(null);
            entity.setLastError(null);
            entity.setUpdatedAt(now);
            jobRepository.save(entity);
            log.info("Tác vụ nền [id={}, type={}] đã hoàn tất thành công", entity.getId(), entity.getJobType());
        });
    }

    private void handleJobExecutionFailure(BackgroundJob job, Exception ex, LocalDateTime now) {
        String safeError = SecretSanitizer.sanitize(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());

        executeInNewTxVoid(() -> {
            BackgroundJob entity = jobRepository.findById(job.getId()).orElse(job);
            int newRetry = entity.getRetryCount() + 1;
            entity.setRetryCount(newRetry);
            entity.setLastError(safeError);
            entity.setLockedBy(null);
            entity.setLockedAt(null);
            entity.setLeaseExpiresAt(null);
            entity.setUpdatedAt(now);

            if (newRetry < entity.getMaxRetries()) {
                long backoffSeconds = calculateBackoff(entity.getBackoffSeconds(), newRetry,
                        properties.getBackoffMultiplier(), properties.getMaxBackoffSeconds());
                LocalDateTime nextRunAt = now.plusSeconds(backoffSeconds);
                entity.setStatus(BackgroundJobStatus.PENDING);
                entity.setNextRunAt(nextRunAt);
                jobRepository.save(entity);
                log.warn("Tác vụ [id={}, type={}] thất bại lần {}/{}: {}. Sẽ thử lại sau {} giây ({})",
                        entity.getId(), entity.getJobType(), newRetry, entity.getMaxRetries(),
                        safeError, backoffSeconds, nextRunAt);
            } else {
                entity.setStatus(BackgroundJobStatus.FAILED);
                entity.setCompletedAt(now);
                jobRepository.save(entity);
                log.error("Tác vụ [id={}, type={}] thất bại hoàn toàn sau {}/{} lần thử: {}",
                        entity.getId(), entity.getJobType(), newRetry, entity.getMaxRetries(), safeError);
            }
        });
    }

    private void markJobFailedPermanently(BackgroundJob job, String error, LocalDateTime now) {
        executeInNewTxVoid(() -> {
            BackgroundJob entity = jobRepository.findById(job.getId()).orElse(job);
            entity.setStatus(BackgroundJobStatus.FAILED);
            entity.setCompletedAt(now);
            entity.setLastError(error);
            entity.setLockedBy(null);
            entity.setLockedAt(null);
            entity.setLeaseExpiresAt(null);
            entity.setUpdatedAt(now);
            jobRepository.save(entity);
        });
    }

    /**
     * Dọn dẹp định kỳ các bản ghi đã hoàn tất cũ (chạy hàng ngày lúc 02:00 sáng).
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanupOldJobsScheduled() {
        lockService.executeWithLock("lock:job:cleanup-old-background-jobs", Duration.ofMinutes(15), () -> {
            int retentionDays = properties.getRetentionDays();
            if (retentionDays > 0) {
                LocalDateTime cutoff = LocalDateTime.now(properties.getZoneId()).minusDays(retentionDays);
                int deleted = jobRepository.deleteOldCompletedJobs(cutoff);
                if (deleted > 0) {
                    log.info("Dọn dẹp {} tác vụ nền cũ đã hoàn tất trước {}", deleted, cutoff);
                }
            }
        });
    }

    public static long calculateBackoff(int baseBackoffSeconds, int retryCount, double multiplier, int maxBackoffSeconds) {
        if (retryCount <= 1) {
            return Math.min(baseBackoffSeconds, maxBackoffSeconds);
        }
        double factor = Math.pow(multiplier, retryCount - 1);
        long delay = (long) (baseBackoffSeconds * factor);
        return Math.min(delay, maxBackoffSeconds);
    }

    private int executeInNewTx(TxAction action) {
        if (requiresNewTemplate != null) {
            Integer res = requiresNewTemplate.execute(status -> action.run());
            return res != null ? res : 0;
        }
        return action.run();
    }

    private void executeInNewTxVoid(Runnable action) {
        if (requiresNewTemplate != null) {
            requiresNewTemplate.executeWithoutResult(status -> action.run());
        } else {
            action.run();
        }
    }

    @FunctionalInterface
    private interface TxAction {
        int run();
    }
}
