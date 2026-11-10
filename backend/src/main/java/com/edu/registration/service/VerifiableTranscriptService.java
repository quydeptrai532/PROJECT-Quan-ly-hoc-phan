package com.edu.registration.service;

import com.edu.registration.config.VerifiableTranscriptConfigProperties;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.VerifiableTranscriptDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.entity.VerifiableTranscript;
import com.edu.registration.entity.VerifiableTranscriptStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.VerifiableTranscriptRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.ExportSecuritySupport;
import com.edu.registration.util.VerifiableCodeSupport;
import java.util.Locale;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dịch vụ xử lý phát hành, xác minh công khai, thu hồi và cấp lại bảng điểm điện tử xác thực
 * (QLSV-99 / TASK-OP-12).
 */
@Service
@Transactional
public class VerifiableTranscriptService {

    private static final Logger log = LoggerFactory.getLogger(VerifiableTranscriptService.class);

    private final VerifiableTranscriptRepository repository;
    private final StudentRepository studentRepository;
    private final SemesterRepository semesterRepository;
    private final StudentTranscriptPdfService studentTranscriptPdfService;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final VerifiableTranscriptConfigProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public VerifiableTranscriptService(
            VerifiableTranscriptRepository repository,
            StudentRepository studentRepository,
            SemesterRepository semesterRepository,
            StudentTranscriptPdfService studentTranscriptPdfService,
            CurrentUser currentUser,
            @Autowired(required = false) AuditLogService auditLogService,
            VerifiableTranscriptConfigProperties properties,
            ObjectMapper objectMapper) {
        this(repository, studentRepository, semesterRepository, studentTranscriptPdfService,
                currentUser, auditLogService, properties, objectMapper, Clock.systemDefaultZone());
    }

