package com.edu.registration.util;

import java.util.List;

/**
 * Tiện ích tạo tệp CSV chuẩn hóa và an toàn (QLSV-91 / TASK-OP-04).
 * Hỗ trợ:
 * - UTF-8 BOM (\uFEFF) để Excel hiển thị đúng tiếng Việt có dấu.
 * - Chống Spreadsheet Formula Injection (CSV Injection / CWE-1236) với các ký tự =, +, -, @, |, %,
 *   và các biến thể toàn góc (full-width) \uFF1D, \uFF0B, \uFF0D, \uFF20, \uFF5C.
 * - Loại bỏ/chuẩn hóa ký tự điều khiển (ISO control characters, null byte, CRLF trong ô).
 * - Chuẩn hóa dấu ngoặc kép theo chuẩn RFC 4180.
 */
public final class CsvUtils {
    /** BOM UTF-8 để Excel mở tiếng Việt đúng. */
    public static final String BOM = "\uFEFF";
    public static final String XUONG_DONG = "\r\n";

    /** Ký tự đầu khiến Excel/Sheets hiểu ô là công thức (gồm cả dạng toàn góc). */
    private static final String KY_TU_CONG_THUC = "=+-@|%\uFF1D\uFF0B\uFF0D\uFF20\uFF5C";

    private CsvUtils() {
    }

    /** Chuẩn hóa một ô: làm sạch control chars, chống formula injection, bao ngoặc kép RFC 4180. */
    public static String cell(Object value) {
        if (value == null) {
            return "\"\"";
        }
        String text = String.valueOf(value);
        StringBuilder sach = new StringBuilder(text.length());
        boolean truocLaDieuKhien = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isISOControl(ch)) {
                // Gộp chuỗi ký tự điều khiển liên tiếp (vd. CRLF, TAB) thành đúng một dấu cách.
                if (!truocLaDieuKhien) {
                    sach.append(' ');
                }
                truocLaDieuKhien = true;
            } else {
                sach.append(ch);
                truocLaDieuKhien = false;
            }
        }
        String noiDung = sach.toString().stripLeading();
        if (!noiDung.isEmpty() && KY_TU_CONG_THUC.indexOf(noiDung.charAt(0)) >= 0) {
            noiDung = "'" + noiDung;
        }
        return "\"" + noiDung.replace("\"", "\"\"") + "\"";
    }

    /** Chuẩn hóa một ô chuỗi. */
    public static String cell(String value) {
        return cell((Object) value);
    }

    /** Ghép một dòng CSV (kết thúc bằng CRLF). */
    public static String row(List<?> cells) {
        if (cells == null || cells.isEmpty()) {
            return XUONG_DONG;
        }
        StringBuilder dong = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                dong.append(',');
            }
            dong.append(cell(cells.get(i)));
        }
        return dong.append(XUONG_DONG).toString();
    }
}
