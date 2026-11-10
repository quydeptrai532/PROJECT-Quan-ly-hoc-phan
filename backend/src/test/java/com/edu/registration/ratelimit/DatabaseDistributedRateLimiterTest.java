package com.edu.registration.ratelimit;

import com.edu.registration.entity.RateLimitBucket;
import com.edu.registration.repository.RateLimitBucketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DatabaseDistributedRateLimiterTest {

    private RateLimitBucketRepository bucketRepository;
    private DatabaseDistributedRateLimiter limiter;

    @BeforeEach
    void setUp() {
        bucketRepository = Mockito.mock(RateLimitBucketRepository.class);
        limiter = new DatabaseDistributedRateLimiter(bucketRepository, null);
    }

    @Test
    @DisplayName("Tạo bucket mới khi key chưa từng xuất hiện trong database")
    void shouldCreateNewBucketWhenKeyDoesNotExist() {
        String key = "login:ip:10.0.0.1";
        when(bucketRepository.findByIdForUpdate(eq(key))).thenReturn(Optional.empty());

        RateLimitResult result = limiter.tryAcquire(key, 10, 60);

        assertTrue(result.allowed());
        assertEquals(9, result.remaining());
        verify(bucketRepository, atLeastOnce()).saveAndFlush(any(RateLimitBucket.class));
    }

    @Test
    @DisplayName("Tăng biến đếm và trả về 429 khi vượt ngưỡng hạn mức")
    void shouldIncrementCountAndRejectWhenCapacityExceeded() {
        String key = "login:account:sv01";
        LocalDateTime now = LocalDateTime.now();
        RateLimitBucket existingBucket = new RateLimitBucket(key, 5, now.minusSeconds(10), now.plusSeconds(50));

        when(bucketRepository.findByIdForUpdate(eq(key))).thenReturn(Optional.of(existingBucket));

        // Dung lượng tối đa là 5, request này là request thứ 6
        RateLimitResult result = limiter.tryAcquire(key, 5, 60);

        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterSeconds() > 0 && result.retryAfterSeconds() <= 50);
        assertEquals(6, existingBucket.getRequestCount());
        verify(bucketRepository, atLeastOnce()).saveAndFlush(existingBucket);
    }

    @Test
    @DisplayName("Tự động làm mới cửa sổ thời gian khi bucket cũ đã hết hạn")
    void shouldResetWindowWhenBucketExpired() {
        String key = "api:user:admin";
        LocalDateTime now = LocalDateTime.now();
        // Bucket cũ đã quá hạn 10 giây trước
        RateLimitBucket expiredBucket = new RateLimitBucket(key, 100, now.minusSeconds(70), now.minusSeconds(10));

        when(bucketRepository.findByIdForUpdate(eq(key))).thenReturn(Optional.of(expiredBucket));

        RateLimitResult result = limiter.tryAcquire(key, 50, 60);

        assertTrue(result.allowed());
        assertEquals(49, result.remaining());
        assertEquals(1, expiredBucket.getRequestCount());
        assertTrue(expiredBucket.getResetAt().isAfter(now));
        verify(bucketRepository, atLeastOnce()).saveAndFlush(expiredBucket);
    }
}
