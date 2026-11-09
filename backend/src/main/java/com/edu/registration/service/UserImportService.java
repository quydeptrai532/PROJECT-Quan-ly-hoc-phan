package com.edu.registration.service;

import com.edu.registration.dto.UserDtos;
import com.edu.registration.dto.ValidationErrorDetail;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.util.CsvUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Service
public class UserImportService {

    private static final Logger log = LoggerFactory.getLogger(UserImportService.class);
    private static final String MAT_KHAU_MAC_DINH = "123456";
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final long PREVIEW_TTL_SECONDS = 900; // 15 phút
    public static final String TRANSACTION_POLICY = "ROW_BY_ROW";
    public static final String IDEMPOTENCY_POLICY = "SKIP_IF_EXISTS";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{7,20}$");

    public enum TargetType {
        SINH_VIEN,
        GIANG_VIEN
    }

    private final UserRepository userRepository;
    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final AdministrativeClassRepository lopHanhChinhRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final TransactionTemplate transactionTemplate;
    private Clock clock;

    private final ConcurrentHashMap<String, UserImportSession> previewSessions = new ConcurrentHashMap<>();

    public UserImportService(
            UserRepository userRepository,
            StudentRepository sinhVienRepository,
            LecturerRepository giangVienRepository,
            FacultyRepository khoaRepository,
            MajorRepository nganhRepository,
            AdministrativeClassRepository lopHanhChinhRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            TransactionTemplate transactionTemplate
    ) {
        this.userRepository = userRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.transactionTemplate = transactionTemplate;
        this.clock = Clock.systemUTC();
    }

    public void setClock(Clock clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }

    // --- TEMPLATE EXPORT ---

