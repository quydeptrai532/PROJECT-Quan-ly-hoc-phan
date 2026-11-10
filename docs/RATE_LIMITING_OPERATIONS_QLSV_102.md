# BÁO CÁO KỸ THUẬT VÀ HƯỚNG DẪN VẬN HÀNH: RATE LIMITING (QLSV-102 — TASK-OP-07B)

> **Mã công việc:** QLSV-102 — TASK-OP-07B  
> **Tên tác vụ:** Rate limiting trong dự án QLSV  
> **Nhánh thực hiện:** `codex/qlsv-102-rate-limiting`  
> **Nhánh đích:** `dev`  

---

## 1. Khảo sát Hiện trạng & Mục tiêu Thiết kế

### 1.1. Hiện trạng Hệ thống & Điểm yếu đã nhận diện
Trước khi triển khai QLSV-102, hệ thống quản lý đào tạo có các đặc điểm:
1. **Điểm cuối đăng nhập (`/api/auth/login`):** Trước đây áp dụng rate limit thuần túy theo địa chỉ IP máy khách với ngưỡng cố định.
   - *Rủi ro DoS người dùng hợp lệ:* Khi sinh viên kết nối qua mạng dùng chung (NAT của trường học, ký túc xá, phòng máy tính), nếu 1 sinh viên gõ sai mật khẩu nhiều lần hoặc khi nhiều sinh viên cùng đăng nhập đầu giờ học, toàn bộ địa chỉ IP chung bị chặn, khiến tất cả người dùng hợp lệ khác bị từ chối dịch vụ (self-inflicted DoS).
   - *Rủi ro Brute-force phân tán:* Kẻ tấn công sử dụng botnet/proxy phân tán có thể đổi IP liên tục để vét cạn mật khẩu của một tài khoản mục tiêu mà không bị chặn IP.
2. **Các điểm cuối nhạy cảm / dễ bị lạm dụng:**
   - Tra cứu văn bằng chứng chỉ công khai (`/api/public/bang-diem/xac-thuc/**`): Dễ bị bot cào quét, thử mã xác thực hàng loạt.
   - Trợ lý Chatbot AI (`/api/chatbot/**`): Tốn chi phí quota gọi External LLM (Gemini API) và tiêu hao tài nguyên CPU/RAM.
   - Thao tác tính toán nặng: Xuất bảng điểm PDF (`/api/student/ho-so-hoc-tap/bang-diem/pdf`), Import/Export CSV, Excel bảng điểm (`/api/lecturer/grades/*`, `/api/admin/*`).
3. **Môi trường Reverse Proxy & Giả mạo IP:**
   - Triển khai thực tế thường đặt backend Spring Boot phía sau Nginx, Traefik, AWS ALB hoặc Docker Bridge Network.
   - Bộ giải mã IP trước đây đọc trực tiếp header `X-Forwarded-For` đầu tiên do client gửi lên. Bất kỳ client nào gửi header giả mạo `X-Forwarded-For: 1.2.3.4` đều có thể vượt qua toàn bộ giới hạn IP hoặc cố tình giả mạo IP của nạn nhân để DoS.
4. **Môi trường Multi-Instance / Clustering:**
   - Khi chạy nhiều instance backend phía sau bộ cân bằng tải (Load Balancer), cơ chế `IN_MEMORY` lưu trong RAM của từng JVM riêng biệt. Một client gửi yêu cầu luân phiên sẽ không bị chặn đúng hạn mức (ngưỡng thực tế bị nhân lên gấp $N$ lần số lượng instance), tạo "cảm giác bảo vệ giả tạo".

---

## 2. Kiến trúc & Các Giải pháp Kỹ thuật Đạt chuẩn

### 2.1. Phân giải Địa chỉ IP An toàn (`ClientIpResolver`)
Hệ thống áp dụng cơ chế xác minh Proxy tin cậy (`Trusted Proxy Verification`):
- Cấu hình danh sách các địa chỉ IP hoặc dải mạng CIDR của Proxy tin cậy thông qua biến `RATE_LIMIT_TRUSTED_PROXIES` (mặc định: `127.0.0.1, ::1, 0:0:0:0:0:0:0:1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16`).
- **Nguyên tắc tin cậy:**
  - Nếu `request.getRemoteAddr()` **KHÔNG** thuộc danh sách Proxy tin cậy: Tuyệt đối bỏ qua mọi header `X-Forwarded-For` và `X-Real-IP`. Địa chỉ IP client được xác định duy nhất bằng `request.getRemoteAddr()`. Header giả mạo do client tự đặt bị vô hiệu hóa hoàn toàn.
  - Nếu `request.getRemoteAddr()` **THUỘC** danh sách Proxy tin cậy: Bóc tách chuỗi `X-Forwarded-For: client, proxy1, proxy2` duyệt ngược từ phải qua trái (từ proxy gần nhất tới máy chủ). Bỏ qua các hop proxy tin cậy để lấy chính xác IP đầu tiên nằm ngoài danh sách proxy tin cậy. Kiểm tra định dạng IP chuẩn để tránh tấn công Header Injection.

