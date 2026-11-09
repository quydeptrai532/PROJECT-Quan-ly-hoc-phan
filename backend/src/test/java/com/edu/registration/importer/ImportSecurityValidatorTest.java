package com.edu.registration.importer;

import com.edu.registration.exception.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportSecurityValidatorTest {

    @Test
    @DisplayName("Tên file hợp lệ được chuẩn hóa an toàn")
    void testValidFileName() {
        String clean = ImportSecurityValidator.sanitizeAndValidateFileName("danh-sach-sinh-vien_2026.csv", ".csv");
        assertEquals("danh-sach-sinh-vien_2026.csv", clean);

        String cleanXlsx = ImportSecurityValidator.sanitizeAndValidateFileName("bang_diem_L01.xlsx", ".xlsx");
        assertEquals("bang_diem_L01.xlsx", cleanXlsx);
    }

    @Test
    @DisplayName("Chặn tấn công Path Traversal qua tên file")
    void testPathTraversalRejection() {
        ApiException ex1 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("../evil.csv", ".csv"));
        assertTrue(ex1.getMessage().contains("Path Traversal"));

        ApiException ex2 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("..\\..\\windows\\system32\\cmd.exe.csv", ".csv"));
        assertTrue(ex2.getMessage().contains("Path Traversal"));

        ApiException ex3 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("/etc/passwd/../sinh-vien.csv", ".csv"));
        assertTrue(ex3.getMessage().contains("Path Traversal"));
    }

    @Test
    @DisplayName("Chặn tên file chứa ký tự điều khiển hoặc null-byte")
    void testControlCharactersAndNullByteRejection() {
        ApiException ex1 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("test\0.csv", ".csv"));
        assertTrue(ex1.getMessage().contains("null-byte") || ex1.getMessage().contains("ký tự điều khiển"));

        ApiException ex2 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("test\r\nbad.csv", ".csv"));
        assertTrue(ex2.getMessage().contains("ký tự điều khiển"));
    }

    @Test
    @DisplayName("Từ chối tên file sai phần mở rộng")
    void testWrongExtensionRejection() {
        ApiException ex = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.sanitizeAndValidateFileName("bang-diem.xlsx", ".csv"));
        assertTrue(ex.getMessage().contains("phần mở rộng .csv"));
    }

    @Test
    @DisplayName("Từ chối tệp rỗng hoặc vượt quá dung lượng tối đa")
    void testFileSizeValidation() {
        ApiException exEmpty = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(new byte[0], 1024, "CSV"));
        assertTrue(exEmpty.getMessage().contains("rỗng"));

        byte[] largeBytes = new byte[2000];
        ApiException exLarge = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(largeBytes, 1000, "CSV"));
        assertTrue(exLarge.getMessage().contains("vượt quá giới hạn"));
    }

    @Test
    @DisplayName("Chặn tải lên tệp thực thi / nhị phân giả mạo CSV (DOS PE, Linux ELF, ZIP, PDF)")
    void testFakeCsvMagicBytesRejection() {
        // DOS PE Executable ('MZ')
        byte[] peHeader = new byte[] { 0x4D, 0x5A, 0x00, 0x01, 0x02 };
        ApiException exPe = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(peHeader, 50000, "CSV"));
        assertTrue(exPe.getMessage().contains("chữ ký nhị phân"));

        // Linux ELF ('\x7fELF')
        byte[] elfHeader = new byte[] { 0x7F, 0x45, 0x4C, 0x46, 0x01 };
        ApiException exElf = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(elfHeader, 50000, "CSV"));
        assertTrue(exElf.getMessage().contains("chữ ký nhị phân"));

        // ZIP Archive ('PK\x03\x04')
        byte[] zipHeader = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x14 };
        ApiException exZip = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(zipHeader, 50000, "CSV"));
        assertTrue(exZip.getMessage().contains("chữ ký nhị phân"));

        // PDF ('%PDF')
        byte[] pdfHeader = new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D };
        ApiException exPdf = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(pdfHeader, 50000, "CSV"));
        assertTrue(exPdf.getMessage().contains("chữ ký nhị phân"));
    }

    @Test
    @DisplayName("Chặn tệp CSV chứa null-byte hoặc chuỗi byte không phải UTF-8 hợp lệ")
    void testInvalidCsvEncodingRejection() {
        // Chứa null-byte
        byte[] nullByteCsv = "ma,hoTen\nSV01\0,Nguyen A\n".getBytes(StandardCharsets.UTF_8);
        ApiException exNull = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(nullByteCsv, 50000, "CSV"));
        assertTrue(exNull.getMessage().contains("null-byte"));

        // Byte không hợp lệ UTF-8
        byte[] invalidUtf8 = new byte[] { (byte) 0xC3, (byte) 0x28, 'a', 'b', 'c' };
        ApiException exUtf8 = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(invalidUtf8, 50000, "CSV"));
        assertTrue(exUtf8.getMessage().contains("UTF-8"));
    }

    @Test
    @DisplayName("Tệp CSV UTF-8 hợp lệ được chấp nhận")
    void testValidCsvAccepted() {
        byte[] validCsv = "\uFEFFma,hoTen,email\nSV001,Nguyễn Văn An,an@edu.vn\n".getBytes(StandardCharsets.UTF_8);
        assertDoesNotThrow(() ->
                ImportSecurityValidator.validateFileSecurity(validCsv, 50000, "CSV"));
    }

    @Test
    @DisplayName("Tệp XLSX bắt buộc phải có chữ ký ZIP OOXML PK..")
    void testXlsxMagicByteValidation() {
        byte[] nonZip = "This is not a zip file".getBytes(StandardCharsets.UTF_8);
        ApiException ex = assertThrows(ApiException.class, () ->
                ImportSecurityValidator.validateFileSecurity(nonZip, 50000, "XLSX"));
        assertTrue(ex.getMessage().contains("chữ ký nhị phân ZIP OOXML"));

        byte[] validZipHeader = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x00, 0x00 };
        assertDoesNotThrow(() ->
                ImportSecurityValidator.validateFileSecurity(validZipHeader, 50000, "XLSX"));
    }
}
