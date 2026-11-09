package com.edu.registration.room;
/**
 * Một lớp khác đang dùng cùng phòng trong cùng học kỳ.
 */
public record RoomSlot(
        Long lopId,
        String maLop,
        int thu,
        int tietBatDau,
        int soTiet
) {

    public int tietKetThuc() {
        return tietBatDau + soTiet - 1;
    }
}
