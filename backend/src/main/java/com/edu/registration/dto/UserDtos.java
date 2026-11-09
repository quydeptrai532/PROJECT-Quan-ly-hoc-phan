package com.edu.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record SinhVienDto(Long id, String ma, String hoTen, String email, String soDienThoai,
                              Long khoaId, String khoaTen, Long nganhId, String nganhTen,
                              Long lopId, String lopTen, String username, boolean trangThai) {
    }

    public record SinhVienRequest(
            @NotBlank(message = "Mã sinh viên không được để trống") String ma,
            @NotBlank(message = "Họ tên không được để trống") String hoTen,
            @Email(message = "Email không hợp lệ") String email,
            String soDienThoai,
            Long khoaId,
            Long nganhId,
            Long lopId,
            @NotBlank(message = "Tên đăng nhập không được để trống") String username,
            String matKhau) {
    }

    public record GiangVienDto(Long id, String ma, String hoTen, String email, String soDienThoai,
                               Long khoaId, String khoaTen, String username, boolean trangThai) {
    }

    public record GiangVienRequest(
            @NotBlank(message = "Mã giảng viên không được để trống") String ma,
            @NotBlank(message = "Họ tên không được để trống") String hoTen,
            @Email(message = "Email không hợp lệ") String email,
            String soDienThoai,
            Long khoaId,
            @NotBlank(message = "Tên đăng nhập không được để trống") String username,
            String matKhau) {
    }

    public record TrangThaiRequest(boolean trangThai) {
    }

    public record ImportResultDto(
            int thanhCong,
            List<String> loi,
            int tongSoDong,
            int soTaoMoi,
            int soCapNhat,
            int soThatBai,
            String transactionPolicy,
            String idempotencyPolicy,
            List<UserImportRowDetailDto> danhSachKetQua
    ) {
        public ImportResultDto(int thanhCong, List<String> loi) {
            this(thanhCong, loi, thanhCong + (loi != null ? loi.size() : 0), thanhCong, 0,
                    loi != null ? loi.size() : 0, "ROW_BY_ROW", "SKIP_IF_EXISTS", List.of());
        }
    }

    public record UserImportRowDetailDto(
            int dong,
            String ma,
            String hoTen,
            String email,
            String soDienThoai,
            Long khoaId,
            String khoaTen,
            Long nganhId,
            String nganhTen,
            Long lopId,
            String lopTen,
            String username,
            boolean hopLe,
            String trangThai,
            String hanhDong,
            List<ValidationErrorDetail> errors
    ) {
    }

    public record UserImportPreviewResponse(
            String previewToken,
            String fileSha256,
            Instant expiresAt,
            String tenTep,
            int tongSoDong,
            int soDongHopLe,
            int soDongLoi,
            int soTaoMoiDuKien,
            int soCapNhatDuKien,
            boolean coTheXacNhan,
            String transactionPolicy,
            String idempotencyPolicy,
            List<UserImportRowDetailDto> danhSachDong,
            List<ValidationErrorDetail> errors
    ) {
    }

    public record UserConfirmImportRequest(
            @NotBlank(message = "Mã phiên xem trước (previewToken) không được để trống")
            String previewToken,
            String fileSha256
    ) {
    }
}
