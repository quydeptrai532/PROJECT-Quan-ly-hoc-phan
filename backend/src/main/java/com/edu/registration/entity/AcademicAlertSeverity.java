package com.edu.registration.entity;

/**
 * Mức độ nghiêm trọng của cảnh báo học tập sớm (QLSV-97 / TASK-OP-10).
 */
public enum AcademicAlertSeverity {
    THAP("Thấp", "Nhắc nhở nhẹ, cần chú ý"),
    TRUNG_BINH("Trung bình", "Cảnh báo cần theo dõi thường xuyên"),
    CANH_BAO("Cảnh báo", "Cảnh báo học tập cần lưu ý và hỗ trợ"),
    CAO("Cao", "Cảnh báo nguy cơ ảnh hưởng trực tiếp đến kết quả"),
    NGHIEM_TRONG("Nghiêm trọng", "Nguy cơ trượt học phần, cấm thi hoặc chậm tiến độ nghiêm trọng");

    private final String tenHienThi;
    private final String moTa;

    AcademicAlertSeverity(String tenHienThi, String moTa) {
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
