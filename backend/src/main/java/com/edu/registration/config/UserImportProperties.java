package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tập trung cho chức năng import người dùng (sinh viên, giảng viên) từ CSV (QLSV-90 / TASK-OP-03 / TASK-AD-07).
 */
@ConfigurationProperties(prefix = "app.user-import")
public record UserImportProperties(
        Long maxFileSizeBytes,
        Integer maxRows,
        Long previewTtlSeconds,
        String transactionPolicy,
        String idempotencyPolicy,
        String policyNote
) {
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    public static final int DEFAULT_MAX_ROWS = 2000;
    public static final long DEFAULT_PREVIEW_TTL_SECONDS = 900L; // 15 phút
    public static final String POLICY_ROW_BY_ROW = "ROW_BY_ROW";
    public static final String POLICY_SKIP_IF_EXISTS = "SKIP_IF_EXISTS";
    public static final String DEFAULT_POLICY_NOTE =
            "Chính sách giao dịch ROW_BY_ROW (mỗi dòng hợp lệ được ghi nhận trong transaction độc lập) "
            + "và chống trùng lặp SKIP_IF_EXISTS (không tự ý ghi đè nếu mã định danh hoặc email đã tồn tại trong hệ thống).";

    public UserImportProperties {
        maxFileSizeBytes = (maxFileSizeBytes == null || maxFileSizeBytes <= 0)
                ? DEFAULT_MAX_FILE_SIZE_BYTES : maxFileSizeBytes;
        maxRows = (maxRows == null || maxRows <= 0)
                ? DEFAULT_MAX_ROWS : maxRows;
        previewTtlSeconds = (previewTtlSeconds == null || previewTtlSeconds <= 0)
                ? DEFAULT_PREVIEW_TTL_SECONDS : previewTtlSeconds;
        transactionPolicy = (transactionPolicy == null || transactionPolicy.isBlank())
                ? POLICY_ROW_BY_ROW : transactionPolicy.trim();
        idempotencyPolicy = (idempotencyPolicy == null || idempotencyPolicy.isBlank())
                ? POLICY_SKIP_IF_EXISTS : idempotencyPolicy.trim();
        policyNote = (policyNote == null || policyNote.isBlank())
                ? DEFAULT_POLICY_NOTE : policyNote.trim();
    }

    public static UserImportProperties defaults() {
        return new UserImportProperties(null, null, null, null, null, null);
    }
}
