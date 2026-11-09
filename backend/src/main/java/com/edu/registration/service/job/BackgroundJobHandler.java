package com.edu.registration.service.job;

import com.edu.registration.entity.BackgroundJob;

/**
 * Interface cho các bộ xử lý nghiệp vụ tác vụ nền cụ thể (QLSV-93 / TASK-OP-06).
 */
public interface BackgroundJobHandler {

    /**
     * Tên định danh của loại tác vụ mà handler này hỗ trợ (ví dụ: NOTIFICATION_EMAIL_DISPATCH).
     */
    String getJobType();

    /**
     * Thực thi tác vụ. Đảm bảo tính idempotent khi xử lý lại cùng một job.
     */
    void execute(BackgroundJob job) throws Exception;
}
