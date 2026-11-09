package com.edu.registration.service;

import com.edu.registration.config.GradeImportProperties;
import com.edu.registration.dto.GradeImportDtos;
import com.edu.registration.dto.ValidationErrorDetail;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.importer.ImportSecurityValidator;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.CsvUtils;
import com.edu.registration.util.XlsxWorkbookSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

@Service
public class GradeImportService {

    public static final String AUDIT_ACTION_IMPORT = "GRADE_IMPORTED_XLSX";
    public static final String AUDIT_ENTITY_TYPE = "GRADE_SHEET";

    private static final Logger log = LoggerFactory.getLogger(GradeImportService.class);
    private static final int MAX_ACTIVE_PREVIEWS = 500;

    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final GradeService gradeService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final GradeImportProperties properties;
    private Clock clock;

    private final ConcurrentHashMap<String, PreviewSession> previewSessions = new ConcurrentHashMap<>();

    @Autowired
    public GradeImportService(
            CourseClassRepository lopHocPhanRepository,
            CourseRegistrationRepository dangKyHocPhanRepository,
            GradeRepository diemRepository,
            GradeService gradeService,
            CurrentUser currentUser,
            AuditLogService auditLogService,
            GradeImportProperties properties
    ) {
        this(lopHocPhanRepository, dangKyHocPhanRepository, diemRepository, gradeService,
                currentUser, auditLogService, properties, Clock.systemUTC());
    }

    public GradeImportService(
            CourseClassRepository lopHocPhanRepository,
            CourseRegistrationRepository dangKyHocPhanRepository,
            GradeRepository diemRepository,
            GradeService gradeService,
            CurrentUser currentUser,
            AuditLogService auditLogService,
            GradeImportProperties properties,
            Clock clock
    ) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.gradeService = gradeService;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.properties = properties != null ? properties : GradeImportProperties.defaults();
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /** Cho phép kiểm thử điều chỉnh đồng hồ để kiểm tra thời hạn hết hạn của phiên preview. */
    public void setClock(Clock clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }

    @Transactional(readOnly = true)
    public GradeImportDtos.GradeImportConfigDto layCauHinhImport(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        gradeService.kiemTraQuyenSuaDiem(lop);
        Subject mon = lop.getMonHoc();
        return new GradeImportDtos.GradeImportConfigDto(
                lop.getId(),
                lop.getMa(),
                mon != null ? mon.getTen() : null,
                lop.getTrangThaiBangDiemHieuLuc().name(),
                properties.maxFileSizeBytes(),
                properties.maxRows(),
                properties.maxSheets(),
                properties.parseTimeoutMs(),
                properties.previewTtlSeconds(),
                properties.transactionPolicy(),
                properties.updatePolicy(),
                XlsxWorkbookSupport.DEFAULT_SHEET_NAME,
                danhSachCotTemplate(),
                properties.policyNote()
        );
    }

