package com.edu.registration.entity;

import java.util.Arrays;
import java.util.Optional;

public enum SupportRequestChangeType {
    KHOI_TAO("Khởi tạo phiếu"),
    TRANG_THAI("Chuyển trạng thái"),
    PHAN_CONG("Gán người xử lý"),
    MUC_UU_TIEN("Thay đổi mức ưu tiên"),
    HAN_XU_LY("Điều chỉnh hạn xử lý"),
    PHAN_LOAI("Phân loại yêu cầu"),
    YEU_CAU_MINH_CHUNG("Yêu cầu bổ sung minh chứng"),
    BO_SUNG_MINH_CHUNG("Bổ sung minh chứng");

    private final String moTa;

    SupportRequestChangeType(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    public static Optional<SupportRequestChangeType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
