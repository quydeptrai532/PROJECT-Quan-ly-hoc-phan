# Chuẩn hóa Trung tâm Thông báo và Chuyển phát Email (QLSV-92 / TASK-OP-05)

Tài liệu kỹ thuật đặc tả kiến trúc chuẩn hóa trung tâm thông báo trong ứng dụng (Notification Center) và kênh email thông báo bổ sung trong hệ thống Quản lý Đào tạo QLSV, tích hợp toàn diện với luồng Đăng ký học phần (TASK-ST-07 / QLSV-63), Thông báo lớp học phần của Giảng viên (TASK-LC-03 / QLSV-68) và Công bố điểm (TASK-ST-09 / QLSV-65).

---

## 1. Mô hình Sự kiện (Event Model)

Hệ thống thống nhất các loại sự kiện trong lớp hằng số tập trung `NotificationEventType`:

| Mã Sự kiện (`eventType`) | Nghiệp vụ liên quan | Mức ưu tiên | Đối tượng nhận |
| :--- | :--- | :---: | :--- |
| `COURSE_REGISTRATION_CONFIRMED` | TASK-ST-07: Đăng ký học phần thành công | NORMAL | Sinh viên đăng ký & Giảng viên phụ trách |
| `COURSE_REGISTRATION_CANCELLED` | TASK-ST-07: Hủy đăng ký học phần | NORMAL | Sinh viên hủy & Giảng viên phụ trách |
| `COURSE_SECTION_CANCELLED` | TASK-ST-07: Quản trị viên hủy lớp học phần | HIGH | Toàn bộ sinh viên đang có trạng thái `DA_DANG_KY` & Giảng viên phụ trách |
| `CLASS_ANNOUNCEMENT` | TASK-LC-03: Giảng viên gửi thông báo cho lớp | NORMAL | Toàn bộ sinh viên đang `DA_DANG_KY` trong lớp học phần |
| `GRADES_PUBLISHED` | TASK-ST-09: Bảng điểm lớp học phần được công bố | HIGH | Toàn bộ sinh viên đang `DA_DANG_KY` có điểm hợp lệ |
| `COURSE_SESSION_CANCELLED` | Giảng viên thông báo tạm nghỉ buổi học | HIGH | Sinh viên đang `DA_DANG_KY` trong lớp học phần |
| `COURSE_SECTION_SCHEDULE_CHANGED` | Cập nhật lịch học (phòng, thứ, tiết) | NORMAL | Sinh viên đang `DA_DANG_KY` & Giảng viên phụ trách |
| `COURSE_SECTION_ASSIGNMENT_CHANGED` | Thay đổi giảng viên phụ trách lớp | NORMAL | Sinh viên đang `DA_DANG_KY` & Giảng viên mới/cũ |
| `CLASS_STUDENT_TRANSFERRED` | Chuyển sinh viên sang lớp học phần khác | HIGH/NORMAL| Sinh viên được chuyển & Giảng viên 2 lớp |
| `GRADE_APPEAL_PROCESSED` | Xử lý hoàn tất hồ sơ phúc khảo điểm | HIGH | Sinh viên nộp đơn phúc khảo |
| `ATTENDANCE_THRESHOLD_ALERT` | Cảnh báo vắng quá ngưỡng quy định | HIGH | Sinh viên vi phạm & Giảng viên phụ trách |
| `ATTENDANCE_ALERT_RESOLVED` | Thu hồi / giải tỏa cảnh báo vắng | NORMAL | Sinh viên & Giảng viên phụ trách |
| `SUPPORT_REQUEST_*` | Phân công, chuyển trạng thái, yêu cầu minh chứng | HIGH/MEDIUM | Người xử lý hoặc Sinh viên gửi phiếu |

---

## 2. Xác định Người nhận (Recipient Resolution)

- **Nguyên tắc xác thực máy chủ (Server-side Resolution)**:
  - Mọi người nhận thông báo in-app và địa chỉ email nhận tin đều được truy vấn trực tiếp từ cơ sở dữ liệu hệ thống (`StudentRepository`, `LecturerRepository`, `UserRepository`).
  - Tuyệt đối **không** tiếp nhận danh sách người nhận, `userId` hoặc địa chỉ `email` do client truyền lên qua request payload hay query parameter.
  - Đối với tài khoản `SINH_VIEN`: Địa chỉ email được lấy từ `Student.getEmail()`.
  - Đối với tài khoản `GIANG_VIEN`: Địa chỉ email được lấy từ `Lecturer.getEmail()`.
  - Nếu bản ghi trong cơ sở dữ liệu không có email hoặc email rỗng, hệ thống ghi nhận trạng thái chuyển phát `SKIPPED` với lý do rõ ràng, không làm gián đoạn thông báo in-app.

