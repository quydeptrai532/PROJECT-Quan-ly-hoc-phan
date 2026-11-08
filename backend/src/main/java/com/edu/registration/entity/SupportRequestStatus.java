package com.edu.registration.entity;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum SupportRequestStatus {
    MOI_TAO("Mới tiếp nhận"),
    DANG_XU_LY("Đang xử lý"),
    CHO_PHAN_HOI_SINH_VIEN("Chờ sinh viên phản hồi"),
    DA_GIAI_QUYET("Đã giải quyết"),
    TU_CHOI("Từ chối"),
    DA_DONG("Đã đóng");

    private final String moTa;

    SupportRequestStatus(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    public Set<SupportRequestStatus> trangThaiTiepTheoHopLe() {
        return switch (this) {
            case MOI_TAO -> EnumSet.of(DANG_XU_LY, CHO_PHAN_HOI_SINH_VIEN, DA_GIAI_QUYET, TU_CHOI, DA_DONG);
            case DANG_XU_LY -> EnumSet.of(CHO_PHAN_HOI_SINH_VIEN, DA_GIAI_QUYET, TU_CHOI, DA_DONG);
            case CHO_PHAN_HOI_SINH_VIEN -> EnumSet.of(DANG_XU_LY, DA_GIAI_QUYET, TU_CHOI, DA_DONG);
            case DA_GIAI_QUYET, TU_CHOI -> EnumSet.of(DANG_XU_LY, DA_DONG);
            case DA_DONG -> EnumSet.noneOf(SupportRequestStatus.class);
        };
    }

    public boolean coTheChuyenSang(SupportRequestStatus dich) {
        return dich != null && trangThaiTiepTheoHopLe().contains(dich);
    }

    public static Optional<SupportRequestStatus> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
