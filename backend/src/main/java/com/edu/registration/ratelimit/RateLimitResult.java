package com.edu.registration.ratelimit;

/**
 * Kết quả kiểm tra rate limit cho mỗi yêu cầu (QLSV-94 / TASK-OP-07 Work Package B).
 */
public record RateLimitResult(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds
) {
    public static RateLimitResult allowed(long limit, long remaining) {
        return new RateLimitResult(true, limit, remaining, 0);
    }

    public static RateLimitResult rejected(long limit, long retryAfterSeconds) {
        return new RateLimitResult(false, limit, 0, Math.max(1, retryAfterSeconds));
    }
}
