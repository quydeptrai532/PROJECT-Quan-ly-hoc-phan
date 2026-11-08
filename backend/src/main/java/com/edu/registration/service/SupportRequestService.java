package com.edu.registration.service;

import com.edu.registration.config.SupportRequestProperties;
import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.SupportRequestStatusHistory;
import com.edu.registration.entity.SupportRequestType;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.SupportRequestAttachment;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestComment;
import com.edu.registration.entity.User;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SupportRequestStatusHistoryRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SupportRequestAttachmentRepository;
import com.edu.registration.repository.SupportRequestCommentRepository;
import com.edu.registration.repository.SupportRequestRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private final CurrentUser currentUser;
    private final SupportAttachmentStorageService storageService;
    private final AuditLogService auditLogService;
    private final SupportRequestProperties properties;

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
        this.yeuCauRepository = yeuCauRepository;
        this.traoDoiRepository = traoDoiRepository;
        this.lichSuTrangThaiRepository = lichSuTrangThaiRepository;
        this.tepDinhKemRepository = tepDinhKemRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.monHocRepository = monHocRepository;
        this.hocKyRepository = hocKyRepository;
        this.diemRepository = diemRepository;
        this.currentUser = currentUser;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.properties = properties != null ? properties : SupportRequestProperties.defaults();
    }

    public record TepTaiXuong(String tenTepGoc, String contentType, byte[] noiDung) {
    }

    @Transactional(readOnly = true)
    public SupportRequestDtos.DanhMucHoTroDto danhMuc() {
        User actor = currentUser.get();
        if (actor.getRole() != Role.SINH_VIEN && actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Bạn không có quyền truy cập danh mục hỗ trợ");
        }

        List<SupportRequestDtos.DanhMucMucDto> loaiYeuCau = properties.allowedTypes().stream()
                .map(item -> new SupportRequestDtos.DanhMucMucDto(item.name(), item.getMoTa()))
                .toList();
        List<SupportRequestDtos.DanhMucMucDto> trangThai = properties.allowedStatuses().stream()
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

        return new SupportRequestDtos.DanhMucHoTroDto(loaiYeuCau, trangThai, chinhSachTep, monHoc, hocKy, diemCuaToi);
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
                initialHistory.setTrangThaiCu(null);
                initialHistory.setTrangThaiMoi(SupportRequestStatus.MOI_TAO);
                initialHistory.setNguoiThucHien(actor);
                initialHistory.setTenNguoiThucHien(tenHienThi(actor, sinhVien));
                initialHistory.setVaiTroNguoiThucHien(actor.getRole());
                initialHistory.setGhiChu("Sinh viên khởi tạo phiếu yêu cầu hỗ trợ");
                initialHistory.setThoiDiem(now);
                SupportRequestStatusHistory savedHistory = lichSuTrangThaiRepository.saveAndFlush(initialHistory);

                List<SupportRequestAttachment> attachments = new ArrayList<>();
                for (SupportAttachmentStorageService.StoredFileResult stored : storedFiles) {
                    SupportRequestAttachment tep = new SupportRequestAttachment();
                    tep.setYeuCau(savedYeuCau);
                    tep.setNguoiTaiLen(actor);
                    tep.setTenTepGoc(stored.originalFileName());
                    tep.setTenLuuTru(stored.storedFileName());
                    tep.setContentType(stored.contentType());
                    tep.setKichThuoc(stored.fileSize());
                    tep.setChecksumSha256(stored.checksumSha256());
                    tep.setCreatedAt(now);
                    attachments.add(tep);
                }
                List<SupportRequestAttachment> savedAttachments = attachments.isEmpty()
                        ? List.of()
                        : tepDinhKemRepository.saveAllAndFlush(attachments);

                audit.entityId(savedYeuCau.getId());
                Map<String, Object> after = new LinkedHashMap<>();
                after.put("maPhieu", savedYeuCau.getMaPhieu());
                after.put("loaiYeuCau", savedYeuCau.getLoaiYeuCau().name());
                after.put("trangThai", savedYeuCau.getTrangThai().name());
                after.put("soTepDinhKem", savedAttachments.size());
                if (finalMonHoc != null) after.put("monHocId", finalMonHoc.getId());
                if (finalHocKy != null) after.put("hocKyId", finalHocKy.getId());
                if (finalDiem != null) after.put("diemId", finalDiem.getId());
                audit.changes(Map.of(), after);

                log.info("Đã tạo phiếu hỗ trợ maPhieu={} loai={} soTep={}",
                        savedYeuCau.getMaPhieu(), savedYeuCau.getLoaiYeuCau(), savedAttachments.size());

                return toChiTietDto(savedYeuCau, actor, savedAttachments, List.of(), List.of(savedHistory));
            });
        } catch (RuntimeException | Error ex) {
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
            throw ApiException.forbidden("Chỉ sinh viên mới truy cập được danh sách phiếu hỗ trợ cá nhân");
        }
        Student sinhVien = sinhVienHienTai(actor);
        SupportRequestStatus filterTrangThai = parseOptionalTrangThai(trangThai);
        SupportRequestType filterLoai = parseOptionalLoaiYeuCau(loaiYeuCau);
        String cleanKeyword = normalizeKeyword(keyword);
        PageRequest pageable = buildPageRequest(page, size);

        return yeuCauRepository.searchBySinhVien(
                        sinhVien.getId(), filterTrangThai, filterLoai, cleanKeyword, pageable)
                .map(this::toTomTatDto);
    }

    @Transactional(readOnly = true)
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachQuanTri(String trangThai,
                                                                    String loaiYeuCau,
                                                                    String keyword,
                                                                    int page,
                                                                    int size) {
        User actor = currentUser.get();
        if (actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ quản trị viên mới có quyền tra cứu toàn bộ phiếu hỗ trợ");
        }
        SupportRequestStatus filterTrangThai = parseOptionalTrangThai(trangThai);
        SupportRequestType filterLoai = parseOptionalLoaiYeuCau(loaiYeuCau);
        String cleanKeyword = normalizeKeyword(keyword);
        PageRequest pageable = buildPageRequest(page, size);

        return yeuCauRepository.searchAll(filterTrangThai, filterLoai, cleanKeyword, pageable)
                .map(this::toTomTatDto);
    }

    @Transactional(readOnly = true)
    public SupportRequestDtos.YeuCauChiTietDto chiTiet(Long id) {
        User actor = currentUser.get();
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(id, actor);
        List<SupportRequestAttachment> tepDinhKem = tepDinhKemRepository.findByYeuCauIdOrderByIdAsc(yeuCau.getId());
        List<SupportRequestComment> traoDoi = traoDoiRepository.findByYeuCauIdOrderByCreatedAtAscIdAsc(yeuCau.getId());
        List<SupportRequestStatusHistory> lichSu = lichSuTrangThaiRepository.findByYeuCauIdOrderByThoiDiemAscIdAsc(yeuCau.getId());
        return toChiTietDto(yeuCau, actor, tepDinhKem, traoDoi, lichSu);
    }

    @Transactional
    public SupportRequestDtos.TraoDoiDto themTraoDoi(Long yeuCauId, SupportRequestDtos.ThemTraoDoiRequest request) {
        User actor = currentUser.get();
        SupportRequest yeuCau = timVaKiemTraQuyenTruyCap(yeuCauId, actor);

        if (request == null) {
            throw ApiException.badRequest("Nội dung trao đổi không hợp lệ");
        }
        String noiDung = validateRequiredText(request.noiDung(), "Nội dung trao đổi", 1, 2000);

        if (!coThePhanHoi(yeuCau)) {
            throw ApiException.badRequest("Phiếu hỗ trợ đang ở trạng thái "
                    + yeuCau.getTrangThai().getMoTa() + ", không thể gửi thêm trao đổi");
        }

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
            traoDoi.setCreatedAt(now);

            SupportRequestComment saved = traoDoiRepository.saveAndFlush(traoDoi);
            if (actor.getRole() == Role.QUAN_TRI && yeuCau.getNguoiXuLy() == null) {
                yeuCau.setNguoiXuLy(actor);
            }
            yeuCau.setUpdatedAt(now);
            yeuCauRepository.saveAndFlush(yeuCau);

            audit.changes(
                    Map.of("maPhieu", yeuCau.getMaPhieu(), "trangThai", yeuCau.getTrangThai().name()),
                    Map.of("maPhieu", yeuCau.getMaPhieu(), "trangThai", yeuCau.getTrangThai().name(), "traoDoiId", saved.getId())
            );

            return toTraoDoiDto(saved);
        });
    }

    @Transactional
    public SupportRequestDtos.YeuCauChiTietDto doiTrangThai(Long yeuCauId,
                                                            SupportRequestDtos.DoiTrangThaiRequest request) {
        User actor = currentUser.get();
        if (actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ quản trị viên mới có quyền cập nhật trạng thái phiếu hỗ trợ");
        }
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
            yeuCau.setNguoiXuLy(actor);
            yeuCau.setUpdatedAt(now);
            SupportRequest savedYeuCau = yeuCauRepository.saveAndFlush(yeuCau);

            SupportRequestStatusHistory lichSu = new SupportRequestStatusHistory();
            lichSu.setYeuCau(savedYeuCau);
            lichSu.setTrangThaiCu(trangThaiCu);
            lichSu.setTrangThaiMoi(trangThaiMoi);
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

            List<SupportRequestAttachment> tepDinhKem = tepDinhKemRepository.findByYeuCauIdOrderByIdAsc(savedYeuCau.getId());
            List<SupportRequestComment> traoDoi = traoDoiRepository.findByYeuCauIdOrderByCreatedAtAscIdAsc(savedYeuCau.getId());
            List<SupportRequestStatusHistory> lichSuList = lichSuTrangThaiRepository.findByYeuCauIdOrderByThoiDiemAscIdAsc(savedYeuCau.getId());
            return toChiTietDto(savedYeuCau, actor, tepDinhKem, traoDoi, lichSuList);
        });
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

    private SupportRequest timVaKiemTraQuyenTruyCap(Long yeuCauId, User actor) {
        if (yeuCauId == null || yeuCauId <= 0) {
            throw ApiException.badRequest("Mã định danh phiếu hỗ trợ không hợp lệ");
        }
        if (actor.getRole() != Role.SINH_VIEN && actor.getRole() != Role.QUAN_TRI) {
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
            if (file.isEmpty() && (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank())) {
                continue;
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
                traoDoi.getCreatedAt()
        );
    }

    private SupportRequestDtos.LichSuTrangThaiDto toLichSuDto(SupportRequestStatusHistory ls) {
        return new SupportRequestDtos.LichSuTrangThaiDto(
                ls.getId(),
                ls.getTrangThaiCu() != null ? ls.getTrangThaiCu().name() : null,
                ls.getTrangThaiCu() != null ? ls.getTrangThaiCu().getMoTa() : null,
                ls.getTrangThaiMoi().name(),
                ls.getTrangThaiMoi().getMoTa(),
                ls.getNguoiThucHien() != null ? ls.getNguoiThucHien().getId() : null,
                ls.getTenNguoiThucHien(),
                ls.getVaiTroNguoiThucHien().name(),
                ls.getGhiChu(),
                ls.getThoiDiem()
        );
    }

    private SupportRequestDtos.YeuCauTomTatDto toTomTatDto(SupportRequest yeuCau) {
        Student sv = yeuCau.getSinhVien();
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
                toMonHocDto(yeuCau.getMonHoc()),
                toDiemDto(yeuCau.getDiem()),
                toHocKyDto(yeuCau.getHocKy()),
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null,
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
        List<String> nextStatuses = viewer.getRole() == Role.QUAN_TRI
                ? yeuCau.getTrangThai().trangThaiTiepTheoHopLe().stream()
                        .filter(properties.allowedStatuses()::contains)
                        .map(Enum::name)
                        .toList()
                : List.of();
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
                toMonHocDto(yeuCau.getMonHoc()),
                toDiemDto(yeuCau.getDiem()),
                toHocKyDto(yeuCau.getHocKy()),
                yeuCau.getNguoiXuLy() != null ? yeuCau.getNguoiXuLy().getUsername() : null,
                yeuCau.getCreatedAt(),
                yeuCau.getUpdatedAt(),
                coThePhanHoi(yeuCau),
                nextStatuses,
                tepDinhKem.stream().map(t -> toTepDto(yeuCau.getId(), t)).toList(),
                traoDoi.stream().map(this::toTraoDoiDto).toList(),
                lichSu.stream().map(this::toLichSuDto).toList()
        );
    }
}
