package com.edu.registration.service.email;

public interface EmailProvider {

    /**
     * Tên định danh của provider (smtp, mock, log, none).
     */
    String getProviderName();

    /**
     * Kiểm tra xem cấu hình của provider đã hợp lệ và sẵn sàng gửi email hay chưa.
     */
    boolean isConfigured();

    /**
     * Gửi email tới người nhận.
     * Ném EmailDeliveryException nếu không gửi được hoặc provider chưa cấu hình.
     * Tuyệt đối không giả lập thành gửi thành công khi chưa cấu hình provider/credentials.
     */
    void sendEmail(String to, String subject, String bodyHtml, String fromAddress, String fromName);
}
