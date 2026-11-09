package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.grade-appeal")
public class GradeAppealProperties {

    /**
     * Thời hạn tiếp nhận hồ sơ phúc khảo (số ngày kể từ thời điểm công bố bảng điểm).
     * Mặc định dự kiến 15 ngày; nếu <= 0 thì không giới hạn.
     */
    private int submissionWindowDays = 15;

    /**
     * Chỉ cho phép nộp phúc khảo khi bảng điểm đã công bố (DA_CONG_BO hoặc DA_KHOA).
     */
    private boolean requirePublishedGrade = true;

    /**
     * Tự động cập nhật điểm khi duyệt chấp thuận hồ sơ qua luồng điều chỉnh có audit.
     */
    private boolean autoApplyGradeChange = true;

    /**
     * Cờ đánh dấu đã có quy chế chính thức được phê duyệt hay chưa.
     */
    private boolean officialRegulation = false;

    /**
     * Ghi chú quy chế và SLA chưa chốt chính thức (cần Phòng Đào tạo / Nhà trường phê duyệt).
     */
    private String slaNote = "Thời hạn tiếp nhận hồ sơ phúc khảo (dự kiến 15 ngày kể từ ngày công bố điểm) và quy trình xử lý SLA đang áp dụng theo cấu hình tập trung dự kiến, chưa phải quy chế chính thức của Nhà trường và chờ Chủ dự án / Phòng Đào tạo phê duyệt.";

    public int getSubmissionWindowDays() {
        return submissionWindowDays;
    }

    public void setSubmissionWindowDays(int submissionWindowDays) {
        this.submissionWindowDays = submissionWindowDays;
    }

    public boolean isRequirePublishedGrade() {
        return requirePublishedGrade;
    }

    public void setRequirePublishedGrade(boolean requirePublishedGrade) {
        this.requirePublishedGrade = requirePublishedGrade;
    }

    public boolean isAutoApplyGradeChange() {
        return autoApplyGradeChange;
    }

    public void setAutoApplyGradeChange(boolean autoApplyGradeChange) {
        this.autoApplyGradeChange = autoApplyGradeChange;
    }

    public boolean isOfficialRegulation() {
        return officialRegulation;
    }

    public void setOfficialRegulation(boolean officialRegulation) {
        this.officialRegulation = officialRegulation;
    }

    public String getSlaNote() {
        return slaNote;
    }

    public void setSlaNote(String slaNote) {
        this.slaNote = slaNote;
    }
}