    @Transactional(readOnly = true)
    public XlsxFile xuatTemplateXlsx(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        Lecturer giangVien = gradeService.kiemTraQuyenSuaDiem(lop);

        List<CourseRegistration> activeRegistrations = dangKyHocPhanRepository
                .findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY)
                .stream()
                .sorted(Comparator.comparing(
                        dk -> dk.getSinhVien() != null ? dk.getSinhVien().getMa() : "",
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();

        Map<Long, Grade> diemMap = gradeService.layDiemTheoDangKy(activeRegistrations);

        List<XlsxWorkbookSupport.TemplateStudentRow> templateRows = new ArrayList<>(activeRegistrations.size());
        for (CourseRegistration dk : activeRegistrations) {
            Student sv = dk.getSinhVien();
            Grade diem = diemMap.get(dk.getId());
            templateRows.add(new XlsxWorkbookSupport.TemplateStudentRow(
                    sv != null ? sv.getMa() : "",
                    sv != null ? sv.getHoTen() : "",
                    diem != null ? diem.getDiemQuaTrinh() : null,
                    diem != null ? diem.getDiemCuoiKy() : null
            ));
        }

        byte[] bytes = XlsxWorkbookSupport.createGradeTemplateWorkbook(templateRows);
        String filename = "mau-nhap-diem-" + sanitizeFilenamePart(lop.getMa()) + ".xlsx";
        log.info("Giảng viên {} đã tải template .xlsx lớp {} ({} sinh viên)",
                giangVien.getMa(), lop.getMa(), templateRows.size());
        return new XlsxFile(filename, bytes, templateRows.size());
    }

    /**
     * Tải lên tệp .xlsx, kiểm tra bảo mật/cấu trúc/quyền hạn và trả về kết quả xem trước (preview).
     * Tuyệt đối không ghi dữ liệu vào bảng điểm ở bước này; tệp tạm luôn được xóa trong khối finally.
     */
    @Transactional(readOnly = true)
    public GradeImportDtos.PreviewResponse xemTruocImport(Long lopHocPhanId, MultipartFile file) {
        evictExpiredPreviews();

        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        Lecturer giangVien = gradeService.kiemTraQuyenSuaDiem(lop);
        User actor = currentUser.get();

        gradeService.kiemTraBangDiemChoPhepSua(lop);

        validateUploadMetadata(file);
        String sanitizedFilename = ImportSecurityValidator.sanitizeAndValidateFileName(file.getOriginalFilename(), ".xlsx");

        Path tempFile = null;
        try {
            TempUpload tempUpload = writeToBoundedTempFile(file);
            tempFile = tempUpload.path();

            XlsxWorkbookSupport.ParsedSheet parsedSheet = XlsxWorkbookSupport.parseWorkbook(tempFile, properties);
            TemplateHeaderMapping headerMapping = validateAndMapHeader(parsedSheet);

            if (parsedSheet.dataRows().isEmpty()) {
                throw ApiException.badRequest("Tệp Excel không có dòng dữ liệu sinh viên nào để import");
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<String, CourseRegistration> activeByMaSv = new LinkedHashMap<>();
            for (CourseRegistration dk : activeRegistrations) {
                if (dk.getSinhVien() != null && dk.getSinhVien().getMa() != null) {
                    activeByMaSv.put(normalizeCode(dk.getSinhVien().getMa()), dk);
                }
            }
            Map<Long, Grade> diemMap = gradeService.layDiemTheoDangKy(activeRegistrations);

            List<GradeImportDtos.PreviewRowDto> previewRows = new ArrayList<>(parsedSheet.dataRows().size());
            List<ValidationErrorDetail> allErrors = new ArrayList<>();
            List<ValidatedImportRow> sessionRows = new ArrayList<>(parsedSheet.dataRows().size());
            Map<String, Integer> firstSeenRowByMaSv = new LinkedHashMap<>();

            int soDongHopLe = 0;
            int soDongLoi = 0;
            int soTaoMoiDuKien = 0;
            int soCapNhatDuKien = 0;
            String sheetName = parsedSheet.sheetName();

            for (XlsxWorkbookSupport.ParsedRow row : parsedSheet.dataRows()) {
                int dong = row.rowNumber();
                List<ValidationErrorDetail> rowErrors = new ArrayList<>();

                XlsxWorkbookSupport.ParsedCell maSvCell = row.cellAt(headerMapping.maSinhVienCol());
                String maSvColLetter = XlsxWorkbookSupport.indexToColLetter(headerMapping.maSinhVienCol());
                String maSvRaw = null;
                CourseRegistration matchedRegistration = null;

                if (maSvCell != null && maSvCell.isFormula()) {
                    rowErrors.add(new ValidationErrorDetail(
                            null, null, null, sheetName, dong,
                            "maSinhVien", maSvColLetter, "maSinhVien",
                            maSvCell.formulaText(),
                            "Ô mã sinh viên không được chứa công thức Excel"
                    ));
                } else if (maSvCell == null || maSvCell.isBlank()) {
                    rowErrors.add(new ValidationErrorDetail(
                            null, null, null, sheetName, dong,
                            "maSinhVien", maSvColLetter, "maSinhVien",
                            "",
                            "Mã sinh viên không được để trống"
                    ));
                } else if (maSvCell.kind() == XlsxWorkbookSupport.CellValueKind.ERROR) {
                    rowErrors.add(new ValidationErrorDetail(
                            null, null, null, sheetName, dong,
                            "maSinhVien", maSvColLetter, "maSinhVien",
                            maSvCell.rawText(),
                            "Ô mã sinh viên chứa giá trị lỗi Excel"
                    ));
                } else {
                    maSvRaw = maSvCell.rawText().trim();
                    String normalizedMaSv = normalizeCode(maSvRaw);
                    Integer previousRow = firstSeenRowByMaSv.putIfAbsent(normalizedMaSv, dong);
                    if (previousRow != null) {
                        rowErrors.add(new ValidationErrorDetail(
                                null, null, maSvRaw, sheetName, dong,
                                "maSinhVien", maSvColLetter, "maSinhVien",
                                maSvRaw,
                                "Mã sinh viên " + maSvRaw + " bị trùng lặp trong tệp (đã xuất hiện tại dòng " + previousRow + ")"
                        ));
                    }
                    matchedRegistration = activeByMaSv.get(normalizedMaSv);
                    if (matchedRegistration == null) {
                        rowErrors.add(new ValidationErrorDetail(
                                null, null, maSvRaw, sheetName, dong,
                                "maSinhVien", maSvColLetter, "maSinhVien",
                                maSvRaw,
                                "Mã sinh viên " + maSvRaw + " không thuộc danh sách đăng ký đang học của lớp học phần"
                        ));
                    }
                }

                if (headerMapping.hoTenCol() != null) {
                    XlsxWorkbookSupport.ParsedCell hoTenCell = row.cellAt(headerMapping.hoTenCol());
                    if (hoTenCell != null && hoTenCell.isFormula()) {
                        String hoTenColLetter = XlsxWorkbookSupport.indexToColLetter(headerMapping.hoTenCol());
                        rowErrors.add(new ValidationErrorDetail(
                                matchedRegistration != null ? matchedRegistration.getId() : null,
                                matchedRegistration != null && matchedRegistration.getSinhVien() != null
                                        ? matchedRegistration.getSinhVien().getId() : null,
                                maSvRaw,
                                sheetName, dong, "hoTen", hoTenColLetter, "hoTen",
                                hoTenCell.formulaText(),
                                "Ô họ tên không được chứa công thức Excel"
                        ));
                    }
                }

                Long dangKyId = matchedRegistration != null ? matchedRegistration.getId() : null;
                Student sv = matchedRegistration != null ? matchedRegistration.getSinhVien() : null;
                Long sinhVienId = sv != null ? sv.getId() : null;
                String canonicalMaSv = sv != null ? sv.getMa() : maSvRaw;
                String hoTenHienThi = sv != null ? sv.getHoTen()
                        : extractOptionalText(row, headerMapping.hoTenCol());

                Double diemQuaTrinhMoi = parseAndValidateGradeCell(
                        row.cellAt(headerMapping.diemQuaTrinhCol()),
                        XlsxWorkbookSupport.indexToColLetter(headerMapping.diemQuaTrinhCol()),
                        "diemQuaTrinh",
                        sheetName,
                        dong,
                        dangKyId,
                        sinhVienId,
                        canonicalMaSv,
                        rowErrors
                );

                Double diemCuoiKyMoi = parseAndValidateGradeCell(
                        row.cellAt(headerMapping.diemCuoiKyCol()),
                        XlsxWorkbookSupport.indexToColLetter(headerMapping.diemCuoiKyCol()),
                        "diemCuoiKy",
                        sheetName,
                        dong,
                        dangKyId,
                        sinhVienId,
                        canonicalMaSv,
                        rowErrors
                );

                Grade existingGrade = dangKyId != null ? diemMap.get(dangKyId) : null;
                Double qtHienTai = existingGrade != null ? existingGrade.getDiemQuaTrinh() : null;
                Double ckHienTai = existingGrade != null ? existingGrade.getDiemCuoiKy() : null;
                Double tkHienTai = existingGrade != null ? existingGrade.getDiemTongKet() : null;
                Double tkDuKien = GradeService.tinhDiemTongKet(diemQuaTrinhMoi, diemCuoiKyMoi);

                boolean hopLe = rowErrors.isEmpty();
                String hanhDongDuKien;
                if (hopLe) {
                    soDongHopLe++;
                    if (existingGrade == null) {
                        hanhDongDuKien = "TAO_MOI";
                        soTaoMoiDuKien++;
                    } else {
                        hanhDongDuKien = "CAP_NHAT";
                        soCapNhatDuKien++;
                    }
                    sessionRows.add(new ValidatedImportRow(
                            dong,
                            canonicalMaSv,
                            diemQuaTrinhMoi,
                            diemCuoiKyMoi
                    ));
                } else {
                    soDongLoi++;
                    hanhDongDuKien = "LOI";
                    allErrors.addAll(rowErrors);
                }

                previewRows.add(new GradeImportDtos.PreviewRowDto(
                        sheetName,
                        dong,
                        dangKyId,
                        sinhVienId,
                        canonicalMaSv,
                        hoTenHienThi,
                        qtHienTai,
                        ckHienTai,
                        tkHienTai,
                        diemQuaTrinhMoi,
                        diemCuoiKyMoi,
                        tkDuKien,
                        hopLe,
                        hanhDongDuKien,
                        List.copyOf(rowErrors)
                ));
            }

            Instant now = clock.instant();
            Instant expiresAt = now.plusSeconds(properties.previewTtlSeconds());
            String previewToken = UUID.randomUUID().toString();
            boolean coTheXacNhan = (soDongLoi == 0 && soDongHopLe > 0);

            PreviewSession session = new PreviewSession(
                    previewToken,
                    actor != null ? actor.getId() : null,
                    giangVien.getId(),
                    lop.getId(),
                    tempUpload.sha256Hex(),
                    sanitizedFilename,
                    sheetName,
                    List.copyOf(sessionRows),
                    List.copyOf(allErrors),
                    now,
                    expiresAt,
                    new AtomicBoolean(false)
            );
            storePreviewSession(session);

            Subject mon = lop.getMonHoc();
            return new GradeImportDtos.PreviewResponse(
                    previewToken,
                    tempUpload.sha256Hex(),
                    expiresAt,
                    lop.getId(),
                    lop.getMa(),
                    mon != null ? mon.getTen() : null,
                    sanitizedFilename,
                    tempUpload.sizeBytes(),
                    sheetName,
                    previewRows.size(),
                    soDongHopLe,
                    soDongLoi,
                    soTaoMoiDuKien,
                    soCapNhatDuKien,
                    coTheXacNhan,
                    properties.transactionPolicy(),
                    properties.updatePolicy(),
                    properties.policyNote(),
                    previewRows,
                    allErrors
            );
        } finally {
            deleteTempFileSafely(tempFile);
        }
    }

    /**
     * Xác nhận ghi dữ liệu điểm từ phiên preview đã kiểm duyệt vào bảng điểm theo chính sách ALL_OR_NOTHING.
     */
    @Transactional
    public GradeImportDtos.ConfirmImportResponse xacNhanImport(
            Long lopHocPhanId,
            GradeImportDtos.ConfirmImportRequest request
    ) {
        evictExpiredPreviews();

        if (request == null || request.previewToken() == null || request.previewToken().isBlank()) {
            throw ApiException.badRequest("Mã phiên xem trước (previewToken) không được để trống");
        }

        CourseClass lop = gradeService.layLopDeCapNhatBangDiem(lopHocPhanId);
        Lecturer giangVien = gradeService.kiemTraQuyenSuaDiem(lop);
        User actor = currentUser.get();

        gradeService.kiemTraBangDiemChoPhepSua(lop);

        String token = request.previewToken().trim();
        PreviewSession session = previewSessions.get(token);
        if (session == null) {
            throw ApiException.badRequest("Phiên xem trước (preview) không tồn tại hoặc đã hết hạn. Vui lòng tải tệp lên và xem trước lại");
        }

        if (clock.instant().isAfter(session.expiresAt())) {
            previewSessions.remove(token);
            throw ApiException.badRequest("Phiên xem trước (preview) đã hết hạn. Vui lòng tải tệp lên và xem trước lại");
        }

        if (!session.lopHocPhanId().equals(lopHocPhanId)) {
            throw ApiException.forbidden("Phiên xem trước không thuộc lớp học phần đang chọn");
        }

        Long actorUserId = actor != null ? actor.getId() : null;
        if ((session.userId() != null && !session.userId().equals(actorUserId))
                || !session.giangVienId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Phiên xem trước không thuộc quyền của giảng viên hiện tại");
        }

        if (request.fileSha256() != null && !request.fileSha256().isBlank()
                && !session.fileSha256().equalsIgnoreCase(request.fileSha256().trim())) {
            throw ApiException.badRequest("Mã kiểm tra tệp (SHA-256) không khớp với phiên xem trước");
        }

        if (session.consumed().get()) {
            throw ApiException.conflict("Phiên xem trước này đã được xác nhận import trước đó");
        }

        if (!session.errors().isEmpty() || session.rows().isEmpty()) {
            throw ApiException.validation(
                    "Phiên xem trước đang có lỗi dữ liệu (" + session.errors().size()
                            + " lỗi), không thể xác nhận import theo chính sách ALL_OR_NOTHING",
                    session.errors()
            );
        }

        return executeAudited(AUDIT_ACTION_IMPORT, AUDIT_ENTITY_TYPE, lopHocPhanId, audit -> {
            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<String, CourseRegistration> activeByMaSv = new LinkedHashMap<>();
            for (CourseRegistration dk : activeRegistrations) {
                if (dk.getSinhVien() != null && dk.getSinhVien().getMa() != null) {
                    activeByMaSv.put(normalizeCode(dk.getSinhVien().getMa()), dk);
                }
            }

            // Kiểm tra lại toàn bộ dữ liệu tại thời điểm xác nhận (phòng trường hợp trạng thái đăng ký thay đổi)
            List<ValidationErrorDetail> confirmErrors = new ArrayList<>();
            for (ValidatedImportRow row : session.rows()) {
                CourseRegistration dk = activeByMaSv.get(normalizeCode(row.maSinhVien()));
                if (dk == null) {
                    confirmErrors.add(new ValidationErrorDetail(
                            null, null, row.maSinhVien(), session.sheetName(), row.dong(),
                            "maSinhVien", "A", "maSinhVien", row.maSinhVien(),
                            "Mã sinh viên " + row.maSinhVien() + " không còn trong danh sách đăng ký đang học của lớp"
                    ));
                    continue;
                }
                Student sv = dk.getSinhVien();
                Long svId = sv != null ? sv.getId() : null;
                GradeService.validateDiem(
                        row.diemQuaTrinh(), session.sheetName(), "diemQuaTrinh", "C",
                        "diemQuaTrinh", row.diemQuaTrinh() != null ? String.valueOf(row.diemQuaTrinh()) : null,
                        dk.getId(), svId, row.maSinhVien(), row.dong(), confirmErrors
                );
                GradeService.validateDiem(
                        row.diemCuoiKy(), session.sheetName(), "diemCuoiKy", "D",
                        "diemCuoiKy", row.diemCuoiKy() != null ? String.valueOf(row.diemCuoiKy()) : null,
                        dk.getId(), svId, row.maSinhVien(), row.dong(), confirmErrors
                );
            }

            if (!confirmErrors.isEmpty()) {
                throw ApiException.validation(
                        "Dữ liệu không còn hợp lệ tại thời điểm xác nhận import (" + confirmErrors.size() + " lỗi)",
                        confirmErrors
                );
            }

            if (!session.consumed().compareAndSet(false, true)) {
                throw ApiException.conflict("Phiên xem trước này đã được xác nhận import trước đó");
            }
            previewSessions.remove(token);

            Map<Long, Grade> diemMap = gradeService.layDiemTheoDangKy(activeRegistrations);
            LocalDateTime now = LocalDateTime.now();
            List<Grade> toSave = new ArrayList<>(session.rows().size());
            List<GradeImportDtos.ConfirmRowResultDto> rowResults = new ArrayList<>(session.rows().size());

            int soTaoMoi = 0;
            int soCapNhat = 0;

            for (ValidatedImportRow row : session.rows()) {
                CourseRegistration dk = activeByMaSv.get(normalizeCode(row.maSinhVien()));
                Student sv = dk.getSinhVien();
                Grade diem = diemMap.get(dk.getId());
                boolean isNew = (diem == null);

                Double qtCu = diem != null ? diem.getDiemQuaTrinh() : null;
                Double ckCu = diem != null ? diem.getDiemCuoiKy() : null;
                Double tkCu = diem != null ? diem.getDiemTongKet() : null;

                if (isNew) {
                    diem = new Grade();
                    diem.setDangKy(dk);
                    diemMap.put(dk.getId(), diem);
                    soTaoMoi++;
                } else {
                    soCapNhat++;
                }

                Double tkMoi = GradeService.tinhDiemTongKet(row.diemQuaTrinh(), row.diemCuoiKy());
                diem.setDiemQuaTrinh(row.diemQuaTrinh());
                diem.setDiemCuoiKy(row.diemCuoiKy());
                diem.setDiemTongKet(tkMoi);
                diem.setCapNhatLuc(now);
                toSave.add(diem);

                rowResults.add(new GradeImportDtos.ConfirmRowResultDto(
                        row.dong(),
                        dk.getId(),
                        sv != null ? sv.getId() : null,
                        sv != null ? sv.getMa() : row.maSinhVien(),
                        sv != null ? sv.getHoTen() : null,
                        qtCu,
                        ckCu,
                        tkCu,
                        row.diemQuaTrinh(),
                        row.diemCuoiKy(),
                        tkMoi,
                        isNew ? "TAO_MOI" : "CAP_NHAT"
                ));
            }

            diemRepository.saveAll(toSave);

            if (lop.getTrangThaiBangDiem() == null) {
                lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
                lopHocPhanRepository.save(lop);
            }

            Map<String, Object> auditAfter = new LinkedHashMap<>();
            auditAfter.put("lopHocPhanId", lop.getId());
            auditAfter.put("maLop", lop.getMa());
            auditAfter.put("tenTep", session.tenTep());
            auditAfter.put("tongSoDong", session.rows().size());
            auditAfter.put("soTaoMoi", soTaoMoi);
            auditAfter.put("soCapNhat", soCapNhat);
            auditAfter.put("soThatBai", 0);
            auditAfter.put("soBanGhiDiem", toSave.size());
            auditAfter.put("transactionPolicy", properties.transactionPolicy());
            auditAfter.put("updatePolicy", properties.updatePolicy());
            auditAfter.put("trangThaiBangDiem", lop.getTrangThaiBangDiemHieuLuc().name());
            audit.changes(Map.of(), auditAfter);

            log.info("Đã xác nhận import .xlsx cho lớp {} (id={}): tổng={}, tạo mới={}, cập nhật={}",
                    lop.getMa(), lop.getId(), session.rows().size(), soTaoMoi, soCapNhat);

            Subject mon = lop.getMonHoc();
            return new GradeImportDtos.ConfirmImportResponse(
                    lop.getId(),
                    lop.getMa(),
                    mon != null ? mon.getTen() : null,
                    session.tenTep(),
                    session.sheetName(),
                    properties.transactionPolicy(),
                    properties.updatePolicy(),
                    session.rows().size(),
                    soTaoMoi,
                    soCapNhat,
                    0,
                    clock.instant(),
                    rowResults
            );
        });
    }

    private void validateUploadMetadata(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn tệp Excel (.xlsx) để tải lên");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw ApiException.badRequest("Kích thước tệp vượt quá giới hạn tối đa cho phép ("
                    + properties.maxFileSizeBytes() + " bytes)");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw ApiException.badRequest("Tên tệp tải lên không hợp lệ. Chỉ chấp nhận tệp có phần mở rộng .xlsx");
        }
        String lowerName = originalFilename.trim().toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".csv")) {
            throw ApiException.badRequest("Không chấp nhận tệp CSV. Chức năng này chỉ nhận tệp Excel định dạng .xlsx theo mẫu");
        }
        if (lowerName.endsWith(".xls")) {
            throw ApiException.badRequest("Không chấp nhận định dạng Excel cũ (.xls). Vui lòng sử dụng tệp .xlsx");
        }
        ImportSecurityValidator.sanitizeAndValidateFileName(originalFilename, ".xlsx");
    }

    public byte[] xuatFileLoiCsv(Long lopHocPhanId, List<ValidationErrorDetail> danhSachLoi) {
        StringBuilder sb = new StringBuilder();
        sb.append(CsvUtils.BOM);
        sb.append(CsvUtils.row(List.of("Dòng", "Mã SV", "Cột", "Cột Excel", "Giá trị nhập", "Nguyên nhân lỗi")));

        if (danhSachLoi != null) {
            for (ValidationErrorDetail err : danhSachLoi) {
                sb.append(CsvUtils.row(List.of(
                        err.dong() != null ? String.valueOf(err.dong()) : "",
                        err.maSinhVien() != null ? err.maSinhVien() : "",
                        err.cot() != null ? err.cot() : (err.field() != null ? err.field() : ""),
                        err.cotExcel() != null ? err.cotExcel() : "",
                        err.giaTriNhap() != null ? err.giaTriNhap() : "",
                        err.message() != null ? err.message() : ""
                )));
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private TempUpload writeToBoundedTempFile(MultipartFile file) {
        Path dir = Paths.get(properties.tempDir()).toAbsolutePath().normalize();
        Path tempFile = null;
        try {
            Files.createDirectories(dir);
            tempFile = Files.createTempFile(dir, "grade-import-", ".xlsx.tmp");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long totalBytes = 0L;
            byte[] buffer = new byte[8192];

            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    totalBytes += read;
                    if (totalBytes > properties.maxFileSizeBytes()) {
                        throw ApiException.badRequest("Kích thước tệp vượt quá giới hạn tối đa cho phép ("
                                + properties.maxFileSizeBytes() + " bytes)");
                    }
                    digest.update(buffer, 0, read);
                    out.write(buffer, 0, read);
                }
            }

            if (totalBytes == 0L) {
                throw ApiException.badRequest("Tệp tải lên đang trống");
            }

            String sha256Hex = HexFormat.of().formatHex(digest.digest());
            return new TempUpload(tempFile, totalBytes, sha256Hex);
        } catch (ApiException ex) {
            deleteTempFileSafely(tempFile);
            throw ex;
        } catch (IOException | NoSuchAlgorithmException ex) {
            deleteTempFileSafely(tempFile);
            throw ApiException.badRequest("Không thể xử lý tệp tải lên tạm thời");
        }
    }

    private void deleteTempFileSafely(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                log.warn("Không thể xóa tệp tạm import điểm: {}", tempFile, ex);
            }
        }
    }

    private record TemplateHeaderMapping(
            int maSinhVienCol,
            Integer hoTenCol,
            int diemQuaTrinhCol,
            int diemCuoiKyCol
    ) {
    }

    private TemplateHeaderMapping validateAndMapHeader(XlsxWorkbookSupport.ParsedSheet parsedSheet) {
        XlsxWorkbookSupport.ParsedRow header = parsedSheet.headerRow();
        if (header == null || header.isCompletelyBlank()) {
            throw ApiException.badRequest("Tệp Excel không đúng mẫu quy định: thiếu dòng tiêu đề (header) ở dòng 1");
        }

        Integer maSvCol = null;
        Integer hoTenCol = null;
        Integer qtCol = null;
        Integer ckCol = null;

        for (Map.Entry<Integer, XlsxWorkbookSupport.ParsedCell> entry : header.cellsByCol().entrySet()) {
            int colIdx = entry.getKey();
            XlsxWorkbookSupport.ParsedCell cell = entry.getValue();
            if (cell == null || cell.isBlank()) {
                continue;
            }
            if (cell.isFormula()) {
                throw ApiException.badRequest("Dòng tiêu đề (dòng 1, cột " + cell.colLetter()
                        + ") không được chứa công thức Excel");
            }
            String normHeader = normalizeHeaderKey(cell.rawText());
            switch (normHeader) {
                case "masinhvien", "masv", "studentcode", "studentid" -> {
                    if (maSvCol != null) {
                        throw ApiException.badRequest("Tệp Excel sai mẫu: cột Mã sinh viên (maSinhVien) bị lặp lại");
                    }
                    maSvCol = colIdx;
                }
                case "hoten", "hovaten", "tensinhvien", "fullname" -> {
                    if (hoTenCol != null) {
                        throw ApiException.badRequest("Tệp Excel sai mẫu: cột Họ tên (hoTen) bị lặp lại");
                    }
                    hoTenCol = colIdx;
                }
                case "stt", "sothutu" -> {
                    // Cho phép cột STT tùy chọn nếu giảng viên giữ nguyên số thứ tự
                }
                case "diemquatrinh", "quatrinh", "diemqt" -> {
                    if (qtCol != null) {
                        throw ApiException.badRequest("Tệp Excel sai mẫu: cột Điểm quá trình (diemQuaTrinh) bị lặp lại");
                    }
                    qtCol = colIdx;
                }
                case "diemcuoiky", "cuoiky", "diemck" -> {
                    if (ckCol != null) {
                        throw ApiException.badRequest("Tệp Excel sai mẫu: cột Điểm cuối kỳ (diemCuoiKy) bị lặp lại");
                    }
                    ckCol = colIdx;
                }
                default -> throw ApiException.badRequest("Tệp Excel không đúng định dạng template quy định: phát hiện cột lạ '"
                        + cell.rawText().trim() + "' tại cột " + cell.colLetter()
                        + ". Mẫu chuẩn chỉ gồm: maSinhVien, hoTen, diemQuaTrinh, diemCuoiKy");
            }
        }

        if (maSvCol == null || qtCol == null || ckCol == null) {
            throw ApiException.badRequest("Tệp Excel không đúng định dạng template quy định. Yêu cầu bắt buộc có đủ các cột: maSinhVien, diemQuaTrinh, diemCuoiKy");
        }

        return new TemplateHeaderMapping(maSvCol, hoTenCol, qtCol, ckCol);
    }

    private Double parseAndValidateGradeCell(
            XlsxWorkbookSupport.ParsedCell cell,
            String colLetter,
            String fieldName,
            String sheetName,
            int dong,
            Long dangKyId,
            Long sinhVienId,
            String maSinhVien,
            List<ValidationErrorDetail> rowErrors
    ) {
        if (cell == null || cell.isBlank()) {
            return null;
        }
        if (cell.isFormula()) {
            rowErrors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheetName, dong,
                    fieldName, colLetter, fieldName, cell.formulaText(),
                    "Ô điểm không được chứa công thức Excel"
            ));
            return null;
        }
        if (cell.kind() == XlsxWorkbookSupport.CellValueKind.ERROR) {
            rowErrors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheetName, dong,
                    fieldName, colLetter, fieldName, cell.rawText(),
                    "Ô điểm chứa giá trị lỗi công thức Excel (" + cell.rawText() + ")"
            ));
            return null;
        }
        if (cell.kind() == XlsxWorkbookSupport.CellValueKind.BOOLEAN
                || cell.kind() == XlsxWorkbookSupport.CellValueKind.DATE) {
            rowErrors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheetName, dong,
                    fieldName, colLetter, fieldName, cell.rawText(),
                    "Điểm không đúng định dạng số"
            ));
            return null;
        }

        String raw = cell.rawText() != null ? cell.rawText().trim() : "";
        if (raw.isEmpty()) {
            return null;
        }

        double parsed;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(raw);
            parsed = bd.doubleValue();
        } catch (NumberFormatException ex) {
            rowErrors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheetName, dong,
                    fieldName, colLetter, fieldName, raw,
                    "Điểm không đúng định dạng số"
            ));
            return null;
        }

        if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
            rowErrors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheetName, dong,
                    fieldName, colLetter, fieldName, raw,
                    "Điểm không đúng định dạng số"
            ));
            return null;
        }

        // Tái sử dụng trực tiếp quy tắc validation điểm của QLSV-72 trong GradeService
        GradeService.validateDiem(
                parsed,
                sheetName,
                fieldName,
                colLetter,
                fieldName,
                raw,
                dangKyId,
                sinhVienId,
                maSinhVien,
                dong,
                rowErrors
        );
        return parsed;
    }

    private String extractOptionalText(XlsxWorkbookSupport.ParsedRow row, Integer colIdx) {
        if (colIdx == null) {
            return null;
        }
        XlsxWorkbookSupport.ParsedCell cell = row.cellAt(colIdx);
        if (cell == null || cell.isBlank() || cell.isFormula()) {
            return null;
        }
        return cell.rawText().trim();
    }

    private void storePreviewSession(PreviewSession session) {
        if (previewSessions.size() >= MAX_ACTIVE_PREVIEWS) {
            evictExpiredPreviews();
            if (previewSessions.size() >= MAX_ACTIVE_PREVIEWS) {
                previewSessions.keySet().stream().findFirst().ifPresent(previewSessions::remove);
            }
        }
        previewSessions.put(session.previewToken(), session);
    }

    private void evictExpiredPreviews() {
        Instant now = clock.instant();
        previewSessions.entrySet().removeIf(entry ->
                now.isAfter(entry.getValue().expiresAt()) || entry.getValue().consumed().get());
    }

    private <T> T executeAudited(String action, String entityType, Object entityId,
                                 Function<AuditLogService.AuditContext, T> operation) {
        if (auditLogService == null) {
            return operation.apply(new AuditLogService.AuditContext(entityId == null ? null : String.valueOf(entityId)));
        }
        return auditLogService.execute(action, entityType, entityId, operation);
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeHeaderKey(String raw) {
        if (raw == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return decomposed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String sanitizeFilenamePart(String value) {
        if (value == null || value.isBlank()) {
            return "lop";
        }
        return value.trim().replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String sanitizeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "bang-diem.xlsx";
        }
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        return base.replaceAll("[\\r\\n\\t]", "_").trim();
    }

    private static List<GradeImportDtos.TemplateColumnDto> danhSachCotTemplate() {
        return List.of(
                new GradeImportDtos.TemplateColumnDto("A", "maSinhVien", "Mã sinh viên", true,
                        "Mã sinh viên duy nhất trong tệp và phải thuộc danh sách đăng ký đang học của lớp"),
                new GradeImportDtos.TemplateColumnDto("B", "hoTen", "Họ và tên", false,
                        "Họ tên sinh viên (cột thông tin hỗ trợ tra cứu nhanh trên tệp mẫu)"),
                new GradeImportDtos.TemplateColumnDto("C", "diemQuaTrinh", "Điểm quá trình", true,
                        "Số thực từ 0 đến 10, tối đa 2 chữ số thập phân (trọng số 40%)"),
                new GradeImportDtos.TemplateColumnDto("D", "diemCuoiKy", "Điểm cuối kỳ", true,
                        "Số thực từ 0 đến 10, tối đa 2 chữ số thập phân (trọng số 60%)")
        );
    }

    private record TempUpload(Path path, long sizeBytes, String sha256Hex) {
    }

    private record ValidatedImportRow(
            int dong,
            String maSinhVien,
            Double diemQuaTrinh,
            Double diemCuoiKy
    ) {
    }

    private record PreviewSession(
            String previewToken,
            Long userId,
            Long giangVienId,
            Long lopHocPhanId,
            String fileSha256,
            String tenTep,
            String sheetName,
            List<ValidatedImportRow> rows,
            List<ValidationErrorDetail> errors,
            Instant createdAt,
            Instant expiresAt,
            AtomicBoolean consumed
    ) {
    }

    public record XlsxFile(String tenTep, byte[] noiDung, int soSinhVien) {
    }
}
