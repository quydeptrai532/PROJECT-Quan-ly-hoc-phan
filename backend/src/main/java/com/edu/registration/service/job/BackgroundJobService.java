package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.BackgroundJobRepository;
import com.edu.registration.util.SecretSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Quản lý vòng đời, xếp hàng và tra cứu tác vụ nền bền vững (QLSV-93 / TASK-OP-06).
 */
@Service
public class BackgroundJobService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobService.class);

    private final BackgroundJobRepository jobRepository;
    private final BackgroundJobProperties properties;

    public BackgroundJobService(BackgroundJobRepository jobRepository,
                                BackgroundJobProperties properties) {
        this.jobRepository = jobRepository;
        this.properties = properties;
    }

    /**
     * Đưa tác vụ vào hàng đợi với cơ chế idempotent theo idempotencyKey.
     */
    @Transactional
    public BackgroundJob enqueue(String jobType, String idempotencyKey, String payload,
                                 Integer maxRetries, Integer backoffSeconds, LocalDateTime runAt) {
        if (jobType == null || jobType.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "jobType không được để trống");
        }

        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        LocalDateTime scheduledTime = runAt != null ? runAt : now;

        // Kiểm tra chống trùng theo idempotencyKey
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<BackgroundJob> existing = jobRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                BackgroundJob job = existing.get();
                log.debug("Tác vụ đã tồn tại với idempotencyKey={} (status={})", idempotencyKey, job.getStatus());
                return job;
            }
        }

        BackgroundJob job = new BackgroundJob();
        job.setJobType(jobType);
        job.setIdempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null);
        job.setPayload(payload != null ? SecretSanitizer.sanitize(payload, 4000) : null);
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setRetryCount(0);
        job.setMaxRetries(maxRetries != null ? Math.max(1, maxRetries) : properties.getDefaultMaxRetries());
        job.setBackoffSeconds(backoffSeconds != null ? Math.max(1, backoffSeconds) : properties.getDefaultBackoffSeconds());
        job.setNextRunAt(scheduledTime);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        try {
            BackgroundJob saved = jobRepository.save(job);
            log.info("Đã ghi nhận tác vụ nền bền vững [id={}, type={}, key={}, nextRunAt={}]",
                    saved.getId(), saved.getJobType(), saved.getIdempotencyKey(), saved.getNextRunAt());
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // Chống xung đột đồng thời ghi nhận cùng một idempotencyKey
            if (idempotencyKey != null) {
                return jobRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    /**
     * Cho phép quản trị viên kích hoạt thử lại thủ công một tác vụ thất bại.
     */
    @Transactional
    public BackgroundJob retryJob(Long id) {
        BackgroundJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tác vụ với id=" + id));

        if (job.getStatus() == BackgroundJobStatus.RUNNING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tác vụ đang chạy, không thể yêu cầu thử lại");
        }

        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setNextRunAt(now);
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setLeaseExpiresAt(null);
        job.setCompletedAt(null);
        job.setUpdatedAt(now);
        // Tăng thêm lượt retry nếu đã hết lượt cũ
        if (job.getRetryCount() >= job.getMaxRetries()) {
            job.setMaxRetries(job.getRetryCount() + 1);
        }

        log.info("Quản trị viên đã yêu cầu thử lại tác vụ [id={}, type={}]", job.getId(), job.getJobType());
        return jobRepository.save(job);
    }

    /**
     * Hủy tác vụ đang chờ xử lý hoặc đã thất bại.
     */
    @Transactional
    public BackgroundJob cancelJob(Long id, String reason) {
        BackgroundJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tác vụ với id=" + id));

        if (job.getStatus() == BackgroundJobStatus.RUNNING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tác vụ đang chạy, không thể hủy");
        }
        if (job.getStatus() == BackgroundJobStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tác vụ đã hoàn tất thành công, không thể hủy");
        }

        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        job.setStatus(BackgroundJobStatus.FAILED);
        job.setLastError(SecretSanitizer.sanitize("Tác vụ đã bị hủy bởi quản trị viên: " + (reason != null ? reason : "Không có lý do cụ thể")));
        job.setCompletedAt(now);
        job.setLockedBy(null);
        job.setLockedAt(null);
        job.setLeaseExpiresAt(null);
        job.setUpdatedAt(now);

        log.info("Quản trị viên đã hủy tác vụ [id={}, type={}]", job.getId(), job.getJobType());
        return jobRepository.save(job);
    }

    /**
     * Tra cứu danh sách tác vụ phân trang phục vụ vận hành.
     */
    @Transactional(readOnly = true)
    public Page<BackgroundJob> listJobs(BackgroundJobStatus status, String jobType, Pageable pageable) {
        if (status != null && jobType != null && !jobType.isBlank()) {
            return jobRepository.findByStatusAndJobType(status, jobType, pageable);
        }
        if (status != null) {
            return jobRepository.findByStatus(status, pageable);
        }
        if (jobType != null && !jobType.isBlank()) {
            return jobRepository.findByJobType(jobType, pageable);
        }
        return jobRepository.findAll(pageable);
    }

    /**
     * Tra cứu chi tiết một tác vụ.
     */
    @Transactional(readOnly = true)
    public Optional<BackgroundJob> getJob(Long id) {
        return jobRepository.findById(id);
    }

    /**
     * Thống kê số lượng tác vụ theo trạng thái cho màn hình vận hành.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        long pending = jobRepository.countByStatus(BackgroundJobStatus.PENDING);
        long running = jobRepository.countByStatus(BackgroundJobStatus.RUNNING);
        long succeeded = jobRepository.countByStatus(BackgroundJobStatus.SUCCEEDED);
        long failed = jobRepository.countByStatus(BackgroundJobStatus.FAILED);
        stats.put("pending", pending);
        stats.put("running", running);
        stats.put("succeeded", succeeded);
        stats.put("failed", failed);
        stats.put("total", pending + running + succeeded + failed);
        return stats;
    }

    /**
     * Dọn dẹp các tác vụ thành công hoặc thất bại cũ vượt quá số ngày lưu trữ cấu hình.
     */
    @Transactional
    public int cleanupOldCompletedJobs() {
        int retentionDays = properties.getRetentionDays();
        if (retentionDays <= 0) {
            return 0;
        }
        LocalDateTime cutoff = LocalDateTime.now(properties.getZoneId()).minusDays(retentionDays);
        int deleted = jobRepository.deleteOldCompletedJobs(cutoff);
        if (deleted > 0) {
            log.info("Đã dọn dẹp {} tác vụ nền cũ trước {}", deleted, cutoff);
        }
        return deleted;
    }
}
