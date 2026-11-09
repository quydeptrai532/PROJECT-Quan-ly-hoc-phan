# BÁO CÁO KỸ THUẬT: HOÀN THIỆN VẬN HÀNH VÀ API DOCS (QLSV-94 / TASK-OP-07)

> **Mã công việc:** QLSV-94 — TASK-OP-07  
> **Nhánh thực hiện:** `codex/qlsv-94-operations-hardening`  
> **Nhánh đích:** `dev`  
> **Phân chia công việc:** 5 Work Packages chi tiết (theo hướng dẫn quản lý tác vụ)

---

## 1. Khảo sát Hiện trạng Hạ tầng, Công nghệ & Môi trường Triển khai

Trước khi tiến hành cài đặt, hệ thống đã được khảo sát toàn diện:
- **Ngôn ngữ & Nền tảng:** Java 21, Spring Boot 3.4.1, Spring Security (JWT không trạng thái / stateless), Spring Data JPA / Hibernate, Jackson.
- **Cơ sở dữ liệu:** MySQL 8.4 Community Server (chạy trong Docker container `qldt-mysql`, cơ sở dữ liệu `quan_ly_dao_tao`, cổng ánh xạ 3307 trên host); H2 Database in-memory phục vụ kiểm thử và kiểm thử hồi quy (CI).
- **Lưu trữ tệp:** Docker volume `support_uploads` gắn tại `/app/data/support-attachments`.
- **Hạ tầng bổ sung:** Hiện tại môi trường triển khai (`docker-compose.yml`) **chưa cấu hình Redis, Memcached, Kafka hay Prometheus/Grafana**.
- **Nguyên tắc tuân thủ:** Tuân thủ triệt để chỉ thị *"Không thêm dịch vụ ngoài môi trường triển khai nếu chưa có cấu hình/nhu cầu rõ. Nếu thiếu hạ tầng, chỉ triển khai phần có thể cấu hình an toàn và ghi rõ giới hạn."* Do đó, mọi giải pháp rate limiting, cache, giám sát metrics và backup đều được thiết kế tối ưu trên nền tảng sẵn có (Spring Boot + MySQL + Local/Volume Storage), đồng thời có sẵn giao diện mở rộng để tương thích ngay khi dự án trang bị Redis hoặc hệ thống giám sát tập trung.

---

## 2. Chi tiết 5 Work Packages trong QLSV-94

### Work Package A: OpenAPI & Swagger Documentation
- **Thư viện tích hợp:** `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0` (tương thích chính thức với Spring Boot 3.4.x).
- **Cấu hình (`OpenApiConfig.java`):**
  - Khai báo Security Scheme kiểu HTTP Bearer JWT (`bearerAuth`), định dạng token `JWT`. Nút "Authorize" trên Swagger UI cho phép dán token xác thực.
  - Phân nhóm API (`GroupedOpenApi`): Chỉ công bố các điểm cuối nghiệp vụ được phép `/api/**`.
  - Loại trừ các đường dẫn nhạy cảm và nội bộ (`/h2-console/**`).
  - Thiết lập thông tin liên hệ, bản quyền, mô tả phân quyền cho 3 nhóm vai trò (`QUAN_TRI`, `GIANG_VIEN`, `SINH_VIEN`).
  - Điểm truy cập tài liệu: `/swagger-ui.html` và `/v3/api-docs`.
- **Bảo mật:** Cho phép truy cập tài liệu API công khai qua `SecurityConfig` phục vụ tích hợp phía frontend và đối tác; các API nghiệp vụ vẫn được bảo vệ nghiêm ngặt theo JWT.