    public byte[] xuatTemplateCsv(TargetType targetType) {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.BOM);
        if (targetType == TargetType.SINH_VIEN) {
            sb.append("ma,hoTen,email,khoaId,nganhId,lopId").append(CsvUtils.XUONG_DONG);
            sb.append("SV010,Nguyen Van A,a@student.edu.vn,1,1,1").append(CsvUtils.XUONG_DONG);
        } else {
            sb.append("ma,hoTen,email,soDienThoai,khoaId").append(CsvUtils.XUONG_DONG);
            sb.append("GV010,Nguyen Van B,b@lecturer.edu.vn,0912345678,1").append(CsvUtils.XUONG_DONG);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- PREVIEW ---

    @Transactional(readOnly = true)
    public UserDtos.UserImportPreviewResponse preview(TargetType targetType, MultipartFile file) {
        byte[] bytes = validateFile(file);
        String sha256 = tinhSha256(bytes);
        String tenTep = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.csv";

        CleanParsedData parsed = parseAndValidate(targetType, bytes);
        cleanExpiredSessions();

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now(clock).plusSeconds(PREVIEW_TTL_SECONDS);

        UserImportSession session = new UserImportSession(
                token, sha256, tenTep, targetType, expiresAt,
                parsed.allRows(), parsed.validRows()
        );
        previewSessions.put(token, session);

        int tongSoDong = parsed.allRows().size();
        int soDongHopLe = parsed.validRows().size();
        int soDongLoi = tongSoDong - soDongHopLe;
        boolean coTheXacNhan = soDongHopLe > 0;

        List<ValidationErrorDetail> allErrors = new ArrayList<>();
        for (UserDtos.UserImportRowDetailDto row : parsed.allRows()) {
            if (row.errors() != null) {
                allErrors.addAll(row.errors());
            }
        }

        return new UserDtos.UserImportPreviewResponse(
                token,
                sha256,
                expiresAt,
                tenTep,
                tongSoDong,
                soDongHopLe,
                soDongLoi,
                soDongHopLe, // soTaoMoiDuKien
                0, // soCapNhatDuKien
                coTheXacNhan,
                TRANSACTION_POLICY,
                IDEMPOTENCY_POLICY,
                parsed.allRows(),
                allErrors
        );
    }

    // --- CONFIRM ---

    public UserDtos.ImportResultDto confirm(TargetType targetType, UserDtos.UserConfirmImportRequest request) {
        if (request == null || request.previewToken() == null || request.previewToken().isBlank()) {
            throw ApiException.badRequest("Mã phiên xem trước (previewToken) không hợp lệ");
        }
        cleanExpiredSessions();

        UserImportSession session = previewSessions.get(request.previewToken().trim());
        if (session == null) {
            throw ApiException.badRequest("Phiên xem trước không tồn tại hoặc đã hết hạn (TTL 15 phút)");
        }
        if (session.targetType != targetType) {
            throw ApiException.badRequest("Phiên xem trước không khớp với đối tượng import");
        }
        if (Instant.now(clock).isAfter(session.expiresAt)) {
            previewSessions.remove(session.token);
            throw ApiException.badRequest("Phiên xem trước đã hết hạn. Vui lòng tải lại file");
        }
        if (request.fileSha256() != null && !request.fileSha256().isBlank()
                && !session.fileSha256.equalsIgnoreCase(request.fileSha256().trim())) {
            throw ApiException.badRequest("Mã băm tệp (fileSha256) không khớp với phiên xem trước");
        }
        if (!session.consumed.compareAndSet(false, true)) {
            throw ApiException.badRequest("Phiên xem trước đã được xác nhận thực hiện trước đó");
        }

        return executeImport(targetType, session.allRows, session.fileName);
    }

    // --- DIRECT IMPORT (BACKWARD COMPATIBLE & 1-STEP) ---

    public UserDtos.ImportResultDto directImport(TargetType targetType, MultipartFile file) {
        byte[] bytes = validateFile(file);
        String tenTep = file.getOriginalFilename() != null ? file.getOriginalFilename() : "import.csv";
        CleanParsedData parsed = transactionTemplate.execute(status -> parseAndValidate(targetType, bytes));
        return executeImport(targetType, parsed.allRows(), tenTep);
    }

    // --- EXECUTE ROWS WITH ISOLATED ROW TRANSACTION ---

    private UserDtos.ImportResultDto executeImport(TargetType targetType,
                                                  List<UserDtos.UserImportRowDetailDto> allRows,
                                                  String tenTep) {
        int soTaoMoi = 0;
        int soThatBai = 0;
        List<String> loiList = new ArrayList<>();
        List<UserDtos.UserImportRowDetailDto> finalResults = new ArrayList<>();

        for (UserDtos.UserImportRowDetailDto row : allRows) {
            if (!row.hopLe()) {
                soThatBai++;
                String rowLoi = formatRowErrorSummary(row);
                loiList.add(rowLoi);
                finalResults.add(row);
                continue;
            }

            try {
                Boolean success = transactionTemplate.execute(status -> {
                    // Double check uniqueness inside transaction to avoid race condition
                    if (userRepository.existsByUsernameIgnoreCase(row.ma().trim())) {
                        throw ApiException.conflict("Tên đăng nhập đã tồn tại: " + row.ma().trim());
                    }
                    if (targetType == TargetType.SINH_VIEN) {
                        if (sinhVienRepository.existsByMaIgnoreCase(row.ma().trim())) {
                            throw ApiException.conflict("Mã sinh viên đã tồn tại: " + row.ma().trim());
                        }
                    } else {
                        if (giangVienRepository.existsByMaIgnoreCase(row.ma().trim())) {
                            throw ApiException.conflict("Mã giảng viên đã tồn tại: " + row.ma().trim());
                        }
                    }

                    User user = new User();
                    user.setUsername(row.ma().trim());
                    user.setPassword(passwordEncoder.encode(MAT_KHAU_MAC_DINH));
                    user.setRole(targetType == TargetType.SINH_VIEN ? Role.SINH_VIEN : Role.GIANG_VIEN);
                    user.setTrangThai(true);
                    User savedUser = userRepository.save(user);

                    if (targetType == TargetType.SINH_VIEN) {
                        Student sv = new Student();
                        sv.setUser(savedUser);
                        sv.setMa(row.ma().trim());
                        sv.setHoTen(row.hoTen().trim());
                        sv.setEmail(row.email());
                        sv.setSoDienThoai(row.soDienThoai());
                        sv.setKhoa(row.khoaId() != null ? khoaRepository.getReferenceById(row.khoaId()) : null);
                        sv.setNganh(row.nganhId() != null ? nganhRepository.getReferenceById(row.nganhId()) : null);
                        sv.setLopHanhChinh(row.lopId() != null ? lopHanhChinhRepository.getReferenceById(row.lopId()) : null);
                        Student savedSv = sinhVienRepository.save(sv);

                        auditLogService.execute("STUDENT_ACCOUNT_CREATED", "STUDENT_ACCOUNT", savedSv.getId(), audit -> {
                            audit.changes(Map.of(), accountSnapshot(row.khoaId(), row.nganhId(), row.lopId()));
                            return null;
                        });
                    } else {
                        Lecturer gv = new Lecturer();
                        gv.setUser(savedUser);
                        gv.setMa(row.ma().trim());
                        gv.setHoTen(row.hoTen().trim());
                        gv.setEmail(row.email());
                        gv.setSoDienThoai(row.soDienThoai());
                        gv.setKhoa(row.khoaId() != null ? khoaRepository.getReferenceById(row.khoaId()) : null);
                        Lecturer savedGv = giangVienRepository.save(gv);

                        auditLogService.execute("LECTURER_ACCOUNT_CREATED", "LECTURER_ACCOUNT", savedGv.getId(), audit -> {
                            audit.changes(Map.of(), lecturerSnapshot(row.khoaId()));
                            return null;
                        });
                    }

                    return true;
                });

                if (Boolean.TRUE.equals(success)) {
                    soTaoMoi++;
                    finalResults.add(new UserDtos.UserImportRowDetailDto(
                            row.dong(), row.ma(), row.hoTen(), row.email(), row.soDienThoai(),
                            row.khoaId(), row.khoaTen(), row.nganhId(), row.nganhTen(),
                            row.lopId(), row.lopTen(), row.username(),
                            true, "THANH_CONG", "TAO_MOI", List.of()
                    ));
                }
            } catch (Exception ex) {
                soThatBai++;
                String errMsg = ex.getMessage() != null ? ex.getMessage() : "Lỗi ghi dữ liệu";
                String rowLoi = "Dòng " + row.dong() + " (" + row.ma() + "): " + errMsg;
                loiList.add(rowLoi);
                ValidationErrorDetail errorDetail = new ValidationErrorDetail(
                        null, null, row.ma(), null, row.dong(), null, null, null, null, errMsg
                );
                finalResults.add(new UserDtos.UserImportRowDetailDto(
                        row.dong(), row.ma(), row.hoTen(), row.email(), row.soDienThoai(),
                        row.khoaId(), row.khoaTen(), row.nganhId(), row.nganhTen(),
                        row.lopId(), row.lopTen(), row.username(),
                        false, "LOI", "BO_QUA", List.of(errorDetail)
                ));
            }
        }

        // Ghi log tổng quan quá trình import
        String batchAction = targetType == TargetType.SINH_VIEN ? "STUDENT_IMPORTED_CSV" : "LECTURER_IMPORTED_CSV";
        String batchEntity = targetType == TargetType.SINH_VIEN ? "STUDENT_IMPORT" : "LECTURER_IMPORT";
        final int finalSoTaoMoi = soTaoMoi;
        final int finalSoThatBai = soThatBai;
        try {
            auditLogService.execute(batchAction, batchEntity, null, audit -> {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("tenTep", tenTep);
                details.put("tongSoDong", allRows.size());
                details.put("soTaoMoi", finalSoTaoMoi);
                details.put("soCapNhat", 0);
                details.put("soThatBai", finalSoThatBai);
                details.put("transactionPolicy", TRANSACTION_POLICY);
                details.put("updatePolicy", IDEMPOTENCY_POLICY);
                audit.changes(Map.of(), details);
                return null;
            });
        } catch (Exception ex) {
            log.warn("Không thể ghi audit log batch import: {}", ex.getMessage());
        }

        return new UserDtos.ImportResultDto(
                soTaoMoi,
                loiList,
                allRows.size(),
                soTaoMoi,
                0,
                soThatBai,
                TRANSACTION_POLICY,
                IDEMPOTENCY_POLICY,
                finalResults
        );
    }

    private String formatRowErrorSummary(UserDtos.UserImportRowDetailDto row) {
        if (row.errors() == null || row.errors().isEmpty()) {
            return "Dòng " + row.dong() + " (" + (row.ma() != null ? row.ma() : "?") + "): dữ liệu không hợp lệ";
        }
        List<String> details = new ArrayList<>();
        for (ValidationErrorDetail err : row.errors()) {
            String col = err.cot() != null ? err.cot() : err.field();
            details.add((col != null ? "[" + col + "] " : "") + err.message());
        }
        return "Dòng " + row.dong() + " (" + (row.ma() != null ? row.ma() : "?") + "): " + String.join("; ", details);
    }

    // --- PARSE AND VALIDATE CSV ---

    private CleanParsedData parseAndValidate(TargetType targetType, byte[] bytes) {
        List<UserDtos.UserImportRowDetailDto> allRows = new ArrayList<>();
        List<UserDtos.UserImportRowDetailDto> validRows = new ArrayList<>();

        Set<String> seenMaInFile = new HashSet<>();
        Set<String> seenEmailInFile = new HashSet<>();

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build()
                     .parse(reader)) {

            var iterator = parser.iterator();
            if (!iterator.hasNext()) {
                throw ApiException.badRequest("File CSV rỗng, không có dữ liệu tiêu đề");
            }

            CSVRecord headerRecord = iterator.next();
            Map<String, Integer> colMap = buildColumnMapping(headerRecord);
            validateHeaderStructure(targetType, colMap);

            while (iterator.hasNext()) {
                CSVRecord record = iterator.next();
                int rowNumber = (int) record.getRecordNumber();

                // Skip completely empty lines
                if (isRecordEmpty(record)) {
                    continue;
                }

                List<ValidationErrorDetail> rowErrors = new ArrayList<>();

                String ma = getVal(record, colMap.get("ma"));
                String hoTen = getVal(record, colMap.get("hoten"));
                String email = getVal(record, colMap.get("email"));
                String soDienThoai = getVal(record, colMap.get("sodienthoai"));
                String khoaVal = getVal(record, colMap.get("khoaid"));
                String nganhVal = getVal(record, colMap.get("nganhid"));
                String lopVal = getVal(record, colMap.get("lopid"));

                // Validate ma
                if (ma == null || ma.isBlank()) {
                    rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "ma", "ma", "ma", ma, "Mã không được để trống"));
                } else {
                    String maClean = ma.trim();
                    if (maClean.length() > 20) {
                        rowErrors.add(new ValidationErrorDetail(null, null, maClean, null, rowNumber, "ma", "ma", "ma", maClean, "Mã không được vượt quá 20 ký tự"));
                    }
                    String maLower = maClean.toLowerCase(Locale.ROOT);
                    if (seenMaInFile.contains(maLower)) {
                        rowErrors.add(new ValidationErrorDetail(null, null, maClean, null, rowNumber, "ma", "ma", "ma", maClean, "Mã trùng lặp với dòng trước trong cùng file import"));
                    } else {
                        seenMaInFile.add(maLower);
                        if (userRepository.existsByUsernameIgnoreCase(maClean)) {
                            rowErrors.add(new ValidationErrorDetail(null, null, maClean, null, rowNumber, "ma", "ma", "ma", maClean, "Tên đăng nhập đã tồn tại trong hệ thống (không tự ghi đè tài khoản)"));
                        } else if (targetType == TargetType.SINH_VIEN && sinhVienRepository.existsByMaIgnoreCase(maClean)) {
                            rowErrors.add(new ValidationErrorDetail(null, null, maClean, null, rowNumber, "ma", "ma", "ma", maClean, "Mã sinh viên đã tồn tại trong hệ thống (không tự ghi đè tài khoản)"));
                        } else if (targetType == TargetType.GIANG_VIEN && giangVienRepository.existsByMaIgnoreCase(maClean)) {
                            rowErrors.add(new ValidationErrorDetail(null, null, maClean, null, rowNumber, "ma", "ma", "ma", maClean, "Mã giảng viên đã tồn tại trong hệ thống (không tự ghi đè tài khoản)"));
                        }
                    }
                }

                // Validate hoTen
                if (hoTen == null || hoTen.isBlank()) {
                    rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "hoTen", "hoTen", "hoTen", hoTen, "Họ tên không được để trống"));
                } else if (hoTen.trim().length() > 150) {
                    rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "hoTen", "hoTen", "hoTen", hoTen, "Họ tên không được vượt quá 150 ký tự"));
                }

                // Validate email
                if (email == null || email.isBlank()) {
                    rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "email", "email", "email", email, "Email không được để trống"));
                } else {
                    String emailClean = email.trim();
                    if (emailClean.length() > 150) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "email", "email", "email", emailClean, "Email không được vượt quá 150 ký tự"));
                    }
                    if (!EMAIL_PATTERN.matcher(emailClean).matches()) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "email", "email", "email", emailClean, "Email không đúng định dạng chuẩn"));
                    }
                    String emailLower = emailClean.toLowerCase(Locale.ROOT);
                    if (seenEmailInFile.contains(emailLower)) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "email", "email", "email", emailClean, "Email trùng lặp với dòng trước trong cùng file import"));
                    } else {
                        seenEmailInFile.add(emailLower);
                        if (sinhVienRepository.existsByEmailIgnoreCase(emailClean) || giangVienRepository.existsByEmailIgnoreCase(emailClean)) {
                            rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "email", "email", "email", emailClean, "Email đã được sử dụng bởi người dùng khác trong hệ thống"));
                        }
                    }
                }

                // Validate soDienThoai
                if (soDienThoai != null && !soDienThoai.isBlank()) {
                    String sdtClean = soDienThoai.trim();
                    if (sdtClean.length() > 20) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "soDienThoai", "soDienThoai", "soDienThoai", sdtClean, "Số điện thoại không được vượt quá 20 ký tự"));
                    }
                    if (!PHONE_PATTERN.matcher(sdtClean).matches()) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "soDienThoai", "soDienThoai", "soDienThoai", sdtClean, "Số điện thoại chứa ký tự không hợp lệ"));
                    }
                }

                // Validate Khoa reference
                Long khoaId = null;
                Faculty khoaEntity = null;
                if (khoaVal != null && !khoaVal.isBlank()) {
                    try {
                        khoaId = Long.parseLong(khoaVal.trim());
                        Optional<Faculty> optKhoa = khoaRepository.findById(khoaId);
                        if (optKhoa.isEmpty()) {
                            rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "khoaId", "khoaId", "khoaId", khoaVal, "Khoa với ID " + khoaId + " không tồn tại"));
                        } else {
                            khoaEntity = optKhoa.get();
                        }
                    } catch (NumberFormatException ex) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "khoaId", "khoaId", "khoaId", khoaVal, "khoaId phải là số nguyên"));
                    }
                } else if (targetType == TargetType.SINH_VIEN) {
                    rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "khoaId", "khoaId", "khoaId", khoaVal, "khoaId không được để trống đối với sinh viên"));
                }

                // Validate Nganh reference (Sinh viên)
                Long nganhId = null;
                Major nganhEntity = null;
                if (nganhVal != null && !nganhVal.isBlank()) {
                    try {
                        nganhId = Long.parseLong(nganhVal.trim());
                        Optional<Major> optNganh = nganhRepository.findById(nganhId);
                        if (optNganh.isEmpty()) {
                            rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "nganhId", "nganhId", "nganhId", nganhVal, "Ngành với ID " + nganhId + " không tồn tại"));
                        } else {
                            nganhEntity = optNganh.get();
                            if (khoaEntity != null && !nganhEntity.getKhoa().getId().equals(khoaEntity.getId())) {
                                rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "nganhId", "nganhId", "nganhId", nganhVal,
                                        "Ngành (ID " + nganhId + " - " + nganhEntity.getTen() + ") không thuộc khoa đã chọn (ID " + khoaEntity.getId() + " - " + khoaEntity.getTen() + ")"));
                            }
                        }
                    } catch (NumberFormatException ex) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "nganhId", "nganhId", "nganhId", nganhVal, "nganhId phải là số nguyên"));
                    }
                }

                // Validate Lop reference (Sinh viên)
                Long lopId = null;
                AdministrativeClass lopEntity = null;
                if (lopVal != null && !lopVal.isBlank()) {
                    try {
                        lopId = Long.parseLong(lopVal.trim());
                        Optional<AdministrativeClass> optLop = lopHanhChinhRepository.findById(lopId);
                        if (optLop.isEmpty()) {
                            rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "lopId", "lopId", "lopId", lopVal, "Lớp hành chính với ID " + lopId + " không tồn tại"));
                        } else {
                            lopEntity = optLop.get();
                            if (nganhEntity != null && !lopEntity.getNganh().getId().equals(nganhEntity.getId())) {
                                rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "lopId", "lopId", "lopId", lopVal,
                                        "Lớp hành chính (ID " + lopId + " - " + lopEntity.getTen() + ") không thuộc ngành đã chọn (ID " + nganhEntity.getId() + " - " + nganhEntity.getTen() + ")"));
                            }
                            if (khoaEntity != null && !lopEntity.getNganh().getKhoa().getId().equals(khoaEntity.getId())) {
                                rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "lopId", "lopId", "lopId", lopVal,
                                        "Lớp hành chính (ID " + lopId + " - " + lopEntity.getTen() + ") không thuộc khoa đã chọn (ID " + khoaEntity.getId() + " - " + khoaEntity.getTen() + ")"));
                            }
                        }
                    } catch (NumberFormatException ex) {
                        rowErrors.add(new ValidationErrorDetail(null, null, ma, null, rowNumber, "lopId", "lopId", "lopId", lopVal, "lopId phải là số nguyên"));
                    }
                }

                boolean hopLe = rowErrors.isEmpty();
                String trangThai = hopLe ? "HOP_LE" : (isDuplicateError(rowErrors) ? "TRUNG_LAP" : "LOI");
                String hanhDong = hopLe ? "TAO_MOI" : "BO_QUA";

                UserDtos.UserImportRowDetailDto rowDto = new UserDtos.UserImportRowDetailDto(
                        rowNumber,
                        ma != null ? ma.trim() : "",
                        hoTen != null ? hoTen.trim() : "",
                        email != null ? email.trim() : null,
                        soDienThoai != null ? soDienThoai.trim() : null,
                        khoaId,
                        khoaEntity != null ? khoaEntity.getTen() : null,
                        nganhId,
                        nganhEntity != null ? nganhEntity.getTen() : null,
                        lopId,
                        lopEntity != null ? lopEntity.getTen() : null,
                        ma != null ? ma.trim() : "",
                        hopLe,
                        trangThai,
                        hanhDong,
                        rowErrors
                );

                allRows.add(rowDto);
                if (hopLe) {
                    validRows.add(rowDto);
                }
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("Không thể đọc và phân tích cú pháp tệp CSV: " + ex.getMessage());
        }

        if (allRows.isEmpty()) {
            throw ApiException.badRequest("File CSV không chứa dữ liệu dòng nào để import");
        }

        return new CleanParsedData(allRows, validRows);
    }

    private boolean isDuplicateError(List<ValidationErrorDetail> errors) {
        return errors.stream().anyMatch(e -> e.message() != null && e.message().contains("đã tồn tại"));
    }

    private boolean isRecordEmpty(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (record.get(i) != null && !record.get(i).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getVal(CSVRecord record, Integer index) {
        if (index == null || index < 0 || index >= record.size()) {
            return null;
        }
        String val = record.get(index);
        return val != null ? val.trim() : null;
    }

    // --- COLUMN HEADER MAPPING & STRUCTURAL VALIDATION ---

    private Map<String, Integer> buildColumnMapping(CSVRecord header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String colRaw = header.get(i);
            if (colRaw == null) continue;
            String normalized = normalizeColName(colRaw);
            map.putIfAbsent(normalized, i);
        }
        return map;
    }

    private String normalizeColName(String raw) {
        String s = raw.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        // Replace accented/diacritics
        s = s.replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace("à", "a").replace("á", "a").replace("ả", "a").replace("ã", "a").replace("ạ", "a")
                .replace("ă", "a").replace("ằ", "a").replace("ắ", "a").replace("ẳ", "a").replace("ẵ", "a").replace("ặ", "a")
                .replace("â", "a").replace("ầ", "a").replace("ấ", "a").replace("ẩ", "a").replace("ẫ", "a").replace("ậ", "a")
                .replace("đ", "d")
                .replace("è", "e").replace("é", "e").replace("ẻ", "e").replace("ẽ", "e").replace("ẹ", "e")
                .replace("ê", "e").replace("ề", "e").replace("ế", "e").replace("ể", "e").replace("ễ", "e").replace("ệ", "e")
                .replace("ì", "i").replace("í", "i").replace("ỉ", "i").replace("ĩ", "i").replace("ị", "i")
                .replace("ò", "o").replace("ó", "o").replace("ỏ", "o").replace("õ", "o").replace("ọ", "o")
                .replace("ô", "o").replace("ồ", "o").replace("ố", "o").replace("ổ", "o").replace("ỗ", "o").replace("ộ", "o")
                .replace("ơ", "o").replace("ờ", "o").replace("ớ", "o").replace("ở", "o").replace("ỡ", "o").replace("ợ", "o")
                .replace("ù", "u").replace("ú", "u").replace("ủ", "u").replace("ũ", "u").replace("ụ", "u")
                .replace("ư", "u").replace("ừ", "u").replace("ứ", "u").replace("ử", "u").replace("ữ", "u").replace("ự", "u")
                .replace("ỳ", "y").replace("ý", "y").replace("ỷ", "y").replace("ỹ", "y").replace("ỵ", "y");

        if (s.equals("ma") || s.equals("masinhvien") || s.equals("magiangvien") || s.equals("masv") || s.equals("magv")) {
            return "ma";
        }
        if (s.equals("hoten") || s.equals("hovaten") || s.equals("ten")) {
            return "hoten";
        }
        if (s.equals("email") || s.equals("mail") || s.equals("thudientu")) {
            return "email";
        }
        if (s.equals("sodienthoai") || s.equals("sdt") || s.equals("phone")) {
            return "sodienthoai";
        }
        if (s.equals("khoaid") || s.equals("khoa") || s.equals("makhoa")) {
            return "khoaid";
        }
        if (s.equals("nganhid") || s.equals("nganh") || s.equals("manganh")) {
            return "nganhid";
        }
        if (s.equals("lopid") || s.equals("lop") || s.equals("malop") || s.equals("lophanhchinh") || s.equals("lophanhchinhid")) {
            return "lopid";
        }
        return s;
    }

    private void validateHeaderStructure(TargetType targetType, Map<String, Integer> colMap) {
        List<String> missing = new ArrayList<>();
        if (!colMap.containsKey("ma")) missing.add("ma");
        if (!colMap.containsKey("hoten")) missing.add("hoTen");
        if (!colMap.containsKey("email")) missing.add("email");

        if (targetType == TargetType.SINH_VIEN) {
            if (!colMap.containsKey("khoaid")) missing.add("khoaId");
        }

        if (!missing.isEmpty()) {
            String expected = targetType == TargetType.SINH_VIEN
                    ? "ma, hoTen, email, khoaId (tùy chọn: nganhId, lopId, soDienThoai)"
                    : "ma, hoTen, email (tùy chọn: soDienThoai, khoaId)";
            throw ApiException.badRequest("Cấu trúc header không hợp lệ: thiếu các cột bắt buộc: ["
                    + String.join(", ", missing) + "]. Định dạng yêu cầu: " + expected);
        }
    }

    // --- FILE VALIDATION & SECURITY ---

    private byte[] validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn file CSV để tải lên");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw ApiException.badRequest("Dung lượng file vượt quá giới hạn cho phép (5 MB)");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.toLowerCase(Locale.ROOT).endsWith(".csv")
                && !originalFilename.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw ApiException.badRequest("Định dạng file không hợp lệ. Chỉ chấp nhận file CSV (.csv)");
        }
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw ApiException.badRequest("Không thể đọc nội dung file: " + ex.getMessage());
        }
    }

    private String tinhSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không tìm thấy thuật toán SHA-256", ex);
        }
    }

    private void cleanExpiredSessions() {
        Instant now = Instant.now(clock);
        previewSessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt) || entry.getValue().consumed.get());
    }

    // --- ERROR REPORT CSV ---

    public byte[] xuatFileLoiCsv(List<UserDtos.UserImportRowDetailDto> danhSachLoi) {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.BOM);
        sb.append(CsvUtils.row(List.of("Dòng", "Mã", "Họ tên", "Trường lỗi", "Giá trị nhập", "Nguyên nhân lỗi")));

        if (danhSachLoi != null) {
            for (UserDtos.UserImportRowDetailDto row : danhSachLoi) {
                if (row.errors() != null && !row.errors().isEmpty()) {
                    for (ValidationErrorDetail err : row.errors()) {
                        sb.append(CsvUtils.row(List.of(
                                String.valueOf(row.dong()),
                                row.ma() != null ? row.ma() : "",
                                row.hoTen() != null ? row.hoTen() : "",
                                err.cot() != null ? err.cot() : (err.field() != null ? err.field() : ""),
                                err.giaTriNhap() != null ? err.giaTriNhap() : "",
                                err.message() != null ? err.message() : ""
                        )));
                    }
                } else if (!row.hopLe()) {
                    sb.append(CsvUtils.row(List.of(
                            String.valueOf(row.dong()),
                            row.ma() != null ? row.ma() : "",
                            row.hoTen() != null ? row.hoTen() : "",
                            "",
                            "",
                            "Dữ liệu dòng không hợp lệ"
                    )));
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    // --- AUDIT SNAPSHOTS ---

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

    private record CleanParsedData(
            List<UserDtos.UserImportRowDetailDto> allRows,
            List<UserDtos.UserImportRowDetailDto> validRows
    ) {
    }

    private static class UserImportSession {
        final String token;
        final String fileSha256;
        final String fileName;
        final TargetType targetType;
        final Instant expiresAt;
        final List<UserDtos.UserImportRowDetailDto> allRows;
        final List<UserDtos.UserImportRowDetailDto> validRows;
        final AtomicBoolean consumed = new AtomicBoolean(false);

        UserImportSession(String token, String fileSha256, String fileName, TargetType targetType,
                          Instant expiresAt, List<UserDtos.UserImportRowDetailDto> allRows,
                          List<UserDtos.UserImportRowDetailDto> validRows) {
            this.token = token;
            this.fileSha256 = fileSha256;
            this.fileName = fileName;
            this.targetType = targetType;
            this.expiresAt = expiresAt;
            this.allRows = allRows;
            this.validRows = validRows;
        }
    }
}
