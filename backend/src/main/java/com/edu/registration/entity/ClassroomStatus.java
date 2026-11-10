package com.edu.registration.entity;

public enum ClassroomStatus {
    HOAT_DONG("Hoạt động"),
    BAO_TRI("Bảo trì"),
    NGUNG_SU_DUNG("Ngừng sử dụng");

    private final String tenHienThi;

    ClassroomStatus(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }
}
