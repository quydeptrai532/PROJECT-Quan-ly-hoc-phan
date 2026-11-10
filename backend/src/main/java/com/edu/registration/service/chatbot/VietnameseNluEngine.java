package com.edu.registration.service.chatbot;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bộ xử lý ngôn ngữ tự nhiên tiếng Việt phân tích ý định (Intent) và thực thể (Slots) (QLSV-106 / TASK-AI-01).
 */
@Component
public class VietnameseNluEngine {

    public enum Intent {
        DANG_KY,
        XAC_NHAN,
        HUY_XAC_NHAN,
        LOP_THAY_THE,
        TRA_CUU_CON_CHO,
        TRA_CUU_LOP,
        TRA_CUU_LICH,
        TRA_CUU_GIANG_VIEN,
        TRA_CUU_PHONG,
        TKB_CA_NHAN,
        THONG_KE,
        TRO_GIUP,
        THOI_TIET,
        GREETING,
        THANKS,
        GOODBYE,
        SMALL_TALK,
        UNKNOWN
    }

    public record NluParseResult(
            Intent intent,
            String rawMessage,
            String normalizedMessage,
            String maLop,
            String maMon,
            String tenMonOrKeyword,
            String tenGiangVien,
            Integer thu,
            Integer tietBatDau,
            Integer selectionIndex,
            boolean onlyAvailableSeats,
            boolean onlyFullClasses
    ) {
    }

