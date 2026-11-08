package com.edu.registration.dto;

import com.edu.registration.entity.AttendanceStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AttendanceDtos {

    private AttendanceDtos() { }

    public record TaoBuoiHocRequest(
            @NotNull(message = "Lớp học phần không được để trống")
            Long lopHocPhanId,

            @NotNull(message = "Ngày học không được để trống")
            LocalDate ngayHoc,

            @NotNull(message = "Ca học không được để trống")
            @Min(value = 1, message = "Ca học phải từ 1 trở lên")
            @Max(value = 10, message = "Ca học không vượt quá 10")
            Integer caHoc,

            @Size(max = 30, message = "Phòng học tối đa 30 ký tự")
            String phongHoc,

            @Min(value = 1, message = "Tiết bắt đầu từ 1 đến 12")
            @Max(value = 12, message = "Tiết bắt đầu từ 1 đến 12")
            Integer tietBatDau,

            @Min(value = 1, message = "Số tiết từ 1 đến 6")
            @Max(value = 6, message = "Số tiết từ 1 đến 6")
            Integer soTiet,

            @Size(max = 255, message = "Nội dung/ghi chú không vượt quá 255 ký tự")
            String noiDung
    ) { }

    public record CapNhatBuoiHocRequest(
            LocalDate ngayHoc,

            @Min(value = 1, message = "Ca học phải từ 1 trở lên")
            @Max(value = 10, message = "Ca học không vượt quá 10")
            Integer caHoc,

            @Size(max = 30, message = "Phòng học tối đa 30 ký tự")
            String phongHoc,

            @Min(value = 1, message = "Tiết bắt đầu từ 1 đến 12")
            @Max(value = 12, message = "Tiết bắt đầu từ 1 đến 12")
            Integer tietBatDau,

            @Min(value = 1, message = "Số tiết từ 1 đến 6")
            @Max(value = 6, message = "Số tiết từ 1 đến 6")
            Integer soTiet,

            @Size(max = 255, message = "Nội dung/ghi chú không vượt quá 255 ký tự")
            String noiDung
    ) { }

    public record BuoiHocDto(
            Long id,
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            LocalDate ngayHoc,
            Integer caHoc,
            String phongHoc,
            Integer tietBatDau,
            Integer soTiet,
            String noiDung,
            boolean daKhoa,
            LocalDateTime thoiGianKhoa,
            String nguoiKhoa,
            String nguoiTao,
            LocalDateTime thoiGianTao,
            String nguoiCapNhat,
            LocalDateTime thoiGianCapNhat,
            int tongSinhVien,
            int soDaDiemDanh,
            int soCoMat,
            int soVang,
            int soMuon,
            int soCoPhep
    ) { }

    public record DiemDanhRecordRequest(
            @NotNull(message = "Sinh viên không được để trống")
            Long sinhVienId,

            @NotNull(message = "Trạng thái điểm danh không được để trống")
            AttendanceStatus trangThai,

            @Size(max = 255, message = "Ghi chú không vượt quá 255 ký tự")
            String ghiChu
    ) { }

    public record LuuDiemDanhRequest(
            @NotNull(message = "Danh sách điểm danh không được để trống")
            List<DiemDanhRecordRequest> danhSach,

            Boolean khoaBuoi
    ) { }

    public record DiemDanhChiTietDto(
            Long id,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String lopHanhChinh,
            AttendanceStatus trangThai,
            String ghiChu,
            String nguoiCapNhat,
            LocalDateTime thoiGianCapNhat
    ) { }

    public record BuoiHocChiTietDto(
            BuoiHocDto buoiHoc,
            List<DiemDanhChiTietDto> danhSachDiemDanh
    ) { }

    public record BaoCaoChuyenCanSinhVienDto(
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String lopHanhChinh,
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            int soTinChi,
            int tongSoBuoi,
            int soCoMat,
            int soVang,
            int soMuon,
            int soCoPhep,
            double tyLeCoMat,
            double tyLeVang,
            String ghiChuQuyChe
    ) { }

    public record BaoCaoChuyenCanLopDto(
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            String tenHocKy,
            int tongSoBuoi,
            int siSoLop,
            String congThucApDung,
            String ghiChuQuyChe,
            List<BaoCaoChuyenCanSinhVienDto> danhSachSinhVien,
            List<BuoiHocDto> danhSachBuoiHoc
    ) { }

    public record KhoaBuoiHocRequest(
            boolean khoa
    ) { }
}
