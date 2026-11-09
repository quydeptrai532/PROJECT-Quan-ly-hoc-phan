package com.edu.registration.service;

import com.edu.registration.config.EarlyWarningProperties;
import com.edu.registration.dto.AcademicAlertDtos.*;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import com.edu.registration.security.CurrentUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dịch vụ Quản lý & Tính toán Cảnh báo Học tập Sớm theo Quy tắc (QLSV-97 / TASK-OP-10).
 *
 * <p>Triển khai các quy tắc minh bạch, giải thích được:
 * 1. Điểm quá trình thấp hơn ngưỡng.
 * 2. Số buổi hoặc tỷ lệ vắng vượt ngưỡng.
 * 3. GPA giảm qua các học kỳ liên tiếp.
 * 4. Thiếu tín chỉ so với tiến độ học tập.
 *
 * <p>Nguyên tắc thiết kế an toàn:
 * - KHÔNG dùng mô hình dự đoán hộp đen.
 * - KHÔNG tự ý thay đổi trạng thái học vụ sinh viên (Student.trangThaiHocTap).
 * - KHÔNG tự ý khóa tài khoản (User.trangThai).
 * - KHÔNG tự ý sửa dữ liệu điểm hoặc đăng ký học phần.
 * - Chống trùng lặp (Idempotent) thông qua khóa tổng hợp duy nhất.
 * - Cập nhật và giải tỏa tự động có lưu vết khi dữ liệu nguồn thay đổi.</p>
 */
@Service
@Transactional
public class EarlyWarningService {

    private static final Logger log = LoggerFactory.getLogger(EarlyWarningService.class);

