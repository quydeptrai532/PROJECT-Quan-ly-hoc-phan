package com.edu.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class SupportRequestDtos {

    private SupportRequestDtos() {
    }

    public record TaoYeuCauRequest(
            @NotBlank(message = "Loại yêu cầu không được để trống")
            @Size(max = 40, message = "Loại yêu cầu không hợp lệ")
            String loaiYeuCau,

            @NotBlank(message = "Tiêu đề không được để trống")
            @Size(min = 5, max = 200, message = "Tiêu đề phải từ 5 đến 200 ký tự")
            String tieuDe,

            @NotBlank(message = "Nội dung không được để trống")
            @Size(min = 10, max = 4000, message = "Nội dung phải từ 10 đến 4000 ký tự")
            String noiDung,

            Long monHocId,
            Long diemId,
            Long hocKyId
    ) {
    }

    public record ThemTraoDoiRequest(
            @NotBlank(message = "Nội dung trao đổi không được để trống")
            @Size(min = 1, max = 2000, message = "Nội dung trao đổi phải từ 1 đến 2000 ký tự")
            String noiDung,

            Boolean isNoiBo
    ) {
        public ThemTraoDoiRequest(String noiDung) {
            this(noiDung, false);
        }
    }

    public record DoiTrangThaiRequest(
            @NotBlank(message = "Trạng thái mới không được để trống")
            @Size(max = 40, message = "Trạng thái mới không hợp lệ")
            String trangThai,

            @Size(max = 500, message = "Ghi chú xử lý tối đa 500 ký tự")
            String ghiChu
    ) {
    }

    public record PhanCongRequest(
            @NotNull(message = "Mã người xử lý không được để trống")
            Long handlerUserId,

            @Size(max = 500, message = "Ghi chú phân công tối đa 500 ký tự")
            String ghiChu
    ) {
    }

    public record DieuChinhYeuCauRequest(
            String loaiYeuCau,
            String mucUuTien,
            LocalDateTime hanXuLy,

            @NotBlank(message = "Lý do điều chỉnh không được để trống")
            @Size(min = 3, max = 500, message = "Lý do điều chỉnh phải từ 3 đến 500 ký tự")
            String ghiChu
    ) {
    }

    public record YeuCauMinhChungRequest(
            @NotBlank(message = "Nội dung yêu cầu bổ sung không được để trống")
            @Size(min = 5, max = 1000, message = "Nội dung yêu cầu phải từ 5 đến 1000 ký tự")
            String noiDung,

            LocalDateTime hanBoSung
    ) {
    }

    public record BoSungMinhChungRequest(
            @NotBlank(message = "Nội dung giải trình không được để trống")
            @Size(min = 5, max = 2000, message = "Nội dung giải trình phải từ 5 đến 2000 ký tự")
            String noiDung
    ) {
    }

    public record DanhMucMucDto(String ma, String ten) {
    }

    public record ChinhSachTepDto(
            long maxFileSizeBytes,
            int maxFilesPerRequest,
            List<String> allowedExtensions,
            String slaNote
    ) {
    }

    public record MonHocLienQuanDto(
            Long id,
            String ma,
            String ten,
            int soTinChi
    ) {
    }

    public record HocKyLienQuanDto(
            Long id,
            String ten,
            String namHoc
    ) {
    }

    public record DiemLienQuanDto(
            Long id,
            Long dangKyId,
            Long monHocId,
            String maMon,
            String tenMon,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            String maLop,
            Double diemQuaTrinh,
            Double diemCuoiKy,
            Double diemTongKet
    ) {
    }

    public record HandlerUserDto(
            Long id,
            String username,
            String hoTen,
            String role
    ) {
    }

    public record DanhMucHoTroDto(
            List<DanhMucMucDto> loaiYeuCau,
            List<DanhMucMucDto> trangThai,
            List<DanhMucMucDto> mucUuTien,
            ChinhSachTepDto chinhSachTep,
            List<MonHocLienQuanDto> monHoc,
            List<HocKyLienQuanDto> hocKy,
            List<DiemLienQuanDto> diemCuaToi,
            List<HandlerUserDto> danhSachNguoiXuLy,
            boolean officialSlaEnabled,
            String slaPolicyNote,
            String workingCalendarNote
    ) {
    }

    public record TepDinhKemDto(
            Long id,
            String tenTepGoc,
            String contentType,
            long kichThuoc,
            LocalDateTime createdAt,
            String downloadUrl
    ) {
    }

    public record TraoDoiDto(
            Long id,
            Long nguoiGuiId,
            String tenHienThiNguoiGui,
            String vaiTroNguoiGui,
            String noiDung,
            boolean congKhai,
            boolean isNoiBo,
            LocalDateTime createdAt
    ) {
    }

    public record LichSuTrangThaiDto(
            Long id,
            String loaiThayDoi,
            String tenLoaiThayDoi,
            String trangThaiCu,
            String tenTrangThaiCu,
            String trangThaiMoi,
            String tenTrangThaiMoi,
            String giaTriCu,
            String giaTriMoi,
            Long nguoiThucHienId,
            String tenNguoiThucHien,
            String vaiTroNguoiThucHien,
            String ghiChu,
            LocalDateTime thoiDiem
    ) {
    }

    public record TrangThaiSlaThoiGianDto(
            String trangThai,
            String tenTrangThai,
            LocalDateTime thoiDiemBatDau,
            LocalDateTime thoiDiemKetThuc,
            long thoiGianPhutLich,
            long thoiGianPhutLamViec,
            boolean tamDungSla
    ) {
    }

    public record SupportRequestSlaDetailDto(
            LocalDateTime targetDueAt,
            boolean isOverdue,
            String slaStatus,
            String tenSlaStatus,
            LocalDateTime firstResponseAt,
            Long firstResponseWorkingMinutes,
            LocalDateTime resolvedAt,
            long activeWorkingMinutes,
            long pausedWorkingMinutes,
            Long remainingWorkingMinutes,
            Long overdueWorkingMinutes,
            List<TrangThaiSlaThoiGianDto> thoiGianTheoTrangThai,
            boolean officialSlaEnabled,
            String slaNote
    ) {
    }

    public record SupportRequestSlaSummaryDto(
            long totalInQueue,
            long unassignedCount,
            long myAssignedCount,
            long inProgressCount,
            long awaitingStudentCount,
            long overdueCount,
            long nearingDueCount,
            long resolvedCount,
            double avgFirstResponseWorkingHours,
            double avgResolveWorkingHours,
            double onTimeResolutionRate,
            boolean officialSlaEnabled,
            String slaNote,
            String workingCalendarNote
    ) {
    }

    public record YeuCauTomTatDto(
            Long id,
            String maPhieu,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String loaiYeuCau,
            String tenLoaiYeuCau,
            String tieuDe,
            String trangThai,
            String tenTrangThai,
            String mucUuTien,
            String tenMucUuTien,
            LocalDateTime hanXuLy,
            MonHocLienQuanDto monHoc,
            DiemLienQuanDto diem,
            HocKyLienQuanDto hocKy,
            Long nguoiXuLyId,
            String nguoiXuLyUsername,
            String nguoiXuLyHoTen,
            String slaStatus,
            String tenSlaStatus,
            boolean isOverdue,
            boolean yeuCauBoSungMinhChung,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record YeuCauChiTietDto(
            Long id,
            String maPhieu,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String loaiYeuCau,
            String tenLoaiYeuCau,
            String tieuDe,
            String noiDungGoc,
            String trangThai,
            String tenTrangThai,
            String mucUuTien,
            String tenMucUuTien,
            LocalDateTime hanXuLy,
            MonHocLienQuanDto monHoc,
            DiemLienQuanDto diem,
            HocKyLienQuanDto hocKy,
            Long nguoiXuLyId,
            String nguoiXuLyUsername,
            String nguoiXuLyHoTen,
            boolean yeuCauBoSungMinhChung,
            String noiDungYeuCauMinhChung,
            SupportRequestSlaDetailDto sla,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            boolean coThePhanHoi,
            List<String> trangThaiTiepTheoHopLe,
            List<TepDinhKemDto> tepDinhKem,
            List<TraoDoiDto> traoDoi,
            List<LichSuTrangThaiDto> lichSuTrangThai
    ) {
    }
}
