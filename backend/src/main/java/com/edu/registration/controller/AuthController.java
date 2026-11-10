package com.edu.registration.controller;

import com.edu.registration.dto.AuthDtos;
import com.edu.registration.dto.MessageResponse;
import com.edu.registration.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Xác thực (Authentication)", description = "Đăng nhập, xem và cập nhật hồ sơ cá nhân, đổi mật khẩu tài khoản")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Đăng nhập hệ thống", description = "Xác thực tài khoản người dùng bằng tên đăng nhập và mật khẩu, trả về access token JWT và vai trò")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Truy xuất thông tin hồ sơ của tài khoản đang đăng nhập từ JWT Bearer token")
    public AuthDtos.ProfileDto me() {
        return authService.profile();
    }

    @PutMapping("/me")
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cập nhật họ tên, email và số điện thoại liên lạc của người dùng hiện tại")
    public AuthDtos.ProfileDto capNhatHoSo(@Valid @RequestBody AuthDtos.UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }

    @PutMapping("/doi-mat-khau")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Kiểm tra mật khẩu cũ và cập nhật mật khẩu mới cho tài khoản hiện tại")
    public MessageResponse doiMatKhau(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return new MessageResponse("Đổi mật khẩu thành công");
    }
}
