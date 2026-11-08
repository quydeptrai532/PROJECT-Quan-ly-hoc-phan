package com.edu.registration.entity;

public enum AlertStatus {
    DANG_CANH_BAO("Đang cảnh báo"),
    DA_GIAI_TOA("Đã giải tỏa");

    private final String moTa;

    AlertStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
