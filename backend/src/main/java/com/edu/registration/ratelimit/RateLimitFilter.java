package com.edu.registration.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Filter kiểm soát tần suất gửi yêu cầu, bảo vệ hệ thống khỏi tấn công brute-force và DoS (QLSV-102 / TASK-OP-07B).
 * Hỗ trợ Dual-Key Rate Limiting (Account & IP), xác minh Proxy tin cậy, và chuẩn phản hồi HTTP 429 Too Many Requests.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Bỏ qua rate limit cho endpoint health check, chatbot status, Swagger UI và OpenAPI docs
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Xác định địa chỉ IP máy khách với kiểm tra Proxy tin cậy (ngăn chặn giả mạo X-Forwarded-For)
        String clientIp = ClientIpResolver.resolveClientIp(
                request,
                rateLimitService.getProperties().getTrustedProxies()
        );

        // 1. Điểm cuối đăng nhập: POST /api/auth/login -> Áp dụng Dual-Key Limiting (Account + IP)
        if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            handleLoginRateLimit(request, response, filterChain, clientIp);
            return;
        }

        // 2. Điểm cuối tra cứu / xác thực văn bằng công khai: /api/public/bang-diem/xac-thuc/**
        if (path.startsWith("/api/public/bang-diem/xac-thuc/")) {
            RateLimitResult result = rateLimitService.checkVerifyLimit(clientIp);
            if (!result.allowed()) {
                sendRateLimitResponse(response, result);
                return;
            }
            appendRateLimitHeaders(response, result);
            filterChain.doFilter(request, response);
            return;
        }

        // Với các endpoint khác thuộc /api/**
        if (path.startsWith("/api/")) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String identifier = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                    ? "user:" + auth.getName()
                    : "ip:" + clientIp;

            RateLimitResult result;

            // 3. Điểm cuối Chatbot AI
            if (path.startsWith("/api/chatbot/")) {
                result = rateLimitService.checkChatbotLimit(identifier);
            }
            // 4. Điểm cuối tốn tài nguyên tính toán / I/O nặng
            else if (isHeavyEndpoint(path)) {
                result = rateLimitService.checkHeavyLimit(identifier);
            }
            // 5. Điểm cuối API nghiệp vụ thông thường
            else {
                result = rateLimitService.checkApiLimit(identifier);
            }

            if (!result.allowed()) {
                sendRateLimitResponse(response, result);
                return;
            }

            appendRateLimitHeaders(response, result);
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleLoginRateLimit(HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain,
                                      String clientIp) throws ServletException, IOException {
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);

        // Trích xuất tên đăng nhập từ JSON payload an toàn (tuyệt đối không log mật khẩu / body)
        String username = extractUsernameFromLoginBody(wrappedRequest.getCachedBody());

        // Kiểm tra ngưỡng theo tài khoản nếu có username (chống brute-force tài khoản mục tiêu)
        if (username != null && !username.isBlank()) {
            RateLimitResult accountResult = rateLimitService.checkLoginAccountLimit(username);
            if (!accountResult.allowed()) {
                sendRateLimitResponse(response, accountResult);
                return;
            }
        }

        // Kiểm tra ngưỡng theo IP (chống tràn lụt DoS, nhưng dung lượng đủ lớn để không khóa IP dùng chung)
        RateLimitResult ipResult = rateLimitService.checkLoginIpLimit(clientIp);
        if (!ipResult.allowed()) {
            sendRateLimitResponse(response, ipResult);
            return;
        }

        appendRateLimitHeaders(response, ipResult);
        filterChain.doFilter(wrappedRequest, response);
    }

    private String extractUsernameFromLoginBody(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root != null && root.has("username") && !root.get("username").isNull()) {
                return root.get("username").asText();
            }
        } catch (Exception ignored) {
            // Body không phải JSON hoặc cấu trúc sai; controller sẽ trả lỗi 400 chuẩn
        }
        return null;
    }

    private void appendRateLimitHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
    }

    private void sendRateLimitResponse(HttpServletResponse response, RateLimitResult result) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("message", "Quá nhiều yêu cầu gửi tới hệ thống. Vui lòng thử lại sau " + result.retryAfterSeconds() + " giây.");
        errorBody.put("timestamp", Instant.now().toString());
        errorBody.put("retryAfterSeconds", result.retryAfterSeconds());

        objectMapper.writeValue(response.getWriter(), errorBody);
    }

    private boolean isExcludedPath(String path) {
        return path == null
                || path.equals("/api/health")
                || path.equals("/api/chatbot/status")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    private boolean isHeavyEndpoint(String path) {
        return path.contains("/bang-diem/pdf")
                || path.endsWith("/export-csv")
                || path.endsWith("/export-xlsx")
                || path.endsWith("/import-csv")
                || path.endsWith("/import-xlsx")
                || path.startsWith("/api/admin/background-jobs/run-all");
    }
}
