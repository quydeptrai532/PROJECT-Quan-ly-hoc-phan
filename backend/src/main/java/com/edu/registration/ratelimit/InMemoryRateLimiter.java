package com.edu.registration.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cài đặt Rate Limiter chạy trong bộ nhớ ứng dụng (In-Memory), hiệu năng cao, thread-safe (QLSV-94 / TASK-OP-07 Work Package B).
 */
@Component
public class InMemoryRateLimiter implements RateLimiter {

    private record WindowCounter(long windowStartEpochSec, AtomicLong count) {}

    private final Map<String, WindowCounter> storage = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryAcquire(String key, long capacity, long windowSeconds) {
        long nowSec = Instant.now().getEpochSecond();

        // Định kỳ dọn các khóa quá hạn (trên 10 phút không hoạt động)
        if (storage.size() > 5000) {
            storage.entrySet().removeIf(entry -> nowSec - entry.getValue().windowStartEpochSec > windowSeconds * 2);
        }

        WindowCounter counter = storage.compute(key, (k, current) -> {
            if (current == null || (nowSec - current.windowStartEpochSec) >= windowSeconds) {
                return new WindowCounter(nowSec, new AtomicLong(1));
            }
            current.count.incrementAndGet();
            return current;
        });

        long currentCount = counter.count.get();
        long elapsedSec = nowSec - counter.windowStartEpochSec;
        long retryAfter = Math.max(1, windowSeconds - elapsedSec);

        if (currentCount <= capacity) {
            long remaining = capacity - currentCount;
            return RateLimitResult.allowed(capacity, remaining);
        } else {
            return RateLimitResult.rejected(capacity, retryAfter);
        }
    }

    public void reset(String key) {
        storage.remove(key);
    }

    public void clear() {
        storage.clear();
    }
}
