package com.edu.registration.service.job.handler;

import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.User;
import com.edu.registration.repository.UserRepository;
import com.edu.registration.service.email.EmailDeliveryService;
import com.edu.registration.service.job.BackgroundJobHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler chuyển phát email thông báo bất đồng bộ qua hàng đợi tác vụ nền (QLSV-93 / TASK-OP-06, tích hợp TASK-OP-05).
 */
@Component
public class EmailDispatchJobHandler implements BackgroundJobHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchJobHandler.class);
    public static final String JOB_TYPE = "EMAIL_DISPATCH";

    private final EmailDeliveryService emailDeliveryService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public EmailDispatchJobHandler(EmailDeliveryService emailDeliveryService,
                                   UserRepository userRepository,
                                   ObjectMapper objectMapper) {
        this.emailDeliveryService = emailDeliveryService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getJobType() {
        return JOB_TYPE;
    }

    @Override
    public void execute(BackgroundJob job) throws Exception {
        if (emailDeliveryService == null || job.getPayload() == null) {
            return;
        }

        JsonNode root = objectMapper.readTree(job.getPayload());
        long recipientUserId = root.path("recipientUserId").asLong();
        String eventType = root.path("eventType").asText();
        String idempotencyKey = root.path("idempotencyKey").asText(job.getIdempotencyKey());
        String subject = root.path("subject").asText();
        String bodyHtml = root.path("bodyHtml").asText();

        User recipient = userRepository.findById(recipientUserId).orElse(null);
        if (recipient == null) {
            log.warn("Không tìm thấy người dùng recipientUserId={} để gửi email trong tác vụ [id={}]",
                    recipientUserId, job.getId());
            return;
        }

        emailDeliveryService.deliverEmailSafely(recipient, eventType, idempotencyKey, subject, bodyHtml);
        log.info("Tác vụ nền [id={}] đã chuyển phát email [{}] cho recipientId={}",
                job.getId(), eventType, recipientUserId);
    }
}
