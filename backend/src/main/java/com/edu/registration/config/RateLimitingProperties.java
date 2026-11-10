package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình tham số giới hạn tần suất yêu cầu (Rate Limiting) (QLSV-102 / TASK-OP-07B).
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

    /**
     * Danh sách địa chỉ IP hoặc dải mạng CIDR của Proxy tin cậy (Nginx, Traefik, Docker gateway, AWS ALB...).
     * Chỉ khi request đến từ các proxy này thì các header X-Forwarded-For / X-Real-IP mới được xem xét.
     */
    private List<String> trustedProxies = new ArrayList<>(List.of(
            "127.0.0.1",
            "::1",
            "0:0:0:0:0:0:0:1",
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16"
    ));

    private LoginLimits login = new LoginLimits();
    private EndpointLimit api = new EndpointLimit(120, 60);
    private EndpointLimit apiAnonymous = new EndpointLimit(60, 60);
    private EndpointLimit verify = new EndpointLimit(20, 60);
    private EndpointLimit chatbot = new EndpointLimit(20, 60);
    private EndpointLimit heavy = new EndpointLimit(15, 60);

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

    /**
     * Cấu hình phân cấp cho điểm cuối đăng nhập (/api/auth/login):
     * - ip: Ngưỡng tối đa từ 1 IP (chống tràn lụt DoS, ngưỡng đủ rộng để cho phép IP mạng dùng chung/ký túc xá).
     * - account: Ngưỡng tối đa thử đăng nhập trên 1 tài khoản (chống brute-force mật khẩu).
     */
    public static class LoginLimits extends EndpointLimit {
        private EndpointLimit ip = new EndpointLimit(60, 60);
        private EndpointLimit account = new EndpointLimit(5, 60);

        public LoginLimits() {
            super(60, 60);
        }

        public LoginLimits(long capacity, long windowSeconds) {
            super(capacity, windowSeconds);
            this.ip = new EndpointLimit(capacity, windowSeconds);
            this.account = new EndpointLimit(5, windowSeconds);
        }

        public EndpointLimit getIp() {
            return ip;
        }

        public void setIp(EndpointLimit ip) {
            this.ip = ip;
        }

        public EndpointLimit getAccount() {
            return account;
        }

        public void setAccount(EndpointLimit account) {
            this.account = account;
        }

        @Override
        public long getCapacity() {
            return ip != null ? ip.getCapacity() : super.getCapacity();
        }

        @Override
        public void setCapacity(long capacity) {
            super.setCapacity(capacity);
            if (ip != null) {
                ip.setCapacity(capacity);
            }
        }

        @Override
        public long getWindowSeconds() {
            return ip != null ? ip.getWindowSeconds() : super.getWindowSeconds();
        }

        @Override
        public void setWindowSeconds(long windowSeconds) {
            super.setWindowSeconds(windowSeconds);
            if (ip != null) {
                ip.setWindowSeconds(windowSeconds);
            }
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

    public List<String> getTrustedProxies() {
        return trustedProxies;
    }

    public void setTrustedProxies(List<String> trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public LoginLimits getLogin() {
        return login;
    }

    public void setLogin(LoginLimits login) {
        this.login = login;
    }

    public void setLogin(EndpointLimit limit) {
        if (limit instanceof LoginLimits ll) {
            this.login = ll;
        } else if (limit != null) {
            this.login = new LoginLimits(limit.getCapacity(), limit.getWindowSeconds());
        }
    }

    public EndpointLimit getApi() {
        return api;
    }

    public void setApi(EndpointLimit api) {
        this.api = api;
    }

    public EndpointLimit getApiAnonymous() {
        return apiAnonymous;
    }

    public void setApiAnonymous(EndpointLimit apiAnonymous) {
        this.apiAnonymous = apiAnonymous;
    }

    public EndpointLimit getVerify() {
        return verify;
    }

    public void setVerify(EndpointLimit verify) {
        this.verify = verify;
    }

    public EndpointLimit getChatbot() {
        return chatbot;
    }

    public void setChatbot(EndpointLimit chatbot) {
        this.chatbot = chatbot;
    }

    public EndpointLimit getHeavy() {
        return heavy;
    }

    public void setHeavy(EndpointLimit heavy) {
        this.heavy = heavy;
    }
}
