# TÀI LIỆU CẤU HÌNH VÀ HƯỚNG DẪN TRUY CẬP SWAGGER / OPENAPI (QLSV-101)

> **Mã công việc:** QLSV-101 — TASK-OP-07A: Swagger/OpenAPI cho dự án QLSV  
> **Phiên bản tài liệu:** 1.0.0  
> **Công nghệ áp dụng:** SpringDoc OpenAPI Starter WebMVC UI 2.7.0 (OpenAPI 3.0 / Swagger UI)  
> **Tương thích:** Spring Boot 3.4.1, Spring Security 6.x, Java 21+

---

## 1. Tổng quan kiến trúc & Mục tiêu

Tài liệu hóa toàn bộ hệ thống giao diện lập trình ứng dụng (RESTful API) của Hệ thống Quản lý Đào tạo & Đăng ký Học phần (QLSV) theo tiêu chuẩn OpenAPI 3.0, đáp ứng:
- **Tài liệu hóa chính xác**: Đồng bộ với mã nguồn và DTO thực tế của hệ thống; mô tả đầy đủ tham số, request body, response schema, mã lỗi và ràng buộc validation (`jakarta.validation`).
- **An toàn bảo mật**: Phân quyền chi tiết theo JWT (`bearerAuth`). Việc bật Swagger UI trên môi trường phát triển tuyệt đối không làm lộ hay mở quyền các endpoint bảo vệ.
- **Kiểm soát môi trường (Environment Profiles)**: Bật tài liệu trên môi trường phát triển (`dev`, `h2`, `local`); tắt hoàn toàn trên môi trường sản xuất (`prod`).
- **Phản hồi lỗi an toàn (Safe Fallback)**: Khi tính năng OpenAPI/Swagger bị tắt trên production, mọi truy vấn tới `/swagger-ui/**` và `/v3/api-docs/**` đều nhận phản hồi HTTP 404 an toàn dạng JSON, không lộ stack trace hay cấu trúc nội bộ.
- **Không lộ dữ liệu nhạy cảm**: Toàn bộ dữ liệu mẫu trong tài liệu tuân thủ RFC 2606 (tên miền `@example.edu.vn`, số điện thoại mẫu `0900000001`), không chứa token thật hay secret.

---

## 2. Thông tin truy cập Swagger UI & OpenAPI Docs trên môi trường Dev

### 2.1. Địa chỉ truy cập

Khi khởi động ứng dụng với profile `dev` hoặc `h2`:

| Loại tài nguyên | URL truy cập | Mô tả |
| :--- | :--- | :--- |
| **Swagger UI HTML** | `http://localhost:8080/swagger-ui/index.html`<br>*(hoặc `http://localhost:8080/swagger-ui.html`)* | Giao diện đồ họa tương tác thử nghiệm trực tiếp API |
| **OpenAPI Spec (JSON)** | `http://localhost:8080/v3/api-docs` | Đặc tả OpenAPI 3.0 đầy đủ dưới dạng JSON |
| **Nhóm API Tất cả** | `http://localhost:8080/v3/api-docs/1-tat-ca` | Đặc tả nhóm toàn bộ API công bố |
| **Nhóm API Xác thực** | `http://localhost:8080/v3/api-docs/2-xac-thuc-dung-chung` | Đặc tả nhóm API Xác thực, Danh mục & Vận hành |
| **Nhóm API Sinh viên** | `http://localhost:8080/v3/api-docs/3-sinh-vien` | Đặc tả nhóm API dành cho Sinh viên |
| **Nhóm API Giảng viên** | `http://localhost:8080/v3/api-docs/4-giang-vien` | Đặc tả nhóm API dành cho Giảng viên |
| **Nhóm API Hỗ trợ & Điểm danh** | `http://localhost:8080/v3/api-docs/5-ho-tro-diem-danh` | Đặc tả nhóm API Điểm danh & Hỗ trợ sinh viên |
| **Nhóm API Quản trị** | `http://localhost:8080/v3/api-docs/6-quan-tri` | Đặc tả nhóm API dành cho Quản trị viên |

