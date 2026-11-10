package com.edu.registration.service;

import com.edu.registration.dto.AdminLookupDtos.*;
import com.edu.registration.entity.*;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** TASK-AD-08: tra cứu hồ sơ và lịch sử học tập/giảng dạy cho quản trị, có audit thao tác xem dữ liệu nhạy cảm. */
@Service
public class AdminLookupService {

    static final int MAX_SIZE = 100;
    private static final Set<String> KETQUA = Set.of("DAT", "CHUA_DAT", "CHUA_CO_DIEM");

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final CourseClassRepository classRepository;
    private final GradeRepository gradeRepository;
    private final AuditLogService auditLogService;

    public AdminLookupService(StudentRepository studentRepository, LecturerRepository lecturerRepository,
                              SemesterRepository semesterRepository,
                              CourseRegistrationRepository registrationRepository,
                              CourseClassRepository classRepository, GradeRepository gradeRepository,
                              AuditLogService auditLogService) {
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.semesterRepository = semesterRepository;
        this.registrationRepository = registrationRepository;
        this.classRepository = classRepository;
        this.gradeRepository = gradeRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public PageDto<StudentSummaryDto> timSinhVien(String keyword, int page, int size) {
        String kw = kw(keyword);
        PageRequest pr = pageRequest(page, size, Sort.by("ma", "id"));
        return auditLogService.execute("ADMIN_LOOKUP_STUDENT_SEARCH", "STUDENT", null, ctx -> {
            Page<Student> result = studentRepository.searchForAdminLookup(kw, pr);
            ctx.changes(Map.of(), Map.of("tuKhoa", kw, "trang", pr.getPageNumber(),
                    "soBanGhi", result.getNumberOfElements()));
            return toPage(result.map(this::toSummary));
        });
    }

    @Transactional
    public PageDto<LecturerSummaryDto> timGiangVien(String keyword, int page, int size) {
        String kw = kw(keyword);
        PageRequest pr = pageRequest(page, size, Sort.by("ma", "id"));
        return auditLogService.execute("ADMIN_LOOKUP_LECTURER_SEARCH", "LECTURER", null, ctx -> {
            Page<Lecturer> result = lecturerRepository.searchForAdminLookup(kw, pr);
            ctx.changes(Map.of(), Map.of("tuKhoa", kw, "trang", pr.getPageNumber(),
                    "soBanGhi", result.getNumberOfElements()));
            return toPage(result.map(this::toSummary));
        });
    }

    @Transactional
    public StudentHistoryDto lichSuSinhVien(Long id, Long hocKyId, String trangThai, String mucDich,
                                            int page, int size) {
        String purpose = purpose(mucDich);
        checkSemester(hocKyId);
        String statusFilter = statusFilter(trangThai);
        PageRequest pr = pageRequest(page, size, Sort.unsorted());
        return auditLogService.execute("ADMIN_VIEW_STUDENT_HISTORY", "STUDENT", id, ctx -> {
            Student sv = studentRepository.findWithAcademicProfileById(id)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy sinh viên"));
            List<CourseRegistration> regs = registrationRepository.findAllForAdminHistory(id, hocKyId);
            Map<Long, Grade> grades = new HashMap<>();
            if (!regs.isEmpty()) {
                gradeRepository.findByDangKyIdIn(regs.stream().map(CourseRegistration::getId).toList())
                        .forEach(g -> grades.put(g.getDangKy().getId(), g));
            }
            Comparator<StudentHistoryItemDto> order = Comparator
                    .comparing(StudentHistoryItemDto::ngayBatDauHocKy, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                    .thenComparing(StudentHistoryItemDto::hocKyId, Comparator.reverseOrder())
                    .thenComparing(StudentHistoryItemDto::maMon, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(StudentHistoryItemDto::dangKyId, Comparator.reverseOrder());
            List<StudentHistoryItemDto> all = regs.stream().map(r -> toItem(r, grades.get(r.getId())))
                    .filter(i -> statusFilter == null || statusFilter.equals(i.trangThaiKetQua()))
                    .sorted(order).toList();
            ctx.changes(Map.of(), auditMap(purpose, hocKyId, all.size(), "sinhVienMa", sv.getMa()));
            return new StudentHistoryDto(toSummary(sv), purpose, slice(all, pr));
        });
    }

    @Transactional
    public LecturerHistoryDto lichSuGiangVien(Long id, Long hocKyId, String mucDich, int page, int size) {
        String purpose = purpose(mucDich);
        checkSemester(hocKyId);
        PageRequest pr = pageRequest(page, size, Sort.unsorted());
        return auditLogService.execute("ADMIN_VIEW_LECTURER_HISTORY", "LECTURER", id, ctx -> {
            Lecturer gv = lecturerRepository.findById(id)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy giảng viên"));
            List<CourseClass> classes = classRepository.findForAdminLecturerHistory(id, hocKyId);
            Map<Long, Long> counts = new HashMap<>();
            if (!classes.isEmpty()) {
                registrationRepository.countByLopIds(classes.stream().map(CourseClass::getId).toList(),
                        RegistrationStatus.DA_DANG_KY).forEach(r -> counts.put((Long) r[0], (Long) r[1]));
            }
            Comparator<LecturerClassItemDto> order = Comparator
                    .comparing(LecturerClassItemDto::ngayBatDauHocKy, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                    .thenComparing(LecturerClassItemDto::hocKyId, Comparator.reverseOrder())
                    .thenComparing(LecturerClassItemDto::maLop, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(LecturerClassItemDto::lopHocPhanId);
            List<LecturerClassItemDto> all = classes.stream().map(l -> toItem(l, counts.getOrDefault(l.getId(), 0L)))
                    .sorted(order).toList();
            ctx.changes(Map.of(), auditMap(purpose, hocKyId, all.size(), "giangVienMa", gv.getMa()));
            return new LecturerHistoryDto(toSummary(gv), purpose, slice(all, pr));
        });
    }

    // ---- helpers ----
    private static Map<String, Object> auditMap(String purpose, Long hocKyId, int count, String key, String ma) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mucDich", purpose);
        m.put("hocKyId", hocKyId);
        m.put("soBanGhi", count);
        m.put(key, ma);
        return m;
    }

    private StudentHistoryItemDto toItem(CourseRegistration r, Grade g) {
        CourseClass lop = r.getLopHocPhan();
        Subject mon = lop.getMonHoc();
        Semester ky = lop.getHocKy();
        boolean published = r.getTrangThai() == RegistrationStatus.DA_DANG_KY && lop.isBangDiemDaCongBo()
                && g != null && g.getDiemTongKet() != null;
        String ketQua = !published ? "CHUA_CO_DIEM"
                : g.getDiemTongKet() >= AcademicRules.DIEM_DAT ? "DAT" : "CHUA_DAT";
        return new StudentHistoryItemDto(r.getId(), ky.getId(), ky.getTen(), ky.getNamHoc(), ky.getNgayBatDau(),
                mon.getMa(), mon.getTen(), mon.getSoTinChi(), lop.getId(), lop.getMa(),
                lop.getGiangVien() != null ? lop.getGiangVien().getHoTen() : null,
                r.getThoiGianDangKy(), r.getTrangThai().name(), lop.isDaHuy(), lop.getLyDoHuy(),
                published ? g.getDiemQuaTrinh() : null, published ? g.getDiemCuoiKy() : null,
                published ? g.getDiemTongKet() : null, ketQua);
    }

    private LecturerClassItemDto toItem(CourseClass l, long active) {
        Semester ky = l.getHocKy();
        LocalDate today = LocalDate.now();
        String status = l.isDaHuy() ? "DA_HUY"
                : (ky.getNgayKetThuc() != null && ky.getNgayKetThuc().isBefore(today)) ? "DA_KET_THUC" : "DANG_MO";
        return new LecturerClassItemDto(l.getId(), l.getMa(), l.getMonHoc().getMa(), l.getMonHoc().getTen(),
                l.getMonHoc().getSoTinChi(), ky.getId(), ky.getTen(), ky.getNamHoc(), ky.getNgayBatDau(),
                l.getThu(), l.getTietBatDau(), l.getSoTiet(), l.getPhong(), l.getSiSoToiDa(), active, status,
                l.isDaHuy(), l.getThoiGianHuy(), l.getLyDoHuy());
    }

    private StudentSummaryDto toSummary(Student s) {
        return new StudentSummaryDto(s.getId(), s.getMa(), s.getHoTen(), s.getEmail(),
                s.getKhoa() != null ? s.getKhoa().getTen() : null,
                s.getNganh() != null ? s.getNganh().getTen() : null,
                s.getLopHanhChinh() != null ? s.getLopHanhChinh().getTen() : null,
                s.getUser() == null || s.getUser().isTrangThai());
    }

    private LecturerSummaryDto toSummary(Lecturer g) {
        return new LecturerSummaryDto(g.getId(), g.getMa(), g.getHoTen(), g.getEmail(),
                g.getKhoa() != null ? g.getKhoa().getTen() : null,
                g.getUser() == null || g.getUser().isTrangThai());
    }

    private static <T> PageDto<T> toPage(Page<T> p) {
        return new PageDto<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    private static <T> PageDto<T> slice(List<T> all, PageRequest pr) {
        int from = (int) Math.min((long) pr.getPageNumber() * pr.getPageSize(), all.size());
        int to = Math.min(from + pr.getPageSize(), all.size());
        int pages = (int) Math.ceil(all.size() / (double) pr.getPageSize());
        return new PageDto<>(all.subList(from, to), pr.getPageNumber(), pr.getPageSize(), all.size(), pages);
    }

    private static PageRequest pageRequest(int page, int size, Sort sort) {
        if (page < 0) throw ApiException.badRequest("Số trang không hợp lệ");
        if (size < 1 || size > MAX_SIZE) throw ApiException.badRequest("Kích thước trang phải từ 1 đến " + MAX_SIZE);
        return PageRequest.of(page, size, sort);
    }

    private static String kw(String keyword) {
        if (keyword == null || keyword.isBlank()) return "";
        String k = keyword.trim().toLowerCase(Locale.ROOT);
        if (k.length() > 100) throw ApiException.badRequest("Từ khóa tối đa 100 ký tự");
        return k.replace("%", "").replace("_", "");
    }

    private static String purpose(String mucDich) {
        String p = mucDich == null ? "" : mucDich.trim();
        if (p.length() < 5 || p.length() > 200) {
            throw ApiException.badRequest("Vui lòng nhập mục đích nghiệp vụ (5-200 ký tự) khi xem dữ liệu chi tiết");
        }
        return p;
    }

    private void checkSemester(Long hocKyId) {
        if (hocKyId == null) return;
        if (hocKyId <= 0) throw ApiException.badRequest("Học kỳ không hợp lệ");
        if (!semesterRepository.existsById(hocKyId)) throw ApiException.notFound("Học kỳ không tồn tại");
    }

    private static String statusFilter(String v) {
        if (v == null || v.isBlank() || "TAT_CA".equalsIgnoreCase(v.trim())) return null;
        String s = v.trim().toUpperCase(Locale.ROOT);
        if (!KETQUA.contains(s)) throw ApiException.badRequest("Trạng thái kết quả không hợp lệ");
        return s;
    }
}