### Work Package B: Giới hạn tần suất yêu cầu (Rate Limiting)
- **Mục tiêu:** Bảo vệ điểm cuối xác thực đăng nhập (`/api/auth/login`) khỏi tấn công brute-force mật khẩu và bảo vệ các API nghiệp vụ (`/api/**`) khỏi tấn công từ chối dịch vụ (DoS).
- **Kiến trúc & Cơ chế hoạt động (`RateLimitFilter`, `RateLimiterService`):**
  - **Môi trường đơn instance / dev:** Cung cấp `InMemoryRateLimiter` sử dụng cửa sổ trượt (sliding window) với `ConcurrentHashMap` thread-safe và cơ chế tự động dọn dẹp bộ nhớ đệm quá hạn.
  - **Môi trường đa instance (Clustering):** Cung cấp `DatabaseDistributedRateLimiter` lưu trữ trạng thái tại bảng `rate_limit_bucket` trong MySQL với giao dịch độc lập `REQUIRES_NEW`, bảo đảm đồng bộ hạn ngạch chính xác giữa nhiều worker/container mà không cần dịch vụ ngoài như Redis.
  - **Khóa định danh (Key partition):**
    - Đăng nhập: `login:{client_ip}` (mặc định tối đa 10 yêu cầu / 60 giây).
    - API nghiệp vụ: `api:user:{username}` (nếu đã đăng nhập) hoặc `api:ip:{client_ip}` (mặc định tối đa 120 yêu cầu / 60 giây).
    - Miễn trừ rate limiting cho `/api/health`, `/swagger-ui/**`, `/v3/api-docs/**`.
  - **Chuẩn phản hồi lỗi khi vượt ngưỡng:**
    - HTTP Status: `429 Too Many Requests`.
    - HTTP Headers: `Retry-After: {giây}`, `X-RateLimit-Limit: {ngưỡng}`, `X-RateLimit-Remaining: 0`.
    - Body JSON chuẩn:
      ```json
      {
        "message": "Quá nhiều yêu cầu gửi tới hệ thống. Vui lòng thử lại sau 60 giây.",
        "timestamp": "2026-11-10T00:30:00Z",
        "retryAfterSeconds": 60
      }
      ```

### Work Package C: Kiểm soát Bộ nhớ đệm & Vô hiệu hóa (Cache & Invalidation)
- **Thư viện tích hợp:** `spring-boot-starter-cache` kết hợp `com.github.ben-manes.caffeine:caffeine`.
- **Nguyên tắc phân vùng & bảo mật dữ liệu cá nhân:**
  - **Danh mục dùng chung (Public/Catalog):**
    - `catalog_subjects`: Bộ đệm danh mục môn học.
    - `catalog_semesters`: Bộ đệm danh mục học kỳ.
    - `catalog_classrooms`: Bộ đệm danh mục phòng học đang hoạt động.
    - Hủy cache tự động khi quản trị viên thêm mới, cập nhật hoặc lưu trữ danh mục.
  - **Dữ liệu cá nhân (Personal Data):**
    - Tuyệt đối **KHÔNG** sử dụng cache key dùng chung cho sinh viên hoặc giảng viên.
    - `student_academic_progress`: Khóa bộ đệm phân vùng tường minh theo mã định danh sinh viên: `student_{maSinhVien}` hoặc `user_{username}`.
    - Tự động hủy cache cá nhân của sinh viên ngay khi có thay đổi liên quan: khi sinh viên đăng ký/hủy học phần (`RegistrationService`) hoặc khi giảng viên/quản trị viên nhập, duyệt và công bố bảng điểm (`GradeService`).
- **Quản trị & Giám sát bộ đệm:**
  - Cung cấp API quản trị `GET /api/admin/cache` và `POST /api/admin/cache/clear?name={cacheName}`.
  - Yêu cầu quyền `ROLE_QUAN_TRI` và tự động ghi nhật ký kiểm toán (`AuditLogService`).

### Work Package D: Quy trình & Công cụ Sao lưu - Khôi phục (Backup & Restore)
- **Tài liệu chuẩn hóa:** `docs/BACKUP_AND_RESTORE.md` trình bày chi tiết quy trình sao lưu, lưu trữ an toàn, chính sách lưu giữ (retention) và khôi phục vào sandbox.
- **Bảo mật Secret:** Không ghi mật khẩu vào script hoặc repository; đọc mật khẩu an toàn từ biến môi trường (`MYSQL_PWD`, `MYSQL_ROOT_PASSWORD`) hoặc tệp cấu hình được phân quyền chặt chẽ.
- **Bộ công cụ thực thi (`tools/ops/`):**
  - `tools/ops/backup.sh` & `backup.ps1`: Tự động tạo bản sao lưu cơ sở dữ liệu `quan_ly_dao_tao` bằng `mysqldump` với các cờ an toàn giao dịch (`--single-transaction --quick --routines --triggers --set-gtid-purged=OFF`), nén gzip và sinh mã băm SHA-256 (`.sha256`). Sao lưu đồng thời tệp đính kèm.
  - `tools/ops/restore-verify.sh` & `restore-verify.ps1`: Tự động kiểm tra mã băm SHA-256, nạp bản sao lưu vào cơ sở dữ liệu sandbox (`quan_ly_dao_tao_restore_test`), kiểm tra số lượng bảng và số lượng bản ghi của các bảng trọng yếu (`nguoi_dung`, `mon_hoc`, `hoc_ky`, `lop_hoc_phan`, `dang_ky`), xuất báo cáo kiểm định trước khi khôi phục vào môi trường thật.

