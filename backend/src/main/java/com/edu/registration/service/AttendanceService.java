package com.edu.registration.service;

import com.edu.registration.config.AttendanceProperties;
import com.edu.registration.dto.AttendanceDtos;
import com.edu.registration.entity.ClassSession;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.AttendanceRecord;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.AttendanceStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.ClassSessionRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.AttendanceRecordRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    public static final String AUDIT_ACTION_SESSION_CREATED = "ATTENDANCE_SESSION_CREATED";
    public static final String AUDIT_ACTION_SESSION_UPDATED = "ATTENDANCE_SESSION_UPDATED";
    public static final String AUDIT_ACTION_SESSION_DELETED = "ATTENDANCE_SESSION_DELETED";
    public static final String AUDIT_ACTION_SESSION_LOCKED = "ATTENDANCE_SESSION_LOCKED";
    public static final String AUDIT_ACTION_SESSION_UNLOCKED = "ATTENDANCE_SESSION_UNLOCKED";
    public static final String AUDIT_ACTION_RECORDED = "ATTENDANCE_RECORDED";
    public static final String AUDIT_ENTITY = "CLASS_ATTENDANCE";

    private final ClassSessionRepository buoiHocRepository;
    private final AttendanceRecordRepository diemDanhRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final LecturerRepository giangVienRepository;
    private final StudentRepository sinhVienRepository;
    private final SemesterRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;
    private final AttendanceProperties attendanceProperties;
    private final AttendanceAlertService attendanceAlertService;

    public AttendanceService(ClassSessionRepository buoiHocRepository,
                             AttendanceRecordRepository diemDanhRepository,
                             CourseClassRepository lopHocPhanRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             LecturerRepository giangVienRepository,
                             StudentRepository sinhVienRepository,
                             SemesterRepository hocKyRepository,
                             CurrentUser currentUser,
                             AuditLogService auditLogService,
                             AttendanceProperties attendanceProperties) {
        this(buoiHocRepository, diemDanhRepository, lopHocPhanRepository, dangKyHocPhanRepository,
                giangVienRepository, sinhVienRepository, hocKyRepository, currentUser, auditLogService,
                attendanceProperties, null);
    }

    @Autowired
    public AttendanceService(ClassSessionRepository buoiHocRepository,
                             AttendanceRecordRepository diemDanhRepository,
                             CourseClassRepository lopHocPhanRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             LecturerRepository giangVienRepository,
                             StudentRepository sinhVienRepository,
                             SemesterRepository hocKyRepository,
                             CurrentUser currentUser,
                             AuditLogService auditLogService,
                             AttendanceProperties attendanceProperties,
                             AttendanceAlertService attendanceAlertService) {
        this.buoiHocRepository = buoiHocRepository;
        this.diemDanhRepository = diemDanhRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
        this.attendanceProperties = attendanceProperties;
        this.attendanceAlertService = attendanceAlertService;
    }

    /**
     * Tạo buổi học gắn với lớp, ngày và ca (QLSV-70).
     * Chặn trùng bằng unique constraint (lop_hoc_phan_id, ngay_hoc, ca_hoc).
     */
    public AttendanceDtos.BuoiHocDto taoBuoiHoc(AttendanceDtos.TaoBuoiHocRequest request) {
        User actor = currentUser.get();
        CourseClass lop = kiemTraQuyenGiangVienVoiLop(request.lopHocPhanId(), actor);

        LocalDate ngayHoc = request.ngayHoc();
        Semester hocKy = lop.getHocKy();
        if (hocKy != null) {
            if (ngayHoc.isBefore(hocKy.getNgayBatDau()) || ngayHoc.isAfter(hocKy.getNgayKetThuc())) {
                throw ApiException.badRequest("Ngày học (" + ngayHoc + ") phải nằm trong thời gian học kỳ: "
                        + hocKy.getNgayBatDau() + " đến " + hocKy.getNgayKetThuc());
            }
        }

        int caHoc = request.caHoc();
        if (buoiHocRepository.existsByLopHocPhanIdAndNgayHocAndCaHoc(lop.getId(), ngayHoc, caHoc)) {
            throw ApiException.conflict("Buổi học vào ngày " + ngayHoc + " (ca " + caHoc
                    + ") của lớp " + lop.getMa() + " đã tồn tại.");
        }

        ClassSession buoiHoc = new ClassSession();
        buoiHoc.setLopHocPhan(lop);
        buoiHoc.setNgayHoc(ngayHoc);
        buoiHoc.setCaHoc(caHoc);
        buoiHoc.setPhongHoc(request.phongHoc() != null && !request.phongHoc().isBlank()
                ? request.phongHoc().trim() : lop.getPhong());
        buoiHoc.setTietBatDau(request.tietBatDau() != null ? request.tietBatDau() : lop.getTietBatDau());
        buoiHoc.setSoTiet(request.soTiet() != null ? request.soTiet() : lop.getSoTiet());
        buoiHoc.setNoiDung(request.noiDung() != null ? request.noiDung().trim() : null);
        buoiHoc.setDaKhoa(false);
        buoiHoc.setNguoiTao(actor);
        buoiHoc.setThoiGianTao(LocalDateTime.now());

        try {
            ClassSession saved = buoiHocRepository.saveAndFlush(buoiHoc);
            auditLogService.execute(AUDIT_ACTION_SESSION_CREATED, AUDIT_ENTITY, saved.getId(), context -> {
                context.changes(Map.of(), Map.of(
                        "buoiHocId", saved.getId(),
                        "lopHocPhanId", lop.getId(),
                        "maLop", lop.getMa(),
                        "ngayHoc", ngayHoc.toString(),
                        "caHoc", caHoc
                ));
                return null;
            });
            if (attendanceAlertService != null) {
                attendanceAlertService.kiemTraVaCapNhatCanhBaoChoLop(saved.getLopHocPhan().getId());
            }
            return mapBuoiHocToDto(saved);
        } catch (DataIntegrityViolationException dive) {
            throw ApiException.conflict("Buổi học vào ngày " + ngayHoc + " (ca " + caHoc
                    + ") của lớp " + lop.getMa() + " đã tồn tại.");
        }
    }

    /**
     * Sửa thông tin buổi học trước khi khóa.
     * Buổi đã khóa chỉ sửa bằng quyền đặc biệt (Role.QUAN_TRI).
     */
    public AttendanceDtos.BuoiHocDto capNhatBuoiHoc(Long buoiHocId, AttendanceDtos.CapNhatBuoiHocRequest request) {
        User actor = currentUser.get();
        ClassSession buoiHoc = buoiHocRepository.findWithDetailsById(buoiHocId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy buổi học"));

        kiemTraQuyenGiangVienVoiLop(buoiHoc.getLopHocPhan().getId(), actor);

        if (buoiHoc.isDaKhoa() && actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.badRequest("Buổi học đã khóa. Chỉ Quản trị viên (quyền đặc biệt) mới có quyền chỉnh sửa buổi học đã khóa.");
        }

        LocalDate ngayMoi = request.ngayHoc() != null ? request.ngayHoc() : buoiHoc.getNgayHoc();
        Integer caMoi = request.caHoc() != null ? request.caHoc() : buoiHoc.getCaHoc();

        Semester hocKy = buoiHoc.getLopHocPhan().getHocKy();
        if (hocKy != null && (ngayMoi.isBefore(hocKy.getNgayBatDau()) || ngayMoi.isAfter(hocKy.getNgayKetThuc()))) {
            throw ApiException.badRequest("Ngày học (" + ngayMoi + ") phải nằm trong thời gian học kỳ: "
                    + hocKy.getNgayBatDau() + " đến " + hocKy.getNgayKetThuc());
        }

        if (!ngayMoi.equals(buoiHoc.getNgayHoc()) || !caMoi.equals(buoiHoc.getCaHoc())) {
            Optional<ClassSession> trung = buoiHocRepository.findByLopHocPhanIdAndNgayHocAndCaHoc(
                    buoiHoc.getLopHocPhan().getId(), ngayMoi, caMoi);
            if (trung.isPresent() && !trung.get().getId().equals(buoiHoc.getId())) {
                throw ApiException.conflict("Buổi học vào ngày " + ngayMoi + " (ca " + caMoi
                        + ") của lớp này đã tồn tại.");
            }
        }

        buoiHoc.setNgayHoc(ngayMoi);
        buoiHoc.setCaHoc(caMoi);
        if (request.phongHoc() != null) buoiHoc.setPhongHoc(request.phongHoc().trim());
        if (request.tietBatDau() != null) buoiHoc.setTietBatDau(request.tietBatDau());
        if (request.soTiet() != null) buoiHoc.setSoTiet(request.soTiet());
        if (request.noiDung() != null) buoiHoc.setNoiDung(request.noiDung().trim());
        buoiHoc.setNguoiCapNhat(actor);
        buoiHoc.setThoiGianCapNhat(LocalDateTime.now());

        try {
            ClassSession saved = buoiHocRepository.saveAndFlush(buoiHoc);
            auditLogService.execute(AUDIT_ACTION_SESSION_UPDATED, AUDIT_ENTITY, saved.getId(), context -> {
                context.changes(Map.of(), Map.of(
                        "buoiHocId", saved.getId(),
                        "lopHocPhanId", saved.getLopHocPhan().getId(),
                        "ngayHoc", saved.getNgayHoc().toString(),
                        "caHoc", saved.getCaHoc()
                ));
                return null;
            });
            return mapBuoiHocToDto(saved);
        } catch (DataIntegrityViolationException dive) {
            throw ApiException.conflict("Buổi học vào ngày " + ngayMoi + " (ca " + caMoi + ") đã tồn tại.");
        }
    }

    /**
     * Khóa hoặc mở khóa buổi học.
     * Buổi đã khóa chỉ được mở khóa bằng quyền đặc biệt (Role.QUAN_TRI).
     */
    public AttendanceDtos.BuoiHocDto khoaBuoiHoc(Long buoiHocId, boolean khoa) {
        User actor = currentUser.get();
        ClassSession buoiHoc = buoiHocRepository.findWithDetailsById(buoiHocId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy buổi học"));

        kiemTraQuyenGiangVienVoiLop(buoiHoc.getLopHocPhan().getId(), actor);

        if (!khoa && buoiHoc.isDaKhoa() && actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ Quản trị viên (quyền đặc biệt) mới có quyền mở khóa buổi học đã khóa.");
        }

        buoiHoc.setDaKhoa(khoa);
        if (khoa) {
            buoiHoc.setThoiGianKhoa(LocalDateTime.now());
            buoiHoc.setNguoiKhoa(actor);
        } else {
            buoiHoc.setThoiGianKhoa(null);
            buoiHoc.setNguoiKhoa(null);
        }
        buoiHoc.setNguoiCapNhat(actor);
        buoiHoc.setThoiGianCapNhat(LocalDateTime.now());

        ClassSession saved = buoiHocRepository.saveAndFlush(buoiHoc);
        String auditAction = khoa ? AUDIT_ACTION_SESSION_LOCKED : AUDIT_ACTION_SESSION_UNLOCKED;
        auditLogService.execute(auditAction, AUDIT_ENTITY, saved.getId(), context -> {
            context.changes(Map.of(), Map.of(
                    "buoiHocId", saved.getId(),
                    "lopHocPhanId", saved.getLopHocPhan().getId(),
                    "daKhoa", khoa ? 1 : 0
            ));
            return null;
        });

        return mapBuoiHocToDto(saved);
    }

    /**
     * Xóa buổi học (và các bản ghi điểm danh kèm theo).
     */
    public void xoaBuoiHoc(Long buoiHocId) {
        User actor = currentUser.get();
        ClassSession buoiHoc = buoiHocRepository.findWithDetailsById(buoiHocId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy buổi học"));

        kiemTraQuyenGiangVienVoiLop(buoiHoc.getLopHocPhan().getId(), actor);

        if (buoiHoc.isDaKhoa() && actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.badRequest("Buổi học đã khóa. Chỉ Quản trị viên mới có thể xóa buổi học đã khóa.");
        }

        Long lopId = buoiHoc.getLopHocPhan().getId();
        diemDanhRepository.deleteByBuoiHocId(buoiHocId);
        buoiHocRepository.delete(buoiHoc);

        auditLogService.execute(AUDIT_ACTION_SESSION_DELETED, AUDIT_ENTITY, buoiHocId, context -> {
            context.changes(Map.of("buoiHocId", buoiHocId, "lopHocPhanId", lopId), Map.of());
            return null;
        });
        if (attendanceAlertService != null) {
            attendanceAlertService.kiemTraVaCapNhatCanhBaoChoLop(lopId);
        }
    }

    /**
     * Điểm danh buổi học: mỗi sinh viên tối đa 1 trạng thái trong mỗi buổi.
     * Cho sửa trước khi khóa buổi. Buổi khóa chỉ sửa bằng quyền đặc biệt (Role.QUAN_TRI).
     */
    public AttendanceDtos.BuoiHocChiTietDto luuDiemDanh(Long buoiHocId, AttendanceDtos.LuuDiemDanhRequest request) {
        User actor = currentUser.get();
        ClassSession buoiHoc = buoiHocRepository.findWithDetailsById(buoiHocId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy buổi học"));

        CourseClass lop = kiemTraQuyenGiangVienVoiLop(buoiHoc.getLopHocPhan().getId(), actor);

        if (buoiHoc.isDaKhoa() && actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.badRequest("Buổi học đã khóa. Chỉ Quản trị viên (quyền đặc biệt) mới có quyền chỉnh sửa điểm danh của buổi học đã khóa.");
        }

        List<CourseRegistration> dangKys = dangKyHocPhanRepository.findByLopHocPhan(lop.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Student> sinhVienHopLeMap = dangKys.stream()
                .map(CourseRegistration::getSinhVien)
                .filter(sv -> sv != null && sv.getUser() != null && sv.getUser().isTrangThai())
                .collect(Collectors.toMap(Student::getId, sv -> sv, (a, b) -> a));

        List<AttendanceRecord> existingList = diemDanhRepository.findByBuoiHocIdOrderBySinhVienMaAsc(buoiHocId);
        Map<Long, AttendanceRecord> existingMap = existingList.stream()
                .collect(Collectors.toMap(dd -> dd.getSinhVien().getId(), dd -> dd));

        LocalDateTime now = LocalDateTime.now();
        int updatedCount = 0;

        for (AttendanceDtos.DiemDanhRecordRequest item : request.danhSach()) {
            if (item.sinhVienId() == null || item.trangThai() == null) {
                continue;
            }
            Student sv = sinhVienHopLeMap.get(item.sinhVienId());
            if (sv == null) {
                throw ApiException.badRequest("Sinh viên ID " + item.sinhVienId()
                        + " không đăng ký hiệu lực trong lớp học phần này.");
            }

            AttendanceRecord dd = existingMap.get(item.sinhVienId());
            if (dd == null) {
                dd = new AttendanceRecord();
                dd.setBuoiHoc(buoiHoc);
                dd.setSinhVien(sv);
                dd.setNguoiTao(actor);
                dd.setThoiGianTao(now);
            }
            dd.setTrangThai(item.trangThai());
            dd.setGhiChu(item.ghiChu() != null ? item.ghiChu().trim() : null);
            dd.setNguoiCapNhat(actor);
            dd.setThoiGianCapNhat(now);

            diemDanhRepository.save(dd);
            updatedCount++;
        }

        if (Boolean.TRUE.equals(request.khoaBuoi())) {
            buoiHoc.setDaKhoa(true);
            buoiHoc.setThoiGianKhoa(now);
            buoiHoc.setNguoiKhoa(actor);
        }
        buoiHoc.setNguoiCapNhat(actor);
        buoiHoc.setThoiGianCapNhat(now);
        buoiHocRepository.save(buoiHoc);

        final int countFinal = updatedCount;
        auditLogService.execute(AUDIT_ACTION_RECORDED, AUDIT_ENTITY, buoiHoc.getId(), context -> {
            context.changes(Map.of(), Map.of(
                    "buoiHocId", buoiHoc.getId(),
                    "lopHocPhanId", lop.getId(),
                    "soSinhVienDiemDanh", countFinal,
                    "daKhoa", buoiHoc.isDaKhoa() ? 1 : 0
            ));
            return null;
        });

        if (attendanceAlertService != null) {
            attendanceAlertService.kiemTraVaCapNhatCanhBaoChoLop(lop.getId());
        }
        return chiTietBuoiHocVaDiemDanh(buoiHocId);
    }

    /**
     * Lấy chi tiết buổi học và danh sách điểm danh của từng sinh viên trong lớp.
     */
    @Transactional(readOnly = true)
    public AttendanceDtos.BuoiHocChiTietDto chiTietBuoiHocVaDiemDanh(Long buoiHocId) {
        ClassSession buoiHoc = buoiHocRepository.findWithDetailsById(buoiHocId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy buổi học"));

        User actor = currentUser.get();
        kiemTraQuyenDocLop(buoiHoc.getLopHocPhan(), actor);

        List<CourseRegistration> dangKys = dangKyHocPhanRepository.findByLopHocPhan(
                buoiHoc.getLopHocPhan().getId(), RegistrationStatus.DA_DANG_KY);

        List<AttendanceRecord> diemDanhs = diemDanhRepository.findByBuoiHocIdOrderBySinhVienMaAsc(buoiHocId);
        Map<Long, AttendanceRecord> diemDanhMap = diemDanhs.stream()
                .collect(Collectors.toMap(dd -> dd.getSinhVien().getId(), dd -> dd));

        List<AttendanceDtos.DiemDanhChiTietDto> chiTietList = new ArrayList<>();
        for (CourseRegistration dk : dangKys) {
            Student sv = dk.getSinhVien();
            if (sv == null) continue;
            AttendanceRecord dd = diemDanhMap.get(sv.getId());
            String lopHanhChinh = sv.getLopHanhChinh() != null ? sv.getLopHanhChinh().getTen() : "";
            chiTietList.add(new AttendanceDtos.DiemDanhChiTietDto(
                    dd != null ? dd.getId() : null,
                    sv.getId(),
                    sv.getMa(),
                    sv.getHoTen(),
                    lopHanhChinh,
                    dd != null ? dd.getTrangThai() : null,
                    dd != null ? dd.getGhiChu() : null,
                    dd != null && dd.getNguoiCapNhat() != null ? dd.getNguoiCapNhat().getUsername() : null,
                    dd != null ? dd.getThoiGianCapNhat() : null
            ));
        }

        return new AttendanceDtos.BuoiHocChiTietDto(mapBuoiHocToDto(buoiHoc), chiTietList);
    }

    /**
     * Danh sách buổi học của lớp học phần.
     */
    @Transactional(readOnly = true)
    public List<AttendanceDtos.BuoiHocDto> danhSachBuoiHocCuaLop(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần"));

        User actor = currentUser.get();
        kiemTraQuyenDocLop(lop, actor);

        List<ClassSession> buoiHocs = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        return buoiHocs.stream().map(this::mapBuoiHocToDto).toList();
    }

    /**
     * Báo cáo chuyên cần của cả lớp học phần (cho Giảng viên và Quản trị).
     */
    @Transactional(readOnly = true)
    public AttendanceDtos.BaoCaoChuyenCanLopDto baoCaoChuyenCanLop(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần"));

        User actor = currentUser.get();
        kiemTraQuyenDocLop(lop, actor);

        List<ClassSession> buoiHocs = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        List<CourseRegistration> dangKys = dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
        List<AttendanceRecord> allDiemDanhs = diemDanhRepository.findByBuoiHocLopHocPhanId(lopHocPhanId);

        Map<Long, List<AttendanceRecord>> studentDiemDanhMap = allDiemDanhs.stream()
                .collect(Collectors.groupingBy(dd -> dd.getSinhVien().getId()));

        int tongBuoi = buoiHocs.size();
        String congThuc = "Tỷ lệ có mặt = (Có mặt + Có phép) / "
                + (attendanceProperties.getMauSoChuyenCan().equals("SO_BUOI_QUY_DINH")
                ? "Số buổi quy định theo tín chỉ" : "Tổng số buổi đã diễn ra (" + tongBuoi + ")");

        List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto> sinhVienReport = new ArrayList<>();
        for (CourseRegistration dk : dangKys) {
            Student sv = dk.getSinhVien();
            if (sv == null) continue;

            List<AttendanceRecord> records = studentDiemDanhMap.getOrDefault(sv.getId(), Collections.emptyList());
            sinhVienReport.add(tinhBaoCaoSinhVien(sv, lop, tongBuoi, records));
        }

        List<AttendanceDtos.BuoiHocDto> buoiDtos = buoiHocs.stream().map(this::mapBuoiHocToDto).toList();

        return new AttendanceDtos.BaoCaoChuyenCanLopDto(
                lop.getId(),
                lop.getMa(),
                lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                lop.getHocKy() != null ? lop.getHocKy().getTen() : "",
                tongBuoi,
                dangKys.size(),
                congThuc,
                attendanceProperties.getGhiChuQuyChe(),
                sinhVienReport,
                buoiDtos
        );
    }

    /**
     * Báo cáo chuyên cần lọc theo sinh viên và học kỳ.
     * Sinh viên tự xem của mình; Giảng viên xem các lớp mình dạy; Quản trị xem tất cả.
     */
    @Transactional(readOnly = true)
    public List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto> baoCaoChuyenCanTheoSinhVien(Long sinhVienId, Long hocKyId) {
        User actor = currentUser.get();
        Student sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy sinh viên"));

        // Kiểm tra quyền truy cập hồ sơ sinh viên
        if (actor.getRole() == Role.SINH_VIEN) {
            Student currentSv = sinhVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.forbidden("Không tìm thấy hồ sơ sinh viên hiện tại"));
            if (!currentSv.getId().equals(sv.getId())) {
                throw ApiException.forbidden("Bạn chỉ được xem báo cáo chuyên cần của chính mình");
            }
        }

        List<CourseRegistration> dangKys;
        if (hocKyId != null) {
            dangKys = dangKyHocPhanRepository.findTheoHocKy(sv.getId(), hocKyId, RegistrationStatus.DA_DANG_KY);
        } else {
            dangKys = dangKyHocPhanRepository.findAllActiveOfStudent(sv.getId(), RegistrationStatus.DA_DANG_KY);
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.forbidden("Không tìm thấy hồ sơ giảng viên"));
            dangKys = dangKys.stream()
                    .filter(dk -> dk.getLopHocPhan().getGiangVien() != null
                            && dk.getLopHocPhan().getGiangVien().getId().equals(gv.getId()))
                    .toList();
        }

        List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto> result = new ArrayList<>();
        for (CourseRegistration dk : dangKys) {
            CourseClass lop = dk.getLopHocPhan();
            List<ClassSession> buoiHocs = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lop.getId());
            List<AttendanceRecord> records = diemDanhRepository.findBySinhVienAndLopHocPhan(sv.getId(), lop.getId());
            result.add(tinhBaoCaoSinhVien(sv, lop, buoiHocs.size(), records));
        }

        return result;
    }

    /**
     * Báo cáo chuyên cần cho sinh viên hiện tại theo học kỳ.
     */
    @Transactional(readOnly = true)
    public List<AttendanceDtos.BaoCaoChuyenCanSinhVienDto> baoCaoChuyenCanCuaSinhVienHienTai(Long hocKyId) {
        User actor = currentUser.get();
        Student sv = sinhVienRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        return baoCaoChuyenCanTheoSinhVien(sv.getId(), hocKyId);
    }

    private AttendanceDtos.BaoCaoChuyenCanSinhVienDto tinhBaoCaoSinhVien(
            Student sv, CourseClass lop, int tongBuoi, List<AttendanceRecord> records) {
        int coMat = 0;
        int vang = 0;
        int muon = 0;
        int coPhep = 0;

        for (AttendanceRecord r : records) {
            if (r.getTrangThai() == null) continue;
            switch (r.getTrangThai()) {
                case CO_MAT -> coMat++;
                case VANG -> vang++;
                case MUON -> muon++;
                case CO_PHEP -> coPhep++;
            }
        }

        // Mẫu số: tính theo cấu hình (mặc định tổng buổi đã điểm danh/tạo; nếu = 0 thì tỷ lệ là 100%)
        int mauSo;
        if ("SO_BUOI_QUY_DINH".equalsIgnoreCase(attendanceProperties.getMauSoChuyenCan())) {
            int soTinChi = lop.getMonHoc() != null ? lop.getMonHoc().getSoTinChi() : 3;
            // 1 tín chỉ = 15 tiết, 3 tiết/buổi -> 5 buổi / tín chỉ
            mauSo = Math.max(1, soTinChi * 5);
        } else {
            mauSo = tongBuoi > 0 ? tongBuoi : 1;
        }

        // Quy đổi muộn thành vắng nếu cấu hình yêu cầu
        int quyDoiVangTuMuon = attendanceProperties.getSoLanMuonThanhMotVang() > 0
                ? (muon / attendanceProperties.getSoLanMuonThanhMotVang()) : 0;
        int tongVang = vang + quyDoiVangTuMuon + (attendanceProperties.isCoPhepTinhVang() ? coPhep : 0);

        // Buổi hợp lệ có mặt
        int tongCoMat = coMat + (!attendanceProperties.isCoPhepTinhVang() ? coPhep : 0);

        double tyLeCoMat = tongBuoi == 0 ? 100.0 : Math.min(100.0, Math.max(0.0,
                Math.round(((double) tongCoMat / mauSo) * 1000.0) / 10.0));
        double tyLeVang = tongBuoi == 0 ? 0.0 : Math.min(100.0, Math.max(0.0,
                Math.round(((double) tongVang / mauSo) * 1000.0) / 10.0));

        String lopHanhChinh = sv.getLopHanhChinh() != null ? sv.getLopHanhChinh().getTen() : "";
        int soTinChi = lop.getMonHoc() != null ? lop.getMonHoc().getSoTinChi() : 0;

        return new AttendanceDtos.BaoCaoChuyenCanSinhVienDto(
                sv.getId(),
                sv.getMa(),
                sv.getHoTen(),
                lopHanhChinh,
                lop.getId(),
                lop.getMa(),
                lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                soTinChi,
                tongBuoi,
                coMat,
                vang,
                muon,
                coPhep,
                tyLeCoMat,
                tyLeVang,
                attendanceProperties.getGhiChuQuyChe()
        );
    }

    private CourseClass kiemTraQuyenGiangVienVoiLop(Long lopHocPhanId, User actor) {
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        if (actor.getRole() == Role.QUAN_TRI) {
            return lop;
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(gv.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
            return lop;
        }

        throw ApiException.forbidden("Bạn không có quyền thực hiện thao tác này");
    }

    private void kiemTraQuyenDocLop(CourseClass lop, User actor) {
        if (actor.getRole() == Role.QUAN_TRI) {
            return;
        }
        if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(gv.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
            return;
        }
        if (actor.getRole() == Role.SINH_VIEN) {
            Student sv = sinhVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
            boolean enrolled = dangKyHocPhanRepository
                    .findBySinhVienIdAndLopHocPhanId(sv.getId(), lop.getId())
                    .filter(dk -> dk.getTrangThai() == RegistrationStatus.DA_DANG_KY)
                    .isPresent();
            if (!enrolled) {
                throw ApiException.forbidden("Bạn không tham gia lớp học phần này");
            }
            return;
        }
        throw ApiException.forbidden("Không có quyền truy cập");
    }

    private AttendanceDtos.BuoiHocDto mapBuoiHocToDto(ClassSession buoi) {
        CourseClass lop = buoi.getLopHocPhan();
        List<AttendanceRecord> records = diemDanhRepository.findByBuoiHocIdOrderBySinhVienMaAsc(buoi.getId());

        int coMat = 0;
        int vang = 0;
        int muon = 0;
        int coPhep = 0;
        for (AttendanceRecord r : records) {
            if (r.getTrangThai() == null) continue;
            switch (r.getTrangThai()) {
                case CO_MAT -> coMat++;
                case VANG -> vang++;
                case MUON -> muon++;
                case CO_PHEP -> coPhep++;
            }
        }

        int tongSinhVien = lop != null ? lop.getSiSoHienTai() : 0;
        int daDiemDanh = records.size();

        String nguoiKhoaUsername = buoi.getNguoiKhoa() != null ? buoi.getNguoiKhoa().getUsername() : null;
        String nguoiTaoUsername = buoi.getNguoiTao() != null ? buoi.getNguoiTao().getUsername() : null;
        String nguoiCapNhatUsername = buoi.getNguoiCapNhat() != null ? buoi.getNguoiCapNhat().getUsername() : null;

        return new AttendanceDtos.BuoiHocDto(
                buoi.getId(),
                lop != null ? lop.getId() : null,
                lop != null ? lop.getMa() : "",
                (lop != null && lop.getMonHoc() != null) ? lop.getMonHoc().getTen() : "",
                buoi.getNgayHoc(),
                buoi.getCaHoc(),
                buoi.getPhongHoc(),
                buoi.getTietBatDau(),
                buoi.getSoTiet(),
                buoi.getNoiDung(),
                buoi.isDaKhoa(),
                buoi.getThoiGianKhoa(),
                nguoiKhoaUsername,
                nguoiTaoUsername,
                buoi.getThoiGianTao(),
                nguoiCapNhatUsername,
                buoi.getThoiGianCapNhat(),
                tongSinhVien,
                daDiemDanh,
                coMat,
                vang,
                muon,
                coPhep
        );
    }
}