    private static final Pattern MA_LOP_PATTERN = Pattern.compile("\\b([A-Z0-9]{2,8}\\.[0-9]{2}(?:-HK[0-9]+)?|LHP[-_]?[0-9A-Za-z]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MA_MON_PATTERN = Pattern.compile("\\b([A-Z]{2,4}[0-9]{2,4})\\b");
    private static final Pattern SELECTION_NUM_PATTERN = Pattern.compile("^\\s*([1-9]|10)\\s*$|^\\s*(?:chon|so|lop)\\s*([1-9]|10)\\s*$", Pattern.CASE_INSENSITIVE);

    // Các mẫu hội thoại xã giao: chỉ khớp khi toàn bộ câu là lời chào/cảm ơn/tạm biệt,
    // tránh nuốt mất các câu có nội dung tra cứu phía sau.
    private static final Pattern GREETING_PATTERN = Pattern.compile(
            "^(?:xin\\s+)?(?:chao|hello|hi|hey|helo|halo|alo|yo)"
                    + "(?:\\s+(?:ban|tro\\s+ly|bot|assistant|admin|moi\\s+nguoi|ca\\s+ban|a|oi|nhe|nha|nhi))*$");
    private static final Pattern THANKS_PATTERN = Pattern.compile(
            "^(?:cam on|thank you|thanks|thank|tks|thanks a lot)"
                    + "(?:\\s+(?:ban|a|oi|nhe|nha|nhi|rat\\s+nhieu|nhieu|so\\s+much|very\\s+much))*$");
    private static final Pattern GOODBYE_PATTERN = Pattern.compile(
            "^(?:tam biet|bye|goodbye|hen gap lai|chao nhe|chao ban nhe|chao ca nha)"
                    + "(?:\\s+(?:ban|nhe|nha|nhi))*$");
    private static final Pattern SMALL_TALK_PATTERN = Pattern.compile(
            "\\b(ban la ai|ban ten (?:la )?gi|ten (?:cua )?ban|may la ai|ai tao ra ban|ban khoe khong|"
                    + "gioi thieu ve ban|ban la tro ly gi)\\b");

    public NluParseResult parse(String message) {
        if (message == null || message.isBlank()) {
            return new NluParseResult(Intent.UNKNOWN, "", "", null, null, null, null, null, null, null, false, false);
        }

        String raw = message.trim();
        String normalized = removeDiacritics(raw).toLowerCase(Locale.ROOT);

        // 1. Kiểm tra lựa chọn theo số thứ tự (khi disambiguation list đang hiển thị)
        Matcher selMatcher = SELECTION_NUM_PATTERN.matcher(normalized);
        Integer selectionIndex = null;
        if (selMatcher.find()) {
            String numStr = selMatcher.group(1) != null ? selMatcher.group(1) : selMatcher.group(2);
            if (numStr != null) {
                try {
                    selectionIndex = Integer.parseInt(numStr);
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Trích xuất mã lớp (nếu có)
        String maLop = null;
        Matcher maLopMatcher = MA_LOP_PATTERN.matcher(raw);
        if (maLopMatcher.find()) {
            maLop = maLopMatcher.group(1).toUpperCase(Locale.ROOT);
        }

        // 3. Trích xuất mã môn (nếu có và không phải mã lớp)
        String maMon = null;
        Matcher maMonMatcher = MA_MON_PATTERN.matcher(raw);
        while (maMonMatcher.find()) {
            String candidate = maMonMatcher.group(1).toUpperCase(Locale.ROOT);
            if (maLop == null || !candidate.equalsIgnoreCase(maLop)) {
                maMon = candidate;
                break;
            }
        }

        // 4. Trích xuất thứ trong tuần
        Integer thu = null;
        if (normalized.contains("thu 2") || normalized.contains("thu hai")) thu = 2;
        else if (normalized.contains("thu 3") || normalized.contains("thu ba")) thu = 3;
        else if (normalized.contains("thu 4") || normalized.contains("thu tu")) thu = 4;
        else if (normalized.contains("thu 5") || normalized.contains("thu nam")) thu = 5;
        else if (normalized.contains("thu 6") || normalized.contains("thu sau")) thu = 6;
        else if (normalized.contains("thu 7") || normalized.contains("thu bay")) thu = 7;
        else if (normalized.contains("chu nhat") || normalized.contains("cn")) thu = 8;

        // 5. Trích xuất tiết
        Integer tiet = null;
        Matcher tietMatcher = Pattern.compile("tiet\\s*([1-9]|1[0-2])").matcher(normalized);
        if (tietMatcher.find()) {
            try {
                tiet = Integer.parseInt(tietMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        // 6. Trích xuất giảng viên
        String giangVien = null;
        Matcher gvMatcher = Pattern.compile("(?:thay|co|giang vien|gv)\\s+([a-zA-Z\\u00C0-\\u024F\\u1EA0-\\u1EF9]+(?:\\s+[a-zA-Z\\u00C0-\\u024F\\u1EA0-\\u1EF9]+){0,3})", Pattern.CASE_INSENSITIVE).matcher(raw);
        if (gvMatcher.find()) {
            giangVien = gvMatcher.group(1).trim();
        }

        // 7. Nhận diện các cờ sĩ số
        boolean onlyAvailableSeats = normalized.contains("con cho")
                || normalized.contains("chua day")
                || normalized.contains("con trong")
                || normalized.contains("chua full");
        boolean onlyFullClasses = normalized.contains("da day")
                || normalized.contains("het cho")
                || normalized.contains("da full")
                || normalized.contains("het slot");

        // 8. Trích xuất từ khóa môn học
        String keyword = extractSubjectKeyword(raw, normalized);

        // 9. Xác định Intent
        Intent intent = determineIntent(normalized, maLop, selectionIndex);

        return new NluParseResult(
                intent,
                raw,
                normalized,
                maLop,
                maMon,
                keyword,
                giangVien,
                thu,
                tiet,
                selectionIndex,
                onlyAvailableSeats,
                onlyFullClasses
        );
    }

    private Intent determineIntent(String normalized, String maLop, Integer selectionIndex) {
        // Xác nhận đăng ký
        if (normalized.equals("xac nhan")
                || normalized.equals("dong y")
                || normalized.equals("ok")
                || normalized.equals("co")
                || normalized.startsWith("xac nhan dang ky")
                || normalized.contains("toi dong y")
                || normalized.contains("tien hanh dang ky")) {
            return Intent.XAC_NHAN;
        }

        // Hủy thao tác
        if (normalized.equals("huy")
                || normalized.startsWith("huy dang ky")
                || normalized.contains("khong dang ky nua")
                || normalized.contains("bo qua")
                || normalized.contains("cancel")) {
            return Intent.HUY_XAC_NHAN;
        }

        // Thời tiết (đặt trước rule lịch học để tránh nhầm "thời tiết" thành "tiết")
        if (normalized.contains("thoi tiet") || normalized.contains("nhiet do")
                || normalized.contains("du bao") || normalized.contains("do am")
                || normalized.contains("troi mua") || normalized.contains("troi nang")
                || normalized.contains("troi nong") || normalized.contains("troi lanh")) {
            return Intent.THOI_TIET;
        }

        // Hội thoại xã giao (chào hỏi / cảm ơn / tạm biệt / giới thiệu)
        String compact = normalized.replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
        if (GOODBYE_PATTERN.matcher(compact).matches()) {
            return Intent.GOODBYE;
        }
        if (THANKS_PATTERN.matcher(compact).matches()) {
            return Intent.THANKS;
        }
        if (GREETING_PATTERN.matcher(compact).matches()) {
            return Intent.GREETING;
        }
        if (SMALL_TALK_PATTERN.matcher(compact).find()) {
            return Intent.SMALL_TALK;
        }

        // Muốn đăng ký
        if (normalized.contains("dang ky") || normalized.contains("ghi danh") || normalized.contains("muon dang ky") || normalized.contains("dk")) {
            return Intent.DANG_KY;
        }

        // Lớp thay thế
        if (normalized.contains("thay the") || normalized.contains("lop khac") || normalized.contains("lop thay cho")) {
            return Intent.LOP_THAY_THE;
        }

        // Thời khóa biểu cá nhân
        if (normalized.contains("tkb cua toi") || normalized.contains("thoi khoa bieu cua toi")
                || normalized.contains("lich hoc cua toi") || normalized.contains("lop da dang ky")
                || normalized.contains("mon da dang ky") || normalized.contains("lich day cua toi")) {
            return Intent.TKB_CA_NHAN;
        }

        // Thống kê đào tạo (QUAN_TRI)
        if (normalized.contains("thong ke") || normalized.contains("bao cao dao tao") || normalized.contains("kpi")) {
            return Intent.THONG_KE;
        }

        // Trợ giúp
        if (normalized.contains("tro giup") || normalized.contains("huong dan") || normalized.equals("help") || normalized.contains("lam duoc gi")) {
            return Intent.TRO_GIUP;
        }

        // Tra cứu còn chỗ / sĩ số
        if (normalized.contains("con cho") || normalized.contains("si so") || normalized.contains("bao nhieu cho") || normalized.contains("het cho") || normalized.contains("da day")) {
            return Intent.TRA_CUU_CON_CHO;
        }

        // Tra cứu lịch học (tránh nhầm "lịch sử", "du lịch", "thời tiết" thành lịch học)
        if (normalized.contains("lich hoc") || normalized.contains("lich day") || normalized.contains("lich thi")
                || normalized.contains("thoi khoa bieu") || normalized.contains("thu may")
                || normalized.contains("tiet hoc")
                || Pattern.compile("\\btiet\\s*\\d+").matcher(normalized).find()) {
            return Intent.TRA_CUU_LICH;
        }

        // Tra cứu giảng viên (không bắt chữ "co"/"thay" đơn lẻ để tránh nuốt câu hỏi thông thường)
        if (normalized.contains("giang vien") || normalized.contains("giao vien")
                || normalized.contains("ai day") || normalized.contains("thay nao")
                || normalized.contains("co nao") || normalized.contains("thay co nao")) {
            return Intent.TRA_CUU_GIANG_VIEN;
        }

        // Tra cứu phòng học (không bắt chữ "phong" đơn lẻ như "phong cách")
        if (normalized.contains("phong hoc") || normalized.contains("hoc o dau")
                || normalized.contains("phong nao") || normalized.contains("o phong")) {
            return Intent.TRA_CUU_PHONG;
        }

        // Tra cứu lớp học / môn học chung
        if (normalized.contains("lop") || normalized.contains("mon") || normalized.contains("tim") || maLop != null || selectionIndex != null) {
            return Intent.TRA_CUU_LOP;
        }

        return Intent.UNKNOWN;
    }

    private String extractSubjectKeyword(String raw, String normalized) {
        Pattern pattern = Pattern.compile("(?:môn|mon|lớp|lop|học\\s*phần|hoc\\s*phan|tìm\\s*môn|tim\\s*mon|tìm\\s*lớp|tim\\s*lop)\\s+([a-zA-Z0-9\\u00C0-\\u024F\\u1EA0-\\u1EF9\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(raw);
        if (matcher.find()) {
            String kw = matcher.group(1).trim();
            if (kw.toLowerCase(Locale.ROOT).startsWith("môn ")) {
                kw = kw.substring(4).trim();
            } else if (kw.toLowerCase(Locale.ROOT).startsWith("mon ")) {
                kw = kw.substring(4).trim();
            } else if (kw.toLowerCase(Locale.ROOT).startsWith("lớp ")) {
                kw = kw.substring(4).trim();
            } else if (kw.toLowerCase(Locale.ROOT).startsWith("lop ")) {
                kw = kw.substring(4).trim();
            }
            kw = kw.replaceAll("(?i)\\s+(?:còn chỗ|con cho|chưa đầy|chua day|đã đầy|da day|hết chỗ|het cho|học thứ\\s*\\d|hoc thu\\s*\\d|thứ\\s*\\d|thu\\s*\\d|tiết\\s*\\d|tiet\\s*\\d|sáng|chống|chiều|chieu|không|khong|nhé|nhe|giùm|gium|giúp|giup).*", "").trim();
            if (kw.equalsIgnoreCase("học phần") || kw.equalsIgnoreCase("hoc phan")
                    || kw.equalsIgnoreCase("môn học") || kw.equalsIgnoreCase("mon hoc")
                    || kw.equalsIgnoreCase("lớp học") || kw.equalsIgnoreCase("lop hoc")
                    || kw.equalsIgnoreCase("tất cả") || kw.equalsIgnoreCase("tat ca")
                    || kw.equalsIgnoreCase("lớp") || kw.equalsIgnoreCase("lop")
                    || kw.isBlank()) {
                return null;
            }
            return kw;
        }
        return null;
    }

    private String removeDiacritics(String str) {
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfd).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
