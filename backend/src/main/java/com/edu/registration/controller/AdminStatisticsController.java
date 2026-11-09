package com.edu.registration.controller;

import com.edu.registration.dto.GradeAnalyticsDtos;
import com.edu.registration.dto.StatDtos;
import com.edu.registration.service.GradeAnalyticsService;
import com.edu.registration.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
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
                                                @RequestParam(required = false) Long khoaId) {
        return thongKeService.theoMon(hocKyId, khoaId);
    }

    @GetMapping("/thong-ke/hanh-dong")
    public StatDtos.HanhDongHocVuDto hanhDong(@RequestParam(required = false) Long hocKyId,
                                              @RequestParam(required = false) Long khoaId) {
        return thongKeService.hanhDong(hocKyId, khoaId);
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
    public ResponseEntity<byte[]> xuatCsv(@PathVariable Long id) {
        byte[] duLieu = thongKeService.xuatDanhSachLop(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lop-hoc-phan-" + id + ".csv");
        return new ResponseEntity<>(duLieu, headers, HttpStatus.OK);
    }
}