### 2.2. Dual-Key Rate Limiting cho Điểm cuối Đăng nhập
Để giải quyết triệt để xung đột giữa **chống brute-force** và **chống DoS người dùng hợp lệ dùng chung IP**, hệ thống triển khai kiểm soát 2 tầng độc lập:
1. **Tầng 1: Giới hạn theo Tài khoản (`login:account:{username}`)**
   - Áp dụng ngưỡng chặt chẽ: mặc định tối đa 5 yêu cầu / 60 giây trên mỗi tài khoản.
   - Trích xuất tên đăng nhập an toàn thông qua `CachedBodyHttpServletRequest` mà **không log mật khẩu hoặc token**.
   - Nếu tài khoản A bị tấn công thử mật khẩu liên tục, duy nhất tài khoản A bị khóa tạm thời 60 giây. Các tài khoản khác từ cùng IP ký túc xá / phòng máy vẫn đăng nhập bình thường.
2. **Tầng 2: Giới hạn theo IP (`login:ip:{clientIp}`)**
   - Áp dụng ngưỡng rộng hơn: mặc định tối đa 60 yêu cầu / 60 giây trên mỗi IP.
   - Ngăn chặn tấn công flood áp đảo tài nguyên endpoint đăng nhập, nhưng đủ rộng cho 50–60 sinh viên cùng gửi yêu cầu đăng nhập đầu giờ từ một mạng Wi-Fi NAT.

### 2.3. Phân nhóm Điểm cuối & Khóa Giới hạn (Endpoint Grouping & Keys)

| Nhóm Điểm cuối | URL Pattern / Matcher | Khóa Định danh (Partition Key) | Cấu hình Mặc định | Mục đích Bảo vệ |
|:---|:---|:---|:---:|:---|
| **Đăng nhập (Tài khoản)** | `POST /api/auth/login` | `login:account:{username}` | 5 req / 60s | Chống Brute-force mật khẩu tài khoản mục tiêu |
| **Đăng nhập (IP)** | `POST /api/auth/login` | `login:ip:{client_ip}` | 60 req / 60s | Chống DoS tràn lụt cổng đăng nhập |
| **Xác thực Bảng điểm** | `GET /api/public/bang-diem/xac-thuc/**` | `verify:ip:{client_ip}` | 20 req / 60s | Chống cào quét / vét cạn mã định danh văn bằng |
| **Trợ lý Chatbot AI** | `/api/chatbot/**` | `chatbot:user:{username}` hoặc `chatbot:ip:{client_ip}` | 20 req / 60s | Bảo vệ quota External LLM (Gemini API) & RAM |
| **Tác vụ Nặng / I/O** | `*export-csv`, `*export-xlsx`, `*import-*`, `*pdf` | `heavy:user:{username}` hoặc `heavy:ip:{client_ip}` | 15 req / 60s | Chống cạn kiệt CPU do render PDF / xử lý file lớn |
| **API Nghiệp vụ (Auth)** | Các API còn lại `/api/**` | `api:user:{username}` | 120 req / 60s | Giữ ổn định hoạt động nghiệp vụ cho người dùng |
| **API Nghiệp vụ (Ẩn danh)**| Các API công khai khác `/api/**` | `api:ip:{client_ip}` | 60 req / 60s | Hạn chế quét API tự động |

**Miễn trừ kiểm soát (Excluded Paths):**
- `/api/health` (Healthcheck phục vụ Load Balancer)
- `/api/chatbot/status` (Kiểm tra trạng thái sẵn sàng của AI)
- `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` (Tài liệu OpenAPI)
- `/h2-console/**` (Console kiểm thử nội bộ)

### 2.4. Chuẩn Phản hồi Lỗi HTTP 429 Too Many Requests
Khi vượt ngưỡng, hệ thống trả về mã trạng thái HTTP 429 và thông tin phản hồi chuẩn RFC:
- **HTTP Headers:**
  - `Retry-After: <số_giây_cần_chờ>`
  - `X-RateLimit-Limit: <hạn_mức_tối_đa>`
  - `X-RateLimit-Remaining: 0`
- **JSON Body:**
  ```json
  {
    "message": "Quá nhiều yêu cầu gửi tới hệ thống. Vui lòng thử lại sau 45 giây.",
    "timestamp": "2026-11-10T14:00:00Z",
    "retryAfterSeconds": 45
  }
  ```

---

## 3. Phân tích Multi-Instance & Chiến lược Lưu trữ Trạng thái

### 3.1. Giới hạn Kỹ thuật của `IN_MEMORY` trong Cụm Đa Instance
- `InMemoryRateLimiter` hoạt động trên JVM RAM cục bộ thông qua `ConcurrentHashMap`.
- Khi triển khai đa instance ($N$ container backend) phía sau Load Balancer:
  1. **Nhân bội hạn ngạch:** Một client gửi 10 yêu cầu phân bổ đều qua 3 instance thì mỗi instance chỉ ghi nhận 3–4 yêu cầu, dẫn đến client có thể gửi tới 30 yêu cầu/phút thay vì 10.
  2. **Trạng thái chập chờn (Jitter/Flickering):** Client có thể nhận 429 từ instance này nhưng ngay sau đó lại thành công ở instance khác do dữ liệu không đồng bộ.
  3. **Kết luận:** Trong môi trường multi-instance, `IN_MEMORY` chỉ mang lại "cảm giác bảo vệ giả tạo".