    private final AcademicAlertRepository alertRepository;
    private final StudentRepository studentRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final ClassSessionRepository sessionRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final ClassAbsenceNoticeRepository absenceNoticeRepository;
    private final CourseClassRepository courseClassRepository;
    private final LecturerRepository lecturerRepository;
    private final CurrentUser currentUser;
    private final EarlyWarningProperties properties;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Autowired
    public EarlyWarningService(
            AcademicAlertRepository alertRepository,
            StudentRepository studentRepository,
            CourseRegistrationRepository registrationRepository,
            GradeRepository gradeRepository,
            ClassSessionRepository sessionRepository,
            AttendanceRecordRepository attendanceRepository,
            ClassAbsenceNoticeRepository absenceNoticeRepository,
            CourseClassRepository courseClassRepository,
            LecturerRepository lecturerRepository,
            CurrentUser currentUser,
            EarlyWarningProperties properties,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.alertRepository = alertRepository;
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.gradeRepository = gradeRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.absenceNoticeRepository = absenceNoticeRepository;
        this.courseClassRepository = courseClassRepository;
        this.lecturerRepository = lecturerRepository;
        this.currentUser = currentUser;
        this.properties = properties;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * Lấy cấu hình tập trung, danh sách quy tắc và các giá trị chờ Hội đồng Đào tạo xác nhận.
     */
    @Transactional(readOnly = true)
    public EarlyWarningConfigDto layCauHinh() {
        List<AlertRuleInfo> dsLuat = new ArrayList<>();

        var rDiem = properties.getDiemQuaTrinh();
        dsLuat.add(new AlertRuleInfo(
                AcademicAlertRuleType.DIEM_QUA_TRINH,
                "RULE_MIDTERM_LOW",
                "Điểm quá trình thấp hơn ngưỡng",
                "Phát hiện sinh viên có điểm quá trình dưới ngưỡng tối thiểu yêu cầu",
                rDiem.getMucDo(),
                rDiem.isEnabled(),
                rDiem.isChoPhepSinhVienXem(),
                "diemQuaTrinh < " + rDiem.getNguongDiem(),
                "Bảng điểm quá trình (Grade.diemQuaTrinh)",
                rDiem.getGhiChu(),
                properties.isOfficialRegulation()
        ));

        var rDiemDanh = properties.getDiemDanh();
        dsLuat.add(new AlertRuleInfo(
                AcademicAlertRuleType.DIEM_DANH,
                "RULE_ATTENDANCE_EXCESSIVE",
                "Số buổi hoặc tỷ lệ vắng vượt ngưỡng",
                "Phát hiện sinh viên có nguy cơ cấm thi do số buổi vắng hoặc tỷ lệ vắng vượt mức cho phép",
                rDiemDanh.getMucDo(),
                rDiemDanh.isEnabled(),
                rDiemDanh.isChoPhepSinhVienXem(),
                "soBuoiVang >= " + rDiemDanh.getNguongSoBuoiVang() + " || tyLeVang >= " + rDiemDanh.getNguongTyLeVang() + "%",
                "Bảng điểm danh & buổi học hợp lệ (AttendanceRecord & ClassSession)",
                rDiemDanh.getGhiChu(),
                properties.isOfficialRegulation()
        ));

        var rGpa = properties.getGpaGiam();
        dsLuat.add(new AlertRuleInfo(
                AcademicAlertRuleType.GPA_GIAM,
                "RULE_GPA_DECLINE",
                "GPA giảm qua các học kỳ liên tiếp",
                "Phát hiện xu hướng sa sút học lực khi điểm trung bình học kỳ giảm liên tiếp qua các kỳ",
                rGpa.getMucDo(),
                rGpa.isEnabled(),
                rGpa.isChoPhepSinhVienXem(),
                "(gpaKyTruoc - gpaKySau) >= " + rGpa.getMucGiamToiThieu() + " (qua " + rGpa.getSoHocKyLienTiep() + " học kỳ)",
                "Bảng điểm tổng kết học phần đã công bố (Grade & Semester)",
                rGpa.getGhiChu(),
                properties.isOfficialRegulation()
        ));

        var rTinChi = properties.getTienDoTinChi();
        dsLuat.add(new AlertRuleInfo(
                AcademicAlertRuleType.TIEN_DO_TIN_CHI,
                "RULE_CREDIT_DEFICIT",
                "Thiếu tín chỉ so với tiến độ học tập",
                "Phát hiện sinh viên bị chậm tiến độ tích lũy tín chỉ so với khung chương trình đào tạo chuẩn",
                rTinChi.getMucDo(),
                rTinChi.isEnabled(),
                rTinChi.isChoPhepSinhVienXem(),
                "tinChiThieu >= " + rTinChi.getNguongTinChiThieu() + " || tyLeHoanThanh < " + rTinChi.getNguongTyLeHoanThanh() + "%",
                "Chương trình đào tạo (Curriculum) & Số tín chỉ tích lũy (tinChiTichLuy)",
                rTinChi.getGhiChu(),
                properties.isOfficialRegulation()
        ));

        return new EarlyWarningConfigDto(
                properties.isEnabled(),
                properties.isOfficialRegulation(),
                properties.getPolicyNote(),
                dsLuat,
                properties.layDanhSachGiaTriChoXacNhan()
        );
    }

    /**
     * Quét toàn bộ hệ thống để phát hiện và cập nhật các cảnh báo học tập sớm.
     */
    public ScanAlertsResultDto quetToanBoCanhBao() {
        long start = System.currentTimeMillis();
        List<Student> students = studentRepository.findAll();
        int totalScanned = 0;
        int created = 0;
        int updated = 0;
        int autoResolved = 0;

        for (Student s : students) {
            totalScanned++;
            ScanSummary summary = quyetToanCanhBaoChoSinhVien(s);
            created += summary.created;
            updated += summary.updated;
            autoResolved += summary.autoResolved;
        }

        long duration = System.currentTimeMillis() - start;
        String msg = String.format("Quét hoàn tất %d sinh viên trong %d ms: %d cảnh báo mới, %d cập nhật, %d tự động giải tỏa.",
                totalScanned, duration, created, updated, autoResolved);

        log.info(msg);
        return new ScanAlertsResultDto(totalScanned, created, updated, autoResolved, duration, msg);
    }

    public static class ScanSummary {
        int created = 0;
        int updated = 0;
        int autoResolved = 0;
    }

    /**
     * Đánh giá và cập nhật cảnh báo cho một sinh viên cụ thể.
     */
    public ScanSummary quyetToanCanhBaoChoSinhVien(Student student) {
        ScanSummary summary = new ScanSummary();
        if (student == null) return summary;

        // 1. Luật Điểm quá trình
        if (properties.getDiemQuaTrinh().isEnabled()) {
            danhGiaLuatDiemQuaTrinh(student, summary);
        }

        // 2. Luật Điểm danh
        if (properties.getDiemDanh().isEnabled()) {
            danhGiaLuatDiemDanh(student, summary);
        }

        // 3. Luật GPA giảm
        if (properties.getGpaGiam().isEnabled()) {
            danhGiaLuatGpaGiam(student, summary);
        }

        // 4. Luật Tiến độ tín chỉ
        if (properties.getTienDoTinChi().isEnabled()) {
            danhGiaLuatTienDoTinChi(student, summary);
        }

        return summary;
    }

    // =========================================================================
    // LUẬT 1: ĐIỂM QUÁ TRÌNH THẤP HƠN NGƯỠNG
    // =========================================================================
    private void danhGiaLuatDiemQuaTrinh(Student student, ScanSummary summary) {
        var cfg = properties.getDiemQuaTrinh();
        double nguongDiem = cfg.getNguongDiem();
        AcademicAlertSeverity mucDo = AcademicAlertSeverity.valueOf(cfg.getMucDo());
        boolean svXem = cfg.isChoPhepSinhVienXem();

        List<CourseRegistration> activeRegs = registrationRepository.findAllActiveOfStudent(student.getId(), RegistrationStatus.DA_DANG_KY);
        if (activeRegs.isEmpty()) return;

        List<Grade> grades = gradeRepository.findByDangKyIdIn(activeRegs.stream().map(CourseRegistration::getId).toList());
        Map<Long, Grade> gradeMap = grades.stream().collect(Collectors.toMap(g -> g.getDangKy().getId(), g -> g));

        for (CourseRegistration reg : activeRegs) {
            Grade grade = gradeMap.get(reg.getId());
            CourseClass lop = reg.getLopHocPhan();
            String nguCanh = "lop:" + lop.getId();
            String maLuat = "RULE_MIDTERM_LOW";
            String fingerprint = taoKhoaChongTrung(student.getId(), maLuat, nguCanh);

            boolean hasMidterm = grade != null && grade.getDiemQuaTrinh() != null;
            boolean viPham = hasMidterm && grade.getDiemQuaTrinh() < nguongDiem;

            if (viPham) {
                double score = grade.getDiemQuaTrinh();
                String tenMon = lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "Môn học";
                String maMon = lop.getMonHoc() != null ? lop.getMonHoc().getMa() : "";
                String lyDo = String.format("Điểm quá trình môn %s (%s) là %.2f/10.0, thấp hơn ngưỡng cảnh báo %.2f/10.0.",
                        tenMon, lop.getMa(), score, nguongDiem);

                String congThuc = String.format(
                        "Công thức kiểm tra: [diemQuaTrinh < nguongDiem]. Điểm quá trình thực tế: %.2f; Ngưỡng quy định: %.2f. " +
                        "Nguy cơ: Điểm quá trình chiếm tỷ trọng lớn trong điểm tổng kết, mức điểm hiện tại có thể dẫn đến không đủ điều kiện dự thi hoặc trượt học phần.",
                        score, nguongDiem);

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("sinhVienId", student.getId());
                evidence.put("maSinhVien", student.getMa());
                evidence.put("hoTenSinhVien", student.getHoTen());
                evidence.put("lopHocPhanId", lop.getId());
                evidence.put("maLop", lop.getMa());
                evidence.put("maMon", maMon);
                evidence.put("tenMon", tenMon);
                evidence.put("diemQuaTrinh", score);
                evidence.put("nguongDiem", nguongDiem);
                evidence.put("thangDiem", 10.0);
                evidence.put("thoiDiemDanhGia", LocalDateTime.now().toString());

                ghiNhanHoacCapNhatCanhBao(student, AcademicAlertRuleType.DIEM_QUA_TRINH, maLuat,
                        "Cảnh báo điểm quá trình thấp", mucDo, lyDo, congThuc, evidence,
                        nguCanh, fingerprint, lop.getId(), svXem, summary);
            } else {
                // Không vi phạm: nếu có cảnh báo đang mở thì tự động giải tỏa
                tuDongGiaiToaNeuTonTai(fingerprint,
                        "Điểm quá trình đã được cải thiện (hoặc đạt chuẩn >= " + nguongDiem + "), cảnh báo tự động giải tỏa.",
                        summary);
            }
        }
    }

    // =========================================================================
    // LUẬT 2: SỐ BUỔI HOẶC TỶ LỆ VẮNG VƯỢT NGƯỠNG
    // =========================================================================
    private void danhGiaLuatDiemDanh(Student student, ScanSummary summary) {
        var cfg = properties.getDiemDanh();
        int nguongSoBuoi = cfg.getNguongSoBuoiVang();
        double nguongTyLe = cfg.getNguongTyLeVang();
        AcademicAlertSeverity mucDo = AcademicAlertSeverity.valueOf(cfg.getMucDo());
        boolean svXem = cfg.isChoPhepSinhVienXem();

        List<CourseRegistration> activeRegs = registrationRepository.findAllActiveOfStudent(student.getId(), RegistrationStatus.DA_DANG_KY);
        LocalDate today = LocalDate.now();

        for (CourseRegistration reg : activeRegs) {
            CourseClass lop = reg.getLopHocPhan();
            String nguCanh = "lop:" + lop.getId();
            String maLuat = "RULE_ATTENDANCE_EXCESSIVE";
            String fingerprint = taoKhoaChongTrung(student.getId(), maLuat, nguCanh);

            // Tìm tất cả buổi học đã diễn ra và không bị hủy
            List<ClassSession> allSessions = sessionRepository.findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(lop.getId());
            Set<LocalDate> ngayNghi = absenceNoticeRepository.findByLopHocPhanId(lop.getId())
                    .stream()
                    .map(ClassAbsenceNotice::getNgayNghi)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<Long> validSessionIds = allSessions.stream()
                    .filter(s -> !s.getNgayHoc().isAfter(today) && !ngayNghi.contains(s.getNgayHoc()))
                    .map(ClassSession::getId)
                    .collect(Collectors.toSet());

            int tongBuoiHopLe = validSessionIds.size();
            if (tongBuoiHopLe == 0) {
                continue;
            }

            // Đếm số buổi vắng
            List<AttendanceRecord> records = attendanceRepository.findBySinhVienAndLopHocPhan(student.getId(), lop.getId());
            int soBuoiVang = 0;
            for (AttendanceRecord rec : records) {
                if (rec.getBuoiHoc() != null && validSessionIds.contains(rec.getBuoiHoc().getId())) {
                    if (rec.getTrangThai() == AttendanceStatus.VANG) {
                        soBuoiVang++;
                    }
                }
            }

            double tyLeVang = Math.round(((double) soBuoiVang / tongBuoiHopLe) * 10000.0) / 100.0;
            boolean viPham = soBuoiVang >= nguongSoBuoi || (tongBuoiHopLe >= 3 && tyLeVang >= nguongTyLe);

            if (viPham) {
                String tenMon = lop.getMonHoc() != null ? lop.getMonHoc().getTen() : "Môn học";
                String lyDo = String.format("Sinh viên vắng %d/%d buổi hợp lệ (%.2f%%) tại lớp %s, vượt ngưỡng quy định (tối đa %d buổi hoặc %.2f%%).",
                        soBuoiVang, tongBuoiHopLe, tyLeVang, lop.getMa(), nguongSoBuoi, nguongTyLe);

                String congThuc = String.format(
                        "Công thức kiểm tra: [soBuoiVang >= %d HOẶC tyLeVang >= %.2f%%]. " +
                        "Dữ liệu thực tế: vắng %d buổi trên tổng số %d buổi đã diễn ra (tỷ lệ: ( %d / %d ) * 100 = %.2f%%). " +
                        "Căn cứ: Dữ liệu điểm danh chính thức từ các buổi học đã diễn ra không bị hủy.",
                        nguongSoBuoi, nguongTyLe, soBuoiVang, tongBuoiHopLe, soBuoiVang, tongBuoiHopLe, tyLeVang);

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("sinhVienId", student.getId());
                evidence.put("maSinhVien", student.getMa());
                evidence.put("hoTenSinhVien", student.getHoTen());
                evidence.put("lopHocPhanId", lop.getId());
                evidence.put("maLop", lop.getMa());
                evidence.put("tenMon", tenMon);
                evidence.put("soBuoiVang", soBuoiVang);
                evidence.put("tongBuoiHopLe", tongBuoiHopLe);
                evidence.put("tyLeVang", tyLeVang);
                evidence.put("nguongSoBuoiVang", nguongSoBuoi);
                evidence.put("nguongTyLeVang", nguongTyLe);
                evidence.put("thoiDiemDanhGia", LocalDateTime.now().toString());

                ghiNhanHoacCapNhatCanhBao(student, AcademicAlertRuleType.DIEM_DANH, maLuat,
                        "Cảnh báo vắng học vượt ngưỡng", mucDo, lyDo, congThuc, evidence,
                        nguCanh, fingerprint, lop.getId(), svXem, summary);
            } else {
                tuDongGiaiToaNeuTonTai(fingerprint,
                        "Số buổi vắng và tỷ lệ vắng đã trở về mức an toàn dưới ngưỡng cảnh báo.",
                        summary);
            }
        }
    }

    // =========================================================================
    // LUẬT 3: GPA GIẢM QUA CÁC HỌC KỲ LIÊN TIẾP
    // =========================================================================
    private void danhGiaLuatGpaGiam(Student student, ScanSummary summary) {
        var cfg = properties.getGpaGiam();
        double mucGiamToiThieu = cfg.getMucGiamToiThieu();
        AcademicAlertSeverity mucDo = AcademicAlertSeverity.valueOf(cfg.getMucDo());
        boolean svXem = cfg.isChoPhepSinhVienXem();

        List<CourseRegistration> allRegs = registrationRepository.findAllActiveOfStudent(student.getId(), RegistrationStatus.DA_DANG_KY);
        if (allRegs.isEmpty()) return;

        List<Grade> grades = gradeRepository.findByDangKyIdIn(allRegs.stream().map(CourseRegistration::getId).toList());
        Map<Long, Grade> gradeMap = grades.stream().collect(Collectors.toMap(g -> g.getDangKy().getId(), g -> g));

        // Nhóm đăng ký theo học kỳ và chỉ xét các học kỳ có điểm đã công bố
        Map<Semester, List<CourseRegistration>> bySemester = allRegs.stream()
                .filter(r -> r.getLopHocPhan().isBangDiemDaCongBo() && gradeMap.get(r.getId()) != null && gradeMap.get(r.getId()).getDiemTongKet() != null)
                .collect(Collectors.groupingBy(r -> r.getLopHocPhan().getHocKy(), LinkedHashMap::new, Collectors.toList()));

        // Sắp xếp các học kỳ theo thứ tự thời gian
        List<Semester> sortedTerms = bySemester.keySet().stream()
                .sorted(Comparator.comparing(Semester::getId))
                .toList();

        if (sortedTerms.size() < 2) return;

        // Tính GPA hệ 4 từng học kỳ
        Map<Long, Double> gpaTheoKy = new LinkedHashMap<>();
        for (Semester sem : sortedTerms) {
            List<CourseRegistration> termRegs = bySemester.get(sem);
            double tongDiem4 = 0;
            int tinChiCoDiem = 0;
            for (CourseRegistration r : termRegs) {
                Grade g = gradeMap.get(r.getId());
                if (g != null && g.getDiemTongKet() != null) {
                    int tc = r.getLopHocPhan().getMonHoc().getSoTinChi();
                    tinChiCoDiem += tc;
                    tongDiem4 += quyDoiHe4(g.getDiemTongKet()) * tc;
                }
            }
            double gpa = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
            gpaTheoKy.put(sem.getId(), gpa);
        }

        // So sánh từng cặp học kỳ liên tiếp
        for (int i = 0; i < sortedTerms.size() - 1; i++) {
            Semester sem1 = sortedTerms.get(i);
            Semester sem2 = sortedTerms.get(i + 1);

            double gpa1 = gpaTheoKy.get(sem1.getId());
            double gpa2 = gpaTheoKy.get(sem2.getId());
            double doGiam = Math.round((gpa1 - gpa2) * 100.0) / 100.0;

            String nguCanh = "hocky:" + sem1.getId() + "-" + sem2.getId();
            String maLuat = "RULE_GPA_DECLINE";
            String fingerprint = taoKhoaChongTrung(student.getId(), maLuat, nguCanh);

            boolean viPham = doGiam >= mucGiamToiThieu;

            if (viPham) {
                String lyDo = String.format("GPA hệ 4 giảm từ %.2f (%s) xuống %.2f (%s), suy giảm %.2f điểm (vượt ngưỡng %.2f).",
                        gpa1, sem1.getTen(), gpa2, sem2.getTen(), doGiam, mucGiamToiThieu);

                String congThuc = String.format(
                        "Công thức kiểm tra: [(gpaKyTruoc - gpaKySau) >= mucGiamToiThieu]. " +
                        "GPA %s = %.2f; GPA %s = %.2f. Mức sụt giảm: %.2f - %.2f = %.2f điểm (ngưỡng tối thiểu: %.2f điểm).",
                        sem1.getTen(), gpa1, sem2.getTen(), gpa2, gpa1, gpa2, doGiam, mucGiamToiThieu);

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("sinhVienId", student.getId());
                evidence.put("maSinhVien", student.getMa());
                evidence.put("hoTenSinhVien", student.getHoTen());
                evidence.put("hocKyTruocId", sem1.getId());
                evidence.put("tenHocKyTruoc", sem1.getTen());
                evidence.put("gpaTruoc", gpa1);
                evidence.put("hocKySauId", sem2.getId());
                evidence.put("tenHocKySau", sem2.getTen());
                evidence.put("gpaSau", gpa2);
                evidence.put("mucGiam", doGiam);
                evidence.put("nguongGiam", mucGiamToiThieu);
                evidence.put("thoiDiemDanhGia", LocalDateTime.now().toString());

                ghiNhanHoacCapNhatCanhBao(student, AcademicAlertRuleType.GPA_GIAM, maLuat,
                        "Cảnh báo suy giảm GPA liên tiếp", mucDo, lyDo, congThuc, evidence,
                        nguCanh, fingerprint, null, svXem, summary);
            } else {
                tuDongGiaiToaNeuTonTai(fingerprint,
                        "Điểm GPA giữa các học kỳ đã cải thiện hoặc chênh lệch giảm đã dưới ngưỡng cảnh báo.",
                        summary);
            }
        }
    }

    // =========================================================================
    // LUẬT 4: THIẾU TÍN CHỈ SO VỚI TIẾN ĐỘ HỌC TẬP
    // =========================================================================
    private void danhGiaLuatTienDoTinChi(Student student, ScanSummary summary) {
        var cfg = properties.getTienDoTinChi();
        int tcChuanMoiKy = cfg.getTinChiChuanMoiKy();
        int nguongThieu = cfg.getNguongTinChiThieu();
        double nguongTyLe = cfg.getNguongTyLeHoanThanh();
        AcademicAlertSeverity mucDo = AcademicAlertSeverity.valueOf(cfg.getMucDo());
        boolean svXem = cfg.isChoPhepSinhVienXem();

        List<CourseRegistration> allRegs = registrationRepository.findAllActiveOfStudent(student.getId(), RegistrationStatus.DA_DANG_KY);
        if (allRegs.isEmpty()) return;

        // Đếm số học kỳ khác nhau mà sinh viên đã tham gia đăng ký
        long countSemesters = allRegs.stream()
                .map(r -> r.getLopHocPhan().getHocKy().getId())
                .distinct()
                .count();

        int soKyDaHoc = Math.max(1, (int) countSemesters);
        int tinChiKyVong = soKyDaHoc * tcChuanMoiKy;

        // Tính tín chỉ tích lũy đạt được (điểm tổng kết >= 5.0, mỗi môn chỉ tính 1 lần)
        List<Grade> grades = gradeRepository.findByDangKyIdIn(allRegs.stream().map(CourseRegistration::getId).toList());
        Map<Long, Grade> gradeMap = grades.stream().collect(Collectors.toMap(g -> g.getDangKy().getId(), g -> g));

        Set<Long> monDaDat = new HashSet<>();
        int tinChiTichLuy = 0;
        for (CourseRegistration reg : allRegs) {
            CourseClass lop = reg.getLopHocPhan();
            Subject mon = lop.getMonHoc();
            if (mon != null && !monDaDat.contains(mon.getId()) && lop.isBangDiemDaCongBo()) {
                Grade g = gradeMap.get(reg.getId());
                if (g != null && g.getDiemTongKet() != null && g.getDiemTongKet() >= AcademicRules.DIEM_DAT) {
                    monDaDat.add(mon.getId());
                    tinChiTichLuy += mon.getSoTinChi();
                }
            }
        }

        int tinChiThieu = Math.max(0, tinChiKyVong - tinChiTichLuy);
        double tyLeHoanThanh = tinChiKyVong == 0 ? 100.0 : Math.round(((double) tinChiTichLuy / tinChiKyVong) * 10000.0) / 100.0;

        String nguCanh = "tiendo:ky-" + soKyDaHoc;
        String maLuat = "RULE_CREDIT_DEFICIT";
        String fingerprint = taoKhoaChongTrung(student.getId(), maLuat, nguCanh);

        boolean viPham = tinChiThieu >= nguongThieu || (soKyDaHoc >= 2 && tyLeHoanThanh < nguongTyLe);

        if (viPham) {
            String lyDo = String.format("Sinh viên đã qua %d học kỳ, tích lũy %d/%d tín chỉ kỳ vọng (thiếu %d tín chỉ, đạt %.2f%% so với chuẩn %.2f%%).",
                    soKyDaHoc, tinChiTichLuy, tinChiKyVong, tinChiThieu, tyLeHoanThanh, nguongTyLe);

            String congThuc = String.format(
                    "Công thức kiểm tra: [tinChiThieu >= %d HOẶC tyLeHoanThanh < %.2f%%]. " +
                    "Số kỳ đào tạo: %d kỳ; Tín chỉ kỳ vọng chuẩn: %d kỳ * %d TC = %d TC; " +
                    "Tín chỉ tích lũy thực tế: %d TC. Tín chỉ thiếu: %d - %d = %d TC (Tỷ lệ: %.2f%%).",
                    nguongThieu, nguongTyLe, soKyDaHoc, soKyDaHoc, tcChuanMoiKy, tinChiKyVong,
                    tinChiTichLuy, tinChiKyVong, tinChiTichLuy, tinChiThieu, tyLeHoanThanh);

            Map<String, Object> evidence = new LinkedHashMap<>();
            evidence.put("sinhVienId", student.getId());
            evidence.put("maSinhVien", student.getMa());
            evidence.put("hoTenSinhVien", student.getHoTen());
            evidence.put("soKyDaHoc", soKyDaHoc);
            evidence.put("tinChiKyVong", tinChiKyVong);
            evidence.put("tinChiTichLuy", tinChiTichLuy);
            evidence.put("tinChiThieu", tinChiThieu);
            evidence.put("nguongTinChiThieu", nguongThieu);
            evidence.put("tyLeHoanThanh", tyLeHoanThanh);
            evidence.put("nguongTyLeHoanThanh", nguongTyLe);
            evidence.put("thoiDiemDanhGia", LocalDateTime.now().toString());

            ghiNhanHoacCapNhatCanhBao(student, AcademicAlertRuleType.TIEN_DO_TIN_CHI, maLuat,
                    "Cảnh báo chậm tiến độ tích lũy tín chỉ", mucDo, lyDo, congThuc, evidence,
                    nguCanh, fingerprint, null, svXem, summary);
        } else {
            tuDongGiaiToaNeuTonTai(fingerprint,
                    "Số tín chỉ tích lũy và tỷ lệ hoàn thành đã đạt mức chuẩn quy định.",
                    summary);
        }
    }

    // =========================================================================
    // HỖ TRỢ XỬ LÝ CHỐNG TRÙNG VÀ CẬP NHẬT/ĐÓNG CÓ LƯU VẾT
    // =========================================================================
    private void ghiNhanHoacCapNhatCanhBao(
            Student student,
            AcademicAlertRuleType loaiLuat,
            String maLuat,
            String tenLuat,
            AcademicAlertSeverity mucDo,
            String lyDo,
            String congThuc,
            Map<String, Object> evidence,
            String nguCanh,
            String fingerprint,
            Long lopHocPhanId,
            boolean choPhepSinhVienXem,
            ScanSummary summary
    ) {
        String jsonEvidence = serializeEvidence(evidence);
        Optional<AcademicAlert> existingOpt = alertRepository.findByKhoaChongTrung(fingerprint);

        if (existingOpt.isPresent()) {
            AcademicAlert alert = existingOpt.get();
            // Cập nhật cảnh báo hiện có mà không tạo bản ghi mới
            alert.setLyDo(lyDo);
            alert.setCongThucGiaiThich(congThuc);
            alert.setDuLieuDanChung(jsonEvidence);
            alert.setCapNhatLuc(LocalDateTime.now());
            alert.setChoPhepSinhVienXem(choPhepSinhVienXem);
            alert.setMucDo(mucDo);

            if (!alert.isActive()) {
                // Tái kích hoạt cảnh báo nếu trước đó đã giải tỏa nhưng nay lại vi phạm
                alert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);
                alert.setActive(true);
                alert.setNguyenNhanGiaiToa(null);
            }

            alertRepository.save(alert);
            summary.updated++;
        } else {
            // Tạo mới cảnh báo duy nhất
            AcademicAlert newAlert = new AcademicAlert();
            newAlert.setSinhVien(student);
            newAlert.setLoaiLuat(loaiLuat);
            newAlert.setMaLuat(maLuat);
            newAlert.setTenLuat(tenLuat);
            newAlert.setMucDo(mucDo);
            newAlert.setLyDo(lyDo);
            newAlert.setCongThucGiaiThich(congThuc);
            newAlert.setDuLieuDanChung(jsonEvidence);
            newAlert.setNguCanh(nguCanh);
            newAlert.setKhoaChongTrung(fingerprint);
            newAlert.setLopHocPhanId(lopHocPhanId);
            newAlert.setTrangThai(AcademicAlertStatus.DANG_CANH_BAO);
            newAlert.setActive(true);
            newAlert.setChoPhepSinhVienXem(choPhepSinhVienXem);
            newAlert.setNgayTao(LocalDateTime.now());
            newAlert.setCapNhatLuc(LocalDateTime.now());

            AcademicAlert saved = alertRepository.save(newAlert);
            summary.created++;

            // Ghi audit log
            try {
                auditLogService.execute("ACADEMIC_ALERT_CREATED", "ACADEMIC_ALERT", saved.getId(), ctx -> {
                    ctx.changes(Map.of(), Map.of(
                            "canhBaoId", saved.getId(),
                            "sinhVienId", student.getId(),
                            "maSinhVien", student.getMa(),
                            "loaiLuat", loaiLuat.name(),
                            "maLuat", maLuat,
                            "mucDo", mucDo.name()
                    ));
                    return null;
                });
            } catch (Exception e) {
                log.warn("Không ghi được audit log cho cảnh báo mới: {}", e.getMessage());
            }
        }
    }

