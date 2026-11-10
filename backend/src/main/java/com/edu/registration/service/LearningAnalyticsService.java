package com.edu.registration.service;

import com.edu.registration.dto.GradeAnalyticsDtos;
import com.edu.registration.dto.LearningAnalyticsDtos;
import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.Faculty;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Major;
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
import com.edu.registration.repository.MajorRepository;
import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.repository.SubjectRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.util.ExportSecuritySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dịch vụ Phân tích học tập (Learning Analytics cơ bản - QLSV-98 / TASK-OP-11).
 *
 * Tuân thủ nghiêm ngặt:
 * 1. Từ điển KPI TASK-AD-11 (QLSV-86) & CLASS_GRADE_ANALYTICS (QLSV-75):
 *    - Tỷ lệ Đạt / Trượt theo ngưỡng AcademicRules.DIEM_DAT (5.0).
 *    - Mẫu số M: chỉ tính các sinh viên có điểm tổng kết hợp lệ [0.0, 10.0] thuộc các lớp đã công bố điểm (DA_CONG_BO, DA_KHOA).
 *    - Lớp chưa công bố điểm bị loại khỏi mẫu số chính thức; điểm thiếu không tự tính thành 0.0.
 * 2. Xu hướng GPA theo học kỳ: tính theo trình tự thời gian qua các học kỳ, hiển thị rõ mẫu số M và trạng thái dữ liệu.
 * 3. So sánh các lớp cùng môn: chỉ so sánh khi cùng môn, cùng học kỳ, cùng trạng thái công bố và cỡ mẫu đủ (M >= 5).
 * 4. Không suy diễn quan hệ nhân quả từ các số liệu mô tả thống kê.
 * 5. Tuyệt đối không để lộ danh tính cá nhân sinh viên (Zero Student PII).
 * 6. Kiểm soát phân quyền chặt chẽ tại tầng backend (QUAN_TRI và GIANG_VIEN).
 */
@Service
@Transactional(readOnly = true)
public class LearningAnalyticsService {

    public static final double NGUONG_DIEM_DAT = AcademicRules.DIEM_DAT; // 5.0
    public static final int SO_MAU_TOI_THIEU_SO_SANH = 5;
    public static final String CHINH_SACH_DIEM_THIEU = "EXCLUDE_FROM_DENOMINATOR: Điểm thiếu hoặc lớp chưa công bố bị loại khỏi mẫu số; tuyệt đối không tự đổi thành 0.0";
    public static final String CANH_BAO_NHAN_QUA = "CẢNH BÁO PHÂN TÍCH: Toàn bộ số liệu trên thuần túy mô tả thống kê phân bố điểm thực tế. Tuyệt đối không suy diễn kết luận nhân quả về năng lực giảng dạy của giảng viên hay mức độ khó của đề thi từ dữ liệu mô tả này mà không qua nghiên cứu kiểm soát các biến ngoại lai (thành phần sinh viên, hình thức thi, điều kiện giảng dạy...).";
    public static final String GHI_CHU_QUY_CHE = "Chỉ số bám sát từ điển KPI TASK-AD-11. Các tham số chờ Hội đồng Đào tạo chuẩn hóa: quy chế tính GPA khi học lại (cao nhất vs lần cuối); quy đổi thang điểm [5.0, 5.5) = 1.5; 5 khoảng phân bố điểm tập trung.";

    private final CourseClassRepository lopHocPhanRepository;
    private final CourseRegistrationRepository dangKyHocPhanRepository;
    private final SemesterRepository hocKyRepository;
    private final FacultyRepository khoaRepository;
    private final MajorRepository nganhRepository;
    private final SubjectRepository monHocRepository;
    private final LecturerRepository giangVienRepository;
    private final SemesterService hocKyService;
    private final CurrentUser currentUser;

    @Autowired
    public LearningAnalyticsService(CourseClassRepository lopHocPhanRepository,
                                  CourseRegistrationRepository dangKyHocPhanRepository,
                                  SemesterRepository hocKyRepository,
                                  FacultyRepository khoaRepository,
                                  MajorRepository nganhRepository,
                                  SubjectRepository monHocRepository,
                                  LecturerRepository giangVienRepository,
                                  SemesterService hocKyService,
                                  CurrentUser currentUser) {
        this.lopHocPhanRepository = lopHocPhanRepository;
        this.dangKyHocPhanRepository = dangKyHocPhanRepository;
        this.hocKyRepository = hocKyRepository;
        this.khoaRepository = khoaRepository;
        this.nganhRepository = nganhRepository;
        this.monHocRepository = monHocRepository;
        this.giangVienRepository = giangVienRepository;
        this.hocKyService = hocKyService;
        this.currentUser = currentUser;
    }

    /**
     * Báo cáo phân tích học tập tổng hợp cho Quản trị viên.
     */
    public LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto layBaoCaoChoQuanTri(
            Long hocKyId, Long khoaId, Long nganhId, Long monHocId, Long lopHocPhanId) {
        yeuCauVaiTro(Role.QUAN_TRI);

        ResolvedFilters filters = validateAndResolveFilters(hocKyId, khoaId, nganhId, monHocId, lopHocPhanId);
        List<CourseClass> danhSachLop = layDanhSachLop(filters, null);

        return buildBaoCaoAnalytics(filters, danhSachLop, "QUAN_TRI", null);
    }

