package com.edu.registration.room;
/**
 * Chính sách cho từng điều kiện kiểm tra khi gán phòng cho lớp.
 */
public record RoomPolicy(
        PolicyMode hoatDong,
        PolicyMode trungLich,
        PolicyMode sucChua,
        PolicyMode loaiPhong,
        PolicyMode thietBi
) {

    /**
     * Mặc định đề xuất:
     * phòng hoạt động và trùng lịch là bắt buộc,
     * còn lại là cảnh báo.
     */
    public static RoomPolicy macDinh() {
        return new RoomPolicy(
                PolicyMode.BAT_BUOC,
                PolicyMode.BAT_BUOC,
                PolicyMode.CANH_BAO,
                PolicyMode.CANH_BAO,
                PolicyMode.CANH_BAO
        );
    }
}
