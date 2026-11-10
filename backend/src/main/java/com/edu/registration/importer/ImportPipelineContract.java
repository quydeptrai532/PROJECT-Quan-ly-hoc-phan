package com.edu.registration.importer;

import java.util.List;

/**
 * Hợp đồng chuẩn hóa (Standard Contract) cho luồng xử lý import dữ liệu (QLSV-90 / TASK-OP-03).
 *
 * Chu trình chuẩn hóa 5 bước:
 * 1. Tải template tệp mẫu (UTF-8 CSV có BOM hoặc Excel OOXML .xlsx).
 * 2. Phân tích & Xem trước (Preview): kiểm tra bảo mật tệp, cấu trúc cột, khóa trùng, toàn vẹn tham chiếu.
 * 3. Cung cấp mã phiên ngắn hạn (previewToken) kèm SHA-256 để chống can thiệp.
 * 4. Xác nhận ghi dữ liệu (Confirm) theo Transaction Policy và Idempotency Policy nhất quán.
 * 5. Xuất báo cáo lỗi (Export Errors) cho các dòng bị từ chối để người dùng khắc phục.
 *
 * @param <P> Kiểu phản hồi xem trước (Preview Response)
 * @param <C> Kiểu yêu cầu xác nhận (Confirm Request)
 * @param <R> Kiểu kết quả sau khi thực hiện (Result Response)
 */
public interface ImportPipelineContract<P, C, R> {

    /**
     * Định danh loại import và metadata tương ứng.
     */
    ImportType getImportType();

    /**
     * Tải tệp mẫu template chuẩn cho người dùng.
     */
    byte[] exportTemplate(Object context);

    /**
     * Kiểm tra và tạo phiên xem trước (Preview). Dữ liệu chỉ đọc, không ghi cơ sở dữ liệu.
     */
    P preview(Object context, Object file);

    /**
     * Xác nhận ghi dữ liệu từ phiên xem trước hợp lệ theo chính sách giao dịch.
     */
    R confirm(Object context, C confirmRequest);

    /**
     * Xuất tệp báo cáo danh sách dòng bị lỗi để người dùng tải về.
     */
    byte[] exportErrorReport(Object context, List<?> errorRows);
}
