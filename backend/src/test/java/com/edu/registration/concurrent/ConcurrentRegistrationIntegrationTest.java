package com.edu.registration.concurrent;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.AuditLog;
import com.edu.registration.entity.Notification;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.User;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.AuditLogRepository;
import com.edu.registration.repository.NotificationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.security.JwtService;
import com.edu.registration.service.NotificationService;
import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.repository.ClassroomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import org.springframework.dao.DataAccessResourceFailureException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentRegistrationIntegrationTest {

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry properties) {
        String jdbcUrl = System.getProperty("registration.test.jdbc-url",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_URL",
                        "jdbc:h2:mem:registration_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"));
        boolean mysql = jdbcUrl.startsWith("jdbc:mysql:");
        properties.add("spring.datasource.url", () -> jdbcUrl);
        properties.add("spring.datasource.username", () -> System.getProperty("registration.test.db-username",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_USERNAME", "sa")));
        properties.add("spring.datasource.password", () -> System.getProperty("registration.test.db-password",
                System.getenv().getOrDefault("REGISTRATION_TEST_DB_PASSWORD", "")));
        properties.add("spring.datasource.driver-class-name", () -> mysql
                ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        properties.add("app.seed.enabled", () -> "true");
    }

    @Autowired private TestRestTemplate http;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository sinhVienRepository;
    @Autowired private FacultyRepository khoaRepository;
    @Autowired private SemesterRepository hocKyRepository;
    @Autowired private SubjectRepository monHocRepository;
    @Autowired private PrerequisiteSubjectRepository monTienQuyetRepository;
    @Autowired private LecturerRepository giangVienRepository;
    @Autowired private CourseClassRepository lopHocPhanRepository;
    @Autowired private ClassroomRepository classroomRepository;
    @Autowired private CourseRegistrationRepository dangKyRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @SpyBean private NotificationRepository thongBaoRepository;
    @Autowired private NotificationService thongBaoService;
    @SpyBean private AuditLogRepository auditLogRepositorySpy;
    @Autowired private JwtService jwtService;
    @Autowired private DataSource dataSource;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;

    @Value("${registration.test.capacity:3}") private int capacity;
    @Value("${registration.test.student-count:12}") private int studentCount;
    @Value("${registration.test.concurrency:12}") private int concurrency;

    private String runId;
    private List<User> students;
    private CourseClass primarySection;
    private CourseClass secondSection;

    @BeforeEach
    void createIsolatedFixture() {
        assertTrue(capacity > 0, "capacity phải lớn hơn 0");
        assertTrue(studentCount > capacity, "student-count phải lớn hơn capacity để có tranh suất");
        assertTrue(concurrency > 0, "concurrency phải lớn hơn 0");
        runId = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

// Tạo phòng học đang hoạt động để phục vụ test cập nhật lịch học
        classroomRepository.saveAndFlush(new Classroom(
                "N" + runId,
                "Phòng kiểm thử " + runId,
                "Nhà A",
                1,
                null,
                60,
                "LY_THUYET",
                null,
                ClassroomStatus.HOAT_DONG,
                "Tốt"
        ));

        Faculty khoa = new Faculty();
        khoa.setMa("T" + runId);
        khoa.setTen("Khoa concurrency test " + runId);
        khoa = khoaRepository.saveAndFlush(khoa);

        Semester hocKy = new Semester();
        hocKy.setTen("Concurrency test " + runId);
        hocKy.setNamHoc("2099-2100");
        hocKy.setNgayBatDau(LocalDate.now().minusDays(1));
        hocKy.setNgayKetThuc(LocalDate.now().plusDays(30));
        hocKy.setDangKyTu(LocalDate.now().minusDays(1));
        hocKy.setDangKyDen(LocalDate.now().plusDays(30));
        hocKy.setSoTinChiToiThieu(0);
        hocKy.setSoTinChiToiDa(24);
        hocKy = hocKyRepository.saveAndFlush(hocKy);

        Subject mon = new Subject();
        mon.setMa("T" + runId);
        mon.setTen("Môn concurrency test " + runId);
        mon.setSoTinChi(3);
        mon.setKhoa(khoa);
        mon = monHocRepository.saveAndFlush(mon);

        User lecturerUser = new User();
        lecturerUser.setUsername("test-gv-" + runId);
        lecturerUser.setPassword("unused");
        lecturerUser.setRole(Role.GIANG_VIEN);
        lecturerUser = userRepository.saveAndFlush(lecturerUser);
        Lecturer lecturer = new Lecturer();
        lecturer.setUser(lecturerUser);
        lecturer.setMa("GV" + runId);
        lecturer.setHoTen("Giảng viên test");
        lecturer = giangVienRepository.saveAndFlush(lecturer);

        primarySection = saveSection("01", mon, hocKy, lecturer, capacity, 2);
        secondSection = saveSection("02", mon, hocKy, lecturer, capacity, 4);

        students = new ArrayList<>();
        int fixtureCount = Math.max(studentCount, 3);
        for (int i = 0; i < fixtureCount; i++) {
            User user = new User();
            user.setUsername("test-sv-" + runId + "-" + i);
            user.setPassword("unused");
            user.setRole(Role.SINH_VIEN);
            user = userRepository.saveAndFlush(user);
            Student student = new Student();
            student.setUser(user);
            student.setMa("SV" + runId + i);
            student.setHoTen("Sinh viên test " + i);
            sinhVienRepository.saveAndFlush(student);
            students.add(user);
        }
    }

    @Test
    void concurrentLastSeatRaceNeverOverbooksAndWritesEvidenceReport() throws Exception {
        int requests = studentCount;
        List<Attempt> attempts = concurrentRequests(requests, index ->
                postRegistration(students.get(index), primarySection.getId()));

        long successes = attempts.stream().filter(Attempt::success).count();
        long conflicts = attempts.stream().filter(attempt -> attempt.status() == 409).count();
        entityManager.clear();
        CourseClass storedSection = lopHocPhanRepository.findById(primarySection.getId()).orElseThrow();
        long activeRows = dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY);

        assertEquals(Math.min(capacity, requests), successes, "Số đăng ký thành công phải lấp đúng sức chứa");
        assertEquals(requests - successes, conflicts, "Yêu cầu thua cuộc phải nhận HTTP 409 nghiệp vụ");
        assertTrue(successes <= capacity, "Không được vượt sức chứa");
        assertEquals(successes, activeRows, "Số bản ghi đăng ký phải khớp số request thành công");
        assertEquals(activeRows, storedSection.getSiSoHienTai(), "Sĩ số lưu phải khớp bản ghi đang hoạt động");

        writeReport(attempts, requests, successes, conflicts, storedSection.getSiSoHienTai(), activeRows);
    }

    @Test
    void sameStudentCannotRegisterTwoSectionsOfSameCourseConcurrently() throws Exception {
        List<Attempt> attempts = concurrentRequests(2, index -> postRegistration(
                students.get(0), index == 0 ? primarySection.getId() : secondSection.getId()));

        assertEquals(1, attempts.stream().filter(Attempt::success).count());
        assertEquals(1, attempts.stream().filter(attempt -> attempt.status() == 409).count());
        assertEquals(1, dangKyRepository.findTheoHocKy(
                sinhVienRepository.findByUser(students.get(0)).orElseThrow().getId(),
                primarySection.getHocKy().getId(), RegistrationStatus.DA_DANG_KY).size());
    }

    @Test
    void duplicateRequestForSameStudentAndSectionReturnsConflict() {
        ResponseEntity<String> first = postRegistration(students.get(0), primarySection.getId());
        ResponseEntity<String> duplicate = postRegistration(students.get(0), primarySection.getId());

        assertEquals(HttpStatus.OK, first.getStatusCode());
        assertEquals(HttpStatus.CONFLICT, duplicate.getStatusCode());
        assertEquals(1, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        entityManager.clear();
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void cancellationReopensSeatAndKeepsCancelledRecord() {
        ResponseEntity<String> registered = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, registered.getStatusCode());
        Long registrationId = readId(registered.getBody());
        HttpHeaders headers = headersFor(students.get(0));
        ResponseEntity<Void> cancelled = http.exchange("/api/dang-ky/" + registrationId,
                HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.OK, cancelled.getStatusCode());
        entityManager.clear();
        CourseRegistration row = dangKyRepository.findById(registrationId).orElseThrow();
        CourseClass section = lopHocPhanRepository.findById(primarySection.getId()).orElseThrow();
        assertEquals(RegistrationStatus.DA_HUY, row.getTrangThai());
        assertEquals(0, section.getSiSoHienTai());
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));

        ResponseEntity<String> retried = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, retried.getStatusCode());
        assertEquals(registrationId, readId(retried.getBody()), "Đăng ký lại phải kích hoạt bản ghi đã hủy");
        entityManager.clear();
        assertEquals(RegistrationStatus.DA_DANG_KY,
                dangKyRepository.findById(registrationId).orElseThrow().getTrangThai());
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void confirmsAValidPlanThroughTheOfficialRegistrationFlow() throws Exception {
        ResponseEntity<String> response = confirmPlan(students.get(0), List.of(primarySection.getId()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        entityManager.clear();
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void invalidCoursePlanRollsBackEarlierItemsAtomically() throws Exception {
        ResponseEntity<String> response = confirmPlan(students.get(0),
                List.of(primarySection.getId(), secondSection.getId()));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("đã đăng ký môn"));
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                secondSection.getId(), RegistrationStatus.DA_DANG_KY));
        entityManager.clear();
        assertEquals(0, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
        assertEquals(0, lopHocPhanRepository.findById(secondSection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void confirmationRechecksPrerequisitesOnTheServer() throws Exception {
        Subject prerequisite = new Subject();
        prerequisite.setMa("P" + runId);
        prerequisite.setTen("Môn tiên quyết test " + runId);
        prerequisite.setSoTinChi(3);
        prerequisite.setKhoa(primarySection.getMonHoc().getKhoa());
        prerequisite = monHocRepository.saveAndFlush(prerequisite);
        monTienQuyetRepository.saveAndFlush(new com.edu.registration.entity.PrerequisiteSubject(
                primarySection.getMonHoc(), prerequisite));

        ResponseEntity<String> response = confirmPlan(students.get(0), List.of(primarySection.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("tiên quyết"));
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));
        entityManager.clear();
        assertEquals(0, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void confirmationRejectsASectionThatFilledAfterItWasAddedToThePlan() throws Exception {
        primarySection.setSiSoHienTai(capacity);
        lopHocPhanRepository.saveAndFlush(primarySection);

        ResponseEntity<String> response = confirmPlan(students.get(0), List.of(primarySection.getId()));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("đủ sĩ số"));
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        entityManager.clear();
        assertEquals(capacity, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    @Test
    void successfulRegistrationAuditsTheServerActorTargetAndCorrelationId() {
        HttpHeaders headers = headersFor(students.get(0));
        headers.set("X-Correlation-ID", "registration-audit-test");
        ResponseEntity<String> response = http.exchange("/api/dang-ky", HttpMethod.POST,
                new HttpEntity<>(Map.of("lopHocPhanId", primarySection.getId()), headers), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuditLog event = auditLogRepository.findAll().stream()
                .filter(row -> "COURSE_REGISTRATION".equals(row.getAction()))
                .filter(row -> students.get(0).getUsername().equals(row.getActorUsername()))
                .findFirst().orElseThrow();
        assertEquals(students.get(0).getId(), event.getActorUserId());
        assertEquals("SINH_VIEN", event.getActorRole());
        assertEquals("COURSE_SECTION", event.getEntityType());
        assertEquals(primarySection.getId().toString(), event.getEntityId());
        assertEquals("SUCCESS", event.getResult());
        assertEquals("registration-audit-test", event.getCorrelationId());
        assertTrue(event.getDetailsJson().contains("DA_DANG_KY"));
        assertFalse(event.getDetailsJson().contains("password"));
        assertFalse(event.getDetailsJson().contains("token"));
    }

    @Test
    void registrationCreatesIdempotentNoticesForTheStudentAndResponsibleLecturer() {
        assertEquals(HttpStatus.OK, postRegistration(students.get(0), primarySection.getId()).getStatusCode());

        List<Notification> studentNotices = thongBaoRepository.findByRecipientId(students.get(0).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        List<Notification> lecturerNotices = thongBaoRepository.findByRecipientId(
                primarySection.getGiangVien().getUser().getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        assertEquals(1, studentNotices.stream().filter(n -> n.getEventType().equals("COURSE_REGISTRATION_CONFIRMED")).count());
        assertEquals(1, lecturerNotices.stream().filter(n -> n.getEventType().equals("COURSE_REGISTRATION_CONFIRMED")).count());
        assertTrue(studentNotices.get(0).getTargetUrl().startsWith("/"));
        assertFalse(studentNotices.get(0).getMessage().contains(students.get(0).getUsername()));
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(1).getId()));

        assertEquals(HttpStatus.CONFLICT, postRegistration(students.get(0), primarySection.getId()).getStatusCode());
        assertEquals(1, thongBaoRepository.findByRecipientId(students.get(0).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent().stream()
                .filter(n -> n.getEventType().equals("COURSE_REGISTRATION_CONFIRMED")).count());
    }

    @Test
    void cancellationCreatesNotificationAndRetryDoesNotDuplicateAndFailedRegistrationCreatesNoNotification() {
        // Failed registration on non-existent class or full class must not create any notification
        assertEquals(HttpStatus.NOT_FOUND, postRegistration(students.get(0), Long.MAX_VALUE).getStatusCode());
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));

        ResponseEntity<String> registered = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, registered.getStatusCode());
        Long registrationId = readId(registered.getBody());

        HttpHeaders headers = headersFor(students.get(0));
        ResponseEntity<String> cancelled = http.exchange(
                "/api/dang-ky/" + registrationId + "?lyDo=Đổi kế hoạch học tập",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, cancelled.getStatusCode());

        // Retrying the same cancellation returns 400 and does not duplicate the cancellation notice
        ResponseEntity<String> retryCancel = http.exchange(
                "/api/dang-ky/" + registrationId + "?lyDo=Đổi kế hoạch học tập",
                HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, retryCancel.getStatusCode());

        List<Notification> notices = thongBaoRepository.findByRecipientId(students.get(0).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent();
        List<Notification> cancelNotices = notices.stream()
                .filter(n -> "COURSE_REGISTRATION_CANCELLED".equals(n.getEventType()))
                .toList();
        assertEquals(1, cancelNotices.size());
        assertEquals("Đã hủy đăng ký học phần", cancelNotices.get(0).getTitle());
        assertEquals("/thoi-khoa-bieu", cancelNotices.get(0).getTargetUrl());
        assertTrue(cancelNotices.get(0).getMessage().contains(primarySection.getMa()));
        assertTrue(cancelNotices.get(0).getMessage().contains("Kết quả: Đã hủy đăng ký"));
        assertTrue(cancelNotices.get(0).getMessage().contains("Đổi kế hoạch học tập"));
        assertTrue(cancelNotices.get(0).getMessage().contains("Bước tiếp theo:"));
    }

    @Test
    void courseSectionCancellationTransitionsActiveRegistrationsAndNotifiesOnlyCurrentlyAffectedStudents() {
        // Student 0 and Student 1 register; Student 1 cancels before section is cancelled
        ResponseEntity<String> reg0 = postRegistration(students.get(0), primarySection.getId());
        ResponseEntity<String> reg1 = postRegistration(students.get(1), primarySection.getId());
        assertEquals(HttpStatus.OK, reg0.getStatusCode());
        assertEquals(HttpStatus.OK, reg1.getStatusCode());
        Long reg0Id = readId(reg0.getBody());
        Long reg1Id = readId(reg1.getBody());

        assertEquals(HttpStatus.OK, http.exchange("/api/dang-ky/" + reg1Id,
                HttpMethod.DELETE, new HttpEntity<>(headersFor(students.get(1))), String.class).getStatusCode());

        User admin = userRepository.findByUsername("admin").orElseThrow();
        HttpEntity<Map<String, String>> cancelSectionReq = new HttpEntity<>(
                Map.of("lyDo", "Không đủ số lượng mở lớp"), headersFor(admin));
        ResponseEntity<String> cancelSectionResp = http.exchange(
                "/api/admin/lop-hoc-phan/" + primarySection.getId() + "/huy",
                HttpMethod.POST, cancelSectionReq, String.class);
        assertEquals(HttpStatus.OK, cancelSectionResp.getStatusCode());
        assertTrue(cancelSectionResp.getBody().contains("\"soDangKyBiHuy\":1"));

        // Retry section cancellation is idempotent
        ResponseEntity<String> retrySectionCancel = http.exchange(
                "/api/admin/lop-hoc-phan/" + primarySection.getId() + "/huy",
                HttpMethod.POST, cancelSectionReq, String.class);
        assertEquals(HttpStatus.OK, retrySectionCancel.getStatusCode());
        assertTrue(retrySectionCancel.getBody().contains("\"soDangKyBiHuy\":0"));

        entityManager.clear();
        assertEquals(RegistrationStatus.DA_HUY, dangKyRepository.findById(reg0Id).orElseThrow().getTrangThai());
        assertEquals(0, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());

        // Student 0 (who was active at cancellation time) receives COURSE_SECTION_CANCELLED
        List<Notification> student0SectionNotices = thongBaoRepository.findByRecipientId(students.get(0).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent().stream()
                .filter(n -> "COURSE_SECTION_CANCELLED".equals(n.getEventType()))
                .toList();
        assertEquals(1, student0SectionNotices.size());
        assertEquals("HIGH", student0SectionNotices.get(0).getPriority());
        assertEquals("/dang-ky", student0SectionNotices.get(0).getTargetUrl());
        assertTrue(student0SectionNotices.get(0).getMessage().contains(primarySection.getMa()));
        assertTrue(student0SectionNotices.get(0).getMessage().contains("Không đủ số lượng mở lớp"));

        // Student 1 (already cancelled beforehand) and Student 2 (never registered) do NOT receive COURSE_SECTION_CANCELLED
        assertEquals(0, thongBaoRepository.findByRecipientId(students.get(1).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent().stream()
                .filter(n -> "COURSE_SECTION_CANCELLED".equals(n.getEventType())).count());
        assertEquals(0, thongBaoRepository.findByRecipientId(students.get(2).getId(),
                org.springframework.data.domain.PageRequest.of(0, 20)).getContent().stream()
                .filter(n -> "COURSE_SECTION_CANCELLED".equals(n.getEventType())).count());
    }

    @Test
    void notificationPersistenceFailureDoesNotRollbackRegistrationAndIsRecoveredOnList() {
        doThrow(new DataAccessResourceFailureException("notification table unavailable"))
                .when(thongBaoRepository).save(any(Notification.class));

        ResponseEntity<String> registered = postRegistration(students.get(0), primarySection.getId());
        assertEquals(HttpStatus.OK, registered.getStatusCode());
        Long registrationId = readId(registered.getBody());

        // Registration is durably committed even though notification save failed
        entityManager.clear();
        assertEquals(RegistrationStatus.DA_DANG_KY,
                dangKyRepository.findById(registrationId).orElseThrow().getTrangThai());
        assertEquals(1, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));

        // Restore notification repository and call GET /api/notifications -> reconciliation recovers the missing notice
        org.mockito.Mockito.reset(thongBaoRepository);
        ResponseEntity<String> listResp = http.exchange("/api/notifications", HttpMethod.GET,
                new HttpEntity<>(headersFor(students.get(0))), String.class);
        assertEquals(HttpStatus.OK, listResp.getStatusCode());
        assertTrue(listResp.getBody().contains("COURSE_REGISTRATION_CONFIRMED"));
        assertTrue(listResp.getBody().contains(primarySection.getMa()));
        assertEquals(1, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));
    }

    @Test
    void scheduleChangeNotifiesOnlyCurrentEnrolledStudentsAndInstructorAndNoOpDoesNotRepeat() {
        assertEquals(HttpStatus.OK, postRegistration(students.get(0), primarySection.getId()).getStatusCode());
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Map<String, Object> edit = Map.of(
                "ma", primarySection.getMa(),
                "monHocId", primarySection.getMonHoc().getId(),
                "hocKyId", primarySection.getHocKy().getId(),
                "giangVienId", primarySection.getGiangVien().getId(),
                "phong", "N" + runId,
                "thu", primarySection.getThu(),
                "tietBatDau", primarySection.getTietBatDau(),
                "soTiet", primarySection.getSoTiet(),
                "siSoToiDa", primarySection.getSiSoToiDa());
        HttpHeaders headers = headersFor(admin);
        String endpoint = "/api/admin/lop-hoc-phan/" + primarySection.getId();

        ResponseEntity<String> response = http.exchange(
                endpoint,
                HttpMethod.PUT,
                new HttpEntity<>(edit, headers),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "PUT lần 1 thất bại: " + response.getBody());

// Giữ nguyên PUT lần 2
        assertEquals(HttpStatus.OK, http.exchange(endpoint, HttpMethod.PUT,
                new HttpEntity<>(edit, headers), String.class).getStatusCode());

        assertEquals(1, thongBaoRepository.findByRecipientId(students.get(0).getId(),
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent().stream()
                .filter(n -> n.getEventType().equals("COURSE_SECTION_SCHEDULE_CHANGED")).count());
        assertEquals(1, thongBaoRepository.findByRecipientId(primarySection.getGiangVien().getUser().getId(),
                org.springframework.data.domain.PageRequest.of(0, 50)).getContent().stream()
                .filter(n -> n.getEventType().equals("COURSE_SECTION_SCHEDULE_CHANGED")).count());
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(1).getId()));
    }

    @Test
    void notificationReadsAndUpdatesAreRestrictedToTheAuthenticatedRecipient() {
        Notification own = saveNotice(students.get(0), "own-notice");
        Notification other = saveNotice(students.get(1), "other-notice");
        HttpEntity<Void> ownRequest = new HttpEntity<>(headersFor(students.get(0)));
        HttpEntity<Void> otherRequest = new HttpEntity<>(headersFor(students.get(1)));

        ResponseEntity<String> ownList = http.exchange("/api/notifications?unreadOnly=true", HttpMethod.GET,
                ownRequest, String.class);
        ResponseEntity<String> otherList = http.exchange("/api/notifications", HttpMethod.GET,
                otherRequest, String.class);
        assertEquals(HttpStatus.OK, ownList.getStatusCode());
        assertTrue(ownList.getBody().contains("own-notice"));
        assertFalse(ownList.getBody().contains("other-notice"));
        assertTrue(otherList.getBody().contains("other-notice"));
        assertFalse(otherList.getBody().contains("own-notice"));

        ResponseEntity<String> forbiddenUpdate = http.exchange("/api/notifications/" + other.getId() + "/read",
                HttpMethod.PATCH, ownRequest, String.class);
        assertEquals(HttpStatus.NOT_FOUND, forbiddenUpdate.getStatusCode());
        ResponseEntity<String> markRead = http.exchange("/api/notifications/" + own.getId() + "/read",
                HttpMethod.PATCH, ownRequest, String.class);
        assertEquals(HttpStatus.OK, markRead.getStatusCode());
        assertTrue(markRead.getBody().contains("\"read\":true"));
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));

        assertEquals(HttpStatus.FORBIDDEN, http.exchange("/api/notifications", HttpMethod.GET,
                new HttpEntity<>(headersFor(userRepository.findByUsername("admin").orElseThrow())), String.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/notifications", String.class).getStatusCode());
    }

    @Test
    void registrationWindowReminderIsOnlyForActiveStudentsAndIsIdempotent() {
        primarySection.getHocKy().setDangKyDen(LocalDate.now().plusDays(1));
        hocKyRepository.saveAndFlush(primarySection.getHocKy());

        int created = thongBaoService.remindClosingRegistrationWindow(primarySection.getHocKy().getId(), LocalDate.now());
        int repeated = thongBaoService.remindClosingRegistrationWindow(primarySection.getHocKy().getId(), LocalDate.now());

        assertEquals(sinhVienRepository.findAllActiveWithUser().size(), created);
        assertEquals(0, repeated);
        assertEquals(1, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(
                primarySection.getGiangVien().getUser().getId()));
    }

    @Test
    void adminActionAnalyticsUsesActiveRegistrationsAndSupportsSemesterAndFacultyFilters() {
        Subject lowEnrollmentSubject = new Subject();
        lowEnrollmentSubject.setMa("L" + runId);
        lowEnrollmentSubject.setTen("Môn ít đăng ký " + runId);
        lowEnrollmentSubject.setSoTinChi(3);
        lowEnrollmentSubject.setKhoa(primarySection.getMonHoc().getKhoa());
        lowEnrollmentSubject = monHocRepository.saveAndFlush(lowEnrollmentSubject);
        secondSection.setMonHoc(lowEnrollmentSubject);
        lopHocPhanRepository.saveAndFlush(secondSection);

        Long registrationId = null;
        for (int i = 0; i < capacity; i++) {
            ResponseEntity<String> registered = postRegistration(students.get(i), primarySection.getId());
            assertEquals(HttpStatus.OK, registered.getStatusCode());
            if (i == 0) registrationId = readId(registered.getBody());
        }
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String endpoint = "/api/admin/thong-ke/hanh-dong?hocKyId=" + primarySection.getHocKy().getId();
        HttpEntity<Void> adminRequest = new HttpEntity<>(headersFor(admin));
        ResponseEntity<String> response = http.exchange(endpoint, HttpMethod.GET, adminRequest, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("LOP_GAN_DAY"));
        assertTrue(response.getBody().contains("MON_HET_CHO"));
        assertTrue(response.getBody().contains("LOP_IT_DANG_KY"));
        assertTrue(response.getBody().contains("\"daDangKy\":" + capacity));
        assertTrue(response.getBody().contains("\"tongChiTieu\":" + capacity));
        assertTrue(response.getBody().contains("/quan-tri/lop-hoc-phan?hocKyId="));

        assertEquals(HttpStatus.OK, http.exchange("/api/dang-ky/" + registrationId, HttpMethod.DELETE,
                new HttpEntity<>(headersFor(students.get(0))), String.class).getStatusCode());
        ResponseEntity<String> afterCancel = http.exchange(endpoint, HttpMethod.GET, adminRequest, String.class);
        assertFalse(afterCancel.getBody().contains("\"loai\":\"LOP_GAN_DAY\""));
        assertFalse(afterCancel.getBody().contains("\"loai\":\"MON_HET_CHO\""));
        assertTrue(afterCancel.getBody().contains("LOP_IT_DANG_KY"));

        Faculty otherFaculty = new Faculty();
        otherFaculty.setMa("F" + runId);
        otherFaculty.setTen("Khoa không có lớp " + runId);
        otherFaculty = khoaRepository.saveAndFlush(otherFaculty);
        ResponseEntity<String> filtered = http.exchange(endpoint + "&khoaId=" + otherFaculty.getId(),
                HttpMethod.GET, adminRequest, String.class);
        assertEquals(HttpStatus.OK, filtered.getStatusCode());
        assertTrue(filtered.getBody().contains("Khoa không có lớp"));
        assertTrue(filtered.getBody().contains("\"canhBao\":[]"));
        ResponseEntity<String> filteredSubjectStats = http.exchange(
                "/api/admin/thong-ke/theo-mon?hocKyId=" + primarySection.getHocKy().getId()
                        + "&khoaId=" + otherFaculty.getId(), HttpMethod.GET, adminRequest, String.class);
        assertEquals(HttpStatus.OK, filteredSubjectStats.getStatusCode());
        assertEquals("[]", filteredSubjectStats.getBody());
        ResponseEntity<String> invalidFaculty = http.exchange(endpoint + "&khoaId=999999999", HttpMethod.GET,
                adminRequest, String.class);
        assertEquals(HttpStatus.NOT_FOUND, invalidFaculty.getStatusCode());

        ResponseEntity<String> studentResponse = http.exchange(endpoint, HttpMethod.GET,
                new HttpEntity<>(headersFor(students.get(0))), String.class);
        ResponseEntity<String> anonymousResponse = http.getForEntity(endpoint, String.class);
        assertEquals(HttpStatus.FORBIDDEN, studentResponse.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, anonymousResponse.getStatusCode());
    }

    @Test
    void adminActionAnalyticsReportsRegistrationWindowClosingOnlyWhenOpenAndWithinSevenDays() {
        primarySection.getHocKy().setDangKyDen(LocalDate.now().plusDays(3));
        hocKyRepository.saveAndFlush(primarySection.getHocKy());
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String endpoint = "/api/admin/thong-ke/hanh-dong?hocKyId=" + primarySection.getHocKy().getId();
        ResponseEntity<String> response = http.exchange(endpoint, HttpMethod.GET,
                new HttpEntity<>(headersFor(admin)), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("DOT_DANG_KY_SAP_DONG"));
        assertTrue(response.getBody().contains("Còn 3 ngày"));
        assertTrue(response.getBody().contains("/quan-tri/hoc-ky"));
    }

    private Notification saveNotice(User recipient, String title) {
        Notification notice = new Notification();
        notice.setRecipient(recipient);
        notice.setEventType("TEST_EVENT");
        notice.setIdempotencyKey(title);
        notice.setTitle(title);
        notice.setMessage("Nội dung kiểm thử");
        notice.setPriority("NORMAL");
        notice.setTargetUrl("/thong-bao");
        notice.setCreatedAt(LocalDateTime.now());
        return thongBaoRepository.saveAndFlush(notice);
    }

    @Test
    void failedRegistrationWritesFailureOutcomeAndSafeCode() {
        Long missingSectionId = Long.MAX_VALUE;
        ResponseEntity<String> response = postRegistration(students.get(0), missingSectionId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        AuditLog event = auditLogRepository.findAll().stream()
                .filter(row -> "COURSE_REGISTRATION".equals(row.getAction()))
                .filter(row -> students.get(0).getUsername().equals(row.getActorUsername()))
                .findFirst().orElseThrow();
        assertEquals("FAILURE", event.getResult());
        assertEquals(missingSectionId.toString(), event.getEntityId());
        assertTrue(event.getDetailsJson().contains("HTTP_404"));
        assertFalse(event.getDetailsJson().contains("Lớp học phần không tồn tại"));
    }

    @Test
    void studentCannotReadAdminAuditLogsAndAnonymousRequestIsRejected() {
        ResponseEntity<String> studentResponse = http.exchange("/api/admin/audit-logs", HttpMethod.GET,
                new HttpEntity<>(headersFor(students.get(0))), String.class);
        ResponseEntity<String> anonymousResponse = http.getForEntity("/api/admin/audit-logs", String.class);

        assertEquals(HttpStatus.FORBIDDEN, studentResponse.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, anonymousResponse.getStatusCode());
    }

    @Test
    void adminAccountStatusChangeAuditsOnlyMinimalStatusValues() throws Exception {
        User admin = userRepository.findByUsername("admin").orElseThrow();
        Student target = sinhVienRepository.findByUser(students.get(0)).orElseThrow();
        HttpEntity<Map<String, Boolean>> request = new HttpEntity<>(Map.of("trangThai", false), headersFor(admin));

        ResponseEntity<String> response = http.exchange("/api/admin/sinh-vien/" + target.getId() + "/trang-thai",
                HttpMethod.PATCH, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuditLog event = auditLogRepository.findAll().stream()
                .filter(row -> "STUDENT_ACCOUNT_STATUS_CHANGED".equals(row.getAction()))
                .filter(row -> admin.getUsername().equals(row.getActorUsername()))
                .findFirst().orElseThrow();
        assertEquals(target.getId().toString(), event.getEntityId());
        assertEquals("SUCCESS", event.getResult());
        assertTrue(event.getDetailsJson().contains("\"active\":true"));
        assertTrue(event.getDetailsJson().contains("\"active\":false"));
        assertFalse(event.getDetailsJson().contains("email"));
        assertFalse(event.getDetailsJson().contains("password"));

        ResponseEntity<String> filtered = http.exchange(
                "/api/admin/audit-logs?action=STUDENT_ACCOUNT_STATUS_CHANGED&actor=admin&entityType=STUDENT_ACCOUNT&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(headersFor(admin)), String.class);
        assertEquals(HttpStatus.OK, filtered.getStatusCode());
        assertTrue(filtered.getBody().contains("STUDENT_ACCOUNT_STATUS_CHANGED"));
        assertTrue(filtered.getBody().contains(target.getId().toString()));
    }

    @Test
    void auditWriteFailureRollsBackRegistrationInsteadOfReturningSuccess() {
        doThrow(new DataAccessResourceFailureException("audit storage unavailable"))
                .when(auditLogRepositorySpy).saveAndFlush(any(AuditLog.class));

        ResponseEntity<String> response = postRegistration(students.get(0), primarySection.getId());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(0, dangKyRepository.countByLopHocPhanIdAndTrangThai(
                primarySection.getId(), RegistrationStatus.DA_DANG_KY));
        assertEquals(0, thongBaoRepository.countByRecipientIdAndReadAtIsNull(students.get(0).getId()));
        entityManager.clear();
        assertEquals(0, lopHocPhanRepository.findById(primarySection.getId()).orElseThrow().getSiSoHienTai());
    }

    private CourseClass saveSection(String suffix, Subject mon, Semester term, Lecturer lecturer,
                                   int max, int weekday) {
        CourseClass section = new CourseClass();
        section.setMa("T" + runId + "." + suffix);
        section.setMonHoc(mon);
        section.setHocKy(term);
        section.setGiangVien(lecturer);
        section.setPhong("TEST");
        section.setThu(weekday);
        section.setTietBatDau(1);
        section.setSoTiet(2);
        section.setSiSoToiDa(max);
        section.setSiSoHienTai(0);
        return lopHocPhanRepository.saveAndFlush(section);
    }

    private List<Attempt> concurrentRequests(int requestCount, java.util.function.IntFunction<ResponseEntity<String>> action)
            throws Exception {
        int workers = Math.max(1, Math.min(concurrency, requestCount));
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<CompletableFuture<Attempt>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                final int index = i;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        long started = System.nanoTime();
                        ResponseEntity<String> response = action.apply(index);
                        return new Attempt(response.getStatusCode().value(),
                                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started), response.getBody());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                }, executor));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return futures.stream().map(CompletableFuture::join).toList();
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<String> postRegistration(User student, Long sectionId) {
        return http.exchange("/api/dang-ky", HttpMethod.POST,
                new HttpEntity<>(Map.of("lopHocPhanId", sectionId), headersFor(student)), String.class);
    }

    private ResponseEntity<String> confirmPlan(User student, List<Long> sectionIds) {
        return http.exchange("/api/dang-ky/xac-nhan-ke-hoach", HttpMethod.POST,
                new HttpEntity<>(Map.of("lopHocPhanIds", sectionIds), headersFor(student)), String.class);
    }

    private HttpHeaders headersFor(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.generateToken(user));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Long readId(String response) {
        try {
            return objectMapper.readTree(response).get("id").asLong();
        } catch (Exception ex) {
            throw new AssertionError("Response đăng ký không có id: " + response, ex);
        }
    }

    private void writeReport(List<Attempt> attempts, int requests, long successes, long conflicts,
                             int storedCount, long activeRows) throws Exception {
        List<Long> latencies = attempts.stream().map(Attempt::latencyMs).sorted(Comparator.naturalOrder()).toList();
        String database;
        try (Connection connection = dataSource.getConnection()) {
            database = connection.getMetaData().getDatabaseProductName() + " "
                    + connection.getMetaData().getDatabaseProductVersion();
        }
        Map<String, Object> report = Map.of(
                "environment", Map.of("database", database, "profile", "integration-test",
                        "fixture", "isolated per test run"),
                "input", Map.of("capacity", capacity, "students", requests, "concurrency", Math.min(concurrency, requests),
                        "sectionId", primarySection.getId()),
                "result", Map.of("success", successes, "businessConflicts", conflicts,
                        "overCapacity", successes > capacity, "activeRegistrations", activeRows,
                        "storedClassCount", storedCount, "consistent", activeRows == storedCount),
                "latencyMs", Map.of("p50", percentile(latencies, 50), "p95", percentile(latencies, 95),
                        "p99", percentile(latencies, 99), "max", latencies.get(latencies.size() - 1)),
                "responses", attempts.stream().map(attempt -> Map.of("status", attempt.status(),
                        "latencyMs", attempt.latencyMs(), "body", attempt.body() == null ? "" : attempt.body())).toList());
        Path reportPath = Path.of(System.getProperty("registration.report", "target/concurrent-registration-report.json"));
        Files.createDirectories(reportPath.toAbsolutePath().getParent());
        Files.writeString(reportPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }

    private long percentile(List<Long> sorted, int percentile) {
        int index = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(percentile * sorted.size() / 100.0) - 1));
        return sorted.get(index);
    }

    private record Attempt(int status, long latencyMs, String body) {
        boolean success() { return status >= 200 && status < 300; }
    }
}
