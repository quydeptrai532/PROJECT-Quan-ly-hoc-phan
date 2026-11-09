package com.edu.registration.service.email;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dịch vụ tạo template email chuẩn hóa và an toàn (QLSV-92 / TASK-OP-05).
 * - Escape toàn bộ nội dung động để chống XSS.
 * - Tuyệt đối không đưa điểm cá nhân hoặc thông tin nhạy cảm vào subject/body.
 * - Chuẩn hóa giao diện email hiển thị thời điểm, môn học, lớp và bước tiếp theo.
 */
@Service
public class EmailTemplateService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record EmailContent(String subject, String bodyHtml) {}

    public String escape(String raw) {
        return raw == null ? "" : HtmlUtils.htmlEscape(raw.trim());
    }

    private String formatTime(LocalDateTime time) {
        return (time != null ? time : LocalDateTime.now()).format(TIME_FORMATTER);
    }

    private String wrapHtml(String title, String badgeText, String badgeTone, String mainHtml, String nextStepText, String targetUrl) {
        String safeTitle = escape(title);
        String safeBadge = escape(badgeText);
        String safeNextStep = escape(nextStepText);
        String safeUrl = escape(targetUrl);

        String badgeBg = "AMBER".equalsIgnoreCase(badgeTone) ? "#fef3c7" : "#e0e7ff";
        String badgeColor = "AMBER".equalsIgnoreCase(badgeTone) ? "#92400e" : "#3730a3";

        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; color: #1e293b; margin: 0; padding: 24px; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
                .header { background: #1e3a8a; color: #ffffff; padding: 20px 24px; }
                .header h1 { margin: 0; font-size: 18px; font-weight: 600; }
                .content { padding: 24px; }
                .badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-size: 12px; font-weight: 600; margin-bottom: 12px; }
                .title { font-size: 16px; font-weight: 600; color: #0f172a; margin: 0 0 12px 0; }
                .body-text { font-size: 14px; line-height: 1.6; color: #334155; margin: 0 0 16px 0; }
                .next-step { background: #f1f5f9; border-left: 4px solid #3b82f6; padding: 12px 16px; border-radius: 4px; font-size: 13px; color: #1e293b; margin: 16px 0; }
                .btn { display: inline-block; background: #2563eb; color: #ffffff !important; text-decoration: none; padding: 10px 18px; border-radius: 6px; font-size: 14px; font-weight: 500; margin-top: 8px; }
                .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px 24px; font-size: 12px; color: #64748b; line-height: 1.5; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>Hệ thống Quản lý Đào tạo</h1>
                </div>
                <div class="content">
                  <span class="badge" style="background-color: %s; color: %s;">%s</span>
                  <div class="title">%s</div>
                  <div class="body-text">%s</div>
                  %s
                  %s
                </div>
                <div class="footer">
                  <p style="margin: 0 0 6px 0;"><strong>Lưu ý bảo mật:</strong> Thông báo tự động từ Hệ thống Quản lý Đào tạo. Vui lòng không trả lời email này.</p>
                  <p style="margin: 0;">Vì lý do an toàn thông tin, điểm số chi tiết và dữ liệu cá nhân chỉ được tra cứu sau khi đăng nhập vào hệ thống chính thức.</p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                safeTitle,
                badgeBg,
                badgeColor,
                safeBadge,
                safeTitle,
                mainHtml,
                safeNextStep.isEmpty() ? "" : "<div class=\"next-step\"><strong>Bước tiếp theo:</strong> " + safeNextStep + "</div>",
                safeUrl.isEmpty() ? "" : "<div style=\"margin-top: 16px;\"><a class=\"btn\" href=\"" + safeUrl + "\">Truy cập hệ thống</a></div>"
        );
    }

    /**
     * Template sự kiện Đăng ký / Hủy đăng ký / Hủy lớp học phần (TASK-ST-07).
     */
    public EmailContent buildRegistrationEmail(boolean registered, boolean sectionCancelled,
                                               String sectionCode, String subjectCode, String subjectName,
                                               String semesterName, LocalDateTime occurredAt, String reason) {
        String safeSectionCode = escape(sectionCode);
        String safeSubjectName = escape(subjectName);
        String safeSemester = escape(semesterName);
        String safeTime = formatTime(occurredAt);
        String safeReason = reason != null && !reason.isBlank() ? escape(reason.trim()) : "";

        String subject;
        String title;
        String badgeText;
        String badgeTone = "INDIGO";
        String mainHtml;
        String nextStep;
        String targetUrl;

        if (registered) {
            subject = "[QLSV] Xác nhận đăng ký học phần " + sectionCode;
            title = "Đăng ký học phần thành công";
            badgeText = "Đã đăng ký";
            mainHtml = String.format(
                    "Bạn đã đăng ký thành công lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>) thuộc học kỳ <strong>%s</strong> vào lúc %s.",
                    safeSectionCode, safeSubjectName, safeSemester, safeTime
            );
            nextStep = "Kiểm tra danh sách học phần đã đăng ký và thời khóa biểu cá nhân trên hệ thống.";
            targetUrl = "/hoc-phan-cua-toi";
        } else if (sectionCancelled) {
            subject = "[QLSV] [Thông báo] Lớp học phần " + sectionCode + " đã bị hủy";
            title = "Lớp học phần bị hủy do kế hoạch đào tạo";
            badgeText = "Lớp bị hủy";
            badgeTone = "AMBER";
            String reasonHtml = safeReason.isEmpty() ? "" : "<br>Lý do: <em>" + safeReason + "</em>";
            mainHtml = String.format(
                    "Lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>, %s) đã bị hủy vào lúc %s.%s<br>Trạng thái đăng ký của bạn đã được chuyển sang <strong>Đã hủy</strong>.",
                    safeSectionCode, safeSubjectName, safeSemester, safeTime, reasonHtml
            );
            nextStep = "Truy cập cổng đăng ký học phần để chọn và đăng ký lớp học phần thay thế nếu cần.";
            targetUrl = "/dang-ky";
        } else {
            subject = "[QLSV] Xác nhận hủy đăng ký học phần " + sectionCode;
            title = "Đã hủy đăng ký học phần";
            badgeText = "Đã hủy";
            String reasonHtml = safeReason.isEmpty() ? "" : "<br>Lý do: <em>" + safeReason + "</em>";
            mainHtml = String.format(
                    "Bạn đã hủy đăng ký lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>) thuộc học kỳ <strong>%s</strong> vào lúc %s.%s",
                    safeSectionCode, safeSubjectName, safeSemester, safeTime, reasonHtml
            );
            nextStep = "Kiểm tra lại thời khóa biểu cá nhân hoặc chọn lớp khác trong đợt đăng ký còn mở.";
            targetUrl = "/thoi-khoa-bieu";
        }

        return new EmailContent(subject, wrapHtml(title, badgeText, badgeTone, mainHtml, nextStep, targetUrl));
    }

    /**
     * Template sự kiện Đăng ký gửi giảng viên (TASK-ST-07).
     */
    public EmailContent buildLecturerRegistrationEmail(boolean registered, String sectionCode,
                                                       String subjectName, LocalDateTime occurredAt) {
        String safeSectionCode = escape(sectionCode);
        String safeSubjectName = escape(subjectName);
        String safeTime = formatTime(occurredAt);
        String action = registered ? "đăng ký vào" : "hủy đăng ký khỏi";

        String subject = "[QLSV] Thay đổi danh sách sinh viên lớp " + sectionCode;
        String title = registered ? "Sinh viên đăng ký lớp học phần" : "Sinh viên hủy đăng ký lớp học phần";
        String mainHtml = String.format(
                "Có sinh viên vừa %s lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>) lúc %s.",
                action, safeSectionCode, safeSubjectName, safeTime
        );
        String nextStep = "Truy cập danh sách lớp phụ trách để theo dõi sĩ số hiện tại.";
        return new EmailContent(subject, wrapHtml(title, "Cập nhật sĩ số", "INDIGO", mainHtml, nextStep, "/lop-cua-toi"));
    }

    /**
     * Template thông báo hủy lớp gửi giảng viên (TASK-ST-07).
     */
    public EmailContent buildLecturerSectionCancelledEmail(String sectionCode, String subjectName,
                                                           String semesterName, LocalDateTime occurredAt, String reason) {
        String safeSectionCode = escape(sectionCode);
        String safeSubjectName = escape(subjectName);
        String safeSemester = escape(semesterName);
        String safeTime = formatTime(occurredAt);
        String safeReason = reason != null && !reason.isBlank() ? escape(reason.trim()) : "";

        String subject = "[QLSV] Thông báo hủy lớp học phần " + sectionCode;
        String title = "Lớp học phần phụ trách đã bị hủy";
        String reasonHtml = safeReason.isEmpty() ? "" : "<br>Lý do: <em>" + safeReason + "</em>";
        String mainHtml = String.format(
                "Lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>, %s) đã bị hủy lúc %s.%s",
                safeSectionCode, safeSubjectName, safeSemester, safeTime, reasonHtml
        );
        String nextStep = "Kiểm tra danh mục lớp phụ trách trên cổng giảng viên.";
        return new EmailContent(subject, wrapHtml(title, "Lớp bị hủy", "AMBER", mainHtml, nextStep, "/lop-cua-toi"));
    }

    /**
     * Template thông báo lớp học phần từ giảng viên (TASK-LC-03 / QLSV-68).
     */
    public EmailContent buildClassAnnouncementEmail(String sectionCode, String subjectName,
                                                    String announcementTitle, String announcementContent,
                                                    LocalDateTime occurredAt) {
        String safeSectionCode = escape(sectionCode);
        String safeSubjectName = escape(subjectName);
        String safeTitle = escape(announcementTitle);
        String safeContent = escape(announcementContent).replace("\n", "<br>");
        String safeTime = formatTime(occurredAt);

        String subject = "[QLSV] Thông báo lớp " + sectionCode + ": " + announcementTitle;
        String title = "Thông báo mới từ Giảng viên phụ trách";
        String mainHtml = String.format(
                "Giảng viên lớp <strong>%s</strong> (Môn: <strong>%s</strong>) đã đăng thông báo mới lúc %s:<br><br>"
                        + "<strong>Tiêu đề:</strong> %s<br><br>"
                        + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 6px;\">%s</div>",
                safeSectionCode, safeSubjectName, safeTime, safeTitle, safeContent
        );
        String nextStep = "Truy cập mục Học phần của tôi để xem chi tiết trao đổi.";
        return new EmailContent(subject, wrapHtml(title, "Thông báo lớp", "INDIGO", mainHtml, nextStep, "/hoc-phan-cua-toi"));
    }

    /**
     * Template thông báo công bố bảng điểm lớp học phần (TASK-ST-09 / QLSV-65).
     * Tuyệt đối không đưa điểm cá nhân hoặc thông tin nhạy cảm vào subject/body.
     */
    public EmailContent buildGradePublishedEmail(String sectionCode, String subjectCode, String subjectName,
                                                 String semesterName, String academicYear, LocalDateTime occurredAt) {
        String safeSectionCode = escape(sectionCode);
        String safeSubjectLabel = escape(subjectCode.isBlank() ? subjectName : subjectCode + " — " + subjectName);
        String safeSemester = escape(academicYear.isBlank() ? semesterName : semesterName + " (" + academicYear + ")");
        String safeTime = formatTime(occurredAt);

        String subject = "[QLSV] Bảng điểm lớp học phần " + sectionCode + " đã được công bố";
        String title = "Công bố điểm lớp học phần";
        String mainHtml = String.format(
                "Bảng điểm lớp học phần <strong>%s</strong> (Môn: <strong>%s</strong>, %s) đã được giảng viên và phòng đào tạo phê duyệt và công bố chính thức lúc %s.<br><br>"
                        + "<em>Lưu ý: Vì lý do bảo mật và bảo vệ dữ liệu cá nhân, điểm số không được gửi trực tiếp qua email.</em>",
                safeSectionCode, safeSubjectLabel, safeSemester, safeTime
        );
        String nextStep = "Vui lòng đăng nhập vào hệ thống và mở mục Kết quả học tập để tra cứu điểm số chi tiết của bạn.";
        return new EmailContent(subject, wrapHtml(title, "Công bố điểm", "INDIGO", mainHtml, nextStep, "/diem"));
    }

    /**
     * Template tổng quát cho các sự kiện khác (lịch học, buổi nghỉ, phiếu hỗ trợ, v.v.).
     */
    public EmailContent buildGenericEmail(String eventType, String title, String message,
                                          String targetUrl, LocalDateTime occurredAt) {
        String safeTitle = escape(title);
        String safeMessage = escape(message);
        String safeTime = formatTime(occurredAt);

        String subject = "[QLSV] " + title;
        String mainHtml = String.format(
                "%s<br><br><span style=\"color: #64748b; font-size: 13px;\">Thời điểm ghi nhận: %s</span>",
                safeMessage, safeTime
        );
        String nextStep = "Đăng nhập hệ thống để xem chi tiết thông tin và thực hiện các thao tác liên quan.";
        return new EmailContent(subject, wrapHtml(safeTitle, "Thông báo", "INDIGO", mainHtml, nextStep, targetUrl));
    }
}