    /**
     * Báo cáo phân tích học tập cho Giảng viên (chỉ trong phạm vi các lớp/môn giảng viên phụ trách).
     */
    public LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto layBaoCaoChoGiangVien(
            Long hocKyId, Long monHocId, Long lopHocPhanId) {
        User user = yeuCauVaiTro(Role.GIANG_VIEN);
        Lecturer giangVien = giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));

        ResolvedFilters filters = validateAndResolveFilters(hocKyId, null, null, monHocId, lopHocPhanId);

        // Giảng viên chỉ được xem môn mà mình có lớp phụ trách
        if (monHocId != null) {
            boolean coDayMon = lopHocPhanRepository.findByGiangVienIdOrderByMaAsc(giangVien.getId()).stream()
                    .anyMatch(lop -> lop.getMonHoc() != null && Objects.equals(lop.getMonHoc().getId(), monHocId));
            if (!coDayMon) {
                throw ApiException.forbidden("Giảng viên không phụ trách môn học này");
            }
        }

        List<CourseClass> danhSachLop = layDanhSachLop(filters, giangVien.getId());
        return buildBaoCaoAnalytics(filters, danhSachLop, "GIANG_VIEN", giangVien);
    }

    /**
     * Xu hướng GPA theo học kỳ cho Quản trị viên (toàn trường hoặc theo Khoa/Ngành/Môn).
     */
    public List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> layXuHuongGpaChoQuanTri(
            Long khoaId, Long nganhId, Long monHocId) {
        yeuCauVaiTro(Role.QUAN_TRI);
        ResolvedFilters filters = validateAndResolveFilters(null, khoaId, nganhId, monHocId, null);
        return tinhXuHuongGpaTheoHocKy(filters, null);
    }

    /**
     * Xu hướng GPA theo học kỳ cho Giảng viên (các lớp do giảng viên phụ trách).
     */
    public List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> layXuHuongGpaChoGiangVien(Long monHocId) {
        User user = yeuCauVaiTro(Role.GIANG_VIEN);
        Lecturer giangVien = giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));

        ResolvedFilters filters = validateAndResolveFilters(null, null, null, monHocId, null);
        return tinhXuHuongGpaTheoHocKy(filters, giangVien.getId());
    }

    /**
     * So sánh các lớp cùng môn cho Quản trị viên.
     */
    public LearningAnalyticsDtos.SoSanhCungMonDto laySoSanhCungMonChoQuanTri(Long monHocId, Long hocKyId) {
        yeuCauVaiTro(Role.QUAN_TRI);
        if (monHocId == null) {
            throw ApiException.badRequest("Vui lòng chọn môn học để thực hiện so sánh các lớp");
        }
        Semester hocKy = hocKyId == null ? hocKyService.hocKyHienTai() : layHocKy(hocKyId);
        Subject monHoc = monHocRepository.findById(monHocId)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));

        ResolvedFilters filters = new ResolvedFilters(hocKy, null, null, monHoc, null);
        List<CourseClass> danhSachLop = lopHocPhanRepository.findForAdminAnalytics(
                hocKy != null ? hocKy.getId() : null, null, monHocId, null);

        return tinhSoSanhCacLopCungMon(monHoc, hocKy, danhSachLop);
    }

    /**
     * So sánh các lớp cùng môn cho Giảng viên.
     */
    public LearningAnalyticsDtos.SoSanhCungMonDto laySoSanhCungMonChoGiangVien(Long monHocId, Long hocKyId) {
        User user = yeuCauVaiTro(Role.GIANG_VIEN);
        Lecturer giangVien = giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));

        if (monHocId == null) {
            throw ApiException.badRequest("Vui lòng chọn môn học để thực hiện so sánh các lớp");
        }
        Semester hocKy = hocKyId == null ? hocKyService.hocKyHienTai() : layHocKy(hocKyId);
        Subject monHoc = monHocRepository.findById(monHocId)
                .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));

        List<CourseClass> danhSachLop = lopHocPhanRepository.findForGradeAnalytics(
                hocKy != null ? hocKy.getId() : null, null, monHocId, giangVien.getId(), null);

        return tinhSoSanhCacLopCungMon(monHoc, hocKy, danhSachLop);
    }

    /**
     * Xuất CSV Learning Analytics cho Quản trị viên.
     */
    public byte[] xuatCsvChoQuanTri(Long hocKyId, Long khoaId, Long nganhId, Long monHocId) {
        yeuCauVaiTro(Role.QUAN_TRI);
        LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto baoCao = layBaoCaoChoQuanTri(
                hocKyId, khoaId, nganhId, monHocId, null);
        return taoNoiDungCsv(baoCao, "QUAN_TRI");
    }

    /**
     * Xuất CSV Learning Analytics cho Giảng viên.
     */
    public byte[] xuatCsvChoGiangVien(Long hocKyId, Long monHocId) {
        User user = yeuCauVaiTro(Role.GIANG_VIEN);
        Lecturer giangVien = giangVienRepository.findByUser(user)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
        LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto baoCao = layBaoCaoChoGiangVien(
                hocKyId, monHocId, null);
        return taoNoiDungCsv(baoCao, "GIANG_VIEN: " + (giangVien.getHoTen() != null ? giangVien.getHoTen() : user.getUsername()));
    }

    // =========================================================================
    // Core Calculation Logic
    // =========================================================================

    private LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto buildBaoCaoAnalytics(
            ResolvedFilters filters, List<CourseClass> danhSachLop, String vaiTro, Lecturer giangVien) {

        // 1. Phân loại lớp công bố vs chưa công bố
        List<CourseClass> lopDaCongBo = danhSachLop.stream()
                .filter(CourseClass::isBangDiemDaCongBo)
                .toList();
        List<Long> lopDaCongBoIds = lopDaCongBo.stream().map(CourseClass::getId).toList();

        int tongSoLop = danhSachLop.size();
        int soLopDaCongBo = lopDaCongBo.size();
        int soLopChuaCongBo = tongSoLop - soLopDaCongBo;
        int tongSoDangKyHieuLuc = danhSachLop.stream().mapToInt(CourseClass::getSiSoHienTai).sum();

        // 2. Điểm hợp lệ của các lớp đã công bố (Mẫu số M)
        List<CourseRegistrationRepository.DiemTheoLopProjection> diemList = lopDaCongBoIds.isEmpty()
                ? List.of()
                : dangKyHocPhanRepository.findDiemTheoDanhSachLop(lopDaCongBoIds, RegistrationStatus.DA_DANG_KY);

        List<CourseRegistrationRepository.DiemTheoLopProjection> diemHopLe = diemList.stream()
                .filter(d -> d.getDiemTongKet() != null && d.getDiemTongKet() >= 0.0 && d.getDiemTongKet() <= 10.0)
                .toList();

        int mauSoDuocTinh = diemHopLe.size();
        int soSinhVienChuaCoDiem = tongSoDangKyHieuLuc - mauSoDuocTinh;

        int soDat = 0;
        int soTruot = 0;
        Double diemThapNhat = null;
        Double diemCaoNhat = null;
        Double diemTrungBinh = null;
        Double gpaThang4 = null;

        if (mauSoDuocTinh > 0) {
            double tongDiem = 0.0;
            double min = 10.0;
            double max = 0.0;
            double tongDiem4 = 0.0;

            for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
                double x = d.getDiemTongKet();
                tongDiem += x;
                if (x < min) min = x;
                if (x > max) max = x;
                if (x >= NGUONG_DIEM_DAT) soDat++;
                else soTruot++;
                tongDiem4 += quyDoiHe4(x);
            }

            diemThapNhat = min;
            diemCaoNhat = max;
            diemTrungBinh = BigDecimal.valueOf(tongDiem)
                    .divide(BigDecimal.valueOf(mauSoDuocTinh), 2, RoundingMode.HALF_UP).doubleValue();
            gpaThang4 = BigDecimal.valueOf(tongDiem4)
                    .divide(BigDecimal.valueOf(mauSoDuocTinh), 2, RoundingMode.HALF_UP).doubleValue();
        }

        double tyLeDat = mauSoDuocTinh == 0 ? 0.0 : calcPercent(soDat, mauSoDuocTinh);
        double tyLeTruot = mauSoDuocTinh == 0 ? 0.0 : calcPercent(soTruot, mauSoDuocTinh);

        LearningAnalyticsDtos.ChiSoKpiHocTapDto kpi = new LearningAnalyticsDtos.ChiSoKpiHocTapDto(
                tongSoLop,
                soLopDaCongBo,
                soLopChuaCongBo,
                tongSoDangKyHieuLuc,
                mauSoDuocTinh,
                soSinhVienChuaCoDiem,
                diemTrungBinh,
                diemThapNhat,
                diemCaoNhat,
                gpaThang4,
                soDat,
                soTruot,
                tyLeDat,
                tyLeTruot,
                NGUONG_DIEM_DAT,
                CHINH_SACH_DIEM_THIEU,
                CANH_BAO_NHAN_QUA
        );

        // 3. Phân bố 5 khoảng điểm chuẩn
        List<LearningAnalyticsDtos.KhoangDiemPhanBoDto> phanBoDiem = tinhPhanBo5Khoang(diemHopLe, mauSoDuocTinh);

        // 4. Xu hướng GPA qua các học kỳ
        List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> xuHuongTheoHocKy = tinhXuHuongGpaTheoHocKy(
                filters, giangVien != null ? giangVien.getId() : null);

        // 5. So sánh các lớp cùng môn (nếu đã chọn môn học cụ thể)
        LearningAnalyticsDtos.SoSanhCungMonDto soSanhCungMon = null;
        if (filters.monHoc() != null) {
            soSanhCungMon = tinhSoSanhCacLopCungMon(filters.monHoc(), filters.hocKy(), danhSachLop);
        }

        // 6. Danh sách môn học hỗ trợ lọc nhanh
        List<GradeAnalyticsDtos.LuaChonMonDto> danhSachMonHoc = layDanhSachMonTrongPhamVi(danhSachLop);

        LearningAnalyticsDtos.BoLocHocTapDto boLoc = new LearningAnalyticsDtos.BoLocHocTapDto(
                filters.hocKy() != null ? filters.hocKy().getId() : null,
                filters.hocKy() != null ? filters.hocKy().getTen() + " - " + filters.hocKy().getNamHoc() : "Tất cả học kỳ",
                filters.khoa() != null ? filters.khoa().getId() : null,
                filters.khoa() != null ? filters.khoa().getTen() : "Tất cả khoa",
                filters.nganh() != null ? filters.nganh().getId() : null,
                filters.nganh() != null ? filters.nganh().getTen() : "Tất cả ngành",
                filters.monHoc() != null ? filters.monHoc().getId() : null,
                filters.monHoc() != null ? filters.monHoc().getMa() + " - " + filters.monHoc().getTen() : "Tất cả môn học",
                vaiTro,
                LocalDateTime.now()
        );

        return new LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto(
                boLoc,
                kpi,
                phanBoDiem,
                xuHuongTheoHocKy,
                soSanhCungMon,
                danhSachMonHoc,
                CANH_BAO_NHAN_QUA,
                GHI_CHU_QUY_CHE
        );
    }

    private List<LearningAnalyticsDtos.KhoangDiemPhanBoDto> tinhPhanBo5Khoang(
            List<CourseRegistrationRepository.DiemTheoLopProjection> diemHopLe, int mauSo) {
        int kem = 0;   // [0.0, 4.0)
        int yeu = 0;   // [4.0, 5.0)
        int tb = 0;    // [5.0, 6.5)
        int kha = 0;   // [6.5, 8.0)
        int gioi = 0;  // [8.0, 10.0]

        for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
            double x = d.getDiemTongKet();
            if (x < 4.0) kem++;
            else if (x < 5.0) yeu++;
            else if (x < 6.5) tb++;
            else if (x < 8.0) kha++;
            else gioi++;
        }

        return List.of(
                new LearningAnalyticsDtos.KhoangDiemPhanBoDto("KEM", "[0.0, 4.0)", "Dưới 4.0 (Kém)", kem, calcPercent(kem, mauSo), false),
                new LearningAnalyticsDtos.KhoangDiemPhanBoDto("YEU", "[4.0, 5.0)", "4.0 đến dưới 5.0 (Yếu)", yeu, calcPercent(yeu, mauSo), false),
                new LearningAnalyticsDtos.KhoangDiemPhanBoDto("TRUNG_BINH", "[5.0, 6.5)", "5.0 đến dưới 6.5 (Trung bình)", tb, calcPercent(tb, mauSo), true),
                new LearningAnalyticsDtos.KhoangDiemPhanBoDto("KHA", "[6.5, 8.0)", "6.5 đến dưới 8.0 (Khá)", kha, calcPercent(kha, mauSo), true),
                new LearningAnalyticsDtos.KhoangDiemPhanBoDto("GIOI_XUAT_SAC", "[8.0, 10.0]", "8.0 đến 10.0 (Giỏi - Xuất sắc)", gioi, calcPercent(gioi, mauSo), true)
        );
    }

    /**
     * Tính xu hướng GPA qua tất cả các học kỳ theo thứ tự thời gian tăng dần.
     */
    private List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> tinhXuHuongGpaTheoHocKy(
            ResolvedFilters filters, Long giangVienId) {
        List<Semester> hocKyList = hocKyRepository.findAllByOrderByNgayBatDauAsc();
        if (hocKyList.isEmpty()) {
            return List.of();
        }

        List<LearningAnalyticsDtos.XuHuongGpaHocKyDto> result = new ArrayList<>();

        for (Semester hk : hocKyList) {
            ResolvedFilters hkFilters = new ResolvedFilters(hk, filters.khoa(), filters.nganh(), filters.monHoc(), null);
            List<CourseClass> lops = layDanhSachLop(hkFilters, giangVienId);

            if (lops.isEmpty()) {
                result.add(new LearningAnalyticsDtos.XuHuongGpaHocKyDto(
                        hk.getId(), hk.getTen(), hk.getNamHoc(),
                        0, 0, 0, 0, 0,
                        null, null, 0, 0.0, 0, 0.0,
                        false, "Không có lớp học phần nào"
                ));
                continue;
            }

            int tongSoLop = lops.size();
            List<CourseClass> lopDaCongBo = lops.stream().filter(CourseClass::isBangDiemDaCongBo).toList();
            int soLopDaCongBo = lopDaCongBo.size();
            int tongDangKy = lops.stream().mapToInt(CourseClass::getSiSoHienTai).sum();

            List<Long> publishedIds = lopDaCongBo.stream().map(CourseClass::getId).toList();
            List<CourseRegistrationRepository.DiemTheoLopProjection> diemList = publishedIds.isEmpty()
                    ? List.of()
                    : dangKyHocPhanRepository.findDiemTheoDanhSachLop(publishedIds, RegistrationStatus.DA_DANG_KY);

            List<CourseRegistrationRepository.DiemTheoLopProjection> diemHopLe = diemList.stream()
                    .filter(d -> d.getDiemTongKet() != null && d.getDiemTongKet() >= 0.0 && d.getDiemTongKet() <= 10.0)
                    .toList();

            int mauSo = diemHopLe.size();
            int chuaCoDiem = tongDangKy - mauSo;

            if (mauSo == 0) {
                result.add(new LearningAnalyticsDtos.XuHuongGpaHocKyDto(
                        hk.getId(), hk.getTen(), hk.getNamHoc(),
                        tongSoLop, soLopDaCongBo, tongDangKy, 0, chuaCoDiem,
                        null, null, 0, 0.0, 0, 0.0,
                        false, soLopDaCongBo == 0 ? "Chưa công bố bảng điểm" : "Chưa có sinh viên có điểm hợp lệ"
                ));
                continue;
            }

            double sumDiem = 0.0;
            double sum4 = 0.0;
            int soDat = 0;
            int soTruot = 0;

            for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
                double x = d.getDiemTongKet();
                sumDiem += x;
                sum4 += quyDoiHe4(x);
                if (x >= NGUONG_DIEM_DAT) soDat++;
                else soTruot++;
            }

            Double diemTb = BigDecimal.valueOf(sumDiem)
                    .divide(BigDecimal.valueOf(mauSo), 2, RoundingMode.HALF_UP).doubleValue();
            Double gpa4 = BigDecimal.valueOf(sum4)
                    .divide(BigDecimal.valueOf(mauSo), 2, RoundingMode.HALF_UP).doubleValue();
            double tyLeDat = calcPercent(soDat, mauSo);
            double tyLeTruot = calcPercent(soTruot, mauSo);

            result.add(new LearningAnalyticsDtos.XuHuongGpaHocKyDto(
                    hk.getId(), hk.getTen(), hk.getNamHoc(),
                    tongSoLop, soLopDaCongBo, tongDangKy, mauSo, chuaCoDiem,
                    diemTb, gpa4, soDat, tyLeDat, soTruot, tyLeTruot,
                    true, "Đã có số liệu điểm công bố"
            ));
        }

        return result;
    }

    /**
     * So sánh các lớp cùng môn:
     * - Chỉ so sánh khi cùng môn, cùng học kỳ.
     * - Chỉ đối chiếu chính thức giữa các lớp CÙNG ĐÃ CÔNG BỐ ĐIỂM.
     * - Lớp chưa công bố được tách riêng với nhãn cảnh báo rõ ràng.
     * - Đánh giá cỡ mẫu M >= 5 (SO_MAU_TOI_THIEU_SO_SANH).
     */
    public LearningAnalyticsDtos.SoSanhCungMonDto tinhSoSanhCacLopCungMon(
            Subject monHoc, Semester hocKy, List<CourseClass> danhSachLop) {

        List<CourseClass> lopsCuaMon = danhSachLop.stream()
                .filter(l -> l.getMonHoc() != null && Objects.equals(l.getMonHoc().getId(), monHoc.getId()))
                .filter(l -> hocKy == null || (l.getHocKy() != null && Objects.equals(l.getHocKy().getId(), hocKy.getId())))
                .sorted(Comparator.comparing(CourseClass::getMa))
                .toList();

        List<Long> lopIds = lopsCuaMon.stream().map(CourseClass::getId).toList();
        List<CourseRegistrationRepository.DiemTheoLopProjection> allGrades = lopIds.isEmpty()
                ? List.of()
                : dangKyHocPhanRepository.findDiemTheoDanhSachLop(lopIds, RegistrationStatus.DA_DANG_KY);

        Map<Long, List<CourseRegistrationRepository.DiemTheoLopProjection>> gradesByLop = allGrades.stream()
                .collect(Collectors.groupingBy(CourseRegistrationRepository.DiemTheoLopProjection::getLopHocPhanId));

        List<LearningAnalyticsDtos.SoSanhLopItemDto> items = new ArrayList<>();
        int countDuDieuKien = 0;
        int countDaCongBo = 0;

        for (CourseClass lop : lopsCuaMon) {
            boolean daCongBo = lop.isBangDiemDaCongBo();
            if (daCongBo) countDaCongBo++;

            List<CourseRegistrationRepository.DiemTheoLopProjection> lopGrades = gradesByLop.getOrDefault(lop.getId(), List.of());
            List<CourseRegistrationRepository.DiemTheoLopProjection> diemHopLe = daCongBo
                    ? lopGrades.stream()
                    .filter(d -> d.getDiemTongKet() != null && d.getDiemTongKet() >= 0.0 && d.getDiemTongKet() <= 10.0)
                    .toList()
                    : List.of();

            int siSoToiDa = lop.getSiSoToiDa();
            int daDangKy = lop.getSiSoHienTai();
            int mauSo = diemHopLe.size();
            int chuaCoDiem = daDangKy - mauSo;

            Double diemTb = null;
            Double diemMin = null;
            Double diemMax = null;
            Double gpa4 = null;
            int soDat = 0;
            int soTruot = 0;

            if (mauSo > 0) {
                double sum = 0.0;
                double sum4 = 0.0;
                double min = 10.0;
                double max = 0.0;

                for (CourseRegistrationRepository.DiemTheoLopProjection d : diemHopLe) {
                    double x = d.getDiemTongKet();
                    sum += x;
                    sum4 += quyDoiHe4(x);
                    if (x < min) min = x;
                    if (x > max) max = x;
                    if (x >= NGUONG_DIEM_DAT) soDat++;
                    else soTruot++;
                }

                diemMin = min;
                diemMax = max;
                diemTb = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(mauSo), 2, RoundingMode.HALF_UP).doubleValue();
                gpa4 = BigDecimal.valueOf(sum4).divide(BigDecimal.valueOf(mauSo), 2, RoundingMode.HALF_UP).doubleValue();
            }

            double tyLeDat = mauSo == 0 ? 0.0 : calcPercent(soDat, mauSo);
            double tyLeTruot = mauSo == 0 ? 0.0 : calcPercent(soTruot, mauSo);

            List<LearningAnalyticsDtos.KhoangDiemPhanBoDto> phanBo = tinhPhanBo5Khoang(diemHopLe, mauSo);

            boolean duDieuKienSoSanh = daCongBo && mauSo > 0;
            boolean duSoMau = mauSo >= SO_MAU_TOI_THIEU_SO_SANH;

            String ghiChu;
            if (!daCongBo) {
                ghiChu = "Bảng điểm chưa công bố (" + moTaTrangThai(lop.getTrangThaiBangDiem())
                        + ") - Không được tính vào so sánh chính thức";
            } else if (mauSo == 0) {
                ghiChu = "Lớp đã công bố nhưng chưa có sinh viên có điểm tổng kết hợp lệ";
            } else if (!duSoMau) {
                ghiChu = "Cỡ mẫu nhỏ (M = " + mauSo + " < " + SO_MAU_TOI_THIEU_SO_SANH
                        + "): số liệu phân bố có thể biến động mạnh, cần thận trọng khi đối chiếu";
            } else {
                ghiChu = "Đủ điều kiện so sánh chính thức (Đã công bố, cỡ mẫu M >= " + SO_MAU_TOI_THIEU_SO_SANH + ")";
                countDuDieuKien++;
            }

            items.add(new LearningAnalyticsDtos.SoSanhLopItemDto(
                    lop.getId(),
                    lop.getMa(),
                    monHoc.getId(),
                    monHoc.getMa(),
                    monHoc.getTen(),
                    monHoc.getSoTinChi(),
                    lop.getGiangVien() != null ? lop.getGiangVien().getId() : null,
                    lop.getGiangVien() != null ? lop.getGiangVien().getHoTen() : "Chưa phân công",
                    lop.getHocKy() != null ? lop.getHocKy().getId() : null,
                    lop.getHocKy() != null ? lop.getHocKy().getTen() + " - " + lop.getHocKy().getNamHoc() : "",
                    lop.getTrangThaiBangDiem() != null ? lop.getTrangThaiBangDiem().name() : "NHAP",
                    moTaTrangThai(lop.getTrangThaiBangDiem()),
                    daCongBo,
                    siSoToiDa,
                    daDangKy,
                    mauSo,
                    chuaCoDiem,
                    diemTb,
                    diemMin,
                    diemMax,
                    gpa4,
                    soDat,
                    tyLeDat,
                    soTruot,
                    tyLeTruot,
                    phanBo,
                    duDieuKienSoSanh,
                    duSoMau,
                    ghiChu
            ));
        }

        boolean coTheSoSanh = countDuDieuKien >= 2;
        String thongDiep = lopsCuaMon.isEmpty()
                ? "Không có lớp học phần nào của môn học trong học kỳ này"
                : "Môn " + monHoc.getMa() + " có " + lopsCuaMon.size() + " lớp ("
                + countDaCongBo + " lớp đã công bố, " + countDuDieuKien + " lớp đủ điều kiện so sánh chính thức với cỡ mẫu >= "
                + SO_MAU_TOI_THIEU_SO_SANH + "). " + (coTheSoSanh ? "Đủ số liệu để so sánh đối chiếu." : "Chưa đủ tối thiểu 2 lớp đã công bố để thực hiện so sánh chính thức.");

        return new LearningAnalyticsDtos.SoSanhCungMonDto(
                monHoc.getId(),
                monHoc.getMa(),
                monHoc.getTen(),
                monHoc.getSoTinChi(),
                hocKy != null ? hocKy.getId() : null,
                hocKy != null ? hocKy.getTen() + " - " + hocKy.getNamHoc() : "Tất cả học kỳ",
                lopsCuaMon.size(),
                countDaCongBo,
                countDuDieuKien,
                coTheSoSanh,
                thongDiep,
                CANH_BAO_NHAN_QUA,
                items
        );
    }

    private List<GradeAnalyticsDtos.LuaChonMonDto> layDanhSachMonTrongPhamVi(List<CourseClass> danhSachLop) {
        Map<Subject, List<CourseClass>> map = danhSachLop.stream()
                .filter(l -> l.getMonHoc() != null)
                .collect(Collectors.groupingBy(CourseClass::getMonHoc, LinkedHashMap::new, Collectors.toList()));

        return map.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getMa()))
                .map(e -> {
                    Subject s = e.getKey();
                    return new GradeAnalyticsDtos.LuaChonMonDto(
                            s.getId(),
                            s.getMa(),
                            s.getTen(),
                            s.getSoTinChi(),
                            s.getKhoa() != null ? s.getKhoa().getId() : null,
                            s.getKhoa() != null ? s.getKhoa().getTen() : null,
                            e.getValue().size()
                    );
                })
                .toList();
    }

    // =========================================================================
    // Helpers & Validations
    // =========================================================================

    public double quyDoiHe4(double diem10) {
        if (diem10 >= 8.5) return 4.0;
        if (diem10 >= 8.0) return 3.5;
        if (diem10 >= 7.0) return 3.0;
        if (diem10 >= 6.5) return 2.5;
        if (diem10 >= 5.5) return 2.0;
        if (diem10 >= NGUONG_DIEM_DAT) return 1.5;
        if (diem10 >= 4.0) return 1.0;
        return 0.0;
    }

    private double calcPercent(int count, long total) {
        if (total == 0) return 0.0;
        return BigDecimal.valueOf(count * 100.0)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private String moTaTrangThai(Object status) {
        if (status == null) return "Bản nháp";
        String s = status.toString();
        return switch (s) {
            case "CHO_DUYET" -> "Chờ duyệt";
            case "TRA_VE" -> "Trả về sửa";
            case "DA_DUYET" -> "Đã duyệt";
            case "DA_CONG_BO" -> "Đã công bố";
            case "DA_KHOA" -> "Đã khóa";
            default -> "Bản nháp";
        };
    }

    private User yeuCauVaiTro(Role expectedRole) {
        User user = currentUser != null ? currentUser.get() : null;
        if (user == null) {
            throw ApiException.forbidden("Vui lòng đăng nhập để tiếp tục");
        }
        if (user.getRole() != expectedRole) {
            throw ApiException.forbidden("Không có quyền truy cập chức năng này");
        }
        return user;
    }

    private Semester layHocKy(Long hocKyId) {
        if (hocKyId == null) return null;
        return hocKyRepository.findById(hocKyId)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại"));
    }

    private List<CourseClass> layDanhSachLop(ResolvedFilters filters, Long giangVienId) {
        Long hocKyId = filters.hocKy() != null ? filters.hocKy().getId() : null;
        Long khoaId = filters.khoa() != null ? filters.khoa().getId() : null;
        Long monHocId = filters.monHoc() != null ? filters.monHoc().getId() : null;
        Long lopHocPhanId = filters.lopHocPhan() != null ? filters.lopHocPhan().getId() : null;

        if (giangVienId != null) {
            return lopHocPhanRepository.findForGradeAnalytics(hocKyId, khoaId, monHocId, giangVienId, lopHocPhanId);
        } else {
            return lopHocPhanRepository.findForAdminAnalytics(hocKyId, khoaId, monHocId, lopHocPhanId);
        }
    }

    private ResolvedFilters validateAndResolveFilters(
            Long hocKyId, Long khoaId, Long nganhId, Long monHocId, Long lopHocPhanId) {
        Semester hocKy = hocKyId != null ? hocKyRepository.findById(hocKyId)
                .orElseThrow(() -> ApiException.notFound("Học kỳ không tồn tại")) : null;

        Faculty khoa = khoaId != null ? khoaRepository.findById(khoaId)
                .orElseThrow(() -> ApiException.notFound("Khoa không tồn tại")) : null;

        Major nganh = null;
        if (nganhId != null) {
            nganh = nganhRepository.findById(nganhId)
                    .orElseThrow(() -> ApiException.notFound("Ngành không tồn tại"));
            if (khoa != null && nganh.getKhoa() != null && !Objects.equals(nganh.getKhoa().getId(), khoa.getId())) {
                throw ApiException.badRequest("Ngành đã chọn không thuộc khoa đang lọc");
            }
        }

        Subject monHoc = null;
        if (monHocId != null) {
            monHoc = monHocRepository.findById(monHocId)
                    .orElseThrow(() -> ApiException.notFound("Môn học không tồn tại"));
            if (khoa != null && monHoc.getKhoa() != null && !Objects.equals(monHoc.getKhoa().getId(), khoa.getId())) {
                throw ApiException.badRequest("Môn học đã chọn không thuộc khoa đang lọc");
            }
        }

        CourseClass lopHocPhan = null;
        if (lopHocPhanId != null) {
            lopHocPhan = lopHocPhanRepository.findById(lopHocPhanId)
                    .orElseThrow(() -> ApiException.notFound("Lớp học phần không tồn tại"));
            if (hocKy != null && lopHocPhan.getHocKy() != null && !Objects.equals(lopHocPhan.getHocKy().getId(), hocKy.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc học kỳ đang lọc");
            }
            if (monHoc != null && lopHocPhan.getMonHoc() != null && !Objects.equals(lopHocPhan.getMonHoc().getId(), monHoc.getId())) {
                throw ApiException.badRequest("Lớp học phần không thuộc môn học đang lọc");
            }
        }

        return new ResolvedFilters(hocKy, khoa, nganh, monHoc, lopHocPhan);
    }

    private record ResolvedFilters(Semester hocKy, Faculty khoa, Major nganh, Subject monHoc, CourseClass lopHocPhan) {
    }

    // =========================================================================
    // CSV Export Generator
    // =========================================================================

    private byte[] taoNoiDungCsv(LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto baoCao, String nguoiXuat) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM
        sb.append('\uFEFF');

        String thoiDiem = LocalDateTime.now(ExportSecuritySupport.VIETNAM_ZONE_ID)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        sb.append("# =====================================================================\n");
        sb.append("# BÁO CÁO PHÂN TÍCH HỌC TẬP (LEARNING ANALYTICS - QLSV)\n");
        sb.append("# Thời điểm xuất: ").append(thoiDiem).append("\n");
        sb.append("# Người thực hiện: ").append(nguoiXuat).append("\n");
        sb.append("# Học kỳ: ").append(baoCao.boLoc().tenHocKy()).append("\n");
        sb.append("# Khoa: ").append(baoCao.boLoc().tenKhoa()).append("\n");
        sb.append("# Ngành: ").append(baoCao.boLoc().tenNganh()).append("\n");
        sb.append("# Môn học: ").append(baoCao.boLoc().tenMonHoc()).append("\n");
        sb.append("# Chính sách điểm thiếu: ").append(CHINH_SACH_DIEM_THIEU).append("\n");
        sb.append("# Cảnh báo nhân quả: ").append(CANH_BAO_NHAN_QUA.replace("\n", " ")).append("\n");
        sb.append("# Bảo mật: Không chứa thông tin định danh cá nhân sinh viên (Zero PII)\n");
        sb.append("# =====================================================================\n\n");

        // Khối 1: Tổng quan KPI
        sb.append("--- PHẦN 1: TỔNG QUAN KPI HỌC TẬP ---\n");
        sb.append("Chỉ số KPI,Tử số,Mẫu số,Giá trị,Đơn vị,Ghi chú\n");
        LearningAnalyticsDtos.ChiSoKpiHocTapDto kpi = baoCao.kpi();
        sb.append("Tổng số lớp học phần,").append(kpi.tongSoLop()).append(",-,")
                .append(kpi.tongSoLop()).append(",Lớp,Bao gồm ").append(kpi.soLopDaCongBo()).append(" lớp công bố và ")
                .append(kpi.soLopChuaCongBo()).append(" lớp chưa công bố\n");
        sb.append("Tổng lượt đăng ký,").append(kpi.tongSoDangKyHieuLuc()).append(",-,")
                .append(kpi.tongSoDangKyHieuLuc()).append(",Lượt,Đăng ký trạng thái DA_DANG_KY\n");
        sb.append("Mẫu số được tính (M),").append(kpi.mauSoDuocTinh()).append(",")
                .append(kpi.tongSoDangKyHieuLuc()).append(",").append(kpi.mauSoDuocTinh())
                .append(",Sinh viên,Chỉ tính SV có điểm hợp lệ thuộc lớp đã công bố\n");
        sb.append("Sinh viên chưa có điểm công bố,").append(kpi.soSinhVienChuaCoDiem()).append(",")
                .append(kpi.tongSoDangKyHieuLuc()).append(",").append(kpi.soSinhVienChuaCoDiem())
                .append(",Sinh viên,Loại khỏi mẫu số; không tự tính thành 0.0\n");
        sb.append("Điểm trung bình (Thang 10),-,M=").append(kpi.mauSoDuocTinh()).append(",")
                .append(kpi.diemTrungBinh() != null ? kpi.diemTrungBinh() : "Chưa có").append(",Điểm,Trung bình cộng điểm tổng kết\n");
        sb.append("GPA tương đương (Thang 4),-,M=").append(kpi.mauSoDuocTinh()).append(",")
                .append(kpi.gpaThang4() != null ? kpi.gpaThang4() : "Chưa có").append(",Điểm,Quy đổi thang 4 theo quy chế\n");
        sb.append("Tỷ lệ Đạt (>= 5.0),").append(kpi.soSinhVienDat()).append(",M=").append(kpi.mauSoDuocTinh()).append(",")
                .append(kpi.tyLeDat()).append(",%,").append(kpi.soSinhVienDat()).append(" sinh viên đạt\n");
        sb.append("Tỷ lệ Trượt (< 5.0),").append(kpi.soSinhVienTruot()).append(",M=").append(kpi.mauSoDuocTinh()).append(",")
                .append(kpi.tyLeTruot()).append(",%,").append(kpi.soSinhVienTruot()).append(" sinh viên trượt\n\n");

        // Khối 2: Phân bố khoảng điểm
        sb.append("--- PHẦN 2: PHÂN BỐ KHOẢNG ĐIỂM (M=").append(kpi.mauSoDuocTinh()).append(") ---\n");
        sb.append("Mã khoảng,Ký hiệu khoảng,Mô tả,Số lượng SV,Tỷ lệ %,Đạt / Chưa đạt\n");
        for (LearningAnalyticsDtos.KhoangDiemPhanBoDto b : baoCao.phanBoDiem()) {
            sb.append(b.maKhoang()).append(",\"").append(b.kyHieuKhoang()).append("\",\"")
                    .append(b.tenKhoang()).append("\",").append(b.soLuong()).append(",")
                    .append(b.tyLePhanTram()).append("%,")
                    .append(b.laKhoangDat() ? "ĐẠT" : "CHƯA ĐẠT").append("\n");
        }
        sb.append("\n");

        // Khối 3: Xu hướng GPA theo học kỳ
        sb.append("--- PHẦN 3: XU HƯỚNG GPA THEO HỌC KỲ ---\n");
        sb.append("Học kỳ,Năm học,Số lớp công bố / Tổng lớp,Mẫu số M (Có điểm),Chưa có điểm,Điểm TB (10),GPA (4),Tỷ lệ Đạt %,Tỷ lệ Trượt %,Trạng thái\n");
        for (LearningAnalyticsDtos.XuHuongGpaHocKyDto xh : baoCao.xuHuongTheoHocKy()) {
            sb.append("\"").append(xh.tenHocKy()).append("\",\"")
                    .append(xh.namHoc()).append("\",\"")
                    .append(xh.soLopDaCongBo()).append("/").append(xh.tongSoLop()).append("\",")
                    .append(xh.mauSoDuocTinh()).append(",")
                    .append(xh.soSinhVienChuaCoDiem()).append(",")
                    .append(xh.diemTrungBinh() != null ? xh.diemTrungBinh() : "-").append(",")
                    .append(xh.gpaThang4() != null ? xh.gpaThang4() : "-").append(",")
                    .append(xh.tyLeDat()).append("%,")
                    .append(xh.tyLeTruot()).append("%,\"")
                    .append(xh.ghiChu()).append("\"\n");
        }
        sb.append("\n");

        // Khối 4: So sánh các lớp cùng môn
        if (baoCao.soSanhCungMon() != null) {
            LearningAnalyticsDtos.SoSanhCungMonDto ss = baoCao.soSanhCungMon();
            sb.append("--- PHẦN 4: SO SÁNH CÁC LỚP CÙNG MÔN (").append(ss.maMon()).append(" - ").append(ss.tenMon()).append(") ---\n");
            sb.append("# Thông điệp: ").append(ss.thongDiepSoSanh()).append("\n");
            sb.append("Mã lớp,Giảng viên,Sĩ số tối đa,Đã đăng ký,Mẫu số M,Chưa có điểm,Trạng thái bảng điểm,Đủ điều kiện so sánh,Điểm TB,Điểm Min - Max,GPA (4),Tỷ lệ Đạt %,Ghi chú điều kiện\n");
            for (LearningAnalyticsDtos.SoSanhLopItemDto lop : ss.danhSachLop()) {
                sb.append("\"").append(lop.maLop()).append("\",\"")
                        .append(lop.tenGiangVien()).append("\",")
                        .append(lop.siSoToiDa()).append(",")
                        .append(lop.daDangKy()).append(",")
                        .append(lop.mauSo()).append(",")
                        .append(lop.soSinhVienChuaCoDiem()).append(",\"")
                        .append(lop.moTaTrangThai()).append("\",")
                        .append(lop.duDieuKienSoSanhChinhThuc() && lop.duSoMauToiThieu() ? "CÓ" : "KHÔNG").append(",")
                        .append(lop.diemTrungBinh() != null ? lop.diemTrungBinh() : "-").append(",\"")
                        .append(lop.diemThapNhat() != null ? lop.diemThapNhat() + " - " + lop.diemCaoNhat() : "-").append("\",")
                        .append(lop.gpaThang4() != null ? lop.gpaThang4() : "-").append(",")
                        .append(lop.tyLeDat()).append("%,\"")
                        .append(lop.ghiChuDieuKien()).append("\"\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
