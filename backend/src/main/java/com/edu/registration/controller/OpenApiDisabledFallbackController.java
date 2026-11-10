package com.edu.registration.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Endpoint xử lý an toàn khi OpenAPI / Swagger UI bị tắt trên môi trường sản xuất (Production)
 * hoặc khi biến cấu hình {@code springdoc.api-docs.enabled=false}.
 * Đảm bảo không tiết lộ cấu trúc nội bộ, mã nguồn hoặc stack trace, trả về mã lỗi 404 và thông điệp an toàn.
 */
@RestController
@Hidden
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "false")
public class OpenApiDisabledFallbackController {

    @RequestMapping(value = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"
    })
    public ResponseEntity<Map<String, Object>> openApiDisabled() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "message", "Tài liệu API không khả dụng trên môi trường hiện tại (OpenAPI/Swagger UI đã được vô hiệu hóa vì lý do an toàn thông tin).",
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.NOT_FOUND.value()
                ));
    }
}
