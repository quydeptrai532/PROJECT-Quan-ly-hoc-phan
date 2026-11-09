package com.edu.registration.entity;

import java.util.Arrays;
import java.util.Optional;

public enum SupportRequestPriority {
    THAP("Thấp", 1),
    TRUNG_BINH("Trung bình", 2),
    CAO("Cao", 3),
    KHAN_CAP("Khẩn cấp", 4);

    private final String moTa;
    private final int capDo;

    SupportRequestPriority(String moTa, int capDo) {
        this.moTa = moTa;
        this.capDo = capDo;
    }

    public String getMoTa() {
        return moTa;
    }

    public int getCapDo() {
        return capDo;
    }

    public static Optional<SupportRequestPriority> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(normalized))
                .findFirst();
    }
}
