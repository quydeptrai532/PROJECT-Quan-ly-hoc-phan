package com.edu.registration.config;

import com.edu.registration.entity.AcademicAlert;
import com.edu.registration.entity.AcademicAlertRuleType;
import com.edu.registration.entity.AcademicAlertSeverity;
import com.edu.registration.entity.AcademicAlertStatus;
import com.edu.registration.entity.AcademicCohort;
import com.edu.registration.entity.AdministrativeClass;
import com.edu.registration.entity.AttendanceRecord;
import com.edu.registration.entity.AttendanceStatus;
import com.edu.registration.entity.ClassSession;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Curriculum;
import com.edu.registration.entity.CurriculumGroup;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeAppeal;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.Notification;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.RegistrationPeriod;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.StudentAcademicStatus;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SupportRequest;
import com.edu.registration.entity.SupportRequestPriority;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestType;
import com.edu.registration.entity.User;
import com.edu.registration.repository.AcademicAlertRepository;
import com.edu.registration.repository.AcademicCohortRepository;
import com.edu.registration.repository.AdministrativeClassRepository;
import com.edu.registration.repository.AttendanceRecordRepository;
import com.edu.registration.repository.ClassSessionRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.GradeAppealRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.NotificationRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.repository.ClassroomRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.RegistrationPeriodRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.SupportRequestRepository;
import com.edu.registration.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final AdministrativeClassRepository lopHanhChinhRepository;
    private final SubjectRepository monHocRepository;
    private final PrerequisiteSubjectRepository monTienQuyetRepository;
    private final SemesterRepository hocKyRepository;
    private final LecturerRepository giangVienRepository;
    private final StudentRepository sinhVienRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final AcademicCohortRepository khoaHocRepository;
    private final RegistrationPeriodRepository dotDangKyRepository;
    private final ClassroomRepository phongHocRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private CurriculumRepository chuongTrinhRepository;
    @Autowired(required = false)
    private RegistrationHistoryRepository lichSuDangKyRepository;
    @Autowired(required = false)
    private ClassSessionRepository buoiHocRepository;
    @Autowired(required = false)
    private AttendanceRecordRepository diemDanhRepository;
    @Autowired(required = false)
    private SupportRequestRepository yeuCauHoTroRepository;
    @Autowired(required = false)
    private GradeAppealRepository phucKhaoRepository;
    @Autowired(required = false)
    private AcademicAlertRepository canhBaoHocTapRepository;
    @Autowired(required = false)
    private NotificationRepository thongBaoRepository;

    @Value("${app.seed.refresh-current-window:true}")
    private boolean refreshCurrentWindow;

    @Value("${app.seed.default-password}")
    private String defaultSeedPassword;

    @Value("${app.seed.admin-password}")
    private String adminSeedPassword;

    private static final String[] LAST_NAMES = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng",
            "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý", "Đinh", "Đoàn", "Lâm", "Trịnh",
            "Mai", "Đào", "Cao", "Hà", "Lương", "Thái", "Tạ", "Châu", "Nghiêm", "Vương"
    };

    private static final String[] MALE_MIDDLES = {
            "Văn", "Đức", "Minh", "Quang", "Hoàng", "Tuấn", "Đình", "Hữu", "Quốc", "Ngọc",
            "Thanh", "Huy", "Gia", "Bảo", "Khắc", "Trọng", "Tiến", "Công", "Xuân", "Nhật"
    };

    private static final String[] MALE_FIRSTS = {
            "An", "Bình", "Cường", "Dũng", "Đạt", "Đức", "Hải", "Hiếu", "Hoàng", "Hùng",
            "Huy", "Khánh", "Khoa", "Kiên", "Long", "Minh", "Nam", "Nghĩa", "Phong", "Phúc",
            "Quân", "Quang", "Sơn", "Thắng", "Thịnh", "Tiến", "Trung", "Tuấn", "Tùng", "Việt", "Vinh", "Vũ"
    };

    private static final String[] FEMALE_MIDDLES = {
            "Thị", "Ngọc", "Thu", "Phương", "Thảo", "Khánh", "Như", "Mai", "Quỳnh", "Diệu",
            "Mỹ", "Thùy", "Kim", "Bảo", "Hoài", "Hồng", "Thúy", "Tuyết", "Trúc", "Thanh"
    };

    private static final String[] FEMALE_FIRSTS = {
            "Anh", "Bình", "Dung", "Duyên", "Giang", "Hà", "Hân", "Hằng", "Hoa", "Hương",
            "Huyền", "Lam", "Lan", "Linh", "Mai", "My", "Nga", "Ngân", "Ngọc", "Nhung",
            "Oanh", "Phương", "Quỳnh", "Thảo", "Thu", "Thủy", "Trang", "Tuyết", "Uyên", "Vân", "Vy", "Yến"
    };

    private static final String[] ACADEMIC_TITLES = {
            "PGS.TS.", "TS.", "ThS.", "TS.", "ThS.", "PGS.TS.", "TS.", "ThS."
    };

    public DataSeeder(UserRepository userRepository, FacultyRepository khoaRepository,
                      MajorRepository nganhRepository, AdministrativeClassRepository lopHanhChinhRepository,
                      SubjectRepository monHocRepository, PrerequisiteSubjectRepository monTienQuyetRepository,
                      SemesterRepository hocKyRepository, LecturerRepository giangVienRepository,
                      StudentRepository sinhVienRepository, CourseClassRepository lopHocPhanRepository,
                      CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                      PasswordEncoder passwordEncoder) {
        this(userRepository, khoaRepository, nganhRepository, lopHanhChinhRepository,
                monHocRepository, monTienQuyetRepository, hocKyRepository, giangVienRepository,
                sinhVienRepository, lopHocPhanRepository, dangKyHocPhanRepository, diemRepository,
                null, null, null, passwordEncoder);
    }

    @Autowired
    public DataSeeder(UserRepository userRepository, FacultyRepository khoaRepository,
                      MajorRepository nganhRepository, AdministrativeClassRepository lopHanhChinhRepository,
                      SubjectRepository monHocRepository, PrerequisiteSubjectRepository monTienQuyetRepository,
                      SemesterRepository hocKyRepository, LecturerRepository giangVienRepository,
                      StudentRepository sinhVienRepository, CourseClassRepository lopHocPhanRepository,
                      CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                      AcademicCohortRepository khoaHocRepository,
                      @Autowired(required = false) RegistrationPeriodRepository dotDangKyRepository,
                      @Autowired(required = false) ClassroomRepository phongHocRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.lopHanhChinhRepository = lopHanhChinhRepository;
        this.monHocRepository = monHocRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.hocKyRepository = hocKyRepository;
        this.giangVienRepository = giangVienRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.khoaHocRepository = khoaHocRepository;
        this.dotDangKyRepository = dotDangKyRepository;
        this.phongHocRepository = phongHocRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        refreshCurrentRegistrationWindow();

        if (userRepository.count() > 0 && sinhVienRepository.count() >= 500) {
            seedKhoaHocNeuChuaCo(null, null);
            seedPhongHocNeuChuaCo();
            log.info("Dữ liệu đầy đủ (500 sinh viên, 50 giảng viên, 50 môn học), bỏ qua bước tạo dữ liệu mẫu");
            return;
        }

        if (userRepository.count() > 0 && sinhVienRepository.count() < 500) {
            log.info("Dữ liệu cũ chưa đủ 500 sinh viên, tiến hành làm mới toàn bộ cơ sở dữ liệu mẫu...");
            diemRepository.deleteAllInBatch();
            dangKyHocPhanRepository.deleteAllInBatch();
            lopHocPhanRepository.deleteAllInBatch();
            monTienQuyetRepository.deleteAllInBatch();
            monHocRepository.deleteAllInBatch();
            sinhVienRepository.deleteAllInBatch();
            giangVienRepository.deleteAllInBatch();
            lopHanhChinhRepository.deleteAllInBatch();
            nganhRepository.deleteAllInBatch();
            khoaRepository.deleteAllInBatch();
            if (khoaHocRepository != null) {
                khoaHocRepository.deleteAllInBatch();
            }
            if (dotDangKyRepository != null) {
                dotDangKyRepository.deleteAllInBatch();
            }
            if (phongHocRepository != null) {
                phongHocRepository.deleteAllInBatch();
            }
            hocKyRepository.deleteAllInBatch();
            userRepository.deleteAllInBatch();
        }

        String encodedDefaultPassword = passwordEncoder.encode(defaultSeedPassword);
        String encodedAdminPassword = passwordEncoder.encode(adminSeedPassword);

        // 1. Tạo 4 Faculty
        Faculty cntt = taoKhoa("CNTT", "Công nghệ thông tin");
        Faculty kt = taoKhoa("KT", "Kinh tế & Quản lý");
        Faculty dt = taoKhoa("DT", "Điện tử - Viễn thông");
        Faculty nn = taoKhoa("NN", "Ngoại ngữ ứng dụng");

        // 2. Tạo 8 Ngành
        Major cnpm = taoNganh("CNPM", "Kỹ thuật phần mềm", cntt);
        Major httt = taoNganh("HTTT", "Hệ thống thông tin", cntt);
        Major khmt = taoNganh("KHMT", "Khoa học máy tính", cntt);
        Major attt = taoNganh("ATTT", "An toàn thông tin", cntt);
        Major qtkd = taoNganh("QTKD", "Quản trị kinh doanh", kt);
        Major tcnh = taoNganh("TCNH", "Tài chính - Ngân hàng", kt);
        Major dtvt = taoNganh("DTVT", "Kỹ thuật Điện tử Viễn thông", dt);
        Major nna = taoNganh("NNA", "Ngôn ngữ Anh thương mại", nn);

        // 3. Tạo 10 Lớp hành chính
        AdministrativeClass cnpm01 = taoLop("CNPM01", "Kỹ thuật phần mềm 01", cnpm);
        AdministrativeClass cnpm02 = taoLop("CNPM02", "Kỹ thuật phần mềm 02", cnpm);
        AdministrativeClass httt01 = taoLop("HTTT01", "Hệ thống thông tin 01", httt);
        AdministrativeClass httt02 = taoLop("HTTT02", "Hệ thống thông tin 02", httt);
        AdministrativeClass khmt01 = taoLop("KHMT01", "Khoa học máy tính 01", khmt);
        AdministrativeClass attt01 = taoLop("ATTT01", "An toàn thông tin 01", attt);
        AdministrativeClass qtkd01 = taoLop("QTKD01", "Quản trị kinh doanh 01", qtkd);
        AdministrativeClass tcnh01 = taoLop("TCNH01", "Tài chính - Ngân hàng 01", tcnh);
        AdministrativeClass dtvt01 = taoLop("DTVT01", "Điện tử Viễn thông 01", dtvt);
        AdministrativeClass nna01 = taoLop("NNA01", "Ngôn ngữ Anh 01", nna);

        AdministrativeClass[] danhSachLopHanhChinh = {
                cnpm01, cnpm02, httt01, httt02, khmt01, attt01, qtkd01, tcnh01, dtvt01, nna01
        };

        // 4. Tạo 50 Môn học chia đều 8 kỳ
        Map<String, Subject> monMap = new HashMap<>();

        // Kỳ 1 (6 môn)
        monMap.put("INT101", taoMon("INT101", "Lập trình cơ bản", 3, cntt));
        monMap.put("MAT101", taoMon("MAT101", "Toán rời rạc", 3, cntt));
        monMap.put("MAT102", taoMon("MAT102", "Giải tích 1", 3, cntt));
        monMap.put("PHI101", taoMon("PHI101", "Triết học Mác - Lênin", 3, kt));
        monMap.put("ENG101", taoMon("ENG101", "Tiếng Anh đại cương 1", 2, nn));
        monMap.put("GEN101", taoMon("GEN101", "Pháp luật đại cương", 2, kt));

        // Kỳ 2 (6 môn)
        monMap.put("INT201", taoMon("INT201", "Cấu trúc dữ liệu và giải thuật", 3, cntt));
        monMap.put("INT102", taoMon("INT102", "Lập trình hướng đối tượng", 3, cntt));
        monMap.put("MAT103", taoMon("MAT103", "Giải tích 2", 3, cntt));
        monMap.put("PHY101", taoMon("PHY101", "Vật lý đại cương", 3, dt));
        monMap.put("PHI102", taoMon("PHI102", "Kinh tế chính trị Mác - Lênin", 2, kt));
        monMap.put("ENG102", taoMon("ENG102", "Tiếng Anh đại cương 2", 2, nn));

        // Kỳ 3 (6 môn)
        monMap.put("INT301", taoMon("INT301", "Cơ sở dữ liệu", 3, cntt));
        monMap.put("INT202", taoMon("INT202", "Kiến trúc máy tính", 3, cntt));
        monMap.put("INT203", taoMon("INT203", "Lập trình ứng dụng Java", 3, cntt));
        monMap.put("MAT201", taoMon("MAT201", "Xác suất thống kê", 3, cntt));
        monMap.put("ECO101", taoMon("ECO101", "Kinh tế vi mô", 3, kt));
        monMap.put("PHI103", taoMon("PHI103", "Chủ nghĩa xã hội khoa học", 2, kt));

        // Kỳ 4 (6 môn)
        monMap.put("INT302", taoMon("INT302", "Lập trình web", 3, cntt));
        monMap.put("INT303", taoMon("INT303", "Mạng máy tính", 3, cntt));
        monMap.put("INT204", taoMon("INT204", "Hệ điều hành", 3, cntt));
        monMap.put("ELE201", taoMon("ELE201", "Kỹ thuật số và vi xử lý", 3, dt));
        monMap.put("ECO102", taoMon("ECO102", "Kinh tế vĩ mô", 3, kt));
        monMap.put("PHI104", taoMon("PHI104", "Lịch sử Đảng Cộng sản VN", 2, kt));

        // Kỳ 5 (6 môn)
        monMap.put("INT304", taoMon("INT304", "Trí tuệ nhân tạo", 3, cntt));
        monMap.put("INT305", taoMon("INT305", "Hệ quản trị cơ sở dữ liệu", 3, cntt));
        monMap.put("INT306", taoMon("INT306", "Phân tích thiết kế hệ thống", 3, cntt));
        monMap.put("INT307", taoMon("INT307", "Phát triển ứng dụng Web nâng cao", 3, cntt));
        monMap.put("SEC301", taoMon("SEC301", "An toàn thông tin căn bản", 3, cntt));
        monMap.put("PHI105", taoMon("PHI105", "Tư tưởng Hồ Chí Minh", 2, kt));

        // Kỳ 6 (7 môn)
        monMap.put("INT401", taoMon("INT401", "Công nghệ phần mềm", 3, cntt));
        monMap.put("INT402", taoMon("INT402", "Lập trình thiết bị di động", 3, cntt));
        monMap.put("INT403", taoMon("INT403", "Học máy cơ sở", 3, cntt));
        monMap.put("CLD301", taoMon("CLD301", "Điện toán đám mây", 3, cntt));
        monMap.put("SEC302", taoMon("SEC302", "An ninh mạng và mật mã học", 3, cntt));
        monMap.put("MGT301", taoMon("MGT301", "Quản trị học đại cương", 3, kt));
        monMap.put("ENG201", taoMon("ENG201", "Tiếng Anh giao tiếp chuyên nghiệp", 2, nn));

        // Kỳ 7 (7 môn)
        monMap.put("INT404", taoMon("INT404", "Kiểm thử phần mềm & QA", 3, cntt));
        monMap.put("INT405", taoMon("INT405", "Phân tích dữ liệu lớn", 3, cntt));
        monMap.put("INT406", taoMon("INT406", "Thị giác máy tính và Xử lý ảnh", 3, cntt));
        monMap.put("INT407", taoMon("INT407", "Xử lý ngôn ngữ tự nhiên", 3, cntt));
        monMap.put("INT408", taoMon("INT408", "DevOps & Tự động hóa CI/CD", 3, cntt));
        monMap.put("MGT401", taoMon("MGT401", "Quản lý dự án công nghệ thông tin", 3, kt));
        monMap.put("FIN301", taoMon("FIN301", "Tài chính doanh nghiệp", 3, kt));

        // Kỳ 8 (6 môn)
        monMap.put("INT490", taoMon("INT490", "Chuyên đề công nghệ mới", 3, cntt));
        monMap.put("INT493", taoMon("INT493", "Kiến trúc microservices", 3, cntt));
        monMap.put("INT494", taoMon("INT494", "Đạo đức nghề nghiệp trong CNTT", 2, cntt));
        monMap.put("MGT490", taoMon("MGT490", "Khởi nghiệp và Đổi mới sáng tạo", 2, kt));
        monMap.put("PRJ491", taoMon("PRJ491", "Thực tập tốt nghiệp doanh nghiệp", 4, cntt));
        monMap.put("PRJ492", taoMon("PRJ492", "Khóa luận tốt nghiệp", 6, cntt));

        // Môn tiên quyết
        taoTienQuyet(monMap.get("INT201"), monMap.get("INT101"));
        taoTienQuyet(monMap.get("INT102"), monMap.get("INT101"));
        taoTienQuyet(monMap.get("MAT103"), monMap.get("MAT102"));
        taoTienQuyet(monMap.get("PHI102"), monMap.get("PHI101"));
        taoTienQuyet(monMap.get("ENG102"), monMap.get("ENG101"));
        taoTienQuyet(monMap.get("INT301"), monMap.get("INT201"));
        taoTienQuyet(monMap.get("INT203"), monMap.get("INT102"));
        taoTienQuyet(monMap.get("INT302"), monMap.get("INT301"));
        taoTienQuyet(monMap.get("INT204"), monMap.get("INT202"));
        taoTienQuyet(monMap.get("ELE201"), monMap.get("PHY101"));
        taoTienQuyet(monMap.get("ECO102"), monMap.get("ECO101"));
        taoTienQuyet(monMap.get("INT304"), monMap.get("INT201"));
        taoTienQuyet(monMap.get("INT305"), monMap.get("INT301"));
        taoTienQuyet(monMap.get("INT306"), monMap.get("INT301"));
        taoTienQuyet(monMap.get("INT307"), monMap.get("INT302"));
        taoTienQuyet(monMap.get("SEC301"), monMap.get("INT303"));
        taoTienQuyet(monMap.get("INT401"), monMap.get("INT306"));
        taoTienQuyet(monMap.get("INT402"), monMap.get("INT203"));
        taoTienQuyet(monMap.get("INT403"), monMap.get("INT304"));
        taoTienQuyet(monMap.get("CLD301"), monMap.get("INT303"));
        taoTienQuyet(monMap.get("SEC302"), monMap.get("SEC301"));
        taoTienQuyet(monMap.get("ENG201"), monMap.get("ENG102"));
        taoTienQuyet(monMap.get("INT404"), monMap.get("INT401"));
        taoTienQuyet(monMap.get("INT405"), monMap.get("INT305"));
        taoTienQuyet(monMap.get("INT406"), monMap.get("INT403"));
        taoTienQuyet(monMap.get("INT407"), monMap.get("INT403"));
        taoTienQuyet(monMap.get("INT408"), monMap.get("CLD301"));
        taoTienQuyet(monMap.get("MGT401"), monMap.get("MGT301"));
        taoTienQuyet(monMap.get("INT493"), monMap.get("INT307"));

        // 5. Tạo Học kỳ
        LocalDate homNay = LocalDate.now();
        Semester hocKyCu2 = taoHocKy("Học kỳ 1", "2025-2026",
                homNay.minusMonths(14), homNay.minusMonths(9),
                homNay.minusMonths(15), homNay.minusMonths(14).plusDays(20), 12, 24);
        Semester hocKyCu = taoHocKy("Học kỳ 2", "2025-2026",
                homNay.minusMonths(8), homNay.minusMonths(4),
                homNay.minusMonths(9), homNay.minusMonths(8).plusDays(20), 12, 24);
        Semester hocKyHienTai = taoHocKy("Học kỳ 1", "2026-2027",
                homNay.minusDays(14), homNay.plusDays(90),
                homNay.minusDays(5), homNay.plusDays(20), 12, 24);
        taoHocKy("Học kỳ 2", "2024-2025",
                homNay.minusMonths(20), homNay.minusMonths(16),
                homNay.minusMonths(21), homNay.minusMonths(20).plusDays(20), 12, 24);
        Semester hocKyCuNhat = taoHocKy("Học kỳ 1", "2024-2025",
                homNay.minusMonths(26), homNay.minusMonths(22),
                homNay.minusMonths(27), homNay.minusMonths(26).plusDays(20), 12, 24);

        seedKhoaHocNeuChuaCo(hocKyCuNhat, hocKyHienTai);
        ensureOpenRegistrationPeriod(hocKyHienTai);

        // 6. Tạo Quản trị viên
        taoUserWithHash("admin", encodedAdminPassword, Role.QUAN_TRI);

        // 7. Tạo 50 Giảng viên với tên người thật và học hàm TS, ThS, PGS.TS
        List<Lecturer> giangVienList = new ArrayList<>();
        Faculty[] khoaArr = {cntt, kt, dt, nn};

        // Giữ 3 giảng viên cố định tương thích test
        giangVienList.add(taoGiangVienWithHash("gv01", "GV01", "TS. Nguyễn Văn An", "an.nv@university.edu.vn", "0911009871", cntt, encodedDefaultPassword));
        giangVienList.add(taoGiangVienWithHash("gv02", "GV02", "ThS. Trần Thị Bình", "binh.tt@university.edu.vn", "0981019742", cntt, encodedDefaultPassword));
        giangVienList.add(taoGiangVienWithHash("gv03", "GV03", "ThS. Lê Hoàng Cường", "cuong.lh@university.edu.vn", "0971029613", kt, encodedDefaultPassword));

        for (int i = 4; i <= 50; i++) {
            String username = String.format("gv%02d", i);
            String ma = String.format("GV%02d", i);
            boolean isMale = (i % 3 != 0);
            String ho = LAST_NAMES[(i * 7 + 3) % LAST_NAMES.length];
            String dem = isMale ? MALE_MIDDLES[(i * 5 + 2) % MALE_MIDDLES.length] : FEMALE_MIDDLES[(i * 3 + 1) % FEMALE_MIDDLES.length];
            String ten = isMale ? MALE_FIRSTS[(i * 11 + 4) % MALE_FIRSTS.length] : FEMALE_FIRSTS[(i * 13 + 5) % FEMALE_FIRSTS.length];
            String title = ACADEMIC_TITLES[i % ACADEMIC_TITLES.length];
            String hoTen = title + " " + ho + " " + dem + " " + ten;
            String cleanTen = stripAccents(ten).toLowerCase();
            String cleanHoDem = stripAccents(ho.substring(0, 1) + dem.substring(0, 1)).toLowerCase();
            String email = cleanTen + "." + cleanHoDem + i + "@university.edu.vn";
            String sdt = generatePhone(i);
            Faculty assignedKhoa = khoaArr[(i - 1) % khoaArr.length];

            giangVienList.add(taoGiangVienWithHash(username, ma, hoTen, email, sdt, assignedKhoa, encodedDefaultPassword));
        }

        Lecturer gv01 = giangVienList.get(0);
        Lecturer gv02 = giangVienList.get(1);
        Lecturer gv03 = giangVienList.get(2);

        // 8. Tạo 500 Sinh viên với tên người thật
        List<Student> sinhVienList = new ArrayList<>();

        // Giữ 5 sinh viên cố định tương thích test
        sinhVienList.add(taoSinhVienWithHash("sv01", "SV001", "Trần Minh Anh", "anh.tm@student.edu.vn", "0901987100", cntt, cnpm, cnpm01, encodedDefaultPassword));
        sinhVienList.add(taoSinhVienWithHash("sv02", "SV002", "Phạm Thu Hà", "ha.pt@student.edu.vn", "0911996971", cntt, cnpm, cnpm01, encodedDefaultPassword));
        sinhVienList.add(taoSinhVienWithHash("sv03", "SV003", "Đỗ Quang Huy", "huy.dq@student.edu.vn", "0982006842", cntt, cnpm, cnpm02, encodedDefaultPassword));
        sinhVienList.add(taoSinhVienWithHash("sv04", "SV004", "Vũ Ngọc Lan", "lan.vn@student.edu.vn", "0972016713", cntt, httt, httt01, encodedDefaultPassword));
        sinhVienList.add(taoSinhVienWithHash("sv05", "SV005", "Hoàng Gia Bảo", "bao.hg@student.edu.vn", "0382026584", cntt, httt, httt01, encodedDefaultPassword));

        for (int i = 6; i <= 500; i++) {
            String username = String.format("sv%02d", i);
            String ma = String.format("SV%03d", i);
            boolean isMale = (i % 2 == 0);
            String ho = LAST_NAMES[(i * 13 + 5) % LAST_NAMES.length];
            String dem = isMale ? MALE_MIDDLES[(i * 17 + 7) % MALE_MIDDLES.length] : FEMALE_MIDDLES[(i * 19 + 3) % FEMALE_MIDDLES.length];
            String ten = isMale ? MALE_FIRSTS[(i * 23 + 11) % MALE_FIRSTS.length] : FEMALE_FIRSTS[(i * 29 + 13) % FEMALE_FIRSTS.length];
            String hoTen = ho + " " + dem + " " + ten;
            String cleanTen = stripAccents(ten).toLowerCase();
            String cleanHoDem = stripAccents(ho.substring(0, 1) + dem.substring(0, 1)).toLowerCase();
            String email = cleanTen + "." + cleanHoDem + i + "@student.edu.vn";
            String sdt = generatePhone(i + 100);

            AdministrativeClass assignedLop = danhSachLopHanhChinh[(i - 1) % danhSachLopHanhChinh.length];
            Major assignedNganh = assignedLop.getNganh();
            Faculty assignedKhoa = assignedNganh.getKhoa();

            sinhVienList.add(taoSinhVienWithHash(username, ma, hoTen, email, sdt, assignedKhoa, assignedNganh, assignedLop, encodedDefaultPassword));
        }

        Student sv01 = sinhVienList.get(0);
        Student sv02 = sinhVienList.get(1);
        Student sv03 = sinhVienList.get(2);
        Student sv04 = sinhVienList.get(3);
        Student sv05 = sinhVienList.get(4);

        // 9. Tạo các Lớp học phần
        // Lớp quá khứ
        CourseClass lopMatCu = taoLopHocPhan("MAT101.01-HK1", monMap.get("MAT101"), hocKyCu2, gv02, "A105", 4, 7, 3, 60);
        CourseClass lopEcoCu = taoLopHocPhan("ECO101.01-HK1", monMap.get("ECO101"), hocKyCu2, gv03, "C301", 3, 4, 3, 60);
        CourseClass lopCu = taoLopHocPhan("INT101.01-HK2", monMap.get("INT101"), hocKyCu, gv01, "A101", 2, 1, 3, 60);
        CourseClass lopMat102Cu = taoLopHocPhan("MAT102.01-HK1", monMap.get("MAT102"), hocKyCu2, giangVienList.get(3), "A102", 5, 1, 3, 60);
        CourseClass lopPhi101Cu = taoLopHocPhan("PHI101.01-HK1", monMap.get("PHI101"), hocKyCu2, giangVienList.get(4), "C302", 6, 4, 3, 60);
        CourseClass lopEng101Cu = taoLopHocPhan("ENG101.01-HK1", monMap.get("ENG101"), hocKyCu2, giangVienList.get(5), "B201", 3, 7, 2, 60);
        CourseClass lopGen101Cu = taoLopHocPhan("GEN101.01-HK1", monMap.get("GEN101"), hocKyCu2, giangVienList.get(6), "C303", 7, 1, 2, 60);

        CourseClass lopInt201Cu = taoLopHocPhan("INT201.01-HK2", monMap.get("INT201"), hocKyCu, giangVienList.get(7), "A102", 3, 1, 3, 60);
        CourseClass lopInt102Cu = taoLopHocPhan("INT102.01-HK2", monMap.get("INT102"), hocKyCu, giangVienList.get(8), "A103", 4, 1, 3, 60);
        CourseClass lopMat103Cu = taoLopHocPhan("MAT103.01-HK2", monMap.get("MAT103"), hocKyCu, giangVienList.get(9), "A104", 5, 4, 3, 60);
        CourseClass lopPhy101Cu = taoLopHocPhan("PHY101.01-HK2", monMap.get("PHY101"), hocKyCu, giangVienList.get(10), "D401", 6, 1, 3, 60);
        CourseClass lopPhi102Cu = taoLopHocPhan("PHI102.01-HK2", monMap.get("PHI102"), hocKyCu, giangVienList.get(11), "C301", 2, 7, 2, 60);
        CourseClass lopEng102Cu = taoLopHocPhan("ENG102.01-HK2", monMap.get("ENG102"), hocKyCu, giangVienList.get(12), "B202", 4, 7, 2, 60);

        CourseClass[] pastClasses = {
                lopMatCu, lopEcoCu, lopCu, lopMat102Cu, lopPhi101Cu, lopEng101Cu, lopGen101Cu,
                lopInt201Cu, lopInt102Cu, lopMat103Cu, lopPhy101Cu, lopPhi102Cu, lopEng102Cu
        };

        // Lớp học phần kỳ hiện tại (đa dạng: đại cương không tiên quyết, cơ sở ngành, chuyên ngành, lớp đầy, xung đột lịch)
        CourseClass int20101 = taoLopHocPhan("INT201.01", monMap.get("INT201"), hocKyHienTai, gv01, "A101", 2, 1, 3, 60);
        CourseClass int20102 = taoLopHocPhan("INT201.02", monMap.get("INT201"), hocKyHienTai, gv02, "A102", 4, 4, 3, 60);
        taoLopHocPhan("INT301.01", monMap.get("INT301"), hocKyHienTai, gv01, "B201", 3, 1, 3, 60);
        taoLopHocPhan("INT302.01", monMap.get("INT302"), hocKyHienTai, gv02, "A103", 5, 1, 3, 60);
        CourseClass int30301 = taoLopHocPhan("INT303.01", monMap.get("INT303"), hocKyHienTai, gv01, "B202", 6, 4, 3, 60);
        taoLopHocPhan("INT304.01", monMap.get("INT304"), hocKyHienTai, gv02, "A104", 7, 1, 3, 60);
        CourseClass ecoLop = taoLopHocPhan("ECO101.01", monMap.get("ECO101"), hocKyHienTai, gv03, "C301", 3, 4, 3, 60);
        CourseClass matLop = taoLopHocPhan("MAT101.01", monMap.get("MAT101"), hocKyHienTai, gv02, "A105", 4, 7, 3, 60);

        // Các lớp đại cương / học lại không yêu cầu tiên quyết (dễ test đăng ký mới & học lại)
        CourseClass int101Cur = taoLopHocPhan("INT101.01", monMap.get("INT101"), hocKyHienTai, giangVienList.get(3), "A101", 5, 7, 3, 60);
        CourseClass int102Cur = taoLopHocPhan("INT102.01", monMap.get("INT102"), hocKyHienTai, gv02, "A102", 2, 4, 3, 60);
        CourseClass mat102Cur = taoLopHocPhan("MAT102.01", monMap.get("MAT102"), hocKyHienTai, giangVienList.get(3), "A103", 6, 1, 3, 60);
        CourseClass phi101Cur = taoLopHocPhan("PHI101.01", monMap.get("PHI101"), hocKyHienTai, gv03, "C302", 6, 4, 3, 60); // cố ý trùng Thứ 6 tiết 4-6 với INT303.01
        CourseClass eng101Cur = taoLopHocPhan("ENG101.01", monMap.get("ENG101"), hocKyHienTai, giangVienList.get(5), "B201", 3, 4, 2, 60); // cố ý trùng Thứ 3 tiết 4-5 với ECO101.01
        CourseClass gen101Cur = taoLopHocPhan("GEN101.01", monMap.get("GEN101"), hocKyHienTai, giangVienList.get(6), "C303", 7, 4, 2, 60);
        CourseClass gen101Full = taoLopHocPhan("GEN101.02", monMap.get("GEN101"), hocKyHienTai, giangVienList.get(6), "C304", 2, 7, 2, 5); // lớp sẽ đầy 5/5
        CourseClass eng102Cur = taoLopHocPhan("ENG102.01", monMap.get("ENG102"), hocKyHienTai, giangVienList.get(7), "B202", 4, 1, 2, 60);
        CourseClass eco102Cur = taoLopHocPhan("ECO102.01", monMap.get("ECO102"), hocKyHienTai, gv03, "C301", 5, 4, 3, 60);
        CourseClass phy101Cur = taoLopHocPhan("PHY101.01", monMap.get("PHY101"), hocKyHienTai, giangVienList.get(10), "D401", 3, 7, 3, 60);
        CourseClass mgt301Cur = taoLopHocPhan("MGT301.01", monMap.get("MGT301"), hocKyHienTai, giangVienList.get(11), "C302", 7, 7, 3, 60);
        CourseClass fin301Cur = taoLopHocPhan("FIN301.01", monMap.get("FIN301"), hocKyHienTai, giangVienList.get(12), "C303", 5, 7, 3, 60);

        // Mở thêm các lớp cho các giảng viên khác phụ trách
        List<CourseClass> currentClasses = new ArrayList<>();
        currentClasses.add(int20101);
        currentClasses.add(int20102);
        currentClasses.add(int30301);
        currentClasses.add(ecoLop);
        currentClasses.add(matLop);
        currentClasses.add(int101Cur);
        currentClasses.add(int102Cur);
        currentClasses.add(mat102Cur);
        currentClasses.add(gen101Cur);
        currentClasses.add(eng102Cur);
        currentClasses.add(eco102Cur);
        currentClasses.add(phy101Cur);

        for (int i = 13; i < 50; i++) {
            Lecturer gv = giangVienList.get(i);
            String[] subjectCodes = {"INT202", "INT203", "MAT201", "SEC301", "INT401", "INT403", "CLD301", "INT404", "INT408"};
            String subCode = subjectCodes[i % subjectCodes.length];
            Subject mon = monMap.get(subCode);
            if (mon != null) {
                String maLop = mon.getMa() + "." + String.format("%02d", (i % 3) + 1);
                if (!lopHocPhanRepository.existsByMa(maLop)) {
                    CourseClass lhp = taoLopHocPhan(maLop, mon, hocKyHienTai, gv, "P" + (100 + i), (i % 6) + 2, ((i % 3) * 3) + 1, 3, 60);
                    currentClasses.add(lhp);
                }
            }
        }

        // Gán chương trình đào tạo cho sinh viên (trừ sv02, sv04 để giữ nguyên test case chưa gán CTDT)
        Map<Long, Curriculum> curriculumByMajor = seedCurriculums(monMap, cnpm, httt, khmt, attt, qtkd, tcnh, dtvt, nna);
        for (int i = 0; i < sinhVienList.size(); i++) {
            Student s = sinhVienList.get(i);
            if (i == 1 || i == 3) continue; // sv02, sv04
            if (s.getNganh() != null && curriculumByMajor.containsKey(s.getNganh().getId())) {
                s.setChuongTrinh(curriculumByMajor.get(s.getNganh().getId()));
                if (i >= 480) {
                    s.setTrangThaiHocTap(i % 2 == 0 ? StudentAcademicStatus.BAO_LUU : StudentAcademicStatus.DANG_HOC);
                }
                sinhVienRepository.save(s);
            }
        }

        // 10. Đăng ký & Cho điểm (nạc mỡ đan xen: có môn đạt, có môn trượt, có môn đang học, đủ điều kiện & không đủ điều kiện)
        // sv01: đã đạt INT101, MAT101, ECO101; đang học 4 lớp (12 TC) -> còn dư 12 TC để đăng ký thêm (INT102, INT301, GEN101...)
        dangKyVoiDiem(sv01, lopCu, 8.5, 9.0);
        dangKyVoiDiem(sv01, lopMatCu, 7.0, 8.0);
        dangKyVoiDiem(sv01, lopEcoCu, 6.5, 7.0);
        dangKyHienTai(sv01, int30301);
        dangKyHienTai(sv01, int20101);
        dangKyHienTai(sv01, matLop);
        dangKyHienTai(sv01, ecoLop);

        // sv02 (không đăng ký lớp hiện tại để phục vụ E2E test; đã đạt INT101 & MAT101)
        dangKyVoiDiem(sv02, lopCu, 7.0, 8.0);
        dangKyVoiDiem(sv02, lopMatCu, 8.0, 8.5);

        // sv03 (Điểm yếu < 5.0 ở INT101 -> test học lại INT101.01, đồng thời vẫn đủ điều kiện các môn đại cương như MAT102.01, GEN101.01)
        dangKyVoiDiem(sv03, lopCu, 3.5, 4.0);
        dangKyVoiDiem(sv03, lopEcoCu, 5.0, 6.0);
        dangKyHienTai(sv03, int30301);
        dangKyHienTai(sv03, matLop);
        dangKyHienTai(sv03, ecoLop);

        // sv04 (Chưa có điểm công bố -> test xuất PDF báo chưa có điểm; đủ điều kiện đăng ký các môn không tiên quyết)
        dangKyHienTai(sv04, int30301);
        dangKyHienTai(sv04, matLop);
        dangKyHienTai(sv04, ecoLop);

        // sv05 (Đã đạt INT101, ENG101, MAT102 -> đủ điều kiện INT201, INT102, ENG102, GEN101; bị chặn INT301, INT408)
        dangKyVoiDiem(sv05, lopCu, 7.5, 8.0);
        dangKyVoiDiem(sv05, lopEng101Cu, 7.0, 7.5);
        dangKyVoiDiem(sv05, lopMat102Cu, 6.5, 7.0);
        dangKyHienTai(sv05, int30301);
        dangKyHienTai(sv05, matLop);

        // sv06..sv20: Đan xen nhiều hồ sơ học tập khác nhau
        for (int i = 6; i <= 20; i++) {
            Student sv = sinhVienList.get(i - 1);
            if (i % 3 == 0) {
                // Nhóm có cả môn trượt để test học lại + môn đạt nền tảng
                dangKyVoiDiem(sv, lopCu, 7.0, 7.5);
                dangKyVoiDiem(sv, lopMat102Cu, 3.5, 4.0); // trượt MAT102
                dangKyVoiDiem(sv, lopEng101Cu, 6.5, 7.0);
            } else if (i % 2 == 0) {
                // Nhóm đã qua chuỗi INT101 -> INT102 -> INT201 (đủ điều kiện đăng ký INT301, INT302, INT304, INT203)
                dangKyVoiDiem(sv, lopCu, 7.5 + (i % 2), 8.0);
                dangKyVoiDiem(sv, lopInt102Cu, 7.0, 7.5);
                dangKyVoiDiem(sv, lopInt201Cu, 7.0, 8.0);
                dangKyVoiDiem(sv, lopEng101Cu, 7.5, 8.0);
            } else {
                // Nhóm mới qua INT101 + MAT101 (đủ điều kiện INT201, INT102, đại cương; bị chặn INT301+)
                dangKyVoiDiem(sv, lopCu, 6.5 + (i % 3), 7.0 + (i % 2));
                dangKyVoiDiem(sv, lopMatCu, 6.0, 7.0);
            }
            // Chỉ đăng ký sẵn 2 lớp (6 TC) để mỗi tài khoản còn trống nhiều tín chỉ & slot thử đăng ký / lập kế hoạch
            dangKyHienTai(sv, int30301);
            dangKyHienTai(sv, matLop);
        }

        // Lấp đầy lớp GEN101.02 (5/5) bằng sv491..sv495 để test trường hợp lớp đã đầy
        for (int i = 491; i <= 495; i++) {
            dangKyHienTai(sinhVienList.get(i - 1), gen101Full);
        }

        // sv21..sv500: Phân bố 4 nhóm năng lực & tiến độ rõ ràng (Xuất sắc/Chuyên ngành, Trung bình-Khá, Có môn trượt/Học lại, Tân sinh viên)
        CourseClass[] openPoolForSeed = {matLop, ecoLop, int101Cur, mat102Cur, gen101Cur, eng102Cur, eco102Cur};
        for (int i = 21; i <= 500; i++) {
            Student sv = sinhVienList.get(i - 1);
            int profile = i % 10;

            if (profile <= 3) {
                // 40%: Đã qua nền tảng CNTT/KT (INT101, INT102, INT201, ENG101)
                double base = 6.8 + (i % 15) * 0.1;
                dangKyVoiDiem(sv, lopCu, Math.min(9.5, base), Math.min(9.5, base + 0.3));
                dangKyVoiDiem(sv, lopInt102Cu, Math.min(9.5, base - 0.2), Math.min(9.5, base + 0.2));
                dangKyVoiDiem(sv, lopInt201Cu, Math.min(9.5, base), Math.min(9.5, base + 0.4));
                dangKyVoiDiem(sv, lopEng101Cu, Math.min(9.5, base - 0.3), Math.min(9.5, base + 0.1));
            } else if (profile <= 6) {
                // 30%: Đan xen môn đạt & 1 môn trượt (< 5.0) để test học lại + cải thiện
                dangKyVoiDiem(sv, lopCu, 6.0 + (i % 3) * 0.5, 6.5 + (i % 3) * 0.5);
                dangKyVoiDiem(sv, lopEcoCu, 6.5, 7.0);
                dangKyVoiDiem(sv, (i % 2 == 0) ? lopInt102Cu : lopMat102Cu, 3.0 + (i % 2) * 0.5, 4.0);
            } else if (profile <= 8) {
                // 20%: Sinh viên năm 3-4 điểm giỏi/xuất sắc, đã tích lũy nhiều môn tiên quyết
                double base = 8.2 + (i % 12) * 0.1;
                dangKyVoiDiem(sv, lopCu, Math.min(9.8, base), Math.min(9.8, base + 0.2));
                dangKyVoiDiem(sv, lopInt102Cu, Math.min(9.8, base - 0.2), Math.min(9.8, base + 0.1));
                dangKyVoiDiem(sv, lopInt201Cu, Math.min(9.8, base), Math.min(9.8, base + 0.2));
                dangKyVoiDiem(sv, lopMatCu, Math.min(9.8, base - 0.1), Math.min(9.8, base + 0.2));
                dangKyVoiDiem(sv, lopEng101Cu, Math.min(9.8, base), Math.min(9.8, base));
                dangKyVoiDiem(sv, lopEng102Cu, Math.min(9.8, base - 0.3), Math.min(9.8, base));
            } else {
                // 10%: Tân sinh viên (chỉ mới có 1 môn đại cương, bị chặn các môn cần tiên quyết)
                dangKyVoiDiem(sv, lopPhi101Cu, 7.0, 7.5);
            }

            // Đăng ký 1-2 lớp kỳ hiện tại (3-6 TC) để luôn còn trống slot đăng ký thêm
            CourseClass c1 = openPoolForSeed[i % openPoolForSeed.length];
            dangKyHienTai(sv, c1);
            if (i % 2 == 0) {
                CourseClass c2 = openPoolForSeed[(i + 2) % openPoolForSeed.length];
                if (!c2.getId().equals(c1.getId()) && !c2.getMonHoc().getId().equals(c1.getMonHoc().getId())) {
                    dangKyHienTai(sv, c2);
                }
            }
        }

        // Đa dạng trạng thái bảng điểm kỳ hiện tại để Giảng viên & Quản trị test luồng duyệt điểm
        phy101Cur.setTrangThaiBangDiem(GradeSheetStatus.CHO_DUYET);
        lopHocPhanRepository.save(phy101Cur);
        mgt301Cur.setTrangThaiBangDiem(GradeSheetStatus.TRA_VE);
        mgt301Cur.setLyDoTraVeBangDiem("Thiếu cột điểm quá trình cho 2 sinh viên trong lớp");
        lopHocPhanRepository.save(mgt301Cur);
        fin301Cur.setTrangThaiBangDiem(GradeSheetStatus.DA_DUYET);
        lopHocPhanRepository.save(fin301Cur);

        // Khóa sổ 1 lớp cũ để test trạng thái DA_KHOA
        lopMatCu.setTrangThaiBangDiem(GradeSheetStatus.DA_KHOA);
        lopHocPhanRepository.save(lopMatCu);

        seedPhongHocNeuChuaCo();
        seedRichFeatureData(sinhVienList, giangVienList, hocKyHienTai, int30301, int20101, matLop, ecoLop, lopCu, monMap);
        log.info("Đã tạo thành công bộ dữ liệu hoàn chỉnh: 500 sinh viên, 50 giảng viên, 50 môn học chia đều các kỳ!");
    }

    private void refreshCurrentRegistrationWindow() {
        if (!refreshCurrentWindow || hocKyRepository.count() == 0) {
            return;
        }
        LocalDate homNay = LocalDate.now();
        Semester hocKy = hocKyRepository
                .findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(homNay, homNay)
                .orElseGet(() -> hocKyRepository.findFirstByOrderByDangKyDenDesc().orElse(null));
        if (hocKy == null) {
            return;
        }
        if (!hocKy.dangMoDangKy()) {
            hocKy.setNgayBatDau(homNay.minusDays(14));
            hocKy.setNgayKetThuc(homNay.plusDays(90));
            hocKy.setDangKyTu(homNay.minusDays(5));
            hocKy.setDangKyDen(homNay.plusDays(20));
            hocKyRepository.save(hocKy);
            log.info("Đã làm mới cửa sổ đăng ký của học kỳ {} ({} đến {})",
                    hocKy.getTen(), hocKy.getDangKyTu(), hocKy.getDangKyDen());
        }
        ensureOpenRegistrationPeriod(hocKy);
    }

    private void ensureOpenRegistrationPeriod(Semester hocKy) {
        if (dotDangKyRepository == null || hocKy == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<RegistrationPeriod> periods = dotDangKyRepository
                .findByHocKyIdAndDangHieuLucTrueOrderByThuTuUuTienAscThoiGianBatDauAsc(hocKy.getId());
        if (periods.stream().anyMatch(p -> p.dangMo(now))) {
            return;
        }

        String maDot = "DOT-CHUNG";
        int suffix = 1;
        while (dotDangKyRepository.existsByHocKyIdAndMaDotIgnoreCase(hocKy.getId(), maDot)) {
            maDot = "DOT-CHUNG-" + (++suffix);
        }

        RegistrationPeriod dot = new RegistrationPeriod();
        dot.setHocKy(hocKy);
        dot.setMaDot(maDot);
        dot.setTenDot("Đợt đăng ký chung toàn trường");
        dot.setLoaiDot("CHINH_THUC");
        dot.setNhomDoiTuong("TOAN_TRUONG");
        dot.setThuTuUuTien(1);
        dot.setThoiGianBatDau(now.minusDays(5).toLocalDate().atStartOfDay());
        dot.setThoiGianKetThuc(now.plusDays(20).toLocalDate().atTime(23, 59, 59));
        dot.setDangHieuLuc(true);
        dot.setDongSom(false);
        dot.setTaoLuc(now);
        dot.setNguoiTaoUsername("system");
        dotDangKyRepository.save(dot);
        log.info("Đã tạo đợt đăng ký mở cho học kỳ {} ({} đến {})",
                hocKy.getTen(), dot.getThoiGianBatDau(), dot.getThoiGianKetThuc());
    }

    private void seedKhoaHocNeuChuaCo(Semester hkBatDau, Semester hkHienTai) {
        if (khoaHocRepository == null || khoaHocRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        taoKhoaHocMau("K2024", "Khóa 2024", "2024-2028", 2024, 2028, hkBatDau,
                LocalDate.of(2024, 9, 1), LocalDate.of(2028, 6, 30),
                "Khóa tuyển sinh đại học chính quy năm 2024", now);
        taoKhoaHocMau("K2025", "Khóa 2025", "2025-2029", 2025, 2029, null,
                LocalDate.of(2025, 9, 1), LocalDate.of(2029, 6, 30),
                "Khóa tuyển sinh đại học chính quy năm 2025", now);
        taoKhoaHocMau("K2026", "Khóa 2026", "2026-2030", 2026, 2030, hkHienTai,
                LocalDate.of(2026, 9, 1), LocalDate.of(2030, 6, 30),
                "Khóa tuyển sinh đại học chính quy năm 2026", now);
    }

    private void taoKhoaHocMau(String ma, String ten, String nienKhoa, int namBatDau, int namKetThuc,
                               Semester hkBatDau, LocalDate hieuLucTu, LocalDate hieuLucDen,
                               String moTa, LocalDateTime now) {
        AcademicCohort cohort = new AcademicCohort();
        cohort.setMa(ma);
        cohort.setTen(ten);
        cohort.setNienKhoa(nienKhoa);
        cohort.setNamBatDau(namBatDau);
        cohort.setNamKetThuc(namKetThuc);
        cohort.setHocKyBatDau(hkBatDau);
        cohort.setHieuLucTu(hieuLucTu);
        cohort.setHieuLucDen(hieuLucDen);
        cohort.setDangHieuLuc(true);
        cohort.setMoTa(moTa);
        cohort.setNguoiTaoUsername("system");
        cohort.setTaoLuc(now);
        cohort.setNguoiCapNhatUsername("system");
        cohort.setCapNhatLuc(now);
        khoaHocRepository.save(cohort);
    }

    private User taoUserWithHash(String username, String encodedPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setTrangThai(true);
        return userRepository.save(user);
    }

    private Faculty taoKhoa(String ma, String ten) {
        Faculty khoa = new Faculty();
        khoa.setMa(ma);
        khoa.setTen(ten);
        return khoaRepository.save(khoa);
    }

    private Major taoNganh(String ma, String ten, Faculty khoa) {
        Major nganh = new Major();
        nganh.setMa(ma);
        nganh.setTen(ten);
        nganh.setKhoa(khoa);
        return nganhRepository.save(nganh);
    }

    private AdministrativeClass taoLop(String ma, String ten, Major nganh) {
        AdministrativeClass lop = new AdministrativeClass();
        lop.setMa(ma);
        lop.setTen(ten);
        lop.setNganh(nganh);
        return lopHanhChinhRepository.save(lop);
    }

    private Subject taoMon(String ma, String ten, int soTinChi, Faculty khoa) {
        Subject monHoc = new Subject();
        monHoc.setMa(ma);
        monHoc.setTen(ten);
        monHoc.setSoTinChi(soTinChi);
        monHoc.setKhoa(khoa);
        LocalDateTime now = LocalDateTime.now();
        monHoc.setNguoiTaoUsername("system");
        monHoc.setTaoLuc(now);
        monHoc.setNguoiCapNhatUsername("system");
        monHoc.setCapNhatLuc(now);
        return monHocRepository.save(monHoc);
    }

    private void taoTienQuyet(Subject monHoc, Subject tienQuyet) {
        if (monHoc != null && tienQuyet != null) {
            PrerequisiteSubject rel = new PrerequisiteSubject(monHoc, tienQuyet);
            LocalDateTime now = LocalDateTime.now();
            rel.setNguoiTaoUsername("system");
            rel.setTaoLuc(now);
            rel.setNguoiCapNhatUsername("system");
            rel.setCapNhatLuc(now);
            monTienQuyetRepository.save(rel);
        }
    }

    private Semester taoHocKy(String ten, String namHoc, LocalDate batDau, LocalDate ketThuc,
                           LocalDate dangKyTu, LocalDate dangKyDen, int min, int max) {
        Semester hocKy = new Semester();
        hocKy.setTen(ten);
        hocKy.setNamHoc(namHoc);
        hocKy.setNgayBatDau(batDau);
        hocKy.setNgayKetThuc(ketThuc);
        hocKy.setDangKyTu(dangKyTu);
        hocKy.setDangKyDen(dangKyDen);
        hocKy.setSoTinChiToiThieu(min);
        hocKy.setSoTinChiToiDa(max);
        return hocKyRepository.save(hocKy);
    }

    private Lecturer taoGiangVienWithHash(String username, String ma, String hoTen, String email, String sdt, Faculty khoa, String encodedPassword) {
        User user = taoUserWithHash(username, encodedPassword, Role.GIANG_VIEN);
        Lecturer giangVien = new Lecturer();
        giangVien.setUser(user);
        giangVien.setMa(ma);
        giangVien.setHoTen(hoTen);
        giangVien.setEmail(email);
        giangVien.setSoDienThoai(sdt);
        giangVien.setKhoa(khoa);
        return giangVienRepository.save(giangVien);
    }

    private Student taoSinhVienWithHash(String username, String ma, String hoTen, String email, String sdt,
                                         Faculty khoa, Major nganh, AdministrativeClass lop, String encodedPassword) {
        User user = taoUserWithHash(username, encodedPassword, Role.SINH_VIEN);
        Student sinhVien = new Student();
        sinhVien.setUser(user);
        sinhVien.setMa(ma);
        sinhVien.setHoTen(hoTen);
        sinhVien.setEmail(email);
        sinhVien.setSoDienThoai(sdt);
        sinhVien.setKhoa(khoa);
        sinhVien.setNganh(nganh);
        sinhVien.setLopHanhChinh(lop);
        return sinhVienRepository.save(sinhVien);
    }

    private CourseClass taoLopHocPhan(String ma, Subject monHoc, Semester hocKy, Lecturer giangVien,
                                     String phong, int thu, int tietBatDau, int soTiet, int siSoToiDa) {
        CourseClass lop = new CourseClass();
        lop.setMa(ma);
        lop.setMonHoc(monHoc);
        lop.setHocKy(hocKy);
        lop.setGiangVien(giangVien);
        lop.setPhong(phong);
        lop.setThu(thu);
        lop.setTietBatDau(tietBatDau);
        lop.setSoTiet(soTiet);
        lop.setSiSoToiDa(siSoToiDa);
        lop.setSiSoHienTai(0);
        lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
        return lopHocPhanRepository.save(lop);
    }

    private void dangKyVoiDiem(Student sinhVien, CourseClass lop, double quaTrinh, double cuoiKy) {
        CourseRegistration dangKy = dangKyHienTai(sinhVien, lop);
        if (dangKy == null) return;
        Grade diem = new Grade();
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(quaTrinh);
        diem.setDiemCuoiKy(cuoiKy);
        diem.setDiemTongKet(Math.round((0.4 * quaTrinh + 0.6 * cuoiKy) * 10.0) / 10.0);
        diemRepository.save(diem);
        if (lop.getTrangThaiBangDiem() != GradeSheetStatus.DA_CONG_BO) {
            lop.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
            lop.setDuyetBangDiemLuc(LocalDateTime.now().minusDays(30));
            lop.setCongBoBangDiemLuc(LocalDateTime.now().minusDays(25));
            lopHocPhanRepository.save(lop);
        }
    }

    private CourseRegistration dangKyHienTai(Student sinhVien, CourseClass lop) {
        if (dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(sinhVien.getId(), lop.getId()).isPresent()) {
            return null;
        }
        CourseRegistration dangKy = new CourseRegistration();
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lop);
        dangKy.setThoiGianDangKy(LocalDateTime.now());
        dangKy.setTrangThai(RegistrationStatus.DA_DANG_KY);
        lop.setSiSoHienTai(lop.getSiSoHienTai() + 1);
        if (lop.getSiSoHienTai() > lop.getSiSoToiDa()) {
            lop.setSiSoToiDa(lop.getSiSoHienTai() + 10);
        }
        lopHocPhanRepository.save(lop);
        return dangKyHocPhanRepository.save(dangKy);
    }

    private static String stripAccents(String s) {
        String nfdNormalizedString = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }

    private static String generatePhone(int index) {
        String[] prefixes = {"090", "091", "098", "097", "038", "039", "086", "077", "079", "083"};
        String prefix = prefixes[index % prefixes.length];
        String num = String.format("%07d", (1000000 + (index * 9871) % 9000000));
        return prefix + num;
    }
    private void seedPhongHocNeuChuaCo() {
        if (phongHocRepository == null || phongHocRepository.count() > 0) {
            return;
        }
        log.info("Khởi tạo danh mục phòng học mẫu...");
        List<Classroom> danhSach = new ArrayList<>();
        danhSach.add(new Classroom("A101", "Phòng học A101", "Nhà A", 1, "Tầng 1, Nhà A", 80, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa, Wi-Fi chuyên dụng", ClassroomStatus.HOAT_DONG, "Phòng học tiêu chuẩn khoa CNTT"));
        danhSach.add(new Classroom("A102", "Phòng học A102", "Nhà A", 1, "Tầng 1, Nhà A", 80, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa, Wi-Fi chuyên dụng", ClassroomStatus.HOAT_DONG, "Phòng học lý thuyết"));
        danhSach.add(new Classroom("A103", "Phòng học A103", "Nhà A", 1, "Tầng 1, Nhà A", 80, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học lý thuyết"));
        danhSach.add(new Classroom("A104", "Phòng học A104", "Nhà A", 1, "Tầng 1, Nhà A", 80, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học lý thuyết"));
        danhSach.add(new Classroom("A105", "Phòng học A105", "Nhà A", 1, "Tầng 1, Nhà A", 80, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học lý thuyết"));

        danhSach.add(new Classroom("B201", "Phòng học B201", "Nhà B", 2, "Tầng 2, Nhà B", 60, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học khoa Ngoại ngữ"));
        danhSach.add(new Classroom("B202", "Phòng học B202", "Nhà B", 2, "Tầng 2, Nhà B", 60, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học khoa Ngoại ngữ"));
        danhSach.add(new Classroom("LAB01", "Phòng thực hành máy tính 01", "Nhà B", 3, "Tầng 3, Nhà B", 45, "THUC_HANH_MAY_TINH", "45 Dàn máy tính (PC), Máy chiếu, Điều hòa, Mạng LAN Gigabit", ClassroomStatus.HOAT_DONG, "Lab thực hành lập trình"));
        danhSach.add(new Classroom("LAB02", "Phòng thực hành máy tính 02", "Nhà B", 3, "Tầng 3, Nhà B", 45, "THUC_HANH_MAY_TINH", "45 Dàn máy tính (PC), Máy chiếu, Điều hòa, Mạng LAN Gigabit", ClassroomStatus.HOAT_DONG, "Lab thực hành mạng máy tính"));

        danhSach.add(new Classroom("C301", "Phòng học C301", "Tòa C", 3, "Tầng 3, Tòa C", 70, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học khoa Kinh tế"));
        danhSach.add(new Classroom("C302", "Phòng học C302", "Tòa C", 3, "Tầng 3, Tòa C", 70, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học khoa Kinh tế"));
        danhSach.add(new Classroom("C303", "Phòng học C303", "Tòa C", 3, "Tầng 3, Tòa C", 70, "LY_THUYET", "Máy chiếu, Điều hòa, Micro & Loa", ClassroomStatus.HOAT_DONG, "Phòng học đa năng"));

        danhSach.add(new Classroom("D401", "Giảng đường D401", "Tòa D", 4, "Tầng 4, Tòa D", 120, "HOI_TRUONG", "Máy chiếu công suất lớn, Âm thanh hội trường, Điều hòa, Camera Hybrid", ClassroomStatus.HOAT_DONG, "Giảng đường bậc thang"));
        danhSach.add(new Classroom("HT01", "Hội trường lớn A", "Tòa Hiệu bộ", 1, "Tầng 1, Tòa Hiệu bộ", 300, "HOI_TRUONG", "Màn hình LED P2.5, Âm thanh vòm, Hệ thống ánh sáng sân khấu, Điều hòa trung tâm", ClassroomStatus.HOAT_DONG, "Hội trường tổ chức sự kiện"));

        // Phòng bảo trì và phòng ngừng sử dụng để kiểm thử nghiệp vụ
        danhSach.add(new Classroom("B204-BT", "Phòng B204 (Bảo trì)", "Nhà B", 2, "Tầng 2, Nhà B", 50, "LY_THUYET", "Điều hòa", ClassroomStatus.BAO_TRI, "Đang bảo dưỡng hệ thống máy chiếu và trần nhà"));
        danhSach.add(new Classroom("C305-NG", "Phòng C305 (Ngừng dùng)", "Tòa C", 3, "Tầng 3, Tòa C", 40, "DA_NANG", "Bàn ghế cũ", ClassroomStatus.NGUNG_SU_DUNG, "Chuyển đổi thành kho thiết bị khoa"));

        for (int i = 0; i < 50; i++) {
            String maP = "P" + (100 + i);
            danhSach.add(new Classroom(maP, "Phòng học " + maP, "Khu P", (i % 4) + 1, "Tầng " + ((i % 4) + 1) + ", Khu P", 60, "LY_THUYET", "Máy chiếu, Điều hòa, Bảng phấn", ClassroomStatus.HOAT_DONG, null));
        }

        phongHocRepository.saveAll(danhSach);
        log.info("Đã tạo {} phòng học mẫu", danhSach.size());
    }

    private Map<Long, Curriculum> seedCurriculums(Map<String, Subject> monMap, Major... majors) {
        Map<Long, Curriculum> result = new HashMap<>();
        if (chuongTrinhRepository == null) {
            return result;
        }
        for (Major major : majors) {
            if (major == null) continue;
            Curriculum c = new Curriculum();
            c.setNganh(major);
            c.setKhoaNhapHoc("K2024");
            c.setPhienBan("2024.1");

            CurriculumGroup g1 = new CurriculumGroup();
            g1.setChuongTrinh(c);
            g1.setTen("Kiến thức đại cương");
            g1.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);
            g1.setTinChiYeuCau(14);
            for (String code : new String[]{"MAT101", "MAT102", "PHI101", "ENG101", "GEN101", "ECO101"}) {
                if (monMap.containsKey(code)) g1.getMonHoc().add(monMap.get(code));
            }

            CurriculumGroup g2 = new CurriculumGroup();
            g2.setChuongTrinh(c);
            g2.setTen("Cơ sở ngành");
            g2.setLoai(CurriculumGroup.LoaiNhom.BAT_BUOC);
            g2.setTinChiYeuCau(18);
            for (String code : new String[]{"INT101", "INT102", "INT201", "INT202", "INT203", "INT301"}) {
                if (monMap.containsKey(code)) g2.getMonHoc().add(monMap.get(code));
            }

            CurriculumGroup g3 = new CurriculumGroup();
            g3.setChuongTrinh(c);
            g3.setTen("Chuyên ngành & Tự chọn");
            g3.setLoai(CurriculumGroup.LoaiNhom.TU_CHON);
            g3.setTinChiYeuCau(15);
            for (String code : new String[]{"INT302", "INT303", "INT304", "INT401", "CLD301", "SEC301"}) {
                if (monMap.containsKey(code)) g3.getMonHoc().add(monMap.get(code));
            }

            c.getNhom().add(g1);
            c.getNhom().add(g2);
            c.getNhom().add(g3);
            result.put(major.getId(), chuongTrinhRepository.save(c));
        }
        return result;
    }

    private void seedRichFeatureData(List<Student> sinhVienList, List<Lecturer> giangVienList,
                                     Semester hocKyHienTai, CourseClass int30301, CourseClass int20101,
                                     CourseClass matLop, CourseClass ecoLop, CourseClass lopCu,
                                     Map<String, Subject> monMap) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        User gv01User = giangVienList.get(0).getUser();

        // 1. Lịch sử đăng ký / hủy (đan xen DANG_KY và DA_HUY)
        if (lichSuDangKyRepository != null) {
            for (int i = 0; i < Math.min(20, sinhVienList.size()); i++) {
                if (i == 1) continue; // giữ sv02 trống kỳ hiện tại
                Student s = sinhVienList.get(i);
                RegistrationHistory h1 = new RegistrationHistory();
                h1.setSinhVien(s);
                h1.setLopHocPhan(int30301);
                h1.setHocKyId(hocKyHienTai.getId());
                h1.setTrangThai(RegistrationStatus.DA_DANG_KY);
                h1.setThoiGianDangKy(now.minusDays(4));
                h1.setThoiDiem(now.minusDays(4));
                lichSuDangKyRepository.save(h1);

                if (i % 2 == 0) {
                    RegistrationHistory h2 = new RegistrationHistory();
                    h2.setSinhVien(s);
                    h2.setLopHocPhan(ecoLop);
                    h2.setHocKyId(hocKyHienTai.getId());
                    h2.setTrangThai(RegistrationStatus.DA_HUY);
                    h2.setThoiGianDangKy(now.minusDays(5));
                    h2.setThoiGianHuy(now.minusDays(3));
                    h2.setThoiDiem(now.minusDays(3));
                    h2.setLyDoHuy("Đổi sang lớp phù hợp lịch học hơn");
                    lichSuDangKyRepository.save(h2);
                }
            }
        }

        // 2. Buổi học & Điểm danh (đan xen CO_MAT, MUON, VANG_CO_PHEP, VANG)
        if (buoiHocRepository != null && diemDanhRepository != null) {
            AttendanceStatus[] statuses = {
                    AttendanceStatus.CO_MAT, AttendanceStatus.CO_MAT, AttendanceStatus.MUON,
                    AttendanceStatus.CO_PHEP, AttendanceStatus.VANG
            };
            for (int sIdx = 1; sIdx <= 4; sIdx++) {
                ClassSession session = new ClassSession();
                session.setLopHocPhan(int30301);
                session.setNgayHoc(today.minusDays(sIdx * 3L));
                session.setCaHoc(2);
                session.setPhongHoc(int30301.getPhong());
                session.setTietBatDau(int30301.getTietBatDau());
                session.setSoTiet(int30301.getSoTiet());
                session.setNoiDung("Buổi học số " + (5 - sIdx) + " - Mạng máy tính");
                session.setNguoiTao(gv01User);
                session.setThoiGianTao(now.minusDays(sIdx * 3L));
                session = buoiHocRepository.save(session);

                for (int i = 0; i < Math.min(20, sinhVienList.size()); i++) {
                    if (i == 1) continue;
                    Student st = sinhVienList.get(i);
                    AttendanceRecord rec = new AttendanceRecord();
                    rec.setBuoiHoc(session);
                    rec.setSinhVien(st);
                    rec.setTrangThai(statuses[(i + sIdx) % statuses.length]);
                    rec.setNguoiTao(gv01User);
                    rec.setThoiGianTao(now.minusDays(sIdx * 3L));
                    diemDanhRepository.save(rec);
                }
            }
        }

        // 3. Yêu cầu hỗ trợ sinh viên (đan xen các loại & trạng thái)
        if (yeuCauHoTroRepository != null) {
            SupportRequestType[] types = SupportRequestType.values();
            SupportRequestStatus[] reqStatuses = {
                    SupportRequestStatus.MOI_TAO, SupportRequestStatus.DANG_XU_LY,
                    SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN, SupportRequestStatus.DA_GIAI_QUYET,
                    SupportRequestStatus.TU_CHOI
            };
            for (int i = 0; i < 12; i++) {
                Student st = sinhVienList.get(i == 1 ? 0 : i);
                SupportRequest req = new SupportRequest();
                req.setMaPhieu(String.format("HT-SEED-%03d", i + 1));
                req.setSinhVien(st);
                req.setNguoiTao(st.getUser());
                req.setLoaiYeuCau(types[i % types.length]);
                req.setTieuDe("Yêu cầu hỗ trợ học vụ mẫu #" + (i + 1));
                req.setNoiDungGoc("Sinh viên " + st.getMa() + " cần hỗ trợ kiểm tra thông tin học phần / điểm số.");
                req.setHocKy(hocKyHienTai);
                req.setMonHoc(monMap.get("INT201"));
                req.setTrangThai(reqStatuses[i % reqStatuses.length]);
                req.setMucUuTien(i % 3 == 0 ? SupportRequestPriority.CAO : SupportRequestPriority.TRUNG_BINH);
                req.setCreatedAt(now.minusDays(12 - i));
                req.setUpdatedAt(now.minusDays(12 - i));
                yeuCauHoTroRepository.save(req);
            }
        }

        // 4. Cảnh báo học tập sớm cho sinh viên có điểm trượt
        if (canhBaoHocTapRepository != null) {
            int[] warnIndices = {2, 8, 11, 14, 17}; // sv03, sv09, sv12, sv15, sv18
            AcademicAlertStatus[] alertStatuses = {
                    AcademicAlertStatus.DANG_CANH_BAO, AcademicAlertStatus.DANG_XU_LY, AcademicAlertStatus.DA_XU_LY
            };
            for (int idx = 0; idx < warnIndices.length; idx++) {
                Student st = sinhVienList.get(warnIndices[idx]);
                AcademicAlert alert = new AcademicAlert();
                alert.setSinhVien(st);
                alert.setLoaiLuat(AcademicAlertRuleType.GPA_GIAM);
                alert.setMaLuat("RULE_GPA_DROP");
                alert.setTenLuat("Cảnh báo kết quả học tập yếu / cần học lại");
                alert.setMucDo(idx % 2 == 0 ? AcademicAlertSeverity.CAO : AcademicAlertSeverity.TRUNG_BINH);
                alert.setLyDo("Sinh viên có môn học đạt điểm dưới 5.0 cần đăng ký học lại");
                alert.setCongThucGiaiThich("soMonChuaDat >= 1");
                alert.setDuLieuDanChung("{\"soMonChuaDat\": 1}");
                alert.setNguCanh("HK-" + hocKyHienTai.getId());
                alert.setKhoaChongTrung("SEED-" + st.getMa() + "-FAILED");
                alert.setTrangThai(alertStatuses[idx % alertStatuses.length]);
                alert.setNgayTao(now.minusDays(idx + 1L));
                alert.setCapNhatLuc(now.minusDays(idx + 1L));
                canhBaoHocTapRepository.save(alert);
            }
        }

        // 5. Thông báo đan xen đã đọc / chưa đọc cho sv01..sv20 và gv01..gv03
        if (thongBaoRepository != null) {
            for (int i = 0; i < Math.min(20, sinhVienList.size()); i++) {
                Student st = sinhVienList.get(i);
                Notification n1 = new Notification();
                n1.setRecipient(st.getUser());
                n1.setEventType("REGISTRATION_OPEN");
                n1.setIdempotencyKey("SEED-REG-" + st.getMa());
                n1.setTitle("Mở đợt đăng ký học phần kỳ hiện tại");
                n1.setMessage("Cổng đăng ký học phần đã mở. Sinh viên kiểm tra lộ trình và hoàn tất đăng ký đúng hạn.");
                n1.setPriority("HIGH");
                n1.setTargetUrl("/dang-ky");
                n1.setCreatedAt(now.minusDays(2));
                if (i % 2 == 0) n1.setReadAt(now.minusDays(1));
                thongBaoRepository.save(n1);
            }
        }
    }
}