---

## 3. Hướng dẫn xác thực JWT trên Swagger UI

Hệ thống bảo vệ các endpoint theo chuẩn **JWT Bearer Token**. Để thử nghiệm các endpoint yêu cầu quyền trên Swagger UI:

### Bước 1: Lấy Token xác thực
Gửi yêu cầu đăng nhập tại nhóm **Xác thực & Người dùng** -> `POST /api/auth/login`:
- **Quản trị viên**: `username: "admin"`, `password: "admin123"` (quyền `QUAN_TRI`)
- **Giảng viên**: `username: "gv001"`, `password: "password123"` (quyền `GIANG_VIEN`)
- **Sinh viên**: `username: "sv001"`, `password: "password123"` (quyền `SINH_VIEN`)

Ví dụ response trả về:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "username": "admin",
  "roles": ["QUAN_TRI"]
}
```

### Bước 2: Thiết lập Token vào Swagger UI
1. Nhấn nút **Authorize** (biểu tượng ổ khóa màu xanh lá cây) ở góc trên bên phải màn hình Swagger UI.
2. Tại popup **Available authorizations**, tìm mục `bearerAuth (http, Bearer)`.
3. Dán chuỗi token vừa nhận được vào ô **Value** (Swagger UI tự động đính kèm tiền tố `Bearer `).
4. Nhấn **Authorize** rồi nhấn **Close**.

### Bước 3: Thực thi API
- Giờ đây mọi yêu cầu gửi từ Swagger UI sẽ tự động kèm header `Authorization: Bearer <token>`.
- Nếu gửi yêu cầu mà không có token hoặc token không hợp lệ: Nhận mã `401 Unauthorized`.
- Nếu token không đủ quyền (ví dụ sinh viên gọi API quản trị `/api/admin/**`): Nhận mã `403 Forbidden`.

---

## 4. Phân nhóm API (GroupedOpenApi)

Hệ thống cung cấp danh mục chọn nhóm (dropdown **Select a definition**) tại Swagger UI với 6 nhóm riêng biệt:

1. **`1-tat-ca` (Toàn bộ API hệ thống)**:
   - Bao gồm tất cả các endpoint công bố của hệ thống: Xác thực, Đăng ký học phần, Giảng dạy, Quản trị, Điểm danh, Hỗ trợ và Xác thực bảng điểm.
2. **`2-xac-thuc-dung-chung` (Xác thực, Thông tin chung & Vận hành)**:
   - `/api/auth/**`: Đăng nhập, thông tin tài khoản hiện tại, đổi mật khẩu, cập nhật hồ sơ.
   - `/api/common/**`: Danh mục học kỳ, phòng học dùng chung.
   - `/api/notifications/**`: Thông báo hệ thống cá nhân hóa.
   - `/api/health`, `/api/admin/metrics`, `/api/admin/system/health`: Vận hành & giám sát hệ thống.
3. **`3-sinh-vien` (Dịch vụ Sinh viên)**:
   - `/api/dang-ky/**`: Đăng ký, hủy môn, lịch sử đăng ký, tra cứu lớp khả dụng.
   - `/api/sinh-vien/**`: Bảng điểm cá nhân, xuất PDF bảng điểm có mã xác thực QR, đồ thị tiến độ học tập, lịch học tuần.
   - `/api/sinh-vien/phuc-khao/**`: Gửi yêu cầu và tra cứu tiến độ phúc khảo điểm.
4. **`4-giang-vien` (Dịch vụ Giảng viên)**:
   - `/api/giang-vien/**`: Lịch giảng dạy cá nhân, danh sách lớp học phần phụ trách, thông báo lớp, quản trị điểm số, nhập điểm qua Excel, quy trình phê duyệt điểm, phân tích phổ điểm.
5. **`5-ho-tro-diem-danh` (Điểm danh & Dịch vụ Hỗ trợ)**:
   - `/api/diem-danh/**`: Điểm danh sinh viên theo buổi học, thống kê chuyên cần, cảnh báo vắng mặt vượt ngưỡng.
   - `/api/yeu-cau-ho-tro/**`: Tiếp nhận phiếu hỗ trợ/khiếu nại của sinh viên, đính kèm minh chứng, trao đổi phản hồi, tra cứu tiến độ xử lý.
6. **`6-quan-tri` (Quản trị Hệ thống)**:
   - `/api/admin/**`: Quản lý danh mục đào tạo (Khoa, Ngành, Môn học, CTĐT), Phòng học & xung đột, Lớp học phần & chuyển lớp, Đợt đăng ký, Người dùng & Phân quyền, Thống kê tuyển sinh & học tập, Tra cứu tổng hợp, Audit Log, Cảnh báo sớm & Quy tắc tự động, Quản lý tác vụ ngầm & Retry.

---

## 5. Cấu hình kiểm soát môi trường (Profile Matrix)

| Thuộc tính | Môi trường Dev (`dev`, `h2`) | Môi trường Production (`prod`) | Ghi chú |
| :--- | :---: | :---: | :--- |
| `springdoc.api-docs.enabled` | `true` | `false` | Bật/tắt endpoint JSON đặc tả OpenAPI (`/v3/api-docs/**`) |
| `springdoc.swagger-ui.enabled` | `true` | `false` | Bật/tắt giao diện Swagger UI (`/swagger-ui/**`) |
| Phản hồi khi truy cập tài liệu | `200 OK` (Hiển thị docs) | `404 Not Found` (Safe Fallback JSON) | Controller dự phòng `OpenApiDisabledFallbackController` |
| H2 Console (`/h2-console`) | Cho phép nội bộ dev | Tắt hoàn toàn | **Không bao giờ** được đưa vào OpenAPI docs |

### Cấu hình biến môi trường tùy chỉnh:
Người quản trị có thể ghi đè trạng thái bật/tắt của OpenAPI thông qua biến môi trường OS hoặc file `.env`:
```properties
SPRINGDOC_API_DOCS_ENABLED=false
SPRINGDOC_SWAGGER_UI_ENABLED=false
```

### Phản hồi an toàn trên môi trường Production (Safe Fallback):
Khi `springdoc.api-docs.enabled=false`, bean `OpenApiDisabledFallbackController` sẽ xử lý các đường dẫn `/v3/api-docs/**` và `/swagger-ui/**`:
- **HTTP Status**: `404 Not Found`
- **Body Content**:
```json
{
  "timestamp": "2026-11-10T10:15:00.000+07:00",
  "status": 404,
  "error": "Not Found",
  "message": "API Documentation is disabled in this environment.",
  "path": "/swagger-ui/index.html"
}
```

---

## 6. Tiêu chuẩn DTO & Mã lỗi

### Cấu trúc mã lỗi chuẩn:
Mọi endpoint đều được định nghĩa rõ ràng các mã lỗi chuẩn:
- **`400 Bad Request`**: Dữ liệu đầu vào vi phạm validation (`ValidationError` chứa danh sách chi tiết lỗi trường dữ liệu).
- **`401 Unauthorized`**: Thiếu hoặc sai JWT Bearer token (`UnauthorizedError`).
- **`403 Forbidden`**: Không có quyền truy cập endpoint nhạy cảm (`ForbiddenError`).
- **`404 Not Found`**: Tài nguyên không tồn tại (`NotFoundError`).

### Dữ liệu mẫu (Sample Data):
- **Domain**: `@example.edu.vn` (thay vì các email cá nhân thực).
- **Phone**: `0900000001` (dãy số kiểm thử an toàn).
- **Mã sinh viên**: `SV001`, `SV002`.
- **Mã giảng viên**: `GV001`.
- **Mã quản trị**: `ADMIN001`.
