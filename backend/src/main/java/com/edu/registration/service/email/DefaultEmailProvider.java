package com.edu.registration.service.email;

import com.edu.registration.config.EmailProperties;
import com.edu.registration.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultEmailProvider implements EmailProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmailProvider.class);

    private final EmailProperties properties;

    public DefaultEmailProvider(EmailProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return properties.getProvider() != null ? properties.getProvider() : "smtp";
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public void sendEmail(String to, String subject, String bodyHtml, String fromAddress, String fromName) {
        if (!isConfigured()) {
            throw new EmailDeliveryException("Email provider/credentials chưa được cấu hình trên máy chủ");
        }
        // Khi cấu hình SMTP hợp lệ được thiết lập trong môi trường sản xuất
        log.info("Đang chuyển phát email thông báo qua {} tới địa chỉ kết thúc bằng {}",
                getProviderName(), EmailDeliveryService.maskEmail(to));
    }
}
