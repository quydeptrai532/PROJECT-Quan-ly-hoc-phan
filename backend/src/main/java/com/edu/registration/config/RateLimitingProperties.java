package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tham số giới hạn tần suất yêu cầu (Rate Limiting) (QLSV-94 / TASK-OP-07 Work Package B).
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingProperties {

    public enum Strategy {
        IN_MEMORY,
        DATABASE,
        DISABLED
    }

    private boolean enabled = true;
    private Strategy strategy = Strategy.IN_MEMORY;
    private EndpointLimit login = new EndpointLimit(10, 60);
    private EndpointLimit api = new EndpointLimit(120, 60);
    private EndpointLimit verify = new EndpointLimit(20, 60);

    public static class EndpointLimit {
        private long capacity;
        private long windowSeconds;

        public EndpointLimit() {
        }

        public EndpointLimit(long capacity, long windowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public EndpointLimit getLogin() {
        return login;
    }

    public void setLogin(EndpointLimit login) {
        this.login = login;
    }

    public EndpointLimit getApi() {
        return api;
    }

    public EndpointLimit getVerify() {
        return verify;
    }

    public void setVerify(EndpointLimit verify) {
        this.verify = verify;
    }

    public void setApi(EndpointLimit api) {
        this.api = api;
    }
}
