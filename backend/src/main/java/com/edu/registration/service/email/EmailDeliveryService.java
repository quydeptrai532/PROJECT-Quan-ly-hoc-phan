package com.edu.registration.service.email;

import com.edu.registration.config.EmailProperties;
import com.edu.registration.entity.EmailDelivery;
import com.edu.registration.entity.EmailDeliveryStatus;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.EmailDeliveryException;
import com.edu.registration.repository.EmailDeliveryRepository;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final EmailDeliveryRepository emailDeliveryRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final EmailProvider emailProvider;
    private final EmailProperties emailProperties;
    private final TransactionTemplate requiresNewTemplate;

    @Autowired
    public EmailDeliveryService(EmailDeliveryRepository emailDeliveryRepository,
                                StudentRepository studentRepository,
                                LecturerRepository lecturerRepository,
                                EmailProvider emailProvider,
                                EmailProperties emailProperties,
                                PlatformTransactionManager transactionManager) {
        this.emailDeliveryRepository = emailDeliveryRepository;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.emailProvider = emailProvider;
        this.emailProperties = emailProperties;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    /**
     * Gửi email thông báo độc lập, cách ly hoàn toàn khỏi transaction nghiệp vụ đã commit.
     */
    public void deliverEmailSafely(User recipient, String eventType, String idempotencyKey,
                                   String subject, String bodyHtml) {
        if (recipient == null || !recipient.isTrangThai() || recipient.getId() == null) {
            return;
        }
        try {
            if (requiresNewTemplate != null) {
                requiresNewTemplate.executeWithoutResult(status ->
                        processEmailDelivery(recipient, eventType, idempotencyKey, subject, bodyHtml));
            } else {
                processEmailDelivery(recipient, eventType, idempotencyKey, subject, bodyHtml);
            }
        } catch (RuntimeException ex) {
            log.warn("Không thể gửi email thông báo [{} - key={}]; nghiệp vụ đã commit không bị ảnh hưởng: {}",
                    eventType, idempotencyKey, ex.getMessage());
        }
    }

    @SuppressWarnings("java:S3776")
    private void processEmailDelivery(User recipient, String eventType, String idempotencyKey,
                                     String subject, String bodyHtml) {
        if (recipient == null || !recipient.isTrangThai() || recipient.getId() == null) {
            return;
        }

        // Chống gửi trùng lặp theo idempotency key
        if (emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(recipient.getId(), idempotencyKey)) {
            log.debug("Bỏ qua email trùng idempotencyKey={} cho recipientId={}", idempotencyKey, recipient.getId());
            return;
        }

        // Kiểm tra và lấy địa chỉ email từ dữ liệu máy chủ (server-side resolution)
        String recipientEmail = resolveRecipientEmailFromServer(recipient);
        if (recipientEmail == null || recipientEmail.isBlank()) {
            recordEmailDelivery(recipient, "unknown@server.local", eventType, idempotencyKey,
                    subject, bodyHtml, EmailDeliveryStatus.SKIPPED, 0, null,
                    "Không tìm thấy địa chỉ email hợp lệ của người nhận trên hệ thống máy chủ");
            return;
        }

        // Kiểm tra cấu hình bật/tắt email
        if (!emailProperties.isEnabled()) {
            recordEmailDelivery(recipient, recipientEmail, eventType, idempotencyKey,
                    subject, bodyHtml, EmailDeliveryStatus.SKIPPED, 0, null,
                    "Kênh email đang tắt trong cấu hình hệ thống (app.notification.email.enabled=false)");
            log.debug("Kênh email đang tắt; bỏ qua gửi email [key={}] tới {}", idempotencyKey, maskEmail(recipientEmail));
            return;
        }

        // Kiểm tra nhà cung cấp và thông tin xác thực email
        if (!emailProvider.isConfigured()) {
            int maxRetries = Math.max(1, emailProperties.getMaxRetries());
            LocalDateTime nextRetry = maxRetries > 1
                    ? LocalDateTime.now().plusSeconds(emailProperties.getRetryDelaySeconds())
                    : null;
            EmailDeliveryStatus status = maxRetries > 1 ? EmailDeliveryStatus.FAILED : EmailDeliveryStatus.EXHAUSTED;
            recordEmailDelivery(recipient, recipientEmail, eventType, idempotencyKey,
                    subject, bodyHtml, status, 1, nextRetry,
                    "Email provider/credentials chưa được cấu hình trên máy chủ");
            log.warn("Email provider/credentials chưa được cấu hình. Ghi nhận trạng thái [{}] cho key={}",
                    status, idempotencyKey);
            return;
        }

        // Tạo bản ghi giao dịch gửi
        EmailDelivery delivery;
        try {
            delivery = recordEmailDelivery(recipient, recipientEmail, eventType, idempotencyKey,
                    subject, bodyHtml, EmailDeliveryStatus.PENDING, 0, null, null);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Bỏ qua email trùng bản ghi đồng thời idempotencyKey={} cho recipientId={}",
                    idempotencyKey, recipient.getId());
            return;
        }

        // Tiến hành gửi email qua provider
        try {
            emailProvider.sendEmail(recipientEmail, subject, bodyHtml,
                    emailProperties.getFromAddress(), emailProperties.getFromName());
            delivery.setStatus(EmailDeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());
            delivery.setErrorMessage(null);
            delivery.setUpdatedAt(LocalDateTime.now());
            emailDeliveryRepository.save(delivery);
            log.info("Đã gửi thành công email [{}] tới recipientId={}, email={}",
                    eventType, recipient.getId(), maskEmail(recipientEmail));
        } catch (Exception ex) {
            int newRetry = delivery.getRetryCount() + 1;
            delivery.setRetryCount(newRetry);
            delivery.setUpdatedAt(LocalDateTime.now());
            String safeError = sanitizeErrorMessage(ex.getMessage());
            delivery.setErrorMessage(safeError);

            if (newRetry < delivery.getMaxRetries()) {
                delivery.setStatus(EmailDeliveryStatus.FAILED);
                delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(emailProperties.getRetryDelaySeconds()));
                log.warn("Gửi email thất bại lần {}/{} cho key={}: {}",
                        newRetry, delivery.getMaxRetries(), idempotencyKey, safeError);
            } else {
                delivery.setStatus(EmailDeliveryStatus.EXHAUSTED);
                delivery.setNextRetryAt(null);
                log.error("Đã hết số lần retry gửi email ({}/{}) cho key={}: {}",
                        newRetry, delivery.getMaxRetries(), idempotencyKey, safeError);
            }
            emailDeliveryRepository.save(delivery);
        }
    }

    /**
     * Thực hiện thử lại gửi email cho các bản ghi FAILED còn lượt retry.
     */
    @Transactional
    public int retryFailedEmails() {
        if (!emailProperties.isEnabled() || !emailProvider.isConfigured()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        List<EmailDelivery> pendingRetries = emailDeliveryRepository.findRetryableEmails(
                List.of(EmailDeliveryStatus.FAILED),
                emailProperties.getMaxRetries(),
                now,
                PageRequest.of(0, 50)
        );

        if (pendingRetries.isEmpty()) {
            return 0;
        }

        int processedCount = 0;
        for (EmailDelivery delivery : pendingRetries) {
            try {
                emailProvider.sendEmail(delivery.getRecipientEmail(), delivery.getSubject(),
                        delivery.getContent(), emailProperties.getFromAddress(), emailProperties.getFromName());
                delivery.setStatus(EmailDeliveryStatus.SENT);
                delivery.setSentAt(LocalDateTime.now());
                delivery.setErrorMessage(null);
                delivery.setUpdatedAt(LocalDateTime.now());
                emailDeliveryRepository.save(delivery);
                processedCount++;
                log.info("Retry gửi email thành công cho id={}, recipient={}",
                        delivery.getId(), maskEmail(delivery.getRecipientEmail()));
            } catch (Exception ex) {
                int newRetry = delivery.getRetryCount() + 1;
                delivery.setRetryCount(newRetry);
                delivery.setUpdatedAt(LocalDateTime.now());
                String safeError = sanitizeErrorMessage(ex.getMessage());
                delivery.setErrorMessage(safeError);

                if (newRetry < delivery.getMaxRetries()) {
                    delivery.setStatus(EmailDeliveryStatus.FAILED);
                    delivery.setNextRetryAt(LocalDateTime.now().plusSeconds(emailProperties.getRetryDelaySeconds()));
                } else {
                    delivery.setStatus(EmailDeliveryStatus.EXHAUSTED);
                    delivery.setNextRetryAt(null);
                }
                emailDeliveryRepository.save(delivery);
                processedCount++;
            }
        }
        return processedCount;
    }

    /**
     * Tra cứu địa chỉ email người nhận từ database hệ thống; tuyệt đối không nhận email từ client.
     */
    public String resolveRecipientEmailFromServer(User recipient) {
        if (recipient == null || recipient.getId() == null) {
            return null;
        }
        Role role = recipient.getRole();
        if (role == Role.SINH_VIEN) {
            Optional<Student> studentOpt = studentRepository.findByUserId(recipient.getId());
            if (studentOpt.isEmpty()) {
                studentOpt = studentRepository.findByUser(recipient);
            }
            return studentOpt.map(Student::getEmail).filter(e -> !e.isBlank()).orElse(null);
        }
        if (role == Role.GIANG_VIEN) {
            Optional<Lecturer> lecturerOpt = lecturerRepository.findByUserId(recipient.getId());
            if (lecturerOpt.isEmpty()) {
                lecturerOpt = lecturerRepository.findByUser(recipient);
            }
            return lecturerOpt.map(Lecturer::getEmail).filter(e -> !e.isBlank()).orElse(null);
        }
        return null;
    }

    private EmailDelivery recordEmailDelivery(User recipient, String recipientEmail, String eventType,
                                              String idempotencyKey, String subject, String content,
                                              EmailDeliveryStatus status, int retryCount,
                                              LocalDateTime nextRetryAt, String errorMessage) {
        EmailDelivery delivery = new EmailDelivery();
        delivery.setRecipient(recipient);
        delivery.setRecipientEmail(recipientEmail);
        delivery.setEventType(eventType);
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setSubject(subject);
        delivery.setContent(content);
        delivery.setStatus(status);
        delivery.setRetryCount(retryCount);
        delivery.setMaxRetries(Math.max(1, emailProperties.getMaxRetries()));
        delivery.setNextRetryAt(nextRetryAt);
        delivery.setErrorMessage(errorMessage);
        delivery.setCreatedAt(LocalDateTime.now());
        delivery.setUpdatedAt(LocalDateTime.now());
        return emailDeliveryRepository.save(delivery);
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + (atIndex >= 0 ? email.substring(atIndex) : "");
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    public static String sanitizeErrorMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown error";
        }
        String clean = raw.trim();
        if (clean.length() > 490) {
            clean = clean.substring(0, 487) + "...";
        }
        // Loại bỏ các từ khóa nhạy cảm nếu có
        return clean.replaceAll("(?i)(password|secret|token|bearer)\\s*[:=]\\s*\\S+", "$1=***");
    }
}
