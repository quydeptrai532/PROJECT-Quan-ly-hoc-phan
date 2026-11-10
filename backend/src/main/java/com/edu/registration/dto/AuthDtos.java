package com.edu.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Schema(description = "Thông tin đăng nhập hệ thống")
    public record LoginRequest(
            @Schema(description = "Tên đăng nhập (mã sinh viên, giảng viên hoặc quản trị)", example = "admin")
            @NotBlank(message = "Vui lòng nhập tên đăng nhập") String username,
            @Schema(description = "Mật khẩu tài khoản", example = "admin123")
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    @Schema(description = "Phản hồi xác thực đăng nhập thành công")
    public record LoginResponse(
            @Schema(description = "Mã token xác thực JWT Bearer", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            String token,
            @Schema(description = "Vai trò người dùng trong hệ thống (QUAN_TRI, GIANG_VIEN, SINH_VIEN)", example = "QUAN_TRI")
            String role,
            @Schema(description = "Họ và tên người dùng", example = "Quản trị viên")
            String hoTen,
            @Schema(description = "Mã định danh cán bộ/sinh viên", example = "ADMIN001")
            String ma,
            @Schema(description = "Tên đăng nhập", example = "admin")
            String username) {
    }

    @Schema(description = "Thông tin hồ sơ tài khoản hiện tại")
    public record ProfileDto(
            @Schema(description = "Tên đăng nhập", example = "sv001")
            String username,
            @Schema(description = "Vai trò người dùng", example = "SINH_VIEN")
            String role,
            @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
            String hoTen,
            @Schema(description = "Mã sinh viên hoặc mã giảng viên", example = "SV20260001")
            String ma,
            @Schema(description = "Địa chỉ email liên hệ", example = "sinhvien@example.edu.vn")
            String email,
            @Schema(description = "Số điện thoại liên lạc", example = "0900000001")
            String soDienThoai) {
    }

    /**
     * Self-service profile update. Only these fields are accepted (see
     * {@code AuthService.SELF_EDITABLE_PROFILE_FIELDS}); unknown properties such as role or code are ignored.
     */
    @Schema(description = "Yêu cầu cập nhật hồ sơ cá nhân")
    public record UpdateProfileRequest(
            @Schema(description = "Họ và tên mới", example = "Nguyễn Văn A")
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự") String hoTen,
            @Schema(description = "Email liên hệ mới", example = "sinhvien@example.edu.vn")
            @Email(message = "Email không hợp lệ")
            @Size(max = 150, message = "Email tối đa 150 ký tự") String email,
            @Schema(description = "Số điện thoại liên lạc mới", example = "0900000001")
            @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự") String soDienThoai) {
    }

    @Schema(description = "Yêu cầu thay đổi mật khẩu")
    public record ChangePasswordRequest(
            @Schema(description = "Mật khẩu hiện tại", example = "CurrentP@ss1")
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String matKhauCu,
            @Schema(description = "Mật khẩu mới (tối thiểu 6 ký tự)", example = "NewSecureP@ss2")
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 ký tự") String matKhauMoi) {
    }
}
