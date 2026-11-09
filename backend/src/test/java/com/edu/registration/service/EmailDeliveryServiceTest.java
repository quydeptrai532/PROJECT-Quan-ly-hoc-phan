package com.edu.registration.service;

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
import com.edu.registration.service.email.EmailDeliveryService;
import com.edu.registration.service.email.EmailProvider;
import com.edu.registration.service.email.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailDeliveryServiceTest {

    @Mock
    private EmailDeliveryRepository emailDeliveryRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private LecturerRepository lecturerRepository;
    @Mock
    private EmailProvider emailProvider;

    private EmailProperties emailProperties;
    private EmailTemplateService emailTemplateService;
    private EmailDeliveryService emailDeliveryService;

    private User studentUser;
    private Student student;
    private User lecturerUser;
    private Lecturer lecturer;

    @BeforeEach
    void setUp() {
        emailProperties = new EmailProperties();
        emailProperties.setEnabled(true);
        emailProperties.setFromAddress("no-reply@university.edu.vn");
        emailProperties.setFromName("Hệ thống Quản lý Đào tạo");
        emailProperties.setMaxRetries(3);
        emailProperties.setRetryDelaySeconds(60);

        emailTemplateService = new EmailTemplateService();
        emailDeliveryService = new EmailDeliveryService(
                emailDeliveryRepository,
                studentRepository,
                lecturerRepository,
                emailProvider,
                emailProperties,
                null
        );

        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setUsername("sv001");
        studentUser.setRole(Role.SINH_VIEN);
        studentUser.setTrangThai(true);

        student = new Student();
        student.setId(100L);
        student.setUser(studentUser);
        student.setMa("SV001");
        student.setHoTen("Nguyễn Văn An");
        student.setEmail("an.nv@student.edu.vn");

        lecturerUser = new User();
        lecturerUser.setId(20L);
        lecturerUser.setUsername("gv001");
        lecturerUser.setRole(Role.GIANG_VIEN);
        lecturerUser.setTrangThai(true);

        lecturer = new Lecturer();
        lecturer.setId(200L);
        lecturer.setUser(lecturerUser);
        lecturer.setMa("GV001");
        lecturer.setHoTen("TS. Trần Văn Bình");
        lecturer.setEmail("binh.tv@university.edu.vn");
    }

    @Test
    @DisplayName("Email tắt: Ghi nhận trạng thái SKIPPED, không gọi gửi provider và thông báo in-app vẫn an toàn")
    void testEmailDisabled_RecordsSkipped_DoesNotCallProvider() {
        emailProperties.setEnabled(false);
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(10L, "reg:1:CONFIRMED"))
                .thenReturn(false);
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(emailDeliveryRepository.save(any(EmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "reg:1:CONFIRMED",
                "Xác nhận đăng ký học phần",
                "<p>Nội dung</p>"
        );

        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(emailDeliveryRepository).save(captor.capture());
        EmailDelivery saved = captor.getValue();
        assertEquals(EmailDeliveryStatus.SKIPPED, saved.getStatus());
        assertTrue(saved.getErrorMessage().contains("Kênh email đang tắt"));
        verify(emailProvider, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Provider chưa cấu hình: Ghi nhận FAILED kèm lý do rõ ràng, tuyệt đối không giả lập thành SENT")
    void testEmailProviderUnconfigured_MarksFailed_NeverPretendsSent() {
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(10L, "reg:1:CONFIRMED"))
                .thenReturn(false);
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(emailProvider.isConfigured()).thenReturn(false);
        when(emailDeliveryRepository.save(any(EmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "reg:1:CONFIRMED",
                "Xác nhận đăng ký",
                "<p>Nội dung</p>"
        );

        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(emailDeliveryRepository).save(captor.capture());
        EmailDelivery saved = captor.getValue();
        assertEquals(EmailDeliveryStatus.FAILED, saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextRetryAt());
        assertTrue(saved.getErrorMessage().contains("chưa được cấu hình"));
        verify(emailProvider, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Gửi email thành công: Chuyển trạng thái sang SENT và lưu mốc thời gian sentAt")
    void testEmailDelivery_Success() {
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(10L, "reg:1:CONFIRMED"))
                .thenReturn(false);
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(emailProvider.isConfigured()).thenReturn(true);
        when(emailDeliveryRepository.save(any(EmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "reg:1:CONFIRMED",
                "Xác nhận đăng ký học phần IT001",
                "<p>Bạn đã đăng ký thành công</p>"
        );

        verify(emailProvider).sendEmail(
                eq("an.nv@student.edu.vn"),
                eq("Xác nhận đăng ký học phần IT001"),
                eq("<p>Bạn đã đăng ký thành công</p>"),
                eq("no-reply@university.edu.vn"),
                eq("Hệ thống Quản lý Đào tạo")
        );

        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(emailDeliveryRepository, times(2)).save(captor.capture());
        EmailDelivery finalRecord = captor.getAllValues().get(1);
        assertEquals(EmailDeliveryStatus.SENT, finalRecord.getStatus());
        assertNotNull(finalRecord.getSentAt());
        assertNull(finalRecord.getErrorMessage());
    }

    @Test
    @DisplayName("Chống gửi trùng: Idempotency key đã tồn tại sẽ bỏ qua gửi lại")
    void testEmailDelivery_IdempotencyKey_PreventsDuplicate() {
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(10L, "reg:1:CONFIRMED"))
                .thenReturn(true);

        emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "reg:1:CONFIRMED",
                "Xác nhận đăng ký",
                "<p>Nội dung</p>"
        );

        verify(emailProvider, never()).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(emailDeliveryRepository, never()).save(any(EmailDelivery.class));
    }

    @Test
    @DisplayName("Retry email thất bại chạm ngưỡng maxRetries: Chuyển sang EXHAUSTED")
    void testEmailDelivery_Failure_ExhaustsRetries() {
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(10L, "reg:1:CONFIRMED"))
                .thenReturn(false);
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(emailProvider.isConfigured()).thenReturn(true);

        when(emailDeliveryRepository.save(any(EmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        org.mockito.Mockito.doThrow(new EmailDeliveryException("SMTP connection timed out"))
                .when(emailProvider).sendEmail(anyString(), anyString(), anyString(), anyString(), anyString());

        emailProperties.setMaxRetries(1); // Set maxRetries = 1 để thử lần đầu là chạm ngưỡng

        emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "reg:1:CONFIRMED",
                "Xác nhận đăng ký",
                "<p>Nội dung</p>"
        );

        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(emailDeliveryRepository, times(2)).save(captor.capture());
        EmailDelivery failedRecord = captor.getAllValues().get(1);
        assertEquals(EmailDeliveryStatus.EXHAUSTED, failedRecord.getStatus());
        assertEquals(1, failedRecord.getRetryCount());
        assertTrue(failedRecord.getErrorMessage().contains("SMTP connection timed out"));
    }

    @Test
    @DisplayName("Thực hiện retryFailedEmails: Gửi lại các email FAILED và cập nhật SENT khi thành công")
    void testRetryFailedEmails_RetriesAndMarksSent() {
        when(emailProvider.isConfigured()).thenReturn(true);

        EmailDelivery failedItem = new EmailDelivery();
        failedItem.setId(99L);
        failedItem.setRecipient(studentUser);
        failedItem.setRecipientEmail("an.nv@student.edu.vn");
        failedItem.setStatus(EmailDeliveryStatus.FAILED);
        failedItem.setRetryCount(1);
        failedItem.setMaxRetries(3);
        failedItem.setSubject("Tiêu đề email");
        failedItem.setContent("<p>Nội dung</p>");

        when(emailDeliveryRepository.findRetryableEmails(any(), anyInt(), any(), any(Pageable.class)))
                .thenReturn(List.of(failedItem));
        when(emailDeliveryRepository.save(any(EmailDelivery.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        int processed = emailDeliveryService.retryFailedEmails();
        assertEquals(1, processed);
        assertEquals(EmailDeliveryStatus.SENT, failedItem.getStatus());
        assertNotNull(failedItem.getSentAt());
        assertNull(failedItem.getErrorMessage());
        verify(emailProvider).sendEmail(
                eq("an.nv@student.edu.vn"),
                eq("Tiêu đề email"),
                eq("<p>Nội dung</p>"),
                eq("no-reply@university.edu.vn"),
                eq("Hệ thống Quản lý Đào tạo")
        );
    }

    @Test
    @DisplayName("Bảo mật điểm: Email công bố điểm KHÔNG chứa điểm cá nhân trong subject hay body")
    void testGradePublishedEmail_NeverContainsPersonalGrades() {
        EmailTemplateService.EmailContent content = emailTemplateService.buildGradePublishedEmail(
                "IT001.01", "IT001", "Lập trình Java", "Học kỳ 1", "2026-2027", LocalDateTime.now()
        );

        // Subject và Body tuyệt đối không chứa điểm số (ví dụ: điểm quá trình, kết thúc, tổng kết)
        assertFalse(content.subject().contains("điểm số"));
        assertFalse(content.subject().matches(".*\\b\\d+\\.\\d+\\b.*"));
        assertFalse(content.bodyHtml().contains("diemTongKet"));
        assertFalse(content.bodyHtml().contains("diemQuaTrinh"));
        assertFalse(content.bodyHtml().contains("diemCuoiKy"));
        // Phải chứa thông báo yêu cầu đăng nhập vào /diem
        assertTrue(content.bodyHtml().contains("/diem"));
        assertTrue(content.bodyHtml().contains("Vì lý do bảo mật và bảo vệ dữ liệu cá nhân"));
    }

    @Test
    @DisplayName("Chống XSS: Template email escape nội dung động")
    void testEmailTemplateService_EscapesDynamicContent() {
        String maliciousTitle = "<script>alert('xss')</script>";
        String maliciousContent = "<img src=x onerror=alert(1)> & nội dung";

        EmailTemplateService.EmailContent content = emailTemplateService.buildClassAnnouncementEmail(
                "IT001.01", "Lập trình", maliciousTitle, maliciousContent, LocalDateTime.now()
        );

        assertFalse(content.bodyHtml().contains("<script>"));
        assertFalse(content.bodyHtml().contains("<img src=x onerror=alert(1)>"));
        assertTrue(content.bodyHtml().contains("&lt;script&gt;"));
        assertTrue(content.bodyHtml().contains("&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    @DisplayName("Lỗi ngoại lệ gửi email không gây throw hoặc rollback nghiệp vụ")
    void testEmailFailure_DoesNotThrowToCaller() {
        when(emailDeliveryRepository.existsByRecipientIdAndIdempotencyKey(any(), any()))
                .thenThrow(new RuntimeException("Database error in email delivery"));

        assertDoesNotThrow(() -> emailDeliveryService.deliverEmailSafely(
                studentUser,
                "COURSE_REGISTRATION_CONFIRMED",
                "key-1",
                "Subject",
                "Body"
        ));
    }
}
