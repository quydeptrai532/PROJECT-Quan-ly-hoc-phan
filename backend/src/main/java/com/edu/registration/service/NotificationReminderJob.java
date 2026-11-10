package com.edu.registration.service;

import com.edu.registration.repository.SemesterRepository;
import com.edu.registration.service.email.EmailDeliveryService;
import com.edu.registration.service.job.DistributedLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class NotificationReminderJob {

    private static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final SemesterRepository hocKyRepository;
    private final NotificationService thongBaoService;
    private final EmailDeliveryService emailDeliveryService;
    private final DistributedLockService distributedLockService;

    public NotificationReminderJob(SemesterRepository hocKyRepository, NotificationService thongBaoService) {
        this(hocKyRepository, thongBaoService, null, null);
    }

    public NotificationReminderJob(SemesterRepository hocKyRepository,
                                  NotificationService thongBaoService,
                                  EmailDeliveryService emailDeliveryService) {
        this(hocKyRepository, thongBaoService, emailDeliveryService, null);
    }

    @Autowired
    public NotificationReminderJob(SemesterRepository hocKyRepository,
                                  NotificationService thongBaoService,
                                  EmailDeliveryService emailDeliveryService,
                                  DistributedLockService distributedLockService) {
        this.hocKyRepository = hocKyRepository;
        this.thongBaoService = thongBaoService;
        this.emailDeliveryService = emailDeliveryService;
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void remindClosingRegistrationWindows() {
        Runnable task = () -> {
            LocalDate today = LocalDate.now(CAMPUS_ZONE);
            hocKyRepository.findAll().stream()
                    .filter(semester -> !semester.getDangKyTu().isAfter(today)
                            && !semester.getDangKyDen().isBefore(today)
                            && !semester.getDangKyDen().isAfter(today.plusDays(1)))
                    .forEach(semester -> thongBaoService.remindClosingRegistrationWindow(semester.getId(), today));
        };

        if (distributedLockService != null) {
            distributedLockService.executeWithLock("lock:job:remind-closing-registration", Duration.ofMinutes(10), task);
        } else {
            task.run();
        }
    }

    /**
     * Tự động thử lại gửi email cho các bản ghi thất bại còn số lần retry (QLSV-92 / TASK-OP-05 / TASK-OP-06).
     * Được bảo vệ bằng khóa phân tán bền vững, chống chạy trùng khi nhiều instance cùng hoạt động.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void retryFailedEmails() {
        if (emailDeliveryService == null) {
            return;
        }

        Runnable task = () -> emailDeliveryService.retryFailedEmails();

        if (distributedLockService != null) {
            distributedLockService.executeWithLock("lock:job:retry-failed-emails", Duration.ofMinutes(4), task);
        } else {
            task.run();
        }
    }
}
