package com.edu.registration.util;

import com.edu.registration.exception.ApiException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Quy ước và tiêu chuẩn an toàn dùng chung cho các chức năng Export dữ liệu (QLSV-91 / TASK-OP-04).
 * Bao gồm:
 * - Giới hạn số lượng bản ghi để chống cạn kiệt bộ nhớ máy chủ (Max Rows Guard).
 * - Chuẩn hóa múi giờ Việt Nam (GMT+7, Asia/Ho_Chi_Minh) và định dạng ngày giờ có timezone rõ ràng.
 * - Chuẩn hóa và làm sạch tên file an toàn (Safe Filename Sanitization).
 * - Tạo HTTP Security Headers bắt buộc khi tải file (Content-Disposition, Cache-Control no-store, nosniff).
 */
public final class ExportSecuritySupport {

    public static final int DEFAULT_MAX_EXPORT_ROWS = 10000;
    public static final ZoneId VIETNAM_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter TIMEZONE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss '(GMT+7)'");
    public static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss '(GMT+7)'");
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private ExportSecuritySupport() {
    }

    /**
     * Kiểm tra giới hạn số bản ghi xuất để tránh tràn bộ nhớ (Out-Of-Memory / DoS).
     */
    public static void validateRowCount(int rowCount, int maxRows, String tenChucNang) {
        if (rowCount > maxRows) {
            throw ApiException.badRequest(
                    "Số lượng bản ghi xuất (" + rowCount + ") vượt quá giới hạn an toàn tối đa ("
                            + maxRows + " dòng) của " + (tenChucNang != null ? tenChucNang : "hệ thống")
                            + ". Vui lòng thu hẹp bộ lọc theo học kỳ, khoa, ngành hoặc từ khóa trước khi xuất dữ liệu.");
        }
    }

    public static void validateRowCount(int rowCount, String tenChucNang) {
        validateRowCount(rowCount, DEFAULT_MAX_EXPORT_ROWS, tenChucNang);
    }

    /**
     * Chuẩn hóa tên file an toàn, loại bỏ ký tự điều khiển, ký tự đặc biệt và đường dẫn tương đối (Path Traversal).
     */
    public static String sanitizeFilename(String rawFilename, String defaultFilename) {
        String fallback = (defaultFilename != null && !defaultFilename.isBlank()) ? defaultFilename.trim() : "export-data";
        if (rawFilename == null || rawFilename.isBlank()) {
            return fallback;
        }

        // Bỏ dấu tiếng Việt và chuẩn hóa chữ đ/Đ
        String clean = rawFilename.trim().replace("đ", "d").replace("Đ", "D");
        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        // Thay ký tự cấm và phân tách thư mục
        clean = clean.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\.\\.+", ".");

        // Giữ lại ký tự chữ số ASCII, dấu gạch nối, gạch dưới và dấu chấm phân cách đuôi mở rộng
        clean = clean.replaceAll("[^A-Za-z0-9._-]", "_");
        clean = clean.replaceAll("_*\\._*", ".");
        clean = clean.replaceAll("_+", "_");
        int start = 0;
        int end = clean.length();
        while (start < end && "._-".indexOf(clean.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && "._-".indexOf(clean.charAt(end - 1)) >= 0) {
            end--;
        }
        clean = clean.substring(start, end);

        return clean.isEmpty() ? fallback : clean;
    }

    public static String sanitizeFilename(String rawFilename) {
        return sanitizeFilename(rawFilename, "export-data");
    }

    /**
     * Định dạng ngày giờ với múi giờ GMT+7 rõ ràng.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return TIMEZONE_FORMATTER.format(dateTime);
    }

    /**
     * Tạo HttpHeaders an toàn cho việc tải tệp đính kèm.
     */
    public static HttpHeaders createSafeDownloadHeaders(String filename, MediaType mediaType) {
        String safeName = sanitizeFilename(filename);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(safeName, StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore().mustRevalidate());
        headers.setPragma("no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
        return headers;
    }
}
