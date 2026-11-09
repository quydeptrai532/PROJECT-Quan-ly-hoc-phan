package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Cấu hình tập trung cho hệ thống tác vụ nền và retry (QLSV-93 / TASK-OP-06).
 */
@ConfigurationProperties(prefix = "app.background-jobs")
public class BackgroundJobProperties {

    /**
     * Bật hoặc tắt worker xử lý tác vụ nền tự động.
     */
    private boolean enabled = true;

    /**
     * Định danh worker hiện tại (nếu để trống sẽ tự sinh theo hostname + UUID).
     */
    private String workerId;

    /**
     * Chu kỳ quét tác vụ pending (mili-giây). Mặc định 5000ms.
     */
    private long pollIntervalMs = 5000L;

    /**
     * Số lần retry mặc định khi tác vụ gặp lỗi ngoại lệ.
     */
    private int defaultMaxRetries = 3;

    /**
     * Khoảng thời gian backoff cơ sở (giây).
     */
    private int defaultBackoffSeconds = 30;

    /**
     * Hệ số nhân exponential backoff.
     */
    private double backoffMultiplier = 2.0;

    /**
     * Thời gian backoff tối đa (giây).
     */
    private int maxBackoffSeconds = 3600;

    /**
     * Thời hạn lease khóa thực thi tác vụ (giây). Quá hạn này tác vụ RUNNING được coi là bị kẹt/treo do worker crash.
     */
    private int leaseTimeoutSeconds = 300;

    /**
     * Số lượng tác vụ tối đa được quét mỗi đợt.
     */
    private int batchSize = 10;

    /**
     * Múi giờ chuẩn hóa cho toàn bộ tác vụ hẹn giờ và ghi nhận mốc thời gian.
     */
    private String timezone = "Asia/Ho_Chi_Minh";

    /**
     * Số ngày lưu trữ các tác vụ đã hoàn tất (SUCCEEDED, FAILED) trước khi dọn dẹp.
     */
    private int retentionDays = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWorkerId() {
        if (workerId == null || workerId.isBlank()) {
            try {
                String host = InetAddress.getLocalHost().getHostName();
                this.workerId = host + "-" + UUID.randomUUID().toString().substring(0, 8);
            } catch (Exception ex) {
                this.workerId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getDefaultMaxRetries() {
        return defaultMaxRetries;
    }

    public void setDefaultMaxRetries(int defaultMaxRetries) {
        this.defaultMaxRetries = defaultMaxRetries;
    }

    public int getDefaultBackoffSeconds() {
        return defaultBackoffSeconds;
    }

    public void setDefaultBackoffSeconds(int defaultBackoffSeconds) {
        this.defaultBackoffSeconds = defaultBackoffSeconds;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public int getMaxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(int maxBackoffSeconds) {
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public int getLeaseTimeoutSeconds() {
        return leaseTimeoutSeconds;
    }

    public void setLeaseTimeoutSeconds(int leaseTimeoutSeconds) {
        this.leaseTimeoutSeconds = leaseTimeoutSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ex) {
            return ZoneId.of("Asia/Ho_Chi_Minh");
        }
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