### Work Package E: Giám sát Sức khỏe, Chỉ số Vận hành & Làm sạch Nhật ký
- **Endpoint Sức khỏe (`GET /api/health`):**
  - Công khai, phản hồi nhanh (phục vụ bộ cân bằng tải Load Balancer và kiểm thử CI E2E).
  - Kiểm tra tính sẵn sàng của kết nối cơ sở dữ liệu và dung lượng lưu trữ tệp tin.
  - Trả về trạng thái `UP` (HTTP 200) hoặc `DOWN` (HTTP 503).
  - Tuyệt đối không để lộ thông tin nhạy cảm: không lộ chuỗi kết nối JDBC, máy chủ lưu trữ, mật khẩu, biến môi trường.
- **Endpoint Chỉ số Vận hành (`GET /api/admin/metrics`):**
  - Bảo vệ nghiêm ngặt: Chỉ người dùng có vai trò `ROLE_QUAN_TRI` mới có quyền truy cập.
  - Chỉ số tài nguyên máy chủ: Thời gian hoạt động (uptime), bộ nhớ heap (đã dùng, tối đa, tỷ lệ %), số nhân CPU, số luồng active.
  - Thống kê nghiệp vụ thực tế: Tổng số người dùng, sinh viên, giảng viên, môn học, lớp học phần, số lượt đăng ký, số phiếu hỗ trợ đang chờ, số tác vụ nền đang xử lý.
  - Trạng thái các vùng bộ đệm đang kích hoạt.
- **Làm sạch Thông tin Nhạy cảm & Điểm cá nhân (`SecretSanitizer`):**
  - Nâng cấp `SecretSanitizer` để không chỉ khử mật khẩu, secret, JWT, Bearer token, JDBC url mà còn khử sạch toàn bộ các giá trị điểm cá nhân sinh viên (`diemQuaTrinh`, `diemCuoiKy`, `diemTongKet`, `diemChu`, `gpa`) thành `***GRADE_REDACTED***`.
  - Tích hợp vào `GlobalExceptionHandler` để đảm bảo khi xảy ra lỗi ngoài ý muốn, thông điệp lỗi và nhật ký log không bao giờ chứa thông tin học tập nhạy cảm của sinh viên.

---

## 3. Các Giới hạn Kỹ thuật & Định hướng Mở rộng

| Thành phần | Hiện trạng Đã Cấu hình | Giới hạn & Khuyến nghị khi Mở rộng Hạ tầng |
|:---|:---|:---|
| **Rate Limiting** | Hoạt động In-Memory (mặc định) và Database Distributed (khi chạy đa instance). | Khi hệ thống đạt quy mô hàng triệu yêu cầu/giờ, có thể bổ sung Redis để giảm tải I/O cho cơ sở dữ liệu MySQL. |
| **Cache** | Caffeine in-memory với phân vùng khóa theo người dùng và tự động invalidation. | Khi chuyển sang microservices hoặc đa container độc lập chia sẻ cache, có thể chuyển tầng Caffeine sang Redis Cache Manager mà không cần sửa code nghiệp vụ. |
| **Metrics** | API `/api/admin/metrics` bảo mật vai trò Quản trị viên, thu thập thời gian thực. | Có thể tích hợp thêm Micrometer Prometheus và cấu hình Prometheus/Grafana scrape endpoint khi hạ tầng DevOps thiết lập bộ giám sát tập trung. |
| **Backup** | Script tự động sao lưu, nén gzip, băm SHA256 và kịch bản xác minh sandbox. | Cần thiết lập cron job hoặc AWS S3/GCS sync định kỳ đẩy bản sao lưu ra hạ tầng lưu trữ offsite an toàn. |
