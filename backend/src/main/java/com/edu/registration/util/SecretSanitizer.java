package com.edu.registration.util;

import java.util.regex.Pattern;

/**
 * Tiện ích làm sạch thông điệp lỗi và dữ liệu tác vụ, ngăn lộ lọt mật khẩu, token, secret vào nhật ký và cơ sở dữ liệu (QLSV-93 / TASK-OP-06).
 */
public final class SecretSanitizer {

    private static final Pattern BEARER_TOKEN_PATTERN =
            Pattern.compile("Bearer\\s+[A-Za-z0-9\\-_.]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern JWT_PATTERN =
            Pattern.compile("eyJ[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+");

    private static final Pattern PASSWORD_KEY_VALUE_PATTERN =
            Pattern.compile("(?i)(password|passwd|pwd|secret|api[_-]?key|access[_-]?token)\\s*[:=]\\s*[\"']?([^\\s,\"'>]+)[\"']?");

    private static final Pattern DB_CREDENTIAL_PATTERN =
            Pattern.compile("(?i)(jdbc:[^:]+://)([^:]+):([^@]+)@");

    private SecretSanitizer() {
    }

    /**
     * Khử nhạy cảm chuỗi văn bản hoặc thông điệp lỗi.
     */
    public static String sanitize(String input, int maxLength) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String cleaned = input;
        cleaned = BEARER_TOKEN_PATTERN.matcher(cleaned).replaceAll("Bearer ***REDACTED***");
        cleaned = JWT_PATTERN.matcher(cleaned).replaceAll("***JWT_REDACTED***");
        cleaned = PASSWORD_KEY_VALUE_PATTERN.matcher(cleaned).replaceAll("$1=***REDACTED***");
        cleaned = DB_CREDENTIAL_PATTERN.matcher(cleaned).replaceAll("$1$2:***REDACTED***@");

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength - 3) + "...";
        }
        return cleaned;
    }

    public static String sanitize(String input) {
        return sanitize(input, 2000);
    }
}
