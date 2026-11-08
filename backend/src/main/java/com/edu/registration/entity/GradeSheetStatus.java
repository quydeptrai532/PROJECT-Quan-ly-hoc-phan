package com.edu.registration.entity;

public enum GradeSheetStatus {
    NHAP("Nháp"),
    CHO_DUYET("Chờ duyệt"),
    TRA_VE("Trả về chỉnh sửa"),
    DA_DUYET("Đã duyệt"),
    DA_CONG_BO("Đã công bố"),
    DA_KHOA("Đã khóa");

    private final String moTa;

    GradeSheetStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    /**
     * Chỉ cho phép nhập/sửa/import điểm khi bảng điểm ở trạng thái bản nháp (NHAP)
     * hoặc bị Phòng Đào tạo trả về yêu cầu chỉnh sửa (TRA_VE).
     */
    public boolean isChoPhepSuaDiem() {
        return this == NHAP || this == TRA_VE;
    }

    /**
     * Bảng điểm được coi là đã công bố cho sinh viên khi ở trạng thái DA_CONG_BO hoặc DA_KHOA.
     */
    public boolean isDaCongBo() {
        return this == DA_CONG_BO || this == DA_KHOA;
    }

    /**
     * Bảng điểm đã khóa hoàn toàn ở bước cuối (DA_CONG_BO hoặc DA_KHOA); cần quyền mở khóa đặc biệt kèm lý do bắt buộc.
     */
    public boolean isDaKhoa() {
        return this == DA_CONG_BO || this == DA_KHOA;
    }
}
