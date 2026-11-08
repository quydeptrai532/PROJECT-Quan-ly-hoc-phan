package com.edu.registration.service;

import com.edu.registration.config.GradeAnalyticsProperties;
import com.edu.registration.dto.GradeAnalyticsDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.GradeSheetStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.RegistrationStatus;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Subject;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.CourseClassRepository;
import com.edu.registration.repository.CourseRegistrationRepository;
import com.edu.registration.repository.FacultyRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ thống kê điểm lớp học phần và phân bố điểm (QLSV-75 / TASK-LC-10).
 *
 * Nguyên tắc cốt lõi:
 * 1. Thống kê chính thức (DA_CONG_BO) chỉ tính trên các lớp có bảng điểm đã công bố (DA_CONG_BO hoặc DA_KHOA)
 *    và các bản ghi đăng ký hiệu lực (DA_DANG_KY) đã có diemTongKet hợp lệ trong [0.0, 10.0].
 * 2. Sinh viên chưa có điểm tổng kết công bố tuyệt đối không bị tính ngầm thành 0.0 mà được tách riêng
 *    vào chỉ số soSinhVienChuaCoDiem (gồm soSinhVienChuaNhapDuDiem và soSinhVienChuaCongBoDiem).
 * 3. Đối với lớp chưa công bố, giảng viên phụ trách hoặc quản trị viên được xem thêm khối thống kê tạm tính
 *    theo trạng thái bảng điểm hiện tại (thongKeTamTinhChuaCongBo) với nhãn cảnh báo rõ ràng, tách biệt hoàn toàn
 *    và không bao giờ trộn vào thống kê công bố chung của môn học hay học kỳ.
 * 4. Mọi phép tính tổng, trung bình, tỷ lệ đạt/trượt và phân bố khoảng điểm dùng BigDecimal để giữ độ chính xác
 *    và chỉ làm tròn HALF_UP 2 chữ số thập phân khi trình bày kết quả.
 * 5. Không nạp và không trả bất kỳ thông tin cá nhân nào của sinh viên (PII) trong dữ liệu thống kê tổng hợp.
 */
@Service
@Transactional(readOnly = true)
public class GradeAnalyticsService {

    public static final String PHAM_VI_DA_CONG_BO = "DA_CONG_BO";
    public static final String PHAM_VI_TAM_TINH = "TAM_TINH_THEO_TRANG_THAI_HIEN_TAI";

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TEN = new BigDecimal("10.0");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal NGUONG_DAT_BD = BigDecimal.valueOf(AcademicRules.DIEM_DAT);

    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final LecturerRepository giangVienRepository;
    private final SubjectRepository monHocRepository;
    private final FacultyRepository khoaRepository;
    private final SemesterService hocKyService;
    private final CurrentUser currentUser;
    private final GradeAnalyticsProperties properties;