### 3.2. Giải pháp Lưu trữ Chung Tương thích Hạ tầng Hiện tại (`DATABASE`)
Dự án hiện tại sử dụng **MySQL 8.4** và **chưa có Redis/Memcached**. Do đó, `DatabaseDistributedRateLimiter` được tối ưu hóa trực tiếp trên bảng `rate_limit_bucket`:
- **Pessimistic Locking (`SELECT ... FOR UPDATE`):** Đảm bảo đồng bộ tuyệt đối khi nhiều instance ghi nhận request cho cùng một khóa đồng thời mà không bị race-condition.
- **Giao dịch Độc lập (`PROPAGATION_REQUIRES_NEW`):** Tránh ảnh hưởng hoặc rollback chéo với giao dịch nghiệp vụ chính.
- **Fail-Open An toàn:** Trong trường hợp cơ sở dữ liệu bị nghẽn I/O đột ngột, Rate Limiter ghi nhận cảnh báo và cho phép yêu cầu đi qua tạm thời, không chặn đứng hệ thống vận hành.
- **Định hướng Tương lai:** Khi hệ thống nâng cấp hạ tầng trang bị Redis, kiến trúc giao diện `RateLimiter` cho phép bổ sung `RedisDistributedRateLimiter` chỉ bằng việc thay đổi cấu hình `strategy: REDIS`.

---

## 4. Hướng dẫn Cấu hình Vận hành & Mẫu Biến Môi trường

Tất cả các tham số đều được ánh xạ qua biến môi trường trong `application.yml` và cấu hình mẫu tại `.env.example`:

```bash
# ==============================================================================
# CẤU HÌNH RATE LIMITING (QLSV-102 / TASK-OP-07B)
# ==============================================================================
# Bật/tắt toàn bộ Rate Limiting: true / false
RATE_LIMITING_ENABLED=true

# Chiến lược lưu trữ:
# - IN_MEMORY: Thích hợp cho môi trường phát triển (DEV) hoặc đơn container
# - DATABASE: Thích hợp cho môi trường sản xuất (PROD) chạy multi-instance
# - DISABLED: Tắt hoàn toàn (phục vụ kiểm thử tải nội bộ)
RATE_LIMITING_STRATEGY=IN_MEMORY

# Danh sách Proxy tin cậy (ngăn chặn giả mạo IP qua X-Forwarded-For)
RATE_LIMIT_TRUSTED_PROXIES=127.0.0.1,::1,0:0:0:0:0:0:0:1,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16

# Ngưỡng Đăng nhập: IP vs Tài khoản
RATE_LIMIT_LOGIN_IP_CAPACITY=60
RATE_LIMIT_LOGIN_IP_WINDOW_SECONDS=60
RATE_LIMIT_LOGIN_ACCOUNT_CAPACITY=5
RATE_LIMIT_LOGIN_ACCOUNT_WINDOW_SECONDS=60

# Ngưỡng Tra cứu văn bằng / Bảng điểm công khai
RATE_LIMIT_VERIFY_CAPACITY=20
RATE_LIMIT_VERIFY_WINDOW_SECONDS=60

# Ngưỡng Chatbot AI
RATE_LIMIT_CHATBOT_CAPACITY=20
RATE_LIMIT_CHATBOT_WINDOW_SECONDS=60

# Ngưỡng Tác vụ nặng (PDF, XLSX, CSV Import/Export)
RATE_LIMIT_HEAVY_CAPACITY=15
RATE_LIMIT_HEAVY_WINDOW_SECONDS=60

# Ngưỡng API nghiệp vụ thông thường (Người dùng đã xác thực vs Ẩn danh)
RATE_LIMIT_API_CAPACITY=120
RATE_LIMIT_API_WINDOW_SECONDS=60
RATE_LIMIT_API_ANONYMOUS_CAPACITY=60
RATE_LIMIT_API_ANONYMOUS_WINDOW_SECONDS=60
```

---

## 5. Nguyên tắc Bảo mật & Tuân thủ Quy chuẩn

1. **Bảo mật Thông tin Xác thực:** Tuyệt đối không ghi nhật ký log (logger) chứa mật khẩu, token JWT, Authorization header hoặc nội dung JSON body gửi lên từ client.
2. **Độc lập với Xác thực & Phân quyền:** Rate Limiter chỉ đóng vai trò điều tiết lưu lượng (traffic shaping) và chống tấn công từ chối dịch vụ. Mọi quy tắc phân quyền người dùng (`ROLE_QUAN_TRI`, `ROLE_GIANG_VIEN`, `ROLE_SINH_VIEN`) và tính hợp lệ của JWT token vẫn do Spring Security (`JwtAuthFilter`, `@PreAuthorize`) kiểm soát chặt chẽ.
3. **An toàn Bí mật (No Secret in Repo):** Toàn bộ cấu hình mẫu không chứa mật khẩu thực tế hoặc secret keys.
