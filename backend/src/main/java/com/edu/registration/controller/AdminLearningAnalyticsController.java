package com.edu.registration.controller;

import com.edu.registration.dto.LearningAnalyticsDtos;
import com.edu.registration.service.LearningAnalyticsService;
import com.edu.registration.util.ExportSecuritySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin/learning-analytics")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Admin Learning Analytics", description = "Phân tích học tập cơ bản cấp quản trị (QLSV-98 / TASK-OP-11)")
public class AdminLearningAnalyticsController {

    private final LearningAnalyticsService learningAnalyticsService;

    public AdminLearningAnalyticsController(LearningAnalyticsService learningAnalyticsService) {
        this.learningAnalyticsService = learningAnalyticsService;
    }

    @GetMapping({"", "/tong-hop"})
    @Operation(summary = "Báo cáo phân tích học tập tổng hợp: KPI, phân bố điểm, xu hướng GPA và so sánh lớp")
    public ResponseEntity<LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto> layBaoCaoTongHop(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long nganhId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId) {
        return ResponseEntity.ok(learningAnalyticsService.layBaoCaoChoQuanTri(
                hocKyId, khoaId, nganhId, monHocId, lopHocPhanId));
    }

    @GetMapping("/xu-huong-gpa")
    @Operation(summary = "Xu hướng điểm trung bình và GPA qua tất cả các học kỳ theo thứ tự thời gian")
    public ResponseEntity<List<LearningAnalyticsDtos.XuHuongGpaHocKyDto>> layXuHuongGpa(
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long nganhId,
            @RequestParam(required = false) Long monHocId) {
        return ResponseEntity.ok(learningAnalyticsService.layXuHuongGpaChoQuanTri(khoaId, nganhId, monHocId));
    }

    @GetMapping("/so-sanh-lop")
    @Operation(summary = "So sánh đối chiếu các lớp cùng môn trong một học kỳ (khi đủ điều kiện công bố và cỡ mẫu)")
    public ResponseEntity<LearningAnalyticsDtos.SoSanhCungMonDto> laySoSanhLopCungMon(
            @RequestParam Long monHocId,
            @RequestParam(required = false) Long hocKyId) {
        return ResponseEntity.ok(learningAnalyticsService.laySoSanhCungMonChoQuanTri(monHocId, hocKyId));
    }

    @GetMapping({"/xuat-csv", "/export"})
    @Operation(summary = "Xuất file CSV báo cáo phân tích học tập theo đúng bộ lọc đang chọn")
    public ResponseEntity<byte[]> xuatCsv(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long khoaId,
            @RequestParam(required = false) Long nganhId,
            @RequestParam(required = false) Long monHocId) {
        byte[] csv = learningAnalyticsService.xuatCsvChoQuanTri(hocKyId, khoaId, nganhId, monHocId);
        String timestamp = LocalDateTime.now(ExportSecuritySupport.VIETNAM_ZONE_ID)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"learning-analytics-" + timestamp + ".csv\"");
        headers.setCacheControl(CacheControl.noStore());

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}
