package com.edu.registration.service;

import com.edu.registration.config.ClassScheduleConflictProperties;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.ClassTransferHistory;
import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.SubjectRelationType;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.room.ClassFacts;
import com.edu.registration.room.PolicyMode;
import com.edu.registration.room.RoomAssignmentChecker;
import com.edu.registration.room.RoomCheckResult;
import com.edu.registration.room.RoomFacts;
import com.edu.registration.room.RoomIssue;
import com.edu.registration.room.RoomPolicy;
import com.edu.registration.repository.ClassAbsenceNoticeRepository;
import com.edu.registration.repository.ClassTransferHistoryRepository;
import com.edu.registration.repository.ClassroomRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.PrerequisiteSubjectRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final GradeRepository diemRepository;
    private final PrerequisiteSubjectRepository monTienQuyetRepository;
    private final StudentRepository sinhVienRepository;
    private final ClassTransferHistoryRepository classTransferHistoryRepository;
    private final ClassroomRepository classroomRepository;

    @Value("${app.room-policy.hoat-dong:BAT_BUOC}")
    private PolicyMode policyHoatDong = PolicyMode.BAT_BUOC;

    @Value("${app.room-policy.suc-chua:CANH_BAO}")
    private PolicyMode policySucChua = PolicyMode.CANH_BAO;

    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService) {
        this(lopHocPhanRepository, monHocRepository, giangVienRepository, dangKyHocPhanRepository,
                null, thongBaoNghiRepository, hocKyService, auditLogService, currentUser, mapper, thongBaoService, null,
                null, null, null, null, null);
    }

    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             RegistrationHistoryRepository lichSuDangKyRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService) {
        this(lopHocPhanRepository, monHocRepository, giangVienRepository, dangKyHocPhanRepository,
                lichSuDangKyRepository, thongBaoNghiRepository, hocKyService, auditLogService, currentUser, mapper, thongBaoService, null,
                null, null, null, null, null);
    }

    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             RegistrationHistoryRepository lichSuDangKyRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService,
                             ClassScheduleConflictService conflictService) {
        this(lopHocPhanRepository, monHocRepository, giangVienRepository, dangKyHocPhanRepository,
                lichSuDangKyRepository, thongBaoNghiRepository, hocKyService, auditLogService, currentUser, mapper, thongBaoService, conflictService,
                null, null, null, null, null);
    }

    @Autowired
    public CourseClassService(CourseClassRepository lopHocPhanRepository, SubjectRepository monHocRepository,
                             LecturerRepository giangVienRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             RegistrationHistoryRepository lichSuDangKyRepository,
                             ClassAbsenceNoticeRepository thongBaoNghiRepository,
                             SemesterService hocKyService, AuditLogService auditLogService,
                             CurrentUser currentUser, DtoMapper mapper, NotificationService thongBaoService,
                             ClassScheduleConflictService conflictService,
                             GradeRepository diemRepository,
                             PrerequisiteSubjectRepository monTienQuyetRepository,
                             StudentRepository sinhVienRepository,
                             ClassTransferHistoryRepository classTransferHistoryRepository,
                              @Autowired(required = false) ClassroomRepository classroomRepository) {
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
        this.diemRepository = diemRepository;
        this.monTienQuyetRepository = monTienQuyetRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.classTransferHistoryRepository = classTransferHistoryRepository;
        this.classroomRepository = classroomRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSach(Long hocKyId, String keyword) {
        return danhSach(hocKyId, keyword, null, null);
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSach(Long hocKyId, String keyword, Long giangVienId, GradeSheetStatus trangThaiBangDiem) {
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);
        List<CourseClass> danhSach = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId());
        if (giangVienId != null) {
            danhSach = danhSach.stream()
                    .filter(lop -> lop.getGiangVien() != null && giangVienId.equals(lop.getGiangVien().getId()))
                    .toList();
        }
        if (trangThaiBangDiem != null) {
            danhSach = danhSach.stream()
                    .filter(lop -> lop.getTrangThaiBangDiemHieuLuc() == trangThaiBangDiem)
                    .toList();
        }
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
            List<String> canhBao = new ArrayList<>(ketQuaXungDot.moTaCanhBao());
            canhBao.addAll(kiemTraPhong(request));

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
                    canhBao,
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
            long siSoDaDangKy = dangKyHocPhanRepository.countByLopHocPhanIdAndTrangThai(
                    id,
                    RegistrationStatus.DA_DANG_KY
            );
            if (request.siSoToiDa() < siSoDaDangKy) {
                throw ApiException.badRequest("Sĩ số tối đa không được nhỏ hơn số sinh viên đã đăng ký ("
                        + siSoDaDangKy + ")");
            }
            Subject monHoc = monHocRepository.findById(request.monHocId())
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            Lecturer giangVien = giangVienRepository.findById(request.giangVienId())
                    .orElseThrow(() -> ApiException.notFound("Giảng viên không tồn tại"));
            Semester hocKy = hocKyService.lay(request.hocKyId());

            CourseClassDtos.KetQuaKiemTraXungDotDto ketQuaXungDot =
                    conflictService.kiemTraVaApDungKhiLuu(request, id);
            List<String> canhBao = new ArrayList<>(ketQuaXungDot.moTaCanhBao());
            canhBao.addAll(kiemTraPhong(request));

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
                    canhBao,
                    ketQuaXungDot.danhSachXungDot(),
                    ketQuaXungDot.coXungDot(),
                    ketQuaXungDot.chinhSachEnforcement(),
                    ketQuaXungDot.chinhSachDaChot(),
                    ketQuaXungDot.ghiChuChinhSach()
            );
        });
    }

    /**
     * Khảo sát tác động trước khi hủy lớp học phần (QLSV-79 - TASK-AD-04).
     * Báo cáo chi tiết số sinh viên đang học, tình trạng điểm và danh sách các lớp đích khả dĩ (cùng môn hoặc tương đương).
     */
    @Transactional(readOnly = true)
    public CourseClassDtos.KhaoSatHuyLopDto khaoSatHuyLop(Long id) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        List<CourseRegistration> activeRegistrations =
                dangKyHocPhanRepository.findByLopHocPhan(id, RegistrationStatus.DA_DANG_KY);

        Map<Long, Grade> diemMap = new HashMap<>();
        if (diemRepository != null && !activeRegistrations.isEmpty()) {
            List<Long> regIds = activeRegistrations.stream().map(CourseRegistration::getId).toList();
            diemRepository.findByDangKyIdIn(regIds).forEach(g -> diemMap.put(g.getDangKy().getId(), g));
        }

        int soSinhVienCoDiem = 0;
        List<CourseClassDtos.SinhVienLopDto> danhSachSinhVien = new ArrayList<>();
        for (CourseRegistration reg : activeRegistrations) {
            Student sv = reg.getSinhVien();
            Grade grade = diemMap.get(reg.getId());
            boolean coDiemSv = grade != null && (grade.getDiemQuaTrinh() != null || grade.getDiemCuoiKy() != null || grade.getDiemTongKet() != null);
            if (coDiemSv) {
                soSinhVienCoDiem++;
            }
            danhSachSinhVien.add(new CourseClassDtos.SinhVienLopDto(
                    sv.getId(),
                    sv.getMa(),
                    sv.getHoTen(),
                    sv.getLopHanhChinh() != null ? sv.getLopHanhChinh().getTen() : null,
                    coDiemSv,
                    grade != null ? grade.getDiemQuaTrinh() : null,
                    grade != null ? grade.getDiemCuoiKy() : null,
                    grade != null ? grade.getDiemTongKet() : null,
                    reg.getThoiGianDangKy()
            ));
        }

        boolean bangDiemKhongPhaiNhap = lop.getTrangThaiBangDiem() != null && lop.getTrangThaiBangDiem() != GradeSheetStatus.NHAP;
        boolean coDiem = soSinhVienCoDiem > 0 || bangDiemKhongPhaiNhap;

        boolean choPhepPhaoHuy = !lop.isDaHuy() && !coDiem;
        String lyDoKhongChoPhep = null;
        if (lop.isDaHuy()) {
            lyDoKhongChoPhep = "Lớp học phần này đã bị hủy trước đó.";
        } else if (coDiem) {
            lyDoKhongChoPhep = "Lớp học phần đã có điểm đã nhập hoặc bảng điểm ở trạng thái "
                    + lop.getTrangThaiBangDiemHieuLuc()
                    + ". Hệ thống dừng thao tác hủy lớp để bảo vệ toàn vẹn dữ liệu điểm. Quyết định cần chốt: (1) Quy chế xử lý điểm quá trình/kết thúc khi lớp bị hủy giữa chừng; (2) Thẩm quyền của Phòng Đào tạo trong việc hủy bỏ hoặc bảo lưu bảng điểm đã nhập.";
        }

        List<CourseClassDtos.LopDichKhaDiDto> lopDichKhaDi = timLopDichKhaDi(lop);

        return new CourseClassDtos.KhaoSatHuyLopDto(
                lop.getId(),
                lop.getMa(),
                lop.getMonHoc().getId(),
                lop.getMonHoc().getMa(),
                lop.getMonHoc().getTen(),
                lop.getMonHoc().getSoTinChi(),
                lop.getGiangVien().getId(),
                lop.getGiangVien().getHoTen(),
                lop.getHocKy().getId(),
                lop.getHocKy().getTen(),
                lop.getSiSoHienTai(),
                lop.getSiSoToiDa(),
                lop.isDaHuy(),
                coDiem,
                soSinhVienCoDiem,
                lop.getTrangThaiBangDiemHieuLuc().name(),
                choPhepPhaoHuy,
                lyDoKhongChoPhep,
                danhSachSinhVien,
                lopDichKhaDi
        );
    }

    /**
     * Hủy lớp học phần an toàn theo quy trình quản trị (QLSV-79 - TASK-AD-04).
     * - Khóa bi quan hàng lớp học phần ngăn tranh chấp đồng thời.
     * - Chặn hủy nếu lớp đã có điểm đã nhập để bảo vệ toàn vẹn dữ liệu điểm.
     * - Đánh dấu daHuy = true; lớp bị hủy không nhận bất kỳ đăng ký mới nào.
     * - Chuyển toàn bộ đăng ký đang hiệu lực sang DA_HUY; không xóa đăng ký/lịch sử/điểm.
     * - Ghi audit log chi tiết và gửi thông báo tới sinh viên, giảng viên lớp bị hủy.
     */
    public CourseClassDtos.HuyLopHocPhanResponse huyLopHocPhan(Long id, String lyDo) {
        if (lyDo != null && lyDo.trim().length() > 500) {
            throw ApiException.badRequest("Lý do hủy lớp học phần tối đa 500 ký tự");
        }
        String normalizedReason = (lyDo == null || lyDo.isBlank()) ? null : lyDo.trim();
        String historyReason = normalizedReason == null
                ? NotificationService.SECTION_CANCELLED_REASON_PREFIX
                : NotificationService.SECTION_CANCELLED_REASON_PREFIX + ": " + normalizedReason;

        return auditLogService.execute("COURSE_SECTION_CANCELLED", "COURSE_SECTION", id, audit -> {
            CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

            if (lop.isDaHuy()) {
                return new CourseClassDtos.HuyLopHocPhanResponse(
                        lop.getId(),
                        lop.getMa(),
                        0,
                        lop.getLyDoHuy() != null ? lop.getLyDoHuy() : normalizedReason
                );
            }

            Map<String, Object> before = auditSnapshot(lop);
            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(id, RegistrationStatus.DA_DANG_KY);

            // Kiểm tra chính sách điểm: nếu lớp đã có điểm hoặc bảng điểm không phải NHAP -> dừng phần không an toàn
            boolean bangDiemKhongPhaiNhap = lop.getTrangThaiBangDiem() != null && lop.getTrangThaiBangDiem() != GradeSheetStatus.NHAP;
            boolean coDiemDaNhap = false;
            if (diemRepository != null && !activeRegistrations.isEmpty()) {
                List<Long> regIds = activeRegistrations.stream().map(CourseRegistration::getId).toList();
                List<Grade> grades = diemRepository.findByDangKyIdIn(regIds);
                coDiemDaNhap = grades.stream().anyMatch(g -> g.getDiemQuaTrinh() != null || g.getDiemCuoiKy() != null || g.getDiemTongKet() != null);
            }

            if (bangDiemKhongPhaiNhap || coDiemDaNhap) {
                throw ApiException.conflict("Lớp học phần " + lop.getMa() + " đã có điểm đã nhập (hoặc bảng điểm ở trạng thái "
                        + lop.getTrangThaiBangDiemHieuLuc()
                        + "). Do chưa có quy chế chính thức về việc hủy lớp khi đã nhập điểm, hệ thống dừng thao tác để bảo vệ toàn vẹn dữ liệu điểm sinh viên. Quyết định cần chốt: (1) Quy chế xử lý điểm quá trình/kết thúc khi lớp bị hủy giữa chừng; (2) Thẩm quyền của Phòng Đào tạo trong việc hủy bỏ hoặc bảo lưu bảng điểm đã nhập.");
            }

            LocalDateTime now = LocalDateTime.now();
            User actor = currentUser.get();

            // Cập nhật trạng thái lớp đã hủy
            lop.setDaHuy(true);
            lop.setThoiGianHuy(now);
            lop.setLyDoHuy(normalizedReason);
            if (actor != null) {
                lop.setNguoiHuyId(actor.getId());
                lop.setNguoiHuyUsername(actor.getUsername());
            }

            // Chuyển toàn bộ đăng ký sang DA_HUY; không xóa dòng dữ liệu
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
            after.put("daHuy", true);
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

    /**
     * Kiểm tra điều kiện trước khi chuyển sinh viên sang lớp đích (QLSV-79 - TASK-AD-04).
     * Kiểm tra: lớp đích tồn tại, cùng học kỳ, chưa bị hủy, cùng môn hoặc môn tương đương,
     * còn chỗ, không trùng lịch, đủ môn tiên quyết và không vi phạm quy tắc chung.
     */
    @Transactional(readOnly = true)
    public CourseClassDtos.KetQuaKiemTraChuyenLopDto kiemTraChuyenLop(Long lopNguonId, Long lopDichId, List<Long> sinhVienIds) {
        CourseClass lopNguon = lopHocPhanRepository.findWithDetailsById(lopNguonId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần nguồn không tồn tại"));
        CourseClass lopDich = lopHocPhanRepository.findWithDetailsById(lopDichId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần đích không tồn tại"));

        if (lopNguon.getId().equals(lopDich.getId())) {
            throw ApiException.badRequest("Lớp nguồn và lớp đích không được trùng nhau");
        }
        if (lopDich.isDaHuy()) {
            throw ApiException.badRequest("Lớp đích " + lopDich.getMa() + " đã bị hủy, không thể chuyển sinh viên đến");
        }
        if (!lopNguon.getHocKy().getId().equals(lopDich.getHocKy().getId())) {
            throw ApiException.badRequest("Lớp nguồn và lớp đích phải thuộc cùng học kỳ " + lopNguon.getHocKy().getTen());
        }

        String loaiQuanHeMon = xacDinhLoaiQuanHeMon(lopNguon, lopDich);
        if (loaiQuanHeMon == null) {
            throw ApiException.badRequest("Lớp đích (" + lopDich.getMonHoc().getMa() + ") không cùng môn và không phải môn tương đương với lớp nguồn (" + lopNguon.getMonHoc().getMa() + ")");
        }

        List<CourseRegistration> activeRegistrations =
                dangKyHocPhanRepository.findByLopHocPhan(lopNguonId, RegistrationStatus.DA_DANG_KY);

        Set<Long> targetSinhVienIds = (sinhVienIds != null && !sinhVienIds.isEmpty())
                ? new HashSet<>(sinhVienIds)
                : activeRegistrations.stream().map(r -> r.getSinhVien().getId()).collect(Collectors.toSet());

        List<CourseClassDtos.KiemTraChuyenSinhVienItemDto> chiTietList = new ArrayList<>();
        int soDuDieuKien = 0;

        for (CourseRegistration reg : activeRegistrations) {
            Student sv = reg.getSinhVien();
            if (!targetSinhVienIds.contains(sv.getId())) {
                continue;
            }

            String error = null;
            boolean trungLich = false;
            String moTaTrungLich = null;
            boolean duTienQuyet = true;
            String moTaTienQuyet = null;
            boolean coDiemTaiLopNguon = false;
            String moTaDiem = null;

            // 1. Kiểm tra điểm ở lớp nguồn
            if (diemRepository != null) {
                Grade grade = diemRepository.findByDangKyId(reg.getId()).orElse(null);
                if (grade != null && (grade.getDiemQuaTrinh() != null || grade.getDiemCuoiKy() != null || grade.getDiemTongKet() != null)) {
                    coDiemTaiLopNguon = true;
                    moTaDiem = "Sinh viên đã có điểm tại lớp nguồn";
                    error = "Sinh viên đã có điểm tại lớp nguồn. Chưa có quy chế chuyển điểm.";
                }
            }
            if (lopNguon.getTrangThaiBangDiem() != null && lopNguon.getTrangThaiBangDiem() != GradeSheetStatus.NHAP) {
                coDiemTaiLopNguon = true;
                moTaDiem = "Bảng điểm lớp nguồn ở trạng thái " + lopNguon.getTrangThaiBangDiemHieuLuc();
                error = "Bảng điểm lớp nguồn đang ở trạng thái " + lopNguon.getTrangThaiBangDiemHieuLuc() + ".";
            }

            // 2. Kiểm tra trùng lịch
            if (error == null) {
                List<CourseRegistration> dangKyCungKy =
                        dangKyHocPhanRepository.findTheoHocKy(sv.getId(), lopDich.getHocKy().getId(), RegistrationStatus.DA_DANG_KY);
                for (CourseRegistration dk : dangKyCungKy) {
                    if (!dk.getId().equals(reg.getId()) && dk.getLopHocPhan().trungLichVoi(lopDich)) {
                        trungLich = true;
                        CourseClass cl = dk.getLopHocPhan();
                        moTaTrungLich = "Trùng lịch với lớp " + cl.getMa() + " (" + cl.getMonHoc().getTen()
                                + ", thứ " + cl.getThu() + ", tiết " + cl.getTietBatDau() + "-" + cl.getTietKetThuc() + ")";
                        error = moTaTrungLich;
                        break;
                    }
                }
            }

            // 3. Kiểm tra môn tiên quyết
            if (error == null && monTienQuyetRepository != null && diemRepository != null) {
                try {
                    kiemTraTienQuyet(sv, lopDich.getMonHoc(), lopDich.getHocKy());
                } catch (Exception ex) {
                    duTienQuyet = false;
                    moTaTienQuyet = ex.getMessage();
                    error = ex.getMessage();
                }
            }

            // 4. Kiểm tra môn học đã đăng ký trong kỳ (nếu không cùng môn và sinh viên đã có đăng ký môn đích)
            if (error == null && !lopNguon.getMonHoc().getId().equals(lopDich.getMonHoc().getId())) {
                List<CourseRegistration> dangKyCungKy =
                        dangKyHocPhanRepository.findTheoHocKy(sv.getId(), lopDich.getHocKy().getId(), RegistrationStatus.DA_DANG_KY);
                for (CourseRegistration dk : dangKyCungKy) {
                    if (!dk.getId().equals(reg.getId()) && dk.getLopHocPhan().getMonHoc().getId().equals(lopDich.getMonHoc().getId())) {
                        error = "Sinh viên đã đăng ký môn " + lopDich.getMonHoc().getTen() + " ở lớp " + dk.getLopHocPhan().getMa();
                        break;
                    }
                }
            }

            boolean duDieuKien = (error == null);
            if (duDieuKien) {
                soDuDieuKien++;
            }

            chiTietList.add(new CourseClassDtos.KiemTraChuyenSinhVienItemDto(
                    sv.getId(),
                    sv.getMa(),
                    sv.getHoTen(),
                    duDieuKien,
                    trungLich,
                    moTaTrungLich,
                    duTienQuyet,
                    moTaTienQuyet,
                    coDiemTaiLopNguon,
                    moTaDiem,
                    error
            ));
        }

        int soChoConLai = lopDich.getConLai();
        boolean choPhepChuyenTatCa = soDuDieuKien > 0 && soDuDieuKien <= soChoConLai && soDuDieuKien == chiTietList.size();

        return new CourseClassDtos.KetQuaKiemTraChuyenLopDto(
                lopNguon.getId(),
                lopNguon.getMa(),
                lopDich.getId(),
                lopDich.getMa(),
                loaiQuanHeMon,
                soChoConLai,
                chiTietList.size(),
                soDuDieuKien,
                choPhepChuyenTatCa,
                chiTietList
        );
    }

    /**
     * Chuyển 1 sinh viên sang lớp khác (QLSV-79 - TASK-AD-04).
     * Thực hiện trong giao dịch READ_COMMITTED và khóa bi quan hàng theo thứ tự ID ổn định ngăn chặn deadlock và vượt sĩ số.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseClassDtos.ChuyenSinhVienResponse chuyenSinhVien(Long lopNguonId, Long lopDichId, Long sinhVienId, String lyDo) {
        if (lopNguonId == null || lopDichId == null || sinhVienId == null) {
            throw ApiException.badRequest("Thông tin lớp nguồn, lớp đích và sinh viên không được để trống");
        }
        if (lopNguonId.equals(lopDichId)) {
            throw ApiException.badRequest("Lớp nguồn và lớp đích không được trùng nhau");
        }
        if (lyDo != null && lyDo.trim().length() > 500) {
            throw ApiException.badRequest("Lý do chuyển lớp tối đa 500 ký tự");
        }
        String normalizedReason = (lyDo == null || lyDo.isBlank()) ? null : lyDo.trim();

        // Khóa bi quan 2 lớp theo thứ tự ID tăng dần
        Long firstId = Math.min(lopNguonId, lopDichId);
        Long secondId = Math.max(lopNguonId, lopDichId);

        CourseClass first = lopHocPhanRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần id=" + firstId));
        CourseClass second = lopHocPhanRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần id=" + secondId));

        CourseClass lopNguon = lopNguonId.equals(first.getId()) ? first : second;
        CourseClass lopDich = lopDichId.equals(first.getId()) ? first : second;

        return chuyenMotSinhVienTrongGiaoDich(lopNguon, lopDich, sinhVienId, normalizedReason);
    }

    /**
     * Chuyển danh sách sinh viên sang lớp khác theo đợt (QLSV-79 - TASK-AD-04).
     * Toàn bộ thao tác chuyển chạy trong một transaction nguyên tử; nếu thêm vào lớp đích thất bại thì trạng thái lớp nguồn được giữ nguyên vẹn.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CourseClassDtos.KetQuaChuyenHangLoatDto chuyenSinhVienHangLoat(
            Long lopNguonId, Long lopDichId, List<Long> sinhVienIds, String lyDo) {

        if (lopNguonId == null || lopDichId == null || sinhVienIds == null || sinhVienIds.isEmpty()) {
            throw ApiException.badRequest("Thông tin lớp và danh sách sinh viên không được để trống");
        }
        if (lopNguonId.equals(lopDichId)) {
            throw ApiException.badRequest("Lớp nguồn và lớp đích không được trùng nhau");
        }
        if (lyDo != null && lyDo.trim().length() > 500) {
            throw ApiException.badRequest("Lý do chuyển lớp tối đa 500 ký tự");
        }
        String normalizedReason = (lyDo == null || lyDo.isBlank()) ? null : lyDo.trim();

        Long firstId = Math.min(lopNguonId, lopDichId);
        Long secondId = Math.max(lopNguonId, lopDichId);
        CourseClass first = lopHocPhanRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần id=" + firstId));
        CourseClass second = lopHocPhanRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lớp học phần id=" + secondId));

        CourseClass lopNguon = lopNguonId.equals(first.getId()) ? first : second;
        CourseClass lopDich = lopDichId.equals(first.getId()) ? first : second;

        int soChoConLai = lopDich.getConLai();
        if (sinhVienIds.size() > soChoConLai) {
            throw ApiException.conflict("Lớp đích " + lopDich.getMa() + " chỉ còn " + soChoConLai
                    + " chỗ trống, không đủ tiếp nhận " + sinhVienIds.size() + " sinh viên");
        }

        List<CourseClassDtos.ChuyenSinhVienResponse> thanhCong = new ArrayList<>();
        for (Long svId : sinhVienIds) {
            CourseClassDtos.ChuyenSinhVienResponse res =
                    chuyenMotSinhVienTrongGiaoDich(lopNguon, lopDich, svId, normalizedReason);
            thanhCong.add(res);
        }

        return new CourseClassDtos.KetQuaChuyenHangLoatDto(
                lopNguon.getId(),
                lopNguon.getMa(),
                lopDich.getId(),
                lopDich.getMa(),
                sinhVienIds.size(),
                thanhCong.size(),
                0,
                thanhCong,
                List.of()
        );
    }

    private CourseClassDtos.ChuyenSinhVienResponse chuyenMotSinhVienTrongGiaoDich(
            CourseClass lopNguon, CourseClass lopDich, Long sinhVienId, String normalizedReason) {

        return auditLogService.execute("CLASS_STUDENT_TRANSFERRED", "COURSE_SECTION", lopDich.getId(), audit -> {
            if (lopDich.isDaHuy()) {
                throw ApiException.badRequest("Lớp đích " + lopDich.getMa() + " đã bị hủy, không thể tiếp nhận sinh viên");
            }
            if (!lopNguon.getHocKy().getId().equals(lopDich.getHocKy().getId())) {
                throw ApiException.badRequest("Lớp nguồn và lớp đích phải thuộc cùng học kỳ " + lopNguon.getHocKy().getTen());
            }

            String loaiQuanHeMon = xacDinhLoaiQuanHeMon(lopNguon, lopDich);
            if (loaiQuanHeMon == null) {
                throw ApiException.badRequest("Lớp đích (" + lopDich.getMonHoc().getMa() + ") không cùng môn và không phải môn tương đương với lớp nguồn (" + lopNguon.getMonHoc().getMa() + ")");
            }

            Student sv = (sinhVienRepository != null)
                    ? sinhVienRepository.findByIdForUpdate(sinhVienId).orElseThrow(() -> ApiException.notFound("Không tìm thấy sinh viên"))
                    : null;
            if (sv == null) {
                throw ApiException.notFound("Không tìm thấy sinh viên");
            }
            if (sv.getUser() != null && !sv.getUser().isTrangThai()) {
                throw ApiException.badRequest("Tài khoản sinh viên đang bị khóa");
            }

            CourseRegistration dangKyNguon = dangKyHocPhanRepository
                    .findBySinhVienIdAndLopHocPhanId(sinhVienId, lopNguon.getId())
                    .orElseThrow(() -> ApiException.badRequest("Sinh viên " + sv.getMa() + " không có đăng ký tại lớp nguồn " + lopNguon.getMa()));

            if (dangKyNguon.getTrangThai() != RegistrationStatus.DA_DANG_KY) {
                throw ApiException.badRequest("Đăng ký của sinh viên " + sv.getMa() + " tại lớp nguồn không ở trạng thái hiệu lực (DA_DANG_KY)");
            }

            // Kiểm tra điểm ở lớp nguồn
            if (diemRepository != null) {
                Grade grade = diemRepository.findByDangKyId(dangKyNguon.getId()).orElse(null);
                if (grade != null && (grade.getDiemQuaTrinh() != null || grade.getDiemCuoiKy() != null || grade.getDiemTongKet() != null)) {
                    throw ApiException.conflict("Sinh viên " + sv.getMa() + " đã có điểm tại lớp nguồn " + lopNguon.getMa()
                            + ". Do chưa có quy chế chính thức về việc chuyển điểm khi chuyển lớp, hệ thống dừng thao tác để bảo vệ toàn vẹn dữ liệu điểm. Quyết định cần chốt: (1) Điểm quá trình ở lớp nguồn có được bảo lưu sang lớp đích hay không; (2) Cách tính lại điểm chuyên cần và thành phần khi chuyển sang giảng viên mới.");
                }
            }
            if (lopNguon.getTrangThaiBangDiem() != null && lopNguon.getTrangThaiBangDiem() != GradeSheetStatus.NHAP) {
                throw ApiException.conflict("Bảng điểm lớp nguồn đang ở trạng thái " + lopNguon.getTrangThaiBangDiemHieuLuc()
                        + ". Do chưa có quy chế chính thức về việc chuyển sinh viên khi bảng điểm đã nộp/duyệt/công bố, hệ thống dừng thao tác. Quyết định cần chốt: Thẩm quyền của Phòng Đào tạo trong việc điều chỉnh danh sách sinh viên sau khi bảng điểm đã khóa/công bố.");
            }

            // Kiểm tra sức chứa lớp đích
            if (lopDich.getSiSoHienTai() >= lopDich.getSiSoToiDa()) {
                throw ApiException.conflict("Lớp đích " + lopDich.getMa() + " đã đủ sĩ số (" + lopDich.getSiSoToiDa() + ")");
            }

            // Kiểm tra trùng lịch
            List<CourseRegistration> dangKyCungKy =
                    dangKyHocPhanRepository.findTheoHocKy(sv.getId(), lopDich.getHocKy().getId(), RegistrationStatus.DA_DANG_KY);
            for (CourseRegistration dk : dangKyCungKy) {
                if (!dk.getId().equals(dangKyNguon.getId()) && dk.getLopHocPhan().trungLichVoi(lopDich)) {
                    CourseClass cl = dk.getLopHocPhan();
                    throw ApiException.conflict("Sinh viên bị trùng lịch với lớp " + cl.getMa() + " (" + cl.getMonHoc().getTen()
                            + ", thứ " + cl.getThu() + ", tiết " + cl.getTietBatDau() + "-" + cl.getTietKetThuc() + ")");
                }
            }

            // Kiểm tra môn tiên quyết
            if (monTienQuyetRepository != null && diemRepository != null) {
                kiemTraTienQuyet(sv, lopDich.getMonHoc(), lopDich.getHocKy());
            }

            // Kiểm tra trùng môn
            if (!lopNguon.getMonHoc().getId().equals(lopDich.getMonHoc().getId())) {
                for (CourseRegistration dk : dangKyCungKy) {
                    if (!dk.getId().equals(dangKyNguon.getId()) && dk.getLopHocPhan().getMonHoc().getId().equals(lopDich.getMonHoc().getId())) {
                        throw ApiException.conflict("Sinh viên đã đăng ký môn " + lopDich.getMonHoc().getTen() + " ở lớp " + dk.getLopHocPhan().getMa());
                    }
                }
            }

            // Kiểm tra trần tín chỉ
            int chenhLechTinChi = lopDich.getMonHoc().getSoTinChi() - lopNguon.getMonHoc().getSoTinChi();
            if (chenhLechTinChi > 0) {
                int tinChiHienTai = dangKyCungKy.stream().mapToInt(dk -> dk.getLopHocPhan().getMonHoc().getSoTinChi()).sum();
                if (tinChiHienTai + chenhLechTinChi > lopDich.getHocKy().getSoTinChiToiDa()) {
                    throw ApiException.badRequest("Vượt quá số tín chỉ tối đa (" + lopDich.getHocKy().getSoTinChiToiDa() + " TC) của học kỳ");
                }
            }

            LocalDateTime now = LocalDateTime.now();
            User actor = currentUser.get();

            // 1. Chuyển đăng ký lớp nguồn thành DA_HUY, giảm sĩ số
            dangKyNguon.setTrangThai(RegistrationStatus.DA_HUY);
            dangKyHocPhanRepository.save(dangKyNguon);
            lopNguon.setSiSoHienTai(Math.max(0, lopNguon.getSiSoHienTai() - 1));
            lopHocPhanRepository.save(lopNguon);

            if (lichSuDangKyRepository != null) {
                RegistrationHistory eventNguon = new RegistrationHistory();
                eventNguon.setSinhVien(sv);
                eventNguon.setLopHocPhan(lopNguon);
                eventNguon.setHocKyId(lopNguon.getHocKy().getId());
                eventNguon.setDangKyId(dangKyNguon.getId());
                eventNguon.setTrangThai(RegistrationStatus.DA_HUY);
                eventNguon.setThoiGianDangKy(dangKyNguon.getThoiGianDangKy());
                eventNguon.setThoiDiem(now);
                eventNguon.setThoiGianHuy(now);
                eventNguon.setLyDoHuy("Chuyển sang lớp " + lopDich.getMa() + (normalizedReason != null ? ": " + normalizedReason : ""));
                lichSuDangKyRepository.save(eventNguon);
            }

            // 2. Tạo hoặc kích hoạt đăng ký ở lớp đích, tăng sĩ số
            CourseRegistration dangKyDich = dangKyHocPhanRepository
                    .findBySinhVienIdAndLopHocPhanId(sv.getId(), lopDich.getId())
                    .orElseGet(() -> {
                        CourseRegistration moi = new CourseRegistration();
                        moi.setSinhVien(sv);
                        moi.setLopHocPhan(lopDich);
                        return moi;
                    });
            dangKyDich.setTrangThai(RegistrationStatus.DA_DANG_KY);
            dangKyDich.setThoiGianDangKy(now);
            CourseRegistration daLuuDich = dangKyHocPhanRepository.save(dangKyDich);

            lopDich.setSiSoHienTai(lopDich.getSiSoHienTai() + 1);
            lopHocPhanRepository.save(lopDich);

            if (lichSuDangKyRepository != null) {
                RegistrationHistory eventDich = new RegistrationHistory();
                eventDich.setSinhVien(sv);
                eventDich.setLopHocPhan(lopDich);
                eventDich.setHocKyId(lopDich.getHocKy().getId());
                eventDich.setDangKyId(daLuuDich.getId());
                eventDich.setTrangThai(RegistrationStatus.DA_DANG_KY);
                eventDich.setThoiGianDangKy(now);
                eventDich.setThoiDiem(now);
                eventDich.setLyDoHuy(null);
                lichSuDangKyRepository.save(eventDich);
            }

            // 3. Lưu lịch sử chuyển lớp
            ClassTransferHistory history = new ClassTransferHistory(
                    lopNguon, lopDich, sv, actor, now, normalizedReason,
                    lopNguon.getMonHoc(), lopDich.getMonHoc(), loaiQuanHeMon,
                    "Chuyển từ lớp " + lopNguon.getMa() + " sang lớp " + lopDich.getMa()
            );
            ClassTransferHistory daLuuHistory = (classTransferHistoryRepository != null)
                    ? classTransferHistoryRepository.save(history)
                    : history;

            // 4. Ghi Audit Log
            audit.changes(
                    Map.of("lopNguonId", lopNguon.getId(), "maLopNguon", lopNguon.getMa(), "sinhVienId", sv.getId(), "siSoHienTai", lopNguon.getSiSoHienTai() + 1),
                    Map.of("lopDichId", lopDich.getId(), "maLopDich", lopDich.getMa(), "sinhVienId", sv.getId(), "siSoHienTai", lopDich.getSiSoHienTai(), "loaiQuanHeMon", loaiQuanHeMon, "lyDo", normalizedReason != null ? normalizedReason : "")
            );

            // 5. Gửi thông báo tới sinh viên, giảng viên lớp nguồn, giảng viên lớp đích
            thongBaoService.studentTransferred(lopNguon, lopDich, sv, now, normalizedReason);

            return new CourseClassDtos.ChuyenSinhVienResponse(
                    daLuuHistory.getId(),
                    lopNguon.getId(),
                    lopNguon.getMa(),
                    lopDich.getId(),
                    lopDich.getMa(),
                    sv.getId(),
                    sv.getMa(),
                    sv.getHoTen(),
                    now,
                    normalizedReason,
                    loaiQuanHeMon
            );
        });
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LichSuChuyenLopDto> danhSachLichSuChuyen(Long lopHocPhanId, Long hocKyId) {
        if (classTransferHistoryRepository == null) {
            return List.of();
        }
        List<ClassTransferHistory> list;
        if (lopHocPhanId != null) {
            list = classTransferHistoryRepository.findByLopId(lopHocPhanId);
        } else if (hocKyId != null) {
            list = classTransferHistoryRepository.findByHocKyId(hocKyId);
        } else {
            list = classTransferHistoryRepository.findByHocKyId(null);
        }
        return list.stream().map(mapper::lichSuChuyenLop).toList();
    }

    public List<CourseClassDtos.LopDichKhaDiDto> timLopDichKhaDi(CourseClass lopNguon) {
        Long hocKyId = lopNguon.getHocKy().getId();
        Long monNguonId = lopNguon.getMonHoc().getId();

        Set<Long> monTuongDuongIds = new HashSet<>();
        if (monTienQuyetRepository != null) {
            List<PrerequisiteSubject> qhTuongDuong = monTienQuyetRepository.findAllQuanHeTuongDuongByMonHocId(monNguonId);
            LocalDate now = LocalDate.now();
            for (PrerequisiteSubject qh : qhTuongDuong) {
                if (qh.laHieuLucCho(lopNguon.getHocKy(), null, now)) {
                    Long otherId = qh.getMonHoc().getId().equals(monNguonId)
                            ? qh.getMonTienQuyet().getId()
                            : qh.getMonHoc().getId();
                    monTuongDuongIds.add(otherId);
                }
            }
        }

        List<CourseClass> tatCaLop = lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKyId);
        List<CourseClassDtos.LopDichKhaDiDto> ketQua = new ArrayList<>();

        for (CourseClass lop : tatCaLop) {
            if (lop.getId().equals(lopNguon.getId()) || lop.isDaHuy()) {
                continue;
            }
            Long monId = lop.getMonHoc().getId();
            String loaiQuanHe = null;
            if (monId.equals(monNguonId)) {
                loaiQuanHe = "CUNG_MON";
            } else if (monTuongDuongIds.contains(monId)) {
                loaiQuanHe = "TUONG_DUONG";
            }

            if (loaiQuanHe != null) {
                ketQua.add(new CourseClassDtos.LopDichKhaDiDto(
                        lop.getId(),
                        lop.getMa(),
                        lop.getMonHoc().getId(),
                        lop.getMonHoc().getMa(),
                        lop.getMonHoc().getTen(),
                        lop.getMonHoc().getSoTinChi(),
                        lop.getGiangVien().getHoTen(),
                        lop.getPhong(),
                        lop.getThu(),
                        lop.getTietBatDau(),
                        lop.getSoTiet(),
                        lop.getSiSoHienTai(),
                        lop.getSiSoToiDa(),
                        lop.getConLai(),
                        loaiQuanHe
                ));
            }
        }
        return ketQua;
    }

    public String xacDinhLoaiQuanHeMon(CourseClass lopNguon, CourseClass lopDich) {
        Long monNguonId = lopNguon.getMonHoc().getId();
        Long monDichId = lopDich.getMonHoc().getId();
        if (monNguonId.equals(monDichId)) {
            return "CUNG_MON";
        }
        if (monTienQuyetRepository != null) {
            List<PrerequisiteSubject> qhList = monTienQuyetRepository.findQuanHeTuongDuong(monNguonId, monDichId);
            LocalDate now = LocalDate.now();
            for (PrerequisiteSubject qh : qhList) {
                if (qh.laHieuLucCho(lopNguon.getHocKy(), null, now)) {
                    return "TUONG_DUONG";
                }
            }
        }
        return null;
    }

    private void kiemTraTienQuyet(Student sinhVien, Subject monHoc, Semester hocKy) {
        List<PrerequisiteSubject> danhSachQuanHe =
                monTienQuyetRepository.findByMonHocIdOrderByIdAsc(monHoc.getId());
        if (danhSachQuanHe.isEmpty()) {
            return;
        }
        String khoaSinhVien = (sinhVien.getChuongTrinh() != null && sinhVien.getChuongTrinh().getKhoaNhapHoc() != null)
                ? sinhVien.getChuongTrinh().getKhoaNhapHoc().trim()
                : null;
        Map<Double, Set<Long>> monDaDatTheoNguong = new HashMap<>();

        for (PrerequisiteSubject quanHe : danhSachQuanHe) {
            if (!quanHe.laHieuLucCho(hocKy, khoaSinhVien, LocalDate.now())) {
                continue;
            }
            SubjectRelationType loai = quanHe.getLoaiQuanHeHieuLuc();
            Subject monLienQuan = quanHe.getMonTienQuyet();

            if (loai == SubjectRelationType.TIEN_QUYET) {
                if (!quanHe.isYeuCauDatHieuLuc()) {
                    continue;
                }
                double nguongDat = quanHe.getDieuKienDatHieuLuc();
                Set<Long> monDaDat = monDaDatTheoNguong.computeIfAbsent(
                        nguongDat,
                        threshold -> new HashSet<>(diemRepository.findMonDaDat(sinhVien.getId(), threshold)));
                if (!monDaDat.contains(monLienQuan.getId())) {
                    String chiTietNguong = Double.compare(nguongDat, AcademicRules.DIEM_DAT) != 0
                            ? " (yêu cầu điểm >= " + nguongDat + ")"
                            : "";
                    throw ApiException.badRequest("Chưa đạt môn tiên quyết: "
                            + monLienQuan.getMa() + " - " + monLienQuan.getTen() + chiTietNguong);
                }
            }
        }
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

    /**
     * Kiểm tra trạng thái và sức chứa bằng danh mục Classroom hiện có.
     * Xung đột lịch được xử lý tập trung bởi ClassScheduleConflictService.
     */
    private List<String> kiemTraPhong(CourseClassDtos.LopHocPhanRequest request) {
        if (classroomRepository == null) {
            return List.of();
        }

        String maPhong = request.phong().trim();
        Classroom phong = classroomRepository.findByMaForUpdate(maPhong)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phòng học: " + maPhong));

        RoomFacts roomFacts = new RoomFacts(
                phong.getTrangThai() == ClassroomStatus.HOAT_DONG,
                phong.getSucChua(),
                phong.getLoaiPhong(),
                Set.of()
        );
        ClassFacts classFacts = new ClassFacts(
                request.siSoToiDa(),
                null,
                Set.of(),
                request.thu(),
                request.tietBatDau(),
                request.soTiet()
        );
        RoomPolicy policy = new RoomPolicy(
                policyHoatDong,
                PolicyMode.TAT,
                policySucChua,
                PolicyMode.TAT,
                PolicyMode.TAT
        );

        RoomCheckResult result = RoomAssignmentChecker.kiemTra(
                roomFacts,
                classFacts,
                List.of(),
                policy
        );
        if (!result.hopLe()) {
            String message = String.join("; ", result.loi().stream().map(RoomIssue::message).toList());
            throw ApiException.badRequest(message);
        }
        return result.canhBao().stream().map(RoomIssue::message).toList();
    }

    private void apDung(CourseClass lop, CourseClassDtos.LopHocPhanRequest request) {
        String reqPhong = request.phong() != null ? request.phong().trim() : "";
        lop.setPhong(reqPhong);
        lop.setThu(request.thu());
        lop.setTietBatDau(request.tietBatDau());
        lop.setSoTiet(request.soTiet());
        lop.setSiSoToiDa(request.siSoToiDa());

        if (classroomRepository != null && !reqPhong.isBlank()) {
            classroomRepository.findByMaIgnoreCase(reqPhong).ifPresent(cr -> {
                if (cr.getTrangThai() == ClassroomStatus.BAO_TRI) {
                    throw ApiException.badRequest("Phòng học " + cr.getMa() + " đang bảo trì, không thể xếp lịch cho lớp học phần");
                }
                if (cr.getTrangThai() == ClassroomStatus.NGUNG_SU_DUNG) {
                    throw ApiException.badRequest("Phòng học " + cr.getMa() + " đã ngừng sử dụng, không thể xếp lịch cho lớp học phần");
                }
                lop.setPhongHoc(cr);
            });
        }
    }
}
