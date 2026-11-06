package com.edu.registration.config;

import com.edu.registration.entity.DangKyHocPhan;
import com.edu.registration.entity.Diem;
import com.edu.registration.entity.GiangVien;
import com.edu.registration.entity.HocKy;
import com.edu.registration.entity.Khoa;
import com.edu.registration.entity.LopHanhChinh;
import com.edu.registration.entity.LopHocPhan;
import com.edu.registration.entity.MonHoc;
import com.edu.registration.entity.MonTienQuyet;
import com.edu.registration.entity.Nganh;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.SinhVien;
import com.edu.registration.entity.TrangThaiBangDiem;
import com.edu.registration.entity.TrangThaiDangKy;
import com.edu.registration.entity.User;
import com.edu.registration.repository.DangKyHocPhanRepository;
import com.edu.registration.repository.DiemRepository;
import com.edu.registration.repository.GiangVienRepository;
import com.edu.registration.repository.HocKyRepository;
import com.edu.registration.repository.KhoaRepository;
import com.edu.registration.repository.LopHanhChinhRepository;
import com.edu.registration.repository.LopHocPhanRepository;
import com.edu.registration.repository.MonHocRepository;
import com.edu.registration.repository.MonTienQuyetRepository;
import com.edu.registration.repository.NganhRepository;
import com.edu.registration.repository.SinhVienRepository;
import com.edu.registration.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final KhoaRepository khoaRepository;
    private final NganhRepository nganhRepository;
    private final LopHanhChinhRepository lopHanhChinhRepository;
    private final MonHocRepository monHocRepository;
    private final MonTienQuyetRepository monTienQuyetRepository;
    private final HocKyRepository hocKyRepository;
    private final GiangVienRepository giangVienRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DiemRepository diemRepository;
    private final PasswordEncoder passwordEncoder;

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

    public DataSeeder(UserRepository userRepository, KhoaRepository khoaRepository,
                      NganhRepository nganhRepository, LopHanhChinhRepository lopHanhChinhRepository,
                      MonHocRepository monHocRepository, MonTienQuyetRepository monTienQuyetRepository,
                      HocKyRepository hocKyRepository, GiangVienRepository giangVienRepository,
                      SinhVienRepository sinhVienRepository, LopHocPhanRepository lopHocPhanRepository,
                      DangKyHocPhanRepository dangKyHocPhanRepository, DiemRepository diemRepository,
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
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0 && sinhVienRepository.count() >= 500) {
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
            hocKyRepository.deleteAllInBatch();
            userRepository.deleteAllInBatch();
        }

        String encodedDefaultPassword = passwordEncoder.encode("123456");
        String encodedAdminPassword = passwordEncoder.encode("admin123");

        // 1. Tạo 4 Khoa
        Khoa cntt = taoKhoa("CNTT", "Công nghệ thông tin");
        Khoa kt = taoKhoa("KT", "Kinh tế & Quản lý");
        Khoa dt = taoKhoa("DT", "Điện tử - Viễn thông");
        Khoa nn = taoKhoa("NN", "Ngoại ngữ ứng dụng");

        // 2. Tạo 8 Ngành
        Nganh cnpm = taoNganh("CNPM", "Kỹ thuật phần mềm", cntt);
        Nganh httt = taoNganh("HTTT", "Hệ thống thông tin", cntt);
        Nganh khmt = taoNganh("KHMT", "Khoa học máy tính", cntt);
        Nganh attt = taoNganh("ATTT", "An toàn thông tin", cntt);
        Nganh qtkd = taoNganh("QTKD", "Quản trị kinh doanh", kt);
        Nganh tcnh = taoNganh("TCNH", "Tài chính - Ngân hàng", kt);
        Nganh dtvt = taoNganh("DTVT", "Kỹ thuật Điện tử Viễn thông", dt);
        Nganh nna = taoNganh("NNA", "Ngôn ngữ Anh thương mại", nn);

        // 3. Tạo 10 Lớp hành chính
        LopHanhChinh cnpm01 = taoLop("CNPM01", "Kỹ thuật phần mềm 01", cnpm);
        LopHanhChinh cnpm02 = taoLop("CNPM02", "Kỹ thuật phần mềm 02", cnpm);
        LopHanhChinh httt01 = taoLop("HTTT01", "Hệ thống thông tin 01", httt);
        LopHanhChinh httt02 = taoLop("HTTT02", "Hệ thống thông tin 02", httt);
        LopHanhChinh khmt01 = taoLop("KHMT01", "Khoa học máy tính 01", khmt);
        LopHanhChinh attt01 = taoLop("ATTT01", "An toàn thông tin 01", attt);
        LopHanhChinh qtkd01 = taoLop("QTKD01", "Quản trị kinh doanh 01", qtkd);
        LopHanhChinh tcnh01 = taoLop("TCNH01", "Tài chính - Ngân hàng 01", tcnh);
        LopHanhChinh dtvt01 = taoLop("DTVT01", "Điện tử Viễn thông 01", dtvt);
        LopHanhChinh nna01 = taoLop("NNA01", "Ngôn ngữ Anh 01", nna);

        LopHanhChinh[] danhSachLopHanhChinh = {
                cnpm01, cnpm02, httt01, httt02, khmt01, attt01, qtkd01, tcnh01, dtvt01, nna01
        };

        // 4. Tạo 50 Môn học chia đều 8 kỳ
        Map<String, MonHoc> monMap = new HashMap<>();

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
        HocKy hocKyCu2 = taoHocKy("Học kỳ 1", "2025-2026",
                homNay.minusMonths(14), homNay.minusMonths(9),
                homNay.minusMonths(15), homNay.minusMonths(14).plusDays(20), 12, 24);
        HocKy hocKyCu = taoHocKy("Học kỳ 2", "2025-2026",
                homNay.minusMonths(8), homNay.minusMonths(4),
                homNay.minusMonths(9), homNay.minusMonths(8).plusDays(20), 12, 24);
        HocKy hocKyHienTai = taoHocKy("Học kỳ 1", "2026-2027",
                homNay.minusDays(14), homNay.plusDays(90),
                homNay.minusDays(5), homNay.plusDays(20), 12, 24);
        taoHocKy("Học kỳ 2", "2024-2025",
                homNay.minusMonths(20), homNay.minusMonths(16),
                homNay.minusMonths(21), homNay.minusMonths(20).plusDays(20), 12, 24);
        taoHocKy("Học kỳ 1", "2024-2025",
                homNay.minusMonths(26), homNay.minusMonths(22),
                homNay.minusMonths(27), homNay.minusMonths(26).plusDays(20), 12, 24);


        // 6. Tạo Quản trị viên
        taoUserWithHash("admin", encodedAdminPassword, Role.QUAN_TRI);

        // 7. Tạo 50 Giảng viên với tên người thật và học hàm TS, ThS, PGS.TS
        List<GiangVien> giangVienList = new ArrayList<>();
        Khoa[] khoaArr = {cntt, kt, dt, nn};

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
            Khoa assignedKhoa = khoaArr[(i - 1) % khoaArr.length];

            giangVienList.add(taoGiangVienWithHash(username, ma, hoTen, email, sdt, assignedKhoa, encodedDefaultPassword));
        }

        GiangVien gv01 = giangVienList.get(0);
        GiangVien gv02 = giangVienList.get(1);
        GiangVien gv03 = giangVienList.get(2);

        // 8. Tạo 500 Sinh viên với tên người thật
        List<SinhVien> sinhVienList = new ArrayList<>();

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

            LopHanhChinh assignedLop = danhSachLopHanhChinh[(i - 1) % danhSachLopHanhChinh.length];
            Nganh assignedNganh = assignedLop.getNganh();
            Khoa assignedKhoa = assignedNganh.getKhoa();

            sinhVienList.add(taoSinhVienWithHash(username, ma, hoTen, email, sdt, assignedKhoa, assignedNganh, assignedLop, encodedDefaultPassword));
        }

        SinhVien sv01 = sinhVienList.get(0);
        SinhVien sv02 = sinhVienList.get(1);
        SinhVien sv03 = sinhVienList.get(2);
        SinhVien sv04 = sinhVienList.get(3);
        SinhVien sv05 = sinhVienList.get(4);

        // 9. Tạo các Lớp học phần
        // Lớp quá khứ
        LopHocPhan lopMatCu = taoLopHocPhan("MAT101.01-HK1", monMap.get("MAT101"), hocKyCu2, gv02, "A105", 4, 7, 3, 60);
        LopHocPhan lopEcoCu = taoLopHocPhan("ECO101.01-HK1", monMap.get("ECO101"), hocKyCu2, gv03, "C301", 3, 4, 3, 60);
        LopHocPhan lopCu = taoLopHocPhan("INT101.01-HK2", monMap.get("INT101"), hocKyCu, gv01, "A101", 2, 1, 3, 60);
        LopHocPhan lopMat102Cu = taoLopHocPhan("MAT102.01-HK1", monMap.get("MAT102"), hocKyCu2, giangVienList.get(3), "A102", 5, 1, 3, 60);
        LopHocPhan lopPhi101Cu = taoLopHocPhan("PHI101.01-HK1", monMap.get("PHI101"), hocKyCu2, giangVienList.get(4), "C302", 6, 4, 3, 60);
        LopHocPhan lopEng101Cu = taoLopHocPhan("ENG101.01-HK1", monMap.get("ENG101"), hocKyCu2, giangVienList.get(5), "B201", 3, 7, 2, 60);
        LopHocPhan lopGen101Cu = taoLopHocPhan("GEN101.01-HK1", monMap.get("GEN101"), hocKyCu2, giangVienList.get(6), "C303", 7, 1, 2, 60);

        LopHocPhan lopInt201Cu = taoLopHocPhan("INT201.01-HK2", monMap.get("INT201"), hocKyCu, giangVienList.get(7), "A102", 3, 1, 3, 60);
        LopHocPhan lopInt102Cu = taoLopHocPhan("INT102.01-HK2", monMap.get("INT102"), hocKyCu, giangVienList.get(8), "A103", 4, 1, 3, 60);
        LopHocPhan lopMat103Cu = taoLopHocPhan("MAT103.01-HK2", monMap.get("MAT103"), hocKyCu, giangVienList.get(9), "A104", 5, 4, 3, 60);
        LopHocPhan lopPhy101Cu = taoLopHocPhan("PHY101.01-HK2", monMap.get("PHY101"), hocKyCu, giangVienList.get(10), "D401", 6, 1, 3, 60);
        LopHocPhan lopPhi102Cu = taoLopHocPhan("PHI102.01-HK2", monMap.get("PHI102"), hocKyCu, giangVienList.get(11), "C301", 2, 7, 2, 60);
        LopHocPhan lopEng102Cu = taoLopHocPhan("ENG102.01-HK2", monMap.get("ENG102"), hocKyCu, giangVienList.get(12), "B202", 4, 7, 2, 60);

        LopHocPhan[] pastClasses = {
                lopMatCu, lopEcoCu, lopCu, lopMat102Cu, lopPhi101Cu, lopEng101Cu, lopGen101Cu,
                lopInt201Cu, lopInt102Cu, lopMat103Cu, lopPhy101Cu, lopPhi102Cu, lopEng102Cu
        };

        // Lớp học phần kỳ hiện tại
        LopHocPhan int20101 = taoLopHocPhan("INT201.01", monMap.get("INT201"), hocKyHienTai, gv01, "A101", 2, 1, 3, 60);
        LopHocPhan int20102 = taoLopHocPhan("INT201.02", monMap.get("INT201"), hocKyHienTai, gv02, "A102", 4, 4, 3, 60);
        taoLopHocPhan("INT301.01", monMap.get("INT301"), hocKyHienTai, gv01, "B201", 3, 1, 3, 60);
        taoLopHocPhan("INT302.01", monMap.get("INT302"), hocKyHienTai, gv02, "A103", 5, 1, 3, 60);
        LopHocPhan int30301 = taoLopHocPhan("INT303.01", monMap.get("INT303"), hocKyHienTai, gv01, "B202", 6, 4, 3, 60);
        taoLopHocPhan("INT304.01", monMap.get("INT304"), hocKyHienTai, gv02, "A104", 7, 1, 3, 60);
        LopHocPhan ecoLop = taoLopHocPhan("ECO101.01", monMap.get("ECO101"), hocKyHienTai, gv03, "C301", 3, 4, 3, 60);
        LopHocPhan matLop = taoLopHocPhan("MAT101.01", monMap.get("MAT101"), hocKyHienTai, gv02, "A105", 4, 7, 3, 60);

        // Mở thêm các lớp cho các giảng viên khác phụ trách
        List<LopHocPhan> currentClasses = new ArrayList<>();
        currentClasses.add(int20101);
        currentClasses.add(int20102);
        currentClasses.add(int30301);
        currentClasses.add(ecoLop);
        currentClasses.add(matLop);

        for (int i = 13; i < 50; i++) {
            GiangVien gv = giangVienList.get(i);
            String[] subjectCodes = {"INT202", "INT203", "MAT201", "SEC301", "INT401", "INT403", "CLD301", "INT404", "INT408"};
            String subCode = subjectCodes[i % subjectCodes.length];
            MonHoc mon = monMap.get(subCode);
            if (mon != null) {
                String maLop = mon.getMa() + "." + String.format("%02d", (i % 3) + 1);
                if (!lopHocPhanRepository.existsByMa(maLop)) {
                    LopHocPhan lhp = taoLopHocPhan(maLop, mon, hocKyHienTai, gv, "P" + (100 + i), (i % 6) + 2, ((i % 3) * 3) + 1, 3, 60);
                    currentClasses.add(lhp);
                }
            }
        }

        // 10. Đăng ký & Cho điểm
        // sv01
        dangKyVoiDiem(sv01, lopCu, 8.5, 9.0);
        dangKyVoiDiem(sv01, lopMatCu, 7.0, 8.0);
        dangKyVoiDiem(sv01, lopEcoCu, 6.5, 7.0);
        dangKyHienTai(sv01, int30301);
        dangKyHienTai(sv01, int20101);
        dangKyHienTai(sv01, matLop);
        dangKyHienTai(sv01, ecoLop);

        // sv02 (không đăng ký lớp hiện tại để phục vụ E2E test)
        dangKyVoiDiem(sv02, lopCu, 7.0, 8.0);
        dangKyVoiDiem(sv02, lopMatCu, 8.0, 8.5);

        // sv03 (Điểm yếu < 5.0 -> Cần học lại / thi lại)
        dangKyVoiDiem(sv03, lopCu, 3.5, 4.0);
        dangKyVoiDiem(sv03, lopEcoCu, 5.0, 6.0);
        dangKyHienTai(sv03, int30301);
        dangKyHienTai(sv03, matLop);
        dangKyHienTai(sv03, ecoLop);

        // sv04, sv05
        dangKyHienTai(sv04, int30301);
        dangKyHienTai(sv04, matLop);
        dangKyHienTai(sv04, ecoLop);
        dangKyHienTai(sv05, int30301);
        dangKyHienTai(sv05, matLop);
        dangKyHienTai(sv05, ecoLop);

        // sv06..sv20 giữ nguyên tương thích test
        for (int i = 6; i <= 20; i++) {
            SinhVien sv = sinhVienList.get(i - 1);
            dangKyVoiDiem(sv, lopCu, 6.5 + (i % 3), 7.0 + (i % 2));
            dangKyHienTai(sv, i % 2 == 0 ? int20102 : int20101);
            dangKyHienTai(sv, int30301);
            dangKyHienTai(sv, matLop);
            dangKyHienTai(sv, ecoLop);
        }

        // sv21..sv500: Phân bố GPA đầy đủ từ Xuất sắc, Giỏi, Khá, Trung bình đến Yếu / Học lại
        // Chỉ đăng ký vào các lớp chung khác, tránh làm thay đổi sĩ số các lớp test (int30301, int20101, int20102)
        List<LopHocPhan> generalCurrentClasses = new ArrayList<>();
        for (int j = 5; j < currentClasses.size(); j++) {
            generalCurrentClasses.add(currentClasses.get(j));
        }
        if (generalCurrentClasses.isEmpty()) {
            generalCurrentClasses.add(matLop);
            generalCurrentClasses.add(ecoLop);
        }

        for (int i = 21; i <= 500; i++) {
            SinhVien sv = sinhVienList.get(i - 1);
            int tier = i % 100;
            double baseScore;

            if (tier < 10) {
                // Xuất sắc (10%): GPA 9.0 - 10.0
                baseScore = 8.8 + ((i * 3) % 10) * 0.1;
            } else if (tier < 35) {
                // Giỏi (25%): GPA 8.0 - 8.9
                baseScore = 8.0 + ((i * 7) % 8) * 0.1;
            } else if (tier < 75) {
                // Khá (40%): GPA 6.5 - 7.9
                baseScore = 6.5 + ((i * 11) % 15) * 0.1;
            } else if (tier < 90) {
                // Trung bình (15%): GPA 5.0 - 6.4
                baseScore = 5.0 + ((i * 5) % 14) * 0.1;
            } else {
                // Yếu / Cần học lại (10%): GPA < 5.0 (học lại)
                baseScore = 3.2 + ((i * 9) % 16) * 0.1;
            }

            // Gán 2 lớp quá khứ có điểm
            LopHocPhan lopPast1 = pastClasses[i % 7];
            LopHocPhan lopPast2 = pastClasses[7 + (i % 6)];

            double qt1 = Math.min(10.0, Math.max(1.0, Math.round((baseScore + ((i % 5) - 2) * 0.4) * 10.0) / 10.0));
            double ck1 = Math.min(10.0, Math.max(1.0, Math.round((baseScore + ((i % 7) - 3) * 0.3) * 10.0) / 10.0));
            dangKyVoiDiem(sv, lopPast1, qt1, ck1);

            double qt2 = Math.min(10.0, Math.max(1.0, Math.round((baseScore + ((i % 3) - 1) * 0.3) * 10.0) / 10.0));
            double ck2 = Math.min(10.0, Math.max(1.0, Math.round((baseScore + ((i % 4) - 2) * 0.4) * 10.0) / 10.0));
            dangKyVoiDiem(sv, lopPast2, qt2, ck2);

            // Đăng ký học kỳ hiện tại vào các lớp mở rộng
            LopHocPhan curLop1 = generalCurrentClasses.get(i % generalCurrentClasses.size());
            dangKyHienTai(sv, curLop1);
            if (generalCurrentClasses.size() > 1 && i % 3 == 0) {
                LopHocPhan curLop2 = generalCurrentClasses.get((i + 3) % generalCurrentClasses.size());
                if (!curLop2.getId().equals(curLop1.getId()) && !curLop2.getMonHoc().getId().equals(curLop1.getMonHoc().getId())) {
                    dangKyHienTai(sv, curLop2);
                }
            }
        }

        log.info("Đã tạo thành công bộ dữ liệu hoàn chỉnh: 500 sinh viên, 50 giảng viên, 50 môn học chia đều các kỳ!");
    }

    private User taoUserWithHash(String username, String encodedPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setRole(role);
        user.setTrangThai(true);
        return userRepository.save(user);
    }

    private Khoa taoKhoa(String ma, String ten) {
        Khoa khoa = new Khoa();
        khoa.setMa(ma);
        khoa.setTen(ten);
        return khoaRepository.save(khoa);
    }

    private Nganh taoNganh(String ma, String ten, Khoa khoa) {
        Nganh nganh = new Nganh();
        nganh.setMa(ma);
        nganh.setTen(ten);
        nganh.setKhoa(khoa);
        return nganhRepository.save(nganh);
    }

    private LopHanhChinh taoLop(String ma, String ten, Nganh nganh) {
        LopHanhChinh lop = new LopHanhChinh();
        lop.setMa(ma);
        lop.setTen(ten);
        lop.setNganh(nganh);
        return lopHanhChinhRepository.save(lop);
    }

    private MonHoc taoMon(String ma, String ten, int soTinChi, Khoa khoa) {
        MonHoc monHoc = new MonHoc();
        monHoc.setMa(ma);
        monHoc.setTen(ten);
        monHoc.setSoTinChi(soTinChi);
        monHoc.setKhoa(khoa);
        return monHocRepository.save(monHoc);
    }

    private void taoTienQuyet(MonHoc monHoc, MonHoc tienQuyet) {
        if (monHoc != null && tienQuyet != null) {
            monTienQuyetRepository.save(new MonTienQuyet(monHoc, tienQuyet));
        }
    }

    private HocKy taoHocKy(String ten, String namHoc, LocalDate batDau, LocalDate ketThuc,
                           LocalDate dangKyTu, LocalDate dangKyDen, int min, int max) {
        HocKy hocKy = new HocKy();
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

    private GiangVien taoGiangVienWithHash(String username, String ma, String hoTen, String email, String sdt, Khoa khoa, String encodedPassword) {
        User user = taoUserWithHash(username, encodedPassword, Role.GIANG_VIEN);
        GiangVien giangVien = new GiangVien();
        giangVien.setUser(user);
        giangVien.setMa(ma);
        giangVien.setHoTen(hoTen);
        giangVien.setEmail(email);
        giangVien.setSoDienThoai(sdt);
        giangVien.setKhoa(khoa);
        return giangVienRepository.save(giangVien);
    }

    private SinhVien taoSinhVienWithHash(String username, String ma, String hoTen, String email, String sdt,
                                         Khoa khoa, Nganh nganh, LopHanhChinh lop, String encodedPassword) {
        User user = taoUserWithHash(username, encodedPassword, Role.SINH_VIEN);
        SinhVien sinhVien = new SinhVien();
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

    private LopHocPhan taoLopHocPhan(String ma, MonHoc monHoc, HocKy hocKy, GiangVien giangVien,
                                     String phong, int thu, int tietBatDau, int soTiet, int siSoToiDa) {
        LopHocPhan lop = new LopHocPhan();
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
        lop.setTrangThaiBangDiem(TrangThaiBangDiem.NHAP);
        return lopHocPhanRepository.save(lop);
    }

    private void dangKyVoiDiem(SinhVien sinhVien, LopHocPhan lop, double quaTrinh, double cuoiKy) {
        DangKyHocPhan dangKy = dangKyHienTai(sinhVien, lop);
        if (dangKy == null) return;
        Diem diem = new Diem();
        diem.setDangKy(dangKy);
        diem.setDiemQuaTrinh(quaTrinh);
        diem.setDiemCuoiKy(cuoiKy);
        diem.setDiemTongKet(Math.round((0.4 * quaTrinh + 0.6 * cuoiKy) * 10.0) / 10.0);
        diemRepository.save(diem);
        if (lop.getTrangThaiBangDiem() != TrangThaiBangDiem.DA_CONG_BO) {
            lop.setTrangThaiBangDiem(TrangThaiBangDiem.DA_CONG_BO);
            lop.setDuyetBangDiemLuc(LocalDateTime.now().minusDays(30));
            lop.setCongBoBangDiemLuc(LocalDateTime.now().minusDays(25));
            lopHocPhanRepository.save(lop);
        }
    }

    private DangKyHocPhan dangKyHienTai(SinhVien sinhVien, LopHocPhan lop) {
        if (dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(sinhVien.getId(), lop.getId()).isPresent()) {
            return null;
        }
        DangKyHocPhan dangKy = new DangKyHocPhan();
        dangKy.setSinhVien(sinhVien);
        dangKy.setLopHocPhan(lop);
        dangKy.setThoiGianDangKy(LocalDateTime.now());
        dangKy.setTrangThai(TrangThaiDangKy.DA_DANG_KY);
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
}