---

## 3. Trung tâm Thông báo Trong ứng dụng (In-App Notification Center)

- **Phân trang & Phân quyền chủ thể**:
  - API `GET /api/notifications?unreadOnly=&page=&size=` phân trang chặt chẽ (mặc định 20, tối đa 50 bản ghi/trang).
  - Người dùng chỉ xem được các thông báo thuộc về chính mình căn cứ theo `currentUser.get().getId()` từ JWT máy chủ.
- **Thao tác đọc Idempotent (Idempotent Mark Read)**:
  - `PATCH /api/notifications/{id}/read`: Kiểm tra `findByIdAndRecipientId`. Nếu `readAt == null`, cập nhật mốc thời gian đọc `LocalDateTime.now()` và lưu; nếu đã đọc (`readAt != null`), bảo toàn mốc thời gian đọc ban đầu mà không ghi đè và trả về DTO với `read = true`.
  - `PATCH /api/notifications/read-all`: Thực thi câu lệnh UPDATE có điều kiện `where recipient_user_id = :id and read_at is null`. Thực hiện nhiều lần liên tiếp trả về kết quả an toàn mà không làm thay đổi trạng thái đã đọc trước đó.

---

## 4. Chuyển phát Email & Quản lý Trạng thái

### 4.1. Cấu hình tập trung (`app.notification.email`)

Kênh email là kênh bổ sung và được kiểm soát độc lập trong `application.yml`:

```yaml
app:
  notification:
    email:
      enabled: ${NOTIFICATION_EMAIL_ENABLED:false}
      from-address: ${NOTIFICATION_EMAIL_FROM:no-reply@university.edu.vn}
      from-name: ${NOTIFICATION_EMAIL_FROM_NAME:Hệ thống Quản lý Đào tạo}
      max-retries: ${NOTIFICATION_EMAIL_MAX_RETRIES:3}
      retry-delay-seconds: ${NOTIFICATION_EMAIL_RETRY_DELAY_SECONDS:60}
      provider: ${NOTIFICATION_EMAIL_PROVIDER:smtp}
      host: ${SMTP_HOST:}
      port: ${SMTP_PORT:587}
      username: ${SMTP_USERNAME:}
      password: ${SMTP_PASSWORD:}
      auth: ${SMTP_AUTH:true}
      starttls: ${SMTP_STARTTLS:true}
```

- **Khi tắt email (`enabled = false`)**: Hệ thống ghi nhận trạng thái `SKIPPED`, không gọi provider, thông báo in-app vẫn hoạt động hoàn toàn độc lập và thông suốt.
- **Xử lý an toàn khi chưa cấu hình Provider/Credentials**:
  - Giao diện `EmailProvider.isConfigured()` kiểm tra cấu hình SMTP host và thông tin xác thực.
  - Khi chưa cấu hình, hệ thống ghi nhận trạng thái `FAILED` kèm thông điệp rõ ràng `Email provider/credentials chưa được cấu hình trên máy chủ`.
  - **Tuyệt đối không giả lập thành gửi thành công (`SENT`)** khi chưa gửi thực tế qua provider được cấu hình hợp lệ.

### 4.2. Bảng dữ liệu theo dõi chuyển phát (`email_delivery`)

- **Khóa chính**: `id` (BIGINT IDENTITY).
- **Người nhận**: `recipient_user_id` (FK `users`), `recipient_email` (VARCHAR(150)).
- **Khóa chống trùng**: `idempotency_key` (VARCHAR(180)).
- **Unique Constraint**: `uk_email_delivery_recipient_event` (`recipient_user_id`, `idempotency_key`).
- **Trạng thái**: `status` (`EmailDeliveryStatus`: `PENDING`, `SENT`, `FAILED`, `EXHAUSTED`, `SKIPPED`).
- **Thông tin retry**: `retry_count`, `max_retries`, `next_retry_at`.
- **Mốc thời gian**: `created_at`, `updated_at`, `sent_at`.
- **Thông báo lỗi**: `error_message` (VARCHAR(500), đã qua khử nhạy cảm).

