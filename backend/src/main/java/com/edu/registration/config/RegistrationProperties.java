package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    public static final String DEFAULT_TIMEZONE_ID = "Asia/Ho_Chi_Minh";

    private String timezone = DEFAULT_TIMEZONE_ID;
    private String multiCohortPriorityPolicy = "HIGHEST_PRIORITY";
    private boolean officialRegulation = false;
    private String policyNote = "Chính sách thứ tự ưu tiên khi sinh viên thuộc nhiều nhóm đối tượng/đợt đăng ký khác nhau đang áp dụng cấu hình dự kiến (HIGHEST_PRIORITY - đợt có thứ tự ưu tiên cao nhất hoặc đợt đang mở) và chưa có quy chế chính thức được Chủ dự án / Phòng Đào tạo phê duyệt.";

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timezone != null && !timezone.isBlank() ? timezone.trim() : DEFAULT_TIMEZONE_ID);
        } catch (RuntimeException ex) {
            return ZoneId.of(DEFAULT_TIMEZONE_ID);
        }
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getMultiCohortPriorityPolicy() {
        return multiCohortPriorityPolicy;
    }

    public void setMultiCohortPriorityPolicy(String multiCohortPriorityPolicy) {
        this.multiCohortPriorityPolicy = multiCohortPriorityPolicy;
    }

    public boolean isOfficialRegulation() {
        return officialRegulation;
    }

    public void setOfficialRegulation(boolean officialRegulation) {
        this.officialRegulation = officialRegulation;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public void setPolicyNote(String policyNote) {
        this.policyNote = policyNote;
    }
}
