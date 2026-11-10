package com.edu.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class RegistrationPeriodDtos {

    private RegistrationPeriodDtos() {
    }

    public record DotDangKyDto(
            Long id,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            String maDot,
            String tenDot,
            String loaiDot,
            Long khoaHocId,
            String maKhoaHoc,
            String tenKhoaHoc,
            String khoaNhapHoc,
            Long nganhId,
            String maNganh,
            String tenNganh,
            Long lopHanhChinhId,
            String maLopHanhChinh,
            String tenLopHanhChinh,
            String nhomDoiTuong,
            int thuTuUuTien,
            LocalDateTime thoiGianBatDau,
            LocalDateTime thoiGianKetThuc,
            boolean dangHieuLuc,
            boolean dongSom,
            LocalDateTime thoiDiemDongSom,
            String lyDoDongSom,
            LocalDateTime thoiGianKetThucGoc,
            String lyDoDieuChinh,
            boolean dangMo,
            String trangThaiHienThi,
            String moTaDoiTuong,
            Long nguoiTaoId,
            String nguoiTaoUsername,
            LocalDateTime taoLuc,
            Long nguoiCapNhatId,
            String nguoiCapNhatUsername,
            LocalDateTime capNhatLuc,
            String ghiChu) {
    }

    public record TaoDotDangKyRequest(
            @NotNull(message = "Vui lòng chọn học kỳ") Long hocKyId,
            @NotBlank(message = "Mã đợt đăng ký không được để trống")
            @Size(max = 40, message = "Mã đợt tối đa 40 ký tự") String maDot,
            @NotBlank(message = "Tên đợt đăng ký không được để trống")
            @Size(max = 150, message = "Tên đợt tối đa 150 ký tự") String tenDot,
            String loaiDot,
            Long khoaHocId,
            @Size(max = 50, message = "Khóa áp dụng tối đa 50 ký tự") String khoaNhapHoc,
            Long nganhId,
            Long lopHanhChinhId,
            @Size(max = 100, message = "Nhóm đối tượng tối đa 100 ký tự") String nhomDoiTuong,
            Integer thuTuUuTien,
            @NotNull(message = "Vui lòng chọn thời gian bắt đầu") LocalDateTime thoiGianBatDau,
            @NotNull(message = "Vui lòng chọn thời gian kết thúc") LocalDateTime thoiGianKetThuc,
            Boolean dangHieuLuc,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String ghiChu) {
    }

    public record SuaDotDangKyRequest(
            @NotBlank(message = "Mã đợt đăng ký không được để trống")
            @Size(max = 40, message = "Mã đợt tối đa 40 ký tự") String maDot,
            @NotBlank(message = "Tên đợt đăng ký không được để trống")
            @Size(max = 150, message = "Tên đợt tối đa 150 ký tự") String tenDot,
            String loaiDot,
            Long khoaHocId,
            @Size(max = 50, message = "Khóa áp dụng tối đa 50 ký tự") String khoaNhapHoc,
            Long nganhId,
            Long lopHanhChinhId,
            @Size(max = 100, message = "Nhóm đối tượng tối đa 100 ký tự") String nhomDoiTuong,
            Integer thuTuUuTien,
            @NotNull(message = "Vui lòng chọn thời gian bắt đầu") LocalDateTime thoiGianBatDau,
            @NotNull(message = "Vui lòng chọn thời gian kết thúc") LocalDateTime thoiGianKetThuc,
            Boolean dangHieuLuc,
            @NotBlank(message = "Vui lòng nhập lý do điều chỉnh đợt đăng ký")
            @Size(max = 500, message = "Lý do điều chỉnh tối đa 500 ký tự") String lyDo,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String ghiChu) {
    }

    public record GiaHanDotRequest(
            @NotNull(message = "Vui lòng chọn thời gian kết thúc mới") LocalDateTime thoiGianKetThucMoi,
            @NotBlank(message = "Vui lòng nhập lý do gia hạn")
            @Size(max = 500, message = "Lý do gia hạn tối đa 500 ký tự") String lyDo) {
    }

    public record DongSomDotRequest(
            @NotBlank(message = "Vui lòng nhập lý do đóng sớm")
            @Size(max = 500, message = "Lý do đóng sớm tối đa 500 ký tự") String lyDo) {
    }

    public record MoBoSungDotRequest(
            @NotNull(message = "Vui lòng chọn học kỳ") Long hocKyId,
            @NotBlank(message = "Mã đợt bổ sung không được để trống")
            @Size(max = 40, message = "Mã đợt tối đa 40 ký tự") String maDot,
            @NotBlank(message = "Tên đợt bổ sung không được để trống")
            @Size(max = 150, message = "Tên đợt tối đa 150 ký tự") String tenDot,
            Long khoaHocId,
            @Size(max = 50, message = "Khóa áp dụng tối đa 50 ký tự") String khoaNhapHoc,
            Long nganhId,
            Long lopHanhChinhId,
            @Size(max = 100, message = "Nhóm đối tượng tối đa 100 ký tự") String nhomDoiTuong,
            Integer thuTuUuTien,
            @NotNull(message = "Vui lòng chọn thời gian bắt đầu") LocalDateTime thoiGianBatDau,
            @NotNull(message = "Vui lòng chọn thời gian kết thúc") LocalDateTime thoiGianKetThuc,
            @NotBlank(message = "Vui lòng nhập lý do mở bổ sung")
            @Size(max = 500, message = "Lý do mở bổ sung tối đa 500 ký tự") String lyDo,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String ghiChu) {
    }

    public record TrangThaiDangKySinhVienDto(
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            String timezone,
            LocalDateTime thoiDiemKiemTra,
            boolean coCauHinhDotRieng,
            boolean thuocDoiTuong,
            boolean dangMoChoSinhVien,
            String maDot,
            String tenDot,
            String loaiDot,
            Integer thuTuUuTien,
            LocalDateTime thoiGianBatDau,
            LocalDateTime thoiGianKetThuc,
            String trangThai,
            String thongDiep,
            List<DotDangKyDto> danhSachDotPhuHop,
            boolean chinhSachUuTienDaChot,
            String phanCanChuDuAnChot,
            String thongTinSinhVien) {
    }
}
