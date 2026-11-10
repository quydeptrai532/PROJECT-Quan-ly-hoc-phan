package com.edu.registration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class StatDtos {

    private StatDtos() {
    }

    public record TongQuanDto(long soSinhVien, long soGiangVien, long soMonHoc, long soLopHocPhan,
                              long soDangKy, String hocKyHienTai) {
    }

    public record ThongKeMonDto(Long monHocId, String maMon, String tenMon, int soLop,
                                int tongChiTieu, int daDangKy, double tiLeLapDay) {
    }

    public record CanhBaoHocVuDto(String loai, String tieuDe, String lyDo, String mucTinh,
                                  Long lopHocPhanId, String maLop, Long monHocId, String maMon,
                                  String tenMon, int soLop, long daDangKy, long tongChiTieu,
                                  double tiLeLapDay, Long hocKyId, String tenHocKy,
                                  LocalDate dangKyDen, String duongDan) {
    }

    public record HanhDongHocVuDto(Long hocKyId, String tenHocKy, Long khoaId, String tenKhoa,
                                   LocalDateTime capNhatLuc, List<CanhBaoHocVuDto> canhBao) {
    }

    public record KhoaKhoangDiemDto(String maKhoang, String kyHieuKhoang, String tenKhoang,
                                    int soLuong, double tyLePhanTram, boolean laKhoangDat) {
    }

    public record XepLoaiGpaDto(String maLoai, String tenLoai, String kyHieuKhoang,
                                int soLuong, double tyLePhanTram) {
    }

    public record ThongKeMonChiTietDto(Long monHocId, String maMon, String tenMon, int soTinChi,
                                       int soLop, int tongChiTieu, int daDangKy, double tiLeLapDay,
                                       int soSinhVienCoDiem, Double diemTrungBinh,
                                       int soSinhVienDat, double tyLeDat,
                                       int soSinhVienTruot, double tyLeTruot,
                                       int soSinhVienChuaCoDiem, Double gpa10, Double gpa4) {
    }

    public record KpiTongHopDto(long tongSoLop, long tongChiTieu, long tongDangKy, double tiLeLapDay,
                                long conTrong, long soSinhVienCoDiem, Double diemTrungBinh,
                                long soSinhVienDat, double tyLeDat, long soSinhVienTruot, double tyLeTruot,
                                long soSinhVienChuaCoDiem, Double gpa10TrungBinh, Double gpa4TrungBinh,
                                Double tyLeHoanThanhTinChiTb, long tongSinhVienXetTienDo,
                                long soSinhVienChuaGanChuongTrinh) {
    }

    public record BoLocThongKeDto(Long hocKyId, String tenHocKy,
                                  Long khoaId, String tenKhoa,
                                  Long nganhId, String tenNganh,
                                  Long monHocId, String tenMonHoc,
                                  Long lopHocPhanId, String maLopHocPhan,
                                  String phamViDiem, LocalDateTime thoiDiemTruyVan) {
    }

    public record DashboardKpiResponseDto(BoLocThongKeDto boLoc,
                                          KpiTongHopDto kpi,
                                          List<KhoaKhoangDiemDto> phanBoDiem,
                                          List<XepLoaiGpaDto> phanBoGpa,
                                          List<ThongKeMonChiTietDto> danhSachMon) {
    }
}