    @Autowired
    public GradeAnalyticsService(CourseClassRepository lopHocPhanRepository,
                                 CourseRegistrationRepository dangKyHocPhanRepository,
                                 LecturerRepository giangVienRepository,
                                 SubjectRepository monHocRepository,
                                 FacultyRepository khoaRepository,
                                 SemesterService hocKyService,
                                 CurrentUser currentUser,
                                 GradeAnalyticsProperties properties) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.giangVienRepository = giangVienRepository;
        this.monHocRepository = monHocRepository;
        this.khoaRepository = khoaRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
        this.properties = properties != null ? properties : new GradeAnalyticsProperties();
        validateBucketsConfiguration(this.properties.getBuckets());
    }

    /**
     * Báo cáo thống kê điểm và phân bố cho giảng viên đang đăng nhập (chỉ trong phạm vi các lớp giảng viên phụ trách).
     */
    public GradeAnalyticsDtos.BaoCaoThongKeDiemDto thongKeChoGiangVien(Long hocKyId, Long monHocId, Long lopHocPhanId) {
        User user = yeuCauDangNhap();
        if (user.getRole() != Role.GIANG_VIEN) {
            throw ApiException.forbidden("Chỉ giảng viên mới được truy cập thống kê điểm lớp phụ trách tại đường dẫn này");
        }
        Lecturer giangVien = giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        return xayDungBaoCao(hocKyId, null, monHocId, lopHocPhanId, giangVien.getId(), Role.GIANG_VIEN);
    }

    /**
     * Báo cáo thống kê điểm và phân bố cho quản trị viên (hỗ trợ lọc học kỳ, khoa, môn học, lớp học phần).
     */
    public GradeAnalyticsDtos.BaoCaoThongKeDiemDto thongKeChoQuanTri(Long hocKyId, Long khoaId, Long monHocId, Long lopHocPhanId) {
        User user = yeuCauDangNhap();
        if (user.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Chỉ quản trị viên mới được truy cập thống kê điểm toàn trường");
        }
        return xayDungBaoCao(hocKyId, khoaId, monHocId, lopHocPhanId, null, Role.QUAN_TRI);
    }

    /**
     * Thống kê chi tiết một lớp học phần cụ thể (kiểm tra chặt quyền giảng viên phụ trách hoặc quản trị viên).
     */
    public GradeAnalyticsDtos.ThongKeDiemLopItemDto thongKeMotLop(Long lopHocPhanId) {
        if (lopHocPhanId == null || lopHocPhanId <= 0) {
            throw ApiException.badRequest("Mã định danh lớp học phần không hợp lệ");
        }
        User user = yeuCauDangNhap();
        CourseClass lop = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

        if (user.getRole() == Role.GIANG_VIEN) {
            Lecturer giangVien = giangVienRepository.findByUser(user)
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
            if (lop.getGiangVien() == null || !lop.getGiangVien().getId().equals(giangVien.getId())) {
                throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
            }
        } else if (user.getRole() != Role.QUAN_TRI) {
            throw ApiException.forbidden("Bạn không có quyền xem thống kê điểm lớp học phần này");
        }

        List<CourseRegistrationRepository.DiemTheoLopProjection> rows =
                dangKyHocPhanRepository.findDiemTheoDanhSachLop(List.of(lop.getId()), RegistrationStatus.DA_DANG_KY);
        return xayDungThongKeLopItem(lop, rows, user.getRole());
    }

    private GradeAnalyticsDtos.BaoCaoThongKeDiemDto xayDungBaoCao(Long hocKyId,
                                                                   Long khoaId,
                                                                   Long monHocId,
                                                                   Long lopHocPhanId,
                                                                   Long giangVienIdScope,
                                                                   Role viewerRole) {
        Semester hocKy = null;
        if (hocKyId != null) {
            if (hocKyId <= 0) {
                throw ApiException.badRequest("Học kỳ không hợp lệ");
            }
            hocKy = hocKyService.lay(hocKyId);
        }

        Faculty khoa = null;
        if (khoaId != null) {
            if (khoaId <= 0) {
                throw ApiException.badRequest("Khoa không hợp lệ");
            }
            khoa = khoaRepository.findById(khoaId)
                    .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại"));
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
        }

        CourseClass lopFilter = null;
        if (lopHocPhanId != null) {
            if (lopHocPhanId <= 0) {
                throw ApiException.badRequest("Lớp học phần không hợp lệ");
            }
            lopFilter = lopHocPhanRepository.findWithDetailsById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));

            if (giangVienIdScope != null) {
                if (lopFilter.getGiangVien() == null || !lopFilter.getGiangVien().getId().equals(giangVienIdScope)) {
                    throw ApiException.forbidden("Bạn không phụ trách lớp học phần này");
                }
            }
            if (hocKy != null && !lopFilter.getHocKy().getId().equals(hocKy.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc học kỳ đang lọc");
            }
            if (monHoc != null && !lopFilter.getMonHoc().getId().equals(monHoc.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc môn học đang lọc");
            }
            if (khoa != null && (lopFilter.getMonHoc().getKhoa() == null
                    || !lopFilter.getMonHoc().getKhoa().getId().equals(khoa.getId()))) {
                throw ApiException.badRequest("Lớp học phần không thuộc khoa đang lọc");
            }
        }

        // Lấy danh sách lớp thuộc phạm vi quyền (theo học kỳ + khoa) để tạo danh sách lựa chọn lọc (danhSachMon, danhSachLop)
        List<CourseClass> lopTrongPhamViHocKyKhoa = lopHocPhanRepository.findForGradeAnalytics(
                hocKy != null ? hocKy.getId() : null,
                khoa != null ? khoa.getId() : null,
                null,
                giangVienIdScope,
                null
        );

        // Nếu giảng viên lọc theo môn học mà giảng viên không phụ trách lớp nào của môn đó, kết quả sẽ rỗng an toàn
        final Long targetMonId = monHoc != null ? monHoc.getId() : null;
        final Long targetLopId = lopFilter != null ? lopFilter.getId() : null;

        List<GradeAnalyticsDtos.LuaChonMonDto> danhSachMon = taoDanhSachLuaChonMon(lopTrongPhamViHocKyKhoa);
        List<GradeAnalyticsDtos.LuaChonLopDto> danhSachLop = lopTrongPhamViHocKyKhoa.stream()
                .filter(l -> targetMonId == null || l.getMonHoc().getId().equals(targetMonId))
                .map(this::toLuaChonLopDto)
                .toList();

        // Lọc danh sách lớp chính thức theo toàn bộ điều kiện (hocKyId, khoaId, monHocId, lopHocPhanId)
        List<CourseClass> danhSachLopThongKe = lopTrongPhamViHocKyKhoa.stream()
                .filter(l -> targetMonId == null || l.getMonHoc().getId().equals(targetMonId))
                .filter(l -> targetLopId == null || l.getId().equals(targetLopId))
                .toList();

        // Truy vấn duy nhất 1 lần toàn bộ điểm của các đăng ký DA_DANG_KY thuộc các lớp trong danhSachLopThongKe
        Map<Long, List<CourseRegistrationRepository.DiemTheoLopProjection>> diemTheoLopMap = new LinkedHashMap<>();
        if (!danhSachLopThongKe.isEmpty()) {
            List<Long> lopIds = danhSachLopThongKe.stream().map(CourseClass::getId).toList();
            List<CourseRegistrationRepository.DiemTheoLopProjection> allRows =
                    dangKyHocPhanRepository.findDiemTheoDanhSachLop(lopIds, RegistrationStatus.DA_DANG_KY);
            for (CourseRegistrationRepository.DiemTheoLopProjection row : allRows) {
                diemTheoLopMap.computeIfAbsent(row.getLopHocPhanId(), k -> new ArrayList<>()).add(row);
            }
        }

        List<GradeAnalyticsDtos.ThongKeDiemLopItemDto> theoLop = new ArrayList<>(danhSachLopThongKe.size());
        int soLopDaCongBo = 0;
        int soLopChuaCongBo = 0;

        for (CourseClass lop : danhSachLopThongKe) {
            if (lop.isBangDiemDaCongBo()) {
                soLopDaCongBo++;
            } else {
                soLopChuaCongBo++;
            }
            List<CourseRegistrationRepository.DiemTheoLopProjection> rowsOfClass =
                    diemTheoLopMap.getOrDefault(lop.getId(), List.of());
            theoLop.add(xayDungThongKeLopItem(lop, rowsOfClass, viewerRole));
        }

        // Tổng hợp theo môn học: CHỈ tính điểm trên các lớp đã công bố (không trộn dữ liệu nháp/chưa công bố)
        List<GradeAnalyticsDtos.ThongKeDiemMonItemDto> theoMon =
                xayDungThongKeTheoMon(danhSachLopThongKe, diemTheoLopMap);

        // Tổng hợp chung toàn phạm vi lọc: CHỈ tính điểm trên các lớp đã công bố
        GradeAnalyticsDtos.ChiSoThongKeDiemDto tongHopCongBo = tinhChiSoCongBoTrenDanhSachLop(
                danhSachLopThongKe,
                diemTheoLopMap,
                "Thống kê chính thức trên các bảng điểm đã công bố (DA_CONG_BO / DA_KHOA)"
        );

        return new GradeAnalyticsDtos.BaoCaoThongKeDiemDto(
                hocKy != null ? hocKy.getId() : null,
                hocKy != null ? hocKy.getTen() : null,
                hocKy != null ? hocKy.getNamHoc() : null,
                khoa != null ? khoa.getId() : null,
                khoa != null ? khoa.getTen() : null,
                monHoc != null ? monHoc.getId() : null,
                monHoc != null ? monHoc.getMa() : null,
                monHoc != null ? monHoc.getTen() : null,
                lopFilter != null ? lopFilter.getId() : null,
                lopFilter != null ? lopFilter.getMa() : null,
                viewerRole.name(),
                LocalDateTime.now(),
                danhSachLopThongKe.size(),
                soLopDaCongBo,
                soLopChuaCongBo,
                layCauHinhDto(),
                tongHopCongBo,
                theoMon,
                theoLop,
                danhSachMon,
                danhSachLop
        );
    }

    private GradeAnalyticsDtos.ThongKeDiemLopItemDto xayDungThongKeLopItem(
            CourseClass lop,
            List<CourseRegistrationRepository.DiemTheoLopProjection> rows,
            Role viewerRole) {
        Subject mon = lop.getMonHoc();
        Faculty khoa = mon != null ? mon.getKhoa() : null;
        Semester hocKy = lop.getHocKy();
        Lecturer gv = lop.getGiangVien();
        GradeSheetStatus status = lop.getTrangThaiBangDiemHieuLuc();
        boolean daCongBo = lop.isBangDiemDaCongBo();

        GradeAnalyticsDtos.ChiSoThongKeDiemDto thongKeCongBo = tinhChiSoCongBoTrenDanhSachLop(
                List.of(lop),
                Map.of(lop.getId(), rows),
                daCongBo
                        ? "Đã công bố chính thức (" + status.getMoTa() + ")"
                        : "Bảng điểm chưa công bố (" + status.getMoTa() + ") — Chưa có điểm công bố chính thức"
        );

        // Nếu lớp chưa công bố, cung cấp thêm khối thống kê tạm tính riêng biệt cho giảng viên phụ trách / quản trị viên
        GradeAnalyticsDtos.ChiSoThongKeDiemDto thongKeTamTinh = null;
        if (!daCongBo) {
            thongKeTamTinh = tinhChiSoTamTinhChoLopChuaCongBo(lop, rows);
        }

        String drillDownUrl = viewerRole == Role.GIANG_VIEN
                ? "/lop-cua-toi/" + lop.getId() + "/nhap-diem"
                : "/quan-tri/lop-hoc-phan?hocKyId=" + (hocKy != null ? hocKy.getId() : "") + "&keyword=" + lop.getMa();

        return new GradeAnalyticsDtos.ThongKeDiemLopItemDto(
                lop.getId(),
                lop.getMa(),
                mon != null ? mon.getId() : null,
                mon != null ? mon.getMa() : null,
                mon != null ? mon.getTen() : null,
                mon != null ? mon.getSoTinChi() : 0,
                khoa != null ? khoa.getId() : null,
                khoa != null ? khoa.getTen() : null,
                hocKy != null ? hocKy.getId() : null,
                hocKy != null ? hocKy.getTen() : null,
                hocKy != null ? hocKy.getNamHoc() : null,
                gv != null ? gv.getId() : null,
                gv != null ? gv.getHoTen() : null,
                status.name(),
                status.getMoTa(),
                daCongBo,
                lop.isBangDiemDaKhoa(),
                lop.getCongBoBangDiemLuc(),
                true,
                drillDownUrl,
                thongKeCongBo,
                thongKeTamTinh
        );
    }

    private List<GradeAnalyticsDtos.ThongKeDiemMonItemDto> xayDungThongKeTheoMon(
            List<CourseClass> danhSachLop,
            Map<Long, List<CourseRegistrationRepository.DiemTheoLopProjection>> diemTheoLopMap) {
        Map<Long, List<CourseClass>> groupedByMon = danhSachLop.stream()
                .filter(l -> l.getMonHoc() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getMonHoc().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<GradeAnalyticsDtos.ThongKeDiemMonItemDto> ketQua = new ArrayList<>(groupedByMon.size());
        groupedByMon.values().stream()
                .sorted(Comparator.comparing(list -> list.get(0).getMonHoc().getMa()))
                .forEach(lopCuaMon -> {
                    Subject mon = lopCuaMon.get(0).getMonHoc();
                    Faculty khoa = mon.getKhoa();
                    int soDaCongBo = (int) lopCuaMon.stream().filter(CourseClass::isBangDiemDaCongBo).count();
                    int soChuaCongBo = lopCuaMon.size() - soDaCongBo;

                    GradeAnalyticsDtos.ChiSoThongKeDiemDto chiSoCongBo = tinhChiSoCongBoTrenDanhSachLop(
                            lopCuaMon,
                            diemTheoLopMap,
                            "Tổng hợp điểm đã công bố của môn " + mon.getMa()
                                    + " (" + soDaCongBo + "/" + lopCuaMon.size() + " lớp đã công bố)"
                    );

                    ketQua.add(new GradeAnalyticsDtos.ThongKeDiemMonItemDto(
                            mon.getId(),
                            mon.getMa(),
                            mon.getTen(),
                            mon.getSoTinChi(),
                            khoa != null ? khoa.getId() : null,
                            khoa != null ? khoa.getTen() : null,
                            lopCuaMon.size(),
                            soDaCongBo,
                            soChuaCongBo,
                            chiSoCongBo
                    ));
                });
        return ketQua;
    }

    /**
     * Tính toán chỉ số thống kê chính thức (chỉ tính điểm của các lớp đã công bố DA_CONG_BO / DA_KHOA).
     * Đăng ký ở lớp chưa công bố hoặc chưa nhập đủ điểm được phân loại rõ vào soSinhVienChuaCoDiem,
     * tuyệt đối không gán bằng 0.0 và không đưa vào mẫu số tính điểm trung bình hay tỷ lệ đạt/trượt.
     */
    private GradeAnalyticsDtos.ChiSoThongKeDiemDto tinhChiSoCongBoTrenDanhSachLop(
            List<CourseClass> danhSachLop,
            Map<Long, List<CourseRegistrationRepository.DiemTheoLopProjection>> diemTheoLopMap,
            String nhanPhamVi) {
        int tongSoDangKyHieuLuc = 0;
        int soChuaNhapDuDiem = 0;
        int soChuaCongBoDiem = 0;
        List<BigDecimal> validPublishedScores = new ArrayList<>();

        boolean tatCaDaCongBo = !danhSachLop.isEmpty()
                && danhSachLop.stream().allMatch(CourseClass::isBangDiemDaCongBo);
        boolean coItNhatMotLopDaCongBo = danhSachLop.stream().anyMatch(CourseClass::isBangDiemDaCongBo);

        for (CourseClass lop : danhSachLop) {
            boolean lopDaCongBo = lop.isBangDiemDaCongBo();
            List<CourseRegistrationRepository.DiemTheoLopProjection> rows =
                    diemTheoLopMap.getOrDefault(lop.getId(), List.of());

            for (CourseRegistrationRepository.DiemTheoLopProjection row : rows) {
                tongSoDangKyHieuLuc++;
                Double rawScore = row.getDiemTongKet();
                boolean hasValidScore = rawScore != null && !rawScore.isNaN() && !rawScore.isInfinite()
                        && rawScore >= 0.0 && rawScore <= 10.0;

                if (!lopDaCongBo) {
                    if (hasValidScore) {
                        soChuaCongBoDiem++;
                    } else {
                        soChuaNhapDuDiem++;
                    }
                } else {
                    if (hasValidScore) {
                        validPublishedScores.add(BigDecimal.valueOf(rawScore));
                    } else {
                        soChuaNhapDuDiem++;
                    }
                }
            }
        }

        String giaiThichMauSo = "Mẫu số tính điểm trung bình, tỷ lệ đạt/trượt và phân bố khoảng điểm là số bản ghi đăng ký đang học (DA_DANG_KY) "
                + "thuộc bảng điểm đã công bố (DA_CONG_BO / DA_KHOA) và đã có điểm tổng kết hợp lệ (soBanGhiDuocTinh). "
                + "Sinh viên chưa có điểm công bố (" + (soChuaNhapDuDiem + soChuaCongBoDiem) + " sinh viên) được tách riêng và không tính thành 0.0.";

        return tinhToanChiSoTuDanhSachDiem(
                PHAM_VI_DA_CONG_BO,
                nhanPhamVi,
                tatCaDaCongBo || coItNhatMotLopDaCongBo,
                tongSoDangKyHieuLuc,
                soChuaNhapDuDiem,
                soChuaCongBoDiem,
                giaiThichMauSo,
                validPublishedScores
        );
    }

    /**
     * Tính toán chỉ số tạm tính trên bảng điểm chưa công bố dành riêng cho giảng viên phụ trách / quản trị viên
     * khi xem từng lớp cụ thể. Gắn nhãn cảnh báo rõ ràng và tách biệt với thống kê công bố.
     */
    private GradeAnalyticsDtos.ChiSoThongKeDiemDto tinhChiSoTamTinhChoLopChuaCongBo(
            CourseClass lop,
            List<CourseRegistrationRepository.DiemTheoLopProjection> rows) {
        int tongSoDangKyHieuLuc = 0;
        int soChuaNhapDuDiem = 0;
        List<BigDecimal> draftScores = new ArrayList<>();

        for (CourseRegistrationRepository.DiemTheoLopProjection row : rows) {
            tongSoDangKyHieuLuc++;
            Double rawScore = row.getDiemTongKet();
            boolean hasValidScore = rawScore != null && !rawScore.isNaN() && !rawScore.isInfinite()
                    && rawScore >= 0.0 && rawScore <= 10.0;
            if (hasValidScore) {
                draftScores.add(BigDecimal.valueOf(rawScore));
            } else {
                soChuaNhapDuDiem++;
            }
        }

        GradeSheetStatus status = lop.getTrangThaiBangDiemHieuLuc();
        String nhanPhamVi = "Số liệu tạm tính theo trạng thái hiện tại (" + status.getMoTa() + " - " + status.name()
                + ") — Chưa công bố chính thức, không gộp vào thống kê công khai";
        String giaiThichMauSo = "Số liệu tạm tính nội bộ cho giảng viên phụ trách / quản trị trên các bản ghi đã nhập đủ điểm tổng kết ("
                + draftScores.size() + "/" + tongSoDangKyHieuLuc + " sinh viên). "
                + "Sinh viên chưa nhập đủ điểm (" + soChuaNhapDuDiem + " sinh viên) không bị tính thành 0.0.";

        return tinhToanChiSoTuDanhSachDiem(
                PHAM_VI_TAM_TINH,
                nhanPhamVi,
                false,
                tongSoDangKyHieuLuc,
                soChuaNhapDuDiem,
                draftScores.size(),
                giaiThichMauSo,
                draftScores
        );
    }

    private GradeAnalyticsDtos.ChiSoThongKeDiemDto tinhToanChiSoTuDanhSachDiem(
            String phamViDuLieu,
            String nhanPhamVi,
            boolean daCongBoChinhThuc,
            int tongSoDangKyHieuLuc,
            int soChuaNhapDuDiem,
            int soChuaCongBoDiem,
            String giaiThichMauSo,
            List<BigDecimal> validScores) {
        int soBanGhiDuocTinh = validScores.size();
        int soSinhVienChuaCoDiem = Math.max(0, tongSoDangKyHieuLuc - soBanGhiDuocTinh);

        List<GradeAnalyticsProperties.BucketDef> bucketDefs = properties.getBuckets();
        int[] bucketCounts = new int[bucketDefs.size()];

        BigDecimal sum = ZERO;
        BigDecimal min = null;
        BigDecimal max = null;
        int soDat = 0;
        int soTruot = 0;

        for (BigDecimal score : validScores) {
            sum = sum.add(score);
            if (min == null || score.compareTo(min) < 0) {
                min = score;
            }
            if (max == null || score.compareTo(max) > 0) {
                max = score;
            }
            if (score.compareTo(NGUONG_DAT_BD) >= 0) {
                soDat++;
            } else {
                soTruot++;
            }
            for (int i = 0; i < bucketDefs.size(); i++) {
                if (bucketDefs.get(i).contains(score)) {
                    bucketCounts[i]++;
                    break;
                }
            }
        }

        Double diemTrungBinh = null;
        Double diemThapNhat = null;
        Double diemCaoNhat = null;
        double tyLeDat = 0.0;
        double tyLeTruot = 0.0;

        if (soBanGhiDuocTinh > 0) {
            BigDecimal denominator = BigDecimal.valueOf(soBanGhiDuocTinh);
            diemTrungBinh = sum.divide(denominator, 2, RoundingMode.HALF_UP).doubleValue();
            diemThapNhat = min.setScale(2, RoundingMode.HALF_UP).doubleValue();
            diemCaoNhat = max.setScale(2, RoundingMode.HALF_UP).doubleValue();
            tyLeDat = BigDecimal.valueOf(soDat)
                    .multiply(HUNDRED)
                    .divide(denominator, 2, RoundingMode.HALF_UP)
                    .doubleValue();
            tyLeTruot = BigDecimal.valueOf(soTruot)
                    .multiply(HUNDRED)
                    .divide(denominator, 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        List<GradeAnalyticsDtos.KhoangPhanBoDiemDto> phanBoDiem = new ArrayList<>(bucketDefs.size());
        int tongSoTrongPhanBo = 0;
        for (int i = 0; i < bucketDefs.size(); i++) {
            GradeAnalyticsProperties.BucketDef def = bucketDefs.get(i);
            int count = bucketCounts[i];
            tongSoTrongPhanBo += count;
            double pct = soBanGhiDuocTinh == 0
                    ? 0.0
                    : BigDecimal.valueOf(count)
                            .multiply(HUNDRED)
                            .divide(BigDecimal.valueOf(soBanGhiDuocTinh), 2, RoundingMode.HALF_UP)
                            .doubleValue();
            phanBoDiem.add(new GradeAnalyticsDtos.KhoangPhanBoDiemDto(
                    def.maKhoang(),
                    def.tenKhoang(),
                    def.kyHieuKhoang(),
                    def.tuDiem().doubleValue(),
                    def.denDiem().doubleValue(),
                    def.baoGomTuDiem(),
                    def.baoGomDenDiem(),
                    def.isLaKhoangDat(),
                    count,
                    pct,
                    soBanGhiDuocTinh
            ));
        }

        return new GradeAnalyticsDtos.ChiSoThongKeDiemDto(
                phamViDuLieu,
                nhanPhamVi,
                daCongBoChinhThuc,
                tongSoDangKyHieuLuc,
                soBanGhiDuocTinh,
                soBanGhiDuocTinh,
                soSinhVienChuaCoDiem,
                soChuaNhapDuDiem,
                soChuaCongBoDiem,
                soBanGhiDuocTinh,
                giaiThichMauSo,
                AcademicRules.DIEM_DAT,
                diemTrungBinh,
                diemThapNhat,
                diemCaoNhat,
                soDat,
                soTruot,
                tyLeDat,
                tyLeTruot,
                tongSoTrongPhanBo,
                phanBoDiem
        );
    }

    public GradeAnalyticsDtos.CauHinhThongKeDiemDto layCauHinhDto() {
        List<GradeAnalyticsDtos.KhoangPhanBoDiemDto> defaultBuckets = properties.getBuckets().stream()
                .map(def -> new GradeAnalyticsDtos.KhoangPhanBoDiemDto(
                        def.maKhoang(),
                        def.tenKhoang(),
                        def.kyHieuKhoang(),
                        def.tuDiem().doubleValue(),
                        def.denDiem().doubleValue(),
                        def.baoGomTuDiem(),
                        def.baoGomDenDiem(),
                        def.isLaKhoangDat(),
                        0,
                        0.0,
                        0
                ))
                .toList();

        return new GradeAnalyticsDtos.CauHinhThongKeDiemDto(
                AcademicRules.DIEM_DAT,
                "AcademicRules.DIEM_DAT",
                properties.getMissingGradePolicy(),
                properties.isOfficialRegulation(),
                properties.getPolicyNote(),
                defaultBuckets
        );
    }

    private List<GradeAnalyticsDtos.LuaChonMonDto> taoDanhSachLuaChonMon(List<CourseClass> lopList) {
        Map<Long, List<CourseClass>> grouped = lopList.stream()
                .filter(l -> l.getMonHoc() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getMonHoc().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .sorted(Comparator.comparing(list -> list.get(0).getMonHoc().getMa()))
                .map(list -> {
                    Subject m = list.get(0).getMonHoc();
                    Faculty k = m.getKhoa();
                    return new GradeAnalyticsDtos.LuaChonMonDto(
                            m.getId(),
                            m.getMa(),
                            m.getTen(),
                            m.getSoTinChi(),
                            k != null ? k.getId() : null,
                            k != null ? k.getTen() : null,
                            list.size()
                    );
                })
                .toList();
    }

    private GradeAnalyticsDtos.LuaChonLopDto toLuaChonLopDto(CourseClass lop) {
        Subject m = lop.getMonHoc();
        Semester hk = lop.getHocKy();
        GradeSheetStatus status = lop.getTrangThaiBangDiemHieuLuc();
        return new GradeAnalyticsDtos.LuaChonLopDto(
                lop.getId(),
                lop.getMa(),
                m != null ? m.getId() : null,
                m != null ? m.getMa() : null,
                m != null ? m.getTen() : null,
                hk != null ? hk.getId() : null,
                hk != null ? hk.getTen() : null,
                hk != null ? hk.getNamHoc() : null,
                status.name(),
                status.getMoTa(),
                lop.isBangDiemDaCongBo()
        );
    }

    /**
     * Kiểm tra các khoảng phân bố điểm liên tiếp, không chồng lấn và phủ kín miền [0.0, 10.0].
     */
    static void validateBucketsConfiguration(List<GradeAnalyticsProperties.BucketDef> buckets) {
        if (buckets == null || buckets.isEmpty()) {
            throw new IllegalArgumentException("Danh sách khoảng phân bố điểm không được để trống");
        }
        GradeAnalyticsProperties.BucketDef first = buckets.get(0);
        if (first.tuDiem().compareTo(ZERO) != 0 || !first.baoGomTuDiem()) {
            throw new IllegalArgumentException("Khoảng phân bố điểm đầu tiên phải bắt đầu từ 0.0 (bao gồm 0.0)");
        }
        GradeAnalyticsProperties.BucketDef last = buckets.get(buckets.size() - 1);
        if (last.denDiem().compareTo(TEN) != 0 || !last.baoGomDenDiem()) {
            throw new IllegalArgumentException("Khoảng phân bố điểm cuối cùng phải kết thúc tại 10.0 (bao gồm 10.0)");
        }
        for (int i = 0; i < buckets.size(); i++) {
            GradeAnalyticsProperties.BucketDef cur = buckets.get(i);
            if (cur.tuDiem().compareTo(cur.denDiem()) >= 0) {
                throw new IllegalArgumentException("Khoảng điểm không hợp lệ: " + cur.kyHieuKhoang());
            }
            if (i > 0) {
                GradeAnalyticsProperties.BucketDef prev = buckets.get(i - 1);
                if (prev.denDiem().compareTo(cur.tuDiem()) != 0) {
                    throw new IllegalArgumentException("Khoảng điểm bị hở hoặc chồng lấn giữa "
                            + prev.kyHieuKhoang() + " và " + cur.kyHieuKhoang());
                }
                if (prev.baoGomDenDiem() == cur.baoGomTuDiem()) {
                    throw new IllegalArgumentException("Giá trị biên " + cur.tuDiem()
                            + " bị trùng hoặc bị bỏ sót giữa " + prev.kyHieuKhoang() + " và " + cur.kyHieuKhoang());
                }
            }
        }
    }

    private User yeuCauDangNhap() {
        User user = currentUser != null ? currentUser.get() : null;
        if (user == null) {
            throw ApiException.forbidden("Vui lòng đăng nhập để tiếp tục");
        }
        return user;
    }
}
