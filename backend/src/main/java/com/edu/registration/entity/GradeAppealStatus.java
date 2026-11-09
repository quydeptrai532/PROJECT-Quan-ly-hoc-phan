package com.edu.registration.entity;

import java.util.Arrays;
import java.util.Optional;

public enum GradeAppealStatus {
    MOI_TAO("Mới tiếp nhận / Chờ xử lý"),
    DANG_XU_LY("Đang xử lý / Chấm lại"),
    CHAP_THUAN("Chấp thuận điều chỉnh điểm"),
    TU_CHOI("Từ chối / Giữ nguyên điểm"),
    DA_HUY("Đã hủy");

    private final String moTa;

    GradeAppealStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    public boolean isDangXuLyHoacCho() {
        return this == MOI_TAO || this == DANG_XU_LY;
    }

    public boolean isDaKetThuc() {
        return this == CHAP_THUAN || this == TU_CHOI || this == DA_HUY;
    }

    public static Optional<GradeAppealStatus> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
