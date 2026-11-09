package com.edu.registration.service;

import com.edu.registration.config.ClassScheduleConflictProperties;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.ClassAbsenceNoticeRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class CourseClassService {

    private final CourseClassRepository lopHocPhanRepository;
    private final SubjectRepository monHocRepository;
    private final LecturerRepository giangVienRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final RegistrationHistoryRepository lichSuDangKyRepository;
    private final ClassAbsenceNoticeRepository thongBaoNghiRepository;
    private final SemesterService hocKyService;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;
    private final NotificationService thongBaoService;
    private final ClassScheduleConflictService conflictService;

    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService) {
        this(lopHocPhanRepository, monHocRepository, giangVienRepository, dangKyHocPhanRepository,
                null, thongBaoNghiRepository, hocKyService, auditLogService, currentUser, mapper, thongBaoService, null);
    }

    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             RegistrationHistoryRepository lichSuDangKyRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService) {
        this(lopHocPhanRepository, monHocRepository, giangVienRepository, dangKyHocPhanRepository,
                lichSuDangKyRepository, thongBaoNghiRepository, hocKyService, auditLogService, currentUser, mapper, thongBaoService, null);
    }

    @Autowired
    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             RegistrationHistoryRepository lichSuDangKyRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService,
                             ClassScheduleConflictService conflictService) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.monHocRepository = monHocRepository;
        this.giangVienRepository = giangVienRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.lichSuDangKyRepository = lichSuDangKyRepository;
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.hocKyService = hocKyService;
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.thongBaoService = thongBaoService;
        this.conflictService = conflictService != null ? conflictService : new ClassScheduleConflictService(lopHocPhanRepository, new ClassScheduleConflictProperties());
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSach(Long hocKyId, String keyword) {
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseClass> danhSach = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId());
        if (keyword != null && !keyword.isBlank()) {
            String tuKhoa = keyword.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(lop -> lop.getMa().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getMonHoc().getTen().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getMonHoc().getMa().toLowerCase(Locale.ROOT).contains(tuKhoa)
                            || lop.getGiangVien().getHoTen().toLowerCase(Locale.ROOT).contains(tuKhoa))
                    .toList();
        }
        return danhSach.stream().map(mapper::lopHocPhan).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSachTheoGiangVien(Long giangVienId, Long hocKyId) {
        if (hocKyId != null) {
            return lopHocPhanRepository.findByGiangVienIdAndHocKyIdOrderByMaAsc(giangVienId, hocKyId)
                    .stream().map(mapper::lopHocPhan).toList();
        }
        return lopHocPhanRepository.findByGiangVienIdOrderByMaAsc(giangVienId)
                .stream().map(mapper::lopHocPhan).toList();
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSachCuaToi(Long hocKyId) {
        Lecturer giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        return danhSachTheoGiangVien(giangVien.getId(), hocKyId);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieuCuaToi(Long hocKyId) {
        Lecturer giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseClass> danhSach = lopHocPhanRepository
                .findByGiangVienIdAndHocKyIdOrderByMaAsc(giangVien.getId(), hocKy.getId());
        return danhSach.stream()
                .map(lop -> new RegistrationDtos.ThoiKhoaBieuDto(lop.getId(), lop.getMa(),
                        lop.getMonHoc().getTen(), lop.getPhong(), lop.getThu(), lop.getTietBatDau(),
                        lop.getSoTiet(), lop.getGiangVien().getHoTen()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseClassDtos.LopHocPhanDto chiTiet(Long id) {
        return mapper.lopHocPhan(lopHocPhanRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại")));
    }

    @Transactional(readOnly = true)
    public CourseClassDtos.KetQuaKiemTraXungDotDto kiemTraXungDot(CourseClassDtos.KiemTraXungDotRequest request) {
        return conflictService.kiemTraXungDot(request, false);
    }

    public CourseClassDtos.LopHocPhanResponse tao(CourseClassDtos.LopHocPhanRequest request) {
        return auditLogService.execute("COURSE_SECTION_CREATED", "COURSE_SECTION", null, audit -> {
            kiemTraLich(request);
            String ma = request.ma().trim();
            if (lopHocPhanRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã lớp học phần đã tồn tại");
            }
            Subject monHoc = monHocRepository.findById(request.monHocId())
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            Lecturer giangVien = giangVienRepository.findById(request.giangVienId())
                    .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
            Semester hocKy = hocKyService.lay(request.hocKyId());

            CourseClassDtos.KetQuaKiemTraXungDotDto ketQuaXungDot =
                    conflictService.kiemTraVaApDungKhiLuu(request, null);

            CourseClass lop = new CourseClass();
            lop.setMa(ma);
            lop.setMonHoc(monHoc);
            lop.setHocKy(hocKy);
            lop.setGiangVien(giangVien);
            apDung(lop, request);
            lop.setSiSoHienTai(0);
            lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);

            CourseClass saved = lopHocPhanRepository.save(lop);
            audit.entityId(saved.getId());
            audit.changes(Map.of(), auditSnapshot(saved));
            return new CourseClassDtos.LopHocPhanResponse(
                    mapper.lopHocPhan(saved),
                    ketQuaXungDot.moTaCanhBao(),
                    ketQuaXungDot.danhSachXungDot(),
                    ketQuaXungDot.coXungDot(),
                    ketQuaXungDot.chinhSachEnforcement(),
                    ketQuaXungDot.chinhSachDaChot(),
                    ketQuaXungDot.ghiChuChinhSach()
            );
        });
    }

    public CourseClassDtos.LopHocPhanResponse sua(Long id, CourseClassDtos.LopHocPhanRequest request) {
        return auditLogService.execute("COURSE_SECTION_UPDATED", "COURSE_SECTION", id, audit -> {
            kiemTraLich(request);
            CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            Map<String, Object> before = auditSnapshot(lop);
            String ma = request.ma().trim();
            if (!lop.getMa().equalsIgnoreCase(ma) && lopHocPhanRepository.existsByMa(ma)) {
                throw ApiException.conflict("Mã lớp học phần đã tồn tại");
            }
            if (request.siSoToiDa() < lop.getSiSoHienTai()) {
                throw ApiException.badRequest("Sĩ số tối đa không được nhỏ hơn số sinh viên đã đăng ký ("
                        + lop.getSiSoHienTai() + ")");
            }
            Subject monHoc = monHocRepository.findById(request.monHocId())
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            Lecturer giangVien = giangVienRepository.findById(request.giangVienId())
                    .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
            Semester hocKy = hocKyService.lay(request.hocKyId());

            CourseClassDtos.KetQuaKiemTraXungDotDto ketQuaXungDot =
                    conflictService.kiemTraVaApDungKhiLuu(request, id);

            lop.setMa(ma);
            lop.setMonHoc(monHoc);
            lop.setHocKy(hocKy);
            lop.setGiangVien(giangVien);
            apDung(lop, request);
            Map<String, Object> after = auditSnapshot(lop);
            audit.changes(before, after);
            thongBaoService.sectionChanged(lop, before, after, null);
            return new CourseClassDtos.LopHocPhanResponse(
                    mapper.lopHocPhan(lop),
                    ketQuaXungDot.moTaCanhBao(),
                    ketQuaXungDot.danhSachXungDot(),
                    ketQuaXungDot.coXungDot(),
                    ketQuaXungDot.chinhSachEnforcement(),
                    ketQuaXungDot.chinhSachDaChot(),
                    ketQuaXungDot.ghiChuChinhSach()
            );
        });
    }

    /**
     * Cancels a course section, transitions all currently active registrations (DA_DANG_KY) to DA_HUY,
     * records durable registration history events, and notifies only the affected students.
     */
    public CourseClassDtos.HuyLopHocPhanResponse huyLopHocPhan(Long id, String lyDo) {
        if (lyDo != null && lyDo.trim().length() > 200) {
            throw ApiException.badRequest("Lý do hủy lớp học phần tối đa 200 ký tự");
        }
        String normalizedReason = (lyDo == null || lyDo.isBlank()) ? null : lyDo.trim();
        String historyReason = normalizedReason == null
                ? NotificationService.SECTION_CANCELLED_REASON_PREFIX
                : NotificationService.SECTION_CANCELLED_REASON_PREFIX + ": " + normalizedReason;

        return auditLogService.execute("COURSE_SECTION_CANCELLED", "COURSE_SECTION", id, audit -> {
            CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            Map<String, Object> before = auditSnapshot(lop);
            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(id, RegistrationStatus.DA_DANG_KY);
            LocalDateTime now = LocalDateTime.now();

            for (CourseRegistration registration : activeRegistrations) {
                registration.setTrangThai(RegistrationStatus.DA_HUY);
                dangKyHocPhanRepository.save(registration);
                if (lichSuDangKyRepository != null) {
                    RegistrationHistory event = new RegistrationHistory();
                    event.setSinhVien(registration.getSinhVien());
                    event.setLopHocPhan(lop);
                    event.setHocKyId(lop.getHocKy().getId());
                    event.setDangKyId(registration.getId());
                    event.setTrangThai(RegistrationStatus.DA_HUY);
                    event.setThoiGianDangKy(registration.getThoiGianDangKy());
                    event.setThoiDiem(now);
                    event.setThoiGianHuy(now);
                    event.setLyDoHuy(historyReason);
                    lichSuDangKyRepository.save(event);
                }
            }
            lop.setSiSoHienTai(0);
            lopHocPhanRepository.save(lop);

            Map<String, Object> after = new LinkedHashMap<>(auditSnapshot(lop));
            after.put("trangThai", "CANCELLED");
            after.put("soDangKyBiHuy", activeRegistrations.size());
            if (normalizedReason != null) {
                after.put("lyDo", normalizedReason);
            }
            audit.changes(before, after);

            thongBaoService.sectionCancelled(lop, activeRegistrations, now, normalizedReason);
            return new CourseClassDtos.HuyLopHocPhanResponse(
                    lop.getId(), lop.getMa(), activeRegistrations.size(), normalizedReason);
        });
    }

    public void xoa(Long id) {
        auditLogService.execute("COURSE_SECTION_DELETED", "COURSE_SECTION", id, audit -> {
            CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            audit.changes(auditSnapshot(lop), Map.of("trangThai", "DELETED"));
            if (dangKyHocPhanRepository.existsByLopHocPhanId(id)) {
                throw ApiException.conflict("Lớp đã có sinh viên đăng ký, không thể xóa");
            }
            thongBaoNghiRepository.deleteByLopHocPhanId(id);
            lopHocPhanRepository.delete(lop);
            return null;
        });
    }

    private Map<String, Object> auditSnapshot(CourseClass lop) {
        return Map.ofEntries(
                Map.entry("maLop", lop.getMa()),
                Map.entry("monHocId", lop.getMonHoc().getId()),
                Map.entry("hocKyId", lop.getHocKy().getId()),
                Map.entry("giangVienId", lop.getGiangVien().getId()),
                Map.entry("phong", lop.getPhong()),
                Map.entry("thu", lop.getThu()),
                Map.entry("tietBatDau", lop.getTietBatDau()),
                Map.entry("soTiet", lop.getSoTiet()),
                Map.entry("siSoToiDa", lop.getSiSoToiDa()),
                Map.entry("siSoHienTai", lop.getSiSoHienTai()),
                Map.entry("conLai", lop.getConLai()));
    }

    private void kiemTraLich(CourseClassDtos.LopHocPhanRequest request) {
        if (request.tietBatDau() + request.soTiet() - 1 > 12) {
            throw ApiException.badRequest("Lịch học vượt quá tiết 12");
        }
    }

    private List<String> kiemTraCanhBao(CourseClassDtos.LopHocPhanRequest request, Long boQuaId) {
        CourseClass nhap = new CourseClass();
        nhap.setThu(request.thu());
        nhap.setTietBatDau(request.tietBatDau());
        nhap.setSoTiet(request.soTiet());

        List<String> canhBao = new ArrayList<>();
        for (CourseClass lop : lopHocPhanRepository.findByHocKyIdOrderByMaAsc(request.hocKyId())) {
            if (boQuaId != null && boQuaId.equals(lop.getId())) {
                continue;
            }
            if (!lop.trungLichVoi(nhap)) {
                continue;
            }
            if (lop.getPhong().equalsIgnoreCase(request.phong().trim())) {
                canhBao.add("Trùng phòng " + lop.getPhong() + " với lớp " + lop.getMa());
            }
            if (lop.getGiangVien().getId().equals(request.giangVienId())) {
                canhBao.add("Giảng viên đã có lớp " + lop.getMa() + " trong cùng khung giờ");
            }
        }
        return canhBao;
    }

    private void apDung(CourseClass lop, CourseClassDtos.LopHocPhanRequest request) {
        lop.setPhong(request.phong().trim());
        lop.setThu(request.thu());
        lop.setTietBatDau(request.tietBatDau());
        lop.setSoTiet(request.soTiet());
        lop.setSiSoToiDa(request.siSoToiDa());
    }
}
