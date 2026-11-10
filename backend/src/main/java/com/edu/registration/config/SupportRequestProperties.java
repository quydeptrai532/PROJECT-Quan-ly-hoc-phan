package com.edu.registration.config;

import com.edu.registration.entity.Role;
import com.edu.registration.entity.SupportRequestPriority;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cấu hình tập trung cho chức năng hàng đợi và xử lý yêu cầu hỗ trợ / khiếu nại sinh viên.
 * Điểm cần Chủ dự án quyết định:
 * 1. Phân quyền xử lý: Vai trò nào được thao tác hàng đợi (mặc định giới hạn QUAN_TRI).
 * 2. Phạm vi hiển thị hàng đợi (tất cả hay chỉ phiếu phân công).
 * 3. Lịch làm việc chính thức (giờ hành chính, nghỉ trưa, ngày làm việc, ngày nghỉ lễ).
 * 4. Chính sách cam kết SLA chính thức (chưa kích hoạt chính thức: officialSlaEnabled = false).
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
        String slaNote,
        QueueProperties queue,
        WorkingCalendarProperties workingCalendar,
        SlaProperties sla
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

    public record QueueProperties(
            List<Role> allowedHandlerRoles,
            String scopeMode,
            SupportRequestPriority defaultPriority,
            List<SupportRequestPriority> allowedPriorities,
            String policyNote
    ) {
        public static final String SCOPE_MODE_ALL = "ALL";
        public static final String SCOPE_MODE_ASSIGNED_ONLY = "ASSIGNED_ONLY";
        public static final String SCOPE_MODE_ASSIGNED_OR_UNASSIGNED = "ASSIGNED_OR_UNASSIGNED";

        public QueueProperties {
            allowedHandlerRoles = (allowedHandlerRoles == null || allowedHandlerRoles.isEmpty())
                    ? List.of(Role.QUAN_TRI)
                    : List.copyOf(allowedHandlerRoles);
            scopeMode = (scopeMode == null || scopeMode.isBlank())
                    ? SCOPE_MODE_ALL
                    : scopeMode.trim().toUpperCase(Locale.ROOT);
            defaultPriority = defaultPriority == null ? SupportRequestPriority.TRUNG_BINH : defaultPriority;
            allowedPriorities = (allowedPriorities == null || allowedPriorities.isEmpty())
                    ? Arrays.asList(SupportRequestPriority.values())
                    : List.copyOf(allowedPriorities);
            policyNote = (policyNote == null || policyNote.isBlank())
                    ? "Vai trò xử lý hàng đợi mặc định giới hạn QUAN_TRI để tránh mở quyền rộng; các vai trò hỗ trợ chuyên trách khác (nếu có) đang chờ Chủ dự án chốt chính thức."
                    : policyNote.trim();
        }

        public static QueueProperties defaults() {
            return new QueueProperties(null, null, null, null, null);
        }
    }

    public record WorkingCalendarProperties(
            List<DayOfWeek> workingDays,
            LocalTime startTime,
            LocalTime endTime,
            LocalTime lunchStartTime,
            LocalTime lunchEndTime,
            List<LocalDate> holidays,
            String timezone,
            String policyNote
    ) {
        private static final List<DayOfWeek> DEFAULT_WORKING_DAYS = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        );
        private static final LocalTime DEFAULT_START_TIME = LocalTime.of(8, 0);
        private static final LocalTime DEFAULT_END_TIME = LocalTime.of(17, 0);
        private static final LocalTime DEFAULT_LUNCH_START = LocalTime.of(12, 0);
        private static final LocalTime DEFAULT_LUNCH_END = LocalTime.of(13, 0);
        private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

        public WorkingCalendarProperties {
            workingDays = (workingDays == null || workingDays.isEmpty())
                    ? DEFAULT_WORKING_DAYS : List.copyOf(workingDays);
            startTime = startTime != null ? startTime : DEFAULT_START_TIME;
            endTime = endTime != null ? endTime : DEFAULT_END_TIME;
            lunchStartTime = lunchStartTime != null ? lunchStartTime : DEFAULT_LUNCH_START;
            lunchEndTime = lunchEndTime != null ? lunchEndTime : DEFAULT_LUNCH_END;
            holidays = holidays != null ? List.copyOf(holidays) : List.of();
            timezone = (timezone == null || timezone.isBlank()) ? DEFAULT_TIMEZONE : timezone.trim();
            policyNote = (policyNote == null || policyNote.isBlank())
                    ? "Lịch làm việc chuẩn (Thứ 2 - Thứ 6, 08:00 - 17:00, nghỉ trưa 12:00 - 13:00) đang áp dụng cấu hình dự kiến và chờ Chủ dự án phê duyệt chính thức."
                    : policyNote.trim();
        }

        public static WorkingCalendarProperties defaults() {
            return new WorkingCalendarProperties(null, null, null, null, null, null, null, null);
        }
    }

    public record SlaProperties(
            boolean officialSlaEnabled,
            boolean pauseOnStudentWait,
            double nearingDueThresholdHours,
            Map<SupportRequestPriority, Double> targetResponseHours,
            Map<SupportRequestPriority, Double> targetResolveHours,
            String policyNote
    ) {
        private static final Map<SupportRequestPriority, Double> DEFAULT_RESPONSE_HOURS = Map.of(
                SupportRequestPriority.KHAN_CAP, 4.0,
                SupportRequestPriority.CAO, 8.0,
                SupportRequestPriority.TRUNG_BINH, 24.0,
                SupportRequestPriority.THAP, 48.0
        );

        private static final Map<SupportRequestPriority, Double> DEFAULT_RESOLVE_HOURS = Map.of(
                SupportRequestPriority.KHAN_CAP, 16.0,
                SupportRequestPriority.CAO, 40.0,
                SupportRequestPriority.TRUNG_BINH, 72.0,
                SupportRequestPriority.THAP, 120.0
        );

        public SlaProperties {
            nearingDueThresholdHours = nearingDueThresholdHours <= 0 ? 4.0 : nearingDueThresholdHours;
            EnumMap<SupportRequestPriority, Double> responseMap = new EnumMap<>(SupportRequestPriority.class);
            responseMap.putAll(DEFAULT_RESPONSE_HOURS);
            if (targetResponseHours != null) {
                responseMap.putAll(targetResponseHours);
            }
            targetResponseHours = Map.copyOf(responseMap);

            EnumMap<SupportRequestPriority, Double> resolveMap = new EnumMap<>(SupportRequestPriority.class);
            resolveMap.putAll(DEFAULT_RESOLVE_HOURS);
            if (targetResolveHours != null) {
                resolveMap.putAll(targetResolveHours);
            }
            targetResolveHours = Map.copyOf(resolveMap);

            policyNote = (policyNote == null || policyNote.isBlank())
                    ? "Chính sách SLA xử lý yêu cầu hỗ trợ và thời hạn cam kết đang áp dụng theo cấu hình tập trung dự kiến (tham khảo), chưa phải quy chế chính thức của Nhà trường và chờ Chủ dự án phê duyệt."
                    : policyNote.trim();
        }

        public static SlaProperties defaults() {
            return new SlaProperties(false, true, 4.0, null, null, null);
        }
    }

    @ConstructorBinding
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
        queue = queue != null ? queue : QueueProperties.defaults();
        workingCalendar = workingCalendar != null ? workingCalendar : WorkingCalendarProperties.defaults();
        sla = sla != null ? sla : SlaProperties.defaults();
    }

    public SupportRequestProperties() {
        this(null, null, null, null, null, null, null, null, null, null, null);
    }

    public SupportRequestProperties(
            String storageDir,
            Long maxFileSizeBytes,
            Integer maxFilesPerRequest,
            List<String> allowedExtensions,
            List<SupportRequestType> allowedTypes,
            List<SupportRequestStatus> allowedStatuses,
            List<SupportRequestStatus> commentAllowedStatuses,
            String slaNote
    ) {
        this(storageDir, maxFileSizeBytes, maxFilesPerRequest, allowedExtensions,
                allowedTypes, allowedStatuses, commentAllowedStatuses, slaNote,
                null, null, null);
    }

    public static SupportRequestProperties defaults() {
        return new SupportRequestProperties(null, null, null, null, null, null, null, null, null, null, null);
    }

    public Set<String> allowedExtensionSet() {
        return allowedExtensions.stream().collect(Collectors.toUnmodifiableSet());
    }
}
