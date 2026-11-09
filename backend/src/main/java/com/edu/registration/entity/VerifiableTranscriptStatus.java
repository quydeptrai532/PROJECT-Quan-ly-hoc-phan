package com.edu.registration.entity;

/**
 * Trạng thái hiệu lực của bảng điểm điện tử xác thực (QLSV-99 / TASK-OP-12).
 */
public enum VerifiableTranscriptStatus {
    HIEU_LUC("Hiệu lực"),
    DA_THU_HOI("Đã thu hồi"),
    DA_THAY_THE("Đã thay thế");

    private final String moTa;

    VerifiableTranscriptStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}
