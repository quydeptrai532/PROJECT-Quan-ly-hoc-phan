package com.edu.registration.service;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.RegistrationHistory;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Room;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.RegistrationHistoryRepository;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.repository.ClassAbsenceNoticeRepository;
import com.edu.registration.repository.RoomRepository;
import com.edu.registration.room.ClassFacts;
import com.edu.registration.room.PolicyMode;
import com.edu.registration.room.RoomAssignmentChecker;
import com.edu.registration.room.RoomCheckResult;
import com.edu.registration.room.RoomFacts;
import com.edu.registration.room.RoomIssue;
import com.edu.registration.room.RoomPolicy;
import com.edu.registration.room.RoomSlot;
import com.edu.registration.security.CurrentUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class CourseClassService {

    private final CourseClassRepository lopHocPhanRepository;
    private final SubjectRepository monHocRepository;
    private final LecturerRepository giangVienRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final RegistrationHistoryRepository lichSuDangKyRepository;
    private final ClassAbsenceNoticeRepository thongBaoNghiRepository;
    private final RoomRepository phongRepository;
    private final SemesterService hocKyService;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;
    private final NotificationService thongBaoService;

    // Chính sách kiểm tra phòng học.
    @Value("${app.room-policy.hoat-dong:BAT_BUOC}")
    private PolicyMode policyHoatDong = PolicyMode.BAT_BUOC;

    @Value("${app.room-policy.trung-lich:BAT_BUOC}")
    private PolicyMode policyTrungLich = PolicyMode.BAT_BUOC;

    @Value("${app.room-policy.suc-chua:CANH_BAO}")
    private PolicyMode policySucChua = PolicyMode.CANH_BAO;

    /*
     * Constructor cũ để tương thích với những nơi khởi tạo service
     * không truyền RegistrationHistoryRepository và RoomRepository.
     */
    public CourseClassService(
            CourseClassRepository lopHocPhanRepository,
            SubjectRepository monHocRepository,
            LecturerRepository giangVienRepository,
            CourseRegistrationRepository dangKyHocPhanRepository,
            ClassAbsenceNoticeRepository thongBaoNghiRepository,
            SemesterService hocKyService,
            AuditLogService auditLogService,
            CurrentUser currentUser,
            DtoMapper mapper,
            NotificationService thongBaoService
    ) {
        this(
                lopHocPhanRepository,
                monHocRepository,
                giangVienRepository,
                dangKyHocPhanRepository,
                null,
                null,
                thongBaoNghiRepository,
                hocKyService,
                auditLogService,
                currentUser,
                mapper,
                thongBaoService
        );
    }

    /*
     * Constructor tương thích với phiên bản cũ có RegistrationHistoryRepository.
     */
    public CourseClassService(
            CourseClassRepository lopHocPhanRepository,
            SubjectRepository monHocRepository,
            LecturerRepository giangVienRepository,
            CourseRegistrationRepository dangKyHocPhanRepository,
            RegistrationHistoryRepository lichSuDangKyRepository,
            ClassAbsenceNoticeRepository thongBaoNghiRepository,
            SemesterService hocKyService,
            AuditLogService auditLogService,
            CurrentUser currentUser,
            DtoMapper mapper,
            NotificationService thongBaoService
    ) {
        this(
                lopHocPhanRepository,
                monHocRepository,
                giangVienRepository,
                dangKyHocPhanRepository,
                lichSuDangKyRepository,
                null,
                thongBaoNghiRepository,
                hocKyService,
                auditLogService,
                currentUser,
                mapper,
                thongBaoService
        );
    }

    // Constructor Spring sử dụng để inject đầy đủ repository.
    @Autowired
    public CourseClassService(
            CourseClassRepository lopHocPhanRepository,
            SubjectRepository monHocRepository,
            LecturerRepository giangVienRepository,
            CourseRegistrationRepository dangKyHocPhanRepository,
            RegistrationHistoryRepository lichSuDangKyRepository,
            RoomRepository phongRepository,
            ClassAbsenceNoticeRepository thongBaoNghiRepository,
            SemesterService hocKyService,
            AuditLogService auditLogService,
            CurrentUser currentUser,
            DtoMapper mapper,
            NotificationService thongBaoService
    ) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.monHocRepository = monHocRepository;
        this.giangVienRepository = giangVienRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.lichSuDangKyRepository = lichSuDangKyRepository;
        this.phongRepository = phongRepository;
        this.thongBaoNghiRepository = thongBaoNghiRepository;
        this.hocKyService = hocKyService;
        this.auditLogService = auditLogService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.thongBaoService = thongBaoService;
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSach(Long hocKyId, String keyword) {
        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);

        List<CourseClass> danhSach =
                lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId());

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
    public List<CourseClassDtos.LopHocPhanDto> danhSachTheoGiangVien(
            Long giangVienId,
            Long hocKyId
    ) {
        if (hocKyId != null) {
            return lopHocPhanRepository
                    .findByGiangVienIdAndHocKyIdOrderByMaAsc(giangVienId, hocKyId)
                    .stream()
                    .map(mapper::lopHocPhan)
                    .toList();
        }

        return lopHocPhanRepository.findByGiangVienIdOrderByMaAsc(giangVienId)
                .stream()
                .map(mapper::lopHocPhan)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseClassDtos.LopHocPhanDto> danhSachCuaToi(Long hocKyId) {
        Lecturer giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() ->
                        ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));

        return danhSachTheoGiangVien(giangVien.getId(), hocKyId);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDtos.ThoiKhoaBieuDto> thoiKhoaBieuCuaToi(Long hocKyId) {
        Lecturer giangVien = giangVienRepository.findByUser(currentUser.get())
                .orElseThrow(() ->
                        ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));

        Semester hocKy = hocKyService.layHoacHienTai(hocKyId);

        List<CourseClass> danhSach = lopHocPhanRepository
                .findByGiangVienIdAndHocKyIdOrderByMaAsc(
                        giangVien.getId(),
                        hocKy.getId()
                );

        return danhSach.stream()
                .map(lop -> new RegistrationDtos.ThoiKhoaBieuDto(
                        lop.getId(),
                        lop.getMa(),
                        lop.getMonHoc().getTen(),
                        lop.getPhong(),
                        lop.getThu(),
                        lop.getTietBatDau(),
                        lop.getSoTiet(),
                        lop.getGiangVien().getHoTen()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseClassDtos.LopHocPhanDto chiTiet(Long id) {
        return mapper.lopHocPhan(
                lopHocPhanRepository.findWithDetailsById(id)
                        .orElseThrow(() ->
                                ApiException.notFound("Lớp học phần không tồn tại"))
        );
    }

    // Tạo lớp học phần.
    public CourseClassDtos.LopHocPhanResponse tao(
            CourseClassDtos.LopHocPhanRequest request
    ) {
        return auditLogService.execute(
                "COURSE_SECTION_CREATED",
                "COURSE_SECTION",
                null,
                audit -> {
                    kiemTraLich(request);

                    String ma = request.ma().trim();

                    if (lopHocPhanRepository.existsByMa(ma)) {
                        throw ApiException.conflict(
                                "Mã lớp học phần đã tồn tại"
                        );
                    }

                    Subject monHoc = monHocRepository.findById(request.monHocId())
                            .orElseThrow(() ->
                                    ApiException.notFound("Môn học không tồn tại"));

                    Lecturer giangVien = giangVienRepository.findById(request.giangVienId())
                            .orElseThrow(() ->
                                    ApiException.notFound("Giảng viên không tồn tại"));

                    Semester hocKy = hocKyService.lay(request.hocKyId());

                    // Lớp mới chưa có sinh viên đăng ký.
                    List<String> canhBao = new ArrayList<>(
                            kiemTraPhong(request, null, 0)
                    );

                    canhBao.addAll(kiemTraCanhBao(request, null));

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
                            canhBao
                    );
                }
        );
    }

    // Sửa lớp học phần.
    public CourseClassDtos.LopHocPhanResponse sua(
            Long id,
            CourseClassDtos.LopHocPhanRequest request
    ) {
        return auditLogService.execute(
                "COURSE_SECTION_UPDATED",
                "COURSE_SECTION",
                id,
                audit -> {
                    kiemTraLich(request);

                    CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                            .orElseThrow(() ->
                                    ApiException.notFound("Lớp học phần không tồn tại"));

                    Map<String, Object> before = auditSnapshot(lop);

                    String ma = request.ma().trim();

                    if (!lop.getMa().equalsIgnoreCase(ma)
                            && lopHocPhanRepository.existsByMa(ma)) {
                        throw ApiException.conflict(
                                "Mã lớp học phần đã tồn tại"
                        );
                    }

                    // Đếm số đăng ký thực tế còn hiệu lực.
                    long siSoDaDangKy =
                            dangKyHocPhanRepository.countByLopHocPhanIdAndTrangThai(
                                    id,
                                    RegistrationStatus.DA_DANG_KY
                            );

                    if (request.siSoToiDa() < siSoDaDangKy) {
                        throw ApiException.badRequest(
                                "Sĩ số tối đa không được nhỏ hơn số sinh viên đã đăng ký ("
                                        + siSoDaDangKy + ")"
                        );
                    }

                    Subject monHoc = monHocRepository.findById(request.monHocId())
                            .orElseThrow(() ->
                                    ApiException.notFound("Môn học không tồn tại"));

                    Lecturer giangVien = giangVienRepository.findById(request.giangVienId())
                            .orElseThrow(() ->
                                    ApiException.notFound("Giảng viên không tồn tại"));

                    Semester hocKy = hocKyService.lay(request.hocKyId());

                    List<String> canhBao = new ArrayList<>(
                            kiemTraPhong(
                                    request,
                                    id,
                                    Math.toIntExact(siSoDaDangKy)
                            )
                    );

                    canhBao.addAll(kiemTraCanhBao(request, id));

                    lop.setMa(ma);
                    lop.setMonHoc(monHoc);
                    lop.setHocKy(hocKy);
                    lop.setGiangVien(giangVien);

                    apDung(lop, request);

                    Map<String, Object> after = auditSnapshot(lop);

                    audit.changes(before, after);

                    thongBaoService.sectionChanged(
                            lop,
                            before,
                            after,
                            null
                    );

                    return new CourseClassDtos.LopHocPhanResponse(
                            mapper.lopHocPhan(lop),
                            canhBao
                    );
                }
        );
    }

    /**
     * Hủy lớp học phần, chuyển các đăng ký đang hoạt động sang trạng thái đã hủy,
     * ghi lịch sử đăng ký và gửi thông báo cho sinh viên bị ảnh hưởng.
     */
    public CourseClassDtos.HuyLopHocPhanResponse huyLopHocPhan(
            Long id,
            String lyDo
    ) {
        if (lyDo != null && lyDo.trim().length() > 200) {
            throw ApiException.badRequest(
                    "Lý do hủy lớp học phần tối đa 200 ký tự"
            );
        }

        String normalizedReason =
                (lyDo == null || lyDo.isBlank()) ? null : lyDo.trim();

        String historyReason = normalizedReason == null
                ? NotificationService.SECTION_CANCELLED_REASON_PREFIX
                : NotificationService.SECTION_CANCELLED_REASON_PREFIX
                + ": " + normalizedReason;

        return auditLogService.execute(
                "COURSE_SECTION_CANCELLED",
                "COURSE_SECTION",
                id,
                audit -> {
                    CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                            .orElseThrow(() ->
                                    ApiException.notFound("Lớp học phần không tồn tại"));

                    Map<String, Object> before = auditSnapshot(lop);

                    List<CourseRegistration> activeRegistrations =
                            dangKyHocPhanRepository.findByLopHocPhan(
                                    id,
                                    RegistrationStatus.DA_DANG_KY
                            );

                    LocalDateTime now = LocalDateTime.now();

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
                            event.setThoiGianDangKy(
                                    registration.getThoiGianDangKy()
                            );
                            event.setThoiDiem(now);
                            event.setThoiGianHuy(now);
                            event.setLyDoHuy(historyReason);

                            lichSuDangKyRepository.save(event);
                        }
                    }

                    lop.setSiSoHienTai(0);
                    lopHocPhanRepository.save(lop);

                    Map<String, Object> after =
                            new LinkedHashMap<>(auditSnapshot(lop));

                    after.put("trangThai", "CANCELLED");
                    after.put("soDangKyBiHuy", activeRegistrations.size());

                    if (normalizedReason != null) {
                        after.put("lyDo", normalizedReason);
                    }

                    audit.changes(before, after);

                    thongBaoService.sectionCancelled(
                            lop,
                            activeRegistrations,
                            now,
                            normalizedReason
                    );

                    return new CourseClassDtos.HuyLopHocPhanResponse(
                            lop.getId(),
                            lop.getMa(),
                            activeRegistrations.size(),
                            normalizedReason
                    );
                }
        );
    }

    // Xóa lớp học phần.
    public void xoa(Long id) {
        auditLogService.execute(
                "COURSE_SECTION_DELETED",
                "COURSE_SECTION",
                id,
                audit -> {
                    CourseClass lop = lopHocPhanRepository.findByIdForUpdate(id)
                            .orElseThrow(() ->
                                    ApiException.notFound("Lớp học phần không tồn tại"));

                    audit.changes(
                            auditSnapshot(lop),
                            Map.of("trangThai", "DELETED")
                    );

                    if (dangKyHocPhanRepository.existsByLopHocPhanId(id)) {
                        throw ApiException.conflict(
                                "Lớp đã có sinh viên đăng ký, không thể xóa"
                        );
                    }

                    thongBaoNghiRepository.deleteByLopHocPhanId(id);
                    lopHocPhanRepository.delete(lop);

                    return null;
                }
        );
    }

    // Kiểm tra phòng học trước khi tạo hoặc sửa lớp.
    private List<String> kiemTraPhong(
            CourseClassDtos.LopHocPhanRequest request,
            Long boQuaId,
            int siSoDangKy
    ) {
        // Trường hợp khởi tạo service bằng constructor cũ trong test.
        if (phongRepository == null) {
            return List.of();
        }

        String maPhong = request.phong().trim();

        /*
         * Khóa phòng trong transaction để hạn chế hai yêu cầu
         * đồng thời cùng xếp lịch vào một phòng.
         */
        Room phong = phongRepository.findByMaForUpdate(maPhong)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Không tìm thấy phòng học: " + maPhong
                        )
                );

        // Lấy lịch của các lớp khác trong cùng học kỳ và cùng phòng.
        List<RoomSlot> cacLichKhac = lopHocPhanRepository
                .findByHocKyIdOrderByMaAsc(request.hocKyId())
                .stream()
                .filter(lop -> boQuaId == null || !boQuaId.equals(lop.getId()))
                .filter(lop -> lop.getPhong() != null
                        && lop.getPhong().trim().equalsIgnoreCase(maPhong))
                .map(lop -> new RoomSlot(
                        lop.getId(),
                        lop.getMa(),
                        lop.getThu(),
                        lop.getTietBatDau(),
                        lop.getSoTiet()
                ))
                .toList();

        RoomFacts roomFacts = new RoomFacts(
                phong.isHoatDong(),
                phong.getSucChua(),
                phong.getLoai(),
                phong.getThietBi() == null
                        ? Set.of()
                        : phong.getThietBi()
        );

        /*
         * Dùng số sinh viên đã đăng ký thực tế để kiểm tra sức chứa.
         * Hiện DTO chưa có thông tin loại phòng và thiết bị yêu cầu,
         * nên hai tiêu chí này tạm thời không được áp dụng.
         */
        ClassFacts classFacts = new ClassFacts(
                siSoDangKy,
                phong.getLoai(),
                Set.of(),
                request.thu(),
                request.tietBatDau(),
                request.soTiet()
        );

        RoomPolicy policy = new RoomPolicy(
                policyHoatDong,
                policyTrungLich,
                policySucChua,
                PolicyMode.TAT,
                PolicyMode.TAT
        );

        RoomCheckResult result = RoomAssignmentChecker.kiemTra(
                roomFacts,
                classFacts,
                cacLichKhac,
                policy
        );

        if (!result.hopLe()) {
            String message = String.join(
                    "; ",
                    result.loi().stream()
                            .map(RoomIssue::message)
                            .toList()
            );

            boolean trungLich = result.loi().stream()
                    .anyMatch(issue ->
                            "TRUNG_LICH_PHONG".equals(issue.code())
                    );

            if (trungLich) {
                throw ApiException.conflict(message);
            }

            throw ApiException.badRequest(message);
        }

        return result.canhBao().stream()
                .map(RoomIssue::message)
                .toList();
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
                Map.entry("conLai", lop.getConLai())
        );
    }

    private void kiemTraLich(CourseClassDtos.LopHocPhanRequest request) {
        if (request.tietBatDau() + request.soTiet() - 1 > 12) {
            throw ApiException.badRequest("Lịch học vượt quá tiết 12");
        }
    }

    // Kiểm tra trùng lịch giảng viên.
    // Kiểm tra trùng phòng đã chuyển sang RoomAssignmentChecker.
    private List<String> kiemTraCanhBao(
            CourseClassDtos.LopHocPhanRequest request,
            Long boQuaId
    ) {
        CourseClass nhap = new CourseClass();

        nhap.setThu(request.thu());
        nhap.setTietBatDau(request.tietBatDau());
        nhap.setSoTiet(request.soTiet());

        List<String> canhBao = new ArrayList<>();

        for (CourseClass lop :
                lopHocPhanRepository.findByHocKyIdOrderByMaAsc(request.hocKyId())) {

            if (boQuaId != null && boQuaId.equals(lop.getId())) {
                continue;
            }

            if (!lop.trungLichVoi(nhap)) {
                continue;
            }

            if (lop.getGiangVien().getId().equals(request.giangVienId())) {
                canhBao.add(
                        "Giảng viên đã có lớp "
                                + lop.getMa()
                                + " trong cùng khung giờ"
                );
            }
        }

        return canhBao;
    }

    private void apDung(
            CourseClass lop,
            CourseClassDtos.LopHocPhanRequest request
    ) {
        lop.setPhong(request.phong().trim());
        lop.setThu(request.thu());
        lop.setTietBatDau(request.tietBatDau());
        lop.setSoTiet(request.soTiet());
        lop.setSiSoToiDa(request.siSoToiDa());
    }
}
