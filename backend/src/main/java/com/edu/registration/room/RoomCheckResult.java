package com.edu.registration.room;
import java.util.List;

/**
 * Kết quả kiểm tra:
 * loi làm từ chối thao tác,
 * canhBao chỉ hiển thị cho người dùng.
 */
public record RoomCheckResult(
        List<RoomIssue> loi,
        List<RoomIssue> canhBao
) {

    public boolean hopLe() {
        return loi.isEmpty();
    }
}
