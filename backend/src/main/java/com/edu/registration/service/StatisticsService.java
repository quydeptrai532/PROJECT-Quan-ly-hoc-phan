package com.edu.registration.service;

import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Curriculum;
import com.edu.registration.entity.CurriculumGroup;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Grade;
import com.edu.registration.entity.Major;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.CurriculumRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.GradeRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import com.edu.registration.util.CsvUtils;
import com.edu.registration.util.ExportSecuritySupport;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    public static final int SAP_DAY_PERCENT = 90;
    public static final int IT_DANG_KY_PERCENT = 25;
    public static final int SAP_DONG_TRONG_NGAY = 7;
    public static final double NGUONG_DIEM_DAT = AcademicRules.DIEM_DAT;

    private final StudentRepository sinhVienRepository;
    private final LecturerRepository giangVienRepository;
    private final SubjectRepository monHocRepository;
    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final GradeRepository diemRepository;
    private final SemesterService hocKyService;
    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final CurriculumRepository chuongTrinhRepository;
    private final CurrentUser currentUser;
    private final AuditLogService auditLogService;

    public StatisticsService(StudentRepository sinhVienRepository,
                             LecturerRepository giangVienRepository,
                             SubjectRepository monHocRepository,
                             CourseClassRepository lopHocPhanRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             GradeRepository diemRepository,
                             SemesterService hocKyService,
                             FacultyRepository khoaRepository,
                             MajorRepository nganhRepository,
                             CurriculumRepository chuongTrinhRepository,
                             CurrentUser currentUser) {
        this(sinhVienRepository, giangVienRepository, monHocRepository,
                lopHocPhanRepository, dangKyHocPhanRepository, diemRepository,
                hocKyService, khoaRepository, nganhRepository, chuongTrinhRepository,
                currentUser, null);
    }

    @Autowired
    public StatisticsService(StudentRepository sinhVienRepository,
                             LecturerRepository giangVienRepository,
                             SubjectRepository monHocRepository,
                             CourseClassRepository lopHocPhanRepository,
                             CourseRegistrationRepository dangKyHocPhanRepository,
                             GradeRepository diemRepository,
                             SemesterService hocKyService,
                             FacultyRepository khoaRepository,
                             MajorRepository nganhRepository,
                             CurriculumRepository chuongTrinhRepository,
                             CurrentUser currentUser,
                             @Autowired(required = false) AuditLogService auditLogService) {
        this.sinhVienRepository = sinhVienRepository;
        this.giangVienRepository = giangVienRepository;
        this.monHocRepository = monHocRepository;
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.diemRepository = diemRepository;
        this.hocKyService = hocKyService;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.chuongTrinhRepository = chuongTrinhRepository;
        this.currentUser = currentUser;
        this.auditLogService = auditLogService;
    }

    public StatDtos.TongQuanDto tongQuan() {
        Semester hocKy = hocKyService.hocKyHienTai();
        return new StatDtos.TongQuanDto(
                sinhVienRepository.count(),
                giangVienRepository.count(),
                monHocRepository.count(),
                lopHocPhanRepository.count(),
                dangKyHocPhanRepository.countByTrangThai(RegistrationStatus.DA_DANG_KY),
                hocKy != null ? hocKy.getTen() + " - " + hocKy.getNamHoc() : "Chưa có học kỳ");
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId) {
        return theoMon(hocKyId, null, null);
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId, Long khoaId) {
        return theoMon(hocKyId, khoaId, null);
    }

    public List<StatDtos.ThongKeMonDto> theoMon(Long hocKyId, Long khoaId, Long nganhId) {
        ResolvedFilters filters = validateAndResolveFilters(hocKyId, khoaId, nganhId, null, null);
        List<CourseClass> danhSachLop = layDanhSachLopTheoBoLoc(filters);

        Map<Subject, List<CourseClass>> theoMon = danhSachLop.stream()
                .collect(Collectors.groupingBy(CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));

        List<StatDtos.ThongKeMonDto> ketQua = new ArrayList<>();
        theoMon.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getMa()))
                .forEach(entry -> {
                    Subject monHoc = entry.getKey();
                    List<CourseClass> lopCuaMon = entry.getValue();
                    int tongChiTieu = lopCuaMon.stream().mapToInt(CourseClass::getSiSoToiDa).sum();
                    int daDangKy = lopCuaMon.stream().mapToInt(CourseClass::getSiSoHienTai).sum();
                    double tiLe = tongChiTieu == 0 ? 0
                            : Math.round((daDangKy * 1000.0 / tongChiTieu)) / 10.0;
                    ketQua.add(new StatDtos.ThongKeMonDto(monHoc.getId(), monHoc.getMa(), monHoc.getTen(),
                            lopCuaMon.size(), tongChiTieu, daDangKy, tiLe));
                });
        return ketQua;
    }

    public StatDtos.HanhDongHocVuDto hanhDong(Long hocKyId, Long khoaId) {
        Semester hocKy = hocKyId == null ? hocKyService.hocKyHienTai() : hocKyService.lay(hocKyId);
        if (hocKy == null) {
            return new StatDtos.HanhDongHocVuDto(null, null, khoaId, null, LocalDateTime.now(), List.of());
        }
        String tenKhoa = khoaId == null ? null : khoaRepository.findById(khoaId)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")).getTen();
        List<CourseClass> lopHocPhans = khoaId == null
                ? lopHocPhanRepository.findByHocKyIdOrderByMaAsc(hocKy.getId())
                : lopHocPhanRepository.findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(hocKy.getId(), khoaId);
        Map<Long, Long> siSoThucTe = lopHocPhanRepository.countDangKyHienHanhTheoLop(
                        hocKy.getId(), khoaId, RegistrationStatus.DA_DANG_KY).stream()
                .collect(Collectors.toMap(CourseClassRepository.DangKyTheoLop::getLopHocPhanId,
                        CourseClassRepository.DangKyTheoLop::getSoDangKy));

        List<StatDtos.CanhBaoHocVuDto> canhBao = new ArrayList<>();
        Map<Subject, List<CourseClass>> theoMon = lopHocPhans.stream().collect(Collectors.groupingBy(
                CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));
        for (CourseClass lop : lopHocPhans) {
            long daDangKy = siSoThucTe.getOrDefault(lop.getId(), 0L);
            int chiTieu = lop.getSiSoToiDa();
            double tiLe = chiTieu == 0 ? 0 : daDangKy * 100.0 / chiTieu;
            if (tiLe >= SAP_DAY_PERCENT) {
                canhBao.add(canhBao("LOP_GAN_DAY", "Lớp gần đầy",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng từ " + SAP_DAY_PERCENT + "%.",
                        lop, daDangKy, chiTieu, tiLe, hocKy));
            } else if (tiLe <= IT_DANG_KY_PERCENT) {
                canhBao.add(canhBao("LOP_IT_DANG_KY", "Lớp ít đăng ký",
                        daDangKy + "/" + chiTieu + " chỗ đã đăng ký; ngưỡng không quá "
                                + IT_DANG_KY_PERCENT + "%.", lop, daDangKy, chiTieu, tiLe, hocKy));
            }
        }
        for (Map.Entry<Subject, List<CourseClass>> entry : theoMon.entrySet()) {
            List<CourseClass> sections = entry.getValue();
            long daDangKy = sections.stream().mapToLong(lop -> siSoThucTe.getOrDefault(lop.getId(), 0L)).sum();
            long tongChiTieu = sections.stream().mapToLong(CourseClass::getSiSoToiDa).sum();
            if (tongChiTieu > 0 && daDangKy >= tongChiTieu) {
                Subject mon = entry.getKey();
                double tiLe = daDangKy * 100.0 / tongChiTieu;
                canhBao.add(new StatDtos.CanhBaoHocVuDto("MON_HET_CHO", "Nhu cầu môn đạt chỉ tiêu",
                        daDangKy + "/" + tongChiTieu + " chỗ đã đăng ký trên " + sections.size()
                                + " lớp; đăng ký đang hiệu lực đạt hoặc vượt tổng chỉ tiêu.",
                        "Tổng đăng ký đang hiệu lực của các lớp môn / tổng chỉ tiêu các lớp môn >= 100%.",
                        null, null, mon.getId(), mon.getMa(), mon.getTen(), sections.size(), daDangKy,
                        tongChiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                        hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                                + "&keyword=" + mon.getMa()));
            }
        }
        LocalDate homNay = LocalDate.now();
        long soNgayConLai = java.time.temporal.ChronoUnit.DAYS.between(homNay, hocKy.getDangKyDen());
        if (hocKy.dangMoDangKy() && soNgayConLai >= 0 && soNgayConLai <= SAP_DONG_TRONG_NGAY) {
            canhBao.add(new StatDtos.CanhBaoHocVuDto("DOT_DANG_KY_SAP_DONG", "Đợt đăng ký sắp đóng",
                    "Còn " + soNgayConLai + " ngày; hạn đăng ký " + hocKy.getDangKyDen() + ".",
                    "Đợt đang mở và ngày kết thúc nằm trong " + SAP_DONG_TRONG_NGAY + " ngày tới.",
                    null, null, null, null, null, lopHocPhans.size(), 0, 0, 0,
                    hocKy.getId(), tenHocKy(hocKy), hocKy.getDangKyDen(),
                    "/quan-tri/hoc-ky"));
        }
        return new StatDtos.HanhDongHocVuDto(hocKy.getId(), tenHocKy(hocKy), khoaId, tenKhoa,
                LocalDateTime.now(), canhBao);
    }

    private StatDtos.CanhBaoHocVuDto canhBao(String loai, String tieuDe, String lyDo, CourseClass lop,
                                              long daDangKy, long chiTieu, double tiLe, Semester hocKy) {
        return new StatDtos.CanhBaoHocVuDto(loai, tieuDe, lyDo,
                "Sĩ số đăng ký đang hiệu lực / chỉ tiêu lớp; " + ("LOP_GAN_DAY".equals(loai)
                        ? "ngưỡng >= " + SAP_DAY_PERCENT + "%" : "ngưỡng <= " + IT_DANG_KY_PERCENT + "%"),
                lop.getId(), lop.getMa(), lop.getMonHoc().getId(), lop.getMonHoc().getMa(), lop.getMonHoc().getTen(),
                1, daDangKy, chiTieu, Math.round(tiLe * 10) / 10.0, hocKy.getId(), tenHocKy(hocKy),
                hocKy.getDangKyDen(), "/quan-tri/lop-hoc-phan?hocKyId=" + hocKy.getId()
                        + "&keyword=" + java.net.URLEncoder.encode(lop.getMa(), StandardCharsets.UTF_8));
    }

    private String tenHocKy(Semester hocKy) {
        return hocKy != null ? hocKy.getTen() + " - " + hocKy.getNamHoc() : "Tất cả học kỳ";
    }

    /**
     * Thống kê KPI tổng hợp toàn diện cho Admin Dashboard, biểu đồ và export.
     * Áp dụng Single Source of Truth cho toàn bộ các chỉ số:
     * - Số đăng ký
     * - Tỷ lệ lấp đầy
     * - Đạt / Trượt
     * - Điểm trung bình & GPA thang 10 / thang 4
     * - Tiến độ học tập theo CTĐT
     */
    public StatDtos.DashboardKpiResponseDto layDashboardKpi(Long hocKyId, Long khoaId, Long nganhId,
                                                            Long monHocId, Long lopHocPhanId) {
        ResolvedFilters filters = validateAndResolveFilters(hocKyId, khoaId, nganhId, monHocId, lopHocPhanId);
        List<CourseClass> danhSachLop = layDanhSachLopTheoBoLoc(filters);

        int tongSoLop = danhSachLop.size();
        long tongChiTieu = danhSachLop.stream().mapToLong(CourseClass::getSiSoToiDa).sum();
        long tongDangKy = danhSachLop.stream().mapToLong(CourseClass::getSiSoHienTai).sum();
        double tiLeLapDay = tongChiTieu == 0 ? 0.0
                : Math.round((tongDangKy * 1000.0 / tongChiTieu)) / 10.0;
        long conTrong = Math.max(0, tongChiTieu - tongDangKy);

        // Chỉ xét điểm của các lớp có bảng điểm ĐÃ CÔNG BỐ (DA_CONG_BO hoặc DA_KHOA)
        List<CourseClass> lopDaCongBo = danhSachLop.stream()
                .filter(CourseClass::isBangDiemDaCongBo)
                .toList();
        List<Long> lopDaCongBoIds = lopDaCongBo.stream().map(CourseClass::getId).toList();

        List<CourseRegistrationRepository.DiemTheoLopProjection> diemList = lopDaCongBoIds.isEmpty()
                ? List.of()
                : dangKyHocPhanRepository.findDiemTheoDanhSachLop(lopDaCongBoIds, RegistrationStatus.DA_DANG_KY);

        // Mẫu số hợp lệ M: có diemTongKet trong [0.0, 10.0]
        List<CourseRegistrationRepository.DiemTheoLopProjection> diemHopLe = diemList.stream()
                .filter(d -> d.getDiemTongKet() != null && d.getDiemTongKet() >= 0.0 && d.getDiemTongKet() <= 10.0)
                .toList();

        long soSinhVienCoDiem = diemHopLe.size();
        long soSinhVienDat = diemHopLe.stream().filter(d -> d.getDiemTongKet() >= NGUONG_DIEM_DAT).count();
        long soSinhVienTruot = diemHopLe.stream().filter(d -> d.getDiemTongKet() < NGUONG_DIEM_DAT).count();
        long soSinhVienChuaCoDiem = tongDangKy - soSinhVienCoDiem;

        double tyLeDat = soSinhVienCoDiem == 0 ? 0.0
                : BigDecimal.valueOf(soSinhVienDat * 100.0)
                .divide(BigDecimal.valueOf(soSinhVienCoDiem), 2, RoundingMode.HALF_UP).doubleValue();
        double tyLeTruot = soSinhVienCoDiem == 0 ? 0.0
                : BigDecimal.valueOf(soSinhVienTruot * 100.0)
                .divide(BigDecimal.valueOf(soSinhVienCoDiem), 2, RoundingMode.HALF_UP).doubleValue();

        Double diemTrungBinh = null;
        Double gpa10Tb = null;
        Double gpa4Tb = null;

        Map<Long, CourseClass> lopById = danhSachLop.stream().collect(Collectors.toMap(CourseClass::getId, Function.identity(), (a, b) -> a));

        if (soSinhVienCoDiem > 0) {
            double tongDiem = diemHopLe.stream().mapToDouble(CourseRegistrationRepository.DiemTheoLopProjection::getDiemTongKet).sum();
            diemTrungBinh = BigDecimal.valueOf(tongDiem)
                    .divide(BigDecimal.valueOf(soSinhVienCoDiem), 2, RoundingMode.HALF_UP).doubleValue();

            // Tính GPA theo tín chỉ
            long tongTinChiCoDiem = 0;
            double tongDiemTinChi10 = 0.0;
            double tongDiemTinChi4 = 0.0;
            for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
                CourseClass lop = lopById.get(d.getLopHocPhanId());
                int stc = (lop != null && lop.getMonHoc() != null) ? lop.getMonHoc().getSoTinChi() : 3;
                tongTinChiCoDiem += stc;
                tongDiemTinChi10 += d.getDiemTongKet() * stc;
                tongDiemTinChi4 += quyDoiHe4(d.getDiemTongKet()) * stc;
            }
            if (tongTinChiCoDiem > 0) {
                gpa10Tb = BigDecimal.valueOf(tongDiemTinChi10)
                        .divide(BigDecimal.valueOf(tongTinChiCoDiem), 2, RoundingMode.HALF_UP).doubleValue();
                gpa4Tb = BigDecimal.valueOf(tongDiemTinChi4)
                        .divide(BigDecimal.valueOf(tongTinChiCoDiem), 2, RoundingMode.HALF_UP).doubleValue();
            }
        }

        // 5 Khoảng phân bố điểm
        int countKem = 0; // [0.0, 4.0)
        int countYeu = 0; // [4.0, 5.0)
        int countTb = 0;  // [5.0, 6.5)
        int countKha = 0; // [6.5, 8.0)
        int countGioi = 0;// [8.0, 10.0]

        for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
            double x = d.getDiemTongKet();
            if (x < 4.0) countKem++;
            else if (x < 5.0) countYeu++;
            else if (x < 6.5) countTb++;
            else if (x < 8.0) countKha++;
            else countGioi++;
        }

        List<StatDtos.KhoaKhoangDiemDto> phanBoDiem = List.of(
                new StatDtos.KhoaKhoangDiemDto("KEM", "[0.0, 4.0)", "Dưới 4.0 (Kém)", countKem, calcPercent(countKem, soSinhVienCoDiem), false),
                new StatDtos.KhoaKhoangDiemDto("YEU", "[4.0, 5.0)", "4.0 đến dưới 5.0 (Yếu)", countYeu, calcPercent(countYeu, soSinhVienCoDiem), false),
                new StatDtos.KhoaKhoangDiemDto("TRUNG_BINH", "[5.0, 6.5)", "5.0 đến dưới 6.5 (Trung bình)", countTb, calcPercent(countTb, soSinhVienCoDiem), true),
                new StatDtos.KhoaKhoangDiemDto("KHA", "[6.5, 8.0)", "6.5 đến dưới 8.0 (Khá)", countKha, calcPercent(countKha, soSinhVienCoDiem), true),
                new StatDtos.KhoaKhoangDiemDto("GIOI_XUAT_SAC", "[8.0, 10.0]", "8.0 đến 10.0 (Giỏi - Xuất sắc)", countGioi, calcPercent(countGioi, soSinhVienCoDiem), true)
        );

        // Phân bố xếp loại GPA hệ 4
        int gpaXuatSac = 0;  // >= 3.6
        int gpaGioi = 0;     // 3.2 - 3.59
        int gpaKha = 0;      // 2.5 - 3.19
        int gpaTrungBinh = 0;// 2.0 - 2.49
        int gpaYeuKem = 0;   // < 2.0

        for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
            double d4 = quyDoiHe4(d.getDiemTongKet());
            if (d4 >= 3.6) gpaXuatSac++;
            else if (d4 >= 3.2) gpaGioi++;
            else if (d4 >= 2.5) gpaKha++;
            else if (d4 >= 2.0) gpaTrungBinh++;
            else gpaYeuKem++;
        }

        List<StatDtos.XepLoaiGpaDto> phanBoGpa = List.of(
                new StatDtos.XepLoaiGpaDto("XUAT_SAC", "Xuất sắc", "[3.6, 4.0]", gpaXuatSac, calcPercent(gpaXuatSac, soSinhVienCoDiem)),
                new StatDtos.XepLoaiGpaDto("GIOI", "Giỏi", "[3.2, 3.6)", gpaGioi, calcPercent(gpaGioi, soSinhVienCoDiem)),
                new StatDtos.XepLoaiGpaDto("KHA", "Khá", "[2.5, 3.2)", gpaKha, calcPercent(gpaKha, soSinhVienCoDiem)),
                new StatDtos.XepLoaiGpaDto("TRUNG_BINH", "Trung bình", "[2.0, 2.5)", gpaTrungBinh, calcPercent(gpaTrungBinh, soSinhVienCoDiem)),
                new StatDtos.XepLoaiGpaDto("YEU_KEM", "Yếu / Kém", "[0.0, 2.0)", gpaYeuKem, calcPercent(gpaYeuKem, soSinhVienCoDiem))
        );

        // Tiến độ học tập theo chương trình đào tạo của sinh viên trong phạm vi lọc
        List<Student> sinhViens = sinhVienRepository.findByKhoaIdAndNganhIdForAnalytics(filters.khoaId(), filters.nganhId());
        long tongSinhVienXetTienDo = sinhViens.size();
        long soSinhVienChuaGanChuongTrinh = sinhViens.stream().filter(sv -> sv.getChuongTrinh() == null).count();

        Double tyLeHoanThanhTinChiTb = null;
        List<Student> svCoChuongTrinh = sinhViens.stream().filter(sv -> sv.getChuongTrinh() != null).toList();
        if (!svCoChuongTrinh.isEmpty()) {
            double tongTyLe = 0;
            int countHopLe = 0;
            for (Student sv : svCoChuongTrinh) {
                Curriculum ct = sv.getChuongTrinh();
                int required = ct.getTongTinChiYeuCau();
                if (required > 0) {
                    List<Long> monDaDatIds = diemRepository.findMonDaDat(sv.getId(), NGUONG_DIEM_DAT);
                    Set<Long> monDaDatSet = new HashSet<>(monDaDatIds);
                    int earned = 0;
                    for (CurriculumGroup group : ct.getNhom()) {
                        int groupEarned = 0;
                        for (Subject s : group.getMonHoc()) {
                            if (monDaDatSet.contains(s.getId())) {
                                groupEarned += s.getSoTinChi();
                            }
                        }
                        earned += Math.min(group.getTinChiYeuCau(), groupEarned);
                    }
                    double pt = Math.min(100.0, (earned * 100.0) / required);
                    tongTyLe += pt;
                    countHopLe++;
                }
            }
            if (countHopLe > 0) {
                tyLeHoanThanhTinChiTb = BigDecimal.valueOf(tongTyLe / countHopLe)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
        }

        // Thống kê chi tiết từng môn học
        Map<Subject, List<CourseClass>> lopTheoMon = danhSachLop.stream()
                .collect(Collectors.groupingBy(CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));

        Map<Long, List<CourseRegistrationRepository.DiemTheoLopProjection>> diemTheoLopId = diemHopLe.stream()
                .collect(Collectors.groupingBy(CourseRegistrationRepository.DiemTheoLopProjection::getLopHocPhanId));

        List<StatDtos.ThongKeMonChiTietDto> danhSachMon = new ArrayList<>();
        lopTheoMon.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getMa()))
                .forEach(entry -> {
                    Subject mon = entry.getKey();
                    List<CourseClass> lops = entry.getValue();
                    int soLop = lops.size();
                    int chiTieuMon = lops.stream().mapToInt(CourseClass::getSiSoToiDa).sum();
                    int daDangKyMon = lops.stream().mapToInt(CourseClass::getSiSoHienTai).sum();
                    double tiLeLapDayMon = chiTieuMon == 0 ? 0.0
                            : Math.round((daDangKyMon * 1000.0 / chiTieuMon)) / 10.0;

                    List<CourseRegistrationRepository.DiemTheoLopProjection> diemCuaMon = lops.stream()
                            .filter(CourseClass::isBangDiemDaCongBo)
                            .flatMap(l -> diemTheoLopId.getOrDefault(l.getId(), List.of()).stream())
                            .toList();

                    int soCoDiemMon = diemCuaMon.size();
                    int soDatMon = (int) diemCuaMon.stream().filter(d -> d.getDiemTongKet() >= NGUONG_DIEM_DAT).count();
                    int soTruotMon = (int) diemCuaMon.stream().filter(d -> d.getDiemTongKet() < NGUONG_DIEM_DAT).count();
                    int soChuaCoDiemMon = daDangKyMon - soCoDiemMon;

                    double tyLeDatMon = soCoDiemMon == 0 ? 0.0
                            : calcPercent(soDatMon, soCoDiemMon);
                    double tyLeTruotMon = soCoDiemMon == 0 ? 0.0
                            : calcPercent(soTruotMon, soCoDiemMon);

                    Double diemTbMon = null;
                    Double gpa10Mon = null;
                    Double gpa4Mon = null;
                    if (soCoDiemMon > 0) {
                        double sumDiem = diemCuaMon.stream().mapToDouble(CourseRegistrationRepository.DiemTheoLopProjection::getDiemTongKet).sum();
                        diemTbMon = BigDecimal.valueOf(sumDiem / soCoDiemMon).setScale(2, RoundingMode.HALF_UP).doubleValue();
                        gpa10Mon = diemTbMon;
                        double sum4 = diemCuaMon.stream().mapToDouble(d -> quyDoiHe4(d.getDiemTongKet())).sum();
                        gpa4Mon = BigDecimal.valueOf(sum4 / soCoDiemMon).setScale(2, RoundingMode.HALF_UP).doubleValue();
                    }

                    danhSachMon.add(new StatDtos.ThongKeMonChiTietDto(
                            mon.getId(), mon.getMa(), mon.getTen(), mon.getSoTinChi(),
                            soLop, chiTieuMon, daDangKyMon, tiLeLapDayMon,
                            soCoDiemMon, diemTbMon, soDatMon, tyLeDatMon, soTruotMon, tyLeTruotMon,
                            soChuaCoDiemMon, gpa10Mon, gpa4Mon
                    ));
                });

        StatDtos.KpiTongHopDto kpi = new StatDtos.KpiTongHopDto(
                tongSoLop, tongChiTieu, tongDangKy, tiLeLapDay, conTrong,
                soSinhVienCoDiem, diemTrungBinh, soSinhVienDat, tyLeDat, soSinhVienTruot, tyLeTruot,
                soSinhVienChuaCoDiem, gpa10Tb, gpa4Tb, tyLeHoanThanhTinChiTb,
                tongSinhVienXetTienDo, soSinhVienChuaGanChuongTrinh
        );

        StatDtos.BoLocThongKeDto boLocDto = new StatDtos.BoLocThongKeDto(
                filters.hocKy() != null ? filters.hocKy().getId() : null,
                filters.hocKy() != null ? filters.hocKy().getTen() + " - " + filters.hocKy().getNamHoc() : "Tất cả học kỳ",
                filters.khoa() != null ? filters.khoa().getId() : null,
                filters.khoa() != null ? filters.khoa().getTen() : "Tất cả khoa",
                filters.nganh() != null ? filters.nganh().getId() : null,
                filters.nganh() != null ? filters.nganh().getTen() : "Tất cả ngành",
                filters.monHoc() != null ? filters.monHoc().getId() : null,
                filters.monHoc() != null ? filters.monHoc().getTen() : "Tất cả môn",
                filters.lopHocPhan() != null ? filters.lopHocPhan().getId() : null,
                filters.lopHocPhan() != null ? filters.lopHocPhan().getMa() : "Tất cả lớp",
                "DA_CONG_BO",
                LocalDateTime.now()
        );

        return new StatDtos.DashboardKpiResponseDto(boLocDto, kpi, phanBoDiem, phanBoGpa, danhSachMon);
    }

    /**
     * Xuất file CSV báo cáo thống kê chính xác theo bộ lọc đang xem.
     * Áp dụng UTF-8 BOM, tiêu đề cột đầy đủ, metadata thời điểm tạo,
     * Zero Student PII.
     */
    public byte[] xuatBaoCaoThongKeCsv(Long hocKyId, Long khoaId, Long nganhId,
                                       Long monHocId, Long lopHocPhanId) {
        StatDtos.DashboardKpiResponseDto report = layDashboardKpi(hocKyId, khoaId, nganhId, monHocId, lopHocPhanId);
        ExportSecuritySupport.validateRowCount(report.danhSachMon().size(), "báo cáo thống kê đào tạo");

        return auditOrExecute("ADMIN_STATISTICS_EXPORTED", "DASHBOARD_KPI", hocKyId != null ? hocKyId : 0L, audit -> {
            StatDtos.BoLocThongKeDto b = report.boLoc();
            StatDtos.KpiTongHopDto k = report.kpi();

            User actor = currentUser != null ? currentUser.get() : null;
            String actorName = actor != null ? (actor.getUsername() + " (" + actor.getRole() + ")") : "QUAN_TRI";
            String timeStr = b.thoiDiemTruyVan().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            audit.changes(Map.of(), Map.of(
                    "hocKyId", hocKyId != null ? hocKyId : "TAT_CA",
                    "khoaId", khoaId != null ? khoaId : "TAT_CA",
                    "nganhId", nganhId != null ? nganhId : "TAT_CA",
                    "monHocId", monHocId != null ? monHocId : "TAT_CA",
                    "lopHocPhanId", lopHocPhanId != null ? lopHocPhanId : "TAT_CA",
                    "soLuongMon", report.danhSachMon().size()
            ));

            StringBuilder csv = new StringBuilder("\uFEFF");
            csv.append("# =====================================================================\n");
            csv.append("# BÁO CÁO THỐNG KÊ VÀ DASHBOARD ĐÀO TẠO (QLSV)\n");
            csv.append("# Thời điểm tạo: ").append(timeStr).append(" (GMT+7)\n");
        csv.append("# Người tạo: ").append(actorName).append("\n");
        csv.append("# Học kỳ áp dụng: ").append(escapeCsv(b.tenHocKy())).append("\n");
        csv.append("# Khoa áp dụng: ").append(escapeCsv(b.tenKhoa())).append("\n");
        csv.append("# Ngành áp dụng: ").append(escapeCsv(b.tenNganh())).append("\n");
        csv.append("# Môn học áp dụng: ").append(escapeCsv(b.tenMonHoc())).append("\n");
        csv.append("# Lớp học phần áp dụng: ").append(escapeCsv(b.maLopHocPhan())).append("\n");
        csv.append("# Phạm vi dữ liệu điểm: ĐÃ CÔNG BỐ (Chính thức)\n");
        csv.append("# Nguyên tắc bảo mật: Không chứa thông tin định danh cá nhân sinh viên (Zero PII)\n");
        csv.append("# =====================================================================\n\n");

        // KHỐI 1: TỔNG QUAN KPI
        csv.append("# KHỐI 1: TỔNG QUAN CÁC CHỈ SỐ KPI HỌC VỤ\n");
        csv.append("Tên chỉ số,Tử số,Mẫu số,Giá trị,Đơn vị,Ghi chú xử lý thiếu dữ liệu\n");
        csv.append("Lượt đăng ký hiệu lực,").append(k.tongDangKy()).append(",-,").append(k.tongDangKy()).append(",Lượt,Loại bỏ các đăng ký DA_HUY\n");
        csv.append("Tỷ lệ lấp đầy,").append(k.tongDangKy()).append(",").append(k.tongChiTieu()).append(",").append(k.tiLeLapDay()).append(",%,Tổng đăng ký / Tổng chỉ tiêu\n");
        csv.append("Tỷ lệ Đạt (>= 5.0),").append(k.soSinhVienDat()).append(",").append(k.soSinhVienCoDiem()).append(",").append(k.tyLeDat()).append(",%,Chỉ tính lớp đã công bố bảng điểm\n");
        csv.append("Tỷ lệ Trượt (< 5.0),").append(k.soSinhVienTruot()).append(",").append(k.soSinhVienCoDiem()).append(",").append(k.tyLeTruot()).append(",%,Chỉ tính lớp đã công bố bảng điểm\n");
        csv.append("Điểm trung bình (Thang 10),-,").append(k.soSinhVienCoDiem()).append(",").append(k.diemTrungBinh() != null ? k.diemTrungBinh() : "N/A").append(",Điểm,Loại bỏ điểm chưa công bố\n");
        csv.append("GPA trung bình (Thang 4),-,-,").append(k.gpa4TrungBinh() != null ? k.gpa4TrungBinh() : "N/A").append(",Điểm,Quy đổi thang 4 theo tín chỉ\n");
        csv.append("Tiến độ tích lũy TB,-,-,").append(k.tyLeHoanThanhTinChiTb() != null ? (k.tyLeHoanThanhTinChiTb() + "%") : "N/A").append(",%,Tính theo CTĐT của ngành/khóa\n");
        csv.append("Số SV chưa có điểm công bố,-,-,").append(k.soSinhVienChuaCoDiem()).append(",Sinh viên,Lớp chưa công bố hoặc chưa nhập đủ điểm\n\n");

        // KHỐI 2: CHI TIẾT THEO MÔN HỌC
        csv.append("# KHỐI 2: THỐNG KÊ CHI TIẾT THEO MÔN HỌC VÀ LỚP HỌC PHẦN\n");
        csv.append("STT,Mã môn,Tên môn,Số tín chỉ,Số lớp,Chỉ tiêu,Đã đăng ký,Tỷ lệ lấp đầy (%),Số SV có điểm công bố,Điểm TB (10),GPA (4),Số Đạt,Tỷ lệ Đạt (%),Số Trượt,Tỷ lệ Trượt (%),Chưa có điểm\n");
        int stt = 1;
        for (StatDtos.ThongKeMonChiTietDto m : report.danhSachMon()) {
            csv.append(stt++).append(',')
                    .append(escapeCsv(m.maMon())).append(',')
                    .append(escapeCsv(m.tenMon())).append(',')
                    .append(m.soTinChi()).append(',')
                    .append(m.soLop()).append(',')
                    .append(m.tongChiTieu()).append(',')
                    .append(m.daDangKy()).append(',')
                    .append(m.tiLeLapDay()).append(',')
                    .append(m.soSinhVienCoDiem()).append(',')
                    .append(m.diemTrungBinh() != null ? m.diemTrungBinh() : "").append(',')
                    .append(m.gpa4() != null ? m.gpa4() : "").append(',')
                    .append(m.soSinhVienDat()).append(',')
                    .append(m.tyLeDat()).append(',')
                    .append(m.soSinhVienTruot()).append(',')
                    .append(m.tyLeTruot()).append(',')
                    .append(m.soSinhVienChuaCoDiem()).append('\n');
        }
        csv.append('\n');

        // KHỐI 3: PHÂN BỐ KHOẢNG ĐIỂM
        csv.append("# KHỐI 3: PHÂN BỐ KHOẢNG ĐIỂM CỦA CÁC LỚP ĐÃ CÔNG BỐ\n");
        csv.append("Mã khoảng,Khoảng điểm toán học,Mô tả,Số lượng,Tỷ lệ (%),Nhóm Đạt\n");
        for (StatDtos.KhoaKhoangDiemDto d : report.phanBoDiem()) {
            csv.append(escapeCsv(d.maKhoang())).append(',')
                    .append(escapeCsv(d.kyHieuKhoang())).append(',')
                    .append(escapeCsv(d.tenKhoang())).append(',')
                    .append(d.soLuong()).append(',')
                    .append(d.tyLePhanTram()).append(',')
                    .append(d.laKhoangDat() ? "ĐẠT" : "CHƯA ĐẠT").append('\n');
        }
        csv.append('\n');

        // KHỐI 4: PHÂN BỐ XẾP LOẠI GPA
        csv.append("# KHỐI 4: PHÂN BỐ XẾP LOẠI HỌC LỰC GPA (HỆ 4)\n");
        csv.append("Mã xếp loại,Xếp loại,Khoảng GPA hệ 4,Số lượng,Tỷ lệ (%)\n");
        for (StatDtos.XepLoaiGpaDto g : report.phanBoGpa()) {
            csv.append(escapeCsv(g.maLoai())).append(',')
                    .append(escapeCsv(g.tenLoai())).append(',')
                    .append(escapeCsv(g.kyHieuKhoang())).append(',')
                    .append(g.soLuong()).append(',')
                    .append(g.tyLePhanTram()).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
        });
    }

    public byte[] xuatDanhSachLop(Long lopHocPhanId) {
        return xuatDanhSachLopChiTiet(lopHocPhanId, null, null).noiDung();
    }

    public ClassExportResult xuatDanhSachLopChiTiet(Long lopHocPhanId, String tuKhoa, String sapXep) {
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
        List<CourseRegistration> danhSach =
                dangKyHocPhanRepository.findByLopHocPhan(lopHocPhanId, RegistrationStatus.DA_DANG_KY);

        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            String q = tuKhoa.trim().toLowerCase(Locale.ROOT);
            danhSach = danhSach.stream()
                    .filter(dk -> dk.getSinhVien() != null && (
                            (dk.getSinhVien().getMa() != null && dk.getSinhVien().getMa().toLowerCase(Locale.ROOT).contains(q)) ||
                            (dk.getSinhVien().getHoTen() != null && dk.getSinhVien().getHoTen().toLowerCase(Locale.ROOT).contains(q)) ||
                            (dk.getSinhVien().getEmail() != null && dk.getSinhVien().getEmail().toLowerCase(Locale.ROOT).contains(q))
                    ))
                    .toList();
        }

        Comparator<CourseRegistration> comparator;
        boolean giamDan = sapXep != null && sapXep.startsWith("-");
        String tieuChi = sapXep != null ? sapXep.replace("-", "").trim() : "ma";
        if ("hoTen".equalsIgnoreCase(tieuChi)) {
            Collator viCollator = Collator.getInstance(Locale.forLanguageTag("vi"));
            comparator = Comparator.comparing(dk -> dk.getSinhVien() != null ? dk.getSinhVien().getHoTen() : "", Comparator.nullsLast(viCollator));
        } else {
            comparator = Comparator.comparing(dk -> dk.getSinhVien() != null ? dk.getSinhVien().getMa() : "", Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (giamDan) {
            comparator = comparator.reversed();
        }
        danhSach = danhSach.stream().sorted(comparator).toList();

        ExportSecuritySupport.validateRowCount(danhSach.size(), "danh sách sinh viên lớp học phần");

        Map<Long, Grade> diemMap = danhSach.isEmpty() ? Map.of()
                : diemRepository.findByDangKyIdIn(danhSach.stream().map(CourseRegistration::getId).toList())
                .stream().collect(Collectors.toMap(diem -> diem.getDangKy().getId(), Function.identity()));

        String tenTep = "danh-sach-sinh-vien-" + ExportSecuritySupport.sanitizeFilename(lop.getMa()) + ".csv";
        final List<CourseRegistration> finalDanhSach = danhSach;

        return auditOrExecute("COURSE_STUDENT_LIST_EXPORTED", "COURSE_SECTION", lopHocPhanId, audit -> {
            audit.changes(Map.of(), Map.of(
                    "lopHocPhanId", lopHocPhanId,
                    "maLop", lop.getMa(),
                    "soBanGhi", finalDanhSach.size(),
                    "nguoiXuat", "QUAN_TRI",
                    "tuKhoa", tuKhoa != null && !tuKhoa.isBlank() ? tuKhoa.trim() : "KHONG_LOC",
                    "sapXep", sapXep != null && !sapXep.isBlank() ? sapXep.trim() : "ma"
            ));

            StringBuilder csv = new StringBuilder(CsvUtils.BOM);
            csv.append(CsvUtils.row(List.of("STT", "Mã sinh viên", "Họ tên", "Email", "Điểm quá trình", "Điểm cuối kỳ", "Điểm tổng kết")));
            int stt = 1;
            for (CourseRegistration dangKy : finalDanhSach) {
                Grade diem = diemMap.get(dangKy.getId());
                csv.append(CsvUtils.row(List.of(
                        String.valueOf(stt++),
                        dangKy.getSinhVien() != null && dangKy.getSinhVien().getMa() != null ? dangKy.getSinhVien().getMa() : "",
                        dangKy.getSinhVien() != null && dangKy.getSinhVien().getHoTen() != null ? dangKy.getSinhVien().getHoTen() : "",
                        dangKy.getSinhVien() != null && dangKy.getSinhVien().getEmail() != null ? dangKy.getSinhVien().getEmail() : "",
                        diem != null && diem.getDiemQuaTrinh() != null ? String.valueOf(diem.getDiemQuaTrinh()) : "",
                        diem != null && diem.getDiemCuoiKy() != null ? String.valueOf(diem.getDiemCuoiKy()) : "",
                        diem != null && diem.getDiemTongKet() != null ? String.valueOf(diem.getDiemTongKet()) : ""
                )));
            }
            return new ClassExportResult(tenTep, csv.toString().getBytes(StandardCharsets.UTF_8), finalDanhSach.size());
        });
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH NỘI BỘ VÀ XÁC THỰC BỘ LỌC
    // ==========================================

    private record ResolvedFilters(Semester hocKy, Long hocKyId,
                                   Faculty khoa, Long khoaId,
                                   Major nganh, Long nganhId,
                                   Subject monHoc, Long monHocId,
                                   CourseClass lopHocPhan, Long lopHocPhanId) {
    }

    private ResolvedFilters validateAndResolveFilters(Long hocKyId, Long khoaId, Long nganhId,
                                                      Long monHocId, Long lopHocPhanId) {
        Semester hocKy = null;
        if (hocKyId != null) {
            if (hocKyId <= 0) {
                throw ApiException.badRequest("Học kỳ không hợp lệ");
            }
            hocKy = hocKyService.lay(hocKyId);
        } else {
            hocKy = hocKyService.hocKyHienTai();
        }

        Faculty khoa = null;
        if (khoaId != null) {
            if (khoaId <= 0) {
                throw ApiException.badRequest("Khoa không hợp lệ");
            }
            khoa = khoaRepository.findById(khoaId)
                    .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
        }

        Major nganh = null;
        if (nganhId != null) {
            if (nganhId <= 0) {
                throw ApiException.badRequest("Ngành học không hợp lệ");
            }
            nganh = nganhRepository.findWithKhoaById(nganhId)
                    .orElseThrow(() -> ApiException.notFound("Ngành học không tồn tại"));
            if (khoa != null && (nganh.getKhoa() == null || !nganh.getKhoa().getId().equals(khoa.getId()))) {
                throw ApiException.badRequest("Ngành đang chọn không thuộc khoa đang lọc");
            }
        }

        Subject monHoc = null;
        if (monHocId != null) {
            if (monHocId <= 0) {
                throw ApiException.badRequest("Môn học không hợp lệ");
            }
            monHoc = monHocRepository.findById(monHocId)
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            if (khoa != null && (monHoc.getKhoa() == null || !monHoc.getKhoa().getId().equals(khoa.getId()))) {
                throw ApiException.badRequest("Môn học đang chọn không thuộc khoa đang lọc");
            }
            if (nganh != null && nganh.getKhoa() != null && !monHoc.getKhoa().getId().equals(nganh.getKhoa().getId())) {
                throw ApiException.badRequest("Môn học đang chọn không thuộc khoa của ngành đang lọc");
            }
        }

        CourseClass lopHocPhan = null;
        if (lopHocPhanId != null) {
            if (lopHocPhanId <= 0) {
                throw ApiException.badRequest("Lớp học phần không hợp lệ");
            }
            lopHocPhan = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            if (hocKy != null && !lopHocPhan.getHocKy().getId().equals(hocKy.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc học kỳ đang lọc");
            }
            if (monHoc != null && !lopHocPhan.getMonHoc().getId().equals(monHoc.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc môn học đang lọc");
            }
            if (khoa != null && (lopHocPhan.getMonHoc().getKhoa() == null
                    || !lopHocPhan.getMonHoc().getKhoa().getId().equals(khoa.getId()))) {
                throw ApiException.badRequest("Lớp học phần không thuộc khoa đang lọc");
            }
        }

        return new ResolvedFilters(
                hocKy, hocKy != null ? hocKy.getId() : null,
                khoa, khoa != null ? khoa.getId() : null,
                nganh, nganh != null ? nganh.getId() : null,
                monHoc, monHoc != null ? monHoc.getId() : null,
                lopHocPhan, lopHocPhan != null ? lopHocPhan.getId() : null
        );
    }

    private List<CourseClass> layDanhSachLopTheoBoLoc(ResolvedFilters f) {
        List<CourseClass> list = lopHocPhanRepository.findForAdminAnalytics(
                f.hocKyId(), f.khoaId(), f.monHocId(), f.lopHocPhanId());

        if (f.nganhId() != null) {
            List<Curriculum> curricula = chuongTrinhRepository.findByNganhId(f.nganhId());
            Set<Long> monThuocNganh = curricula.stream()
                    .flatMap(c -> c.getNhom().stream())
                    .flatMap(g -> g.getMonHoc().stream())
                    .map(Subject::getId)
                    .collect(Collectors.toSet());

            if (!monThuocNganh.isEmpty()) {
                list = list.stream()
                        .filter(l -> monThuocNganh.contains(l.getMonHoc().getId()))
                        .toList();
            } else if (f.nganh() != null && f.nganh().getKhoa() != null) {
                // Fallback nếu ngành chưa có CTĐT gán: lọc theo khoa của ngành
                Long khoaCuaNganhId = f.nganh().getKhoa().getId();
                list = list.stream()
                        .filter(l -> l.getMonHoc().getKhoa() != null && l.getMonHoc().getKhoa().getId().equals(khoaCuaNganhId))
                        .toList();
            }
        }

        return list;
    }

    private double calcPercent(long count, long total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(count * 100.0)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double quyDoiHe4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= NGUONG_DIEM_DAT) return 1.5;
        if (diem10 >= 4.0) return 1.0;
        return 0.0;
    }

    private String escapeCsv(String value) {
        return CsvUtils.cell(value);
    }

    private <T> T auditOrExecute(String action, String entityType, Object entityId,
                                 Function<AuditLogService.AuditContext, T> operation) {
        if (auditLogService != null) {
            return auditLogService.execute(action, entityType, entityId, operation);
        }
        return operation.apply(new AuditLogService.AuditContext(entityId == null ? null : String.valueOf(entityId)));
    }

    public record ClassExportResult(String tenTep, byte[] noiDung, int soBanGhi) {
    }
}
