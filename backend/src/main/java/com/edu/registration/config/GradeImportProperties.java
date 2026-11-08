package com.edu.registration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tập trung cho chức năng import điểm từ tệp Excel (.xlsx) (QLSV-73 / TASK-LC-08).
 * Các thông số giới hạn tệp, mẫu template và chính sách cập nhật điểm hiện có được cấu hình tại đây;
 * các quyết định nghiệp vụ mặc định được ghi chú rõ để Chủ dự án xác nhận chính thức.
 */
@ConfigurationProperties(prefix = "app.grade-import")
public record GradeImportProperties(
        String tempDir,
        Long maxFileSizeBytes,
        Integer maxRows,
        Integer maxSheets,
        Long parseTimeoutMs,
        Long previewTtlSeconds,
        Long maxZipEntrySizeBytes,
        Integer maxZipEntries,
        String transactionPolicy,
        String updatePolicy,
        String policyNote
) {
    public static final String DEFAULT_TEMP_DIR = "./data/temp-grade-imports";
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB
    public static final int DEFAULT_MAX_ROWS = 500;
    public static final int DEFAULT_MAX_SHEETS = 1;
    public static final long DEFAULT_PARSE_TIMEOUT_MS = 5000L; // 5 seconds
    public static final long DEFAULT_PREVIEW_TTL_SECONDS = 900L; // 15 minutes
    public static final long DEFAULT_MAX_ZIP_ENTRY_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB uncompressed
    public static final int DEFAULT_MAX_ZIP_ENTRIES = 50;
    public static final String POLICY_ALL_OR_NOTHING = "ALL_OR_NOTHING";
    public static final String UPDATE_POLICY_OVERWRITE = "OVERWRITE_EXISTING";
    public static final String DEFAULT_POLICY_NOTE =
            "Mẫu Excel chính thức gồm các cột maSinhVien, hoTen, diemQuaTrinh, diemCuoiKy trên 1 sheet BangDiem. "
            + "Chính sách giao dịch mặc định là ALL_OR_NOTHING (chỉ ghi khi toàn bộ tệp hợp lệ) và chính sách cập nhật "
            + "là OVERWRITE_EXISTING (ghi đè điểm nháp hiện có, không tạo bản ghi trùng). Các quyết định này đang áp dụng "
            + "theo cấu hình mặc định và chờ Chủ dự án xác nhận chính thức.";

    public GradeImportProperties {
        tempDir = (tempDir == null || tempDir.isBlank()) ? DEFAULT_TEMP_DIR : tempDir.trim();
        maxFileSizeBytes = (maxFileSizeBytes == null || maxFileSizeBytes <= 0)
                ? DEFAULT_MAX_FILE_SIZE_BYTES : maxFileSizeBytes;
        maxRows = (maxRows == null || maxRows <= 0)
                ? DEFAULT_MAX_ROWS : maxRows;
        maxSheets = (maxSheets == null || maxSheets <= 0)
                ? DEFAULT_MAX_SHEETS : maxSheets;
        parseTimeoutMs = (parseTimeoutMs == null || parseTimeoutMs <= 0)
                ? DEFAULT_PARSE_TIMEOUT_MS : parseTimeoutMs;
        previewTtlSeconds = (previewTtlSeconds == null || previewTtlSeconds <= 0)
                ? DEFAULT_PREVIEW_TTL_SECONDS : previewTtlSeconds;
        maxZipEntrySizeBytes = (maxZipEntrySizeBytes == null || maxZipEntrySizeBytes <= 0)
                ? DEFAULT_MAX_ZIP_ENTRY_SIZE_BYTES : maxZipEntrySizeBytes;
        maxZipEntries = (maxZipEntries == null || maxZipEntries <= 0)
                ? DEFAULT_MAX_ZIP_ENTRIES : maxZipEntries;
        transactionPolicy = (transactionPolicy == null || transactionPolicy.isBlank())
                ? POLICY_ALL_OR_NOTHING : transactionPolicy.trim();
        updatePolicy = (updatePolicy == null || updatePolicy.isBlank())
                ? UPDATE_POLICY_OVERWRITE : updatePolicy.trim();
        policyNote = (policyNote == null || policyNote.isBlank())
                ? DEFAULT_POLICY_NOTE : policyNote.trim();
    }

    public static GradeImportProperties defaults() {
        return new GradeImportProperties(null, null, null, null, null, null, null, null, null, null, null);
    }
}
