package com.edu.registration.entity;

/**
 * Các nhóm luật cảnh báo học tập sớm có thể giải thích được (QLSV-97 / TASK-OP-10).
 */
public enum AcademicAlertRuleType {
    DIEM_QUA_TRINH("Điểm quá trình thấp hơn ngưỡng", "Cảnh báo nguy cơ không đạt điều kiện học phần do điểm quá trình thấp"),
    DIEM_DANH("Số buổi hoặc tỷ lệ vắng vượt ngưỡng", "Cảnh báo nguy cơ cấm thi do vắng học quá quy định"),
    GPA_GIAM("GPA giảm qua các học kỳ", "Cảnh báo xu hướng suy giảm học lực qua các kỳ liên tiếp"),
    TIEN_DO_TIN_CHI("Thiếu tín chỉ so với tiến độ", "Cảnh báo tiến độ tích lũy tín chỉ chậm so với lộ trình đào tạo chuẩn");

    private final String tenHienThi;
    private final String moTa;

    AcademicAlertRuleType(String tenHienThi, String moTa) {
        this.tenHienThi = tenHienThi;
        this.moTa = moTa;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMoTa() {
        return moTa;
    }
}
