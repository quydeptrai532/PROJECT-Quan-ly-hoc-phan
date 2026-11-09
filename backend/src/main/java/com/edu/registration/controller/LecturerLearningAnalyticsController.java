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
@RequestMapping("/api/giang-vien/learning-analytics")
@PreAuthorize("hasRole('GIANG_VIEN')")
@Tag(name = "Lecturer Learning Analytics", description = "Phân tích học tập trong phạm vi phụ trách của giảng viên (QLSV-98 / TASK-OP-11)")
public class LecturerLearningAnalyticsController {

    private final LearningAnalyticsService learningAnalyticsService;

    public LecturerLearningAnalyticsController(LearningAnalyticsService learningAnalyticsService) {
        this.learningAnalyticsService = learningAnalyticsService;
    }

    @GetMapping({"", "/tong-hop"})
    @Operation(summary = "Báo cáo phân tích học tập các lớp giảng viên phụ trách")
    public ResponseEntity<LearningAnalyticsDtos.BaoCaoLearningAnalyticsDto> layBaoCao(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) Long lopHocPhanId) {
        return ResponseEntity.ok(learningAnalyticsService.layBaoCaoChoGiangVien(
                hocKyId, monHocId, lopHocPhanId));
    }

    @GetMapping("/xu-huong-gpa")
    @Operation(summary = "Xu hướng GPA các lớp do giảng viên phụ trách qua các học kỳ")
    public ResponseEntity<List<LearningAnalyticsDtos.XuHuongGpaHocKyDto>> layXuHuongGpa(
            @RequestParam(required = false) Long monHocId) {
        return ResponseEntity.ok(learningAnalyticsService.layXuHuongGpaChoGiangVien(monHocId));
    }

    @GetMapping("/so-sanh-lop")
    @Operation(summary = "So sánh các lớp cùng môn do giảng viên phụ trách trong học kỳ")
    public ResponseEntity<LearningAnalyticsDtos.SoSanhCungMonDto> laySoSanhLopCungMon(
            @RequestParam Long monHocId,
            @RequestParam(required = false) Long hocKyId) {
        return ResponseEntity.ok(learningAnalyticsService.laySoSanhCungMonChoGiangVien(monHocId, hocKyId));
    }

    @GetMapping({"/xuat-csv", "/export"})
    @Operation(summary = "Xuất file CSV báo cáo phân tích học tập các lớp phụ trách")
    public ResponseEntity<byte[]> xuatCsv(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long monHocId) {
        byte[] csv = learningAnalyticsService.xuatCsvChoGiangVien(hocKyId, monHocId);
        String timestamp = LocalDateTime.now(ExportSecuritySupport.VIETNAM_ZONE_ID)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"giang-vien-learning-analytics-" + timestamp + ".csv\"");
        headers.setCacheControl(CacheControl.noStore());

        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }
}
