package com.edu.registration.entity;

public enum AlertThresholdType {
    SO_BUOI_VANG("Số buổi vắng"),
    TY_LE_VANG("Tỷ lệ vắng");

    private final String moTa;

    AlertThresholdType(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
