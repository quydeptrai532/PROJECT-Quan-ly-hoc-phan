package com.edu.registration.entity;

public enum AttendanceStatus {
    CO_MAT("Có mặt"),
    VANG("Vắng"),
    MUON("Muộn"),
    CO_PHEP("Có phép");

    private final String moTa;

    AttendanceStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
