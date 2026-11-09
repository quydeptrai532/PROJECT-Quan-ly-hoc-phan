package com.edu.registration.room;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomAssignmentCheckerTest {

    private static final RoomPolicy MAC_DINH = RoomPolicy.macDinh();

    private static RoomFacts phong(
            boolean hoatDong,
            int sucChua,
            String loai,
            String... thietBi
    ) {
        return new RoomFacts(
                hoatDong,
                sucChua,
                loai,
                Set.of(thietBi)
        );
    }

    private static ClassFacts lop(
            int siSo,
            String loai,
            Set<String> thietBi,
            int thu,
            int batDau,
            int soTiet
    ) {
        return new ClassFacts(
                siSo,
                loai,
                thietBi,
                thu,
                batDau,
                soTiet
        );
    }

    private static ClassFacts lopThuong(int siSo) {
        return lop(siSo, null, Set.of(), 2, 1, 3);
    }

    @Test
    void sucChuaBangSiSoDuocChapNhan() {

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 60, "LT"),
                lopThuong(60),
                List.of(),
                MAC_DINH
        );

        assertTrue(ketQua.hopLe());
        assertTrue(ketQua.canhBao().isEmpty());
    }

    @Test
    void sucChuaNhoHonSiSoBiCanhBaoTheoChinhSachMacDinh() {

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 59, "LT"),
                lopThuong(60),
                List.of(),
                MAC_DINH
        );

        assertTrue(ketQua.hopLe());
        assertEquals(1, ketQua.canhBao().size());
        assertEquals(
                "SUC_CHUA_KHONG_DU",
                ketQua.canhBao().get(0).code()
        );
    }

    @Test
    void sucChuaNhoHonSiSoBiTuChoiKhiLaBatBuoc() {

        RoomPolicy nghiemNgat = new RoomPolicy(
                PolicyMode.BAT_BUOC,
                PolicyMode.BAT_BUOC,
                PolicyMode.BAT_BUOC,
                PolicyMode.CANH_BAO,
                PolicyMode.CANH_BAO
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 59, "LT"),
                lopThuong(60),
                List.of(),
                nghiemNgat
        );

        assertFalse(ketQua.hopLe());
        assertEquals(
                "SUC_CHUA_KHONG_DU",
                ketQua.loi().get(0).code()
        );
        assertTrue(ketQua.canhBao().isEmpty());
    }

    @Test
    void sucChuaTatThiKhongKiemTra() {

        RoomPolicy tat = new RoomPolicy(
                PolicyMode.BAT_BUOC,
                PolicyMode.BAT_BUOC,
                PolicyMode.TAT,
                PolicyMode.CANH_BAO,
                PolicyMode.CANH_BAO
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 10, "LT"),
                lopThuong(60),
                List.of(),
                tat
        );

        assertTrue(ketQua.loi().isEmpty());
        assertTrue(ketQua.canhBao().isEmpty());
    }

    @Test
    void phongNgungHoatDongBiTuChoi() {

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(false, 100, "LT"),
                lopThuong(30),
                List.of(),
                MAC_DINH
        );

        assertFalse(ketQua.hopLe());
        assertEquals(
                "PHONG_NGUNG_HOAT_DONG",
                ketQua.loi().get(0).code()
        );
    }

    @Test
    void trungLichCungThuVaGiaoTietBiTuChoi() {

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "INT101.01", 2, 3, 3)
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 100, "LT"),
                lopThuong(30),
                khac,
                MAC_DINH
        );

        assertFalse(ketQua.hopLe());
        assertEquals(
                "TRUNG_LICH_PHONG",
                ketQua.loi().get(0).code()
        );
        assertTrue(
                ketQua.loi().get(0).message().contains("INT101.01")
        );
    }

    @Test
    void lopLienKeKhongGiaoTietKhongBiTrung() {

        // Lớp mới dùng tiết 1-3,
        // lớp khác dùng tiết 4-6 cùng thứ: không giao nhau.

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "INT101.01", 2, 4, 3)
        );

        assertTrue(
                RoomAssignmentChecker.kiemTra(
                        phong(true, 100, "LT"),
                        lopThuong(30),
                        khac,
                        MAC_DINH
                ).hopLe()
        );
    }

    @Test
    void chiChamMotTietCuoiVanLaTrung() {

        // Lớp mới tiết 1-3,
        // lớp khác tiết 3-5: giao nhau ở tiết 3.

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "INT101.01", 2, 3, 3)
        );

        assertFalse(
                RoomAssignmentChecker.kiemTra(
                        phong(true, 100, "LT"),
                        lopThuong(30),
                        khac,
                        MAC_DINH
                ).hopLe()
        );
    }

    @Test
    void khacThuKhongBiTrung() {

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "INT101.01", 3, 1, 3)
        );

        assertTrue(
                RoomAssignmentChecker.kiemTra(
                        phong(true, 100, "LT"),
                        lopThuong(30),
                        khac,
                        MAC_DINH
                ).hopLe()
        );
    }

    @Test
    void nhieuLopTrungThiMoiLopMotLoi() {

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "A", 2, 1, 2),
                new RoomSlot(6L, "B", 2, 3, 2)
        );

        assertEquals(
                2,
                RoomAssignmentChecker.kiemTra(
                        phong(true, 100, "LT"),
                        lopThuong(30),
                        khac,
                        MAC_DINH
                ).loi().size()
        );
    }

    @Test
    void saiLoaiPhongBiCanhBaoKhongPhanBietHoaThuong() {

        ClassFacts yeuCau = lop(
                30,
                "Thực hành",
                Set.of(),
                2,
                1,
                3
        );

        assertTrue(
                RoomAssignmentChecker.kiemTra(
                        phong(true, 40, "thực hành"),
                        yeuCau,
                        List.of(),
                        MAC_DINH
                ).canhBao().isEmpty()
        );

        RoomCheckResult sai = RoomAssignmentChecker.kiemTra(
                phong(true, 40, "Lý thuyết"),
                yeuCau,
                List.of(),
                MAC_DINH
        );

        assertEquals(
                "SAI_LOAI_PHONG",
                sai.canhBao().get(0).code()
        );
    }

    @Test
    void khongYeuCauLoaiThiKhongKiemTraLoai() {

        assertTrue(
                RoomAssignmentChecker.kiemTra(
                        phong(true, 40, "Lý thuyết"),
                        lopThuong(30),
                        List.of(),
                        MAC_DINH
                ).canhBao().isEmpty()
        );
    }

    @Test
    void thieuThietBiLiệtKeDayDuVaKhongPhanBietHoaThuong() {

        ClassFacts yeuCau = lop(
                30,
                null,
                Set.of("Máy chiếu", "Điều hòa", "Micro"),
                2,
                1,
                3
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(true, 40, "LT", "máy chiếu"),
                yeuCau,
                List.of(),
                MAC_DINH
        );

        assertEquals(1, ketQua.canhBao().size());

        assertEquals(
                "THIEU_THIET_BI",
                ketQua.canhBao().get(0).code()
        );

        assertEquals(
                "Phòng thiếu thiết bị: Micro, Điều hòa",
                ketQua.canhBao().get(0).message()
        );
    }

    @Test
    void nhieuVanDeCungLucDuocTachDungMucChinhSach() {

        ClassFacts yeuCau = lop(
                60,
                "Thực hành",
                Set.of("Máy chiếu"),
                2,
                1,
                3
        );

        List<RoomSlot> khac = List.of(
                new RoomSlot(5L, "INT101.01", 2, 1, 3)
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phong(false, 30, "Lý thuyết"),
                yeuCau,
                khac,
                MAC_DINH
        );

        assertEquals(2, ketQua.loi().size());

        // Ngừng hoạt động + trùng lịch

        assertEquals(3, ketQua.canhBao().size());

        // Sức chứa + loại phòng + thiết bị
    }

    @Test
    void thongTinPhongThieuKhongGayLoi() {

        RoomFacts phongTho = new RoomFacts(
                true,
                50,
                null,
                null
        );

        ClassFacts yeuCau = lop(
                30,
                "Thực hành",
                Set.of("Máy chiếu"),
                2,
                1,
                3
        );

        RoomCheckResult ketQua = RoomAssignmentChecker.kiemTra(
                phongTho,
                yeuCau,
                List.of(),
                MAC_DINH
        );

        assertTrue(ketQua.hopLe());
        assertEquals(2, ketQua.canhBao().size());
    }
}
