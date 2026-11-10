package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cấu hình tập trung cho tính năng Bảng điểm điện tử xác thực (QLSV-99 / TASK-OP-12).
 */
@Component
@ConfigurationProperties(prefix = "app.verifiable-transcript")
public class VerifiableTranscriptConfigProperties {

    private String verificationBaseUrl = "https://qldt.university.edu.vn";
    private boolean requireHttps = true;
    private String publicDisclosurePolicy = "MINIMAL_WITHOUT_GRADES";
    private String policyNote = "Chính sách công khai thông tin xác thực bảng điểm điện tử chỉ công bố thông tin nhận diện cơ bản và tổng kết học vụ đã được phê duyệt, không hiển thị điểm chi tiết từng môn học khi chưa có quy chế/ủy quyền chính thức.";

    public String getVerificationBaseUrl() {
        return verificationBaseUrl;
    }

    public void setVerificationBaseUrl(String verificationBaseUrl) {
        this.verificationBaseUrl = verificationBaseUrl;
    }

    public boolean isRequireHttps() {
        return requireHttps;
    }

    public void setRequireHttps(boolean requireHttps) {
        this.requireHttps = requireHttps;
    }

    public String getPublicDisclosurePolicy() {
        return publicDisclosurePolicy;
    }

    public void setPublicDisclosurePolicy(String publicDisclosurePolicy) {
        this.publicDisclosurePolicy = publicDisclosurePolicy;
    }

    public String getPolicyNote() {
        return policyNote;
    }

    public void setPolicyNote(String policyNote) {
        this.policyNote = policyNote;
    }
}
