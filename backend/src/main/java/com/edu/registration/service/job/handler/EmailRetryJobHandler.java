package com.edu.registration.service.job.handler;

import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.service.email.EmailDeliveryService;
import com.edu.registration.service.job.BackgroundJobHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler thực thi quét và thử lại các email thất bại qua hệ thống tác vụ nền (QLSV-93 / TASK-OP-06, tích hợp TASK-OP-05).
 */
@Component
public class EmailRetryJobHandler implements BackgroundJobHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryJobHandler.class);
    public static final String JOB_TYPE = "EMAIL_RETRY_BATCH";

    private final EmailDeliveryService emailDeliveryService;

    public EmailRetryJobHandler(EmailDeliveryService emailDeliveryService) {
        this.emailDeliveryService = emailDeliveryService;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void execute(BackgroundJob job) throws Exception {
        if (emailDeliveryService == null) {
            log.warn("EmailDeliveryService chưa được cấu hình, bỏ qua tác vụ email retry");
            return;
        }
        int processed = emailDeliveryService.retryFailedEmails();
        log.info("Tác vụ nền [id={}] đã thực thi retry email: {} bản ghi được xử lý", job.getId(), processed);
    }
}
