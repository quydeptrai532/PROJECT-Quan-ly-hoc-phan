package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.entity.SupportRequestAttachment;
import com.edu.registration.entity.SupportRequestChangeType;
import com.edu.registration.entity.SupportRequestComment;
import com.edu.registration.entity.SupportRequestPriority;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestStatusHistory;
import com.edu.registration.entity.SupportRequestType;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.SupportRequestAttachmentRepository;
import com.edu.registration.repository.SupportRequestCommentRepository;
import com.edu.registration.repository.SupportRequestRepository;
import com.edu.registration.repository.SupportRequestStatusHistoryRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class SupportRequestService {

    private static final Logger log = LoggerFactory.getLogger(SupportRequestService.class);
    private static final DateTimeFormatter TICKET_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SupportRequestRepository yeuCauRepository;
    private final SupportRequestCommentRepository traoDoiRepository;
    private final SupportRequestStatusHistoryRepository lichSuTrangThaiRepository;
    private final SupportRequestAttachmentRepository tepDinhKemRepository;
    private final StudentRepository sinhVienRepository;
    private final SubjectRepository monHocRepository;
    private final SemesterRepository hocKyRepository;
    private final GradeRepository diemRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;
    private final SupportAttachmentStorageService storageService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SupportBusinessCalendarService calendarService;
    private final SupportRequestProperties properties;

    @Autowired
    public SupportRequestService(SupportRequestRepository yeuCauRepository,
                                  SupportRequestCommentRepository traoDoiRepository,
                                  SupportRequestStatusHistoryRepository lichSuTrangThaiRepository,
                                  SupportRequestAttachmentRepository tepDinhKemRepository,
                                  StudentRepository sinhVienRepository,
                                  SubjectRepository monHocRepository,
                                  SemesterRepository hocKyRepository,
                                  GradeRepository diemRepository,
                                  UserRepository userRepository,
                                  CurrentUser currentUser,
                                  SupportAttachmentStorageService storageService,
                                  AuditLogService auditLogService,
                                  NotificationService notificationService,
                                  SupportBusinessCalendarService calendarService,
                                  SupportRequestProperties properties) {
        this.yeuCauRepository = yeuCauRepository;
        this.traoDoiRepository = traoDoiRepository;
        this.lichSuTrangThaiRepository = lichSuTrangThaiRepository;
        this.tepDinhKemRepository = tepDinhKemRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.monHocRepository = monHocRepository;
        this.hocKyRepository = hocKyRepository;
        this.diemRepository = diemRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.calendarService = calendarService != null
                ? calendarService
                : new SupportBusinessCalendarService(properties);
        this.properties = properties != null ? properties : SupportRequestProperties.defaults();
    }

    public SupportRequestService(SupportRequestRepository yeuCauRepository,
                                  SupportRequestCommentRepository traoDoiRepository,
                                  SupportRequestStatusHistoryRepository lichSuTrangThaiRepository,
                                  SupportRequestAttachmentRepository tepDinhKemRepository,
                                  StudentRepository sinhVienRepository,
                                  SubjectRepository monHocRepository,
                                  SemesterRepository hocKyRepository,
                                  GradeRepository diemRepository,
                                  CurrentUser currentUser,
                                  SupportAttachmentStorageService storageService,
                                  AuditLogService auditLogService,
                                  SupportRequestProperties properties) {
        this(yeuCauRepository, traoDoiRepository, lichSuTrangThaiRepository, tepDinhKemRepository,
                sinhVienRepository, monHocRepository, hocKyRepository, diemRepository,
                null, currentUser, storageService, auditLogService, null,
                new SupportBusinessCalendarService(properties), properties);
    }

    public record TepTaiXuong(String tenTepGoc, String contentType, byte[] noiDung) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TepTaiXuong other)) {
                return false;
            }
            return Objects.equals(tenTepGoc, other.tenTepGoc)
                    && Objects.equals(contentType, other.contentType)
                    && Arrays.equals(noiDung, other.noiDung);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(tenTepGoc, contentType);
            return 31 * result + Arrays.hashCode(noiDung);
        }

        @Override
        public String toString() {
            return "TepTaiXuong[tenTepGoc=" + tenTepGoc + ", contentType=" + contentType
                    + ", noiDung=" + Arrays.toString(noiDung) + "]";
        }
    }

    @Transactional(readOnly = true)
    public SupportRequestDtos.DanhMucHoTroDto danhMuc() {
        User actor = currentUser.get();
        if (actor.getRole() != Role.SINH_VIEN && !isEligibleHandler(actor)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập danh mục hỗ trợ");
        }

        List<SupportRequestDtos.DanhMucMucDto> loaiYeuCau = properties.allowedTypes().stream()
                .map(item -> new SupportRequestDtos.DanhMucMucDto(item.name(), item.getMoTa()))
                .toList();
        List<SupportRequestDtos.DanhMucMucDto> trangThai = properties.allowedStatuses().stream()
                .map(item -> new SupportRequestDtos.DanhMucMucDto(item.name(), item.getMoTa()))
                .toList();
        List<SupportRequestDtos.DanhMucMucDto> mucUuTien = properties.queue().allowedPriorities().stream()
                .map(item -> new SupportRequestDtos.DanhMucMucDto(item.name(), item.getMoTa()))
                .toList();

        SupportRequestDtos.ChinhSachTepDto chinhSachTep = new SupportRequestDtos.ChinhSachTepDto(
                properties.maxFileSizeBytes(),
                properties.maxFilesPerRequest(),
                properties.allowedExtensions(),
                properties.slaNote()
        );

        List<SupportRequestDtos.MonHocLienQuanDto> monHoc = monHocRepository.findAllByOrderByMaAsc().stream()
                .map(this::toMonHocDto)
                .toList();
        List<SupportRequestDtos.HocKyLienQuanDto> hocKy = hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau", "id")).stream()
                .map(this::toHocKyDto)
                .toList();

        List<SupportRequestDtos.DiemLienQuanDto> diemCuaToi = List.of();
        if (actor.getRole() == Role.SINH_VIEN) {
            Student sinhVien = sinhVienHienTai(actor);
            diemCuaToi = diemRepository.findBySinhVienIdWithDetails(sinhVien.getId()).stream()
                    .filter(d -> d.getDangKy() != null
                            && d.getDangKy().getLopHocPhan() != null
                            && d.getDangKy().getLopHocPhan().isBangDiemDaCongBo()
                            && d.getDiemTongKet() != null)
                    .map(this::toDiemDto)
                    .toList();
        }

        List<SupportRequestDtos.HandlerUserDto> danhSachNguoiXuLy = isEligibleHandler(actor)
                ? danhSachNhanVienXuLy()
                : List.of();

        return new SupportRequestDtos.DanhMucHoTroDto(
                loaiYeuCau,
                trangThai,
                mucUuTien,
                chinhSachTep,
                monHoc,
                hocKy,
                diemCuaToi,
                danhSachNguoiXuLy,
                properties.sla().officialSlaEnabled(),
                properties.sla().policyNote(),
                properties.workingCalendar().policyNote()
        );
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto taoYeuCau(SupportRequestDtos.TaoYeuCauRequest request,
                                                         List<MultipartFile> rawFiles) {
        User actor = currentUser.get();
        if (actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được tạo phiếu yêu cầu hỗ trợ");
        }
        Student sinhVien = sinhVienHienTai(actor);

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu yêu cầu hỗ trợ không hợp lệ");
        }

        SupportRequestType loaiYeuCau = parseAndValidateLoaiYeuCau(request.loaiYeuCau());
        String tieuDe = validateRequiredText(request.tieuDe(), "Tiêu đề", 5, 200);
        String noiDung = validateRequiredText(request.noiDung(), "Nội dung", 10, 4000);

        List<MultipartFile> files = normalizeMultipartFiles(rawFiles);
        if (files.size() > properties.maxFilesPerRequest()) {
            throw ApiException.badRequest("Số lượng tệp đính kèm vượt quá giới hạn cho phép ("
                    + properties.maxFilesPerRequest() + " tệp)");
        }

        Subject monHoc = null;
        if (request.monHocId() != null) {
            monHoc = monHocRepository.findById(request.monHocId())
                    .orElseThrow(() -> ApiException.notFound("Môn học liên quan không tồn tại"));
        }

        Semester hocKy = null;
        if (request.hocKyId() != null) {
            hocKy = hocKyRepository.findById(request.hocKyId())
                    .orElseThrow(() -> ApiException.notFound("Học kỳ liên quan không tồn tại"));
        }

        Grade diem = null;
        if (request.diemId() != null) {
            diem = diemRepository.findWithDetailsById(request.diemId())
                    .orElseThrow(() -> ApiException.notFound("Bản ghi điểm liên quan không tồn tại"));
            CourseRegistration dangKy = diem.getDangKy();
            if (dangKy == null || dangKy.getSinhVien() == null
                    || !dangKy.getSinhVien().getId().equals(sinhVien.getId())) {
                throw ApiException.forbidden("Bạn không được phép chọn bản ghi điểm của sinh viên khác");
            }
            CourseClass lopHocPhan = dangKy.getLopHocPhan();
            if (lopHocPhan != null && !lopHocPhan.isBangDiemDaCongBo()) {
                throw ApiException.badRequest("Điểm của lớp học phần này chưa được công bố");
            }
            if (lopHocPhan != null) {
                if (monHoc != null && lopHocPhan.getMonHoc() != null
                        && !monHoc.getId().equals(lopHocPhan.getMonHoc().getId())) {
                    throw ApiException.badRequest("Môn học đã chọn không khớp với bản ghi điểm liên quan");
                }
                if (hocKy != null && lopHocPhan.getHocKy() != null
                        && !hocKy.getId().equals(lopHocPhan.getHocKy().getId())) {
                    throw ApiException.badRequest("Học kỳ đã chọn không khớp với bản ghi điểm liên quan");
                }
                if (monHoc == null) {
                    monHoc = lopHocPhan.getMonHoc();
                }
                if (hocKy == null) {
                    hocKy = lopHocPhan.getHocKy();
                }
            }
        }

        Subject finalMonHoc = monHoc;
        Semester finalHocKy = hocKy;
        Grade finalDiem = diem;

        List<Path> storedPaths = new ArrayList<>();
        registerRollbackFileCleanup(storedPaths);

        try {
            return auditLogService.execute("SUPPORT_REQUEST_CREATED", "SUPPORT_REQUEST", null, audit -> {
                LocalDateTime now = LocalDateTime.now();

                SupportRequest yeuCau = new SupportRequest();
                yeuCau.setMaPhieu(sinhMaPhieuDuyNhat());
                yeuCau.setSinhVien(sinhVien);
                yeuCau.setNguoiTao(actor);
                yeuCau.setLoaiYeuCau(loaiYeuCau);
                yeuCau.setTieuDe(tieuDe);
                yeuCau.setNoiDungGoc(noiDung);
                yeuCau.setMonHoc(finalMonHoc);
                yeuCau.setHocKy(finalHocKy);
                yeuCau.setDiem(finalDiem);
                yeuCau.setTrangThai(SupportRequestStatus.MOI_TAO);
                yeuCau.setMucUuTien(properties.queue().defaultPriority());

                // Tính thời hạn xử lý ban đầu theo lịch làm việc và SLA dự kiến
                double defaultResolveHours = properties.sla().targetResolveHours()
                        .getOrDefault(properties.queue().defaultPriority(), 72.0);
                LocalDateTime targetDeadline = calendarService.addWorkingHours(now, defaultResolveHours);
                yeuCau.setHanXuLy(targetDeadline);

                yeuCau.setCreatedAt(now);
                yeuCau.setUpdatedAt(now);

                List<SupportAttachmentStorageService.StoredFileResult> storedFiles = new ArrayList<>();
                for (MultipartFile file : files) {
                    SupportAttachmentStorageService.StoredFileResult stored = storageService.store(file);
                    storedPaths.add(stored.storedPath());
                    storedFiles.add(stored);
                }

                SupportRequest savedYeuCau = yeuCauRepository.saveAndFlush(yeuCau);

                SupportRequestStatusHistory initialHistory = new SupportRequestStatusHistory();
                initialHistory.setYeuCau(savedYeuCau);
                initialHistory.setLoaiThayDoi(SupportRequestChangeType.KHOI_TAO);
                initialHistory.setTrangThaiCu(null);
                initialHistory.setTrangThaiMoi(SupportRequestStatus.MOI_TAO);
                initialHistory.setGiaTriCu(null);
                initialHistory.setGiaTriMoi(SupportRequestStatus.MOI_TAO.name());
                initialHistory.setNguoiThucHien(actor);
                initialHistory.setTenNguoiThucHien(tenHienThi(actor, sinhVien));
                initialHistory.setVaiTroNguoiThucHien(actor.getRole());
                initialHistory.setGhiChu("Sinh viên khởi tạo phiếu yêu cầu hỗ trợ");
                initialHistory.setThoiDiem(now);
                SupportRequestStatusHistory savedHistory = lichSuTrangThaiRepository.saveAndFlush(initialHistory);

                List<SupportRequestAttachment> attachmentsToSave = new ArrayList<>();
                for (SupportAttachmentStorageService.StoredFileResult item : storedFiles) {
                    SupportRequestAttachment att = new SupportRequestAttachment();
                    att.setYeuCau(savedYeuCau);
                    att.setNguoiTaiLen(actor);
                    att.setTenTepGoc(item.originalFileName());
                    att.setTenLuuTru(item.storedFileName());
                    att.setKichThuoc(item.fileSize());
                    att.setContentType(item.contentType());
                    att.setChecksumSha256(item.checksumSha256());
                    att.setCreatedAt(now);
                    attachmentsToSave.add(att);
                }
                List<SupportRequestAttachment> savedAttachments = attachmentsToSave.isEmpty()
                        ? List.of()
                        : tepDinhKemRepository.saveAllAndFlush(attachmentsToSave);

                audit.entityId(savedYeuCau.getId());
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("maPhieu", savedYeuCau.getMaPhieu());
                details.put("loaiYeuCau", savedYeuCau.getLoaiYeuCau().name());
                details.put("trangThaiMoi", savedYeuCau.getTrangThai().name());
                details.put("mucUuTien", savedYeuCau.getMucUuTien().name());
                details.put("soTepDinhKem", savedAttachments.size());
                if (savedYeuCau.getMonHoc() != null) details.put("monHocId", savedYeuCau.getMonHoc().getId());
                if (savedYeuCau.getHocKy() != null) details.put("hocKyId", savedYeuCau.getHocKy().getId());
                if (savedYeuCau.getDiem() != null) details.put("diemId", savedYeuCau.getDiem().getId());
                audit.changes(null, details);

                log.info("Đã tạo phiếu hỗ trợ maPhieu={} loai={} soTep={}",
                        savedYeuCau.getMaPhieu(), savedYeuCau.getLoaiYeuCau(), savedAttachments.size());

                return toChiTietDto(savedYeuCau, actor, savedAttachments, List.of(), List.of(savedHistory));
            });
        } catch (RuntimeException ex) {
            storageService.cleanupPaths(storedPaths);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachCuaToi(String trangThai,
                                                                   String loaiYeuCau,
                                                                   String keyword,
                                                                   int page,
                                                                   int size) {
        User actor = currentUser.get();
        if (actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ sinh viên mới được truy cập danh sách phiếu của tôi");
        }
        Student sinhVien = sinhVienHienTai(actor);

        SupportRequestStatus statusFilter = parseOptionalTrangThai(trangThai);
        SupportRequestType typeFilter = parseOptionalLoaiYeuCau(loaiYeuCau);
        String safeKeyword = normalizeKeyword(keyword);

        PageRequest pageRequest = buildPageRequest(page, size);
        Page<SupportRequest> results = yeuCauRepository.searchBySinhVien(
                sinhVien.getId(), statusFilter, typeFilter, safeKeyword, pageRequest);

        return results.map(this::toTomTatDto);
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachQuanTri(String trangThai,
                                                                    String loaiYeuCau,
                                                                    String keyword,
                                                                    int page,
                                                                    int size) {
        return danhSachHangDoi("ALL", trangThai, loaiYeuCau, null, null, null, keyword, page, size);
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachHangDoi(String scope,
                                                                   String trangThai,
                                                                   String loaiYeuCau,
                                                                   String mucUuTien,
                                                                   String slaStatus,
                                                                   Long handlerUserId,
                                                                   String keyword,
                                                                   int page,
                                                                   int size) {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        String effectiveScope = (scope == null || scope.isBlank()) ? "ALL" : scope.trim().toUpperCase(Locale.ROOT);
        if (SupportRequestProperties.QueueProperties.SCOPE_MODE_ASSIGNED_ONLY.equalsIgnoreCase(properties.queue().scopeMode())) {
            effectiveScope = "MY_ASSIGNED";
        }

        Long onlyAssignedToUserId = null;
        boolean onlyUnassigned = false;

        if ("MY_ASSIGNED".equals(effectiveScope)) {
            onlyAssignedToUserId = actor.getId();
        } else if ("UNASSIGNED".equals(effectiveScope)) {
            onlyUnassigned = true;
        }

        if (handlerUserId != null && handlerUserId > 0) {
            onlyAssignedToUserId = handlerUserId;
        }

        SupportRequestStatus statusFilter = parseOptionalTrangThai(trangThai);
        SupportRequestType typeFilter = parseOptionalLoaiYeuCau(loaiYeuCau);
        SupportRequestPriority priorityFilter = parseOptionalPriority(mucUuTien);
        String safeKeyword = normalizeKeyword(keyword);

        boolean filterBySla = "OVERDUE".equalsIgnoreCase(effectiveScope)
                || (slaStatus != null && !slaStatus.isBlank());
        String targetSlaStatus = (slaStatus != null && !slaStatus.isBlank())
                ? slaStatus.trim().toUpperCase(Locale.ROOT)
                : ("OVERDUE".equalsIgnoreCase(effectiveScope) ? "OVERDUE" : null);

        PageRequest pageRequest = buildPageRequest(page, size);

        if (!filterBySla) {
            Page<SupportRequest> dbPage = yeuCauRepository.searchQueue(
                    statusFilter, typeFilter, priorityFilter, null,
                    onlyUnassigned, onlyAssignedToUserId, safeKeyword, pageRequest
            );
            return dbPage.map(this::toTomTatDto);
        }

        // Lọc theo trạng thái SLA tính theo lịch làm việc
        Page<SupportRequest> allMatching = yeuCauRepository.searchQueue(
                statusFilter, typeFilter, priorityFilter, null,
                onlyUnassigned, onlyAssignedToUserId, safeKeyword, PageRequest.of(0, 500, Sort.by(Sort.Order.desc("createdAt")))
        );

        LocalDateTime now = LocalDateTime.now();
        List<SupportRequestDtos.YeuCauTomTatDto> matchedList = allMatching.getContent().stream()
                .map(this::toTomTatDto)
                .filter(item -> {
                    if ("OVERDUE".equals(targetSlaStatus)) {
                        return item.isOverdue() || "OVERDUE".equalsIgnoreCase(item.slaStatus())
                                || "RESOLVED_OVERDUE".equalsIgnoreCase(item.slaStatus());
                    }
                    return targetSlaStatus.equalsIgnoreCase(item.slaStatus());
                })
                .toList();

        int start = Math.min((int) pageRequest.getOffset(), matchedList.size());
        int end = Math.min((start + pageRequest.getPageSize()), matchedList.size());
        List<SupportRequestDtos.YeuCauTomTatDto> subList = matchedList.subList(start, end);

        return new PageImpl<>(subList, pageRequest, matchedList.size());
    }

    @Transactional(readOnly = true)
    public SupportRequestDtos.SupportRequestSlaSummaryDto tomTatHangDoi() {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        List<SupportRequest> recentTickets = yeuCauRepository.findTop500ByOrderByCreatedAtDesc();
        LocalDateTime now = LocalDateTime.now();

        long totalInQueue = recentTickets.size();
        long unassignedCount = 0L;
        long myAssignedCount = 0L;
        long inProgressCount = 0L;
        long awaitingStudentCount = 0L;
        long overdueCount = 0L;
        long nearingDueCount = 0L;
        long resolvedCount = 0L;
        long onTimeResolvedCount = 0L;

        double sumFirstResponseWorkingMinutes = 0;
        int countWithFirstResponse = 0;
        double sumResolveWorkingMinutes = 0;
        int countWithResolve = 0;

        for (SupportRequest t : recentTickets) {
            if (t.getNguoiXuLy() == null) {
                unassignedCount++;
            } else if (actor.getId().equals(t.getNguoiXuLy().getId())) {
                myAssignedCount++;
            }

            if (t.getTrangThai() == SupportRequestStatus.DANG_XU_LY) {
                inProgressCount++;
            } else if (t.getTrangThai() == SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN) {
                awaitingStudentCount++;
            } else if (t.getTrangThai() == SupportRequestStatus.DA_GIAI_QUYET || t.getTrangThai() == SupportRequestStatus.DA_DONG) {
                resolvedCount++;
            }

            SupportRequestDtos.SupportRequestSlaDetailDto sla = calendarService.calculateSla(t, List.of(), now);
            if (sla != null) {
                if (sla.isOverdue() || "OVERDUE".equals(sla.slaStatus()) || "RESOLVED_OVERDUE".equals(sla.slaStatus())) {
                    overdueCount++;
                } else if ("NEARING_DUE".equals(sla.slaStatus())) {
                    nearingDueCount++;
                }

                if ("RESOLVED_ON_TIME".equals(sla.slaStatus())) {
                    onTimeResolvedCount++;
                }

                if (sla.firstResponseWorkingMinutes() != null) {
                    sumFirstResponseWorkingMinutes += sla.firstResponseWorkingMinutes();
                    countWithFirstResponse++;
                }

                if (sla.resolvedAt() != null) {
                    sumResolveWorkingMinutes += sla.activeWorkingMinutes();
                    countWithResolve++;
                }
            }
        }

        double avgFirstResponseHours = countWithFirstResponse > 0
                ? Math.round((sumFirstResponseWorkingMinutes / (countWithFirstResponse * 60.0)) * 10.0) / 10.0
                : 0.0;
        double avgResolveHours = countWithResolve > 0
                ? Math.round((sumResolveWorkingMinutes / (countWithResolve * 60.0)) * 10.0) / 10.0
                : 0.0;
        double onTimeRate = resolvedCount > 0
                ? Math.round((((double) onTimeResolvedCount) / resolvedCount * 100.0) * 10.0) / 10.0
                : 100.0;

        return new SupportRequestDtos.SupportRequestSlaSummaryDto(
                totalInQueue,
                unassignedCount,
                myAssignedCount,
                inProgressCount,
                awaitingStudentCount,
                overdueCount,
                nearingDueCount,
                resolvedCount,
                avgFirstResponseHours,
                avgResolveHours,
                onTimeRate,
                properties.sla().officialSlaEnabled(),
                properties.sla().policyNote(),
                properties.workingCalendar().policyNote()
        );
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto phanCong(Long yeuCauId, SupportRequestDtos.PhanCongRequest request) {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        if (request == null || request.handlerUserId() == null) {
            throw ApiException.badRequest("Mã người xử lý không được để trống");
        }

        User newHandler = timNguoiXuLyHopLe(request.handlerUserId());
        String ghiChu = normalizeOptionalText(request.ghiChu(), "Ghi chú phân công", 500);

        return auditLogService.execute("SUPPORT_REQUEST_ASSIGNED", "SUPPORT_REQUEST", yeuCauId, audit -> {
            SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);
            User oldHandler = yeuCau.getNguoiXuLy();
            String oldName = oldHandler != null ? oldHandler.getUsername() : "(Chưa phân công)";
            String newName = newHandler.getUsername();

            if (oldHandler != null && oldHandler.getId().equals(newHandler.getId())) {
                throw ApiException.badRequest("Phiếu hỗ trợ đã được phân công cho người dùng " + newName);
            }

            LocalDateTime now = LocalDateTime.now();
            yeuCau.setNguoiXuLy(newHandler);
            if (yeuCau.getTrangThai() == SupportRequestStatus.MOI_TAO) {
                yeuCau.setTrangThai(SupportRequestStatus.DANG_XU_LY);
            }
            yeuCau.setUpdatedAt(now);
            SupportRequest saved = yeuCauRepository.saveAndFlush(yeuCau);

            SupportRequestStatusHistory history = new SupportRequestStatusHistory();
            history.setYeuCau(saved);
            history.setLoaiThayDoi(SupportRequestChangeType.PHAN_CONG);
            history.setTrangThaiCu(yeuCau.getTrangThai());
            history.setTrangThaiMoi(saved.getTrangThai());
            history.setGiaTriCu(oldName);
            history.setGiaTriMoi(newName);
            history.setNguoiThucHien(actor);
            history.setTenNguoiThucHien(actor.getUsername());
            history.setVaiTroNguoiThucHien(actor.getRole());
            history.setGhiChu(ghiChu != null ? ghiChu : "Phân công người xử lý: " + newName);
            history.setThoiDiem(now);
            lichSuTrangThaiRepository.saveAndFlush(history);

            audit.changes(
                    Map.of("maPhieu", saved.getMaPhieu(), "nguoiXuLyCuUsername", oldName),
                    Map.of("maPhieu", saved.getMaPhieu(), "nguoiXuLyMoiUsername", newName, "ghiChu", ghiChu != null ? ghiChu : "")
            );

            log.info("Phân công phiếu maPhieu={} tu={} sang={} boi={}",
                    saved.getMaPhieu(), oldName, newName, actor.getUsername());

            if (notificationService != null) {
                notificationService.supportRequestAssigned(newHandler, saved.getMaPhieu(), saved.getTieuDe(), actor.getUsername());
            }

            return chiTiet(saved.getId());
        });
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto dieuChinh(Long yeuCauId, SupportRequestDtos.DieuChinhYeuCauRequest request) {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu điều chỉnh không hợp lệ");
        }
        String lyDo = validateRequiredText(request.ghiChu(), "Lý do điều chỉnh", 3, 500);

        return auditLogService.execute("SUPPORT_REQUEST_TRIAGED", "SUPPORT_REQUEST", yeuCauId, audit -> {
            SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);
            LocalDateTime now = LocalDateTime.now();
            boolean changed = false;

            Map<String, Object> oldValues = new LinkedHashMap<>();
            Map<String, Object> newValues = new LinkedHashMap<>();
            oldValues.put("maPhieu", yeuCau.getMaPhieu());
            newValues.put("maPhieu", yeuCau.getMaPhieu());

            // 1. Phân loại yêu cầu
            if (request.loaiYeuCau() != null && !request.loaiYeuCau().isBlank()) {
                SupportRequestType newType = parseAndValidateLoaiYeuCau(request.loaiYeuCau());
                if (newType != yeuCau.getLoaiYeuCau()) {
                    SupportRequestType oldType = yeuCau.getLoaiYeuCau();
                    yeuCau.setLoaiYeuCau(newType);
                    changed = true;
                    oldValues.put("loaiYeuCau", oldType.name());
                    newValues.put("loaiYeuCau", newType.name());

                    SupportRequestStatusHistory h = new SupportRequestStatusHistory();
                    h.setYeuCau(yeuCau);
                    h.setLoaiThayDoi(SupportRequestChangeType.PHAN_LOAI);
                    h.setTrangThaiCu(yeuCau.getTrangThai());
                    h.setTrangThaiMoi(yeuCau.getTrangThai());
                    h.setGiaTriCu(oldType.getMoTa());
                    h.setGiaTriMoi(newType.getMoTa());
                    h.setNguoiThucHien(actor);
                    h.setTenNguoiThucHien(actor.getUsername());
                    h.setVaiTroNguoiThucHien(actor.getRole());
                    h.setGhiChu(lyDo);
                    h.setThoiDiem(now);
                    lichSuTrangThaiRepository.saveAndFlush(h);
                }
            }

            // 2. Mức ưu tiên
            if (request.mucUuTien() != null && !request.mucUuTien().isBlank()) {
                SupportRequestPriority newPriority = parseAndValidatePriority(request.mucUuTien());
                if (newPriority != yeuCau.getMucUuTien()) {
                    SupportRequestPriority oldPriority = yeuCau.getMucUuTien();
                    yeuCau.setMucUuTien(newPriority);
                    changed = true;
                    oldValues.put("mucUuTien", oldPriority.name());
                    newValues.put("mucUuTien", newPriority.name());

                    // Nếu hạn xử lý chưa được gán cố định hoặc có thay đổi ưu tiên, cập nhật target deadline
                    if (request.hanXuLy() == null) {
                        double resolveHours = properties.sla().targetResolveHours().getOrDefault(newPriority, 72.0);
                        yeuCau.setHanXuLy(calendarService.addWorkingHours(yeuCau.getCreatedAt(), resolveHours));
                    }

                    SupportRequestStatusHistory h = new SupportRequestStatusHistory();
                    h.setYeuCau(yeuCau);
                    h.setLoaiThayDoi(SupportRequestChangeType.MUC_UU_TIEN);
                    h.setTrangThaiCu(yeuCau.getTrangThai());
                    h.setTrangThaiMoi(yeuCau.getTrangThai());
                    h.setGiaTriCu(oldPriority.getMoTa());
                    h.setGiaTriMoi(newPriority.getMoTa());
                    h.setNguoiThucHien(actor);
                    h.setTenNguoiThucHien(actor.getUsername());
                    h.setVaiTroNguoiThucHien(actor.getRole());
                    h.setGhiChu(lyDo);
                    h.setThoiDiem(now);
                    lichSuTrangThaiRepository.saveAndFlush(h);
                }
            }

            // 3. Hạn xử lý
            if (request.hanXuLy() != null) {
                LocalDateTime oldDue = yeuCau.getHanXuLy();
                LocalDateTime newDue = request.hanXuLy();
                if (oldDue == null || !oldDue.isEqual(newDue)) {
                    yeuCau.setHanXuLy(newDue);
                    changed = true;
                    oldValues.put("hanXuLy", oldDue != null ? oldDue.toString() : "");
                    newValues.put("hanXuLy", newDue.toString());

                    SupportRequestStatusHistory h = new SupportRequestStatusHistory();
                    h.setYeuCau(yeuCau);
                    h.setLoaiThayDoi(SupportRequestChangeType.HAN_XU_LY);
                    h.setTrangThaiCu(yeuCau.getTrangThai());
                    h.setTrangThaiMoi(yeuCau.getTrangThai());
                    h.setGiaTriCu(oldDue != null ? oldDue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "(Chưa đặt)");
                    h.setGiaTriMoi(newDue.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    h.setNguoiThucHien(actor);
                    h.setTenNguoiThucHien(actor.getUsername());
                    h.setVaiTroNguoiThucHien(actor.getRole());
                    h.setGhiChu(lyDo);
                    h.setThoiDiem(now);
                    lichSuTrangThaiRepository.saveAndFlush(h);
                }
            }

            if (!changed) {
                throw ApiException.badRequest("Không có thông tin nào thay đổi");
            }

            yeuCau.setUpdatedAt(now);
            SupportRequest saved = yeuCauRepository.saveAndFlush(yeuCau);
            audit.changes(oldValues, newValues);

            log.info("Điều chỉnh phiếu maPhieu={} boi={} lyDo={}", saved.getMaPhieu(), actor.getUsername(), lyDo);
            return chiTiet(saved.getId());
        });
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto yeuCauMinhChung(Long yeuCauId, SupportRequestDtos.YeuCauMinhChungRequest request) {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu yêu cầu bổ sung không hợp lệ");
        }
        String noiDung = validateRequiredText(request.noiDung(), "Nội dung yêu cầu bổ sung", 5, 1000);

        return auditLogService.execute("SUPPORT_REQUEST_EVIDENCE_REQUESTED", "SUPPORT_REQUEST", yeuCauId, audit -> {
            SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);
            SupportRequestStatus oldStatus = yeuCau.getTrangThai();

            LocalDateTime now = LocalDateTime.now();
            yeuCau.setTrangThai(SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN);
            yeuCau.setYeuCauBoSungMinhChung(true);
            yeuCau.setNoiDungYeuCauMinhChung(noiDung);
            if (request.hanBoSung() != null) {
                yeuCau.setHanXuLy(request.hanBoSung());
            }
            if (yeuCau.getNguoiXuLy() == null) {
                yeuCau.setNguoiXuLy(actor);
            }
            yeuCau.setUpdatedAt(now);
            SupportRequest saved = yeuCauRepository.saveAndFlush(yeuCau);

            // Ghi nhận lịch sử
            SupportRequestStatusHistory history = new SupportRequestStatusHistory();
            history.setYeuCau(saved);
            history.setLoaiThayDoi(SupportRequestChangeType.YEU_CAU_MINH_CHUNG);
            history.setTrangThaiCu(oldStatus);
            history.setTrangThaiMoi(SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN);
            history.setGiaTriCu(oldStatus.getMoTa());
            history.setGiaTriMoi(SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN.getMoTa());
            history.setNguoiThucHien(actor);
            history.setTenNguoiThucHien(actor.getUsername());
            history.setVaiTroNguoiThucHien(actor.getRole());
            history.setGhiChu(noiDung);
            history.setThoiDiem(now);
            lichSuTrangThaiRepository.saveAndFlush(history);

            // Đăng một trao đổi công khai để sinh viên thấy thông báo trực quan
            SupportRequestComment comment = new SupportRequestComment();
            comment.setYeuCau(saved);
            comment.setNguoiGui(actor);
            comment.setVaiTroNguoiGui(actor.getRole());
            comment.setTenHienThiNguoiGui("Phòng Đào tạo (" + actor.getUsername() + ")");
            comment.setNoiDung("[YÊU CẦU BỔ SUNG MINH CHỨNG]: " + noiDung);
            comment.setCongKhai(true);
            comment.setCreatedAt(now);
            traoDoiRepository.saveAndFlush(comment);

            audit.changes(
                    Map.of("maPhieu", saved.getMaPhieu(), "trangThaiCu", oldStatus.name()),
                    Map.of("maPhieu", saved.getMaPhieu(), "trangThaiMoi", SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN.name(), "noiDungYeuCau", noiDung)
            );

            log.info("Yêu cầu bổ sung minh chứng phiếu maPhieu={} boi={}", saved.getMaPhieu(), actor.getUsername());

            if (notificationService != null && saved.getSinhVien() != null && saved.getSinhVien().getUser() != null) {
                notificationService.supportRequestEvidenceRequested(
                        saved.getSinhVien().getUser(), saved.getMaPhieu(), saved.getTieuDe(), noiDung);
            }

            return chiTiet(saved.getId());
        });
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto boSungMinhChung(Long yeuCauId,
                                                               SupportRequestDtos.BoSungMinhChungRequest request,
                                                               List<MultipartFile> rawFiles) {
        User actor = currentUser.get();
        if (actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ sinh viên sở hữu phiếu mới được bổ sung minh chứng");
        }
        Student sinhVien = sinhVienHienTai(actor);
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);

        if (!coThePhanHoi(yeuCau)) {
            throw ApiException.badRequest("Phiếu hỗ trợ đang ở trạng thái "
                    + yeuCau.getTrangThai().getMoTa() + ", không thể gửi thêm bổ sung");
        }

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu bổ sung minh chứng không hợp lệ");
        }
        String noiDung = validateRequiredText(request.noiDung(), "Nội dung giải trình", 5, 2000);

        List<MultipartFile> files = normalizeMultipartFiles(rawFiles);
        int currentFileCount = tepDinhKemRepository.findByYeuCauIdOrderByIdAsc(yeuCau.getId()).size();
        if (currentFileCount + files.size() > properties.maxFilesPerRequest()) {
            throw ApiException.badRequest("Tổng số tệp đính kèm vượt quá giới hạn ("
                    + properties.maxFilesPerRequest() + " tệp). Phiếu hiện đã có " + currentFileCount + " tệp.");
        }

        List<Path> storedPaths = new ArrayList<>();
        registerRollbackFileCleanup(storedPaths);

        try {
            return auditLogService.execute("SUPPORT_REQUEST_EVIDENCE_SUBMITTED", "SUPPORT_REQUEST", yeuCauId, audit -> {
                LocalDateTime now = LocalDateTime.now();

                List<SupportAttachmentStorageService.StoredFileResult> storedResults = new ArrayList<>();
                for (MultipartFile f : files) {
                    SupportAttachmentStorageService.StoredFileResult stored = storageService.store(f);
                    storedPaths.add(stored.storedPath());
                    storedResults.add(stored);
                }

                List<SupportRequestAttachment> toSave = new ArrayList<>();
                for (SupportAttachmentStorageService.StoredFileResult item : storedResults) {
                    SupportRequestAttachment att = new SupportRequestAttachment();
                    att.setYeuCau(yeuCau);
                    att.setNguoiTaiLen(actor);
                    att.setTenTepGoc(item.originalFileName());
                    att.setTenLuuTru(item.storedFileName());
                    att.setKichThuoc(item.fileSize());
                    att.setContentType(item.contentType());
                    att.setChecksumSha256(item.checksumSha256());
                    att.setCreatedAt(now);
                    toSave.add(att);
                }
                if (!toSave.isEmpty()) {
                    tepDinhKemRepository.saveAllAndFlush(toSave);
                }

                SupportRequestStatus oldStatus = yeuCau.getTrangThai();
                yeuCau.setYeuCauBoSungMinhChung(false);
                if (oldStatus == SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN) {
                    yeuCau.setTrangThai(SupportRequestStatus.DANG_XU_LY);
                }
                yeuCau.setUpdatedAt(now);
                SupportRequest saved = yeuCauRepository.saveAndFlush(yeuCau);

                // Lưu trao đổi
                SupportRequestComment c = new SupportRequestComment();
                c.setYeuCau(saved);
                c.setNguoiGui(actor);
                c.setVaiTroNguoiGui(actor.getRole());
                c.setTenHienThiNguoiGui(tenHienThi(actor, sinhVien));
                c.setNoiDung("[ĐÃ BỔ SUNG MINH CHỨNG]: " + noiDung + (storedResults.isEmpty() ? "" : " (Đính kèm " + storedResults.size() + " tệp)"));
                c.setCongKhai(true);
                c.setCreatedAt(now);
                traoDoiRepository.saveAndFlush(c);

                // Ghi lịch sử
                SupportRequestStatusHistory history = new SupportRequestStatusHistory();
                history.setYeuCau(saved);
                history.setLoaiThayDoi(SupportRequestChangeType.BO_SUNG_MINH_CHUNG);
                history.setTrangThaiCu(oldStatus);
                history.setTrangThaiMoi(saved.getTrangThai());
                history.setGiaTriCu("Chờ bổ sung");
                history.setGiaTriMoi("Đã bổ sung minh chứng (" + storedResults.size() + " tệp)");
                history.setNguoiThucHien(actor);
                history.setTenNguoiThucHien(tenHienThi(actor, sinhVien));
                history.setVaiTroNguoiThucHien(actor.getRole());
                history.setGhiChu(noiDung);
                history.setThoiDiem(now);
                lichSuTrangThaiRepository.saveAndFlush(history);

                audit.changes(
                        Map.of("maPhieu", saved.getMaPhieu(), "trangThaiCu", oldStatus.name()),
                        Map.of("maPhieu", saved.getMaPhieu(), "trangThaiMoi", saved.getTrangThai().name(), "soTepBoSung", storedResults.size())
                );

                log.info("Sinh viên ma={} bổ sung minh chứng cho phiếu maPhieu={}",
                        sinhVien.getMa(), saved.getMaPhieu());

                if (notificationService != null && saved.getNguoiXuLy() != null) {
                    notificationService.supportRequestCommentAdded(
                            saved.getNguoiXuLy(), saved.getMaPhieu(), saved.getTieuDe(),
                            sinhVien.getHoTen(), "Sinh viên đã bổ sung minh chứng: " + noiDung, false);
                }

                return chiTiet(saved.getId());
            });
        } catch (RuntimeException ex) {
            storageService.cleanupPaths(storedPaths);
            throw ex;
        }
    }

    @Transactional
    public SupportRequestDtos.TraoDoiDto themTraoDoi(Long yeuCauId, SupportRequestDtos.ThemTraoDoiRequest request) {
        User actor = currentUser.get();
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu trao đổi không hợp lệ");
        }
        String noiDung = validateRequiredText(request.noiDung(), "Nội dung trao đổi", 1, 2000);

        if (!coThePhanHoi(yeuCau)) {
            throw ApiException.badRequest("Phiếu hỗ trợ đang ở trạng thái "
                    + yeuCau.getTrangThai().getMoTa() + ", không thể gửi thêm trao đổi");
        }

        boolean isNoiBo = actor.getRole() != Role.SINH_VIEN
                && request.isNoiBo() != null
                && request.isNoiBo();

        return auditLogService.execute("SUPPORT_REQUEST_COMMENT_ADDED", "SUPPORT_REQUEST", yeuCau.getId(), audit -> {
            LocalDateTime now = LocalDateTime.now();
            Student sinhVienActor = actor.getRole() == Role.SINH_VIEN
                    ? yeuCau.getSinhVien()
                    : null;

            SupportRequestComment traoDoi = new SupportRequestComment();
            traoDoi.setYeuCau(yeuCau);
            traoDoi.setNguoiGui(actor);
            traoDoi.setVaiTroNguoiGui(actor.getRole());
            traoDoi.setTenHienThiNguoiGui(tenHienThi(actor, sinhVienActor));
            traoDoi.setNoiDung(noiDung);
            traoDoi.setCongKhai(!isNoiBo);
            traoDoi.setCreatedAt(now);

            SupportRequestComment saved = traoDoiRepository.saveAndFlush(traoDoi);

            if (actor.getRole() != Role.SINH_VIEN) {
                if (!isNoiBo && yeuCau.getThoiDiemPhanHoiDau() == null) {
                    yeuCau.setThoiDiemPhanHoiDau(now);
                }
                if (yeuCau.getNguoiXuLy() == null) {
                    yeuCau.setNguoiXuLy(actor);
                }
            }
            yeuCau.setUpdatedAt(now);
            yeuCauRepository.saveAndFlush(yeuCau);

            audit.changes(
                    Map.of("maPhieu", yeuCau.getMaPhieu(), "trangThai", yeuCau.getTrangThai().name()),
                    Map.of("maPhieu", yeuCau.getMaPhieu(), "traoDoiId", saved.getId(), "isNoiBo", isNoiBo)
            );

            // Gửi thông báo
            if (notificationService != null) {
                if (actor.getRole() != Role.SINH_VIEN) {
                    // Nếu là phản hồi công khai từ nhân viên, báo cho sinh viên
                    if (!isNoiBo && yeuCau.getSinhVien() != null && yeuCau.getSinhVien().getUser() != null) {
                        notificationService.supportRequestCommentAdded(
                                yeuCau.getSinhVien().getUser(), yeuCau.getMaPhieu(), yeuCau.getTieuDe(),
                                "Phòng Đào tạo", noiDung, false);
                    }
                } else {
                    // Sinh viên phản hồi, báo cho người xử lý
                    if (yeuCau.getNguoiXuLy() != null) {
                        notificationService.supportRequestCommentAdded(
                                yeuCau.getNguoiXuLy(), yeuCau.getMaPhieu(), yeuCau.getTieuDe(),
                                actor.getUsername(), noiDung, false);
                    }
                }
            }

            return toTraoDoiDto(saved);
        });
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto doiTrangThai(Long yeuCauId,
                                                            SupportRequestDtos.DoiTrangThaiRequest request) {
        User actor = currentUser.get();
        kiemTraQuyenNhanVien(actor);

        if (request == null) {
            throw ApiException.badRequest("Dữ liệu cập nhật trạng thái không hợp lệ");
        }

        SupportRequestStatus trangThaiMoi = parseAndValidateTrangThai(request.trangThai());
        String ghiChu = normalizeOptionalText(request.ghiChu(), "Ghi chú xử lý", 500);

        return auditLogService.execute("SUPPORT_REQUEST_STATUS_CHANGED", "SUPPORT_REQUEST", yeuCauId, audit -> {
            SupportRequest yeuCau = yeuCauRepository.findWithDetailsById(yeuCauId)
                    .orElseThrow(() -> ApiException.notFound("Phiếu hỗ trợ không tồn tại"));

            SupportRequestStatus trangThaiCu = yeuCau.getTrangThai();
            if (trangThaiCu == trangThaiMoi) {
                throw ApiException.badRequest("Phiếu hỗ trợ đã ở trạng thái " + trangThaiMoi.getMoTa());
            }
            if (!trangThaiCu.coTheChuyenSang(trangThaiMoi)) {
                throw ApiException.badRequest("Không thể chuyển trạng thái từ '"
                        + trangThaiCu.getMoTa() + "' sang '" + trangThaiMoi.getMoTa() + "'");
            }

            LocalDateTime now = LocalDateTime.now();
            yeuCau.setTrangThai(trangThaiMoi);

            if (yeuCau.getThoiDiemPhanHoiDau() == null) {
                yeuCau.setThoiDiemPhanHoiDau(now);
            }
            if (yeuCau.getNguoiXuLy() == null) {
                yeuCau.setNguoiXuLy(actor);
            }

            if (trangThaiMoi == SupportRequestStatus.DA_GIAI_QUYET) {
                yeuCau.setThoiDiemGiaiQuyet(now);
                yeuCau.setYeuCauBoSungMinhChung(false);
            } else if (trangThaiMoi == SupportRequestStatus.DA_DONG || trangThaiMoi == SupportRequestStatus.TU_CHOI) {
                yeuCau.setThoiDiemDong(now);
                yeuCau.setYeuCauBoSungMinhChung(false);
            }

            yeuCau.setUpdatedAt(now);
            SupportRequest savedYeuCau = yeuCauRepository.saveAndFlush(yeuCau);

            SupportRequestStatusHistory lichSu = new SupportRequestStatusHistory();
            lichSu.setYeuCau(savedYeuCau);
            lichSu.setLoaiThayDoi(SupportRequestChangeType.TRANG_THAI);
            lichSu.setTrangThaiCu(trangThaiCu);
            lichSu.setTrangThaiMoi(trangThaiMoi);
            lichSu.setGiaTriCu(trangThaiCu.getMoTa());
            lichSu.setGiaTriMoi(trangThaiMoi.getMoTa());
            lichSu.setNguoiThucHien(actor);
            lichSu.setTenNguoiThucHien(actor.getUsername());
            lichSu.setVaiTroNguoiThucHien(actor.getRole());
            lichSu.setGhiChu(ghiChu);
            lichSu.setThoiDiem(now);
            lichSuTrangThaiRepository.saveAndFlush(lichSu);

            audit.changes(
                    Map.of("maPhieu", savedYeuCau.getMaPhieu(), "trangThaiCu", trangThaiCu.name()),
                    Map.of("maPhieu", savedYeuCau.getMaPhieu(), "trangThaiMoi", trangThaiMoi.name())
            );

            log.info("Đã đổi trạng thái phiếu hỗ trợ maPhieu={} tu={} sang={} boi={}",
                    savedYeuCau.getMaPhieu(), trangThaiCu, trangThaiMoi, actor.getUsername());

            if (notificationService != null && savedYeuCau.getSinhVien() != null && savedYeuCau.getSinhVien().getUser() != null) {
                notificationService.supportRequestStatusChanged(
                        savedYeuCau.getSinhVien().getUser(), savedYeuCau.getMaPhieu(), savedYeuCau.getTieuDe(),
                        trangThaiCu.getMoTa(), trangThaiMoi.getMoTa(), ghiChu);
            }

            return chiTiet(savedYeuCau.getId());
        });
    }

    @Transactional(readOnly = true)
    public SupportRequestDtos.YeuCauChiTietDto chiTiet(Long id) {
        User actor = currentUser.get();
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(id, actor);

        List<SupportRequestAttachment> tepDinhKem = tepDinhKemRepository.findByYeuCauIdOrderByIdAsc(yeuCau.getId());
        List<SupportRequestComment> allTraoDoi = traoDoiRepository.findByYeuCauIdOrderByCreatedAtAscIdAsc(yeuCau.getId());

        // Sinh viên chỉ thấy trao đổi công khai; nhân viên thấy cả nội bộ
        List<SupportRequestComment> visibleTraoDoi = actor.getRole() == Role.SINH_VIEN
                ? allTraoDoi.stream().filter(SupportRequestComment::isCongKhai).toList()
                : allTraoDoi;

        List<SupportRequestStatusHistory> lichSu = lichSuTrangThaiRepository.findByYeuCauIdOrderByThoiDiemAscIdAsc(yeuCau.getId());
        return toChiTietDto(yeuCau, actor, tepDinhKem, visibleTraoDoi, lichSu);
    }

    @Transactional(readOnly = true)
    public TepTaiXuong taiTepDinhKem(Long yeuCauId, Long attachmentId) {
        User actor = currentUser.get();
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);
        SupportRequestAttachment tep = tepDinhKemRepository.findByIdAndYeuCauId(attachmentId, yeuCau.getId())
                .orElseThrow(() -> ApiException.notFound("Tệp đính kèm không tồn tại trên phiếu hỗ trợ này"));
        byte[] noiDung = storageService.readStoredFile(tep.getTenLuuTru());
        return new TepTaiXuong(tep.getTenTepGoc(), tep.getContentType(), noiDung);
    }

    @Transactional(readOnly = true)
    public List<SupportRequestDtos.HandlerUserDto> danhSachNhanVienXuLy() {
        if (userRepository == null) {
            return List.of();
        }
        List<Role> roles = properties.queue().allowedHandlerRoles();
        List<User> handlers = userRepository.findByRoleInAndTrangThaiTrueOrderByUsernameAsc(roles);
        return handlers.stream()
                .map(u -> new SupportRequestDtos.HandlerUserDto(
                        u.getId(),
                        u.getUsername(),
                        u.getUsername() + " (" + u.getRole().name() + ")",
                        u.getRole().name()
                ))
                .toList();
    }

    public boolean isEligibleHandler(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return properties.queue().allowedHandlerRoles().contains(user.getRole())
                || user.getRole() == Role.QUAN_TRI;
    }

    private void kiemTraQuyenNhanVien(User actor) {
        if (!isEligibleHandler(actor)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập hoặc xử lý hàng đợi phiếu hỗ trợ");
        }
    }

    private User timNguoiXuLyHopLe(Long userId) {
        if (userRepository == null) {
            throw ApiException.badRequest("Không thể tìm kiếm người xử lý do cấu hình repository");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));
        if (!user.isTrangThai()) {
            throw ApiException.badRequest("Tài khoản người dùng này đã bị khóa");
        }
        if (!isEligibleHandler(user)) {
            throw ApiException.badRequest("Người dùng này không có vai trò xử lý hỗ trợ");
        }
        return user;
    }

    private SupportRequest timVaKiemTraQuyenTruyCap(Long yeuCauId, User actor) {
        if (yeuCauId == null || yeuCauId <= 0) {
            throw ApiException.badRequest("Mã định danh phiếu hỗ trợ không hợp lệ");
        }
        if (actor.getRole() != Role.SINH_VIEN && !isEligibleHandler(actor)) {
            throw ApiException.forbidden("Bạn không có quyền truy cập phiếu hỗ trợ này");
        }
        SupportRequest yeuCau = yeuCauRepository.findWithDetailsById(yeuCauId)
                .orElseThrow(() -> ApiException.notFound("Phiếu hỗ trợ không tồn tại"));

        if (actor.getRole() == Role.SINH_VIEN) {
            Long ownerUserId = yeuCau.getSinhVien() != null && yeuCau.getSinhVien().getUser() != null
                    ? yeuCau.getSinhVien().getUser().getId()
                    : null;
            if (ownerUserId == null || !ownerUserId.equals(actor.getId())) {
                throw ApiException.forbidden("Bạn không có quyền truy cập phiếu hỗ trợ của sinh viên khác");
            }
        } else if (SupportRequestProperties.QueueProperties.SCOPE_MODE_ASSIGNED_ONLY.equalsIgnoreCase(properties.queue().scopeMode())) {
            // Nếu cấu hình phạm vi nghiêm ngặt, chỉ xem phiếu được phân công
            if (yeuCau.getNguoiXuLy() != null && !actor.getId().equals(yeuCau.getNguoiXuLy().getId())) {
                throw ApiException.forbidden("Phiếu hỗ trợ này không thuộc phạm vi xử lý được phân công của bạn");
            }
        }
        return yeuCau;
    }

    private Student sinhVienHienTai(User user) {
        return sinhVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên của tài khoản hiện tại"));
    }

    private void registerRollbackFileCleanup(List<Path> storedPaths) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        storageService.cleanupPaths(storedPaths);
                    }
                }
            });
        }
    }

    private List<MultipartFile> normalizeMultipartFiles(List<MultipartFile> rawFiles) {
        if (rawFiles == null || rawFiles.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> valid = new ArrayList<>();
        for (MultipartFile file : rawFiles) {
            if (file == null) {
                continue;
            }
            if (file.isEmpty()) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.isBlank()) {
                    continue;
                }
            }
            valid.add(file);
        }
        return valid;
    }

    private SupportRequestType parseAndValidateLoaiYeuCau(String raw) {
        SupportRequestType loai = SupportRequestType.fromCode(raw)
                .orElseThrow(() -> ApiException.badRequest("Loại yêu cầu không hợp lệ"));
        if (!properties.allowedTypes().contains(loai)) {
            throw ApiException.badRequest("Loại yêu cầu này hiện không nằm trong danh mục cho phép");
        }
        return loai;
    }

    private SupportRequestStatus parseAndValidateTrangThai(String raw) {
        SupportRequestStatus trangThai = SupportRequestStatus.fromCode(raw)
                .orElseThrow(() -> ApiException.badRequest("Trạng thái phiếu hỗ trợ không hợp lệ"));
        if (!properties.allowedStatuses().contains(trangThai)) {
            throw ApiException.badRequest("Trạng thái phiếu hỗ trợ không nằm trong danh mục cho phép");
        }
        return trangThai;
    }

    private SupportRequestPriority parseAndValidatePriority(String raw) {
        SupportRequestPriority p = SupportRequestPriority.fromCode(raw)
                .orElseThrow(() -> ApiException.badRequest("Mức ưu tiên không hợp lệ"));
        if (!properties.queue().allowedPriorities().contains(p)) {
            throw ApiException.badRequest("Mức ưu tiên không nằm trong danh mục cho phép");
        }
        return p;
    }

    private SupportRequestType parseOptionalLoaiYeuCau(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseAndValidateLoaiYeuCau(raw);
    }

    private SupportRequestStatus parseOptionalTrangThai(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseAndValidateTrangThai(raw);
    }

    private SupportRequestPriority parseOptionalPriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseAndValidatePriority(raw);
    }

    private String validateRequiredText(String raw, String fieldLabel, int minLen, int maxLen) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest(fieldLabel + " không được để trống");
        }
        String trimmed = raw.trim();
        if (trimmed.length() < minLen || trimmed.length() > maxLen) {
            throw ApiException.badRequest(fieldLabel + " phải có độ dài từ " + minLen + " đến " + maxLen + " ký tự");
        }
        return trimmed;
    }

    private String normalizeOptionalText(String raw, String fieldLabel, int maxLen) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.length() > maxLen) {
            throw ApiException.badRequest(fieldLabel + " tối đa " + maxLen + " ký tự");
        }
        return trimmed;
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private PageRequest buildPageRequest(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private String sinhMaPhieuDuyNhat() {
        String datePart = LocalDate.now().format(TICKET_DATE_FORMAT);
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            String candidate = "HT-" + datePart + "-" + suffix;
            if (!yeuCauRepository.existsByMaPhieu(candidate)) {
                return candidate;
            }
        }
        return "HT-" + datePart + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private boolean coThePhanHoi(SupportRequest yeuCau) {
        return properties.commentAllowedStatuses().contains(yeuCau.getTrangThai());
    }

    private String tenHienThi(User user, Student sinhVien) {
        if (sinhVien != null && sinhVien.getHoTen() != null && !sinhVien.getHoTen().isBlank()) {
            return sinhVien.getHoTen() + " (" + sinhVien.getMa() + ")";
        }
        if (user.getRole() == Role.QUAN_TRI) {
            return "Phòng Đào tạo (" + user.getUsername() + ")";
        }
        return user.getUsername();
    }

    private SupportRequestDtos.MonHocLienQuanDto toMonHocDto(Subject monHoc) {
        if (monHoc == null) {
            return null;
        }
        return new SupportRequestDtos.MonHocLienQuanDto(
                monHoc.getId(), monHoc.getMa(), monHoc.getTen(), monHoc.getSoTinChi()
        );
    }

    private SupportRequestDtos.HocKyLienQuanDto toHocKyDto(Semester hocKy) {
        if (hocKy == null) {
            return null;
        }
        return new SupportRequestDtos.HocKyLienQuanDto(hocKy.getId(), hocKy.getTen(), hocKy.getNamHoc());
    }

    private SupportRequestDtos.DiemLienQuanDto toDiemDto(Grade diem) {
        if (diem == null) {
            return null;
        }
        CourseRegistration dangKy = diem.getDangKy();
        CourseClass lop = dangKy != null ? dangKy.getLopHocPhan() : null;
        Subject mon = lop != null ? lop.getMonHoc() : null;
        Semester hk = lop != null ? lop.getHocKy() : null;
        boolean daCongBo = lop != null && lop.isBangDiemDaCongBo() && diem.getDiemTongKet() != null;
        return new SupportRequestDtos.DiemLienQuanDto(
                diem.getId(),
                dangKy != null ? dangKy.getId() : null,
                mon != null ? mon.getId() : null,
                mon != null ? mon.getMa() : null,
                mon != null ? mon.getTen() : null,
                hk != null ? hk.getId() : null,
                hk != null ? hk.getTen() : null,
                hk != null ? hk.getNamHoc() : null,
                lop != null ? lop.getMa() : null,
                daCongBo ? diem.getDiemQuaTrinh() : null,
                daCongBo ? diem.getDiemCuoiKy() : null,
                daCongBo ? diem.getDiemTongKet() : null
        );
    }

    private SupportRequestDtos.TepDinhKemDto toTepDto(Long yeuCauId, SupportRequestAttachment tep) {
        return new SupportRequestDtos.TepDinhKemDto(
                tep.getId(),
                tep.getTenTepGoc(),
                tep.getContentType(),
                tep.getKichThuoc(),
                tep.getCreatedAt(),
                "/api/yeu-cau-ho-tro/" + yeuCauId + "/tep-dinh-kem/" + tep.getId()
        );
    }

    private SupportRequestDtos.TraoDoiDto toTraoDoiDto(SupportRequestComment traoDoi) {
        return new SupportRequestDtos.TraoDoiDto(
                traoDoi.getId(),
                traoDoi.getNguoiGui() != null ? traoDoi.getNguoiGui().getId() : null,
                traoDoi.getTenHienThiNguoiGui(),
                traoDoi.getVaiTroNguoiGui().name(),
                traoDoi.getNoiDung(),
                traoDoi.isCongKhai(),
                !traoDoi.isCongKhai(),
                traoDoi.getCreatedAt()
        );
    }

    private SupportRequestDtos.LichSuTrangThaiDto toLichSuDto(SupportRequestStatusHistory ls) {
        return new SupportRequestDtos.LichSuTrangThaiDto(
                ls.getId(),
                ls.getLoaiThayDoi() != null ? ls.getLoaiThayDoi().name() : SupportRequestChangeType.TRANG_THAI.name(),
                ls.getLoaiThayDoi() != null ? ls.getLoaiThayDoi().getMoTa() : SupportRequestChangeType.TRANG_THAI.getMoTa(),
                ls.getTrangThaiCu() != null ? ls.getTrangThaiCu().name() : null,
                ls.getTrangThaiCu() != null ? ls.getTrangThaiCu().getMoTa() : null,
                ls.getTrangThaiMoi().name(),
                ls.getTrangThaiMoi().getMoTa(),
                ls.getGiaTriCu(),
                ls.getGiaTriMoi(),
                ls.getNguoiThucHien() != null ? ls.getNguoiThucHien().getId() : null,
                ls.getTenNguoiThucHien(),
                ls.getVaiTroNguoiThucHien().name(),
                ls.getGhiChu(),
                ls.getThoiDiem()
        );
    }

    private SupportRequestDtos.YeuCauTomTatDto toTomTatDto(SupportRequest yeuCau) {
        Student sv = yeuCau.getSinhVien();
        SupportRequestDtos.SupportRequestSlaDetailDto sla = calendarService.calculateSla(yeuCau, List.of(), LocalDateTime.now());

        String slaStatus = sla != null ? sla.slaStatus() : "ON_TIME";
        String tenSlaStatus = sla != null ? sla.tenSlaStatus() : "Đang trong hạn";
        boolean isOverdue = sla != null && sla.isOverdue();

        SupportRequestPriority p = yeuCau.getMucUuTien() != null ? yeuCau.getMucUuTien() : SupportRequestPriority.TRUNG_BINH;
        String nguoiXuLyHoTen = yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null;

        return new SupportRequestDtos.YeuCauTomTatDto(
                yeuCau.getId(),
                yeuCau.getMaPhieu(),
                sv != null ? sv.getId() : null,
                sv != null ? sv.getMa() : null,
                sv != null ? sv.getHoTen() : null,
                yeuCau.getLoaiYeuCau().name(),
                yeuCau.getLoaiYeuCau().getMoTa(),
                yeuCau.getTieuDe(),
                yeuCau.getTrangThai().name(),
                yeuCau.getTrangThai().getMoTa(),
                p.name(),
                p.getMoTa(),
                yeuCau.getHanXuLy(),
                toMonHocDto(yeuCau.getMonHoc()),
                toDiemDto(yeuCau.getDiem()),
                toHocKyDto(yeuCau.getHocKy()),
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getId() : null,
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null,
                nguoiXuLyHoTen,
                slaStatus,
                tenSlaStatus,
                isOverdue,
                yeuCau.isYeuCauBoSungMinhChung(),
                yeuCau.getCreatedAt(),
                yeuCau.getUpdatedAt()
        );
    }

    private SupportRequestDtos.YeuCauChiTietDto toChiTietDto(SupportRequest yeuCau,
                                                             User viewer,
                                                             List<SupportRequestAttachment> tepDinhKem,
                                                             List<SupportRequestComment> traoDoi,
                                                             List<SupportRequestStatusHistory> lichSu) {
        Student sv = yeuCau.getSinhVien();
        List<String> nextStatuses = isEligibleHandler(viewer)
                ? yeuCau.getTrangThai().trangThaiTiepTheoHopLe().stream()
                .filter(properties.allowedStatuses()::contains)
                .map(Enum::name)
                .toList()
                : List.of();

        SupportRequestDtos.SupportRequestSlaDetailDto sla = calendarService.calculateSla(yeuCau, lichSu, LocalDateTime.now());
        SupportRequestPriority p = yeuCau.getMucUuTien() != null ? yeuCau.getMucUuTien() : SupportRequestPriority.TRUNG_BINH;

        return new SupportRequestDtos.YeuCauChiTietDto(
                yeuCau.getId(),
                yeuCau.getMaPhieu(),
                sv != null ? sv.getId() : null,
                sv != null ? sv.getMa() : null,
                sv != null ? sv.getHoTen() : null,
                yeuCau.getLoaiYeuCau().name(),
                yeuCau.getLoaiYeuCau().getMoTa(),
                yeuCau.getTieuDe(),
                yeuCau.getNoiDungGoc(),
                yeuCau.getTrangThai().name(),
                yeuCau.getTrangThai().getMoTa(),
                p.name(),
                p.getMoTa(),
                yeuCau.getHanXuLy(),
                toMonHocDto(yeuCau.getMonHoc()),
                toDiemDto(yeuCau.getDiem()),
                toHocKyDto(yeuCau.getHocKy()),
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getId() : null,
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null,
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null,
                yeuCau.isYeuCauBoSungMinhChung(),
                yeuCau.getNoiDungYeuCauMinhChung(),
                sla,
                yeuCau.getCreatedAt(),
                yeuCau.getUpdatedAt(),
                coThePhanHoi(yeuCau),
                nextStatuses,
                tepDinhKem != null ? tepDinhKem.stream().filter(java.util.Objects::nonNull).map(t -> toTepDto(yeuCau.getId(), t)).toList() : List.of(),
                traoDoi.stream().map(this::toTraoDoiDto).toList(),
                lichSu.stream().map(this::toLichSuDto).toList()
        );
    }
}
