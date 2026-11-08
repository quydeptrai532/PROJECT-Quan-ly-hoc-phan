package com.edu.registration.service;

import com.edu.registration.dto.LecturerDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.CourseClassAnnouncement;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.CourseClassAnnouncementRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CourseClassAnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(CourseClassAnnouncementService.class);
    public static final String AUDIT_ACTION = "CLASS_ANNOUNCEMENT_CREATED";
    public static final String AUDIT_ENTITY = "COURSE_SECTION";

    private final CourseClassAnnouncementRepository thongBaoLopHocPhanRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final LecturerRepository giangVienRepository;
    private final StudentRepository sinhVienRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final CurrentUser currentUser;
    private final NotificationService thongBaoService;
    private final AuditLogService auditLogService;

    public CourseClassAnnouncementService(CourseClassAnnouncementRepository thongBaoLopHocPhanRepository,
                                     CourseClassRepository lopHocPhanRepository,
                                     LecturerRepository giangVienRepository,
                                     StudentRepository sinhVienRepository,
                                     CourseRegistrationRepository dangKyHocPhanRepository,
                                     CurrentUser currentUser,
                                     NotificationService thongBaoService,
                                     AuditLogService auditLogService) {
        this.thongBaoLopHocPhanRepository = thongBaoLopHocPhanRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.currentUser = currentUser;
        this.thongBaoService = thongBaoService;
        this.auditLogService = auditLogService;
    }

    /**
     * Lecturer sends an announcement to the actively enrolled students of their assigned class (QLSV-68).
     * Idempotency is preserved based on idempotencyKey.
     * Only immediate dispatch is supported (no scheduler or edit/delete policy exists in project).
     */
    public LecturerDtos.ThongBaoLopDto taoThongBao(Long lopHocPhanId, LecturerDtos.TaoThongBaoLopRequest request, String headerKey) {
        Lecturer giangVien = giangVienHienTai();
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không có quyền gửi thông báo cho lớp học phần này");
        }

        String tieuDe = request.tieuDe() != null ? request.tieuDe().trim() : "";
        if (tieuDe.isBlank()) {
            throw ApiException.badRequest("Tiêu đề thông báo không được để trống");
        }
        if (tieuDe.length() > 120) {
            throw ApiException.badRequest("Tiêu đề không được vượt quá 120 ký tự");
        }

        String noiDung = request.noiDung() != null ? request.noiDung().trim() : "";
        if (noiDung.isBlank()) {
            throw ApiException.badRequest("Nội dung thông báo không được để trống");
        }
        if (noiDung.length() > 1000) {
            throw ApiException.badRequest("Nội dung không được vượt quá 1000 ký tự");
        }

        String rawKey = (request.idempotencyKey() != null && !request.idempotencyKey().isBlank())
                ? request.idempotencyKey().trim()
                : (headerKey != null && !headerKey.isBlank() ? headerKey.trim() : null);
        String idempotencyKey = (rawKey != null) ? rawKey : UUID.randomUUID().toString();

        Optional<CourseClassAnnouncement> existing = thongBaoLopHocPhanRepository
                .findByLopHocPhanIdAndIdempotencyKey(lop.getId(), idempotencyKey);
        if (existing.isPresent()) {
            return mapToDto(existing.get(), "DA_GUI");
        }

        // Determine actively enrolled students (DA_DANG_KY and active user account)
        List<CourseRegistration> activeRegistrations = dangKyHocPhanRepository
                .findByLopHocPhan(lop.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, User> recipientsById = new LinkedHashMap<>();
        for (CourseRegistration reg : activeRegistrations) {
            if (reg != null && reg.getTrangThai() == RegistrationStatus.DA_DANG_KY && reg.getSinhVien() != null) {
                User u = reg.getSinhVien().getUser();
                if (u != null && u.isTrangThai() && u.getId() != null) {
                    recipientsById.putIfAbsent(u.getId(), u);
                }
            }
        }
        List<User> recipients = List.copyOf(recipientsById.values());
        int soNguoiNhan = recipients.size();

        try {
            return auditLogService.execute(AUDIT_ACTION, AUDIT_ENTITY, lop.getId(), audit -> {
                CourseClassAnnouncement entity = new CourseClassAnnouncement();
                entity.setLopHocPhan(lop);
                entity.setGiangVien(giangVien);
                entity.setTieuDe(tieuDe);
                entity.setNoiDung(noiDung);
                entity.setIdempotencyKey(idempotencyKey);
                entity.setSoNguoiNhan(soNguoiNhan);
                entity.setThoiGianTao(LocalDateTime.now());
                CourseClassAnnouncement saved = thongBaoLopHocPhanRepository.save(entity);

                audit.changes(Map.of(), Map.of(
                        "lopHocPhanId", lop.getId(),
                        "maLop", lop.getMa(),
                        "soNguoiNhan", soNguoiNhan,
                        "thongBaoLopId", saved.getId(),
                        "tieuDe", tieuDe
                ));

                if (thongBaoService != null) {
                    thongBaoService.classAnnouncementCreated(saved, recipients);
                }
                return mapToDto(saved, "DA_GUI");
            });
        } catch (DataIntegrityViolationException dive) {
            return thongBaoLopHocPhanRepository
                    .findByLopHocPhanIdAndIdempotencyKey(lop.getId(), idempotencyKey)
                    .map(tb -> mapToDto(tb, "DA_GUI"))
                    .orElseThrow(() -> dive);
        }
    }

    @Transactional(readOnly = true)
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoLop(Long lopHocPhanId) {
        Lecturer giangVien = giangVienHienTai();
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không có quyền truy cập thông báo của lớp học phần này");
        }
        return thongBaoLopHocPhanRepository
                .findByLopHocPhanIdOrderByThoiGianTaoDesc(lopHocPhanId)
                .stream()
                .map(tb -> mapToDto(tb, "DA_GUI"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoCuaGiangVien(Long lopHocPhanId) {
        Lecturer giangVien = giangVienHienTai();
        if (lopHocPhanId != null) {
            return danhSachThongBaoLop(lopHocPhanId);
        }
        return thongBaoLopHocPhanRepository
                .findByGiangVienIdOrderByThoiGianTaoDesc(giangVien.getId())
                .stream()
                .map(tb -> mapToDto(tb, "DA_GUI"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LecturerDtos.ThongBaoLopDto> danhSachThongBaoChoSinhVien(Long lopHocPhanId) {
        Student sinhVien = sinhVienHienTai();
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        // Verify student has active registration in this class
        boolean registered = dangKyHocPhanRepository
                .findBySinhVienIdAndLopHocPhanId(sinhVien.getId(), lop.getId())
                .filter(reg -> reg.getTrangThai() == RegistrationStatus.DA_DANG_KY)
                .isPresent();
        if (!registered) {
            throw ApiException.forbidden("Bạn không đăng ký lớp học phần này");
        }

        return thongBaoLopHocPhanRepository
                .findByLopHocPhanIdOrderByThoiGianTaoDesc(lopHocPhanId)
                .stream()
                .map(tb -> mapToDto(tb, "DA_GUI"))
                .toList();
    }

    private LecturerDtos.ThongBaoLopDto mapToDto(CourseClassAnnouncement entity, String trangThaiGui) {
        CourseClass lop = entity.getLopHocPhan();
        String tenMon = (lop != null && lop.getMonHoc() != null) ? lop.getMonHoc().getTen() : "";
        String maLop = lop != null ? lop.getMa() : "";
        String giangVienTen = entity.getGiangVien() != null ? entity.getGiangVien().getHoTen() : "";
        return new LecturerDtos.ThongBaoLopDto(
                entity.getId(),
                lop != null ? lop.getId() : null,
                maLop,
                tenMon,
                entity.getTieuDe(),
                entity.getNoiDung(),
                giangVienTen,
                entity.getThoiGianTao(),
                entity.getSoNguoiNhan(),
                trangThaiGui
        );
    }

    private Lecturer giangVienHienTai() {
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    private Student sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }
}
