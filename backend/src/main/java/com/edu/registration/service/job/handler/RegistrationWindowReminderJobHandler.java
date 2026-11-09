package com.edu.registration.service.job.handler;

import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.service.NotificationService;
import com.edu.registration.service.job.BackgroundJobHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Handler gửi thông báo nhắc nhở đóng cổng đăng ký học phần qua tác vụ nền (QLSV-93 / TASK-OP-06).
 */
@Component
public class RegistrationWindowReminderJobHandler implements BackgroundJobHandler {

    private static final Logger log = LoggerFactory.getLogger(RegistrationWindowReminderJobHandler.class);
    public static final String JOB_TYPE = "REGISTRATION_WINDOW_REMINDER";

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public RegistrationWindowReminderJobHandler(NotificationService notificationService,
                                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void execute(BackgroundJob job) throws Exception {
        if (notificationService == null || job.getPayload() == null) {
            return;
        }

        JsonNode root = objectMapper.readTree(job.getPayload());
        long semesterId = root.path("semesterId").asLong();
        String dateStr = root.path("targetDate").asText();
        LocalDate targetDate = dateStr != null && !dateStr.isBlank()
                ? LocalDate.parse(dateStr)
                : LocalDate.now();

        notificationService.remindClosingRegistrationWindow(semesterId, targetDate);
        log.info("Tác vụ nền [id={}] đã gửi nhắc nhở đợt đăng ký cho semesterId={}, targetDate={}",
                job.getId(), semesterId, targetDate);
    }
}
