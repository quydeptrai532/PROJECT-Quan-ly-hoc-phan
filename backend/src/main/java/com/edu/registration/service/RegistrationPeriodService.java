package com.edu.registration.service;

import com.edu.registration.config.RegistrationProperties;
import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.entity.AcademicCohort;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.RegistrationPeriod;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.AcademicCohortRepository;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.RegistrationPeriodRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class RegistrationPeriodService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationPeriodService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final RegistrationPeriodRepository dotDangKyRepository;
    private final SemesterRepository hocKyRepository;
    private final AcademicCohortRepository khoaHocRepository;
    private final MajorRepository nganhRepository;
    private final AdministrativeClassRepository lopHanhChinhRepository;
    private final StudentRepository sinhVienRepository;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final RegistrationProperties properties;
    private final DtoMapper mapper;

    public RegistrationPeriodService(RegistrationPeriodRepository dotDangKyRepository,
                                     SemesterRepository hocKyRepository,
                                     AcademicCohortRepository khoaHocRepository,
                                     MajorRepository nganhRepository,
                                     AdministrativeClassRepository lopHanhChinhRepository,
                                     StudentRepository sinhVienRepository,
                                     CurrentUser currentUser,
                                     AuditLogService auditLogService,
                                     RegistrationProperties properties,
                                     DtoMapper mapper) {
        this.dotDangKyRepository = dotDangKyRepository;
        this.hocKyRepository = hocKyRepository;
        this.khoaHocRepository = khoaHocRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.properties = properties;
        this.mapper = mapper;
    }

    public ZoneId zoneId() {
        return properties.getZoneId();
    }

    public LocalDateTime nowInZone() {
        return LocalDateTime.now(zoneId());
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriodDtos.DotDangKyDto> danhSach(Long hocKyId, String keyword,
                                                              Boolean dangHieuLuc, String loaiDot) {
        LocalDateTime now = nowInZone();
        List<RegistrationPeriod> list;
        if (hocKyId != null || dangHieuLuc != null || (loaiDot != null && !loaiDot.isBlank())) {
            list = dotDangKyRepository.search(hocKyId, dangHieuLuc,
                    (loaiDot != null && !loaiDot.isBlank()) ? loaiDot.trim() : null);
        } else {
            list = dotDangKyRepository.findAll();
            list.sort(Comparator.comparing((RegistrationPeriod p) -> p.getHocKy().getId()).reversed()
                    .thenComparingInt(RegistrationPeriod::getThuTuUuTien)
                    .thenComparing(RegistrationPeriod::getThoiGianBatDau));
        }

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(p -> (p.getMaDot() != null && p.getMaDot().toLowerCase().contains(kw))
                            || (p.getTenDot() != null && p.getTenDot().toLowerCase().contains(kw))
                            || (p.getKhoaNhapHoc() != null && p.getKhoaNhapHoc().toLowerCase().contains(kw))
                            || (p.getNhomDoiTuong() != null && p.getNhomDoiTuong().toLowerCase().contains(kw)))
                    .toList();
        }

        return list.stream().map(dot -> mapper.dotDangKy(dot, now)).toList();
    }

    @Transactional(readOnly = true)
    public RegistrationPeriodDtos.DotDangKyDto chiTiet(Long id) {
        RegistrationPeriod dot = timDot(id);
        return mapper.dotDangKy(dot, nowInZone());
    }

    public RegistrationPeriodDtos.DotDangKyDto tao(RegistrationPeriodDtos.TaoDotDangKyRequest request) {
        return auditLogService.execute("REGISTRATION_PERIOD_CREATED", "REGISTRATION_PERIOD", null, audit -> {
            Semester hocKy = hocKyRepository.findById(request.hocKyId())
                    .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));

            kiemTraThoiGian(request.thoiGianBatDau(), request.thoiGianKetThuc());

            if (dotDangKyRepository.existsByHocKyIdAndMaDotIgnoreCase(hocKy.getId(), request.maDot().trim())) {
                throw ApiException.conflict("Mã đợt đăng ký '" + request.maDot().trim() + "' đã tồn tại trong " + hocKy.getTen());
            }

            RegistrationPeriod dot = new RegistrationPeriod();
            dot.setHocKy(hocKy);
            dot.setMaDot(request.maDot().trim());
            dot.setTenDot(request.tenDot().trim());
            dot.setLoaiDot(request.loaiDot() != null && !request.loaiDot().isBlank() ? request.loaiDot().trim() : "CHINH_THUC");
            dot.setThuTuUuTien(request.thuTuUuTien() != null ? request.thuTuUuTien() : 1);
            dot.setThoiGianBatDau(request.thoiGianBatDau());
            dot.setThoiGianKetThuc(request.thoiGianKetThuc());
            dot.setDangHieuLuc(request.dangHieuLuc() == null || request.dangHieuLuc());
            dot.setGhiChu(request.ghiChu() != null && !request.ghiChu().isBlank() ? request.ghiChu().trim() : null);

            ganDoiTuong(dot, request.khoaHocId(), request.khoaNhapHoc(), request.nganhId(),
                    request.lopHanhChinhId(), request.nhomDoiTuong());

            User actor = currentUser.get();
            LocalDateTime now = nowInZone();
            dot.setTaoLuc(now);
            dot.setCapNhatLuc(now);
            if (actor != null) {
                dot.setNguoiTaoId(actor.getId());
                dot.setNguoiTaoUsername(actor.getUsername());
                dot.setNguoiCapNhatId(actor.getId());
                dot.setNguoiCapNhatUsername(actor.getUsername());
            }

            RegistrationPeriod saved = dotDangKyRepository.save(dot);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            log.info("Tạo mới đợt đăng ký {} ({}) cho học kỳ {}", saved.getMaDot(), saved.getTenDot(), hocKy.getTen());
            return mapper.dotDangKy(saved, now);
        });
    }

    public RegistrationPeriodDtos.DotDangKyDto sua(Long id, RegistrationPeriodDtos.SuaDotDangKyRequest request) {
        if (request.lyDo() == null || request.lyDo().isBlank()) {
            throw ApiException.badRequest("Vui lòng nhập lý do điều chỉnh đợt đăng ký");
        }
        return auditLogService.execute("REGISTRATION_PERIOD_UPDATED", "REGISTRATION_PERIOD", id, audit -> {
            RegistrationPeriod dot = timDot(id);
            Map<String, Object> before = auditSnapshot(dot);

            kiemTraThoiGian(request.thoiGianBatDau(), request.thoiGianKetThuc());

            if (!dot.getMaDot().equalsIgnoreCase(request.maDot().trim())
                    && dotDangKyRepository.existsByHocKyIdAndMaDotIgnoreCase(dot.getHocKy().getId(), request.maDot().trim())) {
                throw ApiException.conflict("Mã đợt đăng ký '" + request.maDot().trim() + "' đã tồn tại trong " + dot.getHocKy().getTen());
            }

            dot.setMaDot(request.maDot().trim());
            dot.setTenDot(request.tenDot().trim());
            if (request.loaiDot() != null && !request.loaiDot().isBlank()) {
                dot.setLoaiDot(request.loaiDot().trim());
            }
            if (request.thuTuUuTien() != null) {
                dot.setThuTuUuTien(request.thuTuUuTien());
            }
            dot.setThoiGianBatDau(request.thoiGianBatDau());
            dot.setThoiGianKetThuc(request.thoiGianKetThuc());
            if (request.dangHieuLuc() != null) {
                dot.setDangHieuLuc(request.dangHieuLuc());
            }
            dot.setLyDoDieuChinh(request.lyDo().trim());
            if (request.ghiChu() != null) {
                dot.setGhiChu(request.ghiChu().trim());
            }

            ganDoiTuong(dot, request.khoaHocId(), request.khoaNhapHoc(), request.nganhId(),
                    request.lopHanhChinhId(), request.nhomDoiTuong());

            User actor = currentUser.get();
            LocalDateTime now = nowInZone();
            dot.setCapNhatLuc(now);
            if (actor != null) {
                dot.setNguoiCapNhatId(actor.getId());
                dot.setNguoiCapNhatUsername(actor.getUsername());
            }

            RegistrationPeriod updated = dotDangKyRepository.save(dot);
            audit.changes(before, auditSnapshot(updated));
            log.info("Cập nhật đợt đăng ký id={} ({}) bởi user={}. Lý do: {}",
                    updated.getId(), updated.getMaDot(), dot.getNguoiCapNhatUsername(), request.lyDo());
            return mapper.dotDangKy(updated, now);
        });
    }

    public RegistrationPeriodDtos.DotDangKyDto giaHan(Long id, RegistrationPeriodDtos.GiaHanDotRequest request) {
        if (request.lyDo() == null || request.lyDo().isBlank()) {
            throw ApiException.badRequest("Vui lòng nhập lý do gia hạn đợt đăng ký");
        }
        if (request.thoiGianKetThucMoi() == null) {
            throw ApiException.badRequest("Vui lòng chọn thời gian kết thúc mới");
        }
        return auditLogService.execute("REGISTRATION_PERIOD_EXTENDED", "REGISTRATION_PERIOD", id, audit -> {
            RegistrationPeriod dot = timDot(id);
            if (!request.thoiGianKetThucMoi().isAfter(dot.getThoiGianKetThuc())) {
                throw ApiException.badRequest("Thời gian gia hạn mới (" + request.thoiGianKetThucMoi().format(FORMATTER)
                        + ") phải sau thời gian kết thúc hiện tại (" + dot.getThoiGianKetThuc().format(FORMATTER) + ")");
            }

            Map<String, Object> before = auditSnapshot(dot);

            if (dot.getThoiGianKetThucGoc() == null) {
                dot.setThoiGianKetThucGoc(dot.getThoiGianKetThuc());
            }
            dot.setThoiGianKetThuc(request.thoiGianKetThucMoi());
            dot.setLyDoDieuChinh("GIA_HAN: " + request.lyDo().trim());
            dot.setDongSom(false); // Mở lại nếu từng bị đóng sớm
            dot.setDangHieuLuc(true);

            User actor = currentUser.get();
            LocalDateTime now = nowInZone();
            dot.setCapNhatLuc(now);
            if (actor != null) {
                dot.setNguoiCapNhatId(actor.getId());
                dot.setNguoiCapNhatUsername(actor.getUsername());
            }

            RegistrationPeriod updated = dotDangKyRepository.save(dot);
            audit.changes(before, auditSnapshot(updated));
            log.info("Gia hạn đợt đăng ký id={} đến {}. Lý do: {}", updated.getId(), request.thoiGianKetThucMoi(), request.lyDo());
            return mapper.dotDangKy(updated, now);
        });
    }

    public RegistrationPeriodDtos.DotDangKyDto dongSom(Long id, RegistrationPeriodDtos.DongSomDotRequest request) {
        if (request.lyDo() == null || request.lyDo().isBlank()) {
            throw ApiException.badRequest("Vui lòng nhập lý do đóng sớm đợt đăng ký");
        }
        return auditLogService.execute("REGISTRATION_PERIOD_CLOSED_EARLY", "REGISTRATION_PERIOD", id, audit -> {
            RegistrationPeriod dot = timDot(id);
            if (dot.isDongSom()) {
                throw ApiException.badRequest("Đợt đăng ký này đã được đóng sớm trước đó");
            }

            Map<String, Object> before = auditSnapshot(dot);
            LocalDateTime now = nowInZone();

            dot.setDongSom(true);
            dot.setThoiDiemDongSom(now);
            dot.setLyDoDongSom(request.lyDo().trim());
            dot.setLyDoDieuChinh("DONG_SOM: " + request.lyDo().trim());

            User actor = currentUser.get();
            dot.setCapNhatLuc(now);
            if (actor != null) {
                dot.setNguoiCapNhatId(actor.getId());
                dot.setNguoiCapNhatUsername(actor.getUsername());
            }

            RegistrationPeriod updated = dotDangKyRepository.save(dot);
            audit.changes(before, auditSnapshot(updated));
            log.info("Đóng sớm đợt đăng ký id={} lúc {}. Lý do: {}", updated.getId(), now, request.lyDo());
            return mapper.dotDangKy(updated, now);
        });
    }

    public RegistrationPeriodDtos.DotDangKyDto moBoSung(RegistrationPeriodDtos.MoBoSungDotRequest request) {
        if (request.lyDo() == null || request.lyDo().isBlank()) {
            throw ApiException.badRequest("Vui lòng nhập lý do mở bổ sung đợt đăng ký");
        }
        return auditLogService.execute("REGISTRATION_PERIOD_SUPPLEMENTAL_OPENED", "REGISTRATION_PERIOD", null, audit -> {
            Semester hocKy = hocKyRepository.findById(request.hocKyId())
                    .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));

            kiemTraThoiGian(request.thoiGianBatDau(), request.thoiGianKetThuc());

            if (dotDangKyRepository.existsByHocKyIdAndMaDotIgnoreCase(hocKy.getId(), request.maDot().trim())) {
                throw ApiException.conflict("Mã đợt đăng ký '" + request.maDot().trim() + "' đã tồn tại trong " + hocKy.getTen());
            }

            RegistrationPeriod dot = new RegistrationPeriod();
            dot.setHocKy(hocKy);
            dot.setMaDot(request.maDot().trim());
            dot.setTenDot(request.tenDot().trim());
            dot.setLoaiDot("BO_SUNG");
            dot.setThuTuUuTien(request.thuTuUuTien() != null ? request.thuTuUuTien() : 2);
            dot.setThoiGianBatDau(request.thoiGianBatDau());
            dot.setThoiGianKetThuc(request.thoiGianKetThuc());
            dot.setDangHieuLuc(true);
            dot.setLyDoDieuChinh("MO_BO_SUNG: " + request.lyDo().trim());
            dot.setGhiChu(request.ghiChu() != null && !request.ghiChu().isBlank() ? request.ghiChu().trim() : null);

            ganDoiTuong(dot, request.khoaHocId(), request.khoaNhapHoc(), request.nganhId(),
                    request.lopHanhChinhId(), request.nhomDoiTuong());

            User actor = currentUser.get();
            LocalDateTime now = nowInZone();
            dot.setTaoLuc(now);
            dot.setCapNhatLuc(now);
            if (actor != null) {
                dot.setNguoiTaoId(actor.getId());
                dot.setNguoiTaoUsername(actor.getUsername());
                dot.setNguoiCapNhatId(actor.getId());
                dot.setNguoiCapNhatUsername(actor.getUsername());
            }

            RegistrationPeriod saved = dotDangKyRepository.save(dot);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            log.info("Mở đợt bổ sung {} cho {}. Lý do: {}", saved.getMaDot(), hocKy.getTen(), request.lyDo());
            return mapper.dotDangKy(saved, now);
        });
    }

    public void xoa(Long id) {
        auditLogService.execute("REGISTRATION_PERIOD_DELETED", "REGISTRATION_PERIOD", id, audit -> {
            RegistrationPeriod dot = timDot(id);
            audit.changes(auditSnapshot(dot), Map.of("trangThai", "DELETED"));
            dotDangKyRepository.delete(dot);
            log.info("Xóa đợt đăng ký id={} ({})", id, dot.getMaDot());
            return null;
        });
    }

    /**
     * Kiểm tra quyền đăng ký của sinh viên tại thời điểm `at`.
     * Nếu học kỳ có cấu hình đợt riêng:
     * - Sinh viên phải thỏa mãn ít nhất 1 đợt phù hợp.
     * - Đợt phù hợp phải đang mở (`dangMo(at)`: trong khoảng [thoiGianBatDau, thoiGianKetThuc] và chưa đóng sớm).
     * Nếu học kỳ không có cấu hình đợt riêng:
     * - Tái sử dụng điều kiện mặc định `hocKy.dangMoDangKy()`.
     */
    @Transactional(readOnly = true)
    public void kiemTraQuyenDangKy(Student sinhVien, Semester hocKy, LocalDateTime at) {
        if (sinhVien == null) {
            throw ApiException.badRequest("Không tìm thấy thông tin sinh viên để kiểm tra điều kiện đăng ký");
        }
        if (hocKy == null) {
            throw ApiException.badRequest("Học kỳ không hợp lệ");
        }
        LocalDateTime thoiDiem = (at != null) ? at : nowInZone();

        List<RegistrationPeriod> danhSachDot = dotDangKyRepository.findByHocKyIdAndDangHieuLucTrueOrderByThuTuUuTienAscThoiGianBatDauAsc(hocKy.getId());
        if (danhSachDot.isEmpty()) {
            // Tương thích ngược: kiểm tra theo cấu hình ngày của học kỳ
            if (!hocKy.dangMoDangKy()) {
                throw ApiException.badRequest("Hiện không trong thời gian đăng ký của " + hocKy.getTen()
                        + " (Thời gian mở: " + hocKy.getDangKyTu() + " đến " + hocKy.getDangKyDen() + ")");
            }
            return;
        }

        String cohort = xacDinhKhoaSinhVien(sinhVien);
        List<RegistrationPeriod> dotPhuHop = danhSachDot.stream()
                .filter(p -> p.phuHopVoi(sinhVien, cohort))
                .toList();

        if (dotPhuHop.isEmpty()) {
            String moTaSv = moTaSinhVien(sinhVien, cohort);
            throw ApiException.badRequest("Tài khoản của bạn không thuộc đối tượng được đăng ký trong " + hocKy.getTen()
                    + " (" + moTaSv + ")");
        }

        // Kiểm tra xem có đợt nào đang mở không
        boolean coDotDangMo = dotPhuHop.stream().anyMatch(p -> p.dangMo(thoiDiem));
        if (coDotDangMo) {
            return;
        }

        // Nếu không có đợt nào đang mở, tìm lý do chi tiết nhất để báo cho sinh viên
        RegistrationPeriod dotGanNhat = dotPhuHop.get(0);
        if (dotGanNhat.isDongSom()) {
            throw ApiException.badRequest("Đợt đăng ký '" + dotGanNhat.getTenDot()
                    + "' dành cho bạn đã đóng sớm. Lý do: "
                    + (dotGanNhat.getLyDoDongSom() != null ? dotGanNhat.getLyDoDongSom() : "Theo quyết định của Nhà trường"));
        }
        if (thoiDiem.isBefore(dotGanNhat.getThoiGianBatDau())) {
            throw ApiException.badRequest("Chưa đến thời gian đăng ký dành cho bạn trong " + hocKy.getTen()
                    + ". Đợt '" + dotGanNhat.getTenDot() + "' sẽ mở lúc: "
                    + dotGanNhat.getThoiGianBatDau().format(FORMATTER) + " (" + properties.getTimezone() + ")");
        }
        if (thoiDiem.isAfter(dotGanNhat.getThoiGianKetThuc())) {
            throw ApiException.badRequest("Đã hết thời gian đăng ký dành cho bạn trong " + hocKy.getTen()
                    + ". Đợt '" + dotGanNhat.getTenDot() + "' đã kết thúc lúc: "
                    + dotGanNhat.getThoiGianKetThuc().format(FORMATTER) + " (" + properties.getTimezone() + ")");
        }

        throw ApiException.badRequest("Hiện không trong thời gian đăng ký dành cho bạn trong " + hocKy.getTen());
    }

    /**
     * Kiểm tra quyền hủy đăng ký của sinh viên.
     */
    @Transactional(readOnly = true)
    public void kiemTraQuyenHuyDangKy(Student sinhVien, Semester hocKy, LocalDateTime at) {
        LocalDateTime thoiDiem = (at != null) ? at : nowInZone();
        List<RegistrationPeriod> danhSachDot = dotDangKyRepository.findByHocKyIdAndDangHieuLucTrueOrderByThuTuUuTienAscThoiGianBatDauAsc(hocKy.getId());
        if (danhSachDot.isEmpty()) {
            if (!hocKy.dangMoDangKy()) {
                throw ApiException.badRequest("Đã hết thời gian hủy đăng ký của " + hocKy.getTen());
            }
            return;
        }

        String cohort = xacDinhKhoaSinhVien(sinhVien);
        List<RegistrationPeriod> dotPhuHop = danhSachDot.stream()
                .filter(p -> p.phuHopVoi(sinhVien, cohort))
                .toList();

        if (dotPhuHop.isEmpty()) {
            throw ApiException.badRequest("Tài khoản của bạn không thuộc đợt đăng ký của " + hocKy.getTen() + " để thực hiện hủy");
        }

        boolean coDotDangMo = dotPhuHop.stream().anyMatch(p -> p.dangMo(thoiDiem));
        if (!coDotDangMo) {
            throw ApiException.badRequest("Hiện không trong thời gian cho phép hủy đăng ký của " + hocKy.getTen());
        }
    }

    /**
     * Sinh viên tra cứu trạng thái đợt đăng ký áp dụng cho tài khoản của mình.
     */
    @Transactional(readOnly = true)
    public RegistrationPeriodDtos.TrangThaiDangKySinhVienDto layTrangThaiDangKySinhVien(Long hocKyId) {
        User user = currentUser.get();
        Student sv = sinhVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        Semester hocKy;
        if (hocKyId != null) {
            hocKy = hocKyRepository.findById(hocKyId)
                    .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
        } else {
            hocKy = hocKyHienTai();
            if (hocKy == null) {
                throw ApiException.notFound("Chưa có học kỳ nào trong hệ thống");
            }
        }

        LocalDateTime now = nowInZone();
        String cohort = xacDinhKhoaSinhVien(sv);
        String moTaSv = moTaSinhVien(sv, cohort);

        List<RegistrationPeriod> allPeriods = dotDangKyRepository.findByHocKyIdOrderByThuTuUuTienAscThoiGianBatDauAsc(hocKy.getId());
        if (allPeriods.isEmpty()) {
            // Không có đợt riêng -> áp dụng đợt chung của học kỳ
            boolean dangMo = hocKy.dangMoDangKy();
            LocalDateTime batDau = hocKy.getDangKyTu().atStartOfDay();
            LocalDateTime ketThuc = hocKy.getDangKyDen().atTime(23, 59, 59);
            String trangThai = dangMo ? "DANG_MO" : (now.isBefore(batDau) ? "SAP_MO" : "DA_DONG");
            String thongDiep = dangMo
                    ? ("Học kỳ " + hocKy.getTen() + " đang mở đăng ký chung đến hết ngày " + hocKy.getDangKyDen())
                    : ("Học kỳ " + hocKy.getTen() + " hiện không trong thời gian mở đăng ký");

            return new RegistrationPeriodDtos.TrangThaiDangKySinhVienDto(
                    hocKy.getId(),
                    hocKy.getTen(),
                    hocKy.getNamHoc(),
                    properties.getTimezone() + " (UTC+7)",
                    now,
                    false,
                    true,
                    dangMo,
                    "MAC_DINH",
                    "Đợt đăng ký chung " + hocKy.getTen(),
                    "CHINH_THUC",
                    1,
                    batDau,
                    ketThuc,
                    trangThai,
                    thongDiep,
                    List.of(),
                    true,
                    "Áp dụng khoảng thời gian đăng ký chung của học kỳ (toàn trường).",
                    moTaSv
            );
        }

        List<RegistrationPeriod> matchingPeriods = allPeriods.stream()
                .filter(p -> p.phuHopVoi(sv, cohort))
                .toList();

        if (matchingPeriods.isEmpty()) {
            return new RegistrationPeriodDtos.TrangThaiDangKySinhVienDto(
                    hocKy.getId(),
                    hocKy.getTen(),
                    hocKy.getNamHoc(),
                    properties.getTimezone() + " (UTC+7)",
                    now,
                    true,
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "KHONG_THUOC_DOI_TUONG",
                    "Tài khoản của bạn không thuộc nhóm đối tượng được đăng ký trong " + hocKy.getTen() + " (" + moTaSv + ")",
                    List.of(),
                    properties.isOfficialRegulation(),
                    properties.getPolicyNote(),
                    moTaSv
            );
        }

        // Tìm đợt đang mở có ưu tiên cao nhất, hoặc đợt sắp mở sớm nhất
        RegistrationPeriod activePeriod = matchingPeriods.stream()
                .filter(p -> p.dangMo(now))
                .min(Comparator.comparingInt(RegistrationPeriod::getThuTuUuTien)
                        .thenComparing(RegistrationPeriod::getThoiGianBatDau))
                .orElse(null);

        RegistrationPeriod chosenPeriod = activePeriod != null ? activePeriod : matchingPeriods.get(0);
        boolean dangMoChoSinhVien = activePeriod != null;

        String trangThai;
        String thongDiep;
        if (chosenPeriod.isDongSom()) {
            trangThai = "DONG_SOM";
            thongDiep = "Đợt đăng ký '" + chosenPeriod.getTenDot() + "' đã đóng sớm. Lý do: "
                    + (chosenPeriod.getLyDoDongSom() != null ? chosenPeriod.getLyDoDongSom() : "Theo kế hoạch đào tạo");
        } else if (dangMoChoSinhVien) {
            trangThai = "DANG_MO";
            thongDiep = "Bạn đang trong thời gian mở đăng ký của đợt '" + chosenPeriod.getTenDot()
                    + "' (kết thúc lúc " + chosenPeriod.getThoiGianKetThuc().format(FORMATTER) + ")";
        } else if (now.isBefore(chosenPeriod.getThoiGianBatDau())) {
            trangThai = "SAP_MO";
            thongDiep = "Đợt đăng ký '" + chosenPeriod.getTenDot() + "' sẽ mở lúc "
                    + chosenPeriod.getThoiGianBatDau().format(FORMATTER) + " (" + properties.getTimezone() + ")";
        } else {
            trangThai = "DA_DONG";
            thongDiep = "Đợt đăng ký '" + chosenPeriod.getTenDot() + "' đã kết thúc lúc "
                    + chosenPeriod.getThoiGianKetThuc().format(FORMATTER);
        }

        List<RegistrationPeriodDtos.DotDangKyDto> dtos = matchingPeriods.stream()
                .map(p -> mapper.dotDangKy(p, now))
                .toList();

        return new RegistrationPeriodDtos.TrangThaiDangKySinhVienDto(
                hocKy.getId(),
                hocKy.getTen(),
                hocKy.getNamHoc(),
                properties.getTimezone() + " (UTC+7)",
                now,
                true,
                true,
                dangMoChoSinhVien,
                chosenPeriod.getMaDot(),
                chosenPeriod.getTenDot(),
                chosenPeriod.getLoaiDot(),
                chosenPeriod.getThuTuUuTien(),
                chosenPeriod.getThoiGianBatDau(),
                chosenPeriod.getThoiGianKetThuc(),
                trangThai,
                thongDiep,
                dtos,
                properties.isOfficialRegulation(),
                properties.getPolicyNote(),
                moTaSv
        );
    }

    public String xacDinhKhoaSinhVien(Student sv) {
        if (sv == null) {
            return null;
        }
        if (sv.getChuongTrinh() != null && sv.getChuongTrinh().getKhoaNhapHoc() != null
                && !sv.getChuongTrinh().getKhoaNhapHoc().isBlank()) {
            return sv.getChuongTrinh().getKhoaNhapHoc().trim();
        }
        return null;
    }

    private String moTaSinhVien(Student sv, String cohort) {
        StringBuilder sb = new StringBuilder();
        sb.append("Mã: ").append(sv.getMa());
        if (cohort != null) {
            sb.append(", Khóa: ").append(cohort);
        }
        if (sv.getNganh() != null) {
            sb.append(", Ngành: ").append(sv.getNganh().getMa());
        }
        if (sv.getLopHanhChinh() != null) {
            sb.append(", Lớp: ").append(sv.getLopHanhChinh().getMa());
        }
        return sb.toString();
    }

    private Semester hocKyHienTai() {
        return hocKyRepository.findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(
                nowInZone().toLocalDate(), nowInZone().toLocalDate())
                .or(() -> hocKyRepository.findFirstByOrderByDangKyDenDesc())
                .orElse(null);
    }

    private RegistrationPeriod timDot(Long id) {
        return dotDangKyRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Đợt đăng ký không tồn tại (id=" + id + ")"));
    }

    private void kiemTraThoiGian(LocalDateTime batDau, LocalDateTime ketThuc) {
        if (batDau == null || ketThuc == null) {
            throw ApiException.badRequest("Thời gian bắt đầu và kết thúc không được để trống");
        }
        if (!batDau.isBefore(ketThuc)) {
            throw ApiException.badRequest("Thời gian bắt đầu (" + batDau.format(FORMATTER)
                    + ") phải trước thời gian kết thúc (" + ketThuc.format(FORMATTER) + ")");
        }
    }

    private void ganDoiTuong(RegistrationPeriod dot, Long khoaHocId, String khoaNhapHoc,
                             Long nganhId, Long lopHanhChinhId, String nhomDoiTuong) {
        if (khoaHocId != null) {
            AcademicCohort cohort = khoaHocRepository.findById(khoaHocId)
                    .orElseThrow(() -> ApiException.notFound("Khóa học/niên khóa không tồn tại (id=" + khoaHocId + ")"));
            dot.setKhoaHoc(cohort);
            dot.setKhoaNhapHoc(cohort.getMa());
        } else if (khoaNhapHoc != null && !khoaNhapHoc.isBlank()) {
            dot.setKhoaNhapHoc(khoaNhapHoc.trim());
            khoaHocRepository.findByMaIgnoreCase(khoaNhapHoc.trim()).ifPresent(dot::setKhoaHoc);
        } else {
            dot.setKhoaHoc(null);
            dot.setKhoaNhapHoc(null);
        }

        if (nganhId != null) {
            Major nganh = nganhRepository.findById(nganhId)
                    .orElseThrow(() -> ApiException.notFound("Ngành đào tạo không tồn tại (id=" + nganhId + ")"));
            dot.setNganh(nganh);
        } else {
            dot.setNganh(null);
        }

        if (lopHanhChinhId != null) {
            AdministrativeClass lop = lopHanhChinhRepository.findById(lopHanhChinhId)
                    .orElseThrow(() -> ApiException.notFound("Lớp hành chính không tồn tại (id=" + lopHanhChinhId + ")"));
            dot.setLopHanhChinh(lop);
        } else {
            dot.setLopHanhChinh(null);
        }

        dot.setNhomDoiTuong(nhomDoiTuong != null && !nhomDoiTuong.isBlank() ? nhomDoiTuong.trim() : null);
    }

    private Map<String, Object> auditSnapshot(RegistrationPeriod p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("dotDangKyId", p.getId());
        map.put("hocKyId", p.getHocKy() != null ? p.getHocKy().getId() : null);
        map.put("maDot", p.getMaDot());
        map.put("tenDot", p.getTenDot());
        map.put("loaiDot", p.getLoaiDot());
        map.put("khoaHocId", p.getKhoaHoc() != null ? p.getKhoaHoc().getId() : null);
        map.put("khoaNhapHoc", p.getKhoaNhapHoc());
        map.put("nganhId", p.getNganh() != null ? p.getNganh().getId() : null);
        map.put("lopHanhChinhId", p.getLopHanhChinh() != null ? p.getLopHanhChinh().getId() : null);
        map.put("nhomDoiTuong", p.getNhomDoiTuong());
        map.put("thuTuUuTien", p.getThuTuUuTien());
        map.put("thoiGianBatDau", p.getThoiGianBatDau() != null ? p.getThoiGianBatDau().toString() : null);
        map.put("thoiGianKetThuc", p.getThoiGianKetThuc() != null ? p.getThoiGianKetThuc().toString() : null);
        map.put("dangHieuLuc", p.isDangHieuLuc());
        map.put("dongSom", p.isDongSom());
        map.put("thoiDiemDongSom", p.getThoiDiemDongSom() != null ? p.getThoiDiemDongSom().toString() : null);
        map.put("lyDoDongSom", p.getLyDoDongSom());
        map.put("thoiGianKetThucGoc", p.getThoiGianKetThucGoc() != null ? p.getThoiGianKetThucGoc().toString() : null);
        map.put("lyDoDieuChinh", p.getLyDoDieuChinh());
        return map;
    }
}
