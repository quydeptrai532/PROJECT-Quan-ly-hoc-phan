package com.edu.registration.service;

import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.ValidationErrorDetail;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeSheetStateHistory;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.GradeSheetStateHistoryRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GradeService {

    private static final Logger log = LoggerFactory.getLogger(GradeService.class);
    private static final Set<String> TRANG_THAI_KET_QUA_HOP_LE = Set.of("DAT", "CHUA_DAT", "CHUA_CO_DIEM");

    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final SemesterRepository hocKyRepository;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;
    private final AuditLogService auditLogService;
    private final NotificationService thongBaoService;
    private final GradeSheetStateHistoryRepository lichSuTrangThaiRepository;

    public GradeService(CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                        CourseClassRepository lopHocPhanRepository, StudentRepository sinhVienRepository,
                        LecturerRepository giangVienRepository, CurrentUser currentUser, DtoMapper mapper) {
        this(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository, sinhVienRepository,
                giangVienRepository, null, currentUser, mapper, null, null, null);
    }

    public GradeService(CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                        CourseClassRepository lopHocPhanRepository, StudentRepository sinhVienRepository,
                        LecturerRepository giangVienRepository, SemesterRepository hocKyRepository,
                        CurrentUser currentUser, DtoMapper mapper) {
        this(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository, sinhVienRepository,
                giangVienRepository, hocKyRepository, currentUser, mapper, null, null, null);
    }

    public GradeService(CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                        CourseClassRepository lopHocPhanRepository, StudentRepository sinhVienRepository,
                        LecturerRepository giangVienRepository, SemesterRepository hocKyRepository,
                        CurrentUser currentUser, DtoMapper mapper,
                        AuditLogService auditLogService, NotificationService thongBaoService) {
        this(dangKyHocPhanRepository, diemRepository, lopHocPhanRepository, sinhVienRepository,
                giangVienRepository, hocKyRepository, currentUser, mapper, auditLogService, thongBaoService, null);
    }

    @Autowired
    public GradeService(CourseRegistrationRepository dangKyHocPhanRepository, GradeRepository diemRepository,
                        CourseClassRepository lopHocPhanRepository, StudentRepository sinhVienRepository,
                        LecturerRepository giangVienRepository, SemesterRepository hocKyRepository,
                        CurrentUser currentUser, DtoMapper mapper,
                        AuditLogService auditLogService, NotificationService thongBaoService,
                        GradeSheetStateHistoryRepository lichSuTrangThaiRepository) {
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.hocKyRepository = hocKyRepository;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.auditLogService = auditLogService;
        this.thongBaoService = thongBaoService;
        this.lichSuTrangThaiRepository = lichSuTrangThaiRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DiemDto> danhSachDiemLop(Long lopHocPhanId) {
        return danhSachDiemLop(lopHocPhanId, null);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.DiemDto> danhSachDiemLop(Long lopHocPhanId, String tuKhoa) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        kiemTraQuyenXemBangDiem(lop);
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            String kw = tuKhoa.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(dk -> {
                        Student sv = dk.getSinhVien();
                        return (sv.getMa() != null && sv.getMa().toLowerCase(Locale.ROOT).contains(kw))
                                || (sv.getHoTen() != null && sv.getHoTen().toLowerCase(Locale.ROOT).contains(kw))
                                || (sv.getEmail() != null && sv.getEmail().toLowerCase(Locale.ROOT).contains(kw));
                    })
                    .toList();
        }
        Map<Long, Grade> diemMap = layDiemTheoDangKy(danhSach);
        return danhSach.stream()
                .map(dk -> mapper.diem(dk, diemMap.get(dk.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegistrationDtos.TrangThaiBangDiemDto chiTietTrangThaiBangDiem(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        kiemTraQuyenXemBangDiem(lop);
        List<CourseRegistration> activeRegistrations =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
        int soDuDieuKien = demSinhVienDuDieuKien(activeRegistrations, diemMap);
        return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), soDuDieuKien);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.LichSuTrangThaiBangDiemDto> lichSuTrangThaiBangDiem(Long lopHocPhanId) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        kiemTraQuyenXemBangDiem(lop);
        return layLichSuChuyenTrangThai(lopHocPhanId);
    }

    public RegistrationDtos.DiemDto nhapDiem(Long dangKyId, RegistrationDtos.NhapDiemRequest request) {
        CourseRegistration dangKy = dangKyHocPhanRepository.findById(dangKyId)
                .orElseThrow(() -> ApiException.notFound("Đăng ký không tồn tại"));
        if (dangKy.getTrangThai() != RegistrationStatus.DA_DANG_KY) {
            throw ApiException.badRequest("Đăng ký này đã bị hủy, không thể nhập điểm");
        }
        CourseClass lop = dangKy.getLopHocPhan();
        if (lop != null && lop.getId() != null) {
            lop = lopHocPhanRepository.findByIdForUpdate(lop.getId()).orElse(lop);
        }
        kiemTraQuyenSuaDiem(lop);
        kiemTraBangDiemChoPhepSua(lop);

        int currentSheetVersion = lop != null ? lop.getPhienBanBangDiemHieuLuc() : 0;
        Grade existingDiem = diemRepository.findByDangKyId(dangKyId).orElse(null);
        Long currentGradeVer = existingDiem != null ? existingDiem.getVersion() : null;

        if (request != null && request.phienBanBangDiem() != null && !request.phienBanBangDiem().equals(currentSheetVersion)) {
            throw ApiException.gradeConflict("Bảng điểm lớp học phần đã bị thay đổi bởi người khác (phiên bản bảng điểm: "
                    + currentSheetVersion + "). Vui lòng tải lại và đối chiếu.", currentGradeVer, currentSheetVersion);
        }
        if (existingDiem != null) {
            if (request != null && request.version() != null && !request.version().equals(existingDiem.getVersion())) {
                throw ApiException.gradeConflict("Điểm của sinh viên đã bị thay đổi bởi người khác (phiên bản hiện tại: "
                        + existingDiem.getVersion() + "). Vui lòng tải lại và đối chiếu.", existingDiem.getVersion(), currentSheetVersion);
            }
        } else if (request != null && request.version() != null && !request.version().equals(0L)) {
            throw ApiException.gradeConflict("Bản ghi điểm của sinh viên chưa tồn tại nhưng request mang phiên bản "
                    + request.version() + ". Vui lòng tải lại và đối chiếu.", null, currentSheetVersion);
        }

        Student sv = dangKy.getSinhVien();
        Long svId = sv != null ? sv.getId() : null;
        String maSv = sv != null ? sv.getMa() : null;

        List<ValidationErrorDetail> errors = new ArrayList<>();
        validateDiem(request != null ? request.diemQuaTrinh() : null, "diemQuaTrinh", dangKyId, svId, maSv, 1, errors);
        validateDiem(request != null ? request.diemCuoiKy() : null, "diemCuoiKy", dangKyId, svId, maSv, 1, errors);

        if (!errors.isEmpty()) {
            throw ApiException.validation(errors.get(0).message(), errors);
        }

        Double tongKet = tinhDiemTongKet(request.diemQuaTrinh(), request.diemCuoiKy());

        Double diemQuaTrinhCu = existingDiem != null ? existingDiem.getDiemQuaTrinh() : null;
        Double diemCuoiKyCu = existingDiem != null ? existingDiem.getDiemCuoiKy() : null;
        Double diemTongKetCu = existingDiem != null ? existingDiem.getDiemTongKet() : null;
        Long versionCu = existingDiem != null ? existingDiem.getVersion() : null;
        int phienBanCu = currentSheetVersion;

        Grade diem = existingDiem != null ? existingDiem : new Grade();
        if (existingDiem == null) {
            diem.setDangKy(dangKy);
        }
        diem.setDiemQuaTrinh(request.diemQuaTrinh());
        diem.setDiemCuoiKy(request.diemCuoiKy());
        diem.setDiemTongKet(tongKet);
        diem.setCapNhatLuc(LocalDateTime.now());

        if (lop != null) {
            lop.tangPhienBanBangDiem();
            if (lop.getTrangThaiBangDiem() == null) {
                lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
            }
            if (lop.getId() != null) {
                lopHocPhanRepository.save(lop);
            }
        }
        Grade daLuu = diemRepository.save(diem);

        final CourseClass lopRef = lop;
        executeAudited("GRADE_UPDATED", "GRADE", dangKyId, audit -> {
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("dangKyId", dangKy.getId());
            if (lopRef != null) {
                before.put("lopHocPhanId", lopRef.getId());
                before.put("maLop", lopRef.getMa());
            }
            before.put("diemQuaTrinhCu", diemQuaTrinhCu);
            before.put("diemCuoiKyCu", diemCuoiKyCu);
            before.put("diemTongKetCu", diemTongKetCu);
            before.put("versionCu", versionCu);
            before.put("phienBanBangDiemCu", phienBanCu);

            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("dangKyId", dangKy.getId());
            if (lopRef != null) {
                changes.put("lopHocPhanId", lopRef.getId());
                changes.put("maLop", lopRef.getMa());
                changes.put("phienBanBangDiemCu", phienBanCu);
                changes.put("phienBanBangDiemMoi", lopRef.getPhienBanBangDiemHieuLuc());
                changes.put("trangThaiBangDiem", lopRef.getTrangThaiBangDiemHieuLuc().name());
            }
            changes.put("soBanGhiDiem", 1);
            if (daLuu != null && daLuu.getId() != null) {
                changes.put("diemId", daLuu.getId());
            }
            changes.put("diemQuaTrinhMoi", daLuu != null ? daLuu.getDiemQuaTrinh() : null);
            changes.put("diemCuoiKyMoi", daLuu != null ? daLuu.getDiemCuoiKy() : null);
            changes.put("diemTongKetMoi", daLuu != null ? daLuu.getDiemTongKet() : null);
            changes.put("versionMoi", daLuu != null ? daLuu.getVersion() : null);
            audit.changes(before, changes);
            return null;
        });

        log.info("Đã lưu điểm cho đăng ký id={} thuộc lớp {} (trạng thái bảng điểm={})",
                dangKy.getId(), lop != null ? lop.getMa() : null, lop != null ? lop.getTrangThaiBangDiemHieuLuc() : null);
        return mapper.diem(dangKy, daLuu);
    }

    public List<RegistrationDtos.DiemDto> capNhatDiemHangLoat(Long lopHocPhanId, RegistrationDtos.CapNhatDiemHangLoatRequest request) {
        if (request == null || request.danhSach() == null || request.danhSach().isEmpty()) {
            throw ApiException.badRequest("Danh sách cập nhật điểm không được để trống");
        }
        CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
        kiemTraQuyenSuaDiem(lop);
        kiemTraBangDiemChoPhepSua(lop);

        int currentSheetVersion = lop.getPhienBanBangDiemHieuLuc();
        if (request.phienBanBangDiem() != null && !request.phienBanBangDiem().equals(currentSheetVersion)) {
            throw ApiException.gradeConflict("Bảng điểm lớp học phần đã bị thay đổi bởi người khác (phiên bản bảng điểm: "
                    + currentSheetVersion + "). Vui lòng tải lại và đối chiếu.", null, currentSheetVersion);
        }

        List<CourseRegistration> activeRegistrations =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
        Map<Long, CourseRegistration> activeMap = activeRegistrations.stream()
                .collect(Collectors.toMap(CourseRegistration::getId, Function.identity()));

        List<ValidationErrorDetail> errors = new ArrayList<>();
        int rowIndex = 1;
        for (RegistrationDtos.CapNhatDiemItemRequest item : request.danhSach()) {
            if (item.dangKyId() == null) {
                errors.add(new ValidationErrorDetail(null, null, null, rowIndex, "dangKyId", "dangKyId", "Mã đăng ký không được để trống"));
                rowIndex++;
                continue;
            }
            CourseRegistration dk = activeMap.get(item.dangKyId());
            if (dk == null) {
                errors.add(new ValidationErrorDetail(item.dangKyId(), null, null, rowIndex, "dangKyId", "dangKyId",
                        "Đăng ký không thuộc lớp học phần hoặc đã bị hủy"));
            } else {
                Student sv = dk.getSinhVien();
                Long svId = sv != null ? sv.getId() : null;
                String maSv = sv != null ? sv.getMa() : null;
                validateDiem(item.diemQuaTrinh(), "diemQuaTrinh", item.dangKyId(), svId, maSv, rowIndex, errors);
                validateDiem(item.diemCuoiKy(), "diemCuoiKy", item.dangKyId(), svId, maSv, rowIndex, errors);
            }
            rowIndex++;
        }

        if (!errors.isEmpty()) {
            String errorSummary = errors.size() == 1
                    ? errors.get(0).message()
                    : "Dữ liệu điểm không hợp lệ (" + errors.size() + " lỗi)";
            throw ApiException.validation(errorSummary, errors);
        }

        Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);

        // Kiểm tra optimistic lock version từng bản ghi điểm
        for (RegistrationDtos.CapNhatDiemItemRequest item : request.danhSach()) {
            Grade existing = diemMap.get(item.dangKyId());
            if (existing != null) {
                if (item.version() != null && !item.version().equals(existing.getVersion())) {
                    throw ApiException.gradeConflict("Điểm của đăng ký id " + item.dangKyId()
                            + " đã bị thay đổi bởi người khác (phiên bản hiện tại: " + existing.getVersion()
                            + "). Vui lòng tải lại và đối chiếu.", existing.getVersion(), currentSheetVersion);
                }
            } else if (item.version() != null && !item.version().equals(0L)) {
                throw ApiException.gradeConflict("Điểm của đăng ký id " + item.dangKyId()
                        + " không tồn tại nhưng request mang phiên bản " + item.version()
                        + ". Vui lòng tải lại và đối chiếu.", null, currentSheetVersion);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<Grade> toSave = new ArrayList<>();

        for (RegistrationDtos.CapNhatDiemItemRequest item : request.danhSach()) {
            CourseRegistration dk = activeMap.get(item.dangKyId());
            Grade diem = diemMap.get(item.dangKyId());
            if (diem == null) {
                diem = new Grade();
                diem.setDangKy(dk);
                diemMap.put(dk.getId(), diem);
            }
            diem.setDiemQuaTrinh(item.diemQuaTrinh());
            diem.setDiemCuoiKy(item.diemCuoiKy());
            diem.setDiemTongKet(tinhDiemTongKet(item.diemQuaTrinh(), item.diemCuoiKy()));
            diem.setCapNhatLuc(now);
            toSave.add(diem);
        }

        int phienBanCu = currentSheetVersion;
        lop.tangPhienBanBangDiem();
        if (lop.getTrangThaiBangDiem() == null) {
            lop.setTrangThaiBangDiem(GradeSheetStatus.NHAP);
        }
        lopHocPhanRepository.save(lop);
        diemRepository.saveAll(toSave);

        executeAudited("GRADE_UPDATED", "GRADE_SHEET", lopHocPhanId, audit -> {
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("lopHocPhanId", lop.getId());
            before.put("phienBanBangDiemCu", phienBanCu);

            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("lopHocPhanId", lop.getId());
            changes.put("maLop", lop.getMa());
            changes.put("soBanGhiDiem", toSave.size());
            changes.put("phienBanBangDiemCu", phienBanCu);
            changes.put("phienBanBangDiemMoi", lop.getPhienBanBangDiemHieuLuc());
            changes.put("trangThaiBangDiem", lop.getTrangThaiBangDiemHieuLuc().name());
            audit.changes(before, changes);
            return null;
        });

        log.info("Đã cập nhật hàng loạt {} điểm cho lớp {} (id={})", toSave.size(), lop.getMa(), lop.getId());

        return request.danhSach().stream()
                .map(item -> {
                    CourseRegistration dk = activeMap.get(item.dangKyId());
                    Grade d = diemMap.get(item.dangKyId());
                    return mapper.diem(dk, d);
                })
                .toList();
    }

    /**
     * Giảng viên phụ trách lớp gửi duyệt bảng điểm (NHAP hoặc TRA_VE -> CHO_DUYET).
     * Không phát thông báo công bố tới sinh viên.
     */
    public RegistrationDtos.TrangThaiBangDiemDto guiDuyetBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_SUBMITTED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenGuiDuyetBangDiem(lop);

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == GradeSheetStatus.CHO_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đang ở trạng thái chờ duyệt (CHO_DUYET)");
            }
            if (trangThaiCu == GradeSheetStatus.DA_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được phê duyệt");
            }
            if (trangThaiCu == GradeSheetStatus.DA_CONG_BO || trangThaiCu == GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được công bố/khóa");
            }
            if (trangThaiCu != GradeSheetStatus.NHAP && trangThaiCu != GradeSheetStatus.TRA_VE) {
                throw ApiException.conflict("Không thể gửi duyệt bảng điểm lớp " + lop.getMa() + " từ trạng thái " + trangThaiCu);
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<CourseRegistration> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(lop, trangThaiCu, activeRegistrations.size(), eligible.size());
            LocalDateTime now = LocalDateTime.now();
            lop.setTrangThaiBangDiem(GradeSheetStatus.CHO_DUYET);
            lop.setGuiDuyetBangDiemLuc(now);
            lop.setNguoiGuiDuyetBangDiemId(actor != null ? actor.getId() : null);
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);

            ghiLichSuChuyenTrangThai(lop, "GUI_DUYET", trangThaiCu, GradeSheetStatus.CHO_DUYET, null, actor, now);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.CHO_DUYET, activeRegistrations.size(), eligible.size());
            after.put("hanhDong", "GUI_DUYET");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.CHO_DUYET.name());
            audit.changes(before, after);

            log.info("Đã gửi duyệt bảng điểm lớp {} (id={}, trangThaiCu={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), trangThaiCu, eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Người có quyền duyệt của Phòng Đào tạo (QUAN_TRI) trả bảng điểm về chỉnh sửa kèm lý do bắt buộc
     * (CHO_DUYET hoặc DA_DUYET -> TRA_VE).
     */
    public RegistrationDtos.TrangThaiBangDiemDto traVeBangDiem(Long lopHocPhanId, String lyDo) {
        return executeAudited("GRADE_SHEET_RETURNED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenDuyetBangDiem("Chỉ người có quyền phê duyệt của Phòng Đào tạo (QUAN_TRI) mới được trả bảng điểm về chỉnh sửa");

            String cleanLyDo = chuanHoaLyDoBatBuoc(lyDo, "Lý do trả bảng điểm về chỉnh sửa là bắt buộc");

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == GradeSheetStatus.DA_CONG_BO || trangThaiCu == GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đã được công bố/khóa, không thể trả sửa trực tiếp. Vui lòng mở khóa kèm lý do nếu cần điều chỉnh");
            }
            if (trangThaiCu != GradeSheetStatus.CHO_DUYET && trangThaiCu != GradeSheetStatus.DA_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đang ở trạng thái " + trangThaiCu + ", chỉ có thể trả về khi đang Chờ duyệt (CHO_DUYET) hoặc Đã duyệt (DA_DUYET)");
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            int soDuDieuKien = demSinhVienDuDieuKien(activeRegistrations, diemMap);

            Map<String, Object> before = gradeSheetAuditSnapshot(lop, trangThaiCu, activeRegistrations.size(), soDuDieuKien);
            LocalDateTime now = LocalDateTime.now();
            lop.setTrangThaiBangDiem(GradeSheetStatus.TRA_VE);
            lop.setTraVeBangDiemLuc(now);
            lop.setNguoiTraVeBangDiemId(actor != null ? actor.getId() : null);
            lop.setLyDoTraVeBangDiem(cleanLyDo);
            lop.setDuyetBangDiemLuc(null);
            lop.setNguoiDuyetBangDiemId(null);
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);

            ghiLichSuChuyenTrangThai(lop, "TRA_VE", trangThaiCu, GradeSheetStatus.TRA_VE, cleanLyDo, actor, now);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.TRA_VE, activeRegistrations.size(), soDuDieuKien);
            after.put("hanhDong", "TRA_VE");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.TRA_VE.name());
            after.put("lyDo", cleanLyDo);
            after.put("lyDoTraVe", cleanLyDo);
            audit.changes(before, after);

            log.info("Đã trả bảng điểm lớp {} (id={}) về chỉnh sửa với lý do: {}",
                    lop.getMa(), lop.getId(), cleanLyDo);
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), soDuDieuKien);
        });
    }

    /**
     * Người có quyền duyệt của Phòng Đào tạo (QUAN_TRI) phê duyệt bảng điểm (CHO_DUYET -> DA_DUYET).
     * Chưa phát thông báo tới sinh viên cho đến khi công bố.
     */
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_APPROVED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenDuyetBangDiem("Chỉ người có quyền phê duyệt của Phòng Đào tạo mới được duyệt bảng điểm");

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == GradeSheetStatus.DA_CONG_BO || trangThaiCu == GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được công bố/khóa");
            }
            if (trangThaiCu == GradeSheetStatus.DA_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã được phê duyệt");
            }
            if (trangThaiCu != GradeSheetStatus.CHO_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đang ở trạng thái " + trangThaiCu + ", giảng viên phụ trách cần gửi duyệt trước khi phê duyệt");
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<CourseRegistration> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(lop, trangThaiCu, activeRegistrations.size(), eligible.size());
            LocalDateTime now = LocalDateTime.now();
            if (lop.getGuiDuyetBangDiemLuc() == null) {
                lop.setGuiDuyetBangDiemLuc(now);
            }
            lop.setTrangThaiBangDiem(GradeSheetStatus.DA_DUYET);
            lop.setDuyetBangDiemLuc(now);
            lop.setNguoiDuyetBangDiemId(actor != null ? actor.getId() : null);
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);

            ghiLichSuChuyenTrangThai(lop, "PHE_DUYET", trangThaiCu, GradeSheetStatus.DA_DUYET, null, actor, now);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.DA_DUYET, activeRegistrations.size(), eligible.size());
            after.put("hanhDong", "PHE_DUYET");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.DA_DUYET.name());
            audit.changes(before, after);

            log.info("Đã phê duyệt bảng điểm lớp {} (id={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Người có quyền công bố/khóa của Phòng Đào tạo (QUAN_TRI) công bố bảng điểm đã duyệt (DA_DUYET -> DA_CONG_BO),
     * đồng thời khóa chỉnh sửa bảng điểm và phát sự kiện GRADES_PUBLISHED sau khi transaction commit.
     */
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(Long lopHocPhanId) {
        return executeAudited("GRADE_SHEET_PUBLISHED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenDuyetBangDiem("Chỉ người có quyền công bố/khóa của Phòng Đào tạo (QUAN_TRI) mới được công bố bảng điểm");

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == GradeSheetStatus.NHAP
                    || trangThaiCu == GradeSheetStatus.CHO_DUYET
                    || trangThaiCu == GradeSheetStatus.TRA_VE) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đang ở trạng thái " + trangThaiCu + ", cần được phê duyệt trước khi công bố");
            }
            if (trangThaiCu == GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã ở trạng thái khóa (DA_KHOA)");
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<CourseRegistration> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(
                    lop, trangThaiCu, activeRegistrations.size(), eligible.size());

            boolean congBoMoi = trangThaiCu != GradeSheetStatus.DA_CONG_BO
                    || lop.getCongBoBangDiemLuc() == null;

            LocalDateTime publishedAt = lop.getCongBoBangDiemLuc() != null
                    ? lop.getCongBoBangDiemLuc()
                    : LocalDateTime.now();

            int version = Math.max(1, lop.getPhienBanCongBo());

            if (congBoMoi) {
                // Mỗi lần công bố mới phải có phiên bản mới để không dùng lại idempotency key cũ.
                version = Math.max(1, lop.getPhienBanCongBo() + 1);
                publishedAt = LocalDateTime.now();

                lop.setTrangThaiBangDiem(GradeSheetStatus.DA_CONG_BO);
                lop.setPhienBanCongBo(version);
                lop.setCongBoBangDiemLuc(publishedAt);
                lop.setNguoiCongBoBangDiemId(actor != null ? actor.getId() : null);
                lop.setKhoaBangDiemLuc(publishedAt);
                lop.setNguoiKhoaBangDiemId(actor != null ? actor.getId() : null);
                lop.tangPhienBanBangDiem();
                lopHocPhanRepository.save(lop);

                ghiLichSuChuyenTrangThai(
                        lop, "CONG_BO", trangThaiCu, GradeSheetStatus.DA_CONG_BO,
                        null, actor, publishedAt
                );
            }

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.DA_CONG_BO, activeRegistrations.size(), eligible.size());
            after.put("hanhDong", "CONG_BO");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.DA_CONG_BO.name());
            audit.changes(before, after);

            notifyGradesPublishedSafely(lop, eligible, version, publishedAt);

            log.info("Đã công bố bảng điểm lớp {} (id={}, phienBan={}, soSinhVienDuDieuKien={})",
                    lop.getMa(), lop.getId(), version, eligible.size());
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Người có quyền phù hợp (QUAN_TRI) khóa bảng điểm (DA_CONG_BO hoặc DA_DUYET -> DA_KHOA).
     * Nếu khóa trực tiếp từ DA_DUYET mà chưa công bố, hệ thống đồng thời công bố điểm và phát thông báo sau commit.
     */
    public RegistrationDtos.TrangThaiBangDiemDto khoaBangDiem(Long lopHocPhanId, String lyDo) {
        return executeAudited("GRADE_SHEET_LOCKED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenDuyetBangDiem("Chỉ người có quyền công bố/khóa của Phòng Đào tạo (QUAN_TRI) mới được khóa bảng điểm");

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu == GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa() + " đã ở trạng thái khóa (DA_KHOA)");
            }
            if (trangThaiCu != GradeSheetStatus.DA_CONG_BO && trangThaiCu != GradeSheetStatus.DA_DUYET) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " đang ở trạng thái " + trangThaiCu + ", cần được phê duyệt hoặc công bố trước khi khóa");
            }

            String cleanLyDo = chuanHoaLyDoTuyChon(lyDo);

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            List<CourseRegistration> eligible = locDangKyCoDiemHopLe(activeRegistrations, diemMap);
            kiemTraDuDieuKienDuyetVaCongBo(lop, activeRegistrations, eligible);

            Map<String, Object> before = gradeSheetAuditSnapshot(
                    lop, trangThaiCu, activeRegistrations.size(), eligible.size());

            LocalDateTime now = LocalDateTime.now();
            boolean vuaCongBo = lop.getCongBoBangDiemLuc() == null;
            int version = Math.max(1, lop.getPhienBanCongBo());

            if (vuaCongBo) {
                // Khóa trực tiếp từ DA_DUYET cũng là một lần công bố mới.
                version = Math.max(1, lop.getPhienBanCongBo() + 1);

                lop.setCongBoBangDiemLuc(now);
                lop.setNguoiCongBoBangDiemId(actor != null ? actor.getId() : null);
                lop.setPhienBanCongBo(version);
            }

            lop.setTrangThaiBangDiem(GradeSheetStatus.DA_KHOA);
            lop.setKhoaBangDiemLuc(now);
            lop.setNguoiKhoaBangDiemId(actor != null ? actor.getId() : null);
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);

            ghiLichSuChuyenTrangThai(lop, "KHOA", trangThaiCu, GradeSheetStatus.DA_KHOA, cleanLyDo, actor, now);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.DA_KHOA, activeRegistrations.size(), eligible.size());
            after.put("hanhDong", "KHOA");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.DA_KHOA.name());
            if (cleanLyDo != null) {
                after.put("lyDo", cleanLyDo);
            }
            audit.changes(before, after);

            if (vuaCongBo) {
                notifyGradesPublishedSafely(lop, eligible, version, lop.getCongBoBangDiemLuc());
            }

            log.info("Đã khóa bảng điểm lớp {} (id={}, trangThaiCu={})", lop.getMa(), lop.getId(), trangThaiCu);
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), eligible.size());
        });
    }

    /**
     * Mở khóa bảng điểm đã công bố/đã khóa (DA_CONG_BO hoặc DA_KHOA -> TRA_VE).
     * Yêu cầu quyền đặc biệt (QUAN_TRI) và lý do bắt buộc.
     */
    public RegistrationDtos.TrangThaiBangDiemDto moKhoaBangDiem(Long lopHocPhanId, String lyDo) {
        return executeAudited("GRADE_SHEET_UNLOCKED", "GRADE_SHEET", lopHocPhanId, audit -> {
            CourseClass lop = layLopDeCapNhatBangDiem(lopHocPhanId);
            User actor = kiemTraQuyenDuyetBangDiem("Chỉ người có quyền đặc biệt (QUAN_TRI) mới được mở khóa bảng điểm");

            String cleanLyDo = chuanHoaLyDoBatBuoc(lyDo, "Lý do mở khóa bảng điểm là bắt buộc");

            GradeSheetStatus trangThaiCu = lop.getTrangThaiBangDiemHieuLuc();
            if (trangThaiCu != GradeSheetStatus.DA_CONG_BO && trangThaiCu != GradeSheetStatus.DA_KHOA) {
                throw ApiException.conflict("Bảng điểm lớp " + lop.getMa()
                        + " chưa ở trạng thái đã công bố hoặc đã khóa nên không thể mở khóa");
            }

            List<CourseRegistration> activeRegistrations =
                    dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);
            Map<Long, Grade> diemMap = layDiemTheoDangKy(activeRegistrations);
            int soDuDieuKien = demSinhVienDuDieuKien(activeRegistrations, diemMap);

            Map<String, Object> before = gradeSheetAuditSnapshot(
                    lop, trangThaiCu, activeRegistrations.size(), soDuDieuKien);

            LocalDateTime now = LocalDateTime.now();
            lop.setTrangThaiBangDiem(GradeSheetStatus.TRA_VE);
            lop.setMoKhoaBangDiemLuc(now);
            lop.setNguoiMoKhoaBangDiemId(actor != null ? actor.getId() : null);
            lop.setLyDoMoKhoaBangDiem(cleanLyDo);
            lop.setTraVeBangDiemLuc(now);
            lop.setNguoiTraVeBangDiemId(actor != null ? actor.getId() : null);
            lop.setLyDoTraVeBangDiem(cleanLyDo);
            lop.setDuyetBangDiemLuc(null);
            lop.setNguoiDuyetBangDiemId(null);
            lop.setCongBoBangDiemLuc(null);
            lop.setNguoiCongBoBangDiemId(null);
            lop.setKhoaBangDiemLuc(null);
            lop.setNguoiKhoaBangDiemId(null);
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);

            ghiLichSuChuyenTrangThai(lop, "MO_KHOA", trangThaiCu, GradeSheetStatus.TRA_VE, cleanLyDo, actor, now);

            Map<String, Object> after = gradeSheetAuditSnapshot(
                    lop, GradeSheetStatus.TRA_VE, activeRegistrations.size(), soDuDieuKien);
            after.put("hanhDong", "MO_KHOA");
            after.put("trangThaiCu", trangThaiCu.name());
            after.put("trangThaiMoi", GradeSheetStatus.TRA_VE.name());
            after.put("lyDo", cleanLyDo);
            after.put("lyDoMoKhoa", cleanLyDo);
            audit.changes(before, after);

            log.info("Đã mở khóa bảng điểm lớp {} (id={}, trangThaiCu={}) với lý do: {}",
                    lop.getMa(), lop.getId(), trangThaiCu, cleanLyDo);
            return toTrangThaiBangDiemDto(lop, activeRegistrations.size(), soDuDieuKien);
        });
    }

    /**
     * Alias tương thích ngược cho thao tác thu hồi công bố / mở khóa bảng điểm; bắt buộc truyền lý do.
     */
    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(Long lopHocPhanId, String lyDo) {
        return moKhoaBangDiem(lopHocPhanId, lyDo);
    }

    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(Long lopHocPhanId) {
        return moKhoaBangDiem(lopHocPhanId, null);
    }

    /**
     * QLSV-85: Điều chỉnh điểm thông qua hồ sơ phúc khảo được chấp thuận.
     * Đi qua luồng điều chỉnh có audit bắt buộc; ghi nhận snapshot trước/sau và lý do,
     * tuyệt đối không sửa âm thầm điểm gốc và không mở khóa tùy tiện cả lớp.
     */
    public RegistrationDtos.DiemDto dieuChinhDiemPhucKhao(Long diemId, Long phucKhaoId,
                                                          Double diemQuaTrinhMoi, Double diemCuoiKyMoi,
                                                          String lyDo, User actor) {
        if (diemId == null) {
            throw ApiException.badRequest("Mã bản ghi điểm không được để trống");
        }
        Grade diem = diemRepository.findWithDetailsById(diemId)
                .orElseThrow(() -> ApiException.notFound("Bản ghi điểm không tồn tại"));

        CourseRegistration dangKy = diem.getDangKy();
        CourseClass lop = dangKy != null ? dangKy.getLopHocPhan() : null;

        if (actor == null || actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ người có quyền quản trị (QUAN_TRI) mới được điều chỉnh điểm sau phúc khảo");
        }

        List<ValidationErrorDetail> errors = new ArrayList<>();
        Student sv = dangKy != null ? dangKy.getSinhVien() : null;
        Long svId = sv != null ? sv.getId() : null;
        String maSv = sv != null ? sv.getMa() : null;

        Double finalDiemQuaTrinh = diemQuaTrinhMoi != null ? diemQuaTrinhMoi : diem.getDiemQuaTrinh();
        Double finalDiemCuoiKy = diemCuoiKyMoi != null ? diemCuoiKyMoi : diem.getDiemCuoiKy();

        validateDiem(finalDiemQuaTrinh, "diemQuaTrinh", dangKy != null ? dangKy.getId() : null, svId, maSv, 1, errors);
        validateDiem(finalDiemCuoiKy, "diemCuoiKy", dangKy != null ? dangKy.getId() : null, svId, maSv, 1, errors);

        if (!errors.isEmpty()) {
            throw ApiException.validation(errors.get(0).message(), errors);
        }

        Double tongKetMoi = tinhDiemTongKet(finalDiemQuaTrinh, finalDiemCuoiKy);

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("diemId", diem.getId());
        if (dangKy != null) {
            before.put("dangKyId", dangKy.getId());
        }
        if (lop != null) {
            before.put("lopHocPhanId", lop.getId());
            before.put("maLop", lop.getMa());
            before.put("phienBanBangDiemCu", lop.getPhienBanBangDiemHieuLuc());
        }
        before.put("diemQuaTrinh", diem.getDiemQuaTrinh());
        before.put("diemCuoiKy", diem.getDiemCuoiKy());
        before.put("diemTongKet", diem.getDiemTongKet());
        before.put("versionCu", diem.getVersion());

        LocalDateTime now = LocalDateTime.now();
        diem.setDiemQuaTrinh(finalDiemQuaTrinh);
        diem.setDiemCuoiKy(finalDiemCuoiKy);
        diem.setDiemTongKet(tongKetMoi);
        diem.setCapNhatLuc(now);
        if (lop != null) {
            lop.tangPhienBanBangDiem();
            lopHocPhanRepository.save(lop);
        }
        Grade daLuu = diemRepository.save(diem);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("diemId", daLuu.getId());
        if (dangKy != null) {
            after.put("dangKyId", dangKy.getId());
        }
        if (lop != null) {
            after.put("lopHocPhanId", lop.getId());
            after.put("maLop", lop.getMa());
            after.put("trangThaiBangDiem", lop.getTrangThaiBangDiemHieuLuc().name());
            after.put("phienBanBangDiemMoi", lop.getPhienBanBangDiemHieuLuc());
        }
        after.put("diemQuaTrinh", daLuu.getDiemQuaTrinh());
        after.put("diemCuoiKy", daLuu.getDiemCuoiKy());
        after.put("diemTongKet", daLuu.getDiemTongKet());
        after.put("versionMoi", daLuu.getVersion());
        after.put("phucKhaoId", phucKhaoId);
        after.put("lyDo", lyDo);

        executeAudited("GRADE_ADJUSTED_VIA_APPEAL", "GRADE", diem.getId(), audit -> {
            audit.changes(before, after);
            return null;
        });

        log.info("Đã điều chỉnh điểm phúc khảo id={} cho đăng ký id={} (điểm mới: QT={}, CK={}, TK={})",
                phucKhaoId, dangKy != null ? dangKy.getId() : null, finalDiemQuaTrinh, finalDiemCuoiKy, tongKetMoi);
        return mapper.diem(dangKy, daLuu);
    }

    @Transactional(readOnly = true)
    public RegistrationDtos.KetQuaHocTapDto ketQuaHocTap() {
        Student sinhVien = sinhVienHienTai();
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findAllActiveOfStudent(sinhVien.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> diemMap = layDiemTheoDangKy(danhSach);

        List<RegistrationDtos.DangKyDto> items = danhSach.stream()
                .map(dk -> mapper.dangKy(dk, diemMap.get(dk.getId())))
                .toList();

        Map<Long, List<RegistrationDtos.DangKyDto>> theoHocKy = items.stream()
                .collect(Collectors.groupingBy(RegistrationDtos.DangKyDto::hocKyId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RegistrationDtos.HocKyTongKetDto> tongKetTheoHocKy = new ArrayList<>();
        theoHocKy.forEach((hocKyId, danhSachMon) -> {
            RegistrationDtos.DangKyDto dau = danhSachMon.get(0);
            TongHop tongHop = tinhTongHop(danhSachMon);
            tongKetTheoHocKy.add(new RegistrationDtos.HocKyTongKetDto(hocKyId, dau.tenHocKy(),
                    dau.namHoc(), tongHop.tongTinChi(), tongHop.tinChiCoDiem(), tongHop.gpa10(), tongHop.gpa4()));
        });

        TongHop tongHop = tinhTongHop(items);
        return new RegistrationDtos.KetQuaHocTapDto(items, tongKetTheoHocKy, tongHop.tongTinChi(),
                tongHop.gpa10(), tongHop.gpa4(), tongHop.tinChiTichLuy());
    }

    @Transactional(readOnly = true)
    public RegistrationDtos.LichSuHocTapDto lichSuHocTap(Long hocKyId, String trangThai) {
        if (hocKyId != null) {
            if (hocKyId <= 0) {
                throw ApiException.badRequest("Học kỳ không hợp lệ");
            }
            if (hocKyRepository != null && !hocKyRepository.existsById(hocKyId)) {
                throw ApiException.notFound("Học kỳ không tồn tại");
            }
        }
        String statusFilter = chuanHoaTrangThaiKetQua(trangThai);

        Student sinhVien = sinhVienHienTai();
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findAllActiveOfStudent(sinhVien.getId(), RegistrationStatus.DA_DANG_KY);
        Map<Long, Grade> diemMap = layDiemTheoDangKy(danhSach);

        Comparator<CourseRegistration> thuTuThoiGian = Comparator
                .comparing((CourseRegistration dk) -> dk.getLopHocPhan().getHocKy().getNgayBatDau(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getHocKy().getId(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMonHoc().getMa(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMonHoc().getId(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseRegistration::getThoiGianDangKy,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(dk -> dk.getLopHocPhan().getMa(),
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CourseRegistration::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        List<CourseRegistration> daSapXep = danhSach.stream().sorted(thuTuThoiGian).toList();

        Map<Long, Integer> tongSoLanTheoMon = new HashMap<>();
        for (CourseRegistration dk : daSapXep) {
            Long monHocId = dk.getLopHocPhan().getMonHoc().getId();
            tongSoLanTheoMon.merge(monHocId, 1, Integer::sum);
        }

        Map<Long, Integer> demLanTheoMon = new HashMap<>();
        List<RegistrationDtos.MucLichSuHocTapDto> tatCaMuc = new ArrayList<>(daSapXep.size());
        for (CourseRegistration dk : daSapXep) {
            CourseClass lop = dk.getLopHocPhan();
            Subject mon = lop.getMonHoc();
            Semester ky = lop.getHocKy();
            int lanHoc = demLanTheoMon.merge(mon.getId(), 1, Integer::sum);
            int tongSoLanHoc = tongSoLanTheoMon.getOrDefault(mon.getId(), 1);

            Grade diem = diemMap.get(dk.getId());
            boolean daCongBo = lop.isBangDiemDaCongBo() && diem != null && diem.getDiemTongKet() != null;
            Double diemQuaTrinh = daCongBo ? diem.getDiemQuaTrinh() : null;
            Double diemCuoiKy = daCongBo ? diem.getDiemCuoiKy() : null;
            Double diemTongKet = daCongBo ? diem.getDiemTongKet() : null;

            String maTrangThai;
            String tenTrangThai;
            if (!daCongBo) {
                maTrangThai = "CHUA_CO_DIEM";
                tenTrangThai = "Chưa có điểm";
            } else if (diemTongKet >= AcademicRules.DIEM_DAT) {
                maTrangThai = "DAT";
                tenTrangThai = "Đạt";
            } else {
                maTrangThai = "CHUA_DAT";
                tenTrangThai = "Chưa đạt";
            }

            tatCaMuc.add(new RegistrationDtos.MucLichSuHocTapDto(
                    dk.getId(),
                    ky.getId(),
                    ky.getTen(),
                    ky.getNamHoc(),
                    ky.getNgayBatDau(),
                    mon.getId(),
                    mon.getMa(),
                    mon.getTen(),
                    mon.getSoTinChi(),
                    lop.getId(),
                    lop.getMa(),
                    lop.getGiangVien() != null ? lop.getGiangVien().getHoTen() : null,
                    dk.getThoiGianDangKy(),
                    diemQuaTrinh,
                    diemCuoiKy,
                    diemTongKet,
                    maTrangThai,
                    tenTrangThai,
                    lanHoc,
                    tongSoLanHoc));
        }

        boolean coNhieuLanHoc = tatCaMuc.stream().anyMatch(item -> item.tongSoLanHoc() > 1);

        List<RegistrationDtos.MucLichSuHocTapDto> items = tatCaMuc.stream()
                .filter(item -> hocKyId == null || item.hocKyId().equals(hocKyId))
                .filter(item -> statusFilter == null || item.trangThaiKetQua().equals(statusFilter))
                .toList();

        Map<Long, List<RegistrationDtos.MucLichSuHocTapDto>> theoHocKy = items.stream()
                .collect(Collectors.groupingBy(RegistrationDtos.MucLichSuHocTapDto::hocKyId,
                        LinkedHashMap::new, Collectors.toList()));

        List<RegistrationDtos.HocKyTongKetDto> tongKetTheoHocKy = new ArrayList<>();
        theoHocKy.forEach((kyId, danhSachMon) -> {
            RegistrationDtos.MucLichSuHocTapDto dau = danhSachMon.get(0);
            TongHop tongHopKy = tinhTongHopLichSu(danhSachMon);
            tongKetTheoHocKy.add(new RegistrationDtos.HocKyTongKetDto(kyId, dau.tenHocKy(),
                    dau.namHoc(), tongHopKy.tongTinChi(), tongHopKy.tinChiCoDiem(),
                    tongHopKy.gpa10(), tongHopKy.gpa4()));
        });

        TongHop tongHop = tinhTongHopLichSu(items);
        List<RegistrationDtos.MucLichSuHocTapDto> monChuaDat = items.stream()
                .filter(item -> "CHUA_DAT".equals(item.trangThaiKetQua()))
                .toList();
        int soMonDat = (int) items.stream().filter(item -> "DAT".equals(item.trangThaiKetQua())).count();
        int soMonChuaDat = monChuaDat.size();
        int soMonChuaCoDiem = (int) items.stream().filter(item -> "CHUA_CO_DIEM".equals(item.trangThaiKetQua())).count();

        return new RegistrationDtos.LichSuHocTapDto(
                items,
                tongKetTheoHocKy,
                monChuaDat,
                tongHop.tongTinChi(),
                tongHop.tinChiCoDiem(),
                tongHop.tinChiTichLuy(),
                tongHop.gpa10(),
                tongHop.gpa4(),
                soMonDat,
                soMonChuaDat,
                soMonChuaCoDiem,
                coNhieuLanHoc);
    }

    CourseClass layLopDeCapNhatBangDiem(Long lopHocPhanId) {
        return lopHocPhanRepository.findByIdForUpdate(lopHocPhanId)
                .or(() -> lopHocPhanRepository.findWithDetailsById(lopHocPhanId))
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
    }

    public static Double tinhDiemTongKet(Double diemQuaTrinh, Double diemCuoiKy) {
        if (diemQuaTrinh == null || diemCuoiKy == null) {
            return null;
        }
        return Math.round((0.4 * diemQuaTrinh + 0.6 * diemCuoiKy) * 10.0) / 10.0;
    }

    public static void validateDiem(Double diem, String tenCot, Long dangKyId, Long sinhVienId,
                                    String maSinhVien, Integer dong, List<ValidationErrorDetail> errors) {
        validateDiem(diem, null, tenCot, null, tenCot, null, dangKyId, sinhVienId, maSinhVien, dong, errors);
    }

    public static void validateDiem(Double diem, String sheet, String tenCot, String cotExcel,
                                    String field, String giaTriNhap, Long dangKyId, Long sinhVienId,
                                    String maSinhVien, Integer dong, List<ValidationErrorDetail> errors) {
        if (diem == null) {
            return;
        }
        if (Double.isNaN(diem) || Double.isInfinite(diem) || diem < 0.0 || diem > 10.0) {
            errors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheet, dong, tenCot, cotExcel, field, giaTriNhap,
                    "Điểm phải từ 0 đến 10"
            ));
            return;
        }
        BigDecimal bd = BigDecimal.valueOf(diem);
        if (bd.stripTrailingZeros().scale() > 2) {
            errors.add(new ValidationErrorDetail(
                    dangKyId, sinhVienId, maSinhVien, sheet, dong, tenCot, cotExcel, field, giaTriNhap,
                    "Điểm chỉ được có tối đa 2 chữ số thập phân"
            ));
        }
    }

    /**
     * Chặn chỉnh sửa hoặc import điểm khi bảng điểm không ở trạng thái cho phép sửa (NHAP hoặc TRA_VE).
     */
    public void kiemTraBangDiemChoPhepSua(CourseClass lop) {
        GradeSheetStatus status = lop.getTrangThaiBangDiemHieuLuc();
        if (status.isChoPhepSuaDiem()) {
            return;
        }
        switch (status) {
            case CHO_DUYET -> throw ApiException.conflict(
                    "Bảng điểm lớp học phần đã được gửi duyệt (CHO_DUYET), không thể chỉnh sửa hoặc import điểm trực tiếp");
            case DA_DUYET -> throw ApiException.conflict(
                    "Bảng điểm lớp học phần đã được phê duyệt (DA_DUYET), không thể chỉnh sửa hoặc import điểm trực tiếp");
            case DA_CONG_BO -> throw ApiException.conflict(
                    "Bảng điểm lớp học phần đã được công bố, không thể chỉnh sửa trực tiếp. Vui lòng mở khóa kèm lý do trước khi điều chỉnh");
            case DA_KHOA -> throw ApiException.conflict(
                    "Bảng điểm lớp học phần đã bị khóa (DA_KHOA), không thể chỉnh sửa hoặc import điểm trực tiếp. Vui lòng mở khóa kèm lý do trước khi điều chỉnh");
            default -> throw ApiException.conflict(
                    "Bảng điểm lớp học phần không ở trạng thái cho phép chỉnh sửa (" + status + ")");
        }
    }

    Lecturer kiemTraQuyenSuaDiem(CourseClass lop) {
        User actor = currentUser.get();
        if (actor != null && actor.getRole() != null && actor.getRole() != Role.GIANG_VIEN) {
            throw ApiException.forbidden("Chỉ giảng viên phụ trách lớp mới có quyền sửa điểm");
        }
        Lecturer giangVien = giangVienHienTai();
        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }
        return giangVien;
    }

    private User kiemTraQuyenGuiDuyetBangDiem(CourseClass lop) {
        User actor = currentUser.get();
        if (actor != null && actor.getRole() != null && actor.getRole() != Role.GIANG_VIEN) {
            throw ApiException.forbidden("Chỉ giảng viên phụ trách lớp học phần mới có quyền gửi duyệt bảng điểm");
        }
        kiemTraQuyenSuaDiem(lop);
        return actor;
    }

    private User kiemTraQuyenDuyetBangDiem(String forbiddenMessage) {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden(forbiddenMessage);
        }
        return actor;
    }

    private User kiemTraQuyenXemBangDiem(CourseClass lop) {
        User actor = currentUser.get();
        if (actor != null && actor.getRole() == Role.QUAN_TRI) {
            return actor;
        }
        Lecturer giangVien = giangVienHienTai();
        if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
            throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
        }
        return actor;
    }

    private String chuanHoaLyDoBatBuoc(String lyDo, String emptyMessage) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            throw ApiException.badRequest(emptyMessage);
        }
        String trimmed = lyDo.trim();
        if (trimmed.length() > 500) {
            throw ApiException.badRequest("Lý do tối đa 500 ký tự");
        }
        return trimmed;
    }

    private String chuanHoaLyDoTuyChon(String lyDo) {
        if (lyDo == null || lyDo.trim().isEmpty()) {
            return null;
        }
        String trimmed = lyDo.trim();
        if (trimmed.length() > 500) {
            throw ApiException.badRequest("Lý do tối đa 500 ký tự");
        }
        return trimmed;
    }

    private void ghiLichSuChuyenTrangThai(CourseClass lop, String hanhDong,
                                          GradeSheetStatus trangThaiTruoc, GradeSheetStatus trangThaiSau,
                                          String lyDo, User actor, LocalDateTime thoiDiem) {
        if (lichSuTrangThaiRepository == null || lop == null || lop.getId() == null) {
            return;
        }
        GradeSheetStateHistory item = new GradeSheetStateHistory();
        item.setLopHocPhan(lop);
        item.setHanhDong(hanhDong);
        item.setTrangThaiTruoc(trangThaiTruoc);
        item.setTrangThaiSau(trangThaiSau);
        item.setLyDo(lyDo);
        item.setNguoiThucHienId(actor != null ? actor.getId() : null);
        item.setNguoiThucHienUsername(actor != null ? actor.getUsername() : null);
        item.setNguoiThucHienRole(actor != null && actor.getRole() != null ? actor.getRole().name() : null);
        item.setThoiDiem(thoiDiem != null ? thoiDiem : LocalDateTime.now());
        lichSuTrangThaiRepository.save(item);
    }

    private List<RegistrationDtos.LichSuTrangThaiBangDiemDto> layLichSuChuyenTrangThai(Long lopHocPhanId) {
        if (lichSuTrangThaiRepository == null || lopHocPhanId == null) {
            return List.of();
        }
        return lichSuTrangThaiRepository.findByLopHocPhanIdOrderByThoiDiemDescIdDesc(lopHocPhanId)
                .stream()
                .map(row -> new RegistrationDtos.LichSuTrangThaiBangDiemDto(
                        row.getId(),
                        lopHocPhanId,
                        row.getHanhDong(),
                        row.getTrangThaiTruoc().name(),
                        row.getTrangThaiSau().name(),
                        row.getLyDo(),
                        row.getNguoiThucHienId(),
                        row.getNguoiThucHienUsername(),
                        row.getNguoiThucHienRole(),
                        row.getThoiDiem()))
                .toList();
    }

    private List<CourseRegistration> locDangKyCoDiemHopLe(List<CourseRegistration> activeRegistrations, Map<Long, Grade> diemMap) {
        List<CourseRegistration> eligible = new ArrayList<>();
        for (CourseRegistration dk : activeRegistrations) {
            if (dk.getTrangThai() != RegistrationStatus.DA_DANG_KY) {
                continue;
            }
            Grade diem = diemMap.get(dk.getId());
            if (laDiemDayDu(diem)) {
                eligible.add(dk);
            }
        }
        return eligible;
    }

    /** A grade row counts as complete when process, final and total scores are all present. */
    public static boolean laDiemDayDu(Grade diem) {
        return diem != null && diem.getDiemQuaTrinh() != null
                && diem.getDiemCuoiKy() != null && diem.getDiemTongKet() != null;
    }

    private int demSinhVienDuDieuKien(List<CourseRegistration> activeRegistrations, Map<Long, Grade> diemMap) {
        return locDangKyCoDiemHopLe(activeRegistrations, diemMap).size();
    }

    private void kiemTraDuDieuKienDuyetVaCongBo(CourseClass lop,
                                                List<CourseRegistration> activeRegistrations,
                                                List<CourseRegistration> eligibleRegistrations) {
        if (activeRegistrations.isEmpty()) {
            throw ApiException.badRequest("Lớp học phần " + lop.getMa() + " chưa có sinh viên đang theo học để duyệt hoặc công bố điểm");
        }
        if (eligibleRegistrations.size() < activeRegistrations.size()) {
            throw ApiException.badRequest("Còn sinh viên đang theo học lớp " + lop.getMa()
                    + " chưa được nhập đủ điểm (" + eligibleRegistrations.size() + "/"
                    + activeRegistrations.size() + "), không thể duyệt hoặc công bố bảng điểm");
        }
    }

    private void notifyGradesPublishedSafely(CourseClass lop, List<CourseRegistration> eligibleRegistrations,
                                             int version, LocalDateTime publishedAt) {
        if (thongBaoService == null) {
            return;
        }
        try {
            thongBaoService.gradesPublished(lop, eligibleRegistrations, version, publishedAt);
        } catch (RuntimeException ex) {
            log.error("Không thể lập lịch thông báo công bố điểm cho lớp id={} (ma={}); sẽ đồng bộ bù khi sinh viên mở trung tâm thông báo: {}",
                    lop.getId(), lop.getMa(), ex.getClass().getSimpleName());
        }
    }

    private <T> T executeAudited(String action, String entityType, Object entityId,
                                 Function<AuditLogService.AuditContext, T> operation) {
        if (auditLogService == null) {
            return operation.apply(new AuditLogService.AuditContext(entityId == null ? null : String.valueOf(entityId)));
        }
        return auditLogService.execute(action, entityType, entityId, operation);
    }

    private Map<String, Object> gradeSheetAuditSnapshot(CourseClass lop, GradeSheetStatus trangThaiBangDiem,
                                                        int soSinhVienDangHoc, int soSinhVienDuDieuKien) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("lopHocPhanId", lop.getId());
        snapshot.put("maLop", lop.getMa());
        if (lop.getMonHoc() != null) {
            snapshot.put("monHocId", lop.getMonHoc().getId());
            snapshot.put("maMon", lop.getMonHoc().getMa());
            snapshot.put("tenMon", lop.getMonHoc().getTen());
        }
        if (lop.getHocKy() != null) {
            snapshot.put("hocKyId", lop.getHocKy().getId());
            snapshot.put("tenHocKy", lop.getHocKy().getTen());
            snapshot.put("namHoc", lop.getHocKy().getNamHoc());
        }
        snapshot.put("trangThaiBangDiem", trangThaiBangDiem.name());
        snapshot.put("phienBanCongBo", Math.max(1, lop.getPhienBanCongBo()));
        snapshot.put("phienBanBangDiem", lop.getPhienBanBangDiemHieuLuc());
        snapshot.put("daKhoa", trangThaiBangDiem.isDaKhoa());
        snapshot.put("choPhepSuaDiem", trangThaiBangDiem.isChoPhepSuaDiem());
        snapshot.put("soSinhVienDangHoc", soSinhVienDangHoc);
        snapshot.put("soSinhVienDuDieuKien", soSinhVienDuDieuKien);
        return snapshot;
    }

    private RegistrationDtos.TrangThaiBangDiemDto toTrangThaiBangDiemDto(CourseClass lop,
                                                                         int soSinhVienDangHoc,
                                                                         int soSinhVienDuDieuKien) {
        Subject mon = lop.getMonHoc();
        Semester ky = lop.getHocKy();
        GradeSheetStatus status = lop.getTrangThaiBangDiemHieuLuc();
        return new RegistrationDtos.TrangThaiBangDiemDto(
                lop.getId(),
                lop.getMa(),
                mon != null ? mon.getMa() : null,
                mon != null ? mon.getTen() : null,
                ky != null ? ky.getId() : null,
                ky != null ? ky.getTen() : null,
                ky != null ? ky.getNamHoc() : null,
                status.name(),
                status.getMoTa(),
                Math.max(1, lop.getPhienBanCongBo()),
                lop.getPhienBanBangDiemHieuLuc(),
                status.isChoPhepSuaDiem(),
                status.isDaCongBo(),
                status.isDaKhoa(),
                soSinhVienDangHoc,
                soSinhVienDuDieuKien,
                lop.getGuiDuyetBangDiemLuc(),
                lop.getNguoiGuiDuyetBangDiemId(),
                lop.getTraVeBangDiemLuc(),
                lop.getNguoiTraVeBangDiemId(),
                lop.getLyDoTraVeBangDiem(),
                lop.getDuyetBangDiemLuc(),
                lop.getNguoiDuyetBangDiemId(),
                lop.getCongBoBangDiemLuc(),
                lop.getNguoiCongBoBangDiemId(),
                lop.getKhoaBangDiemLuc(),
                lop.getNguoiKhoaBangDiemId(),
                lop.getMoKhoaBangDiemLuc(),
                lop.getNguoiMoKhoaBangDiemId(),
                lop.getLyDoMoKhoaBangDiem(),
                layLichSuChuyenTrangThai(lop.getId()));
    }

    private String chuanHoaTrangThaiKetQua(String trangThai) {
        if (trangThai == null || trangThai.isBlank()) {
            return null;
        }
        String normalized = trangThai.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized) || "TAT_CA".equals(normalized)) {
            return null;
        }
        if (!TRANG_THAI_KET_QUA_HOP_LE.contains(normalized)) {
            throw ApiException.badRequest("Trạng thái kết quả không hợp lệ. Chỉ chấp nhận: DAT, CHUA_DAT, CHUA_CO_DIEM");
        }
        return normalized;
    }

    private TongHop tinhTongHop(List<RegistrationDtos.DangKyDto> items) {
        int tongTinChi = 0;
        int tinChiCoDiem = 0;
        int tinChiTichLuy = 0;
        double tongDiem10 = 0;
        double tongDiem4 = 0;

        for (RegistrationDtos.DangKyDto item : items) {
            tongTinChi += item.soTinChi();
            if (item.diemTongKet() == null) {
                continue;
            }
            tinChiCoDiem += item.soTinChi();
            tongDiem10 += item.diemTongKet() * item.soTinChi();
            tongDiem4 += quyDoi4(item.diemTongKet()) * item.soTinChi();
            if (item.diemTongKet() >= AcademicRules.DIEM_DAT) {
                tinChiTichLuy += item.soTinChi();
            }
        }

        double gpa10 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem10 / tinChiCoDiem) * 100.0) / 100.0;
        double gpa4 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
        return new TongHop(tongTinChi, tinChiCoDiem, gpa10, gpa4, tinChiTichLuy);
    }

    private TongHop tinhTongHopLichSu(List<RegistrationDtos.MucLichSuHocTapDto> items) {
        int tongTinChi = 0;
        int tinChiCoDiem = 0;
        int tinChiTichLuy = 0;
        double tongDiem10 = 0;
        double tongDiem4 = 0;

        for (RegistrationDtos.MucLichSuHocTapDto item : items) {
            tongTinChi += item.soTinChi();
            if (item.diemTongKet() == null) {
                continue;
            }
            tinChiCoDiem += item.soTinChi();
            tongDiem10 += item.diemTongKet() * item.soTinChi();
            tongDiem4 += quyDoi4(item.diemTongKet()) * item.soTinChi();
            if (item.diemTongKet() >= AcademicRules.DIEM_DAT) {
                tinChiTichLuy += item.soTinChi();
            }
        }

        double gpa10 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem10 / tinChiCoDiem) * 100.0) / 100.0;
        double gpa4 = tinChiCoDiem == 0 ? 0 : Math.round((tongDiem4 / tinChiCoDiem) * 100.0) / 100.0;
        return new TongHop(tongTinChi, tinChiCoDiem, gpa10, gpa4, tinChiTichLuy);
    }

    public double quyDoiHe4(double diem10) {
        return quyDoi4(diem10);
    }

    private double quyDoi4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= AcademicRules.DIEM_DAT) return 1.5;
        if (diem10 >= 4.0) return 1.0;
        return 0.0;
    }

    Map<Long, Grade> layDiemTheoDangKy(List<CourseRegistration> danhSach) {
        if (danhSach.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = danhSach.stream().map(CourseRegistration::getId).toList();
        return diemRepository.findByDangKyIdIn(ids).stream()
                .collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));
    }

    private Student sinhVienHienTai() {
        return sinhVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));
    }

    private Lecturer giangVienHienTai() {
        return giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
    }

    private record TongHop(int tongTinChi, int tinChiCoDiem, double gpa10, double gpa4, int tinChiTichLuy) {
    }
}