package com.edu.registration.service;

import com.edu.registration.dto.AuthDtos;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.security.JwtService;
import com.edu.registration.security.UserAuthCache;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /**
     * Fields a student/lecturer may change on their own profile via {@code PUT /api/auth/me}.
     * Role, user code, faculty, class assignments and account status remain admin-managed.
     */
    public static final java.util.List<String> SELF_EDITABLE_PROFILE_FIELDS =
            java.util.List.of("hoTen", "email", "soDienThoai");

    private final UserRepository userRepository;
    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final UserAuthCache userAuthCache;

    public AuthService(UserRepository userRepository, StudentRepository sinhVienRepository,
                       LecturerRepository giangVienRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser,
                       AuditLogService auditLogService, UserAuthCache userAuthCache) {
        this.userRepository = userRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.userAuthCache = userAuthCache;
    }

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByUsername(request.username().trim()).orElse(null);
        if (user == null) {
            auditLogService.recordLoginFailure(null, request.username(), "UNKNOWN_USER");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            auditLogService.recordLoginFailure(user, user.getUsername(), "BAD_CREDENTIALS");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng");
        }
        if (!user.isTrangThai()) {
            auditLogService.recordLoginFailure(user, user.getUsername(), "ACCOUNT_LOCKED");
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo");
        }

        String hoTen = "";
        String ma = "";
        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            Student sv = sinhVienRepository.findByUser(user).orElse(null);
            if (sv != null) {
                hoTen = sv.getHoTen();
                ma = sv.getMa();
            }
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(user).orElse(null);
            if (gv != null) {
                hoTen = gv.getHoTen();
                ma = gv.getMa();
            }
        } else {
            hoTen = "Quản trị viên";
            ma = "ADMIN";
        }

        String sessionId = java.util.UUID.randomUUID().toString();
        user.setCurrentSessionId(sessionId);
        userRepository.save(user);
        userAuthCache.evict(user.getUsername());

        return new AuthDtos.LoginResponse(jwtService.generateToken(user, sessionId), user.getRole().name(),
                hoTen, ma, user.getUsername());
    }

    @Transactional
    public void logout(String sessionId) {
        User user = currentUser.get();
        if (sessionId != null && sessionId.equals(user.getCurrentSessionId())) {
            user.setCurrentSessionId(java.util.UUID.randomUUID().toString());
            userRepository.save(user);
        }
        userAuthCache.evict(user.getUsername());
    }

    @Transactional(readOnly = true)
    public AuthDtos.ProfileDto profile() {
        User user = currentUser.get();
        String hoTen = "";
        String ma = "";
        String email = null;
        String soDienThoai = null;

        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            Student sv = sinhVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
            hoTen = sv.getHoTen();
            ma = sv.getMa();
            email = sv.getEmail();
            soDienThoai = sv.getSoDienThoai();
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            hoTen = gv.getHoTen();
            ma = gv.getMa();
            email = gv.getEmail();
            soDienThoai = gv.getSoDienThoai();
        } else {
            hoTen = "Quản trị viên";
            ma = "ADMIN";
        }

        return new AuthDtos.ProfileDto(user.getUsername(), user.getRole().name(), hoTen, ma, email, soDienThoai);
    }

    @Transactional
    public AuthDtos.ProfileDto updateProfile(AuthDtos.UpdateProfileRequest request) {
        User user = currentUser.get();
        if (user.getRole() == com.edu.registration.entity.Role.SINH_VIEN) {
            Student sv = sinhVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
            sv.setHoTen(request.hoTen().trim());
            sv.setEmail(request.email());
            sv.setSoDienThoai(request.soDienThoai());
        } else if (user.getRole() == com.edu.registration.entity.Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            gv.setHoTen(request.hoTen().trim());
            gv.setEmail(request.email());
            gv.setSoDienThoai(request.soDienThoai());
        }
        return profile();
    }

    @Transactional
    public void changePassword(AuthDtos.ChangePasswordRequest request) {
        User user = currentUser.get();
        if (!passwordEncoder.matches(request.matKhauCu(), user.getPassword())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(request.matKhauMoi()));
        userAuthCache.evict(user.getUsername());
    }
}