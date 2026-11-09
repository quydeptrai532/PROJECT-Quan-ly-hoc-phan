package com.edu.registration.ratelimit;

import com.edu.registration.config.RateLimitingProperties;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ điều phối Rate Limiting trung tâm theo cấu hình (QLSV-94 / TASK-OP-07 Work Package B).
 */
@Service
public class RateLimitService {

    private final RateLimitingProperties properties;
    private final InMemoryRateLimiter inMemoryRateLimiter;
    private final DatabaseDistributedRateLimiter databaseDistributedRateLimiter;

    public RateLimitService(RateLimitingProperties properties,
                            InMemoryRateLimiter inMemoryRateLimiter,
                            DatabaseDistributedRateLimiter databaseDistributedRateLimiter) {
        this.properties = properties;
        this.inMemoryRateLimiter = inMemoryRateLimiter;
        this.databaseDistributedRateLimiter = databaseDistributedRateLimiter;
    }

    public RateLimitResult checkLoginLimit(String clientIp) {
        if (!properties.isEnabled() || properties.getStrategy() == RateLimitingProperties.Strategy.DISABLED) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "login:" + clientIp;
        RateLimitingProperties.EndpointLimit config = properties.getLogin();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    public RateLimitResult checkApiLimit(String clientIdentifier) {
        if (!properties.isEnabled() || properties.getStrategy() == RateLimitingProperties.Strategy.DISABLED) {
            return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE);
        }
        String key = "api:" + clientIdentifier;
        RateLimitingProperties.EndpointLimit config = properties.getApi();
        return getActiveLimiter().tryAcquire(key, config.getCapacity(), config.getWindowSeconds());
    }

    private RateLimiter getActiveLimiter() {
        if (properties.getStrategy() == RateLimitingProperties.Strategy.DATABASE) {
            return databaseDistributedRateLimiter;
        }
        return inMemoryRateLimiter;
    }

    public RateLimitingProperties getProperties() {
        return properties;
    }
}
