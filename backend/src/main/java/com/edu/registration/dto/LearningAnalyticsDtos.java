package com.edu.registration.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các DTO phục vụ phân tích học tập (Learning Analytics cơ bản - QLSV-98 / TASK-OP-11).
 *
 * Nguyên tắc tuân thủ:
 * 1. Tái sử dụng định nghĩa KPI từ TASK-AD-11 (QLSV-86):
 *    - Tỷ lệ Đạt / Trượt theo ngưỡng AcademicRules.DIEM_DAT (>= 5.0).
 *    - Mẫu số M: chỉ tính các bản ghi điểm hợp lệ [0.0, 10.0] thuộc lớp đã công bố bảng điểm (DA_CONG_BO hoặc DA_KHOA).
 *    - Điểm thiếu không tự tính thành 0.0 mà loại khỏi mẫu số và tách riêng vào soSinhVienChuaCoDiem.
 * 2. So sánh lớp cùng môn: chỉ so sánh khi cùng trạng thái công bố, cùng kỳ, đủ mẫu số (>= 5) và cùng công thức.
 * 3. Tuyệt đối không chứa thông tin định danh cá nhân (Zero Student PII).
 * 4. Không đưa ra kết luận nhân quả từ số liệu mô tả thống kê.
 */
public final class LearningAnalyticsDtos {

    private LearningAnalyticsDtos() {
    }

    public record KhoangDiemPhanBoDto(
            String maKhoang,
            String kyHieuKhoang,
            String tenKhoang,
            int soLuong,
            double tyLePhanTram,
            boolean laKhoangDat
    ) {
    }

    public record ChiSoKpiHocTapDto(
            int tongSoLop,
            int soLopDaCongBo,
            int soLopChuaCongBo,
            int tongSoDangKyHieuLuc,
            int mauSoDuocTinh,
            int soSinhVienChuaCoDiem,
            Double diemTrungBinh,
            Double diemThapNhat,
            Double diemCaoNhat,
            Double gpaThang4,
            int soSinhVienDat,
            int soSinhVienTruot,
            double tyLeDat,
            double tyLeTruot,
            double nguongDiemDat,
            String chinhSachDiemThieu,
            String khuyenCaoNhanQua
    ) {
    }

    public record XuHuongGpaHocKyDto(
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            int tongSoLop,
            int soLopDaCongBo,
            int soDangKy,
            int mauSoDuocTinh,
            int soSinhVienChuaCoDiem,
            Double diemTrungBinh,
            Double gpaThang4,
            int soSinhVienDat,
            double tyLeDat,
            int soSinhVienTruot,
            double tyLeTruot,
            boolean coDiemCongBo,
            String ghiChu
    ) {
    }

    public record SoSanhLopItemDto(
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long giangVienId,
            String tenGiangVien,
            Long hocKyId,
            String tenHocKy,
            String trangThaiBangDiem,
            String moTaTrangThai,
            boolean daCongBo,
            int siSoToiDa,
            int daDangKy,
            int mauSo,
            int soSinhVienChuaCoDiem,
            Double diemTrungBinh,
            Double diemThapNhat,
            Double diemCaoNhat,
            Double gpaThang4,
            int soSinhVienDat,
            double tyLeDat,
            int soSinhVienTruot,
            double tyLeTruot,
            List<KhoangDiemPhanBoDto> phanBoDiem,
            boolean duDieuKienSoSanhChinhThuc,
            boolean duSoMauToiThieu,
            String ghiChuDieuKien
    ) {
    }

    public record SoSanhCungMonDto(
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long hocKyId,
            String tenHocKy,
            int tongSoLop,
            int soLopDaCongBo,
            int soLopDuDieuKienSoSanh,
            boolean coTheSoSanh,
            String thongDiepSoSanh,
            String canhBaoNhanQua,
            List<SoSanhLopItemDto> danhSachLop
    ) {
    }

    public record BoLocHocTapDto(
            Long hocKyId,
            String tenHocKy,
            Long khoaId,
            String tenKhoa,
            Long nganhId,
            String tenNganh,
            Long monHocId,
            String tenMonHoc,
            String vaiTroNguoiXem,
            LocalDateTime thoiDiemTruyVan
    ) {
    }

    public record BaoCaoLearningAnalyticsDto(
            BoLocHocTapDto boLoc,
            ChiSoKpiHocTapDto kpi,
            List<KhoangDiemPhanBoDto> phanBoDiem,
            List<XuHuongGpaHocKyDto> xuHuongTheoHocKy,
            SoSanhCungMonDto soSanhCungMon,
            List<GradeAnalyticsDtos.LuaChonMonDto> danhSachMonHoc,
            String canhBaoNhanQua,
            String ghiChuQuyChe
    ) {
    }
}
