package com.edu.registration.service;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.entity.StudentAcademicStatus;
import com.edu.registration.entity.StudentAcademicStatusHistory;
import com.edu.registration.repository.StudentAcademicStatusHistoryRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.security.UserAuthCache;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private static final String MAT_KHAU_MAC_DINH = "123456";

    private final UserRepository userRepository;
    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final AdministrativeClassRepository lopHanhChinhRepository;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper mapper;
    private final TransactionTemplate transactionTemplate;
    private final UserAuthCache userAuthCache;
    private final AuditLogService auditLogService;
    private final UserImportService userImportService;
    private final StudentAcademicStatusHistoryRepository studentAcademicStatusHistoryRepository;
    private final CurrentUser currentUser;

    public UserService(UserRepository userRepository, StudentRepository sinhVienRepository,
                       LecturerRepository giangVienRepository, FacultyRepository khoaRepository,
                       MajorRepository nganhRepository, AdministrativeClassRepository lopHanhChinhRepository,
                       PasswordEncoder passwordEncoder, DtoMapper mapper, TransactionTemplate transactionTemplate,
                       UserAuthCache userAuthCache, AuditLogService auditLogService,
                       UserImportService userImportService) {
        this(userRepository, sinhVienRepository, giangVienRepository, khoaRepository,
                nganhRepository, lopHanhChinhRepository, passwordEncoder, mapper, transactionTemplate,
                userAuthCache, auditLogService, userImportService, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(UserRepository userRepository, StudentRepository sinhVienRepository,
                       LecturerRepository giangVienRepository, FacultyRepository khoaRepository,
                       MajorRepository nganhRepository, AdministrativeClassRepository lopHanhChinhRepository,
                       PasswordEncoder passwordEncoder, DtoMapper mapper, TransactionTemplate transactionTemplate,
                       UserAuthCache userAuthCache, AuditLogService auditLogService,
                       UserImportService userImportService,
                       StudentAcademicStatusHistoryRepository studentAcademicStatusHistoryRepository,
                       CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
        this.transactionTemplate = transactionTemplate;
        this.userAuthCache = userAuthCache;
        this.auditLogService = auditLogService;
        this.userImportService = userImportService;
        this.studentAcademicStatusHistoryRepository = studentAcademicStatusHistoryRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<UserDtos.SinhVienDto> danhSachSinhVien(String keyword) {
        List<Student> danhSach = (keyword == null || keyword.isBlank())
                ? sinhVienRepository.findAllByOrderByMaAsc()
                : sinhVienRepository.findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(
                        keyword.trim(), keyword.trim());
        return danhSach.stream().map(mapper::sinhVien).toList();
    }

    @Transactional
    public UserDtos.SinhVienDto taoSinhVien(UserDtos.SinhVienRequest request) {
        return taoSinhVienTuRecord(request.ma(), request.hoTen(), request.email(), request.soDienThoai(),
                request.khoaId(), request.nganhId(), request.lopId(), request.username(), request.matKhau());
    }

    @Transactional
    public UserDtos.SinhVienDto suaSinhVien(Long id, UserDtos.SinhVienRequest request) {
        return auditLogService.execute("STUDENT_ACCOUNT_UPDATED", "STUDENT_ACCOUNT", id, audit -> {
            Student sinhVien = sinhVienRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Sinh viên không tồn tại"));
            Map<String, Object> before = accountSnapshot(
                    sinhVien.getKhoa() == null ? null : sinhVien.getKhoa().getId(),
                    sinhVien.getNganh() == null ? null : sinhVien.getNganh().getId(),
                    sinhVien.getLopHanhChinh() == null ? null : sinhVien.getLopHanhChinh().getId());
            String ma = request.ma().trim();
            if (!sinhVien.getMa().equalsIgnoreCase(ma) && sinhVienRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã sinh viên đã tồn tại");
            }
            sinhVien.setMa(ma);
            sinhVien.setHoTen(request.hoTen().trim());
            sinhVien.setEmail(request.email());
            sinhVien.setSoDienThoai(request.soDienThoai());
            sinhVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
            sinhVien.setNganh(request.nganhId() != null ? timNganh(request.nganhId()) : null);
            sinhVien.setLopHanhChinh(request.lopId() != null ? timLop(request.lopId()) : null);
            if (request.matKhau() != null && !request.matKhau().isBlank()) {
                sinhVien.getUser().setPassword(passwordEncoder.encode(request.matKhau()));
            }
            audit.changes(before, accountSnapshot(
                    sinhVien.getKhoa() == null ? null : sinhVien.getKhoa().getId(),
                    sinhVien.getNganh() == null ? null : sinhVien.getNganh().getId(),
                    sinhVien.getLopHanhChinh() == null ? null : sinhVien.getLopHanhChinh().getId()));
            return mapper.sinhVien(sinhVien);
        });
    }

    @Transactional
    public UserDtos.SinhVienDto doiTrangThaiSinhVien(Long id, boolean trangThai) {
        return auditLogService.execute("STUDENT_ACCOUNT_STATUS_CHANGED", "STUDENT_ACCOUNT", id, audit -> {
            Student sinhVien = sinhVienRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Sinh viên không tồn tại"));
            boolean before = sinhVien.getUser().isTrangThai();
            sinhVien.getUser().setTrangThai(trangThai);
            userAuthCache.evict(sinhVien.getUser().getUsername());
            audit.changes(Map.of("active", before), Map.of("active", trangThai));
            return mapper.sinhVien(sinhVien);
        });
    }

    @Transactional
    public UserDtos.SinhVienDto capNhatTrangThaiHocTap(Long id, UserDtos.CapNhatTrangThaiHocTapRequest request) {
        if (request == null || request.trangThaiMoi() == null) {
            throw ApiException.badRequest("Trạng thái học tập mới không được để trống");
        }
        if (request.ngayHieuLuc() == null) {
            throw ApiException.badRequest("Ngày hiệu lực không được để trống");
        }
        if (request.lyDo() == null || request.lyDo().trim().isEmpty()) {
            throw ApiException.badRequest("Lý do cập nhật trạng thái không được để trống");
        }
        if (request.lyDo().trim().length() > 500) {
            throw ApiException.badRequest("Lý do không được vượt quá 500 ký tự");
        }

        return auditLogService.execute("STUDENT_ACADEMIC_STATUS_CHANGED", "STUDENT", id, audit -> {
            Student sinhVien = sinhVienRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Sinh viên không tồn tại"));

            StudentAcademicStatus trangThaiTruoc = sinhVien.getTrangThaiHocTap();
            StudentAcademicStatus trangThaiMoi = request.trangThaiMoi();

            if (trangThaiTruoc == trangThaiMoi) {
                throw ApiException.badRequest("Trạng thái mới phải khác trạng thái học tập hiện tại của sinh viên");
            }

            User nguoiThucHien = currentUser != null ? currentUser.get() : null;
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            StudentAcademicStatusHistory history = new StudentAcademicStatusHistory(
                    sinhVien,
                    trangThaiTruoc,
                    trangThaiMoi,
                    request.ngayHieuLuc(),
                    request.lyDo().trim(),
                    nguoiThucHien,
                    now
            );
            studentAcademicStatusHistoryRepository.save(history);

            sinhVien.setTrangThaiHocTap(trangThaiMoi);

            Map<String, Object> before = Map.of(
                    "trangThaiHocTap", trangThaiTruoc != null ? trangThaiTruoc.name() : "DANG_HOC"
            );
            Map<String, Object> after = Map.of(
                    "trangThaiHocTap", trangThaiMoi.name(),
                    "ngayHieuLuc", request.ngayHieuLuc().toString(),
                    "lyDo", request.lyDo().trim()
            );
            audit.changes(before, after);

            return mapper.sinhVien(sinhVien);
        });
    }

    @Transactional(readOnly = true)
    public List<UserDtos.StudentAcademicStatusHistoryDto> layLichSuTrangThaiHocTap(Long sinhVienId) {
        if (!sinhVienRepository.existsById(sinhVienId)) {
            throw ApiException.notFound("Sinh viên không tồn tại");
        }
        List<StudentAcademicStatusHistory> list = studentAcademicStatusHistoryRepository
                .findBySinhVienIdOrderByThoiDiemDescIdDesc(sinhVienId);
        return list.stream().map(mapper::lichSuTrangThaiHocTap).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.StudentAcademicStatusHistoryDto> layLichSuTrangThaiHocTapSinhVienHienTai() {
        if (currentUser == null) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        Student sinhVien = sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        List<StudentAcademicStatusHistory> list = studentAcademicStatusHistoryRepository
                .findBySinhVienIdOrderByThoiDiemDescIdDesc(sinhVien.getId());
        return list.stream().map(mapper::lichSuTrangThaiHocTap).toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.GiangVienDto> danhSachGiangVien(String keyword) {
        List<Lecturer> danhSach = (keyword == null || keyword.isBlank())
                ? giangVienRepository.findAllByOrderByMaAsc()
                : giangVienRepository.findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(
                        keyword.trim(), keyword.trim());
        return danhSach.stream().map(mapper::giangVien).toList();
    }

    @Transactional
    public UserDtos.GiangVienDto taoGiangVien(UserDtos.GiangVienRequest request) {
        return auditLogService.execute("LECTURER_ACCOUNT_CREATED", "LECTURER_ACCOUNT", null, audit -> {
            String ma = request.ma().trim();
            if (giangVienRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã giảng viên đã tồn tại");
            }
            User user = taoUser(request.username(), request.matKhau(), Role.GIANG_VIEN);
            Lecturer giangVien = new Lecturer();
            giangVien.setUser(user);
            giangVien.setMa(ma);
            giangVien.setHoTen(request.hoTen().trim());
            giangVien.setEmail(request.email());
            giangVien.setSoDienThoai(request.soDienThoai());
            giangVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
            Lecturer saved = giangVienRepository.save(giangVien);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), lecturerSnapshot(saved.getKhoa() == null ? null : saved.getKhoa().getId()));
            return mapper.giangVien(saved);
        });
    }

    @Transactional
    public UserDtos.GiangVienDto suaGiangVien(Long id, UserDtos.GiangVienRequest request) {
        return auditLogService.execute("LECTURER_ACCOUNT_UPDATED", "LECTURER_ACCOUNT", id, audit -> {
            Lecturer giangVien = giangVienRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
            Map<String, Object> before = lecturerSnapshot(
                    giangVien.getKhoa() == null ? null : giangVien.getKhoa().getId());
            String ma = request.ma().trim();
            if (!giangVien.getMa().equalsIgnoreCase(ma) && giangVienRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã giảng viên đã tồn tại");
            }
            giangVien.setMa(ma);
            giangVien.setHoTen(request.hoTen().trim());
            giangVien.setEmail(request.email());
            giangVien.setSoDienThoai(request.soDienThoai());
            giangVien.setKhoa(request.khoaId() != null ? timKhoa(request.khoaId()) : null);
            if (request.matKhau() != null && !request.matKhau().isBlank()) {
                giangVien.getUser().setPassword(passwordEncoder.encode(request.matKhau()));
            }
            audit.changes(before, lecturerSnapshot(
                    giangVien.getKhoa() == null ? null : giangVien.getKhoa().getId()));
            return mapper.giangVien(giangVien);
        });
    }

    @Transactional
    public UserDtos.GiangVienDto doiTrangThaiGiangVien(Long id, boolean trangThai) {
        return auditLogService.execute("LECTURER_ACCOUNT_STATUS_CHANGED", "LECTURER_ACCOUNT", id, audit -> {
            Lecturer giangVien = giangVienRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
            boolean before = giangVien.getUser().isTrangThai();
            giangVien.getUser().setTrangThai(trangThai);
            userAuthCache.evict(giangVien.getUser().getUsername());
            audit.changes(Map.of("active", before), Map.of("active", trangThai));
            return mapper.giangVien(giangVien);
        });
    }

    public UserDtos.UserImportConfigDto layCauHinhImportSinhVien() {
        return userImportService.layCauHinhImport(UserImportService.TargetType.SINH_VIEN);
    }

    public UserDtos.UserImportConfigDto layCauHinhImportGiangVien() {
        return userImportService.layCauHinhImport(UserImportService.TargetType.GIANG_VIEN);
    }

    public UserDtos.ImportResultDto importSinhVien(MultipartFile file) {
        return userImportService.directImport(UserImportService.TargetType.SINH_VIEN, file);
    }

    public UserDtos.UserImportPreviewResponse previewImportSinhVien(MultipartFile file) {
        return userImportService.preview(UserImportService.TargetType.SINH_VIEN, file);
    }

    public UserDtos.ImportResultDto confirmImportSinhVien(UserDtos.UserConfirmImportRequest request) {
        return userImportService.confirm(UserImportService.TargetType.SINH_VIEN, request);
    }

    public byte[] getSinhVienTemplateCsv() {
        return userImportService.xuatTemplateCsv(UserImportService.TargetType.SINH_VIEN);
    }

    public UserDtos.ImportResultDto importGiangVien(MultipartFile file) {
        return userImportService.directImport(UserImportService.TargetType.GIANG_VIEN, file);
    }

    public UserDtos.UserImportPreviewResponse previewImportGiangVien(MultipartFile file) {
        return userImportService.preview(UserImportService.TargetType.GIANG_VIEN, file);
    }

    public UserDtos.ImportResultDto confirmImportGiangVien(UserDtos.UserConfirmImportRequest request) {
        return userImportService.confirm(UserImportService.TargetType.GIANG_VIEN, request);
    }

    public byte[] getGiangVienTemplateCsv() {
        return userImportService.xuatTemplateCsv(UserImportService.TargetType.GIANG_VIEN);
    }

    public byte[] exportErrorReport(List<UserDtos.UserImportRowDetailDto> rows) {
        return userImportService.xuatFileLoiCsv(rows);
    }

    private UserDtos.SinhVienDto taoSinhVienTuRecord(String ma, String hoTen, String email, String soDienThoai,
                                                     Long khoaId, Long nganhId, Long lopId,
                                                     String username, String matKhau) {
        return auditLogService.execute("STUDENT_ACCOUNT_CREATED", "STUDENT_ACCOUNT", null, audit -> {
            if (ma == null || ma.isBlank()) {
                throw ApiException.badRequest("Mã sinh viên không được để trống");
            }
            if (hoTen == null || hoTen.isBlank()) {
                throw ApiException.badRequest("Họ tên không được để trống");
            }
            if (username == null || username.isBlank()) {
                throw ApiException.badRequest("Tên đăng nhập không được để trống");
            }
            String maChuan = ma.trim();
            if (sinhVienRepository.existsByMa(maChuan)) {
                throw ApiException.conflict("Mã sinh viên đã tồn tại: " + maChuan);
            }

            User user = taoUser(username, matKhau, Role.SINH_VIEN);
            Student sinhVien = new Student();
            sinhVien.setUser(user);
            sinhVien.setMa(maChuan);
            sinhVien.setHoTen(hoTen.trim());
            sinhVien.setEmail(email);
            sinhVien.setSoDienThoai(soDienThoai);
            sinhVien.setKhoa(khoaId != null ? timKhoa(khoaId) : null);
            sinhVien.setNganh(nganhId != null ? timNganh(nganhId) : null);
            sinhVien.setLopHanhChinh(lopId != null ? timLop(lopId) : null);
            Student saved = sinhVienRepository.save(sinhVien);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), accountSnapshot(
                    saved.getKhoa() == null ? null : saved.getKhoa().getId(),
                    saved.getNganh() == null ? null : saved.getNganh().getId(),
                    saved.getLopHanhChinh() == null ? null : saved.getLopHanhChinh().getId()));
            return mapper.sinhVien(saved);
        });
    }

    private User taoUser(String username, String matKhau, Role role) {
        String tenDangNhap = username.trim();
        if (userRepository.existsByUsername(tenDangNhap)) {
            throw ApiException.conflict("Tên đăng nhập đã tồn tại: " + tenDangNhap);
        }
        User user = new User();
        user.setUsername(tenDangNhap);
        user.setPassword(passwordEncoder.encode(
                matKhau == null || matKhau.isBlank() ? MAT_KHAU_MAC_DINH : matKhau));
        user.setRole(role);
        user.setTrangThai(true);
        return userRepository.save(user);
    }

    private Long parseLong(CSVRecord record, int index) {
        if (record.size() <= index) {
            return null;
        }
        String value = record.get(index);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw ApiException.badRequest("Giá trị số không hợp lệ: " + value);
        }
    }

    private Faculty timKhoa(Long id) {
        return khoaRepository.findById(id).orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
    }

    private Major timNganh(Long id) {
        return nganhRepository.findById(id).orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
    }

    private AdministrativeClass timLop(Long id) {
        return lopHanhChinhRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại"));
    }

    private Map<String, Object> accountSnapshot(Long khoaId, Long nganhId, Long lopId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("khoaId", khoaId);
        snapshot.put("nganhId", nganhId);
        snapshot.put("lopId", lopId);
        return snapshot;
    }

    private Map<String, Object> lecturerSnapshot(Long khoaId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("khoaId", khoaId);
        return snapshot;
    }
}
