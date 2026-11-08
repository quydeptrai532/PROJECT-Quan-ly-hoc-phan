package com.edu.registration.config;

import com.edu.registration.entity.SupportRequestType;
import com.edu.registration.entity.SupportRequestStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cấu hình tập trung cho chức năng yêu cầu hỗ trợ / khiếu nại sinh viên.
 * Các giá trị mặc định dưới đây là cấu hình tạm thời an toàn để vận hành;
 * danh mục loại yêu cầu, trạng thái, chính sách tệp và SLA chính thức cần chủ dự án xác nhận.
 */
@ConfigurationProperties(prefix = "app.support-request")
public record SupportRequestProperties(
        String storageDir,
        Long maxFileSizeBytes,
        Integer maxFilesPerRequest,
        List<String> allowedExtensions,
        List<SupportRequestType> allowedTypes,
        List<SupportRequestStatus> allowedStatuses,
        List<SupportRequestStatus> commentAllowedStatuses,
        String slaNote
) {
    private static final String DEFAULT_STORAGE_DIR = "./data/support-attachments";
    private static final long DEFAULT_MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int DEFAULT_MAX_FILES_PER_REQUEST = 3;
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = List.of("pdf", "png", "jpg", "jpeg");
    private static final List<SupportRequestStatus> DEFAULT_COMMENT_ALLOWED_STATUSES = List.of(
            SupportRequestStatus.MOI_TAO,
            SupportRequestStatus.DANG_XU_LY,
            SupportRequestStatus.CHO_PHAN_HOI_SINH_VIEN,
            SupportRequestStatus.DA_GIAI_QUYET
    );

    public SupportRequestProperties {
        storageDir = (storageDir == null || storageDir.isBlank()) ? DEFAULT_STORAGE_DIR : storageDir.trim();
        maxFileSizeBytes = (maxFileSizeBytes == null || maxFileSizeBytes <= 0)
                ? DEFAULT_MAX_FILE_SIZE_BYTES : maxFileSizeBytes;
        maxFilesPerRequest = (maxFilesPerRequest == null || maxFilesPerRequest <= 0)
                ? DEFAULT_MAX_FILES_PER_REQUEST : maxFilesPerRequest;
        allowedExtensions = (allowedExtensions == null || allowedExtensions.isEmpty())
                ? DEFAULT_ALLOWED_EXTENSIONS
                : allowedExtensions.stream()
                        .filter(ext -> ext != null && !ext.isBlank())
                        .map(ext -> ext.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.+", ""))
                        .distinct()
                        .toList();
        allowedTypes = (allowedTypes == null || allowedTypes.isEmpty())
                ? Arrays.asList(SupportRequestType.values())
                : List.copyOf(allowedTypes);
        allowedStatuses = (allowedStatuses == null || allowedStatuses.isEmpty())
                ? Arrays.asList(SupportRequestStatus.values())
                : List.copyOf(allowedStatuses);
        commentAllowedStatuses = (commentAllowedStatuses == null || commentAllowedStatuses.isEmpty())
                ? DEFAULT_COMMENT_ALLOWED_STATUSES
                : List.copyOf(commentAllowedStatuses);
        slaNote = slaNote == null ? "" : slaNote.trim();
    }

    public static SupportRequestProperties defaults() {
        return new SupportRequestProperties(null, null, null, null, null, null, null, null);
    }

    public Set<String> allowedExtensionSet() {
        return allowedExtensions.stream().collect(Collectors.toUnmodifiableSet());
    }
}
