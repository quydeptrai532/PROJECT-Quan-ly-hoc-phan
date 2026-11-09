package com.edu.registration.service;

import com.edu.registration.config.AttendanceProperties;
import com.edu.registration.dto.AttendanceAlertDtos;
import com.edu.registration.entity.ClassSession;
import com.edu.registration.entity.AttendanceAlert;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.AttendanceRecord;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.AlertThresholdType;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.ClassAbsenceNotice;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.AlertStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.AttendanceStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.ClassSessionRepository;
import com.edu.registration.repository.AttendanceAlertRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.AttendanceRecordRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.ClassAbsenceNoticeRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dịch vụ tính toán, cập nhật và quản lý cảnh báo chuyên cần quá ngưỡng (QLSV-71 / TASK-LC-06).
 */
@Service
@Transactional
public class AttendanceAlertService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAlertService.class);

    private final CourseClassRepository lopHocPhanRepository;
    private final ClassSessionRepository buoiHocRepository;
    private final AttendanceRecordRepository diemDanhRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final ClassAbsenceNoticeRepository thongBaoNghiRepository;
    private final AttendanceAlertRepository canhBaoDiemDanhRepository;
    private final LecturerRepository giangVienRepository;
    private final StudentRepository sinhVienRepository;
    private final CurrentUser currentUser;
    private final AttendanceProperties attendanceProperties;
    private final NotificationService thongBaoService;
    private final AuditLogService auditLogService;

    @Autowired
    public AttendanceAlertService(CourseClassRepository lopHocPhanRepository,
                                  ClassSessionRepository buoiHocRepository,
                                  AttendanceRecordRepository diemDanhRepository,
                                  CourseRegistrationRepository dangKyHocPhanRepository,
                                  ClassAbsenceNoticeRepository thongBaoNghiRepository,
                                  AttendanceAlertRepository canhBaoDiemDanhRepository,
                                  LecturerRepository giangVienRepository,
                                  StudentRepository sinhVienRepository,
                                  CurrentUser currentUser,
                                  AttendanceProperties attendanceProperties,
                                  NotificationService thongBaoService,
                                  AuditLogService auditLogService) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.buoiHocRepository = buoiHocRepository;
        this.diemDanhRepository = diemDanhRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.canhBaoDiemDanhRepository = canhBaoDiemDanhRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.currentUser = currentUser;
        this.attendanceProperties = attendanceProperties;
        this.thongBaoService = thongBaoService;
        this.auditLogService = auditLogService;
    }

    /**
     * Tính toán và đồng bộ lại cảnh báo vắng cho tất cả sinh viên trong lớp học phần.
     * Tính toán trực tiếp từ dữ liệu điểm danh và buổi học chuẩn, không dùng bộ đếm riêng rẽ.
     * Loại trừ các buổi chưa diễn ra hoặc buổi bị hủy theo ClassAbsenceNotice.
     */
    public int kiemTraVaCapNhatCanhBaoChoLop(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findById(lopHocPhanId).orElse(null);
        if (lop == null) {
            return 0;
        }

        // 1. Xác định các buổi học hợp lệ (đã hoặc đang diễn ra và không bị hủy)
        LocalDate today = LocalDate.now();
        List<ClassSession> allSessions = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        List<ClassAbsenceNotice> cancellations = thongBaoNghiRepository.findByLopHocPhanId(lopHocPhanId);
        Set<LocalDate> cancelledDates = cancellations.stream()
                .map(ClassAbsenceNotice::getNgayNghi)
                .collect(Collectors.toSet());

        List<ClassSession> validSessions = allSessions.stream()
                .filter(b -> !b.getNgayHoc().isAfter(today) && !cancelledDates.contains(b.getNgayHoc()))
                .toList();
        Set<Long> validSessionIds = validSessions.stream()
                .map(ClassSession::getId)
                .collect(Collectors.toSet());
        int tongSoBuoiHopLe = validSessions.size();

        // 2. Xác định cấu hình ngưỡng hiệu lực
        Integer effectiveNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double effectiveNguongTyLe = lop.getNguongTyLeVang();
        boolean hasClassConfig = effectiveNguongSoBuoi != null || effectiveNguongTyLe != null;

        if (!hasClassConfig && attendanceProperties.isCanhBaoKichHoat()) {
            effectiveNguongSoBuoi = attendanceProperties.getNguongSoBuoiVang();
            effectiveNguongTyLe = attendanceProperties.getNguongTyLeVang();
        }

        boolean alertEnabled = (effectiveNguongSoBuoi != null || effectiveNguongTyLe != null);

        // 3. Lấy danh sách sinh viên đăng ký hiệu lực
        List<CourseRegistration> dangKys = dangKyHocPhanRepository.findByLopHocPhan(lop.getId(), RegistrationStatus.DA_DANG_KY);
        List<AttendanceRecord> allDiemDanhs = diemDanhRepository.findByBuoiHocLopHocPhanId(lop.getId());
        Map<Long, List<AttendanceRecord>> studentRecordsMap = allDiemDanhs.stream()
                .filter(dd -> validSessionIds.contains(dd.getBuoiHoc().getId()))
                .collect(Collectors.groupingBy(dd -> dd.getSinhVien().getId()));

        int evaluatedCount = 0;

        for (CourseRegistration dk : dangKys) {
            Student sv = dk.getSinhVien();
            if (sv == null || sv.getUser() == null || !sv.getUser().isTrangThai()) {
                continue;
            }

            List<AttendanceRecord> records = studentRecordsMap.getOrDefault(sv.getId(), Collections.emptyList());

            int vang = 0;
            int muon = 0;
            int coPhep = 0;
            for (AttendanceRecord r : records) {
                if (r.getTrangThai() == null) continue;
                switch (r.getTrangThai()) {
                    case VANG -> vang++;
                    case MUON -> muon++;
                    case CO_PHEP -> coPhep++;
                    default -> {}
                }
            }

            int quyDoiVangTuMuon = attendanceProperties.getSoLanMuonThanhMotVang() > 0
                    ? (muon / attendanceProperties.getSoLanMuonThanhMotVang()) : 0;
            int soBuoiVang = vang + quyDoiVangTuMuon + (attendanceProperties.isCoPhepTinhVang() ? coPhep : 0);

            double tyLeVang = tongSoBuoiHopLe > 0
                    ? (Math.round(((double) soBuoiVang / tongSoBuoiHopLe) * 1000.0) / 10.0)
                    : 0.0;

            // Đánh giá ngưỡng số buổi vắng
            if (effectiveNguongSoBuoi != null) {
                boolean breach = soBuoiVang >= effectiveNguongSoBuoi;
                evaluateAlertRow(lop, sv, AlertThresholdType.SO_BUOI_VANG, (double) effectiveNguongSoBuoi,
                        soBuoiVang, tongSoBuoiHopLe, tyLeVang, breach);
                evaluatedCount++;
            } else {
                // Nếu ngưỡng số buổi không còn áp dụng, giải tỏa cảnh báo cũ nếu có
                resolveOldAlertIfPresent(lop, sv, AlertThresholdType.SO_BUOI_VANG, soBuoiVang, tongSoBuoiHopLe, tyLeVang);
            }

            // Đánh giá ngưỡng tỷ lệ vắng
            if (effectiveNguongTyLe != null) {
                boolean breach = tongSoBuoiHopLe > 0 && tyLeVang >= effectiveNguongTyLe;
                evaluateAlertRow(lop, sv, AlertThresholdType.TY_LE_VANG, effectiveNguongTyLe,
                        soBuoiVang, tongSoBuoiHopLe, tyLeVang, breach);
                evaluatedCount++;
            } else {
                // Nếu ngưỡng tỷ lệ không còn áp dụng, giải tỏa cảnh báo cũ nếu có
                resolveOldAlertIfPresent(lop, sv, AlertThresholdType.TY_LE_VANG, soBuoiVang, tongSoBuoiHopLe, tyLeVang);
            }
        }

        return evaluatedCount;
    }

    /**
     * Lấy báo cáo và danh sách cảnh báo của lớp học phần.
     * Giảng viên chỉ được xem của lớp mình phụ trách (kiểm tra quyền tại backend); Quản trị viên xem tất cả.
     */
    public AttendanceAlertDtos.BaoCaoCanhBaoLopDto layBaoCaoCanhBaoLop(Long lopHocPhanId) {
        User actor = currentUser.get();
        CourseClass lop = kiemTraQuyenGiangVienVoiLop(lopHocPhanId, actor);

        // Đồng bộ lại cảnh báo mới nhất
        kiemTraVaCapNhatCanhBaoChoLop(lopHocPhanId);

        LocalDate today = LocalDate.now();
        List<ClassSession> allSessions = buoiHocRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lopHocPhanId);
        List<ClassAbsenceNotice> cancellations = thongBaoNghiRepository.findByLopHocPhanId(lopHocPhanId);
        Set<LocalDate> cancelledDates = cancellations.stream()
                .map(ClassAbsenceNotice::getNgayNghi)
                .collect(Collectors.toSet());
        int tongSoBuoiHopLe = (int) allSessions.stream()
                .filter(b -> !b.getNgayHoc().isAfter(today) && !cancelledDates.contains(b.getNgayHoc()))
                .count();

        Integer effectiveNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double effectiveNguongTyLe = lop.getNguongTyLeVang();
        String nguonNguong = "CHUA_CAU_HINH";
        boolean alertEnabled = false;

        if (effectiveNguongSoBuoi != null || effectiveNguongTyLe != null) {
            nguonNguong = "LOP_HOC_PHAN";
            alertEnabled = true;
        } else if (attendanceProperties.isCanhBaoKichHoat() &&
                (attendanceProperties.getNguongSoBuoiVang() != null || attendanceProperties.getNguongTyLeVang() != null)) {
            effectiveNguongSoBuoi = attendanceProperties.getNguongSoBuoiVang();
            effectiveNguongTyLe = attendanceProperties.getNguongTyLeVang();
            nguonNguong = "HE_THONG";
            alertEnabled = true;
        }

        List<AttendanceAlert> alerts = canhBaoDiemDanhRepository
                .findByLopHocPhanIdOrderByThoiGianCapNhatDescThoiGianTaoDesc(lopHocPhanId);

        List<AttendanceAlertDtos.CanhBaoDiemDanhDto> alertDtos = alerts.stream()
                .map(this::mapToDto)
                .toList();

        long activeCount = alerts.stream()
                .filter(a -> a.getTrangThai() == AlertStatus.DANG_CANH_BAO)
                .count();

        return new AttendanceAlertDtos.BaoCaoCanhBaoLopDto(
                lop.getId(),
                lop.getMa(),
                lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "",
                effectiveNguongSoBuoi,
                effectiveNguongTyLe,
                nguonNguong,
                alertEnabled,
                attendanceProperties.getGhiChuNguong(),
                tongSoBuoiHopLe,
                (int) activeCount,
                alertDtos
        );
    }

    /**
     * Giảng viên hoặc Quản trị thiết lập cấu hình ngưỡng cảnh báo cho lớp học phần.
     */
    public AttendanceAlertDtos.BaoCaoCanhBaoLopDto capNhatCauHinhNguongLop(
            Long lopHocPhanId, AttendanceAlertDtos.CauHinhNguongLopRequest request) {
        User actor = currentUser.get();
        CourseClass lop = kiemTraQuyenGiangVienVoiLop(lopHocPhanId, actor);

        Integer oldNguongSoBuoi = lop.getNguongSoBuoiVang();
        Double oldNguongTyLe = lop.getNguongTyLeVang();

        lop.setNguongSoBuoiVang(request.nguongSoBuoiVang());
        lop.setNguongTyLeVang(request.nguongTyLeVang());
        lopHocPhanRepository.save(lop);

        auditLogService.execute("ATTENDANCE_THRESHOLD_CONFIG_UPDATED", "LOP_HOC_PHAN", lop.getId(), ctx -> {
            ctx.changes(
                    Map.of(
                            "lopHocPhanId", lop.getId(),
                            "maLop", lop.getMa(),
                            "nguongSoBuoiVang", oldNguongSoBuoi != null ? oldNguongSoBuoi : 0,
                            "nguongTyLeVang", oldNguongTyLe != null ? oldNguongTyLe : 0.0
                    ),
                    Map.of(
                            "lopHocPhanId", lop.getId(),
                            "maLop", lop.getMa(),
                            "nguongSoBuoiVang", request.nguongSoBuoiVang() != null ? request.nguongSoBuoiVang() : 0,
                            "nguongTyLeVang", request.nguongTyLeVang() != null ? request.nguongTyLeVang() : 0.0
                    )
            );
            return null;
        });

        kiemTraVaCapNhatCanhBaoChoLop(lopHocPhanId);
        return layBaoCaoCanhBaoLop(lopHocPhanId);
    }

    /**
     * Danh sách cảnh báo chuyên cần thuộc các lớp do giảng viên phụ trách (hoặc tất cả cho Quản trị).
     */
    @Transactional(readOnly = true)
    public List<AttendanceAlertDtos.CanhBaoDiemDanhDto> danhSachCanhBaoCuaGiangVien(AlertStatus trangThai) {
        User actor = currentUser.get();
        if (actor.getRole() == Role.QUAN_TRI) {
            List<AttendanceAlert> list = (trangThai == null)
                    ? canhBaoDiemDanhRepository.findAll()
                    : canhBaoDiemDanhRepository.findAll().stream().filter(a -> a.getTrangThai() == trangThai).toList();
            return list.stream().map(this::mapToDto).toList();
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienRepository.findByUser(actor)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            return canhBaoDiemDanhRepository.findByGiangVienIdAndTrangThai(gv.getId(), trangThai)
                    .stream().map(this::mapToDto).toList();
        }

        throw ApiException.forbidden("Bạn không có quyền thực hiện thao tác này");
    }

    /**
     * Danh sách cảnh báo chuyên cần của sinh viên đang đăng nhập.
     */
    @Transactional(readOnly = true)
    public List<AttendanceAlertDtos.CanhBaoDiemDanhDto> danhSachCanhBaoCuaSinhVien(AlertStatus trangThai) {
        User actor = currentUser.get();
        Student sv = sinhVienRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
        return canhBaoDiemDanhRepository.findBySinhVienIdAndTrangThai(sv.getId(), trangThai)
                .stream().map(this::mapToDto).toList();
    }

    private void evaluateAlertRow(CourseClass lop, Student sv,
                                  AlertThresholdType loaiNguong, double giaTriNguong,
                                  int soBuoiVang, int tongBuoiHopLe, double tyLeVang,
                                  boolean breach) {
        Optional<AttendanceAlert> existingOpt = canhBaoDiemDanhRepository
                .findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(sv.getId(), lop.getId(), loaiNguong);

        String subjectName = lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "";
        String thresholdDesc = loaiNguong == AlertThresholdType.SO_BUOI_VANG
                ? ((int) giaTriNguong + " buổi")
                : (giaTriNguong + "%");

        LocalDateTime now = LocalDateTime.now();

        if (breach) {
            String msg = "Sinh viên " + sv.getHoTen() + " (" + sv.getMa() + ")"
                    + " đã vắng " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ ("
                    + String.format(Locale.US, "%.1f", tyLeVang) + "%),"
                    + " đạt/vượt ngưỡng " + thresholdDesc + " của lớp " + lop.getMa() + " (" + subjectName + ").";
            String idempKey = "attendance-alert:" + lop.getId() + ":" + sv.getId() + ":" + loaiNguong.name()
                    + ":" + soBuoiVang + "/" + tongBuoiHopLe + ":ALERT";

            if (existingOpt.isEmpty()) {
                AttendanceAlert alert = new AttendanceAlert();
                alert.setLopHocPhan(lop);
                alert.setSinhVien(sv);
                alert.setLoaiNguong(loaiNguong);
                alert.setGiaTriNguong(giaTriNguong);
                alert.setSoBuoiVang(soBuoiVang);
                alert.setTongBuoiHopLe(tongBuoiHopLe);
                alert.setTyLeVang(tyLeVang);
                alert.setTrangThai(AlertStatus.DANG_CANH_BAO);
                alert.setThongDiep(msg);
                alert.setIdempotencyKey(idempKey);
                alert.setThoiGianTao(now);
                alert.setThoiGianCapNhat(now);
                AttendanceAlert saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                auditLogService.execute("ATTENDANCE_ALERT_TRIGGERED", "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                    ctx.changes(Map.of(), Map.of(
                            "canhBaoId", saved.getId(),
                            "lopHocPhanId", lop.getId(),
                            "sinhVienId", sv.getId(),
                            "soBuoiVang", soBuoiVang,
                            "tongBuoiHopLe", tongBuoiHopLe,
                            "tyLeVang", tyLeVang,
                            "loaiNguong", loaiNguong.name(),
                            "giaTriNguong", giaTriNguong,
                            "trangThaiCanhBao", AlertStatus.DANG_CANH_BAO.name()
                    ));
                    return null;
                });

                if (thongBaoService != null) {
                    thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                            soBuoiVang, tongBuoiHopLe, tyLeVang, false);
                }
            } else {
                AttendanceAlert alert = existingOpt.get();
                boolean wasActive = alert.getTrangThai() == AlertStatus.DANG_CANH_BAO;
                boolean numbersChanged = alert.getSoBuoiVang() != soBuoiVang || alert.getTongBuoiHopLe() != tongBuoiHopLe
                        || Math.abs(alert.getTyLeVang() - tyLeVang) > 0.01 || Math.abs(alert.getGiaTriNguong() - giaTriNguong) > 0.01;

                if (!wasActive || numbersChanged) {
                    alert.setGiaTriNguong(giaTriNguong);
                    alert.setSoBuoiVang(soBuoiVang);
                    alert.setTongBuoiHopLe(tongBuoiHopLe);
                    alert.setTyLeVang(tyLeVang);
                    alert.setTrangThai(AlertStatus.DANG_CANH_BAO);
                    alert.setThongDiep(msg);
                    alert.setIdempotencyKey(idempKey);
                    alert.setThoiGianCapNhat(now);
                    AttendanceAlert saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                    String action = !wasActive ? "ATTENDANCE_ALERT_TRIGGERED" : "ATTENDANCE_ALERT_UPDATED";
                    auditLogService.execute(action, "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                        ctx.changes(Map.of(), Map.of(
                                "canhBaoId", saved.getId(),
                                "lopHocPhanId", lop.getId(),
                                "sinhVienId", sv.getId(),
                                "soBuoiVang", soBuoiVang,
                                "tongBuoiHopLe", tongBuoiHopLe,
                                "tyLeVang", tyLeVang,
                                "loaiNguong", loaiNguong.name(),
                                "giaTriNguong", giaTriNguong,
                                "trangThaiCanhBao", AlertStatus.DANG_CANH_BAO.name()
                        ));
                        return null;
                    });

                    if (thongBaoService != null) {
                        thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                                soBuoiVang, tongBuoiHopLe, tyLeVang, false);
                    }
                }
            }
        } else {
            // Dưới ngưỡng: nếu đang ở DANG_CANH_BAO thì giải tỏa
            if (existingOpt.isPresent()) {
                AttendanceAlert alert = existingOpt.get();
                if (alert.getTrangThai() == AlertStatus.DANG_CANH_BAO) {
                    String msg = "Cảnh báo chuyên cần đã được giải tỏa: Sinh viên " + sv.getHoTen() + " (" + sv.getMa() + ")"
                            + " hiện có số buổi vắng là " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ ("
                            + String.format(Locale.US, "%.1f", tyLeVang) + "%),"
                            + " đã trở về dưới ngưỡng quy định của lớp " + lop.getMa() + " (" + subjectName + ").";
                    String idempKey = "attendance-alert:" + lop.getId() + ":" + sv.getId() + ":" + loaiNguong.name()
                            + ":" + soBuoiVang + "/" + tongBuoiHopLe + ":RESOLVED";

                    alert.setSoBuoiVang(soBuoiVang);
                    alert.setTongBuoiHopLe(tongBuoiHopLe);
                    alert.setTyLeVang(tyLeVang);
                    alert.setTrangThai(AlertStatus.DA_GIAI_TOA);
                    alert.setThongDiep(msg);
                    alert.setIdempotencyKey(idempKey);
                    alert.setThoiGianCapNhat(now);
                    AttendanceAlert saved = canhBaoDiemDanhRepository.saveAndFlush(alert);

                    auditLogService.execute("ATTENDANCE_ALERT_RESOLVED", "ATTENDANCE_ALERT", saved.getId(), ctx -> {
                        ctx.changes(Map.of("trangThaiCanhBao", AlertStatus.DANG_CANH_BAO.name()), Map.of(
                                "canhBaoId", saved.getId(),
                                "lopHocPhanId", lop.getId(),
                                "sinhVienId", sv.getId(),
                                "soBuoiVang", soBuoiVang,
                                "tongBuoiHopLe", tongBuoiHopLe,
                                "tyLeVang", tyLeVang,
                                "loaiNguong", loaiNguong.name(),
                                "giaTriNguong", giaTriNguong,
                                "trangThaiCanhBao", AlertStatus.DA_GIAI_TOA.name()
                        ));
                        return null;
                    });

                    if (thongBaoService != null) {
                        thongBaoService.attendanceThresholdAlert(lop, sv, loaiNguong.name(), giaTriNguong,
                                soBuoiVang, tongBuoiHopLe, tyLeVang, true);
                    }
                }
            }
        }
    }

    private void resolveOldAlertIfPresent(CourseClass lop, Student sv, AlertThresholdType loaiNguong,
                                         int soBuoiVang, int tongBuoiHopLe, double tyLeVang) {
        canhBaoDiemDanhRepository.findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(sv.getId(), lop.getId(), loaiNguong)
                .ifPresent(alert -> {
                    if (alert.getTrangThai() == AlertStatus.DANG_CANH_BAO) {
                        alert.setTrangThai(AlertStatus.DA_GIAI_TOA);
                        alert.setSoBuoiVang(soBuoiVang);
                        alert.setTongBuoiHopLe(tongBuoiHopLe);
                        alert.setTyLeVang(tyLeVang);
                        alert.setThongDiep("Cảnh báo đã được giải tỏa do ngưỡng không còn áp dụng.");
                        alert.setThoiGianCapNhat(LocalDateTime.now());
                        canhBaoDiemDanhRepository.save(alert);
                    }
                });
    }

    public CourseClass kiemTraQuyenGiangVienVoiLop(Long lopHocPhanId, User actor) {
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

    private AttendanceAlertDtos.CanhBaoDiemDanhDto mapToDto(AttendanceAlert a) {
        CourseClass lop = a.getLopHocPhan();
        Student sv = a.getSinhVien();
        return new AttendanceAlertDtos.CanhBaoDiemDanhDto(
                a.getId(),
                lop != null ? lop.getId() : null,
                lop != null ? lop.getMa() : "",
                (lop != null && lop.getMonHoc() != null) ? lop.getMonHoc().getTen() : "",
                sv != null ? sv.getId() : null,
                sv != null ? sv.getMa() : "",
                sv != null ? sv.getHoTen() : "",
                (sv != null && sv.getLopHanhChinh() != null) ? sv.getLopHanhChinh().getTen() : "",
                a.getLoaiNguong(),
                a.getGiaTriNguong(),
                a.getSoBuoiVang(),
                a.getTongBuoiHopLe(),
                a.getTyLeVang(),
                a.getTrangThai(),
                a.getThongDiep(),
                a.getThoiGianTao(),
                a.getThoiGianCapNhat()
        );
    }
}