### 4.3. Cơ chế Retry có giới hạn

- Khi gửi qua provider gặp lỗi mạng hoặc ngoại lệ:
  1. Tăng `retry_count = retry_count + 1`.
  2. Khử nhạy cảm thông điệp lỗi và lưu vào `error_message`.
  3. Nếu `retry_count < max_retries`: Đặt trạng thái `FAILED`, lên lịch `next_retry_at = now + retryDelaySeconds`.
  4. Nếu `retry_count >= max_retries`: Chuyển trạng thái sang `EXHAUSTED`, không thử lại tự động nữa.
- Job định kỳ `NotificationReminderJob.retryFailedEmails()` chạy mỗi 5 phút quét các bản ghi `FAILED` có `next_retry_at <= now` để thực hiện gửi bù an toàn.

---

## 5. Idempotency & Cách ly Giao dịch

- **Chống gửi trùng lặp**:
  - Khóa `idempotencyKey` tất định theo từng loại sự kiện:
    - Đăng ký học phần (TASK-ST-07): `registration:{dangKyId}:{eventType}:{thoiGianDangKy}`
    - Hủy lớp học phần (TASK-ST-07): `registration:{dangKyId}:COURSE_SECTION_CANCELLED:{thoiGianDangKy}`
    - Thông báo lớp học phần (TASK-LC-03): `class-announcement:{thongBaoLopId}`
    - Công bố bảng điểm (TASK-ST-09): `grade-published:{lopHocPhanId}:v{phienBanCongBo}`
  - Kiểm tra `existsByRecipientIdAndIdempotencyKey` trước khi khởi tạo và chặn trùng lặp qua Unique Constraint `uk_email_delivery_recipient_event` và `uk_thong_bao_recipient_event`.
- **Cách ly giao dịch nghiệp vụ**:
  - Quá trình chuyển phát email và lưu thông báo được đăng ký qua `TransactionSynchronization.afterCommit()`.
  - Thực thi trong transaction riêng biệt `PROPAGATION_REQUIRES_NEW`.
  - Lỗi xảy ra khi lưu hoặc gửi email chỉ được ghi nhận vào nhật ký/bảng chuyển phát, **tuyệt đối không rollback** giao dịch nghiệp vụ (đăng ký, nhập điểm, hủy lớp) đã commit thành công.

---

## 6. Bảo mật Nội dung & Kiểm toán (Security & Privacy)

1. **Bảo vệ dữ liệu Điểm số Cá nhân (TASK-ST-09)**:
   - Email công bố điểm (`GRADES_PUBLISHED`) **tuyệt đối không chứa** điểm cá nhân (`diemQuaTrinh`, `diemCuoiKy`, `diemTongKet`, GPA hay xếp loại đạt/trượt) trong tiêu đề hoặc thân email.
   - Nội dung email chỉ xác nhận việc bảng điểm lớp đã được phê duyệt và công bố chính thức, đồng thời hướng dẫn sinh viên đăng nhập hệ thống và truy cập `/diem` để tra cứu điểm cá nhân an toàn.
2. **Chống tấn công XSS / Template Injection**:
   - Mọi biến nội dung động (mã lớp, tên môn học, học kỳ, tiêu đề thông báo, nội dung thông báo, lý do hủy lớp) đều được escape an toàn bằng `HtmlUtils.htmlEscape` trước khi đưa vào template HTML.
3. **Bảo vệ Thông tin Nhạy cảm trong Nhật ký (Log Sanitization)**:
   - Địa chỉ email trong log được che giấu định danh (ví dụ `a***@student.edu.vn`).
   - Lọc bỏ mật khẩu, JWT token, Bearer token hoặc secret khỏi nội dung `error_message` và application log.

---

## 7. Kiểm thử & Nghiệm thu

- Đã triển khai bộ test toàn diện:
  - `EmailDeliveryServiceTest`: 9 kịch bản kiểm thử (email tắt, provider chưa cấu hình, gửi thành công, chống trùng key, retry chạm ngưỡng exhausted, retry batch, bảo mật điểm số, escape nội dung động, cách ly ngoại lệ).
  - `NotificationServiceTest`: 8 kịch bản kiểm thử (idempotent markRead, dispatch đồng thời in-app và email, đối soát bù tự động, cách ly lỗi DB).
  - Toàn bộ 24 integration test liên quan (TASK-ST-07, TASK-LC-03, TASK-ST-09) đều vượt qua thành công 100%.
