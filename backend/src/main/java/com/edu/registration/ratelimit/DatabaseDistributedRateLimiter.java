package com.edu.registration.ratelimit;

import com.edu.registration.entity.RateLimitBucket;
import com.edu.registration.repository.RateLimitBucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cài đặt Rate Limiter phân tán lưu trữ trong cơ sở dữ liệu (Database-backed), an toàn khi ứng dụng chạy nhiều instance song song (QLSV-94 / TASK-OP-07 Work Package B).
 */
@Component
public class DatabaseDistributedRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDistributedRateLimiter.class);

    private final RateLimitBucketRepository bucketRepository;
    private final TransactionTemplate requiresNewTemplate;

    public DatabaseDistributedRateLimiter(RateLimitBucketRepository bucketRepository,
                                          PlatformTransactionManager transactionManager) {
        this.bucketRepository = bucketRepository;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    @Override
    public RateLimitResult tryAcquire(String key, long capacity, long windowSeconds) {
        if (requiresNewTemplate != null) {
            try {
                return requiresNewTemplate.execute(status -> doTryAcquire(key, capacity, windowSeconds));
            } catch (Exception ex) {
                log.warn("Lỗi khi kiểm tra distributed rate limit key [{}]: {}. Cho phép qua tạm thời.", key, ex.getMessage());
                return RateLimitResult.allowed(capacity, capacity - 1);
            }
        }
        return doTryAcquire(key, capacity, windowSeconds);
    }

    private RateLimitResult doTryAcquire(String key, long capacity, long windowSeconds) {
        LocalDateTime now = LocalDateTime.now();
        Optional<RateLimitBucket> optional = bucketRepository.findById(key);

        if (optional.isEmpty()) {
            LocalDateTime resetAt = now.plusSeconds(windowSeconds);
            RateLimitBucket bucket = new RateLimitBucket(key, 1, now, resetAt);
            bucketRepository.saveAndFlush(bucket);
            return RateLimitResult.allowed(capacity, capacity - 1);
        }

        RateLimitBucket bucket = optional.get();
        if (now.isAfter(bucket.getResetAt())) {
            // Cửa sổ thời gian cũ đã hết hạn, tạo cửa sổ mới
            bucket.setRequestCount(1);
            bucket.setWindowStart(now);
            bucket.setResetAt(now.plusSeconds(windowSeconds));
            bucketRepository.saveAndFlush(bucket);
            return RateLimitResult.allowed(capacity, capacity - 1);
        }

        long currentCount = bucket.getRequestCount() + 1;
        bucket.setRequestCount(currentCount);
        bucketRepository.saveAndFlush(bucket);

        long retryAfterSeconds = Math.max(1, Duration.between(now, bucket.getResetAt()).toSeconds());
        if (currentCount <= capacity) {
            long remaining = capacity - currentCount;
            return RateLimitResult.allowed(capacity, remaining);
        } else {
            return RateLimitResult.rejected(capacity, retryAfterSeconds);
        }
    }
}
