package com.edu.registration.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Tiện ích tạo và xác thực mã có entropy cao, băm an toàn SHA-256 và sinh mã QR (QLSV-99 / TASK-OP-12).
 */
public final class VerifiableCodeSupport {

    // Bảng ký tự 32 ký tự an toàn, loại bỏ ký tự dễ nhầm lẫn (0, O, 1, I)
    // 32 ký tự ngẫu nhiên => 32 * 5 bits = 160 bits entropy thực tế, không thể đoán được.
    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int RAW_CODE_LENGTH = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private VerifiableCodeSupport() {
    }

    /**
     * Cấp mã xác minh ngẫu nhiên có entropy cao (160+ bits) với định dạng dễ đọc:
     * VT-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
     */
    public static String generateVerificationCode() {
        StringBuilder raw = new StringBuilder(RAW_CODE_LENGTH);
        for (int i = 0; i < RAW_CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(CODE_ALPHABET.length);
            raw.append(CODE_ALPHABET[index]);
        }

        StringBuilder formatted = new StringBuilder("VT-");
        for (int i = 0; i < RAW_CODE_LENGTH; i++) {
            if (i > 0 && i % 4 == 0) {
                formatted.append('-');
            }
            formatted.append(raw.charAt(i));
        }
        return formatted.toString();
    }

    /**
     * Chuẩn hóa mã xác minh (loại bỏ khoảng trắng, dấu gạch ngang, chuyển chữ hoa)
     * để so khớp và tính mã băm đồng nhất.
     */
    public static String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        String clean = code.trim().toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
        if (clean.startsWith("VT")) {
            clean = clean.substring(2);
        }
        return clean;
    }

    /**
     * Tính mã băm SHA-256 của mã xác minh chuẩn hóa để lưu trong cơ sở dữ liệu.
     * Tuyệt đối không lưu mã bí mật dạng thô trong cơ sở dữ liệu.
     */
    public static String sha256Hex(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hệ thống không hỗ trợ thuật toán SHA-256", e);
        }
    }

    /**
     * Tạo mã hiển thị an toàn (che mờ phần bí mật) dùng cho danh sách quản trị và audit log.
     * Ví dụ: VT-****-****-****-****-****-****-****-A1B2
     */
    public static String maskCode(String code) {
        String clean = normalizeCode(code);
        if (clean.length() < 4) {
            return "VT-****";
        }
        String last4 = clean.substring(clean.length() - 4);
        return "VT-****-****-****-****-****-****-****-" + last4;
    }

    /**
     * Xây dựng URL xác thực theo chuẩn HTTPS dựa trên cấu hình môi trường.
     */
    public static String buildVerificationUrl(String baseUrl, String verificationCode) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? "https://qldt.university.edu.vn" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        // Đảm bảo giao thức HTTPS theo cấu hình và yêu cầu an toàn môi trường
        if (!base.startsWith("https://") && !base.startsWith("http://localhost") && !base.startsWith("http://127.0.0.1")) {
            base = base.replaceFirst("^http://", "https://");
            if (!base.startsWith("https://")) {
                base = "https://" + base;
            }
        }
        return base + "/xac-thuc-bang-diem?code=" + verificationCode;
    }

    /**
     * Sinh ảnh mã QR dạng byte array (PNG) với mức sửa lỗi cao (M) và lề nhỏ.
     */
    public static byte[] generateQrCodePng(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 1
            );
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
            try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
                return pngOutputStream.toByteArray();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể sinh mã QR xác thực", ex);
        }
    }
}
