package com.edu.registration.room;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Kiểm tra điều kiện khi gán phòng cho lớp
 * (thuần Java, không phụ thuộc Spring nên dễ kiểm thử).
 */
public final class RoomAssignmentChecker {

    private RoomAssignmentChecker() {
    }

    /**
     * @param khac các lớp khác của cùng học kỳ đang dùng phòng này
     *             (đã loại chính lớp đang sửa)
     */
    public static RoomCheckResult kiemTra(
            RoomFacts phong,
            ClassFacts lop,
            List<RoomSlot> khac,
            RoomPolicy chinhSach
    ) {

        List<RoomIssue> loi = new ArrayList<>();
        List<RoomIssue> canhBao = new ArrayList<>();

        // 1. Kiểm tra phòng có đang hoạt động không
        if (!phong.hoatDong()) {
            ghi(
                    chinhSach.hoatDong(),
                    new RoomIssue(
                            "PHONG_NGUNG_HOAT_DONG",
                            "Phòng đang ngừng hoạt động"
                    ),
                    loi,
                    canhBao
            );
        }

        // 2. Kiểm tra sức chứa phòng
        if (phong.sucChua() < lop.siSoToiDa()) {
            ghi(
                    chinhSach.sucChua(),
                    new RoomIssue(
                            "SUC_CHUA_KHONG_DU",
                            "Sức chứa phòng (" + phong.sucChua()
                                    + ") nhỏ hơn sĩ số tối đa của lớp ("
                                    + lop.siSoToiDa() + ")"
                    ),
                    loi,
                    canhBao
            );
        }

        // 3. Kiểm tra loại phòng
        if (coGiaTri(lop.loaiYeuCau())
                && !lop.loaiYeuCau().trim()
                .equalsIgnoreCase(chuoi(phong.loai()))) {

            ghi(
                    chinhSach.loaiPhong(),
                    new RoomIssue(
                            "SAI_LOAI_PHONG",
                            "Lớp yêu cầu loại phòng '"
                                    + lop.loaiYeuCau().trim()
                                    + "' nhưng phòng thuộc loại '"
                                    + chuoi(phong.loai()) + "'"
                    ),
                    loi,
                    canhBao
            );
        }

        // 4. Kiểm tra thiết bị
        Set<String> thieu = thietBiConThieu(
                phong.thietBi(),
                lop.thietBiYeuCau()
        );

        if (!thieu.isEmpty()) {
            ghi(
                    chinhSach.thietBi(),
                    new RoomIssue(
                            "THIEU_THIET_BI",
                            "Phòng thiếu thiết bị: "
                                    + String.join(", ", thieu)
                    ),
                    loi,
                    canhBao
            );
        }

        // 5. Kiểm tra trùng lịch
        int ketThuc = lop.tietBatDau() + lop.soTiet() - 1;

        for (RoomSlot slot : khac) {

            boolean giaoNhau =
                    slot.thu() == lop.thu()
                            && lop.tietBatDau() <= slot.tietKetThuc()
                            && slot.tietBatDau() <= ketThuc;

            if (giaoNhau) {
                ghi(
                        chinhSach.trungLich(),
                        new RoomIssue(
                                "TRUNG_LICH_PHONG",
                                "Phòng đã được lớp " + slot.maLop()
                                        + " dùng vào thứ " + slot.thu()
                                        + ", tiết " + slot.tietBatDau()
                                        + "-" + slot.tietKetThuc()
                        ),
                        loi,
                        canhBao
                );
            }
        }

        return new RoomCheckResult(
                List.copyOf(loi),
                List.copyOf(canhBao)
        );
    }

    /**
     * Phân loại vấn đề theo chính sách.
     */
    private static void ghi(
            PolicyMode mode,
            RoomIssue issue,
            List<RoomIssue> loi,
            List<RoomIssue> canhBao
    ) {

        if (mode == PolicyMode.BAT_BUOC) {
            loi.add(issue);

        } else if (mode == PolicyMode.CANH_BAO) {
            canhBao.add(issue);
        }
    }

    /**
     * Tìm các thiết bị phòng còn thiếu.
     * So sánh không phân biệt chữ hoa, chữ thường.
     */
    private static Set<String> thietBiConThieu(
            Set<String> coSan,
            Set<String> yeuCau
    ) {

        Set<String> thieu = new TreeSet<>();

        if (yeuCau == null) {
            return thieu;
        }

        Set<String> daCo = new HashSet<>();

        if (coSan != null) {
            coSan.forEach(
                    ten -> daCo.add(
                            ten.trim().toLowerCase(Locale.ROOT)
                    )
            );
        }

        for (String ten : yeuCau) {

            if (coGiaTri(ten)
                    && !daCo.contains(
                    ten.trim().toLowerCase(Locale.ROOT)
            )) {

                thieu.add(ten.trim());
            }
        }

        return thieu;
    }

    private static boolean coGiaTri(String giaTri) {
        return giaTri != null && !giaTri.isBlank();
    }

    private static String chuoi(String giaTri) {
        return giaTri == null ? "" : giaTri.trim();
    }
}
