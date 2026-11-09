package com.edu.registration.service;

import com.edu.registration.config.GradeAppealProperties;
import com.edu.registration.dto.GradeAppealDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.GradeAppeal;
import com.edu.registration.entity.GradeAppealHistory;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.GradeAppealHistoryRepository;
import com.edu.registration.repository.GradeAppealRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

@Service
@Transactional
public class GradeAppealService {

    private static final Logger log = LoggerFactory.getLogger(GradeAppealService.class);
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Set<String> LOAI_DIEM_HOP_LE = Set.of("DIEM_QUA_TRINH", "DIEM_CUOI_KY", "CA_HAI");

    private final GradeAppealRepository appealRepository;
    private final GradeAppealHistoryRepository historyRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final StudentRepository studentRepository;
    private final CurrentUser currentUser;
    private final GradeService gradeService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final GradeAppealProperties properties;

    public GradeAppealService(GradeAppealRepository appealRepository,
                              GradeAppealHistoryRepository historyRepository,
                              CourseRegistrationRepository registrationRepository,
                              GradeRepository gradeRepository,
                              StudentRepository studentRepository,
                              CurrentUser currentUser,
                              GradeService gradeService,
                              AuditLogService auditLogService,
                              NotificationService notificationService,
                              GradeAppealProperties properties) {
        this.appealRepository = appealRepository;
        this.historyRepository = historyRepository;
        this.registrationRepository = registrationRepository;
        this.gradeRepository = gradeRepository;
        this.studentRepository = studentRepository;
        this.currentUser = currentUser;
        this.gradeService = gradeService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    public GradeAppealProperties getProperties() {
        return properties;
    }

    // ==========================================
    // DÀNH CHO SINH VIÊN
    // ==========================================

    @Transactional(readOnly = true)
    public GradeAppealDtos.DanhMucPhucKhaoDto danhMuc() {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được truy cập danh mục điểm phúc khảo");
        }
        Student sinhVien = studentRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        List<CourseRegistration> activeRegistrations =
                registrationRepository.findAllActiveOfStudent(sinhVien.getId(), RegistrationStatus.DA_DANG_KY);

        LocalDateTime now = LocalDateTime.now();
        List<GradeAppealDtos.DiemKhaDungPhucKhaoDto> danhSachDiem = new ArrayList<>();

        for (CourseRegistration reg : activeRegistrations) {
            CourseClass lop = reg.getLopHocPhan();
            if (lop == null || !lop.isBangDiemDaCongBo()) {
                continue; // Sinh viên chỉ thấy điểm sau công bố
            }
            Grade diem = gradeRepository.findByDangKyId(reg.getId()).orElse(null);
            if (diem == null || diem.getDiemTongKet() == null) {
                continue;
            }

            LocalDateTime congBoLuc = lop.getCongBoBangDiemLuc() != null
                    ? lop.getCongBoBangDiemLuc()
                    : lop.getKhoaBangDiemLuc();
            LocalDateTime hanNop = tinhHanNop(congBoLuc);
            boolean quaHanNop = hanNop != null && now.isAfter(hanNop);

            List<GradeAppeal> activeAppeals = appealRepository.findActiveByDangKyId(
                    reg.getId(), List.of(GradeAppealStatus.MOI_TAO, GradeAppealStatus.DANG_XU_LY));
            boolean daCoDonDangXuLy = !activeAppeals.isEmpty();

            Subject mon = lop.getMonHoc();
            danhSachDiem.add(new GradeAppealDtos.DiemKhaDungPhucKhaoDto(
                    reg.getId(),
                    diem.getId(),
                    lop.getId(),
                    lop.getMa(),
                    mon != null ? mon.getId() : null,
                    mon != null ? mon.getMa() : null,
                    mon != null ? mon.getTen() : null,
                    mon != null ? mon.getSoTinChi() : null,
                    lop.getHocKy() != null ? lop.getHocKy().getId() : null,
                    lop.getHocKy() != null ? lop.getHocKy().getTen() : null,
                    lop.getHocKy() != null ? lop.getHocKy().getNamHoc() : null,
                    diem.getDiemQuaTrinh(),
                    diem.getDiemCuoiKy(),
                    diem.getDiemTongKet(),
                    congBoLuc,
                    hanNop,
                    quaHanNop,
                    daCoDonDangXuLy
            ));
        }

        return new GradeAppealDtos.DanhMucPhucKhaoDto(
                danhSachDiem,
                properties.getSubmissionWindowDays(),
                properties.isOfficialRegulation(),
                properties.getSlaNote()
        );
    }

