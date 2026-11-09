package com.edu.registration.util;

import com.edu.registration.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExportSecuritySupportTest {

    @Test
    void validateRowCount_shouldPassWhenUnderLimit() {
        assertDoesNotThrow(() -> ExportSecuritySupport.validateRowCount(100, 1000, "Danh sách"));
        assertDoesNotThrow(() -> ExportSecuritySupport.validateRowCount(1000, 1000, "Danh sách"));
    }

    @Test
    void validateRowCount_shouldThrowWhenExceedingLimit() {
        ApiException ex = assertThrows(ApiException.class, () ->
                ExportSecuritySupport.validateRowCount(1001, 1000, "Danh sách sinh viên")
        );
        assertTrue(ex.getMessage().contains("vượt quá giới hạn an toàn tối đa"));
        assertTrue(ex.getMessage().contains("1000 dòng"));
        assertTrue(ex.getMessage().contains("Danh sách sinh viên"));
    }

    @Test
    void sanitizeFilename_shouldCleanUnsafeCharacters() {
        assertEquals("report_test.csv", ExportSecuritySupport.sanitizeFilename("report/test.csv"));
        assertEquals("report_test.csv", ExportSecuritySupport.sanitizeFilename("..\\report/test.csv"));
        assertEquals("bang_diem_lop_123.xlsx", ExportSecuritySupport.sanitizeFilename("bảng điểm: lớp <123>?.xlsx"));
        assertEquals("export-data", ExportSecuritySupport.sanitizeFilename(null));
        assertEquals("export-data", ExportSecuritySupport.sanitizeFilename("   "));
        assertEquals("my-export.csv", ExportSecuritySupport.sanitizeFilename("", "my-export.csv"));
    }

    @Test
    void createSafeDownloadHeaders_shouldSetAllSecurityHeaders() {
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders("test-file.csv", MediaType.parseMediaType("text/csv"));

        assertNotNull(headers.get(HttpHeaders.CONTENT_DISPOSITION));
        String disposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertTrue(disposition.contains("attachment"));
        assertTrue(disposition.contains("test-file.csv"));

        assertEquals("no-store, must-revalidate", headers.getFirst(HttpHeaders.CACHE_CONTROL));
        assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
        assertEquals("no-cache", headers.getFirst("Pragma"));
    }

    @Test
    void timeFormatters_shouldIncludeVietnamTimezone() {
        LocalDateTime dt = LocalDateTime.of(2026, 11, 9, 21, 30, 0);
        String formatted = ExportSecuritySupport.formatDateTime(dt);
        assertTrue(formatted.contains("(GMT+7)"), "Timestamp must explicitly declare Vietnam timezone (GMT+7)");
        assertTrue(formatted.startsWith("2026-11-09 21:30:00"));
    }

    @Test
    void csvFormulaInjection_shouldBeNeutralized() {
        // Test formula trigger characters: = + - @ | % fullwidth \uFF1D
        String[] dangerousInputs = {
                "=cmd|' /C calc'!A0",
                "+1+1",
                "-2+3",
                "@SUM(1,2)",
                "|calc.exe",
                "%COMSPEC%",
                "\uFF1D1+1",
                "\t=HYPERLINK(\"http://evil.com\")"
        };

        for (String input : dangerousInputs) {
            String sanitized = CsvUtils.cell(input);
            assertTrue(sanitized.startsWith("\"'") || sanitized.startsWith("'"),
                    "Dangerous input [" + input + "] must be prefixed with single-quote: " + sanitized);
        }
    }

    @Test
    void csvUtils_rowAndCell_shouldEscapeQuotesProperly() {
        assertEquals("\"\"", CsvUtils.cell(null));
        assertEquals("\"Hello\"", CsvUtils.cell("Hello"));
        assertEquals("\"He said \"\"Hi\"\"\"", CsvUtils.cell("He said \"Hi\""));
        assertEquals("\"123\"", CsvUtils.cell(123));

        String row = CsvUtils.row(List.of("A", "B,C", "=1+2"));
        assertEquals("\"A\",\"B,C\",\"'=1+2\"\r\n", row);
    }
}
