package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final RegistrationHistoryRepository lichSuDangKyRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final GradeRepository diemRepository;
    private final PrerequisiteSubjectRepository monTienQuyetRepository;
    private final StudentRepository sinhVienRepository;
    private final CurrentUser currentUser;
    private final SemesterService hocKyService;
    private final DtoMapper mapper;
    private final AuditLogService auditLogService;
    private final NotificationService thongBaoService;

    public RegistrationService(CourseRegistrationRepository dangKyHocPhanRepository,
                         RegistrationHistoryRepository lichSuDangKyRepository,
                         CourseClassRepository lopHocPhanRepository, GradeRepository diemRepository,
                         PrerequisiteSubjectRepository monTienQuyetRepository, StudentRepository sinhVienRepository,
                         CurrentUser currentUser, SemesterService hocKyService, DtoMapper mapper,
                         AuditLogService auditLogService, NotificationService thongBaoService) {
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.lichSuDangKyRepository = lichSuDangKyRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.diemRepository = diemRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.currentUser = currentUser;
        this.hocKyService = hocKyService;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.thongBaoService = thongBaoService;
    }

    // Serialize one student's cross-section checks; READ_COMMITTED refreshes MySQL reads after lock waits.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RegistrationDtos.DangKyDto dangKy(Long lopHocPhanId, String loaiDangKy) {
        Student sinhVien = sinhVienHienTaiForUpdate();
        return dangKyTrongGiaoDich(sinhVien, lopHocPhanId, loaiDangKy);
    }

    /** Backward-compatible internal entry point for existing callers. */
    public RegistrationDtos.DangKyDto dangKy(Long lopHocPhanId) {
        return dangKy(lopHocPhanId, null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<RegistrationDtos.DangKyDto> xacNhanKeHoach(List<Long> lopHocPhanIds) {
        if (lopHocPhanIds == null || lopHocPhanIds.isEmpty()) {
            return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", null,
                    audit -> { throw ApiException.badRequest("Giỏ học phần đang trống"); });
        }
        if (new HashSet<>(lopHocPhanIds).size() != lopHocPhanIds.size()) {
            Long duplicateId = lopHocPhanIds.stream().filter(id -> java.util.Collections.frequency(lopHocPhanIds, id) > 1)
                    .findFirst().orElse(null);
            return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", duplicateId,
                    audit -> { throw ApiException.badRequest("Giỏ học phần có lớp bị lặp"); });
        }

        Student sinhVien = sinhVienHienTaiForUpdate();
        List<RegistrationDtos.DangKyDto> ketQua = new ArrayList<>();
        // Lock section rows in a stable order so overlapping multi-class plans do not deadlock.
        for (Long lopHocPhanId : lopHocPhanIds.stream().sorted().toList()) {
            ketQua.add(dangKyTrongGiaoDich(sinhVien, lopHocPhanId, null));
        }
        return ketQua;
    }

    private RegistrationDtos.DangKyDto dangKyTrongGiaoDich(Student sinhVien, Long lopHocPhanId,
                                                            String loaiDangKyYeuCau) {
        return auditLogService.execute("COURSE_REGISTRATION", "COURSE_SECTION", lopHocPhanId, audit -> {
        CourseClass lop = lopHocPhanRepository.findByIdForUpdate(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        Semester hocKy = lop.getHocKy();
        Subject monHoc = lop.getMonHoc();

        if (!hocKy.dangMoDangKy()) {
            throw ApiException.badRequest("Hiện không trong thời gian đăng ký của " + hocKy.getTen());
        }

        String loaiDuocPhep = loaiDangKyPhuHop(sinhVien, monHoc);
        if ("MOI".equals(loaiDuocPhep) && loaiDangKyYeuCau != null) {
            throw ApiException.badRequest("Bạn chưa có kết quả môn này nên không thuộc diện học lại hoặc học cải thiện");
        }
        if (!"MOI".equals(loaiDuocPhep) && loaiDangKyYeuCau == null) {
            throw ApiException.badRequest("Môn đã có kết quả trước đây; hãy đăng ký từ mục học lại/học cải thiện");
        }
        if (loaiDangKyYeuCau != null && !loaiDuocPhep.equals(loaiDangKyYeuCau)) {
            throw ApiException.badRequest("Kết quả gần nhất không khớp loại đăng ký. Loại phù hợp: "
                    + ("HOC_LAI".equals(loaiDuocPhep) ? "học lại" : "học cải thiện"));
        }

        List<CourseRegistration> dangKyHienTai =
                dangKyHocPhanRepository.findTheoHocKy(sinhVien.getId(), hocKy.getId(), RegistrationStatus.DA_DANG_KY);

        for (CourseRegistration daDangKy : dangKyHienTai) {
            if (daDangKy.getLopHocPhan().getMonHoc().getId().equals(monHoc.getId())) {
                throw ApiException.conflict("Bạn đã đăng ký môn " + monHoc.getTen() + " trong học kỳ này");
            }
        }

        for (CourseRegistration daDangKy : dangKyHienTai) {
            if (daDangKy.getLopHocPhan().trungLichVoi(lop)) {
                CourseClass trung = daDangKy.getLopHocPhan();
                throw ApiException.conflict("Trùng lịch học với lớp " + trung.getMa() + " (thứ "
                        + trung.getThu() + ", tiết " + trung.getTietBatDau() + "-" + trung.getTietKetThuc() + ")");
            }
        }

        kiemTraTienQuyet(sinhVien, monHoc);

        if (lop.getSiSoHienTai() >= lop.getSiSoToiDa()) {
            throw ApiException.conflict("Lớp " + lop.getMa() + " đã đủ sĩ số");
        }

        int tinChiDaDangKy = dangKyHienTai.stream()
                .mapToInt(dk -> dk.getLopHocPhan().getMonHoc().getSoTinChi())
                .sum();
        if (tinChiDaDangKy + monHoc.getSoTinChi() > hocKy.getSoTinChiToiDa()) {
            throw ApiException.badRequest("Vượt quá số tín chỉ tối đa (" + hocKy.getSoTinChiToiDa()
                    + " tín chỉ) của " + hocKy.getTen());
        }

        CourseRegistration dangKy = dangKyHocPhanRepository
                .findBySinhVienIdAndLopHocPhanId(sinhVien.getId(), lop.getId())
                .orElseGet(() -> {
                    CourseRegistration moi = new CourseRegistration();
                    moi.setSinhVien(sinhVien);
                    moi.setLopHocPhan(lop);
                    return moi;
                });
        LocalDateTime thoiDiemDangKy = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        if (dangKy.getThoiGianDangKy() != null
                && !thoiDiemDangKy.isAfter(dangKy.getThoiGianDangKy().truncatedTo(ChronoUnit.SECONDS))) {
            thoiDiemDangKy = dangKy.getThoiGianDangKy().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
        }
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);
        dangKy.setThoiGianDangKy(thoiDiemDangKy);
        lop.setSiSoHienTai(lop.getSiSoHienTai() + 1);

        CourseRegistration daLuu = dangKyHocPhanRepository.save(dangKy);
        ghiLichSu(sinhVien, lop, daLuu, RegistrationStatus.DA_DANG_KY, thoiDiemDangKy, thoiDiemDangKy, null);
        notifyRegistrationChangedSafely(daLuu, true, thoiDiemDangKy, null);
        Map<String, Object> before = Map.of("maLop", lop.getMa(), "siSoToiDa", lop.getSiSoToiDa(),
                "siSoHienTai", lop.getSiSoHienTai() - 1);
        audit.changes(before, Map.of("maLop", lop.getMa(), "siSoToiDa", lop.getSiSoToiDa(),
                "siSoHienTai", lop.getSiSoHienTai(), "trangThai", dangKy.getTrangThai().name(),
                "dangKyId", daLuu.getId(), "loaiDangKy", "MOI".equals(loaiDuocPhep) ? "DANG_KY_MOI" : loaiDuocPhep));
        log.info("Sinh viên {} đăng ký lớp {} (môn {})", sinhVien.getMa(), lop.getMa(), monHoc.getMa());
        return mapper.dangKy(daLuu, null);
        });
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void huyDangKy(Long dangKyId, String lyDoHuy) {
        if (lyDoHuy != null && lyDoHuy.length() > 255) {
            throw ApiException.badRequest("Lý do hủy tối đa 255 ký tự");
        }
        auditLogService.execute("COURSE_REGISTRATION_CANCELLED", "COURSE_REGISTRATION", dangKyId, audit -> {
        Student sinhVien = sinhVienHienTaiForUpdate();
        CourseRegistration dangKy = dangKyHocPhanRepository.findByIdAndSinhVienId(dangKyId, sinhVien.getId())
                .orElseThrow(() -> ApiException.notFound("Đăng ký không tồn tại"));
        if (dangKy.getTrangThai() == RegistrationStatus.DA_HUY) {
            throw ApiException.badRequest("Đăng ký này đã được hủy trước đó");
        }

        CourseClass lop = lopHocPhanRepository.findByIdForUpdate(dangKy.getLopHocPhan().getId())
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        if (!lop.getHocKy().dangMoDangKy()) {
            throw ApiException.badRequest("Đã hết thời gian hủy đăng ký của " + lop.getHocKy().getTen());
        }

        long tongTinChiHienTai = dangKyHocPhanRepository.tinhTongTinChi(
                sinhVien.getId(), lop.getHocKy().getId(), RegistrationStatus.DA_DANG_KY);
        int tinChiConLai = (int) Math.max(0, tongTinChiHienTai - lop.getMonHoc().getSoTinChi());
        if (tinChiConLai > 0 && tinChiConLai < lop.getHocKy().getSoTinChiToiThieu()) {
            throw ApiException.badRequest("Không thể hủy: tổng tín chỉ còn lại (" + tinChiConLai
                    + ") thấp hơn mức tối thiểu " + lop.getHocKy().getSoTinChiToiThieu()
                    + ". Hãy đăng ký thêm học phần hoặc hủy toàn bộ học phần của học kỳ.");
        }

        Map<String, Object> before = Map.of("dangKyId", dangKy.getId(), "lopHocPhanId", lop.getId(),
                "maLop", lop.getMa(), "trangThai", dangKy.getTrangThai().name(),
                "siSoHienTai", lop.getSiSoHienTai());
        LocalDateTime thoiDiemHuy = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String normalizedReason = lyDoHuy == null || lyDoHuy.isBlank() ? null : lyDoHuy.trim();
        dangKy.setTrangThai(RegistrationStatus.DA_HUY);
        lop.setSiSoHienTai(Math.max(0, lop.getSiSoHienTai() - 1));
        ghiLichSu(sinhVien, lop, dangKy, RegistrationStatus.DA_HUY, dangKy.getThoiGianDangKy(), thoiDiemHuy,
                normalizedReason);
        notifyRegistrationChangedSafely(dangKy, false, thoiDiemHuy, normalizedReason);
        Map<String, Object> after = new java.util.LinkedHashMap<>();
        after.put("dangKyId", dangKy.getId());
        after.put("lopHocPhanId", lop.getId());
        after.put("maLop", lop.getMa());
        after.put("trangThai", dangKy.getTrangThai().name());
        after.put("siSoHienTai", lop.getSiSoHienTai());
        if (normalizedReason != null) after.put("lyDoHuy", normalizedReason);
        audit.changes(before, after);
        log.info("Sinh viên {} hủy đăng ký lớp {}", sinhVien.getMa(), lop.getMa());
        return null;
        });
    }

    private void notifyRegistrationChangedSafely(CourseRegistration registration, boolean registered,
                                                 LocalDateTime occurredAt, String reason) {
        try {
            thongBaoService.registrationChanged(registration, registered, occurredAt, reason);
        } catch (RuntimeException ex) {
            log.error("Không thể lập lịch thông báo cho đăng ký id={} (registered={}); sẽ đồng bộ bù từ lịch sử: {}",
                    registration != null ? registration.getId() : null, registered, ex.getClass().getSimpleName());
        }
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DangKyDto> danhSachCuaToi(Long hocKyId) {
        Student sinhVien = sinhVienHienTai();
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findTheoHocKy(sinhVien.getId(), hocKy.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> diemMap = layDiemTheoDangKy(danhSach);
        return danhSach.stream()
                .map(dk -> mapper.dangKy(dk, diemMap.get(dk.getId())))
                .toList();
    }

    @Transactional
    public Page<RegistrationDtos.LichSuDangKyDto> lichSuCuaToi(Long hocKyId, int page, int size) {
        if (page < 0) throw ApiException.badRequest("Trang phải lớn hơn hoặc bằng 0");
        if (size < 1 || size > 100) throw ApiException.badRequest("Kích thước trang phải từ 1 đến 100");
        Student sinhVien = sinhVienHienTai();
        Semester hocKy = hocKyService.lay(hocKyId);
        // Preserve visibility for registrations created before the event table existed.
        for (CourseRegistration legacy : dangKyHocPhanRepository
                .findBySinhVienIdAndLopHocPhanHocKyIdOrderByIdAsc(sinhVien.getId(), hocKy.getId())) {
            if (!lichSuDangKyRepository.existsByDangKyId(legacy.getId())) {
                LocalDateTime registeredAt = legacy.getThoiGianDangKy();
                ghiLichSu(sinhVien, legacy.getLopHocPhan(), legacy, legacy.getTrangThai(), registeredAt,
                        registeredAt, null);
            }
        }
        return lichSuDangKyRepository.findLichSu(sinhVien.getId(), hocKy.getId(),
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("thoiDiem"), Sort.Order.desc("id"))))
                .map(event -> {
                    CourseClass lop = event.getLopHocPhan();
                    return new RegistrationDtos.LichSuDangKyDto(event.getId(), event.getDangKyId(), hocKy.getId(),
                            lop.getMa(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                            event.getThoiGianDangKy(), event.getTrangThai().name(), event.getThoiDiem(),
                            event.getThoiGianHuy(),
                            event.getLyDoHuy());
                });
    }

    private void ghiLichSu(Student sinhVien, CourseClass lop, CourseRegistration registration,
                           RegistrationStatus status, LocalDateTime registeredAt, LocalDateTime occurredAt,
                           String reason) {
        RegistrationHistory event = new RegistrationHistory();
        event.setSinhVien(sinhVien);
        event.setLopHocPhan(lop);
        event.setHocKyId(lop.getHocKy().getId());
        event.setDangKyId(registration.getId());
        event.setTrangThai(status);
        event.setThoiGianDangKy(registeredAt);
        event.setThoiDiem(occurredAt);
        event.setLyDoHuy(reason);
        if (status == RegistrationStatus.DA_HUY) event.setThoiGianHuy(occurredAt);
        lichSuDangKyRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieu(Long hocKyId) {
        Student sinhVien = sinhVienHienTai();
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        return dangKyHocPhanRepository
                .findTheoHocKy(sinhVien.getId(), hocKy.getId(), RegistrationStatus.DA_DANG_KY)
                .stream().map(mapper::thoiKhoaBieu).toList();
    }

    private void kiemTraTienQuyet(Student sinhVien, Subject monHoc) {
        List<PrerequisiteSubject> danhSachTienQuyet =
                monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHoc.getId());
        if (danhSachTienQuyet.isEmpty()) {
            return;
        }
        Set<Long> monDaDat = new HashSet<>(diemRepository.findMonDaDat(sinhVien.getId(), AcademicRules.DIEM_DAT));
        for (PrerequisiteSubject lienKet : danhSachTienQuyet) {
            Subject tienQuyet = lienKet.getMonTienQuyet();
            if (!monDaDat.contains(tienQuyet.getId())) {
                throw ApiException.badRequest("Chưa đạt môn tiên quyết: "
                        + tienQuyet.getMa() + " - " + tienQuyet.getTen());
            }
        }
    }

    private String loaiDangKyPhuHop(Student sinhVien, Subject monHoc) {
        List<CourseRegistration> attempts = dangKyHocPhanRepository
                .findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(sinhVien.getId());
        if (attempts.isEmpty()) return "MOI";
        List<Long> ids = attempts.stream().map(CourseRegistration::getId).toList();
        Map<Long, Grade> grades = diemRepository.findByDangKyIdIn(ids).stream()
                .collect(Collectors.toMap(grade -> grade.getDangKy().getId(), Function.identity()));
        for (CourseRegistration attempt : attempts) {
            if (!attempt.getLopHocPhan().getMonHoc().getId().equals(monHoc.getId())) continue;
            Grade grade = grades.get(attempt.getId());
            if (attempt.getLopHocPhan().isBangDiemDaCongBo() && grade != null && grade.getDiemTongKet() != null) {
                return grade.getDiemTongKet() < AcademicRules.DIEM_DAT ? "HOC_LAI" : "HOC_CAI_THIEN";
            }
        }
        return "MOI";
    }

    private Map<Long, Grade> layDiemTheoDangKy(List<CourseRegistration> danhSach) {
        if (danhSach.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = danhSach.stream().map(CourseRegistration::getId).toList();
        return diemRepository.findByDangKyIdIn(ids).stream()
                .collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));
    }

    private Student sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private Student sinhVienHienTaiForUpdate() {
        return sinhVienRepository.findByUserIdForUpdate(currentUser.get().getId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }
}
