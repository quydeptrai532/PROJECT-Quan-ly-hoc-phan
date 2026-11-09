package com.edu.registration.controller;

import com.edu.registration.dto.BackgroundJobDtos;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.service.job.BackgroundJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoint tra cứu, giám sát và vận hành tác vụ nền dành riêng cho Quản trị viên (QLSV-93 / TASK-OP-06).
 * Được bảo vệ chặt chẽ bằng quyền QUAN_TRI, ngăn chặn người dùng thường truy cập thông tin hạ tầng nội bộ.
 */
@RestController
@RequestMapping("/api/admin/background-jobs")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminBackgroundJobController {

    private final BackgroundJobService jobService;

    public AdminBackgroundJobController(BackgroundJobService jobService) {
        this.jobService = jobService;
    }

    /**
     * Tra cứu danh sách tác vụ nền phân trang và lọc theo trạng thái/loại tác vụ.
     */
    @GetMapping
    public Page<BackgroundJobDtos.JobSummaryDto> list(
            @RequestParam(required = false) BackgroundJobStatus status,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int limitedSize = Math.min(Math.max(1, size), 50);
        Pageable pageable = PageRequest.of(Math.max(0, page), limitedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        return jobService.listJobs(status, jobType, pageable)
                .map(BackgroundJobDtos.JobSummaryDto::fromEntity);
    }

    /**
     * Tra cứu chi tiết một tác vụ nền cụ thể.
     */
    @GetMapping("/{id}")
    public BackgroundJobDtos.JobDetailDto getDetail(@PathVariable Long id) {
        BackgroundJob job = jobService.getJob(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy tác vụ với id=" + id));
        return BackgroundJobDtos.JobDetailDto.fromEntity(job);
    }

    /**
     * Kích hoạt thử lại thủ công một tác vụ thất bại.
     */
    @PostMapping("/{id}/retry")
    public BackgroundJobDtos.JobDetailDto retry(@PathVariable Long id) {
        BackgroundJob job = jobService.retryJob(id);
        return BackgroundJobDtos.JobDetailDto.fromEntity(job);
    }

    /**
     * Hủy tác vụ nền đang chờ hoặc đã thất bại.
     */
    @PostMapping("/{id}/cancel")
    public BackgroundJobDtos.JobDetailDto cancel(
            @PathVariable Long id,
            @RequestBody(required = false) BackgroundJobDtos.CancelJobRequest request) {

        String reason = request != null ? request.reason() : null;
        BackgroundJob job = jobService.cancelJob(id, reason);
        return BackgroundJobDtos.JobDetailDto.fromEntity(job);
    }

    /**
     * Báo cáo thống kê tổng hợp số lượng tác vụ theo từng trạng thái phục vụ dashboard vận hành.
     */
    @GetMapping("/stats")
    public BackgroundJobDtos.JobStatsDto getStats() {
        Map<String, Long> stats = jobService.getStatistics();
        return new BackgroundJobDtos.JobStatsDto(
                stats.getOrDefault("pending", 0L),
                stats.getOrDefault("running", 0L),
                stats.getOrDefault("succeeded", 0L),
                stats.getOrDefault("failed", 0L),
                stats.getOrDefault("total", 0L)
        );
    }
}
