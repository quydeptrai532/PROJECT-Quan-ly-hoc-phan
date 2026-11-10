package com.edu.registration.room;
import java.util.Set;

/**
 * Thông tin lớp học phần đang được gán phòng;
 * loaiYeuCau và thietBiYeuCau có thể rỗng.
 */
public record ClassFacts(
        int siSoToiDa,
        String loaiYeuCau,
        Set<String> thietBiYeuCau,
        int thu,
        int tietBatDau,
        int soTiet
) {
}