    public VerifiableTranscriptService(
            VerifiableTranscriptRepository repository,
            StudentRepository studentRepository,
            SemesterRepository semesterRepository,
            StudentTranscriptPdfService studentTranscriptPdfService,
            CurrentUser currentUser,
            AuditLogService auditLogService,
            VerifiableTranscriptConfigProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.semesterRepository = semesterRepository;
        this.studentTranscriptPdfService = studentTranscriptPdfService;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Sinh viên xuất bảng điểm điện tử có mã xác minh và QR.
     * Cấp mã ngẫu nhiên có entropy cao (160+ bits), chụp snapshot bất biến, băm lưu an toàn SHA-256.
     */
    public StudentTranscriptPdfService.PdfFile xuatBangDiemDienTuSinhVien(Long hocKyId) {
        Student sv = laySinhVienHienTai();
        RegistrationDtos.BangDiemPdfDto duLieu = studentTranscriptPdfService.layDuLieuBangDiem(hocKyId);

        // Sinh mã ngẫu nhiên có entropy cao (cryptographically strong)
        String maXacMinh = VerifiableCodeSupport.generateVerificationCode();
        String codeNormalized = VerifiableCodeSupport.normalizeCode(maXacMinh);
        String codeHash = VerifiableCodeSupport.sha256Hex(codeNormalized);
        String codeMasked = VerifiableCodeSupport.maskCode(maXacMinh);

        // Xác định số phiên bản tiếp theo
        Optional<VerifiableTranscript> latestOpt = (hocKyId != null)
                ? repository.findFirstBySinhVienIdAndHocKyIdOrderByPhienBanDesc(sv.getId(), hocKyId)
                : repository.findFirstBySinhVienIdAndHocKyIsNullOrderByPhienBanDesc(sv.getId());
        int phienBan = latestOpt.map(v -> v.getPhienBan() + 1).orElse(1);
        Long banGhiGocId = latestOpt.map(VerifiableTranscript::getId).orElse(null);

        // Nếu bản ghi trước đó đang ở trạng thái HIEU_LUC, tự động cập nhật sang DA_THAY_THE
        if (latestOpt.isPresent() && latestOpt.get().getTrangThai() == VerifiableTranscriptStatus.HIEU_LUC) {
            VerifiableTranscript cu = latestOpt.get();
            cu.setTrangThai(VerifiableTranscriptStatus.DA_THAY_THE);
            cu.setThoiDiemThuHoi(LocalDateTime.now(clock));
            cu.setLyDoThuHoi("Tự động thay thế bởi phiên bản phát hành mới hơn (v" + phienBan + ")");
            repository.save(cu);
        }

        String verificationUrl = VerifiableCodeSupport.buildVerificationUrl(properties.getVerificationBaseUrl(), maXacMinh);
        byte[] pdfBytes = studentTranscriptPdfService.taoFilePdf(duLieu, maXacMinh, verificationUrl, phienBan);
        String fileSha256 = tinhSha256Bytes(pdfBytes);

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(duLieu);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể chụp snapshot bảng điểm", e);
        }

        Semester hocKy = hocKyId != null ? semesterRepository.findById(hocKyId).orElse(null) : null;

        VerifiableTranscript entity = new VerifiableTranscript();
        entity.setMaXacMinhHash(codeHash);
        entity.setMaXacMinhMasked(codeMasked);
        entity.setMaSinhVien(sv.getMa());
        entity.setSinhVien(sv);
        entity.setHocKy(hocKy);
        entity.setTenHocKy(hocKy != null ? hocKy.getTen() : (hocKyId != null ? "Học kỳ " + hocKyId : null));
        entity.setPhienBan(phienBan);
        entity.setTrangThai(VerifiableTranscriptStatus.HIEU_LUC);
        entity.setFileSha256(fileSha256);
        entity.setSoMonDaCongBo(duLieu.tongSoMonDaCongBo());
        entity.setTongTinChiCoDiem(duLieu.tongTinChiCoDiem());
        entity.setTongTinChiTichLuy(duLieu.tinChiTichLuy());
        entity.setGpa10(duLieu.gpa10());
        entity.setGpa4(duLieu.gpa4());
        entity.setSnapshotJson(snapshotJson);
        entity.setDonViPhatHanh(duLieu.donViPhatHanh());
        entity.setThoiDiemPhatHanh(LocalDateTime.now(clock));
        entity.setNguoiPhatHanhId(currentUser.get().getId());
        entity.setNguoiPhatHanhUsername(currentUser.get().getUsername());
        entity.setNguoiPhatHanhRole(currentUser.get().getRole().name());
        entity.setBanGhiGocId(banGhiGocId);

        VerifiableTranscript saved = repository.save(entity);

        // Ghi audit log (tuyệt đối KHÔNG ghi mã token bí mật thô vào log)
        ghiAuditLog("VERIFIABLE_TRANSCRIPT_ISSUED", "VERIFIABLE_TRANSCRIPT", saved.getId(), Map.of(
                "maSinhVien", sv.getMa(),
                "phienBan", phienBan,
                "maXacMinhMasked", codeMasked,
                "fileSha256", fileSha256,
                "soMonDaCongBo", duLieu.tongSoMonDaCongBo()
        ));
        ghiAuditLog("STUDENT_TRANSCRIPT_EXPORTED", "STUDENT_TRANSCRIPT", sv.getId(), Map.of(
                "maSinhVien", sv.getMa(),
                "hocKyId", hocKyId != null ? hocKyId : "TAT_CA",
                "soMonDaCongBo", duLieu.tongSoMonDaCongBo(),
                "maXacMinhMasked", codeMasked
        ));

        String maSvAnToan = ExportSecuritySupport.sanitizeFilename(sv.getMa());
        String tenTep = (hocKyId != null)
                ? String.format(Locale.ROOT, "bang-diem-%s-hk%d.pdf", maSvAnToan, hocKyId)
                : String.format(Locale.ROOT, "bang-diem-%s.pdf", maSvAnToan);

        log.info("Phát hành bảng điểm điện tử xác thực thành công cho sinh viên {} phiên bản v{} (id={})",
                sv.getMa(), phienBan, saved.getId());

        return new StudentTranscriptPdfService.PdfFile(
                tenTep, pdfBytes, duLieu.tongSoMonDaCongBo(), duLieu, maXacMinh, verificationUrl, phienBan);
    }

    /**
     * Xác thực công khai mã bảng điểm điện tử qua Web/QR.
     * TUÂN THỦ BẢO MẬT:
     * - Chỉ trả thông tin tối thiểu đã duyệt (không công khai điểm chi tiết từng môn).
     * - Mã sai, hết hạn hoặc thu hồi trả thông báo tương ứng và TUYỆT ĐỐI KHÔNG tiết lộ thông tin cá nhân.
     */
    @Transactional(readOnly = true)
    public VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto xacThucCongKhai(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.khongHopLe(
                    "Vui lòng cung cấp mã xác minh bảng điểm.");
        }

        String normalized = VerifiableCodeSupport.normalizeCode(rawCode);
        if (normalized.length() < 8) {
            return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.khongHopLe(
                    "Định dạng mã xác minh không hợp lệ. Vui lòng kiểm tra lại.");
        }

