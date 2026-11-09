package com.edu.registration.ratelimit;

/**
 * Giao diện trừu tượng cho cơ chế Rate Limiter (QLSV-94 / TASK-OP-07 Work Package B).
 */
public interface RateLimiter {

    /**
     * Kiểm tra và tiêu thụ 1 token cho key xác định.
     *
     * @param key           Khóa định danh (vd: login:ip hoặc api:user:username)
     * @param capacity      Số lượng request tối đa trong cửa sổ thời gian
     * @param windowSeconds Thời gian cửa sổ (giây)
     * @return Kết quả kiểm tra (allowed, limit, remaining, retryAfterSeconds)
     */
    RateLimitResult tryAcquire(String key, long capacity, long windowSeconds);
}
