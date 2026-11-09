package com.edu.registration.entity;

/**
 * Trạng thái vòng đời và xử lý cảnh báo học tập sớm (QLSV-97 / TASK-OP-10).
 */
public enum AcademicAlertStatus {
    DANG_CANH_BAO("Đang cảnh báo", "Cảnh báo đang mở và cần được theo dõi hoặc can thiệp"),
    DANG_XU_LY("Đang xử lý", "Cán bộ đào tạo hoặc giảng viên đã tiếp nhận và đang hỗ trợ/tư vấn"),
    DA_XU_LY("Đã xử lý", "Đã hoàn thành tư vấn hoặc có giải pháp xử lý ghi chú cụ thể"),
    DA_GIAI_TOA("Đã giải tỏa", "Tự động đóng do dữ liệu nguồn đã thay đổi/cải thiện vượt qua ngưỡng cảnh báo"),
    BO_QUA("Bỏ qua", "Được miễn trừ hoặc bỏ qua có ghi nhận lý do chính đáng");

    private final String tenHienThi;
    private final String moTa;

    AcademicAlertStatus(String tenHienThi, String moTa) {
        this.tenHienThi = tenHienThi;
        this.moTa = moTa;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMoTa() {
        return moTa;
    }

    /** Kiểm tra trạng thái có còn hiệu lực hoạt động hay không */
    public boolean isActive() {
        return this == DANG_CANH_BAO || this == DANG_XU_LY;
    }
}
