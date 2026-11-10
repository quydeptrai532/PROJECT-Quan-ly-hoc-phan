package com.edu.registration.entity;

public enum StudentAcademicStatus {
    DANG_HOC("Đang học"),
    BAO_LUU("Bảo lưu"),
    THOI_HOC("Thôi học"),
    TOT_NGHIEP("Tốt nghiệp");

    private final String moTa;

    StudentAcademicStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