    private void tuDongGiaiToaNeuTonTai(String fingerprint, String nguyenNhan, ScanSummary summary) {
        Optional<AcademicAlert> existingOpt = alertRepository.findByKhoaChongTrung(fingerprint);
        if (existingOpt.isPresent()) {
            AcademicAlert alert = existingOpt.get();
            if (alert.isActive()) {
                alert.setTrangThai(AcademicAlertStatus.DA_GIAI_TOA);
                alert.setActive(false);
                alert.setNgayXuLy(LocalDateTime.now());
                alert.setNguyenNhanGiaiToa(nguyenNhan);
                alert.setCapNhatLuc(LocalDateTime.now());
                AcademicAlert saved = alertRepository.save(alert);
                summary.autoResolved++;

                try {
                    auditLogService.execute("ACADEMIC_ALERT_AUTO_RESOLVED", "ACADEMIC_ALERT", saved.getId(), ctx -> {
                        ctx.changes(Map.of("trangThaiCu", AcademicAlertStatus.DANG_CANH_BAO.name()), Map.of(
                                "canhBaoId", saved.getId(),
                                "trangThaiMoi", AcademicAlertStatus.DA_GIAI_TOA.name(),
                                "nguyenNhanGiaiToa", nguyenNhan
                        ));
                        return null;
                    });
                } catch (Exception e) {
                    log.warn("Không ghi được audit log cho giải tỏa tự động: {}", e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    // CÁC HÀM TRUY VẤN VÀ CẬP NHẬT TRẠNG THÁI CHO TỪNG PHÂN QUYỀN
    // =========================================================================

    /**
     * Tra cứu cảnh báo cho Quản trị viên (lọc theo loại luật, trạng thái, mức độ, keyword).
     */
    @Transactional(readOnly = true)
    public Page<AcademicAlertDto> danhSachCanhBaoAdmin(
            AcademicAlertStatus trangThai,
            AcademicAlertRuleType loaiLuat,
            AcademicAlertSeverity mucDo,
            Boolean active,
            String keyword,
            Pageable pageable
    ) {
        return alertRepository.filterForAdmin(trangThai, loaiLuat, mucDo, active, keyword, pageable)
                .map(this::toDto);
    }

    /**
     * Tra cứu cảnh báo cho Giảng viên (chỉ xem sinh viên thuộc các lớp giảng viên phụ trách).
     */
    @Transactional(readOnly = true)
    public Page<AcademicAlertDto> danhSachCanhBaoGiangVien(Long lopHocPhanId, Pageable pageable) {
        Lecturer gv = giangVienHienTai();
        List<CourseClass> classesTaught = courseClassRepository.findByGiangVienIdOrderByMaAsc(gv.getId());
        Set<Long> assignedClassIds = classesTaught.stream().map(CourseClass::getId).collect(Collectors.toSet());

        if (assignedClassIds.isEmpty()) {
            return Page.empty(pageable);
        }

        if (lopHocPhanId != null) {
            if (!assignedClassIds.contains(lopHocPhanId)) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
            return alertRepository.findByLopHocPhanIdIn(List.of(lopHocPhanId), pageable).map(this::toDto);
        }

        // Lấy danh sách sinh viên thuộc các lớp giảng viên phụ trách
        List<Student> assignedStudents = registrationRepository.findAssignedStudentsOfLecturer(
                gv.getId(), null, RegistrationStatus.DA_DANG_KY);
        Set<Long> studentIds = assignedStudents.stream().map(Student::getId).collect(Collectors.toSet());

        if (studentIds.isEmpty()) {
            return Page.empty(pageable);
        }

        return alertRepository.findBySinhVienIdIn(studentIds, pageable).map(this::toDto);
    }

    /**
     * Tra cứu cảnh báo cho chính Sinh viên (chỉ xem cảnh báo của mình VÀ cho phép sinh viên xem).
     */
    @Transactional(readOnly = true)
    public Page<AcademicAlertDto> danhSachCanhBaoSinhVien(Pageable pageable) {
        Student sv = sinhVienHienTai();
        return alertRepository.findForStudent(sv.getId(), pageable).map(this::toDto);
    }

    /**
     * Tóm tắt cảnh báo cho sinh viên hiện tại.
     */
    @Transactional(readOnly = true)
    public StudentAlertSummaryDto tomTatCanhBaoSinhVien() {
        Student sv = sinhVienHienTai();
        List<AcademicAlert> alerts = alertRepository.findForStudentAll(sv.getId());
        long activeCount = alerts.stream().filter(AcademicAlert::isActive).count();
        List<AcademicAlertDto> dtos = alerts.stream().map(this::toDto).toList();
        return new StudentAlertSummaryDto(alerts.size(), activeCount, dtos);
    }

    /**
     * Cập nhật trạng thái xử lý cảnh báo (Cán bộ Đào tạo hoặc Giảng viên phụ trách).
     * Tuyệt đối không thay đổi trạng thái học vụ sinh viên hoặc khóa tài khoản!
     */
    public AcademicAlertDto capNhatTrangThaiXuLy(Long alertId, CapNhatTrangThaiRequest request) {
        if (request == null || request.trangThai() == null) {
            throw ApiException.badRequest("Trạng thái xử lý không được để trống");
        }

        AcademicAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cảnh báo học tập"));

        User actor = currentUser.get();
        if (actor.getRole() == Role.SINH_VIEN) {
            throw ApiException.forbidden("Sinh viên không có quyền thay đổi trạng thái xử lý cảnh báo");
        }

        if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienHienTai();
            boolean isAssigned = alert.getLopHocPhanId() != null
                    ? kiemTraLopThuocGiangVien(alert.getLopHocPhanId(), gv.getId())
                    : registrationRepository.existsBySinhVienIdAndGiangVienIdAndTrangThai(
                            alert.getSinhVien().getId(), gv.getId(), RegistrationStatus.DA_DANG_KY);
            if (!isAssigned) {
                throw ApiException.forbidden("Bạn chỉ được đánh dấu xử lý cho sinh viên thuộc lớp bạn phụ trách");
            }
        }

        AcademicAlertStatus oldStatus = alert.getTrangThai();
        alert.setTrangThai(request.trangThai());
        alert.setNgayXuLy(LocalDateTime.now());
        alert.setNguoiXuLy(actor.getUsername());
        alert.setGhiChuXuLy(request.ghiChu());
        alert.setCapNhatLuc(LocalDateTime.now());

        AcademicAlert saved = alertRepository.save(alert);

        try {
            auditLogService.execute("ACADEMIC_ALERT_STATUS_MANUALLY_UPDATED", "ACADEMIC_ALERT", saved.getId(), ctx -> {
                ctx.changes(Map.of("trangThaiCu", oldStatus.name()), Map.of(
                        "canhBaoId", saved.getId(),
                        "trangThaiMoi", request.trangThai().name(),
                        "nguoiXuLy", actor.getUsername(),
                        "ghiChu", request.ghiChu() != null ? request.ghiChu() : ""
                ));
                return null;
            });
        } catch (Exception e) {
            log.warn("Lỗi ghi audit status update: {}", e.getMessage());
        }

        return toDto(saved);
    }

    /**
     * Chi tiết cảnh báo theo ID.
     */
    @Transactional(readOnly = true)
    public AcademicAlertDto layChiTiet(Long alertId) {
        AcademicAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy cảnh báo học tập"));

        User actor = currentUser.get();
        if (actor.getRole() == Role.SINH_VIEN) {
            Student sv = sinhVienHienTai();
            if (!alert.getSinhVien().getId().equals(sv.getId()) || !alert.isChoPhepSinhVienXem()) {
                throw ApiException.forbidden("Bạn không có quyền xem cảnh báo này");
            }
        } else if (actor.getRole() == Role.GIANG_VIEN) {
            Lecturer gv = giangVienHienTai();
            boolean isAssigned = alert.getLopHocPhanId() != null
                    ? kiemTraLopThuocGiangVien(alert.getLopHocPhanId(), gv.getId())
                    : registrationRepository.existsBySinhVienIdAndGiangVienIdAndTrangThai(
                            alert.getSinhVien().getId(), gv.getId(), RegistrationStatus.DA_DANG_KY);
            if (!isAssigned) {
                throw ApiException.forbidden("Bạn không có quyền xem cảnh báo này");
            }
        }

        return toDto(alert);
    }

    private boolean kiemTraLopThuocGiangVien(Long lopHocPhanId, Long giangVienId) {
        if (lopHocPhanId == null || giangVienId == null) return false;
        return courseClassRepository.findById(lopHocPhanId)
                .map(c -> c.getGiangVien() != null && giangVienId.equals(c.getGiangVien().getId()))
                .orElse(false);
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================
    private String taoKhoaChongTrung(Long sinhVienId, String maLuat, String nguCanh) {
        return "sv:" + sinhVienId + "#" + maLuat + "#" + nguCanh;
    }

    private double quyDoiHe4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= 5.0) return 1.5;
        if (diem10 >= 4.0) return 1.0;
        return 0.0;
    }

    private String serializeEvidence(Map<String, Object> evidence) {
        try {
            return objectMapper.writeValueAsString(evidence);
        } catch (JsonProcessingException e) {
            log.error("Không serialize được dữ liệu dẫn chứng: {}", e.getMessage());
            return "{}";
        }
    }

    private AcademicAlertDto toDto(AcademicAlert alert) {
        Student sv = alert.getSinhVien();
        String tenMon = null;
        String maLop = null;
        if (alert.getLopHocPhanId() != null) {
            Optional<CourseClass> lopOpt = courseClassRepository.findById(alert.getLopHocPhanId());
            if (lopOpt.isPresent()) {
                maLop = lopOpt.get().getMa();
                if (lopOpt.get().getMonHoc() != null) {
                    tenMon = lopOpt.get().getMonHoc().getTen();
                }
            }
        }

        String lopHanhChinh = sv.getLopHanhChinh() != null ? sv.getLopHanhChinh().getTen() : null;

        return new AcademicAlertDto(
                alert.getId(),
                sv.getId(),
                sv.getMa(),
                sv.getHoTen(),
                lopHanhChinh,
                alert.getLopHocPhanId(),
                maLop,
                tenMon,
                alert.getLoaiLuat(),
                alert.getLoaiLuat() != null ? alert.getLoaiLuat().getTenHienThi() : null,
                alert.getMaLuat(),
                alert.getTenLuat(),
                alert.getMucDo(),
                alert.getMucDo() != null ? alert.getMucDo().getTenHienThi() : null,
                alert.getLyDo(),
                alert.getCongThucGiaiThich(),
                alert.getDuLieuDanChung(),
                alert.getNguCanh(),
                alert.getTrangThai(),
                alert.getTrangThai() != null ? alert.getTrangThai().getTenHienThi() : null,
                alert.isChoPhepSinhVienXem(),
                alert.isActive(),
                alert.getNgayTao(),
                alert.getCapNhatLuc(),
                alert.getNgayXuLy(),
                alert.getNguoiXuLy(),
                alert.getGhiChuXuLy(),
                alert.getNguyenNhanGiaiToa()
        );
    }

    private Student sinhVienHienTai() {
        return studentRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private Lecturer giangVienHienTai() {
        return lecturerRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }
}
