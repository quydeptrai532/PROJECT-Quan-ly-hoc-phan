package com.edu.registration.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record KhoaDto(Long id, String ma, String ten) {
    }

    public record KhoaRequest(
            @NotBlank(message = "Mã khoa không được để trống") String ma,
            @NotBlank(message = "Tên khoa không được để trống") String ten) {
    }

    public record NganhDto(Long id, String ma, String ten, Long khoaId, String khoaTen) {
    }

    public record NganhRequest(
            @NotBlank(message = "Mã ngành không được để trống") String ma,
            @NotBlank(message = "Tên ngành không được để trống") String ten,
            @NotNull(message = "Vui lòng chọn khoa") Long khoaId) {
    }

    public record LopHanhChinhDto(Long id, String ma, String ten, Long nganhId, String nganhTen,
                                  Long khoaId, String khoaTen) {
    }

    public record LopHanhChinhRequest(
            @NotBlank(message = "Mã lớp không được để trống") String ma,
            @NotBlank(message = "Tên lớp không được để trống") String ten,
            @NotNull(message = "Vui lòng chọn ngành") Long nganhId) {
    }

    public record MonHocDto(
            Long id,
            String ma,
            String ten,
            int soTinChi,
            Long khoaId,
            String khoaTen,
            Long nguoiTaoId,
            String nguoiTaoUsername,
            LocalDateTime taoLuc,
            Long nguoiCapNhatId,
            String nguoiCapNhatUsername,
            LocalDateTime capNhatLuc) {

        public MonHocDto(Long id, String ma, String ten, int soTinChi, Long khoaId, String khoaTen) {
            this(id, ma, ten, soTinChi, khoaId, khoaTen, null, null, null, null, null, null);
        }
    }

    public record MonHocRequest(
            @NotBlank(message = "Mã môn học không được để trống") String ma,
            @NotBlank(message = "Tên môn học không được để trống") String ten,
            @Min(value = 1, message = "Số tín chỉ tối thiểu là 1")
            @Max(value = 10, message = "Số tín chỉ tối đa là 10") int soTinChi,
            @NotNull(message = "Vui lòng chọn khoa") Long khoaId) {
    }

    public record MonTienQuyetDto(
            Long id,
            Long monHocId,
            String maMonHoc,
            String tenMonHoc,
            int soTinChiMonHoc,
            Long monTienQuyetId,
            Long monLienQuanId,
            String maMon,
            String tenMon,
            int soTinChi,
            String loaiQuanHe,
            String tenLoaiQuanHe,
            Double dieuKienDat,
            boolean yeuCauDat,
            String maNhomTuChon,
            Integer tinChiTuChonYeuCau,
            String khoaNhapHoc,
            Long hocKyHieuLucTuId,
            String tenHocKyHieuLucTu,
            Long hocKyHieuLucDenId,
            String tenHocKyHieuLucDen,
            LocalDate hieuLucTu,
            LocalDate hieuLucDen,
            boolean dangHieuLuc,
            boolean quyCheDaChot,
            String moTaQuyCheHienHanh,
            String phanCanChuDuAnChot,
            String ghiChu,
            Long nguoiTaoId,
            String nguoiTaoUsername,
            LocalDateTime taoLuc,
            Long nguoiCapNhatId,
            String nguoiCapNhatUsername,
            LocalDateTime capNhatLuc) {

        public MonTienQuyetDto(Long id, Long monHocId, Long monTienQuyetId,
                               String maMon, String tenMon, int soTinChi) {
            this(id, monHocId, null, null, 0, monTienQuyetId, monTienQuyetId,
                    maMon, tenMon, soTinChi, "TIEN_QUYET", "Tiên quyết",
                    5.0, true, null, null, null,
                    null, null, null, null, null, null, true,
                    true, null, null, null,
                    null, null, null, null, null, null);
        }
    }

    public record MonTienQuyetRequest(
            Long monTienQuyetId,
            Long monLienQuanId,
            String loaiQuanHe,
            @DecimalMin(value = "0.0", message = "Điều kiện điểm đạt phải từ 0.0 đến 10.0")
            @DecimalMax(value = "10.0", message = "Điều kiện điểm đạt phải từ 0.0 đến 10.0")
            Double dieuKienDat,
            Boolean yeuCauDat,
            @Size(max = 60, message = "Mã nhóm tự chọn tối đa 60 ký tự")
            String maNhomTuChon,
            @Min(value = 1, message = "Số tín chỉ tự chọn yêu cầu tối thiểu là 1")
            @Max(value = 50, message = "Số tín chỉ tự chọn yêu cầu tối đa là 50")
            Integer tinChiTuChonYeuCau,
            @Size(max = 50, message = "Khóa áp dụng tối đa 50 ký tự")
            String khoaNhapHoc,
            Long hocKyHieuLucTuId,
            Long hocKyHieuLucDenId,
            LocalDate hieuLucTu,
            LocalDate hieuLucDen,
            Boolean dangHieuLuc,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
            String ghiChu) {

        public MonTienQuyetRequest(Long monTienQuyetId) {
            this(monTienQuyetId, monTienQuyetId, "TIEN_QUYET", 5.0, true,
                    null, null, null, null, null, null, null, true, null);
        }

        public Long resolvedMonLienQuanId() {
            return monTienQuyetId != null ? monTienQuyetId : monLienQuanId;
        }
    }

    public record QuanHeMonRequest(
            @NotNull(message = "Vui lòng chọn môn học chính")
            Long monHocId,
            Long monTienQuyetId,
            Long monLienQuanId,
            String loaiQuanHe,
            @DecimalMin(value = "0.0", message = "Điều kiện điểm đạt phải từ 0.0 đến 10.0")
            @DecimalMax(value = "10.0", message = "Điều kiện điểm đạt phải từ 0.0 đến 10.0")
            Double dieuKienDat,
            Boolean yeuCauDat,
            @Size(max = 60, message = "Mã nhóm tự chọn tối đa 60 ký tự")
            String maNhomTuChon,
            @Min(value = 1, message = "Số tín chỉ tự chọn yêu cầu tối thiểu là 1")
            @Max(value = 50, message = "Số tín chỉ tự chọn yêu cầu tối đa là 50")
            Integer tinChiTuChonYeuCau,
            @Size(max = 50, message = "Khóa áp dụng tối đa 50 ký tự")
            String khoaNhapHoc,
            Long hocKyHieuLucTuId,
            Long hocKyHieuLucDenId,
            LocalDate hieuLucTu,
            LocalDate hieuLucDen,
            Boolean dangHieuLuc,
            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
            String ghiChu) {

        public Long resolvedMonLienQuanId() {
            return monTienQuyetId != null ? monTienQuyetId : monLienQuanId;
        }

        public MonTienQuyetRequest toMonTienQuyetRequest() {
            return new MonTienQuyetRequest(
                    resolvedMonLienQuanId(),
                    resolvedMonLienQuanId(),
                    loaiQuanHe,
                    dieuKienDat,
                    yeuCauDat,
                    maNhomTuChon,
                    tinChiTuChonYeuCau,
                    khoaNhapHoc,
                    hocKyHieuLucTuId,
                    hocKyHieuLucDenId,
                    hieuLucTu,
                    hieuLucDen,
                    dangHieuLuc,
                    ghiChu
            );
        }
    }

    public record QuyCheQuanHeMonDto(
            String loaiQuanHe,
            String tenLoaiQuanHe,
            boolean quyCheDaChot,
            String trangThaiQuyChe,
            String moTaQuyCheHienHanh,
            String phanCanChuDuAnChot) {
    }

    public record KhoaHocDto(
            Long id,
            String ma,
            String ten,
            String nienKhoa,
            int namBatDau,
            int namKetThuc,
            Long hocKyBatDauId,
            String tenHocKyBatDau,
            Long hocKyKetThucId,
            String tenHocKyKetThuc,
            LocalDate hieuLucTu,
            LocalDate hieuLucDen,
            boolean dangHieuLuc,
            String moTa,
            Long nguoiTaoId,
            String nguoiTaoUsername,
            LocalDateTime taoLuc,
            Long nguoiCapNhatId,
            String nguoiCapNhatUsername,
            LocalDateTime capNhatLuc) {
    }

    public record KhoaHocRequest(
            @NotBlank(message = "Mã khóa học/niên khóa không được để trống")
            @Size(max = 30, message = "Mã khóa học/niên khóa tối đa 30 ký tự")
            String ma,
            @NotBlank(message = "Tên khóa học/niên khóa không được để trống")
            @Size(max = 150, message = "Tên khóa học/niên khóa tối đa 150 ký tự")
            String ten,
            @NotBlank(message = "Niên khóa không được để trống")
            @Size(max = 30, message = "Niên khóa tối đa 30 ký tự")
            String nienKhoa,
            @Min(value = 1990, message = "Năm bắt đầu không hợp lệ")
            @Max(value = 2100, message = "Năm bắt đầu không hợp lệ")
            int namBatDau,
            @Min(value = 1990, message = "Năm kết thúc không hợp lệ")
            @Max(value = 2100, message = "Năm kết thúc không hợp lệ")
            int namKetThuc,
            Long hocKyBatDauId,
            Long hocKyKetThucId,
            LocalDate hieuLucTu,
            LocalDate hieuLucDen,
            Boolean dangHieuLuc,
            @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
            String moTa) {
    }

    public record HocKyDto(Long id, String ten, String namHoc, LocalDate ngayBatDau, LocalDate ngayKetThuc,
                           LocalDate dangKyTu, LocalDate dangKyDen, int soTinChiToiThieu,
                           int soTinChiToiDa, boolean dangMoDangKy) {
    }

    public record HocKyRequest(
            @NotBlank(message = "Tên học kỳ không được để trống") String ten,
            @NotBlank(message = "Năm học không được để trống") String namHoc,
            @NotNull(message = "Vui lòng chọn ngày bắt đầu") LocalDate ngayBatDau,
            @NotNull(message = "Vui lòng chọn ngày kết thúc") LocalDate ngayKetThuc,
            @NotNull(message = "Vui lòng chọn ngày mở đăng ký") LocalDate dangKyTu,
            @NotNull(message = "Vui lòng chọn ngày đóng đăng ký") LocalDate dangKyDen,
            @Min(value = 0, message = "Số tín chỉ tối thiểu không hợp lệ") int soTinChiToiThieu,
            @Min(value = 1, message = "Số tín chỉ tối đa không hợp lệ") int soTinChiToiDa) {
    }
}
