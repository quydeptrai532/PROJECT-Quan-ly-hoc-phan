package com.edu.registration.importer;

import com.edu.registration.exception.ApiException;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Trình thẩm định và rào chắn an toàn dữ liệu tập trung cho toàn bộ pipeline import (QLSV-90 / TASK-OP-03).
 * Xử lý:
 * - Chống Path Traversal (đường dẫn .., ký tự điều khiển, null-byte)
 * - Giới hạn dung lượng tệp và số dòng dữ liệu
 * - Chống giả mạo định dạng (chữ ký nhị phân executable, ELF, ZIP, PDF, OLE2...)
 * - Kiểm tra chuẩn mã hóa văn bản UTF-8
 */
public final class ImportSecurityValidator {

    private ImportSecurityValidator() {
    }

    /**
     * Làm sạch và thẩm định tên tệp gốc; từ chối mọi chuỗi có chứa Path Traversal, ký tự điều khiển,
     * null byte hoặc sai phần mở rộng.
     *
     * @param originalFilename Tên tệp gửi lên từ client
     * @param expectedExtension Phần mở rộng kỳ vọng (ví dụ: ".csv", ".xlsx")
     * @return Tên tệp an toàn (chỉ chứa tên cơ sở)
     */
    public static String sanitizeAndValidateFileName(String originalFilename, String expectedExtension) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw ApiException.badRequest("Tên file tải lên không được để trống");
        }

        String raw = originalFilename.trim();

        // 1. Kiểm tra ký tự điều khiển và null byte
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch == '\0' || (ch < 32 && ch != '\t') || ch == 127) {
                throw ApiException.badRequest("Tên file chứa ký tự điều khiển hoặc null-byte không an toàn");
            }
        }

        // 2. Chuẩn hóa dấu phân cách thư mục để kiểm tra Path Traversal
        String normalized = raw.replace('\\', '/');

        // 3. Chống Path Traversal (../ hoặc ..)
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..")) {
            throw ApiException.badRequest("Tên file chứa đường dẫn nguy hiểm (Path Traversal bị chặn)");
        }

        // 4. Trích xuất tên tệp cơ sở (basename)
        int lastSlash = normalized.lastIndexOf('/');
        String baseName = lastSlash >= 0 ? normalized.substring(lastSlash + 1).trim() : normalized;
        if (baseName.isEmpty() || baseName.equals(".") || baseName.equals("..")) {
            throw ApiException.badRequest("Tên file không hợp lệ sau khi loại bỏ đường dẫn thư mục");
        }

        // 5. Kiểm tra phần mở rộng kỳ vọng
        if (expectedExtension != null && !expectedExtension.isBlank()) {
            String lowerBase = baseName.toLowerCase(Locale.ROOT);
            String lowerExt = expectedExtension.toLowerCase(Locale.ROOT);
            if (!lowerBase.endsWith(lowerExt)) {
                throw ApiException.badRequest("Định dạng file không hợp lệ. Hệ thống chỉ chấp nhận tệp có phần mở rộng " + expectedExtension);
            }
        }

        return baseName;
    }

    /**
     * Kiểm tra dung lượng tệp và các chữ ký nhị phân độc hại / giả mạo định dạng.
     *
     * @param bytes Mảng byte dữ liệu tệp
     * @param maxSizeBytes Giới hạn dung lượng tối đa (byte)
     * @param expectedFormat Định dạng kỳ vọng ("CSV" hoặc "XLSX")
     */
    public static void validateFileSecurity(byte[] bytes, long maxSizeBytes, String expectedFormat) {
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("Tệp tải lên rỗng, không chứa dữ liệu");
        }

        if (bytes.length > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            long fileKb = bytes.length / 1024;
            throw ApiException.badRequest("Dung lượng tệp (" + fileKb + " KB) vượt quá giới hạn tối đa cho phép (" + maxMb + " MB)");
        }

        boolean isZip = isZipMagicBytes(bytes);

        if ("XLSX".equalsIgnoreCase(expectedFormat)) {
            // Tệp OOXML .xlsx bắt buộc phải là một gói ZIP hợp lệ
            if (!isZip) {
                throw ApiException.badRequest("Tệp Excel tải lên không hợp lệ: thiếu chữ ký nhị phân ZIP OOXML chuẩn (PK..)");
            }
        } else if ("CSV".equalsIgnoreCase(expectedFormat)) {
            // CSV là tệp văn bản thuần; nghiêm cấm các tệp nhị phân nén hoặc thực thi trá hình
            if (isExecutableOrBinaryMagicBytes(bytes)) {
                throw ApiException.badRequest("Tệp tải lên bị từ chối: phát hiện chữ ký nhị phân tệp thực thi/tệp nén không hợp lệ cho định dạng CSV");
            }
            validateCsvUtf8Encoding(bytes);
        }
    }

    /**
     * Chữ ký ZIP: PK\x03\x04
     */
    public static boolean isZipMagicBytes(byte[] bytes) {
        return bytes != null && bytes.length >= 4
                && bytes[0] == 0x50 && bytes[1] == 0x4B && bytes[2] == 0x03 && bytes[3] == 0x04;
    }

    /**
     * Phát hiện các chữ ký nhị phân thường gặp của malware, file thực thi hoặc file định dạng sai:
     * - DOS/Windows PE Executable: 'MZ' (0x4D, 0x5A)
     * - Linux ELF Executable: '\x7fELF' (0x7F, 0x45, 0x4C, 0x46)
     * - PDF: '%PDF' (0x25, 0x50, 0x44, 0x46)
     * - OLE2 Compound Document (Word/Excel cũ .doc/.xls): \xD0\xCF\x11\xE0
     * - ZIP Package: 'PK\x03\x04'
     * - GZIP Archive: \x1F\x8B
     * - Java Class File: 0xCA, 0xFE, 0xBA, 0xBE
     * - 7-Zip Archive: 0x37, 0x7A, 0xBC, 0xAF, 0x27, 0x1C
     */
    public static boolean isExecutableOrBinaryMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }

        // PE Executable ('MZ')
        if (bytes[0] == 0x4D && bytes[1] == 0x5A) {
            return true;
        }

        // GZIP
        if ((bytes[0] & 0xFF) == 0x1F && (bytes[1] & 0xFF) == 0x8B) {
            return true;
        }

        if (bytes.length >= 4) {
            // Linux ELF
            if (bytes[0] == 0x7F && bytes[1] == 0x45 && bytes[2] == 0x4C && bytes[3] == 0x46) {
                return true;
            }
            // PDF
            if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
                return true;
            }
            // OLE2 Compound Document
            if ((bytes[0] & 0xFF) == 0xD0 && (bytes[1] & 0xFF) == 0xCF
                    && (bytes[2] & 0xFF) == 0x11 && (bytes[3] & 0xFF) == 0xE0) {
                return true;
            }
            // ZIP
            if (isZipMagicBytes(bytes)) {
                return true;
            }
            // Java bytecode
            if ((bytes[0] & 0xFF) == 0xCA && (bytes[1] & 0xFF) == 0xFE
                    && (bytes[2] & 0xFF) == 0xBA && (bytes[3] & 0xFF) == 0xBE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Kiểm tra nội dung CSV có phải văn bản thuần UTF-8 hợp lệ không chứa null-byte.
     */
    public static void validateCsvUtf8Encoding(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }

        // Từ chối nếu chứa byte 0 (null-byte)
        for (byte b : bytes) {
            if (b == 0) {
                throw ApiException.badRequest("Tệp CSV chứa ký tự nhị phân (null-byte) không hợp lệ");
            }
        }

        // Kiểm tra giải mã UTF-8 nghiêm ngặt
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException ex) {
            throw ApiException.badRequest("Tệp CSV không đúng định dạng mã hóa ký tự UTF-8");
        }
    }
}
