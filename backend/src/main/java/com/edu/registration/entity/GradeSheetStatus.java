package com.edu.registration.entity;

public enum GradeSheetStatus {
    NHAP("Nháp"),
    CHO_DUYET("Chờ duyệt"),
    DA_DUYET("Đã duyệt"),
    DA_CONG_BO("Đã công bố");

    private final String moTa;

    GradeSheetStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
