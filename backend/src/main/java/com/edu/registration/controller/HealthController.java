package com.edu.registration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint kiểm tra tình trạng sức khỏe hệ thống (Health Check) (QLSV-94 / TASK-OP-07 Work Package E).
 * Tuyệt đối không để lộ mật khẩu, chuỗi kết nối cơ sở dữ liệu, token hoặc cấu hình môi trường.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Health Check", description = "Kiểm tra tình trạng sẵn sàng của hệ thống")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    @Operation(summary = "Kiểm tra trạng thái hoạt động của ứng dụng và các dịch vụ nền tảng")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("service", "quan-ly-dao-tao");
        response.put("timestamp", Instant.now().toString());

        boolean dbOk = checkDatabase();
        boolean storageOk = checkStorage();

        response.put("database", dbOk ? "UP" : "DOWN");
        response.put("storage", storageOk ? "UP" : "DEGRADED");

        if (dbOk) {
            response.put("status", "UP");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }

    private boolean checkDatabase() {
        if (dataSource == null) {
            return false;
        }
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception ex) {
            log.error("Kiểm tra kết nối cơ sở dữ liệu thất bại");
            return false;
        }
    }

    private boolean checkStorage() {
        try {
            File dir = new File("./data");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            return dir.canWrite();
        } catch (Exception ex) {
            return false;
        }
    }
}
