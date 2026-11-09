package com.edu.registration.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class GradeImportDtos {

    private GradeImportDtos() {
    }

    public record TemplateColumnDto(
            String cotExcel,
            String maCot,
            String tenHienThi,
            boolean batBuoc,
            String moTa
    ) {
    }

    public record GradeImportConfigDto(
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            String trangThaiBangDiem,
            long maxFileSizeBytes,
            int maxRows,
            int maxSheets,
            long parseTimeoutMs,
            long previewTtlSeconds,
            String transactionPolicy,
            String updatePolicy,
            String sheetMacDinh,
            List<TemplateColumnDto> danhSachCot,
            String policyNote
    ) {
    }

    public record PreviewRowDto(
            String sheet,
            int dong,
            Long dangKyId,
            Long sinhVienId,
            String maSinhVien,
            String hoTen,
            Double diemQuaTrinhHienTai,
            Double diemCuoiKyHienTai,
            Double diemTongKetHienTai,
            Double diemQuaTrinhMoi,
            Double diemCuoiKyMoi,
            Double diemTongKetDuKien,
            boolean hopLe,
            String hanhDongDuKien,
            List<ValidationErrorDetail> errors
    ) {
    }

    public record PreviewResponse(
            String previewToken,
            String fileSha256,
            Instant expiresAt,
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            String tenTep,
            long kichThuocTep,
            String sheetName,
            int tongSoDong,
            int soDongHopLe,
            int soDongLoi,
            int soTaoMoiDuKien,
            int soCapNhatDuKien,
            boolean coTheXacNhan,
            String transactionPolicy,
            String updatePolicy,
            String policyNote,
            List<PreviewRowDto> danhSachDong,
            List<ValidationErrorDetail> errors
    ) {
    }

    public record ConfirmImportRequest(
            @NotBlank(message = "Mã phiên xem trước (previewToken) không được để trống")
            String previewToken,
            String fileSha256
    ) {
    }

    public record ConfirmRowResultDto(
            int dong,
            Long dangKyId,
            Long sinhVienId,
            String maSinhVien,
            String hoTen,
            Double diemQuaTrinhCu,
            Double diemCuoiKyCu,
            Double diemTongKetCu,
            Double diemQuaTrinhMoi,
            Double diemCuoiKyMoi,
            Double diemTongKetMoi,
            String hanhDong
    ) {
    }

    public record ConfirmImportResponse(
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            String tenTep,
            String sheetName,
            String transactionPolicy,
            String updatePolicy,
            int tongSoDong,
            int soTaoMoi,
            int soCapNhat,
            int soThatBai,
            Instant importedAt,
            List<ConfirmRowResultDto> danhSachKetQua
    ) {
    }
}
