package com.edu.registration.room;
import java.util.Set;

/**
 * Thông tin phòng học cần cho việc kiểm tra
 * (lấy từ danh mục phòng).
 */
public record RoomFacts(
        boolean hoatDong,
        int sucChua,
        String loai,
        Set<String> thietBi
) {
}
