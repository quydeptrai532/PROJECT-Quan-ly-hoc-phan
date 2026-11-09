package com.edu.registration.service;

import com.edu.registration.dto.NotificationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Notification;
import com.edu.registration.entity.CourseClassAnnouncement;
import com.edu.registration.entity.ClassAbsenceNotice;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.NotificationRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final String SECTION_CANCELLED_REASON_PREFIX = "Lớp học phần bị hủy";
    public static final String GRADES_PUBLISHED_EVENT = "GRADES_PUBLISHED";
    public static final String CLASS_ANNOUNCEMENT_EVENT = "CLASS_ANNOUNCEMENT";
    public static final String ATTENDANCE_ALERT_EVENT = "ATTENDANCE_THRESHOLD_ALERT";
    public static final String ATTENDANCE_RESOLVED_EVENT = "ATTENDANCE_ALERT_RESOLVED";
    public static final String CLASS_STUDENT_TRANSFERRED_EVENT = "CLASS_STUDENT_TRANSFERRED";

    private final NotificationRepository thongBaoRepository;
    private final CourseRegistrationRepository dangKyRepository;
    private final RegistrationHistoryRepository lichSuDangKyRepository;
    private final GradeRepository diemRepository;
    private final StudentRepository sinhVienRepository;
    private final SemesterRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final TransactionTemplate requiresNewTemplate;

    public NotificationService(NotificationRepository thongBaoRepository,
                           CourseRegistrationRepository dangKyRepository,
                           StudentRepository sinhVienRepository,
                           SemesterRepository hocKyRepository,
                           CurrentUser currentUser) {
        this(thongBaoRepository, dangKyRepository, null, null, sinhVienRepository, hocKyRepository, currentUser, null);
    }

    public NotificationService(NotificationRepository thongBaoRepository,
                           CourseRegistrationRepository dangKyRepository,
                           RegistrationHistoryRepository lichSuDangKyRepository,
                           StudentRepository sinhVienRepository,
                           SemesterRepository hocKyRepository,
                           CurrentUser currentUser,
                           PlatformTransactionManager transactionManager) {
        this(thongBaoRepository, dangKyRepository, lichSuDangKyRepository, null,
                sinhVienRepository, hocKyRepository, currentUser, transactionManager);
    }

    @Autowired
    public NotificationService(NotificationRepository thongBaoRepository,
                           CourseRegistrationRepository dangKyRepository,
                           RegistrationHistoryRepository lichSuDangKyRepository,
                           GradeRepository diemRepository,
                           StudentRepository sinhVienRepository,
                           SemesterRepository hocKyRepository,
                           CurrentUser currentUser,
                           PlatformTransactionManager transactionManager) {
        this.thongBaoRepository = thongBaoRepository;
        this.dangKyRepository = dangKyRepository;
        this.lichSuDangKyRepository = lichSuDangKyRepository;
        this.diemRepository = diemRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    @Transactional
    public NotificationDtos.ThongBaoPageDto list(boolean unreadOnly, int page, int size) {
        Long recipientId = currentUser.get().getId();
        reconcileForUserSafely(recipientId);
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 50));
        PageRequest request = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Notification> notifications = unreadOnly
                ? thongBaoRepository.findByRecipientIdAndReadAtIsNull(recipientId, request)
                : thongBaoRepository.findByRecipientId(recipientId, request);
        return new NotificationDtos.ThongBaoPageDto(
                notifications.getContent().stream().map(this::toDto).toList(),
                notifications.getNumber(), notifications.getSize(), notifications.getTotalElements(),
                notifications.getTotalPages(), thongBaoRepository.countByRecipientIdAndReadAtIsNull(recipientId));
    }

    @Transactional
    public NotificationDtos.ThongBaoDto markRead(Long id) {
        Long recipientId = currentUser.get().getId();
        Notification notification = thongBaoRepository.findByIdAndRecipientId(id, recipientId)
                .orElseThrow(() -> ApiException.notFound("Thông báo không tồn tại"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return toDto(thongBaoRepository.save(notification));
    }

    @Transactional
    public long markAllRead() {
        Long recipientId = currentUser.get().getId();
        reconcileForUserSafely(recipientId);
        return thongBaoRepository.markAllRead(recipientId, LocalDateTime.now());
    }

    /**
     * Emits registration/cancellation notices only after the business transaction commits,
     * isolating any notification persistence error from the committed registration state.
     */
    public void registrationChanged(CourseRegistration registration, boolean registered) {
        registrationChanged(registration, registered, LocalDateTime.now(), null);
    }

    public void registrationChanged(CourseRegistration registration, boolean registered,
                                    LocalDateTime occurredAt, String reason) {
        if (registration == null || registration.getLopHocPhan() == null || registration.getSinhVien() == null) {
            return;
        }
        CourseClass section = registration.getLopHocPhan();
        User studentUser = registration.getSinhVien().getUser();
        Lecturer lecturer = section.getGiangVien();
        User lecturerUser = lecturer != null ? lecturer.getUser() : null;
        Long registrationId = registration.getId();
        LocalDateTime registeredAt = registration.getThoiGianDangKy();
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt
                : (registeredAt != null ? registeredAt : LocalDateTime.now());
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";

        String eventType = registered ? "COURSE_REGISTRATION_CONFIRMED" : "COURSE_REGISTRATION_CANCELLED";
        String idempotencyKey = buildRegistrationEventKey(registrationId, eventType, registeredAt);
        String title = registered ? "Đăng ký học phần thành công" : "Đã hủy đăng ký học phần";
        String message = buildStudentRegistrationMessage(
                registered, false, sectionCode, subjectCode, subjectName, semesterName, effectiveTime, reason);
        String targetUrl = registered ? "/hoc-phan-cua-toi" : "/thoi-khoa-bieu";

        String action = registered ? "đăng ký" : "hủy đăng ký";
        String lecturerTitle = registered ? "Sinh viên đăng ký lớp học phần" : "Sinh viên hủy đăng ký lớp học phần";
        String lecturerMessage = "Có sinh viên " + action + " lớp " + sectionCode + " — "
                + subjectName + " lúc " + formatTime(effectiveTime) + ".";

        dispatchAfterCommit(eventType + ":" + idempotencyKey, () -> {
            send(studentUser, eventType, idempotencyKey, title, message, "NORMAL", targetUrl, effectiveTime);
            if (lecturerUser != null) {
                send(lecturerUser, eventType, idempotencyKey, lecturerTitle, lecturerMessage,
                        "NORMAL", "/lop-cua-toi", effectiveTime);
            }
        });
    }

    /**
     * Emits high-priority notices to only the students whose registrations were active at the moment
     * a course section was cancelled and transitioned to DA_HUY.
     */
    public void sectionCancelled(CourseClass section, List<CourseRegistration> affectedRegistrations,
                                 LocalDateTime occurredAt, String reason) {
        if (section == null) {
            return;
        }
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt : LocalDateTime.now();
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
        String cleanReason = cleanSectionCancelReason(reason);

        String eventType = "COURSE_SECTION_CANCELLED";
        String title = "Lớp học phần bị hủy";
        String studentMessage = buildStudentRegistrationMessage(
                false, true, sectionCode, subjectCode, subjectName, semesterName, effectiveTime, cleanReason);

        record RecipientNotice(User user, String key) {}
        List<RecipientNotice> studentNotices = new ArrayList<>();
        if (affectedRegistrations != null) {
            for (CourseRegistration reg : affectedRegistrations) {
                if (reg != null && reg.getSinhVien() != null && reg.getSinhVien().getUser() != null) {
                    String key = buildRegistrationEventKey(reg.getId(), eventType, reg.getThoiGianDangKy());
                    studentNotices.add(new RecipientNotice(reg.getSinhVien().getUser(), key));
                }
            }
        }

        User lecturerUser = section.getGiangVien() != null ? section.getGiangVien().getUser() : null;
        String lecturerKey = "section-cancelled:" + section.getId() + ":"
                + (cleanReason == null ? "default" : cleanReason);
        String lecturerMessage = "Lớp học phần " + sectionCode + " — " + subjectName + " (" + semesterName
                + ") đã bị hủy lúc " + formatTime(effectiveTime) + formatReason(cleanReason)
                + ". Bước tiếp theo: kiểm tra danh sách lớp phụ trách.";

        dispatchAfterCommit(eventType + ":section:" + section.getId(), () -> {
            for (RecipientNotice item : studentNotices) {
                send(item.user(), eventType, item.key(), title, studentMessage, "HIGH", "/dang-ky", effectiveTime);
            }
            if (lecturerUser != null) {
                send(lecturerUser, eventType, lecturerKey, title, lecturerMessage,
                        "HIGH", "/lop-cua-toi", effectiveTime);
            }
        });
    }

    /**
     * Emits notices to currently enrolled students when a lecturer announces a class session cancellation.
     */
    /**
     * Emits notifications when an administrator transfers a student to another class (QLSV-79).
     * Dispatches after commit, uses deterministic idempotency keys per recipient to eliminate duplicate notifications.
     */
    public void studentTransferred(CourseClass lopNguon, CourseClass lopDich, Student sinhVien,
                                   LocalDateTime occurredAt, String reason) {
        if (lopNguon == null || lopDich == null || sinhVien == null) {
            return;
        }
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt : LocalDateTime.now();
        User studentUser = sinhVien.getUser();
        Lecturer srcLec = lopNguon.getGiangVien();
        User srcLecUser = srcLec != null ? srcLec.getUser() : null;
        Lecturer tgtLec = lopDich.getGiangVien();
        User tgtLecUser = tgtLec != null ? tgtLec.getUser() : null;

        String cleanReason = cleanSectionCancelReason(reason);
        String timeStr = formatTime(effectiveTime);
        String eventType = CLASS_STUDENT_TRANSFERRED_EVENT;
        String baseKey = "transfer:" + lopNguon.getId() + ":" + lopDich.getId() + ":" + sinhVien.getId()
                + ":" + effectiveTime.truncatedTo(ChronoUnit.SECONDS);

        String studentTitle = "Chuyển lớp học phần thành công";
        String studentMsg = "Bạn đã được chuyển từ lớp " + lopNguon.getMa() + " (" + lopNguon.getMonHoc().getTen() + ")"
                + " sang lớp " + lopDich.getMa() + " (" + lopDich.getMonHoc().getTen() + ") lúc " + timeStr
                + formatReason(cleanReason) + ". Bước tiếp theo: kiểm tra thời khóa biểu mới.";

        String srcLecTitle = "Sinh viên được chuyển khỏi lớp";
        String srcLecMsg = "Sinh viên " + sinhVien.getHoTen() + " (" + sinhVien.getMa() + ")"
                + " đã được chuyển khỏi lớp " + lopNguon.getMa() + " sang lớp " + lopDich.getMa()
                + " lúc " + timeStr + formatReason(cleanReason) + ".";

        String tgtLecTitle = "Tiếp nhận sinh viên mới vào lớp";
        String tgtLecMsg = "Lớp " + lopDich.getMa() + " (" + lopDich.getMonHoc().getTen() + ")"
                + " tiếp nhận sinh viên " + sinhVien.getHoTen() + " (" + sinhVien.getMa() + ")"
                + " được chuyển từ lớp " + lopNguon.getMa() + " lúc " + timeStr + formatReason(cleanReason) + ".";

        dispatchAfterCommit(eventType + ":" + baseKey, () -> {
            if (studentUser != null) {
                send(studentUser, eventType, baseKey + ":student", studentTitle, studentMsg, "HIGH", "/thoi-khoa-bieu", effectiveTime);
            }
            if (srcLecUser != null) {
                send(srcLecUser, eventType, baseKey + ":src-lec", srcLecTitle, srcLecMsg, "NORMAL", "/lop-cua-toi", effectiveTime);
            }
            if (tgtLecUser != null) {
                send(tgtLecUser, eventType, baseKey + ":tgt-lec", tgtLecTitle, tgtLecMsg, "NORMAL", "/lop-cua-toi", effectiveTime);
            }
        });
    }

    public void classSessionCancelled(ClassAbsenceNotice thongBaoNghi) {
        if (thongBaoNghi == null || thongBaoNghi.getLopHocPhan() == null) {
            return;
        }
        CourseClass section = thongBaoNghi.getLopHocPhan();
        Long sectionId = section.getId();
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
        LocalDate ngayNghi = thongBaoNghi.getNgayNghi();
        LocalDateTime effectiveTime = thongBaoNghi.getThoiGianTao() != null
                ? thongBaoNghi.getThoiGianTao() : LocalDateTime.now();
        String reason = thongBaoNghi.getLyDo();

        List<User> recipients = dangKyRepository.findByLopHocPhan(sectionId, RegistrationStatus.DA_DANG_KY)
                .stream()
                .filter(reg -> reg.getSinhVien() != null && reg.getSinhVien().getUser() != null)
                .map(reg -> reg.getSinhVien().getUser())
                .toList();

        String eventType = "COURSE_SESSION_CANCELLED";
        String idempotencyKey = "session-cancelled:" + sectionId + ":" + ngayNghi;
        String title = "Thông báo nghỉ học lớp " + sectionCode;
        String subjectLabel = subjectCode.isBlank() ? subjectName : subjectCode + " — " + subjectName;
        String message = "Lớp " + sectionCode + " (" + subjectLabel + ", " + semesterName
                + ") tạm nghỉ buổi học ngày " + ngayNghi + " (cập nhật lúc " + formatTime(effectiveTime) + "). "
                + "Kết quả: Buổi học tạm nghỉ" + formatReason(reason)
                + ". Bước tiếp theo: theo dõi lịch học bù và thời khóa biểu cá nhân.";

        dispatchAfterCommit(eventType + ":" + idempotencyKey, () -> {
            for (User recipient : recipients) {
                send(recipient, eventType, idempotencyKey, title, message,
                        "HIGH", "/thoi-khoa-bieu", effectiveTime);
            }
        });
    }

    /**
     * Emits class announcement notifications to actively enrolled students of a course section (QLSV-68).
     */
    public void classAnnouncementCreated(CourseClassAnnouncement announcement, List<User> eligibleStudents) {
        if (announcement == null || announcement.getLopHocPhan() == null || eligibleStudents == null || eligibleStudents.isEmpty()) {
            return;
        }
        CourseClass section = announcement.getLopHocPhan();
        String sectionCode = section.getMa();
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        LocalDateTime effectiveTime = announcement.getThoiGianTao() != null ? announcement.getThoiGianTao() : LocalDateTime.now();
        String title = announcement.getTieuDe();
        String prefix = "[Lớp " + sectionCode + (subjectName.isBlank() ? "" : " — " + subjectName) + "] ";
        String message = prefix + announcement.getNoiDung();
        if (message.length() > 1000) {
            message = message.substring(0, 997) + "...";
        }
        String idempotencyKey = "class-announcement:" + announcement.getId();
        String targetUrl = "/hoc-phan-cua-toi";

        String finalMessage = message;
        dispatchAfterCommit(CLASS_ANNOUNCEMENT_EVENT + ":" + idempotencyKey, () -> {
            sendBatchByIdempotencyKey(eligibleStudents, CLASS_ANNOUNCEMENT_EVENT, idempotencyKey,
                    title, finalMessage, "NORMAL", targetUrl, effectiveTime);
        });
    }

    /**
     * Emits internal notifications to eligible students of a class section only after the grade sheet
     * has been approved and transitions to DA_CONG_BO. Never includes personal grades in content or URL.
     */
    public void gradesPublished(CourseClass section, List<CourseRegistration> eligibleRegistrations,
                                int publicationVersion, LocalDateTime occurredAt) {
        if (section == null || section.getId() == null || eligibleRegistrations == null || eligibleRegistrations.isEmpty()) {
            return;
        }
        Long sectionId = section.getId();
        String sectionCode = section.getMa();
        String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
        String academicYear = section.getHocKy() != null ? section.getHocKy().getNamHoc() : "";
        LocalDateTime effectiveTime = occurredAt != null ? occurredAt : LocalDateTime.now();

        String idempotencyKey = buildGradePublishedEventKey(sectionId, publicationVersion);
        String title = "Điểm lớp học phần " + sectionCode + " đã được công bố";
        String message = buildGradePublishedMessage(
                sectionCode, subjectCode, subjectName, semesterName, academicYear, effectiveTime);
        String targetUrl = "/diem";

        Map<Long, User> recipientsById = new LinkedHashMap<>();
        for (CourseRegistration reg : eligibleRegistrations) {
            if (reg == null || reg.getTrangThai() != RegistrationStatus.DA_DANG_KY || reg.getSinhVien() == null) {
                continue;
            }
            User studentUser = reg.getSinhVien().getUser();
            if (studentUser != null && studentUser.isTrangThai() && studentUser.getId() != null) {
                recipientsById.putIfAbsent(studentUser.getId(), studentUser);
            }
        }
        if (recipientsById.isEmpty()) {
            return;
        }

        List<User> recipients = List.copyOf(recipientsById.values());
        dispatchAfterCommit(GRADES_PUBLISHED_EVENT + ":" + idempotencyKey, () ->
                sendBatchByIdempotencyKey(recipients, GRADES_PUBLISHED_EVENT, idempotencyKey,
                        title, message, "HIGH", targetUrl, effectiveTime));
    }

    /** Called only when the locked section's schedule or assigned lecturer actually changes. */
    public void sectionChanged(CourseClass section, Map<String, Object> before, Map<String, Object> after,
                               String idempotencyKey) {
        boolean scheduleChanged = !same(before, after, "phong") || !same(before, after, "thu")
                || !same(before, after, "tietBatDau") || !same(before, after, "soTiet");
        boolean lecturerChanged = !same(before, after, "giangVienId");
        if (!scheduleChanged && !lecturerChanged) return;

        String eventType = scheduleChanged ? "COURSE_SECTION_SCHEDULE_CHANGED" : "COURSE_SECTION_ASSIGNMENT_CHANGED";
        String effectiveKey = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : deterministicSectionChangeKey(section.getId(), eventType, after);
        LocalDateTime occurredAt = LocalDateTime.now();
        String title = scheduleChanged ? "Lịch lớp học phần đã thay đổi" : "Giảng viên phụ trách lớp đã thay đổi";
        String message = scheduleChanged
                ? "Lịch lớp " + section.getMa() + " — " + section.getMonHoc().getTen() + " đã đổi lúc "
                    + formatTime(occurredAt) + ": " + thuLabel(section.getThu()) + ", tiết "
                    + section.getTietBatDau() + "-" + section.getTietKetThuc() + ", phòng " + section.getPhong()
                    + ". Bước tiếp theo: kiểm tra lại thời khóa biểu."
                : "Giảng viên phụ trách lớp " + section.getMa() + " — " + section.getMonHoc().getTen()
                    + " đã được cập nhật lúc " + formatTime(occurredAt)
                    + ". Bước tiếp theo: kiểm tra thông tin lớp học phần.";

        List<User> enrolledStudents = dangKyRepository.findByLopHocPhan(
                        section.getId(), RegistrationStatus.DA_DANG_KY)
                .stream()
                .filter(reg -> reg.getSinhVien() != null && reg.getSinhVien().getUser() != null)
                .map(reg -> reg.getSinhVien().getUser())
                .toList();
        User lecturerUser = section.getGiangVien() != null ? section.getGiangVien().getUser() : null;

        dispatchAfterCommit(eventType + ":" + effectiveKey, () -> {
            for (User studentUser : enrolledStudents) {
                send(studentUser, eventType, effectiveKey, title, message,
                        "NORMAL", "/thoi-khoa-bieu", occurredAt);
            }
            if (lecturerUser != null) {
                send(lecturerUser, eventType, effectiveKey, title, message,
                        "NORMAL", "/lop-cua-toi/thoi-khoa-bieu", occurredAt);
            }
        });
    }

    /**
     * Emits attendance threshold breach or resolution notification to lecturer and student (QLSV-71).
     */
    public void attendanceThresholdAlert(CourseClass section, Student student,
                                         String loaiNguong, double giaTriNguong,
                                         int soBuoiVang, int tongBuoiHopLe, double tyLeVang,
                                         boolean resolved) {
        if (section == null || student == null) {
            return;
        }
        String sectionCode = section.getMa();
        String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
        User studentUser = student.getUser();
        User lecturerUser = section.getGiangVien() != null ? section.getGiangVien().getUser() : null;

        String thresholdDesc = "SO_BUOI_VANG".equalsIgnoreCase(loaiNguong)
                ? ((int) giaTriNguong + " buổi")
                : (giaTriNguong + "%");

        String eventType = resolved ? ATTENDANCE_RESOLVED_EVENT : ATTENDANCE_ALERT_EVENT;
        String idempotencyKey = "attendance-alert:" + section.getId() + ":" + student.getId()
                + ":" + loaiNguong + ":" + soBuoiVang + "/" + tongBuoiHopLe + ":" + (resolved ? "RESOLVED" : "ALERT");

        String title = resolved
                ? "Giải tỏa cảnh báo chuyên cần: " + sectionCode
                : "Cảnh báo vắng quá ngưỡng: " + sectionCode;

        String message = resolved
                ? "Cảnh báo chuyên cần đã được giải tỏa: Sinh viên " + student.getHoTen() + " (" + student.getMa() + ")"
                  + " hiện có số buổi vắng là " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ (" + String.format(java.util.Locale.US, "%.1f", tyLeVang) + "%),"
                  + " đã trở về dưới ngưỡng quy định của lớp " + sectionCode + " (" + subjectName + ")."
                : "Sinh viên " + student.getHoTen() + " (" + student.getMa() + ")"
                  + " đã vắng " + soBuoiVang + "/" + tongBuoiHopLe + " buổi hợp lệ (" + String.format(java.util.Locale.US, "%.1f", tyLeVang) + "%),"
                  + " đạt/vượt ngưỡng " + thresholdDesc + " của lớp " + sectionCode + " (" + subjectName + ").";

        String priority = resolved ? "NORMAL" : "HIGH";
        LocalDateTime now = LocalDateTime.now();

        dispatchAfterCommit(eventType + ":" + idempotencyKey, () -> {
            if (studentUser != null) {
                send(studentUser, eventType, idempotencyKey, title, message, priority, "/chuyen-can-cua-toi", now);
            }
            if (lecturerUser != null) {
                send(lecturerUser, eventType, idempotencyKey, title, message, priority,
                        "/lop-cua-toi/" + section.getId() + "/diem-danh", now);
            }
        });
    }

    /** Idempotent daily reminder; the semester row lock serializes multiple scheduler instances. */
    @Transactional
    public int remindClosingRegistrationWindow(Long semesterId, LocalDate today) {
        Semester semester = hocKyRepository.findByIdForUpdate(semesterId).orElse(null);
        if (semester == null || semester.getDangKyTu().isAfter(today)
                || semester.getDangKyDen().isBefore(today)
                || semester.getDangKyDen().isAfter(today.plusDays(1))) {
            return 0;
        }
        String key = "registration-window-closing:" + semester.getId() + ":" + semester.getDangKyDen();
        int created = 0;
        LocalDateTime occurredAt = LocalDateTime.now();
        String title = "Đợt đăng ký học phần sắp kết thúc";
        String message = "Đợt đăng ký " + semester.getTen() + " sẽ kết thúc ngày "
                + semester.getDangKyDen() + ". Hãy hoàn tất kế hoạch học tập nếu cần.";
        for (Student student : sinhVienRepository.findAllActiveWithUser()) {
            if (send(student.getUser(), "REGISTRATION_WINDOW_CLOSING", key,
                    title, message, "HIGH", "/dang-ky", occurredAt)) {
                created++;
            }
        }
        return created;
    }

    /**
     * Reconciles durable registration and grade publication events for a user so any notification
     * whose post-commit write failed transiently is deterministically recovered without duplication.
     */
    public int reconcileForUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        int recovered = 0;
        if (lichSuDangKyRepository != null) {
            List<RegistrationHistory> events = lichSuDangKyRepository.findByStudentUserIdWithDetails(userId);
            for (RegistrationHistory event : events) {
                if (event.getDangKyId() == null || event.getLopHocPhan() == null || event.getSinhVien() == null) {
                    continue;
                }
                User studentUser = event.getSinhVien().getUser();
                if (studentUser == null || !studentUser.isTrangThai()) {
                    continue;
                }
                boolean registered = event.getTrangThai() == RegistrationStatus.DA_DANG_KY;
                boolean sectionCancelled = !registered && isSectionCancelledReason(event.getLyDoHuy());
                String eventType = registered
                        ? "COURSE_REGISTRATION_CONFIRMED"
                        : (sectionCancelled ? "COURSE_SECTION_CANCELLED" : "COURSE_REGISTRATION_CANCELLED");
                String idempotencyKey = buildRegistrationEventKey(
                        event.getDangKyId(), eventType, event.getThoiGianDangKy());
                CourseClass section = event.getLopHocPhan();
                String sectionCode = section.getMa();
                String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
                String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
                String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
                LocalDateTime occurredAt = event.getThoiDiem() != null
                        ? event.getThoiDiem() : event.getThoiGianDangKy();
                String reason = sectionCancelled ? cleanSectionCancelReason(event.getLyDoHuy()) : event.getLyDoHuy();
                String title = registered
                        ? "Đăng ký học phần thành công"
                        : (sectionCancelled ? "Lớp học phần bị hủy" : "Đã hủy đăng ký học phần");
                String message = buildStudentRegistrationMessage(
                        registered, sectionCancelled, sectionCode, subjectCode, subjectName, semesterName, occurredAt, reason);
                String priority = sectionCancelled ? "HIGH" : "NORMAL";
                String targetUrl = registered
                        ? "/hoc-phan-cua-toi"
                        : (sectionCancelled ? "/dang-ky" : "/thoi-khoa-bieu");
                if (send(studentUser, eventType, idempotencyKey, title, message, priority, targetUrl, occurredAt)) {
                    recovered++;
                }
            }
        }

        if (diemRepository != null) {
            List<Grade> publishedGrades = diemRepository.findPublishedGradesForStudentUser(userId);
            for (Grade diem : publishedGrades) {
                CourseRegistration reg = diem.getDangKy();
                if (reg == null || reg.getTrangThai() != RegistrationStatus.DA_DANG_KY
                        || reg.getLopHocPhan() == null || reg.getSinhVien() == null) {
                    continue;
                }
                User studentUser = reg.getSinhVien().getUser();
                if (studentUser == null || !studentUser.isTrangThai()) {
                    continue;
                }
                CourseClass section = reg.getLopHocPhan();
                if (!section.isBangDiemDaCongBo() || section.getCongBoBangDiemLuc() == null) {
                    continue;
                }
                String idempotencyKey = buildGradePublishedEventKey(section.getId(), section.getPhienBanCongBo());
                String sectionCode = section.getMa();
                String subjectCode = section.getMonHoc() != null ? section.getMonHoc().getMa() : "";
                String subjectName = section.getMonHoc() != null ? section.getMonHoc().getTen() : "";
                String semesterName = section.getHocKy() != null ? section.getHocKy().getTen() : "";
                String academicYear = section.getHocKy() != null ? section.getHocKy().getNamHoc() : "";
                LocalDateTime publishedAt = section.getCongBoBangDiemLuc();
                String title = "Điểm lớp học phần " + sectionCode + " đã được công bố";
                String message = buildGradePublishedMessage(
                        sectionCode, subjectCode, subjectName, semesterName, academicYear, publishedAt);
                if (send(studentUser, GRADES_PUBLISHED_EVENT, idempotencyKey,
                        title, message, "HIGH", "/diem", publishedAt)) {
                    recovered++;
                }
            }
        }

        return recovered;
    }

    private void reconcileForUserSafely(Long userId) {
        if (userId == null || (lichSuDangKyRepository == null && diemRepository == null)) {
            return;
        }
        try {
            if (requiresNewTemplate != null) {
                requiresNewTemplate.executeWithoutResult(status -> reconcileForUser(userId));
            } else {
                reconcileForUser(userId);
            }
        } catch (RuntimeException ex) {
            log.warn("Không thể đồng bộ bù thông báo nội bộ cho userId={}: {}",
                    userId, ex.getClass().getSimpleName());
        }
    }

    private void dispatchAfterCommit(String context, Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeSafelyInNewTransaction(context, task);
                }
            });
        } else {
            executeSafelyInNewTransaction(context, task);
        }
    }

    private void executeSafelyInNewTransaction(String context, Runnable task) {
        try {
            if (requiresNewTemplate != null) {
                requiresNewTemplate.executeWithoutResult(status -> task.run());
            } else {
                task.run();
            }
        } catch (RuntimeException ex) {
            log.error("Không thể lưu thông báo nội bộ [{}]; giao dịch nghiệp vụ đã commit không bị ảnh hưởng: {}",
                    context, ex.getClass().getSimpleName());
        }
    }

    private void sendBatchByIdempotencyKey(List<User> recipients, String eventType, String idempotencyKey,
                                           String title, String message, String priority,
                                           String targetUrl, LocalDateTime createdAt) {
        List<User> activeRecipients = recipients.stream()
                .filter(u -> u != null && u.isTrangThai() && u.getId() != null)
                .toList();
        if (activeRecipients.isEmpty()) {
            return;
        }
        Set<Long> existingRecipientIds = thongBaoRepository.findExistingRecipientIdsByIdempotencyKey(
                idempotencyKey, activeRecipients.stream().map(User::getId).toList());
        for (User recipient : activeRecipients) {
            if (existingRecipientIds != null && existingRecipientIds.contains(recipient.getId())) {
                continue;
            }
            saveNotification(recipient, eventType, idempotencyKey, title, message, priority, targetUrl, createdAt);
        }
    }

    private boolean send(User recipient, String eventType, String idempotencyKey, String title,
                         String message, String priority, String targetUrl, LocalDateTime createdAt) {
        if (recipient == null || !recipient.isTrangThai()
                || thongBaoRepository.existsByRecipientIdAndIdempotencyKey(recipient.getId(), idempotencyKey)) {
            return false;
        }
        try {
            return saveNotification(recipient, eventType, idempotencyKey, title, message, priority, targetUrl, createdAt);
        } catch (DataAccessResourceFailureException e) {
            log.warn("Failed to persist notification [{}] for recipientId={}: {}", eventType, recipient.getId(), e.getMessage());
            return false;
        }
    }

    private boolean saveNotification(User recipient, String eventType, String idempotencyKey, String title,
                                     String message, String priority, String targetUrl, LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setEventType(eventType);
        notification.setIdempotencyKey(idempotencyKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setPriority(priority);
        notification.setTargetUrl(targetUrl);
        notification.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        try {
            thongBaoRepository.save(notification);
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Bỏ qua thông báo trùng idempotencyKey={} cho recipientId={}",
                    idempotencyKey, recipient.getId());
            return false;
        }
    }

    public static String buildRegistrationEventKey(Long registrationId, String eventType, LocalDateTime registeredAt) {
        LocalDateTime normalized = registeredAt != null ? registeredAt.truncatedTo(ChronoUnit.SECONDS) : null;
        return "registration:" + registrationId + ":" + eventType + ":" + normalized;
    }

    public static String buildGradePublishedEventKey(Long sectionId, int publicationVersion) {
        int safeVersion = Math.max(1, publicationVersion);
        return "grade-published:" + sectionId + ":v" + safeVersion;
    }

    public static String deterministicSectionChangeKey(Long sectionId, String eventType, Map<String, Object> after) {
        if (after == null) {
            return "section-update:" + sectionId + ":" + eventType;
        }
        return "section-update:" + sectionId + ":" + eventType + ":"
                + after.get("phong") + ":" + after.get("thu") + ":"
                + after.get("tietBatDau") + ":" + after.get("soTiet") + ":"
                + after.get("giangVienId");
    }

    private String buildGradePublishedMessage(String sectionCode, String subjectCode, String subjectName,
                                              String semesterName, String academicYear, LocalDateTime occurredAt) {
        String subjectLabel = (subjectCode == null || subjectCode.isBlank())
                ? subjectName
                : subjectCode + " - " + subjectName;
        String semesterLabel = (academicYear == null || academicYear.isBlank())
                ? semesterName
                : semesterName + " (" + academicYear + ")";
        return "Bảng điểm lớp " + sectionCode + " — " + subjectLabel + ", " + semesterLabel
                + " đã được phê duyệt và công bố lúc " + formatTime(occurredAt)
                + ". Bước tiếp theo: truy cập trang Kết quả học tập để xem chi tiết.";
    }

    private String buildStudentRegistrationMessage(boolean registered, boolean sectionCancelled,
                                                   String sectionCode, String subjectCode,
                                                   String subjectName, String semesterName,
                                                   LocalDateTime occurredAt, String reason) {
        String subjectLabel = (subjectCode == null || subjectCode.isBlank())
                ? subjectName
                : subjectCode + " - " + subjectName;
        String timeStr = formatTime(occurredAt);
        if (registered) {
            return "Bạn đã đăng ký lớp " + sectionCode + " — " + subjectLabel
                    + ", " + semesterName + " lúc " + timeStr
                    + ". Kết quả: Đã đăng ký. Bước tiếp theo: kiểm tra danh sách học phần đã đăng ký và thời khóa biểu.";
        }
        if (sectionCancelled) {
            return "Lớp học phần " + sectionCode + " — " + subjectLabel
                    + ", " + semesterName + " đã bị hủy lúc " + timeStr
                    + ". Kết quả: Trạng thái đăng ký chuyển sang Đã hủy" + formatReason(reason)
                    + ". Bước tiếp theo: mở trang đăng ký học phần để chọn lớp thay thế.";
        }
        return "Bạn đã hủy đăng ký lớp " + sectionCode + " — " + subjectLabel
                + ", " + semesterName + " lúc " + timeStr
                + ". Kết quả: Đã hủy đăng ký" + formatReason(reason)
                + ". Bước tiếp theo: kiểm tra lại thời khóa biểu hoặc đăng ký lớp học phần khác nếu cần.";
    }

    private boolean isSectionCancelledReason(String reason) {
        return reason != null && reason.startsWith(SECTION_CANCELLED_REASON_PREFIX);
    }

    private String cleanSectionCancelReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        if (trimmed.equals(SECTION_CANCELLED_REASON_PREFIX)) {
            return null;
        }
        if (trimmed.startsWith(SECTION_CANCELLED_REASON_PREFIX + ":")) {
            String detail = trimmed.substring((SECTION_CANCELLED_REASON_PREFIX + ":").length()).trim();
            return detail.isEmpty() ? null : detail;
        }
        return trimmed;
    }

    private String formatReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " (Lý do: " + reason.trim() + ")";
    }

    private String formatTime(LocalDateTime time) {
        return (time != null ? time : LocalDateTime.now()).format(TIME_FORMATTER);
    }

    private boolean same(Map<String, Object> before, Map<String, Object> after, String key) {
        return java.util.Objects.equals(before.get(key), after.get(key));
    }

    private String thuLabel(int thu) {
        return thu == 8 ? "Chủ nhật" : "Thứ " + (thu - 1);
    }

    private NotificationDtos.ThongBaoDto toDto(Notification notification) {
        boolean read = notification.getReadAt() != null;
        return new NotificationDtos.ThongBaoDto(notification.getId(), notification.getEventType(),
                notification.getTitle(), notification.getMessage(), notification.getPriority(),
                notification.getTargetUrl(), notification.getCreatedAt(), notification.getReadAt(), read);
    }
}
