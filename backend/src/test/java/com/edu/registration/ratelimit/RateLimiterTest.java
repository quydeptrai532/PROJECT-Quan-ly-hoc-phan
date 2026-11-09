package com.edu.registration.ratelimit;

import com.edu.registration.config.RateLimitingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    private InMemoryRateLimiter inMemoryRateLimiter;
    private RateLimitingProperties properties;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        inMemoryRateLimiter = new InMemoryRateLimiter();
        properties = new RateLimitingProperties();
        properties.setEnabled(true);
        properties.setStrategy(RateLimitingProperties.Strategy.IN_MEMORY);
        properties.setLogin(new RateLimitingProperties.EndpointLimit(3, 60));
        properties.setApi(new RateLimitingProperties.EndpointLimit(5, 60));

        rateLimitService = new RateLimitService(properties, inMemoryRateLimiter, null);
    }

    @Test
    void shouldAllowRequestsWithinCapacity() {
        String clientIp = "192.168.1.100";

        RateLimitResult r1 = rateLimitService.checkLoginLimit(clientIp);
        assertTrue(r1.allowed());
        assertEquals(2, r1.remaining());

        RateLimitResult r2 = rateLimitService.checkLoginLimit(clientIp);
        assertTrue(r2.allowed());
        assertEquals(1, r2.remaining());

        RateLimitResult r3 = rateLimitService.checkLoginLimit(clientIp);
        assertTrue(r3.allowed());
        assertEquals(0, r3.remaining());
    }

    @Test
    void shouldRejectWhenExceedingCapacityAndProvideRetryAfter() {
        String clientIp = "192.168.1.200";

        rateLimitService.checkLoginLimit(clientIp); // 1
        rateLimitService.checkLoginLimit(clientIp); // 2
        rateLimitService.checkLoginLimit(clientIp); // 3

        RateLimitResult r4 = rateLimitService.checkLoginLimit(clientIp);
        assertFalse(r4.allowed());
        assertEquals(0, r4.remaining());
        assertTrue(r4.retryAfterSeconds() > 0 && r4.retryAfterSeconds() <= 60);
    }

    @Test
    void shouldIsolateLimitsByClientIp() {
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        rateLimitService.checkLoginLimit(ip1);
        rateLimitService.checkLoginLimit(ip1);
        rateLimitService.checkLoginLimit(ip1);

        assertFalse(rateLimitService.checkLoginLimit(ip1).allowed());
        assertTrue(rateLimitService.checkLoginLimit(ip2).allowed());
    }

    @Test
    void shouldAllowAllWhenDisabled() {
        properties.setEnabled(false);
        String clientIp = "1.2.3.4";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.checkLoginLimit(clientIp).allowed());
        }
    }
}