        String codeHash = VerifiableCodeSupport.sha256Hex(normalized);
        Optional<VerifiableTranscript> opt = repository.findByMaXacMinhHash(codeHash);

        if (opt.isEmpty()) {
            return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.khongHopLe(
                    "Mã xác minh không tồn tại trong hệ thống. Vui lòng kiểm tra lại mã hoặc quét lại mã QR trên bảng điểm chính thức.");
        }

        VerifiableTranscript vt = opt.get();

        if (vt.getTrangThai() == VerifiableTranscriptStatus.DA_THU_HOI) {
            return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.daThuHoi(
                    vt.getThoiDiemThuHoi(), vt.getLyDoThuHoi(), vt.getPhienBan());
        }

        if (vt.getTrangThai() == VerifiableTranscriptStatus.DA_THAY_THE) {
            return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.daThayThe(vt.getPhienBan());
        }

        // Bảng điểm HIEU_LUC: Trả thông tin tối thiểu đã duyệt (không công khai điểm chi tiết từng môn)
        RegistrationDtos.BangDiemPdfDto snapshot;
        try {
            snapshot = objectMapper.readValue(vt.getSnapshotJson(), RegistrationDtos.BangDiemPdfDto.class);
        } catch (JsonProcessingException e) {
            log.error("Không thể đọc snapshot JSON của bảng điểm id={}", vt.getId(), e);
            throw new IllegalStateException("Lỗi cấu trúc dữ liệu bản ghi bảng điểm");
        }

        String phamVi = vt.getTenHocKy() != null ? vt.getTenHocKy() : "Toàn khóa";
        String donVi = (vt.getDonViPhatHanh() != null && !vt.getDonViPhatHanh().isBlank())
                ? vt.getDonViPhatHanh()
                : (snapshot.donViPhatHanh() != null ? snapshot.donViPhatHanh() : "Hệ thống Quản lý Đào tạo");

