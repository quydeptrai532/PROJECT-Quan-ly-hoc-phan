package com.edu.registration.entity;

/**
 * Các loại quan hệ giữa các môn học trong danh mục đào tạo (QLSV-76).
 *
 * Lưu ý nghiệp vụ:
 * - TIEN_QUYET đã có quy tắc chính thức trong README/SRS (phải có điểm tổng kết đã công bố >= ngưỡng đạt trước khi đăng ký).
 * - SONG_HANH, TUONG_DUONG, THAY_THE, NHOM_TU_CHON được lưu cấu hình có cấu trúc đầy đủ và đối chiếu khi kiểm tra đăng ký,
 *   nhưng chưa tự áp đặt quy chế chặn/thay thế tự động khi chủ dự án chưa chốt quy chế.
 */
public enum SubjectRelationType {
    TIEN_QUYET(
            "Tiên quyết",
            true,
            "Sinh viên phải đạt môn tiên quyết (điểm tổng kết đã công bố >= ngưỡng đạt, mặc định 5.0) trước khi đăng ký.",
            null
    ),
    SONG_HANH(
            "Song hành",
            false,
            "Đã lưu cấu hình môn song hành và đối chiếu trạng thái đăng ký cùng kỳ / lịch sử học tập khi kiểm tra đăng ký.",
            "Cần chủ dự án chốt: bắt buộc đăng ký đồng thời trong cùng học kỳ hay chấp nhận môn đã học trước đó (kể cả chưa đạt), và quy tắc khi hủy 1 trong 2 môn song hành."
    ),
    TUONG_DUONG(
            "Tương đương",
            false,
            "Đã lưu cấu hình môn tương đương và đối chiếu kết quả học tập của môn tương đương khi kiểm tra đăng ký.",
            "Cần chủ dự án chốt: phạm vi tự động công nhận thay thế hai chiều khi xét môn tiên quyết/tốt nghiệp, cách quy đổi điểm GPA và có chặn đăng ký khi đã đạt môn tương đương hay không."
    ),
    THAY_THE(
            "Thay thế",
            false,
            "Đã lưu cấu hình môn thay thế (môn mới thay cho môn cũ) và đối chiếu kết quả khi kiểm tra đăng ký.",
            "Cần chủ dự án chốt: chiều áp dụng thay thế một chiều, điều kiện chỉ áp dụng khi môn cũ ngừng mở lớp hay mọi trường hợp, và quy tắc tính điểm học lại/cải thiện."
    ),
    NHOM_TU_CHON(
            "Nhóm tự chọn",
            false,
            "Đã lưu cấu hình liên kết nhóm tự chọn (mã nhóm, số tín chỉ tự chọn yêu cầu) và đối chiếu khi kiểm tra đăng ký.",
            "Cần chủ dự án chốt: có chặn đăng ký vượt số tín chỉ yêu cầu của nhóm tự chọn ngay lúc đăng ký học phần hay chỉ giới hạn trần tín chỉ khi xét tiến độ tốt nghiệp."
    );

    private final String tenHienThi;
    private final boolean quyCheDaChot;
    private final String moTaQuyCheHienHanh;
    private final String phanCanChuDuAnChot;

    SubjectRelationType(String tenHienThi, boolean quyCheDaChot, String moTaQuyCheHienHanh, String phanCanChuDuAnChot) {
        this.tenHienThi = tenHienThi;
        this.quyCheDaChot = quyCheDaChot;
        this.moTaQuyCheHienHanh = moTaQuyCheHienHanh;
        this.phanCanChuDuAnChot = phanCanChuDuAnChot;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public boolean isQuyCheDaChot() {
        return quyCheDaChot;
    }

    public String getMoTaQuyCheHienHanh() {
        return moTaQuyCheHienHanh;
    }

    public String getPhanCanChuDuAnChot() {
        return phanCanChuDuAnChot;
    }
}
