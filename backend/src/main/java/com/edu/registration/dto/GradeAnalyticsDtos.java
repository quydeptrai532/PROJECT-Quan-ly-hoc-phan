package com.edu.registration.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các DTO phục vụ thống kê điểm lớp học phần và phân bố điểm (QLSV-75 / TASK-LC-10).
 * Tuyệt đối không chứa thông tin định danh cá nhân của sinh viên (PII) trong phần tổng hợp.
 */
public final class GradeAnalyticsDtos {

    private GradeAnalyticsDtos() {
    }

    public record KhoangPhanBoDiemDto(
            String maKhoang,
            String tenKhoang,
            String kyHieuKhoang,
            double tuDiem,
            double denDiem,
            boolean baoGomTuDiem,
            boolean baoGomDenDiem,
            boolean laKhoangDat,
            int soLuong,
            double tyLePhanTram,
            int mauSoTyLe
    ) {
    }

    public record ChiSoThongKeDiemDto(
            String phamViDuLieu,
            String nhanPhamVi,
            boolean daCongBoChinhThuc,
            int tongSoDangKyHieuLuc,
            int soBanGhiDuocTinh,
            int soSinhVienCoDiem,
            int soSinhVienChuaCoDiem,
            int soSinhVienChuaNhapDuDiem,
            int soSinhVienChuaCongBoDiem,
            int mauSoTinhTrungBinhVaTyLe,
            String giaiThichMauSo,
            double nguongDat,
            Double diemTrungBinh,
            Double diemThapNhat,
            Double diemCaoNhat,
            int soSinhVienDat,
            int soSinhVienTruot,
            double tyLeDat,
            double tyLeTruot,
            int tongSoTrongPhanBo,
            List<KhoangPhanBoDiemDto> phanBoDiem
    ) {
    }

    public record ThongKeDiemLopItemDto(
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long khoaId,
            String tenKhoa,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            Long giangVienId,
            String tenGiangVien,
            String trangThaiBangDiem,
            String moTaTrangThaiBangDiem,
            boolean daCongBo,
            boolean daKhoa,
            LocalDateTime congBoLuc,
            boolean coQuyenDrillDownSinhVien,
            String duongDanDrillDown,
            ChiSoThongKeDiemDto thongKeCongBo,
            ChiSoThongKeDiemDto thongKeTamTinhChuaCongBo
    ) {
    }

    public record ThongKeDiemMonItemDto(
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long khoaId,
            String tenKhoa,
            int tongSoLop,
            int soLopDaCongBo,
            int soLopChuaCongBo,
            ChiSoThongKeDiemDto thongKeCongBo
    ) {
    }

    public record CauHinhThongKeDiemDto(
            double nguongDat,
            String nguonNguongDat,
            String chinhSachDiemThieu,
            boolean laQuyCheChinhThuc,
            String ghiChuCanChot,
            List<KhoangPhanBoDiemDto> khoangPhanBoMacDinh
    ) {
    }

    public record LuaChonMonDto(
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long khoaId,
            String tenKhoa,
            int soLopTrongPhamVi
    ) {
    }

    public record LuaChonLopDto(
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            String trangThaiBangDiem,
            String moTaTrangThaiBangDiem,
            boolean daCongBo
    ) {
    }

    public record BaoCaoThongKeDiemDto(
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            Long khoaId,
            String tenKhoa,
            Long monHocId,
            String maMon,
            String tenMon,
            Long lopHocPhanId,
            String maLop,
            String vaiTroNguoiXem,
            LocalDateTime capNhatLuc,
            int tongSoLopTrongPhamVi,
            int soLopDaCongBo,
            int soLopChuaCongBo,
            CauHinhThongKeDiemDto cauHinh,
            ChiSoThongKeDiemDto tongHopCongBo,
            List<ThongKeDiemMonItemDto> theoMon,
            List<ThongKeDiemLopItemDto> theoLop,
            List<LuaChonMonDto> danhSachMon,
            List<LuaChonLopDto> danhSachLop
    ) {
    }
}
