package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình chính sách kiểm tra và xử lý xung đột lịch giảng viên / phòng học khi mở lớp (QLSV-78 - TASK-AD-03).
 * Tách biệt logic kiểm tra và chính sách enforcement để có thể cấu hình linh hoạt.
 */
@Configuration
@ConfigurationProperties(prefix = "app.class-schedule-conflict")
public class ClassScheduleConflictProperties {

    public enum EnforcementPolicy {
        /** Chặn lưu hoàn toàn khi phát hiện xung đột lịch giảng viên hoặc phòng học. */
        BLOCK_ON_CONFLICT,

        /** Cho phép ghi đè lưu nếu có cờ xác nhận chủ động từ quản trị viên. */
        ALLOW_OVERRIDE_WITH_CONFIRMATION,

        /** Chỉ cảnh báo danh sách xung đột, vẫn cho phép lưu dữ liệu. */
        WARN_ONLY
    }

    private EnforcementPolicy enforcementPolicy = EnforcementPolicy.BLOCK_ON_CONFLICT;

    private boolean officialRegulation = false;

    private String policyNote = "Chính sách chặn lưu hay cho phép cảnh báo/ghi đè khi phát hiện trùng lịch giảng viên hoặc phòng học chưa được Chủ dự án chốt chính thức. Hệ thống hiện đang áp dụng chính sách chặn lưu (BLOCK_ON_CONFLICT) để đảm bảo không vi phạm quy chế thời khóa biểu.";

    public EnforcementPolicy getEnforcementPolicy() {
        return enforcementPolicy;
    }

    public void setEnforcementPolicy(EnforcementPolicy enforcementPolicy) {
        this.enforcementPolicy = enforcementPolicy;
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