        return VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto.hopLe(
                vt.getMaXacMinhMasked(),
                snapshot.sinhVien().maSinhVien(),
                snapshot.sinhVien().hoTen(),
                snapshot.sinhVien().khoa(),
                snapshot.sinhVien().nganh(),
                snapshot.sinhVien().lopHanhChinh(),
                snapshot.sinhVien().khoaNhapHoc(),
                phamVi,
                vt.getPhienBan(),
                vt.getThoiDiemPhatHanh(),
                donVi,
                vt.getSoMonDaCongBo(),
                vt.getTongTinChiTichLuy(),
                vt.getGpa10(),
                vt.getGpa4(),
                vt.getFileSha256(),
                properties.getPolicyNote()
        );
    }

    /**
     * Thu hồi bảng điểm điện tử (Chỉ dành cho cán bộ QUAN_TRI).
     */
    public VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto thuHoiBangDiem(Long id, String lyDo) {
        kiemTraQuyenQuanTri();
        if (lyDo == null || lyDo.trim().isBlank()) {
            throw ApiException.badRequest("Lý do thu hồi bảng điểm là bắt buộc");
        }

        VerifiableTranscript vt = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bản ghi bảng điểm xác thực id=" + id));

        if (vt.getTrangThai() == VerifiableTranscriptStatus.DA_THU_HOI) {
            throw ApiException.badRequest("Bảng điểm này đã ở trạng thái thu hồi");
        }

        vt.setTrangThai(VerifiableTranscriptStatus.DA_THU_HOI);
        vt.setThoiDiemThuHoi(LocalDateTime.now(clock));
        vt.setNguoiThuHoiId(currentUser.get().getId());
        vt.setNguoiThuHoiUsername(currentUser.get().getUsername());
        vt.setNguoiThuHoiRole(currentUser.get().getRole().name());
        vt.setLyDoThuHoi(lyDo.trim());

        VerifiableTranscript saved = repository.save(vt);

        ghiAuditLog("VERIFIABLE_TRANSCRIPT_REVOKED", "VERIFIABLE_TRANSCRIPT", saved.getId(), Map.of(
                "maSinhVien", saved.getMaSinhVien(),
                "phienBan", saved.getPhienBan(),
                "maXacMinhMasked", saved.getMaXacMinhMasked(),
                "lyDoThuHoi", lyDo.trim()
        ));

        log.info("Cán bộ {} đã thu hồi bảng điểm điện tử id={} của SV {} (lý do: {})",
                currentUser.get().getUsername(), saved.getId(), saved.getMaSinhVien(), lyDo.trim());

        return toSummaryDto(saved);
    }

    /**
     * Cấp lại bảng điểm điện tử (Chỉ dành cho cán bộ QUAN_TRI).
     * Thu hồi bản ghi cũ (chuyển sang DA_THAY_THE) và phát hành phiên bản mới tăng tiến (phienBan + 1).
     */
    public StudentTranscriptPdfService.PdfFile capLaiBangDiem(Long id, String lyDo) {
        kiemTraQuyenQuanTri();
        if (lyDo == null || lyDo.trim().isBlank()) {
            throw ApiException.badRequest("Lý do cấp lại bảng điểm là bắt buộc");
        }

        VerifiableTranscript old = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bản ghi bảng điểm xác thực id=" + id));

        // Đọc snapshot cũ để lấy cấu trúc dữ liệu và cập nhật thời điểm phát hành mới
        RegistrationDtos.BangDiemPdfDto snapshotCu;
        try {
            snapshotCu = objectMapper.readValue(old.getSnapshotJson(), RegistrationDtos.BangDiemPdfDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể đọc snapshot bản ghi cũ", e);
        }

        // Tạo snapshot mới với thời điểm phát hành cập nhật
        RegistrationDtos.BangDiemPdfDto snapshotMoi = new RegistrationDtos.BangDiemPdfDto(
                snapshotCu.sinhVien(),
                snapshotCu.donViPhatHanh(),
                LocalDateTime.now(clock),
                snapshotCu.hocKy(),
                snapshotCu.tongSoMonDaCongBo(),
                snapshotCu.tongTinChiCoDiem(),
                snapshotCu.tinChiTichLuy(),
                snapshotCu.gpa10(),
                snapshotCu.gpa4(),
                snapshotCu.coNhieuLanHoc()
        );

        int phienBanMoi = old.getPhienBan() + 1;
        String maXacMinhMoi = VerifiableCodeSupport.generateVerificationCode();
        String codeNormalized = VerifiableCodeSupport.normalizeCode(maXacMinhMoi);
        String codeHash = VerifiableCodeSupport.sha256Hex(codeNormalized);
        String codeMasked = VerifiableCodeSupport.maskCode(maXacMinhMoi);

        String verificationUrl = VerifiableCodeSupport.buildVerificationUrl(
                properties.getVerificationBaseUrl(), maXacMinhMoi);
        byte[] pdfBytes = studentTranscriptPdfService.taoFilePdf(
                snapshotMoi, maXacMinhMoi, verificationUrl, phienBanMoi);
        String fileSha256 = tinhSha256Bytes(pdfBytes);

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshotMoi);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể chụp snapshot bảng điểm cấp lại", e);
        }

        // Cập nhật bản ghi cũ sang DA_THAY_THE
        old.setTrangThai(VerifiableTranscriptStatus.DA_THAY_THE);
        old.setThoiDiemThuHoi(LocalDateTime.now(clock));
        old.setNguoiThuHoiId(currentUser.get().getId());
        old.setNguoiThuHoiUsername(currentUser.get().getUsername());
        old.setNguoiThuHoiRole(currentUser.get().getRole().name());
        old.setLyDoThuHoi("Cấp lại phiên bản v" + phienBanMoi + ": " + lyDo.trim());

        // Tạo bản ghi mới
        VerifiableTranscript moi = new VerifiableTranscript();
        moi.setMaXacMinhHash(codeHash);
        moi.setMaXacMinhMasked(codeMasked);
        moi.setMaSinhVien(old.getMaSinhVien());
        moi.setSinhVien(old.getSinhVien());
        moi.setHocKy(old.getHocKy());
        moi.setTenHocKy(old.getTenHocKy());
        moi.setPhienBan(phienBanMoi);
        moi.setTrangThai(VerifiableTranscriptStatus.HIEU_LUC);
        moi.setFileSha256(fileSha256);
        moi.setSoMonDaCongBo(snapshotMoi.tongSoMonDaCongBo());
        moi.setTongTinChiCoDiem(snapshotMoi.tongTinChiCoDiem());
        moi.setTongTinChiTichLuy(snapshotMoi.tinChiTichLuy());
        moi.setGpa10(snapshotMoi.gpa10());
        moi.setGpa4(snapshotMoi.gpa4());
        moi.setSnapshotJson(snapshotJson);
        moi.setDonViPhatHanh(snapshotMoi.donViPhatHanh());
        moi.setThoiDiemPhatHanh(LocalDateTime.now(clock));
        moi.setNguoiPhatHanhId(currentUser.get().getId());
        moi.setNguoiPhatHanhUsername(currentUser.get().getUsername());
        moi.setNguoiPhatHanhRole(currentUser.get().getRole().name());
        moi.setBanGhiGocId(old.getId());

        VerifiableTranscript savedMoi = repository.save(moi);
        old.setBanGhiThayTheId(savedMoi.getId());
        repository.save(old);

        ghiAuditLog("VERIFIABLE_TRANSCRIPT_REISSUED", "VERIFIABLE_TRANSCRIPT", savedMoi.getId(), Map.of(
                "maSinhVien", savedMoi.getMaSinhVien(),
                "phienBanCu", old.getPhienBan(),
                "phienBanMoi", phienBanMoi,
                "banGhiGocId", old.getId(),
                "banGhiThayTheId", savedMoi.getId(),
                "fileSha256", fileSha256
        ));

        String tenTep = (old.getHocKy() != null)
                ? "bang-diem-xac-thuc-" + old.getMaSinhVien() + "-hk" + old.getHocKy().getId() + "-v" + phienBanMoi + ".pdf"
                : "bang-diem-xac-thuc-" + old.getMaSinhVien() + "-v" + phienBanMoi + ".pdf";

        log.info("Cán bộ {} đã cấp lại bảng điểm id={} sang phiên bản v{} (idMoi={}) cho SV {}",
                currentUser.get().getUsername(), old.getId(), phienBanMoi, savedMoi.getId(), old.getMaSinhVien());

        return new StudentTranscriptPdfService.PdfFile(
                tenTep, pdfBytes, snapshotMoi.tongSoMonDaCongBo(), snapshotMoi, maXacMinhMoi, verificationUrl, phienBanMoi);
    }

    /**
     * Tải tệp PDF bảng điểm xác thực đã phát hành dựa trên snapshot bất biến và mã xác minh.
     */
    @Transactional(readOnly = true)
    public StudentTranscriptPdfService.PdfFile taiPdfDaPhatHanh(Long id) {
        VerifiableTranscript vt = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bản ghi bảng điểm xác thực id=" + id));

        User user = currentUser.get();
        if (user.getRole() == Role.SINH_VIEN) {
            Student sv = laySinhVienHienTai();
            if (!Objects.equals(sv.getId(), vt.getSinhVien().getId())) {
                throw ApiException.forbidden("Bạn không có quyền tải bảng điểm của sinh viên khác");
            }
        }

        RegistrationDtos.BangDiemPdfDto snapshot;
        try {
            snapshot = objectMapper.readValue(vt.getSnapshotJson(), RegistrationDtos.BangDiemPdfDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể đọc snapshot dữ liệu của bảng điểm", e);
        }

        String verificationUrl = properties.getVerificationBaseUrl() + "/xac-thuc-bang-diem";
        byte[] pdfBytes = studentTranscriptPdfService.taoFilePdf(
                snapshot, vt.getMaXacMinhMasked(), verificationUrl, vt.getPhienBan());

        String tenTep = (vt.getHocKy() != null)
                ? "bang-diem-xac-thuc-" + vt.getMaSinhVien() + "-hk" + vt.getHocKy().getId() + "-v" + vt.getPhienBan() + ".pdf"
                : "bang-diem-xac-thuc-" + vt.getMaSinhVien() + "-v" + vt.getPhienBan() + ".pdf";

        return new StudentTranscriptPdfService.PdfFile(
                tenTep, pdfBytes, vt.getSoMonDaCongBo(), snapshot, vt.getMaXacMinhMasked(), verificationUrl, vt.getPhienBan());
    }

    /**
     * Lấy danh sách bảng điểm điện tử đã phát hành của chính sinh viên đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> danhSachCuaSinhVienHienTai() {
        Student sv = laySinhVienHienTai();
        return repository.findBySinhVienIdOrderByThoiDiemPhatHanhDesc(sv.getId())
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    /**
     * Tra cứu danh sách bảng điểm điện tử xác thực (dành cho QUAN_TRI).
     */
    @Transactional(readOnly = true)
    public Page<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> timKiemQuanTri(
            String maSinhVien, VerifiableTranscriptStatus trangThai, Long hocKyId, int page, int size) {
        kiemTraQuyenQuanTri();
        String maSvFilter = (maSinhVien != null && !maSinhVien.isBlank()) ? maSinhVien.trim() : null;
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return repository.search(maSvFilter, trangThai, hocKyId, pageRequest).map(this::toSummaryDto);
    }

    /**
     * Xem chi tiết bảng điểm xác thực kèm snapshot và lịch sử liên kết phiên bản (dành cho QUAN_TRI).
     */
    @Transactional(readOnly = true)
    public VerifiableTranscriptDtos.VerifiableTranscriptDetailDto chiTietQuanTri(Long id) {
        kiemTraQuyenQuanTri();
        VerifiableTranscript vt = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bản ghi bảng điểm xác thực id=" + id));

        RegistrationDtos.BangDiemPdfDto snapshot;
        try {
            snapshot = objectMapper.readValue(vt.getSnapshotJson(), RegistrationDtos.BangDiemPdfDto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không thể đọc snapshot dữ liệu", e);
        }

        // Lấy toàn bộ các phiên bản của cùng sinh viên để theo dõi liên kết
        List<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> lichSu = repository
                .findBySinhVienIdOrderByThoiDiemPhatHanhDesc(vt.getSinhVien().getId())
                .stream()
                .filter(item -> Objects.equals(item.getHocKy() != null ? item.getHocKy().getId() : null,
                        vt.getHocKy() != null ? vt.getHocKy().getId() : null))
                .map(this::toSummaryDto)
                .toList();

        return new VerifiableTranscriptDtos.VerifiableTranscriptDetailDto(
                vt.getId(),
                vt.getMaXacMinhMasked(),
                vt.getMaSinhVien(),
                vt.getSinhVien().getHoTen(),
                vt.getHocKy() != null ? vt.getHocKy().getId() : null,
                vt.getTenHocKy(),
                vt.getPhienBan(),
                vt.getTrangThai(),
                vt.getTrangThai().getMoTa(),
                vt.getFileSha256(),
                vt.getSoMonDaCongBo(),
                vt.getTongTinChiCoDiem(),
                vt.getTongTinChiTichLuy(),
                vt.getGpa10(),
                vt.getGpa4(),
                vt.getDonViPhatHanh(),
                vt.getThoiDiemPhatHanh(),
                vt.getNguoiPhatHanhUsername(),
                vt.getNguoiPhatHanhRole(),
                vt.getThoiDiemThuHoi(),
                vt.getNguoiThuHoiUsername(),
                vt.getNguoiThuHoiRole(),
                vt.getLyDoThuHoi(),
                vt.getBanGhiGocId(),
                vt.getBanGhiThayTheId(),
                snapshot,
                lichSu
        );
    }

    private VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto toSummaryDto(VerifiableTranscript vt) {
        return new VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto(
                vt.getId(),
                vt.getMaXacMinhMasked(),
                vt.getMaSinhVien(),
                vt.getSinhVien() != null ? vt.getSinhVien().getHoTen() : null,
                vt.getHocKy() != null ? vt.getHocKy().getId() : null,
                vt.getTenHocKy(),
                vt.getPhienBan(),
                vt.getTrangThai(),
                vt.getTrangThai().getMoTa(),
                vt.getFileSha256(),
                vt.getSoMonDaCongBo(),
                vt.getTongTinChiCoDiem(),
                vt.getTongTinChiTichLuy(),
                vt.getGpa10(),
                vt.getGpa4(),
                vt.getThoiDiemPhatHanh(),
                vt.getNguoiPhatHanhUsername(),
                vt.getThoiDiemThuHoi(),
                vt.getNguoiThuHoiUsername(),
                vt.getLyDoThuHoi(),
                vt.getBanGhiGocId(),
                vt.getBanGhiThayTheId()
        );
    }

    private Student laySinhVienHienTai() {
        User user = currentUser.get();
        return studentRepository.findWithAcademicProfileByUserId(user.getId())
                .or(() -> studentRepository.findByUser(user))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private void kiemTraQuyenQuanTri() {
        User user = currentUser.get();
        if (user.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ quản trị viên mới có quyền thực hiện thao tác này");
        }
    }

    private static String tinhSha256Bytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Không tìm thấy thuật toán SHA-256", e);
        }
    }

    private void ghiAuditLog(String action, String entityType, Long entityId, Map<String, Object> changes) {
        if (auditLogService != null) {
            try {
                auditLogService.execute(action, entityType, entityId, audit -> {
                    audit.changes(Map.of(), changes);
                    return null;
                });
            } catch (Exception ex) {
                log.warn("Không thể ghi audit log cho hành động {}: {}", action, ex.getMessage());
            }
        }
    }
}