    @Transactional(readOnly = true)
    public List<GradeAppealDtos.HoSoPhucKhaoDto> danhSachCuaSinhVien() {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được xem danh sách hồ sơ của mình");
        }
        Student sinhVien = studentRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        List<GradeAppeal> list = appealRepository.findBySinhVienIdOrderByCreatedAtDesc(sinhVien.getId());
        return list.stream().map(a -> toDto(a, false)).toList();
    }

    @Transactional(readOnly = true)
    public GradeAppealDtos.HoSoPhucKhaoDto chiTietCuaSinhVien(Long id) {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được xem chi tiết hồ sơ");
        }
        Student sinhVien = studentRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        GradeAppeal appeal = appealRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Hồ sơ phúc khảo không tồn tại"));

        if (!appeal.getSinhVien().getId().equals(sinhVien.getId())) {
            throw ApiException.forbidden("Bạn không có quyền truy cập hồ sơ phúc khảo của sinh viên khác");
        }
        return toDto(appeal, true);
    }

    public GradeAppealDtos.HoSoPhucKhaoDto taoHoSo(GradeAppealDtos.TaoHoSoPhucKhaoRequest request) {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được nộp hồ sơ phúc khảo điểm");
        }
        Student sinhVien = studentRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        if (request == null || request.dangKyId() == null) {
            throw ApiException.badRequest("Mã đăng ký học phần không được để trống");
        }

        CourseRegistration dangKy = registrationRepository.findById(request.dangKyId())
                .orElseThrow(() -> ApiException.notFound("Đăng ký học phần không tồn tại"));

        if (dangKy.getSinhVien() == null || !dangKy.getSinhVien().getId().equals(sinhVien.getId())) {
            throw ApiException.forbidden("Bạn không được phép nộp hồ sơ phúc khảo cho kết quả của sinh viên khác");
        }
        if (dangKy.getTrangThai() != RegistrationStatus.DA_DANG_KY) {
            throw ApiException.badRequest("Học phần này đã bị hủy đăng ký, không thể nộp đơn phúc khảo");
        }

        CourseClass lop = dangKy.getLopHocPhan();
        if (lop == null) {
            throw ApiException.notFound("Lớp học phần liên quan không tồn tại");
        }

        if (properties.isRequirePublishedGrade() && !lop.isBangDiemDaCongBo()) {
            throw ApiException.badRequest("Bảng điểm của lớp học phần " + lop.getMa()
                    + " chưa được công bố chính thức. Sinh viên chỉ được nộp hồ sơ phúc khảo sau khi điểm đã công bố");
        }

        Grade diem = gradeRepository.findByDangKyId(dangKy.getId())
                .orElseThrow(() -> ApiException.notFound("Chưa có bản ghi điểm cho học phần này"));

        if (diem.getDiemTongKet() == null) {
            throw ApiException.badRequest("Học phần này chưa có điểm tổng kết hợp lệ để phúc khảo");
        }

        // Kiểm tra hạn nộp hồ sơ theo cấu hình tập trung
        LocalDateTime congBoLuc = lop.getCongBoBangDiemLuc() != null
                ? lop.getCongBoBangDiemLuc()
                : lop.getKhoaBangDiemLuc();
        LocalDateTime hanNop = tinhHanNop(congBoLuc);
        LocalDateTime now = LocalDateTime.now();
        if (hanNop != null && now.isAfter(hanNop)) {
            throw ApiException.badRequest("Đã quá thời hạn nộp hồ sơ phúc khảo cho môn học này (hạn nộp: "
                    + hanNop.format(TIME_FORMATTER) + "). Theo cấu hình hiện tại: " + properties.getSubmissionWindowDays() + " ngày kể từ ngày công bố điểm");
        }

        // Chặn trùng lặp hồ sơ đang xử lý
        List<GradeAppeal> activeAppeals = appealRepository.findActiveByDangKyId(
                dangKy.getId(), List.of(GradeAppealStatus.MOI_TAO, GradeAppealStatus.DANG_XU_LY));
        if (!activeAppeals.isEmpty()) {
            throw ApiException.conflict("Bạn đã có hồ sơ phúc khảo đang chờ hoặc đang được xử lý cho học phần này ("
                    + activeAppeals.get(0).getMaHoSo() + ")");
        }

        String loaiDiem = chuanHoaLoaiDiem(request.loaiDiem());
        String lyDo = validateRequiredText(request.lyDo(), "Lý do phúc khảo", 10, 2000);
        Double diemMongMuon = request.diemMongMuon();
        if (diemMongMuon != null && (diemMongMuon < 0.0 || diemMongMuon > 10.0)) {
            throw ApiException.badRequest("Điểm mong muốn phải từ 0.0 đến 10.0");
        }

        String maHoSo = sinhMaHoSoDuyNhat();

        GradeAppeal appeal = new GradeAppeal();
        appeal.setMaHoSo(maHoSo);
        appeal.setSinhVien(sinhVien);
        appeal.setLopHocPhan(lop);
        appeal.setMonHoc(lop.getMonHoc());
        appeal.setDangKy(dangKy);
        appeal.setDiem(diem);
        appeal.setDiemQuaTrinhGoc(diem.getDiemQuaTrinh());
        appeal.setDiemCuoiKyGoc(diem.getDiemCuoiKy());
        appeal.setDiemTongKetGoc(diem.getDiemTongKet());
        appeal.setLoaiDiem(loaiDiem);
        appeal.setDiemMongMuon(diemMongMuon);
        appeal.setLyDo(lyDo);
        appeal.setHanNop(hanNop);
        appeal.setTrangThai(GradeAppealStatus.MOI_TAO);
        appeal.setCreatedAt(now);
        appeal.setUpdatedAt(now);

        GradeAppeal daLuu = appealRepository.save(appeal);

        // Lưu vết lịch sử tạo mới
        ghiLichSu(daLuu, "TAO_MOI", null, GradeAppealStatus.MOI_TAO, actor, lyDo, null, null, null, now);

        // Ghi nhận Audit Log
        executeAudited("GRADE_APPEAL_CREATED", "GRADE_APPEAL", daLuu.getId(), audit -> {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("phucKhaoId", daLuu.getId());
            changes.put("maHoSo", daLuu.getMaHoSo());
            changes.put("sinhVienId", sinhVien.getId());
            changes.put("maSinhVien", sinhVien.getMa());
            changes.put("lopHocPhanId", lop.getId());
            changes.put("maLop", lop.getMa());
            changes.put("monHocId", lop.getMonHoc() != null ? lop.getMonHoc().getId() : null);
            changes.put("diemId", diem.getId());
            changes.put("diemQuaTrinhGoc", daLuu.getDiemQuaTrinhGoc());
            changes.put("diemCuoiKyGoc", daLuu.getDiemCuoiKyGoc());
            changes.put("diemTongKetGoc", daLuu.getDiemTongKetGoc());
            changes.put("loaiDiemPhucKhao", loaiDiem);
            changes.put("trangThaiMoi", GradeAppealStatus.MOI_TAO.name());
            if (hanNop != null) {
                changes.put("hanNop", hanNop.toString());
            }
            audit.changes(Map.of(), changes);
            return null;
        });

        log.info("Sinh viên {} ({}) đã nộp hồ sơ phúc khảo {} cho môn {} thuộc lớp {}",
                sinhVien.getHoTen(), sinhVien.getMa(), maHoSo, lop.getMonHoc().getTen(), lop.getMa());
        return toDto(daLuu, true);
    }

    public GradeAppealDtos.HoSoPhucKhaoDto huyHoSo(Long id, String lyDo) {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.SINH_VIEN) {
            throw ApiException.forbidden("Chỉ tài khoản sinh viên mới được hủy hồ sơ của mình");
        }
        Student sinhVien = studentRepository.findByUser(actor)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ sinh viên"));

        GradeAppeal appeal = appealRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Hồ sơ phúc khảo không tồn tại"));

        if (!appeal.getSinhVien().getId().equals(sinhVien.getId())) {
            throw ApiException.forbidden("Bạn không có quyền thao tác trên hồ sơ của sinh viên khác");
        }

        if (appeal.getTrangThai() != GradeAppealStatus.MOI_TAO) {
            throw ApiException.conflict("Hồ sơ đang ở trạng thái " + appeal.getTrangThai()
                    + ", chỉ có thể hủy khi hồ sơ mới tiếp nhận và chưa bắt đầu xử lý");
        }

        LocalDateTime now = LocalDateTime.now();
        String cleanLyDo = lyDo != null && !lyDo.isBlank() ? lyDo.trim() : "Sinh viên tự hủy hồ sơ";
        GradeAppealStatus trangThaiCu = appeal.getTrangThai();
        appeal.setTrangThai(GradeAppealStatus.DA_HUY);
        appeal.setUpdatedAt(now);
        GradeAppeal daLuu = appealRepository.save(appeal);

        ghiLichSu(daLuu, "HUY", trangThaiCu, GradeAppealStatus.DA_HUY, actor, cleanLyDo, null, null, null, now);

        executeAudited("GRADE_APPEAL_CANCELLED", "GRADE_APPEAL", daLuu.getId(), audit -> {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("phucKhaoId", daLuu.getId());
            changes.put("maHoSo", daLuu.getMaHoSo());
            changes.put("trangThaiCu", trangThaiCu.name());
            changes.put("trangThaiMoi", GradeAppealStatus.DA_HUY.name());
            changes.put("lyDo", cleanLyDo);
            audit.changes(Map.of("trangThai", trangThaiCu.name()), changes);
            return null;
        });

        log.info("Sinh viên {} đã hủy hồ sơ phúc khảo {}", sinhVien.getMa(), appeal.getMaHoSo());
        return toDto(daLuu, true);
    }

    // ==========================================
    // DÀNH CHO QUẢN TRỊ / CÁN BỘ KHẢO THÍ
    // ==========================================

    @Transactional(readOnly = true)
    public GradeAppealDtos.CauHinhPhucKhaoDto layCauHinh() {
        kiemTraQuyenQuanTri("Chỉ người có quyền quản trị mới được xem cấu hình phúc khảo");
        return new GradeAppealDtos.CauHinhPhucKhaoDto(
                properties.getSubmissionWindowDays(),
                properties.isRequirePublishedGrade(),
                properties.isAutoApplyGradeChange(),
                properties.isOfficialRegulation(),
                properties.getSlaNote()
        );
    }

    @Transactional(readOnly = true)
    public List<GradeAppealDtos.HoSoPhucKhaoDto> danhSachQuanTri(Long hocKyId,
                                                                 Long lopHocPhanId,
                                                                 Long monHocId,
                                                                 String maSinhVien,
                                                                 GradeAppealStatus trangThai,
                                                                 String keyword) {
        kiemTraQuyenQuanTri("Chỉ người có quyền quản trị mới được truy cập danh sách hồ sơ phúc khảo");
        List<GradeAppeal> list = appealRepository.findWithFilters(hocKyId, lopHocPhanId, monHocId, trangThai,
                maSinhVien != null && !maSinhVien.isBlank() ? maSinhVien.trim() : null);

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase(Locale.ROOT);
            list = list.stream()
                    .filter(a -> a.getMaHoSo().toLowerCase(Locale.ROOT).contains(kw)
                            || (a.getSinhVien().getMa() != null && a.getSinhVien().getMa().toLowerCase(Locale.ROOT).contains(kw))
                            || (a.getSinhVien().getHoTen() != null && a.getSinhVien().getHoTen().toLowerCase(Locale.ROOT).contains(kw))
                            || (a.getLopHocPhan().getMa() != null && a.getLopHocPhan().getMa().toLowerCase(Locale.ROOT).contains(kw))
                            || (a.getMonHoc().getTen() != null && a.getMonHoc().getTen().toLowerCase(Locale.ROOT).contains(kw)))
                    .toList();
        }

        return list.stream().map(a -> toDto(a, false)).toList();
    }

    @Transactional(readOnly = true)
    public GradeAppealDtos.HoSoPhucKhaoDto chiTietQuanTri(Long id) {
        kiemTraQuyenQuanTri("Chỉ người có quyền quản trị mới được xem chi tiết hồ sơ phúc khảo");
        GradeAppeal appeal = appealRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Hồ sơ phúc khảo không tồn tại"));
        return toDto(appeal, true);
    }

    public GradeAppealDtos.HoSoPhucKhaoDto tiepNhan(Long id, String ghiChu) {
        User actor = kiemTraQuyenQuanTri("Chỉ người có quyền quản trị mới được tiếp nhận xử lý hồ sơ phúc khảo");
        GradeAppeal appeal = appealRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Hồ sơ phúc khảo không tồn tại"));

        if (appeal.getTrangThai() != GradeAppealStatus.MOI_TAO) {
            throw ApiException.conflict("Hồ sơ đang ở trạng thái " + appeal.getTrangThai()
                    + ", chỉ hồ sơ ở trạng thái mới tiếp nhận (MOI_TAO) mới có thể chuyển sang đang xử lý");
        }

        LocalDateTime now = LocalDateTime.now();
        String cleanGhiChu = ghiChu != null && !ghiChu.isBlank() ? ghiChu.trim() : "Tiếp nhận hồ sơ chuyển chấm lại";
        GradeAppealStatus trangThaiCu = appeal.getTrangThai();
        appeal.setTrangThai(GradeAppealStatus.DANG_XU_LY);
        appeal.setNguoiXuLy(actor);
        appeal.setThoiDiemXuLy(now);
        appeal.setUpdatedAt(now);
        GradeAppeal daLuu = appealRepository.save(appeal);

        ghiLichSu(daLuu, "TIEP_NHAN", trangThaiCu, GradeAppealStatus.DANG_XU_LY, actor, cleanGhiChu, null, null, null, now);

        executeAudited("GRADE_APPEAL_IN_PROGRESS", "GRADE_APPEAL", daLuu.getId(), audit -> {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("phucKhaoId", daLuu.getId());
            changes.put("maHoSo", daLuu.getMaHoSo());
            changes.put("trangThaiCu", trangThaiCu.name());
            changes.put("trangThaiMoi", GradeAppealStatus.DANG_XU_LY.name());
            changes.put("ghiChuXuLy", cleanGhiChu);
            audit.changes(Map.of("trangThai", trangThaiCu.name()), changes);
            return null;
        });

        log.info("Quản trị viên {} đã tiếp nhận hồ sơ phúc khảo {}", actor.getUsername(), appeal.getMaHoSo());
        return toDto(daLuu, true);
    }

    public GradeAppealDtos.HoSoPhucKhaoDto xuLy(Long id, GradeAppealDtos.XuLyPhucKhaoRequest request) {
        User actor = kiemTraQuyenQuanTri("Chỉ người có quyền quản trị mới được xử lý hồ sơ phúc khảo");
        if (request == null) {
            throw ApiException.badRequest("Dữ liệu xử lý phúc khảo không hợp lệ");
        }

        GradeAppeal appeal = appealRepository.findWithDetailsById(id)
                .orElseThrow(() -> ApiException.notFound("Hồ sơ phúc khảo không tồn tại"));

        if (appeal.getTrangThai() != GradeAppealStatus.MOI_TAO && appeal.getTrangThai() != GradeAppealStatus.DANG_XU_LY) {
            throw ApiException.conflict("Hồ sơ đang ở trạng thái " + appeal.getTrangThai()
                    + ", chỉ có thể xử lý khi đang ở trạng thái MOI_TAO hoặc DANG_XU_LY");
        }

        GradeAppealStatus targetStatus = GradeAppealStatus.fromCode(request.trangThai())
                .orElseThrow(() -> ApiException.badRequest("Trạng thái xử lý không hợp lệ. Chỉ chấp nhận CHAP_THUAN hoặc TU_CHOI"));

        if (targetStatus != GradeAppealStatus.CHAP_THUAN && targetStatus != GradeAppealStatus.TU_CHOI) {
            throw ApiException.badRequest("Trạng thái kết quả chỉ có thể là Chấp thuận (CHAP_THUAN) hoặc Từ chối (TU_CHOI)");
        }

        String ketQua = validateRequiredText(request.ketQuaXuLy(), "Kết quả/Lý do xử lý", 5, 2000);
        LocalDateTime now = LocalDateTime.now();
        GradeAppealStatus trangThaiCu = appeal.getTrangThai();

        Double diemQtMoi = null;
        Double diemCkMoi = null;
        Double diemTkMoi = null;

        if (targetStatus == GradeAppealStatus.TU_CHOI) {
            // Từ chối / giữ nguyên điểm: không sửa điểm gốc
            appeal.setTrangThai(GradeAppealStatus.TU_CHOI);
            appeal.setNguoiXuLy(actor);
            appeal.setThoiDiemXuLy(now);
            appeal.setKetQuaXuLy(ketQua);
            appeal.setUpdatedAt(now);
            GradeAppeal daLuu = appealRepository.save(appeal);

            ghiLichSu(daLuu, "TU_CHOI", trangThaiCu, GradeAppealStatus.TU_CHOI, actor, ketQua, null, null, null, now);

            executeAudited("GRADE_APPEAL_REJECTED", "GRADE_APPEAL", daLuu.getId(), audit -> {
                Map<String, Object> changes = new LinkedHashMap<>();
                changes.put("phucKhaoId", daLuu.getId());
                changes.put("maHoSo", daLuu.getMaHoSo());
                changes.put("trangThaiCu", trangThaiCu.name());
                changes.put("trangThaiMoi", GradeAppealStatus.TU_CHOI.name());
                changes.put("ketQuaXuLy", ketQua);
                audit.changes(Map.of("trangThai", trangThaiCu.name()), changes);
                return null;
            });

            // Gửi thông báo tới sinh viên sau commit
            if (notificationService != null && appeal.getSinhVien() != null && appeal.getSinhVien().getUser() != null) {
                notificationService.gradeAppealProcessed(
                        appeal.getSinhVien().getUser(),
                        appeal.getMaHoSo(),
                        appeal.getMonHoc().getTen(),
                        "TU_CHOI",
                        ketQua,
                        now
                );
            }

            log.info("Đã từ chối/giữ nguyên điểm cho hồ sơ phúc khảo {} với lý do: {}", appeal.getMaHoSo(), ketQua);
            return toDto(daLuu, true);
        }

        // Trường hợp CHAP_THUAN:
        // Xác thực điểm mới nếu có thay đổi
        diemQtMoi = request.diemQuaTrinhMoi() != null ? request.diemQuaTrinhMoi() : appeal.getDiemQuaTrinhGoc();
        diemCkMoi = request.diemCuoiKyMoi() != null ? request.diemCuoiKyMoi() : appeal.getDiemCuoiKyGoc();

        if (request.diemQuaTrinhMoi() != null || request.diemCuoiKyMoi() != null) {
            diemTkMoi = GradeService.tinhDiemTongKet(diemQtMoi, diemCkMoi);
        } else {
            diemTkMoi = appeal.getDiemTongKetGoc();
        }

        // Cập nhật điểm gốc thông qua luồng điều chỉnh có audit bắt buộc (không sửa trực tiếp bản ghi khóa)
        if (properties.isAutoApplyGradeChange() && appeal.getDiem() != null && appeal.getDiem().getId() != null) {
            gradeService.dieuChinhDiemPhucKhao(
                    appeal.getDiem().getId(),
                    appeal.getId(),
                    diemQtMoi,
                    diemCkMoi,
                    "Điều chỉnh điểm theo kết quả hồ sơ phúc khảo " + appeal.getMaHoSo() + ": " + ketQua,
                    actor
            );
        }

        appeal.setTrangThai(GradeAppealStatus.CHAP_THUAN);
        appeal.setNguoiXuLy(actor);
        appeal.setThoiDiemXuLy(now);
        appeal.setKetQuaXuLy(ketQua);
        appeal.setDiemQuaTrinhMoi(diemQtMoi);
        appeal.setDiemCuoiKyMoi(diemCkMoi);
        appeal.setDiemTongKetMoi(diemTkMoi);
        appeal.setUpdatedAt(now);
        GradeAppeal daLuu = appealRepository.save(appeal);

        ghiLichSu(daLuu, "CHAP_THUAN", trangThaiCu, GradeAppealStatus.CHAP_THUAN, actor, ketQua, diemQtMoi, diemCkMoi, diemTkMoi, now);

        final Double finalQt = diemQtMoi;
        final Double finalCk = diemCkMoi;
        final Double finalTk = diemTkMoi;
        executeAudited("GRADE_APPEAL_APPROVED", "GRADE_APPEAL", daLuu.getId(), audit -> {
            Map<String, Object> changes = new LinkedHashMap<>();
            changes.put("phucKhaoId", daLuu.getId());
            changes.put("maHoSo", daLuu.getMaHoSo());
            changes.put("trangThaiCu", trangThaiCu.name());
            changes.put("trangThaiMoi", GradeAppealStatus.CHAP_THUAN.name());
            changes.put("ketQuaXuLy", ketQua);
            changes.put("diemQuaTrinhGoc", daLuu.getDiemQuaTrinhGoc());
            changes.put("diemCuoiKyGoc", daLuu.getDiemCuoiKyGoc());
            changes.put("diemTongKetGoc", daLuu.getDiemTongKetGoc());
            changes.put("diemQuaTrinhMoi", finalQt);
            changes.put("diemCuoiKyMoi", finalCk);
            changes.put("diemTongKetMoi", finalTk);
            audit.changes(Map.of("trangThai", trangThaiCu.name()), changes);
            return null;
        });

        // Gửi thông báo tới sinh viên sau commit
        if (notificationService != null && appeal.getSinhVien() != null && appeal.getSinhVien().getUser() != null) {
            notificationService.gradeAppealProcessed(
                    appeal.getSinhVien().getUser(),
                    appeal.getMaHoSo(),
                    appeal.getMonHoc().getTen(),
                    "CHAP_THUAN",
                    ketQua,
                    now
            );
        }

        log.info("Đã chấp thuận điều chỉnh điểm cho hồ sơ phúc khảo {} (Điểm mới: QT={}, CK={}, TK={})",
                appeal.getMaHoSo(), diemQtMoi, diemCkMoi, diemTkMoi);
        return toDto(daLuu, true);
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH NỘI BỘ
    // ==========================================

    private LocalDateTime tinhHanNop(LocalDateTime congBoLuc) {
        if (congBoLuc == null || properties.getSubmissionWindowDays() <= 0) {
            return null;
        }
        return congBoLuc.plusDays(properties.getSubmissionWindowDays());
    }

    private User kiemTraQuyenQuanTri(String message) {
        User actor = currentUser.get();
        if (actor == null || actor.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden(message);
        }
        return actor;
    }

    private String chuanHoaLoaiDiem(String input) {
        if (input == null || input.isBlank()) {
            return "DIEM_CUOI_KY";
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT);
        if (!LOAI_DIEM_HOP_LE.contains(normalized)) {
            throw ApiException.badRequest("Loại điểm phúc khảo không hợp lệ. Chỉ chấp nhận: DIEM_QUA_TRINH, DIEM_CUOI_KY, CA_HAI");
        }
        return normalized;
    }

    private String validateRequiredText(String text, String fieldName, int min, int max) {
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest(fieldName + " không được để trống");
        }
        String clean = text.trim();
        if (clean.length() < min || clean.length() > max) {
            throw ApiException.badRequest(fieldName + " phải từ " + min + " đến " + max + " ký tự");
        }
        return clean;
    }

    private String sinhMaHoSoDuyNhat() {
        String prefix = "PK-" + LocalDateTime.now().format(CODE_DATE_FORMATTER) + "-";
        for (int i = 0; i < 10; i++) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
            String candidate = prefix + suffix;
            if (appealRepository.findByMaHoSo(candidate).isEmpty()) {
                return candidate;
            }
        }
        return prefix + System.currentTimeMillis() % 1000000;
    }

    private void ghiLichSu(GradeAppeal hoSo, String hanhDong,
                           GradeAppealStatus trangThaiTruoc, GradeAppealStatus trangThaiSau,
                           User actor, String lyDo,
                           Double qtMoi, Double ckMoi, Double tkMoi, LocalDateTime thoiDiem) {
        GradeAppealHistory history = new GradeAppealHistory();
        history.setHoSo(hoSo);
        history.setHanhDong(hanhDong);
        history.setTrangThaiTruoc(trangThaiTruoc);
        history.setTrangThaiSau(trangThaiSau);
        history.setNguoiThucHien(actor);
        if (actor != null) {
            history.setNguoiThucHienUsername(actor.getUsername());
            history.setNguoiThucHienRole(actor.getRole());
            history.setNguoiThucHienHoTen(layHoTenActor(actor));
        }
        history.setLyDo(lyDo);
        history.setDiemQuaTrinhMoi(qtMoi);
        history.setDiemCuoiKyMoi(ckMoi);
        history.setDiemTongKetMoi(tkMoi);
        history.setThoiDiem(thoiDiem != null ? thoiDiem : LocalDateTime.now());
        historyRepository.save(history);
    }

    private String layHoTenActor(User actor) {
        if (actor == null) {
            return null;
        }
        return actor.getUsername();
    }

    private GradeAppealDtos.HoSoPhucKhaoDto toDto(GradeAppeal appeal, boolean includeHistory) {
        List<GradeAppealDtos.LichSuPhucKhaoDto> lichSu = Collections.emptyList();
        if (includeHistory) {
            lichSu = historyRepository.findByHoSoIdOrderByThoiDiemAsc(appeal.getId()).stream()
                    .map(h -> new GradeAppealDtos.LichSuPhucKhaoDto(
                            h.getId(),
                            h.getHanhDong(),
                            h.getTrangThaiTruoc() != null ? h.getTrangThaiTruoc().name() : null,
                            h.getTrangThaiSau().name(),
                            h.getNguoiThucHien() != null ? h.getNguoiThucHien().getId() : null,
                            h.getNguoiThucHienUsername(),
                            h.getNguoiThucHienHoTen(),
                            h.getNguoiThucHienRole() != null ? h.getNguoiThucHienRole().name() : null,
                            h.getLyDo(),
                            h.getDiemQuaTrinhMoi(),
                            h.getDiemCuoiKyMoi(),
                            h.getDiemTongKetMoi(),
                            h.getThoiDiem()
                    ))
                    .toList();
        }

        LocalDateTime now = LocalDateTime.now();
        boolean quaHanNop = appeal.getHanNop() != null && now.isAfter(appeal.getHanNop());

        return new GradeAppealDtos.HoSoPhucKhaoDto(
                appeal.getId(),
                appeal.getMaHoSo(),
                appeal.getSinhVien() != null ? appeal.getSinhVien().getId() : null,
                appeal.getSinhVien() != null ? appeal.getSinhVien().getMa() : null,
                appeal.getSinhVien() != null ? appeal.getSinhVien().getHoTen() : null,
                appeal.getLopHocPhan() != null ? appeal.getLopHocPhan().getId() : null,
                appeal.getLopHocPhan() != null ? appeal.getLopHocPhan().getMa() : null,
                appeal.getMonHoc() != null ? appeal.getMonHoc().getId() : null,
                appeal.getMonHoc() != null ? appeal.getMonHoc().getMa() : null,
                appeal.getMonHoc() != null ? appeal.getMonHoc().getTen() : null,
                appeal.getMonHoc() != null ? appeal.getMonHoc().getSoTinChi() : null,
                appeal.getLopHocPhan() != null && appeal.getLopHocPhan().getHocKy() != null ? appeal.getLopHocPhan().getHocKy().getId() : null,
                appeal.getLopHocPhan() != null && appeal.getLopHocPhan().getHocKy() != null ? appeal.getLopHocPhan().getHocKy().getTen() : null,
                appeal.getLopHocPhan() != null && appeal.getLopHocPhan().getHocKy() != null ? appeal.getLopHocPhan().getHocKy().getNamHoc() : null,
                appeal.getDangKy() != null ? appeal.getDangKy().getId() : null,
                appeal.getDiem() != null ? appeal.getDiem().getId() : null,
                appeal.getDiemQuaTrinhGoc(),
                appeal.getDiemCuoiKyGoc(),
                appeal.getDiemTongKetGoc(),
                appeal.getLoaiDiem(),
                appeal.getDiemMongMuon(),
                appeal.getLyDo(),
                appeal.getHanNop(),
                quaHanNop,
                appeal.getTrangThai().name(),
                appeal.getTrangThai().getMoTa(),
                appeal.getNguoiXuLy() != null ? appeal.getNguoiXuLy().getId() : null,
                appeal.getNguoiXuLy() != null ? appeal.getNguoiXuLy().getUsername() : null,
                appeal.getNguoiXuLy() != null ? layHoTenActor(appeal.getNguoiXuLy()) : null,
                appeal.getThoiDiemXuLy(),
                appeal.getKetQuaXuLy(),
                appeal.getDiemQuaTrinhMoi(),
                appeal.getDiemCuoiKyMoi(),
                appeal.getDiemTongKetMoi(),
                appeal.getCreatedAt(),
                appeal.getUpdatedAt(),
                lichSu
        );
    }

    private <T> T executeAudited(String action, String entityType, Object entityId,
                                 Function<AuditLogService.AuditContext, T> operation) {
        if (auditLogService == null) {
            return operation.apply(new AuditLogService.AuditContext(entityId == null ? null : String.valueOf(entityId)));
        }
        return auditLogService.execute(action, entityType, entityId, operation);
    }
}
