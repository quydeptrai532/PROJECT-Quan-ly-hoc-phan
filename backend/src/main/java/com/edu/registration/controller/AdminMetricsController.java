package com.edu.registration.controller;

import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.repository.*;
import com.edu.registration.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint cung cấp chỉ số vận hành và tài nguyên hệ thống (Operational Metrics) (QLSV-94 / TASK-OP-07 Work Package E).
 * Chỉ người dùng có vai trò QUAN_TRI mới có quyền truy cập, bảo mật thông tin nội bộ.
 */
@RestController
@RequestMapping("/api/admin/metrics")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Admin Metrics", description = "Chỉ số vận hành hệ thống và tài nguyên máy chủ")
public class AdminMetricsController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final SubjectRepository subjectRepository;
    private final CourseClassRepository courseClassRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final SupportRequestRepository supportRequestRepository;
    private final BackgroundJobRepository backgroundJobRepository;
    private final CacheManagementService cacheManagementService;

    public AdminMetricsController(UserRepository userRepository,
                                  StudentRepository studentRepository,
                                  LecturerRepository lecturerRepository,
                                  SubjectRepository subjectRepository,
                                  CourseClassRepository courseClassRepository,
                                  CourseRegistrationRepository registrationRepository,
                                  SupportRequestRepository supportRequestRepository,
                                  BackgroundJobRepository backgroundJobRepository,
                                  CacheManagementService cacheManagementService) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.subjectRepository = subjectRepository;
        this.courseClassRepository = courseClassRepository;
        this.registrationRepository = registrationRepository;
        this.supportRequestRepository = supportRequestRepository;
        this.backgroundJobRepository = backgroundJobRepository;
        this.cacheManagementService = cacheManagementService;
    }

    @GetMapping
    @Operation(summary = "Lấy các chỉ số tài nguyên máy chủ và thống kê vận hành thời gian thực")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("timestamp", Instant.now().toString());

        // 1. Chỉ số JVM và tài nguyên máy chủ
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        jvm.put("availableProcessors", runtime.availableProcessors());
        jvm.put("activeThreadCount", Thread.activeCount());
        jvm.put("memoryUsedBytes", usedMemory);
        jvm.put("memoryFreeBytes", freeMemory);
        jvm.put("memoryTotalBytes", totalMemory);
        jvm.put("memoryMaxBytes", maxMemory);
        jvm.put("memoryUsedPercent", maxMemory > 0 ? (usedMemory * 100 / maxMemory) : 0);
        metrics.put("jvm", jvm);

        // 2. Thống kê quy mô dữ liệu nghiệp vụ
        Map<String, Object> business = new LinkedHashMap<>();
        business.put("totalUsers", userRepository.count());
        business.put("totalStudents", studentRepository.count());
        business.put("totalLecturers", lecturerRepository.count());
        business.put("totalSubjects", subjectRepository.count());
        business.put("totalClasses", courseClassRepository.count());
        business.put("totalRegistrations", registrationRepository.count());
        business.put("totalSupportRequests", supportRequestRepository.count());
        business.put("unassignedSupportRequests", supportRequestRepository.countByNguoiXuLyIsNull());

        long activeJobs = backgroundJobRepository.countByStatus(BackgroundJobStatus.PENDING)
                + backgroundJobRepository.countByStatus(BackgroundJobStatus.RUNNING);
        business.put("activeBackgroundJobs", activeJobs);
        metrics.put("business", business);

        // 3. Thông tin bộ nhớ đệm (Cache)
        Map<String, Object> cacheInfo = new LinkedHashMap<>();
        cacheInfo.put("activeCaches", cacheManagementService.getCacheNames());
        metrics.put("cache", cacheInfo);

        return ResponseEntity.ok(metrics);
    }
}
