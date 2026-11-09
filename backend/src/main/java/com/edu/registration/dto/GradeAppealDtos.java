package com.edu.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class GradeAppealDtos {

    private GradeAppealDtos() {}

    public record TaoHoSoPhucKhaoRequest(
            @NotNull(message = "Mã đăng ký học phần không được để trống")
            Long dangKyId,

            @NotBlank(message = "Loại điểm phúc khảo không được để trống")
            String loaiDiem,

            Double diemMongMuon,

            @NotBlank(message = "Lý do phúc khảo không được để trống")
            @Size(min = 10, max = 2000, message = "Lý do phúc khảo phải từ 10 đến 2000 ký tự")
            String lyDo
    ) {}

    public record XuLyPhucKhaoRequest(
            @NotBlank(message = "Trạng thái xử lý không được để trống")
            String trangThai, // CHAP_THUAN hoặc TU_CHOI

            @NotBlank(message = "Kết quả/Lý do xử lý không được để trống")
            @Size(min = 5, max = 2000, message = "Kết quả/Lý do xử lý phải từ 5 đến 2000 ký tự")
            String ketQuaXuLy,

            Double diemQuaTrinhMoi,
            Double diemCuoiKyMoi
    ) {}

    public record TiepNhanRequest(
            String ghiChu
    ) {}

    public record HuyPhucKhaoRequest(
            String lyDo
    ) {}

    public record LichSuPhucKhaoDto(
            Long id,
            String hanhDong,
            String trangThaiTruoc,
            String trangThaiSau,
            Long nguoiThucHienId,
            String nguoiThucHienUsername,
            String nguoiThucHienHoTen,
            String nguoiThucHienRole,
            String lyDo,
            Double diemQuaTrinhMoi,
            Double diemCuoiKyMoi,
            Double diemTongKetMoi,
            LocalDateTime thoiDiem
    ) {}

    public record HoSoPhucKhaoDto(
            Long id,
            String maHoSo,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            Long lopHocPhanId,
            String maLopHocPhan,
            Long monHocId,
            String maMonHoc,
            String tenMonHoc,
            Integer soTinChi,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            Long dangKyId,
            Long diemId,
            Double diemQuaTrinhGoc,
            Double diemCuoiKyGoc,
            Double diemTongKetGoc,
            String loaiDiem,
            Double diemMongMuon,
            String lyDo,
            LocalDateTime hanNop,
            boolean quaHanNop,
            String trangThai,
            String moTaTrangThai,
            Long nguoiXuLyId,
            String nguoiXuLyUsername,
            String nguoiXuLyHoTen,
            LocalDateTime thoiDiemXuLy,
            String ketQuaXuLy,
            Double diemQuaTrinhMoi,
            Double diemCuoiKyMoi,
            Double diemTongKetMoi,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<LichSuPhucKhaoDto> lichSu
    ) {}

    public record DiemKhaDungPhucKhaoDto(
            Long dangKyId,
            Long diemId,
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            Integer soTinChi,
            Long hocKyId,
            String tenHocKy,
            String namHoc,
            Double diemQuaTrinh,
            Double diemCuoiKy,
            Double diemTongKet,
            LocalDateTime congBoLuc,
            LocalDateTime hanNop,
            boolean quaHanNop,
            boolean daCoDonDangXuLy
    ) {}

    public record DanhMucPhucKhaoDto(
            List<DiemKhaDungPhucKhaoDto> danhSachDiem,
            int submissionWindowDays,
            boolean officialRegulation,
            String slaNote
    ) {}

    public record CauHinhPhucKhaoDto(
            int submissionWindowDays,
            boolean requirePublishedGrade,
            boolean autoApplyGradeChange,
            boolean officialRegulation,
            String slaNote
    ) {}
}
