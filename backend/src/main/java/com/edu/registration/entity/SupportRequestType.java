package com.edu.registration.entity;

import java.util.Arrays;
import java.util.Optional;

public enum SupportRequestType {
    HOC_PHAN("Hỗ trợ đăng ký / môn học"),
    DIEM_SO("Khiếu nại / phúc khảo điểm"),
    HOC_KY("Học kỳ / kế hoạch học tập"),
    HO_SO_HOC_VU("Hồ sơ học vụ"),
    KHAC("Yêu cầu hỗ trợ khác");

    private final String moTa;

    SupportRequestType(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }

    public static Optional<SupportRequestType> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
