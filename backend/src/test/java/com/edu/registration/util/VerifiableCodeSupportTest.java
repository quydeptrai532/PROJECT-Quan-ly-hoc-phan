package com.edu.registration.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifiableCodeSupportTest {

    @Test
    void testGenerateVerificationCodeHighEntropyAndUniqueness() {
        Set<String> generated = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String code = VerifiableCodeSupport.generateVerificationCode();
            assertNotNull(code);
            assertTrue(code.startsWith("VT-"));
            // Định dạng: VT-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX (3 + 8*4 + 7 = 42 ký tự)
            assertEquals(42, code.length());
            assertFalse(generated.contains(code), "Mã phải có entropy cao và không bị trùng lặp");
            generated.add(code);
        }
    }

    @Test
    void testNormalizeCode() {
        String code1 = "VT-ABCD-EFGH-2345-6789-JKLM-NPQR-STUV-WXYZ";
        String normalized1 = VerifiableCodeSupport.normalizeCode(code1);
        assertEquals("ABCDEFGH23456789JKLMNPQRSTUVWXYZ", normalized1);

        String code2 = "vt-abcd-efgh-2345-6789-jklm-npqr-stuv-wxyz";
        String normalized2 = VerifiableCodeSupport.normalizeCode(code2);
        assertEquals(normalized1, normalized2, "Chuẩn hóa phải không phân biệt hoa thường");

        String code3 = "  ABCDEFGH23456789JKLMNPQRSTUVWXYZ  ";
        assertEquals(normalized1, VerifiableCodeSupport.normalizeCode(code3));
    }

    @Test
    void testSha256HexDeterministic() {
        String input = "ABCDEFGH23456789";
        String hash1 = VerifiableCodeSupport.sha256Hex(input);
        String hash2 = VerifiableCodeSupport.sha256Hex(input);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);

        String hash3 = VerifiableCodeSupport.sha256Hex("DIFFERENT");
        assertNotEquals(hash1, hash3);
    }

    @Test
    void testMaskCode() {
        String code = "VT-ABCD-EFGH-2345-6789-JKLM-NPQR-STUV-WXYZ";
        String masked = VerifiableCodeSupport.maskCode(code);
        assertTrue(masked.startsWith("VT-****"));
        assertTrue(masked.endsWith("WXYZ"));
        assertFalse(masked.contains("ABCD"));
    }

    @Test
    void testBuildVerificationUrlHttpsEnforcement() {
        String url1 = VerifiableCodeSupport.buildVerificationUrl("https://qldt.hcmus.edu.vn", "VT-123");
        assertEquals("https://qldt.hcmus.edu.vn/xac-thuc-bang-diem?code=VT-123", url1);

        // Tự động nâng cấp http thành https
        String url2 = VerifiableCodeSupport.buildVerificationUrl("http://portal.university.edu.vn", "VT-123");
        assertEquals("https://portal.university.edu.vn/xac-thuc-bang-diem?code=VT-123", url2);

        // Ngoại lệ localhost phục vụ kiểm thử
        String urlLocal = VerifiableCodeSupport.buildVerificationUrl("http://localhost:5173", "VT-123");
        assertEquals("http://localhost:5173/xac-thuc-bang-diem?code=VT-123", urlLocal);
    }

    @Test
    void testGenerateQrCodePng() {
        byte[] qrBytes = VerifiableCodeSupport.generateQrCodePng("https://qldt.university.edu.vn/xac-thuc-bang-diem?code=VT-TEST", 200, 200);
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 100);
        // Kiểm tra chữ ký PNG (89 50 4E 47 0D 0A 1A 0A)
        assertEquals((byte) 0x89, qrBytes[0]);
        assertEquals((byte) 0x50, qrBytes[1]); // 'P'
        assertEquals((byte) 0x4E, qrBytes[2]); // 'N'
        assertEquals((byte) 0x47, qrBytes[3]); // 'G'
    }
}
