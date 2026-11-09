package com.edu.registration.service;

import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.service.email.EmailDeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class NotificationReminderJob {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SemesterRepository hocKyRepository;
    private final NotificationService thongBaoService;
    private final EmailDeliveryService emailDeliveryService;

    public NotificationReminderJob(SemesterRepository hocKyRepository, NotificationService thongBaoService) {
        this(hocKyRepository, thongBaoService, null);
    }

    @Autowired
    public NotificationReminderJob(SemesterRepository hocKyRepository,
                                  NotificationService thongBaoService,
                                  EmailDeliveryService emailDeliveryService) {
        this.hocKyRepository = hocKyRepository;
        this.thongBaoService = thongBaoService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void remindClosingRegistrationWindows() {
        LocalDate today = LocalDate.now(CAMPUS_ZONE);
        hocKyRepository.findAll().stream()
                .filter(semester -> !semester.getDangKyTu().isAfter(today)
                        && !semester.getDangKyDen().isBefore(today)
                        && !semester.getDangKyDen().isAfter(today.plusDays(1)))
                .forEach(semester -> thongBaoService.remindClosingRegistrationWindow(semester.getId(), today));
    }

    /**
     * Tự động thử lại gửi email cho các bản ghi thất bại còn số lần retry (QLSV-92).
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void retryFailedEmails() {
        if (emailDeliveryService != null) {
            emailDeliveryService.retryFailedEmails();
        }
    }
}
