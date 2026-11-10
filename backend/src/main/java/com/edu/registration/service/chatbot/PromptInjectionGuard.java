package com.edu.registration.service.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Phòng vệ chống Prompt Injection, Jailbreak và cố ý truy xuất dữ liệu ngoài quyền (QLSV-106 / TASK-AI-01).
 */
@Component
public class PromptInjectionGuard {

    private static final Logger log = LoggerFactory.getLogger(PromptInjectionGuard.class);

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            // Chỉ thị bỏ qua quy tắc / Jailbreak
            Pattern.compile("ignore\\s+.*(previous|prior|above)\\s+.*(instructions|prompts|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+.*(previous|prior|above)\\s+.*(instructions|prompts|rules)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("bo\\s*qua\\s+.*(huong\\s*dan|chi\\s*dan|quy\\s*tac|lenh)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("quen\\s+.*(quy\\s*tac|chi\\s*dan|huong\\s*dan)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ghi\\s*de\\s+.*(he\\s*thong|quy\\s*tac|chi\\s*dan)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("system\\s*override|developer\\s*mode|dan\\s*mode|jailbreak", Pattern.CASE_INSENSITIVE),

            // Đóng vai trò mạo nhận quyền
            Pattern.compile("(dong\\s*vai|gia\\s*lap|tro\\s*thanh)\\s+.*(admin|quan\\s*tri|he\\s*thong|root)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(act|pretend)\\s+as\\s+.*(admin|root|system|developer)", Pattern.CASE_INSENSITIVE),

            // Đòi trích xuất prompt gốc / token / mật khẩu / secret
            Pattern.compile(".*(show|print|reveal|display|output|repeat).*(system\\s*prompt|instructions|secret|token|password).*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*(cho\\s+(toi\\s+)?xem|hien\\s*thi|in\\s*ra|tiet\\s*lo|xem).*(system\\s*prompt|mat\\s*khau|password|token|jwt|khoa\\s*bi\\s*mat|api\\s*key).*", Pattern.CASE_INSENSITIVE),

            // Đòi truy xuất dữ liệu cá nhân của sinh viên khác / trái quyền
            Pattern.compile(".*(diem|thong\\s*tin|ho\\s*so|token|jwt|mat\\s*khau|password).*(sinh\\s*vien\\s*khac|tat\\s*ca\\s*sinh\\s*vien|ban\\s*hoc|nguoi\\s*khac|\\bsv\\d+).*", Pattern.CASE_INSENSITIVE),

            // Injection mã lệnh CSDL độc hại
            Pattern.compile("union\\s+select|select\\s+.+\\s+from\\s+users|drop\\s+table|delete\\s+from\\s+users", Pattern.CASE_INSENSITIVE)
    );

    public record GuardResult(boolean safe, String reason) {
        public static GuardResult allow() {
            return new GuardResult(true, null);
        }

        public static GuardResult block(String reason) {
            return new GuardResult(false, reason);
        }
    }

    public GuardResult checkInput(String input) {
        if (input == null || input.isBlank()) {
            return GuardResult.allow();
        }

        String normalized = removeDiacritics(input).toLowerCase(Locale.ROOT);

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(normalized).find() || pattern.matcher(input).find()) {
                log.warn("Phát hiện nguy cơ Prompt Injection / Bảo mật trong câu hỏi chatbot: [{}]", input);
                return GuardResult.block("Yêu cầu chứa chỉ thị vi phạm chính sách bảo mật (Prompt Injection / Trích xuất dữ liệu không hợp lệ). Thao tác bị chặn để đảm bảo an toàn hệ thống.");
            }
        }

        return GuardResult.allow();
    }

    private String removeDiacritics(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
