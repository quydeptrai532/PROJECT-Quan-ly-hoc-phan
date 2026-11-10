package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tập trung cho kênh email thông báo (QLSV-92 / TASK-OP-05).
 * Email là kênh thông báo bổ sung cho Notification Center trong ứng dụng.
 */
@Component
@ConfigurationProperties(prefix = "app.notification.email")
public class EmailProperties {

    /**
     * Bật/tắt kênh email. Khi false, hệ thống bỏ qua gửi email, thông báo in-app vẫn hoạt động bình thường.
     */
    private boolean enabled = false;

    /**
     * Địa chỉ người gửi mặc định.
     */
    private String fromAddress = "no-reply@university.edu.vn";

    /**
     * Tên người gửi hiển thị.
     */
    private String fromName = "Hệ thống Quản lý Đào tạo";

    /**
     * Số lần thử lại tối đa khi gửi thất bại.
     */
    private int maxRetries = 3;

    /**
     * Khoảng thời gian chờ (giây) trước lần thử lại kế tiếp.
     */
    private int retryDelaySeconds = 60;

    /**
     * Tên nhà cung cấp (smtp, mock, log, none).
     */
    private String provider = "smtp";

    /**
     * Cấu hình SMTP
     */
    private String host = "";
    private int port = 587;
    private String username = "";
    private String password = "";
    private boolean auth = true;
    private boolean starttls = true;

    /**
     * Ghi chú chính sách bảo mật email.
     */
    private String policyNote = "Email là kênh thông báo bổ sung được kiểm soát bật/tắt độc lập. Lỗi gửi email không ảnh hưởng giao dịch nghiệp vụ. Mọi thông tin nhạy cảm và điểm số cá nhân không được gửi qua email mà phải đăng nhập tra cứu qua kênh in-app đã xác thực.";

    /**
     * Kiểm tra xem thông tin cấu hình nhà cung cấp email đã đầy đủ để thực hiện gửi hay chưa.
     */
    public boolean isConfigured() {
        if (host == null || host.isBlank()) {
            return false;
        }
        if (auth && (username == null || username.isBlank() || password == null || password.isBlank())) {
            return false;
        }
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public boolean isStarttls() {
        return starttls;
    }

    public void setStarttls(boolean starttls) {
        this.starttls = starttls;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public void setPolicyNote(String policyNote) {
        this.policyNote = policyNote;
    }
}
