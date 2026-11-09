package com.edu.registration.controller;

import com.edu.registration.dto.GradeAnalyticsDtos;
import com.edu.registration.dto.StatDtos;
import com.edu.registration.service.GradeAnalyticsService;
import com.edu.registration.service.StatisticsService;
import com.edu.registration.util.ExportSecuritySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminStatisticsController {

    private final StatisticsService thongKeService;
    private final GradeAnalyticsService gradeAnalyticsService;

    public AdminStatisticsController(StatisticsService thongKeService) {
        this(thongKeService, null);
    }

    @Autowired
    public AdminStatisticsController(StatisticsService thongKeService,
                                     GradeAnalyticsService gradeAnalyticsService) {
        this.thongKeService = thongKeService;
        this.gradeAnalyticsService = gradeAnalyticsService;
    }

    @GetMapping("/thong-ke/tong-quan")
    public StatDtos.TongQuanDto tongQuan() {
        return thongKeService.tongQuan();
    }

    @GetMapping("/thong-ke/theo-mon")
    public List<StatDtos.ThongKeMonDto> theoMon(@RequestParam(required = false) Long hocKyId,
                                                @RequestParam(required = false) Long khoaId,
                                                @RequestParam(required = false) Long nganhId) {
        return thongKeService.theoMon(hocKyId, khoaId, nganhId);
    }

    @GetMapping("/thong-ke/hanh-dong")
    public StatDtos.HanhDongHocVuDto hanhDong(@RequestParam(required = false) Long hocKyId,
                                              @RequestParam(required = false) Long khoaId) {
        return thongKeService.hanhDong(hocKyId, khoaId);
    }

    /**
     * Dashboard KPI tổng hợp toàn diện (QLSV-86 / TASK-AD-11):
     * Cung cấp số đăng ký, tỷ lệ lấp đầy, đạt/trượt, GPA, tiến độ học tập và chi tiết theo môn
     * từ cùng một nguồn tính toán (Single Source of Truth), chỉ dùng điểm đã công bố.
     */
    @GetMapping("/thong-ke/kpi-dashboard")
    public StatDtos.DashboardKpiResponseDto layKpiDashboard(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long nganhId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId) {
        return thongKeService.layDashboardKpi(hocKyId, khoaId, nganhId, monHocId, lopHocPhanId);
    }

    /**
     * Xuất file CSV báo cáo thống kê chính xác theo bộ lọc đang xem (QLSV-86 / TASK-AD-11 / QLSV-91).
     * Khớp 100% với số liệu trên Dashboard, có timestamp, UTF-8 BOM, Zero PII.
     */
    @GetMapping({"/thong-ke/export", "/thong-ke/xuat-csv"})
    public ResponseEntity<byte[]> xuatBaoCaoCsv(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long nganhId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(defaultValue = "CSV") String dinhDang) {
        byte[] duLieu = thongKeService.xuatBaoCaoThongKeCsv(hocKyId, khoaId, nganhId, monHocId, lopHocPhanId);
        String timestamp = LocalDateTime.now(ExportSecuritySupport.VIETNAM_ZONE_ID)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao-cao-thong-ke-" + timestamp + ".csv\"");
        headers.setCacheControl(CacheControl.noStore());
        return new ResponseEntity<>(duLieu, headers, HttpStatus.OK);
    }

    @GetMapping({"/thong-ke/diem-lop", "/thong-ke-diem"})
    public GradeAnalyticsDtos.BaoCaoThongKeDiemDto thongKeDiemLop(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId) {
        return gradeAnalyticsService.thongKeChoQuanTri(hocKyId, khoaId, monHocId, lopHocPhanId);
    }

    @GetMapping("/lop-hoc-phan/{id}/thong-ke-diem")
    public GradeAnalyticsDtos.ThongKeDiemLopItemDto thongKeDiemMotLop(@PathVariable Long id) {
        return gradeAnalyticsService.thongKeMotLop(id);
    }

    @GetMapping("/lop-hoc-phan/{id}/csv")
    public ResponseEntity<byte[]> xuatCsv(
            @PathVariable Long id,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String sapXep) {
        StatisticsService.ClassExportResult ketQua = thongKeService.xuatDanhSachLopChiTiet(id, tuKhoa, sapXep);
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(ketQua.tenTep(), MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return new ResponseEntity<>(ketQua.noiDung(), headers, HttpStatus.OK);
    }
}
