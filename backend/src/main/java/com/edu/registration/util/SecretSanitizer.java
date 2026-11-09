package com.edu.registration.util;

import java.util.regex.Pattern;

/**
 * Tiện ích làm sạch thông điệp lỗi và dữ liệu tác vụ, ngăn lộ lọt mật khẩu, token, secret,
 * và điểm số cá nhân sinh viên vào nhật ký và phản hồi lỗi (QLSV-93 / QLSV-94 / TASK-OP-07 Work Package E).
 */
public final class SecretSanitizer {

    private static final Pattern BEARER_TOKEN_PATTERN =
            Pattern.compile("Bearer\\s+[A-Za-z0-9\\-_.]+", Pattern.CASE_INSENSITIVE);

    private static final Pattern JWT_PATTERN =
            Pattern.compile("eyJ[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+\\.[A-Za-z0-9\\-_=]+");

    private static final Pattern PASSWORD_KEY_VALUE_PATTERN =
            Pattern.compile("(?i)\\b(password|passwd|pwd|secret|api[_-]?key|access[_-]?token|ma[_-]?xac[_-]?minh|verification[_-]?code)\\b\\s*[:=]\\s*[\"']?([^\\s,\"'>]+)[\"']?");

    private static final Pattern DB_CREDENTIAL_PATTERN =
            Pattern.compile("(?i)(jdbc:[^:]+://)([^:]+):([^@]+)@");

    // Ngăn chặn rò rỉ điểm số cá nhân sinh viên vào log/lỗi theo yêu cầu QLSV-94 Work Package E
    private static final Pattern PERSONAL_GRADE_PATTERN =
            Pattern.compile("(?i)\\b(diem[_-]?(?:qua[_-]?trinh|cuoi[_-]?ky|tong[_-]?ket|chu)|gpa)\\b\\s*[:=]\\s*[\"']?([0-9]+(?:\\.[0-9]+)?|[A-F][+-]?(?!\\w))[\"']?");

    private SecretSanitizer() {
    }

    /**
     * Khử thông tin nhạy cảm (mật khẩu, token, JWT, chuỗi kết nối DB, điểm cá nhân) khỏi văn bản hoặc log lỗi.
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
        cleaned = PERSONAL_GRADE_PATTERN.matcher(cleaned).replaceAll("$1=***GRADE_REDACTED***");
        cleaned = Pattern.compile("(?i)\\bVT-[0-9A-Z]{4}(-[0-9A-Z]{4}){7}\\b").matcher(cleaned).replaceAll("VT-***CODE_REDACTED***");

        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength - 3) + "...";
        }
        return cleaned;
    }

    public static String sanitize(String input) {
        return sanitize(input, 2000);
    }
}
