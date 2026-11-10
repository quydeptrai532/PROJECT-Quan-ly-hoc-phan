# TÀI LIỆU MA TRẬN PHÂN QUYỀN VÀ KIỂM SOÁT TRUY CẬP (QLSV-88 / TASK-OP-01)

## 1. Giới thiệu và Nguyên tắc Thiết kế
Dự án Quản lý đào tạo & Đăng ký học phần (QLSV) áp dụng mô hình phân quyền kết hợp:
1. **Role-Based Access Control (RBAC)**: Phân quyền theo vai trò cấp cao tại `SecurityConfig` và chú thích `@PreAuthorize`. Hệ thống có 3 vai trò thực tế: `QUAN_TRI`, `GIANG_VIEN`, `SINH_VIEN`.
2. **Record-Level / Attribute-Based Access Control (ABAC)**: Kiểm soát chi tiết đến từng bản ghi tại tầng Service/Repository, đảm bảo không tin cậy client-supplied identifier (chống IDOR).
3. **Active Account Enforcement**: Kiểm tra trạng thái khóa tài khoản (`trangThai == false`) ngay tại bộ lọc JWT `JwtAuthFilter` và tầng Service thông qua `CurrentUser`. Khi tài khoản bị vô hiệu hóa, mọi yêu cầu API cần xác thực bị chặn ngay lập tức với mã **403 Forbidden**, dù token JWT vẫn còn hạn hiệu lực.
4. **Information Concealment**: Khi người dùng cố gắng thao tác hoặc truy xuất bản ghi thuộc sở hữu của người khác mà chính sách bảo mật yêu cầu che giấu sự tồn tại của dữ liệu, hệ thống trả về **404 Not Found** (ví dụ: hủy đăng ký học phần, chi tiết yêu cầu hỗ trợ cá nhân). Khi người dùng cố tình vi phạm ranh giới nghiệp vụ/phân công, hệ thống trả về **403 Forbidden**.

---

## 2. Ma trận phân quyền chi tiết (Authorization Matrix)

| Role | Hành động | Loại dữ liệu | Phạm vi bản ghi | Kết quả khi không có quyền |
| :--- | :--- | :--- | :--- | :--- |
| **Bất kỳ (Chưa đăng nhập)** | POST /api/auth/login | Tài khoản xác thực | Toàn bộ tài khoản | 401 Unauthorized nếu sai mật khẩu / 403 Forbidden nếu tài khoản bị khóa |
| **Bất kỳ (Chưa đăng nhập)** | GET /api/public/bang-diem/xac-thuc/** | Xác minh bảng điểm điện tử | Bảng điểm có mã xác minh hợp lệ (chỉ trả thông tin tối thiểu) | 429 Too Many Requests nếu vượt giới hạn tốc độ; KHONG_HOP_LE nếu sai mã |
| **Tất cả (Đã xác thực)** | `GET /api/auth/me` | Thông tin người dùng hiện tại | Chỉ tài khoản người dùng đang đăng nhập (`actor`) | `401 Unauthorized` nếu chưa đăng nhập / `403 Forbidden` nếu tài khoản bị khóa |
| **Tất cả (Đã xác thực)** | `PUT /api/auth/change-password` | Mật khẩu tài khoản | Chỉ tài khoản người dùng đang đăng nhập (`actor`) | `401 Unauthorized` / `403 Forbidden` nếu tài khoản bị khóa |
| **SINH_VIEN** | `GET /api/sinh-vien/me/thong-tin` | Hồ sơ sinh viên | Bản ghi sinh viên liên kết với tài khoản sinh viên đăng nhập | `401` nếu chưa đăng nhập; `403` nếu khác role hoặc tài khoản khóa |
| **SINH_VIEN** | `GET /api/sinh-vien/me/diem` | Bảng điểm, GPA/CPA | Điểm các môn học của chính sinh viên đang đăng nhập | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/diem/pdf` | File PDF bảng điểm | File PDF bảng điểm của chính sinh viên đang đăng nhập | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/thoi-khoa-bieu` | Thời khóa biểu | Các lớp học phần sinh viên đã đăng ký hợp lệ (`DA_DANG_KY`) | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/lich-su-hoc-tap` | Lịch sử học tập | Lịch sử học phần tích lũy của chính sinh viên đang đăng nhập | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/canh-bao-hoc-vu` | Cảnh báo học tập | Danh sách cảnh báo học vụ của chính sinh viên | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/tien-do-hoc-tap` | Tiến độ học tập | Chương trình đào tạo và tiến độ hoàn thành của chính sinh viên | `401` / `403` |
| **SINH_VIEN** | `POST, GET, PUT /api/sinh-vien/me/ke-hoach-hoc-tap/**` | Kế hoạch học tập cá nhân | Kế hoạch dự kiến môn học do chính sinh viên lập | `401` / `403` |
| **SINH_VIEN** | `GET /api/sinh-vien/me/dang-ky-hoc-phan/lich-su` | Lịch sử đăng ký học phần | Lịch sử thao tác đăng ký của chính sinh viên | `401` / `403` |
| **SINH_VIEN** | `POST /api/dang-ky` | Đăng ký học phần | Lớp học phần đang mở trong đợt đăng ký áp dụng cho sinh viên | `400` nếu xung đột / ngoài đợt; `403` nếu vi phạm quyền |
| **SINH_VIEN** | `DELETE /api/dang-ky/{id}` | Hủy đăng ký học phần | Bản ghi đăng ký của chính sinh viên (`sinhVienId == actor.id`) | `404 Not Found` (che giấu sự tồn tại nếu ID thuộc SV khác) |
| **SINH_VIEN** | `GET /api/sinh-vien/lop-hoc-phan/{id}/thong-bao` | Thông báo lớp học | Chỉ lớp học phần sinh viên đang ghi danh hợp lệ | `403 Forbidden` nếu không học lớp này |
| **SINH_VIEN** | `GET /api/phuc-khao/me` | Đơn phúc khảo | Danh sách đơn phúc khảo do chính sinh viên tạo | `401` / `403` |
| **SINH_VIEN** | `POST /api/phuc-khao` | Nộp đơn phúc khảo | Bản ghi điểm thi của chính sinh viên trong thời hạn phúc khảo | `400` / `403` nếu không đủ điều kiện |
| **SINH_VIEN** | `GET /api/phuc-khao/{id}` | Chi tiết đơn phúc khảo | Đơn phúc khảo của chính sinh viên | `404 Not Found` nếu ID thuộc sinh viên khác |
| **SINH_VIEN** | `GET, POST /api/support-requests/**` | Yêu cầu hỗ trợ sinh viên | Chỉ xem và gửi yêu cầu do chính sinh viên tạo (`nguoiGui.id == actor.id`) | `404 Not Found` hoặc `403 Forbidden` nếu truy cập ticket khác |
| **SINH_VIEN** | `GET /api/sinh-vien/**` ngoại vi hoặc admin | Quản lý hệ thống | Không có quyền | `403 Forbidden` |
| **GIANG_VIEN** | `GET /api/giang-vien/me/dashboard` | Dashboard giảng viên | Số liệu thống kê lớp do chính giảng viên phụ trách | `401` / `403` |
| **GIANG_VIEN** | `GET /api/giang-vien/lop-hoc-phan` | Danh sách lớp học phần | Các lớp học phần được phân công giảng dạy cho giảng viên đăng nhập | `401` / `403` |
| **GIANG_VIEN** | `GET /api/giang-vien/lop-hoc-phan/{id}/sinh-vien` | Danh sách SV & bảng điểm lớp | Chỉ lớp học phần thuộc quyền phụ trách của giảng viên | `403 Forbidden` nếu lớp thuộc GV khác |
| **GIANG_VIEN** | `GET /api/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv` | Export danh sách SV (CSV) | Chỉ lớp học phần thuộc quyền phụ trách của giảng viên | `403 Forbidden` nếu lớp thuộc GV khác |
| **GIANG_VIEN** | `GET /api/giang-vien/lop-hoc-phan/{id}/thong-ke-diem` | Thống kê phân phổ điểm | Chỉ lớp học phần thuộc quyền phụ trách của giảng viên | `403 Forbidden` nếu lớp thuộc GV khác |
| **GIANG_VIEN** | `POST /api/giang-vien/lop-hoc-phan/{id}/thong-bao` | Gửi thông báo lớp học | Chỉ lớp học phần thuộc quyền phụ trách của giảng viên | `403 Forbidden` nếu lớp thuộc GV khác |
| **GIANG_VIEN** | `GET, POST, PUT /api/giang-vien/lop-hoc-phan/{id}/diem-danh/**` | Điểm danh sinh viên theo buổi | Chỉ lớp học phần thuộc quyền phụ trách của giảng viên | `403 Forbidden` nếu lớp thuộc GV khác |
| **GIANG_VIEN** | `GET /api/giang-vien/sinh-vien/{sinhVienId}/chuyen-can` | Báo cáo chuyên cần sinh viên | Chỉ sinh viên đang theo học ít nhất 1 lớp do giảng viên phụ trách | `403 Forbidden` nếu sinh viên không học lớp nào của GV |
| **GIANG_VIEN** | `POST /api/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem` | Trình duyệt bảng điểm | Lớp học phần do giảng viên phụ trách ở trạng thái `CHUA_KHOA` | `403 Forbidden` nếu lớp của GV khác |
| **GIANG_VIEN** | `POST /api/giang-vien/lop-hoc-phan/{id}/phe-duyet-diem` | Duyệt bảng điểm | **Không có quyền** (Chỉ dành riêng cho `QUAN_TRI`) | `403 Forbidden` |
| **GIANG_VIEN** | `POST /api/giang-vien/lop-hoc-phan/{id}/cong-bo-diem` | Công bố bảng điểm | **Không có quyền** (Chỉ dành riêng cho `QUAN_TRI`) | `403 Forbidden` |
| **GIANG_VIEN** | `GET, POST, PATCH /api/phuc-khao/**` | Phúc khảo điểm môn học | Chỉ các đơn phúc khảo thuộc các lớp do giảng viên phụ trách | `403 Forbidden` nếu ngoài phân công |
| **GIANG_VIEN** | `POST /api/admin/**` | Quản trị hệ thống | Không có quyền | `403 Forbidden` |
| **QUAN_TRI** | `GET, POST, PUT, DELETE /api/admin/sinh-vien/**` | Quản lý sinh viên | Toàn bộ sinh viên trong trường | `401` nếu chưa đăng nhập; `403` nếu không phải QUAN_TRI |
| **QUAN_TRI** | PATCH /api/admin/sinh-vien/{id}/trang-thai | Khóa/Mở khóa tài khoản SV | Toàn bộ sinh viên | 401 / 403 |
| **QUAN_TRI** | GET, POST /api/admin/bang-diem-xac-thuc/** | Quản lý bảng điểm xác thực | Tra cứu toàn trường, tải PDF snapshot, thu hồi và cấp lại phiên bản mới | 401 / 403 |
| **SINH_VIEN** | GET /api/sinh-vien/me/bang-diem-xac-thuc/** | Bảng điểm xác thực cá nhân | Chỉ danh sách và PDF snapshot của chính sinh viên đang đăng nhập | 401 / 403 |
| **QUAN_TRI** | `POST /api/admin/sinh-vien/import-csv` | Bulk import sinh viên | Toàn trường | `401` / `403` |
| **QUAN_TRI** | `GET, POST, PUT, DELETE /api/admin/giang-vien/**` | Quản lý giảng viên | Toàn bộ giảng viên trong trường | `401` / `403` |
| **QUAN_TRI** | `PATCH /api/admin/giang-vien/{id}/trang-thai` | Khóa/Mở khóa tài khoản GV | Toàn bộ giảng viên | `401` / `403` |
| **QUAN_TRI** | `GET, POST, PUT, DELETE /api/admin/lop-hoc-phan/**` | Quản lý lớp học phần | Toàn bộ lớp học phần tất cả học kỳ | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/{id}/huy-lop` | Hủy lớp học phần | Toàn bộ lớp học phần | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/{id}/chuyen-lop` | Chuyển sinh viên sang lớp khác | Toàn bộ lớp học phần | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/kiem-tra-xung-dot` | Phát hiện xung đột phòng/lịch | Toàn bộ lớp học phần | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/{id}/phe-duyet-diem` | Phê duyệt bảng điểm | Toàn bộ lớp học phần ở trạng thái chờ duyệt | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/{id}/tra-ve-diem` | Trả về bảng điểm để sửa | Toàn bộ lớp học phần ở trạng thái chờ duyệt | `401` / `403` |
| **QUAN_TRI** | `POST /api/admin/lop-hoc-phan/{id}/cong-bo-diem` | Công bố bảng điểm chính thức | Toàn bộ lớp học phần đã duyệt | `401` / `403` |
| **QUAN_TRI** | `GET, POST, PUT /api/admin/phong-hoc/**` | Quản lý phòng học | Toàn bộ phòng học | `401` / `403` |
| **QUAN_TRI** | `GET /api/admin/thong-ke/**` | Báo cáo, thống kê, KPI | Toàn trường và theo khoa/ngành | `401` / `403` |
| **QUAN_TRI** | `GET /api/admin/thong-ke/xuat-excel` | Export Excel thống kê | Toàn bộ chỉ số KPI đào tạo | `401` / `403` |
| **QUAN_TRI** | `GET /api/admin/audit-logs/**` | Nhật ký kiểm toán hệ thống | Toàn bộ log thao tác hệ thống | `401` / `403` |
| **QUAN_TRI** | `GET, PATCH /api/admin/support-requests/**` | Quản lý Ticket hỗ trợ | Toàn bộ ticket hỗ trợ sinh viên | `401` / `403` |

---

## 3. Chuẩn hóa mã phản hồi HTTP (HTTP Status Code Standardization)

1. **401 Unauthorized**:
   - Áp dụng khi yêu cầu không có JWT header (`Authorization: Bearer <token>`), token không đúng cấu trúc, sai chữ ký cryptographic, hoặc đã hết hạn (expired).
   - Định dạng phản hồi:
     ```json
     {
       "timestamp": "2026-11-09T18:48:00",
       "status": 401,
       "error": "Unauthorized",
       "message": "Phiên đăng nhập không hợp lệ",
       "path": "/api/..."
     }
     ```
2. **403 Forbidden**:
   - Áp dụng khi người dùng đã xác thực nhưng:
     a) Role không có quyền truy cập endpoint (ví dụ: `SINH_VIEN` gọi endpoint `/api/admin/**`).
     b) Tài khoản đang ở trạng thái bị vô hiệu hóa / khóa (`trangThai == false`).
     c) Giảng viên cố gắng truy cập dữ liệu của lớp/sinh viên không nằm trong phân công phụ trách.
   - Định dạng phản hồi khi tài khoản bị khóa:
     ```json
     {
       "status": 403,
       "message": "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo"
     }
     ```
   - Định dạng phản hồi khi không có quyền thao tác:
     ```json
     {
       "status": 403,
       "message": "Bạn không có quyền thực hiện thao tác này"
     }
     ```
3. **404 Not Found**:
   - Áp dụng khi tài nguyên không tồn tại trong hệ thống.
   - **Chính sách che giấu (Information Concealment)**: Khi sinh viên thao tác trên tài nguyên nhạy cảm (như hủy đăng ký môn học `DELETE /api/dang-ky/{id}` hoặc xem chi tiết phúc khảo `GET /api/phuc-khao/{id}`), nếu `{id}` thuộc sở hữu của sinh viên khác, hệ thống truy vấn theo điều kiện kết hợp `id` và `sinhVienId`. Khi không tìm thấy bản ghi tương ứng, hệ thống trả về **404 Not Found** với thông điệp trung tính (ví dụ: `"Đăng ký không tồn tại"`), ngăn ngừa kẻ tấn công dò quét sự tồn tại của ID (ID enumeration).

---

## 4. Cơ chế chống IDOR (Insecure Direct Object Reference)

1. **Không tin cậy Client-Supplied Identifiers**:
   - Với vai trò `SINH_VIEN`: Toàn bộ các API cá nhân (`/api/sinh-vien/me/**`) giải mã danh tính trực tiếp từ JWT Subject (`actor.getUsername()` -> `Student`). Backend không sử dụng tham số `sinhVienId` từ Query Params hoặc Request Body để xác định người thao tác.
   - Khi thao tác trên bản ghi đơn lẻ (`/api/dang-ky/{id}`): Tầng service thực hiện truy vấn `findByIdAndSinhVienId(id, currentStudent.getId())`.
2. **Kiểm tra ranh giới phân công giảng dạy (Lecturer Scope Enforcement)**:
   - Mọi thao tác của giảng viên trên Lớp học phần (`/api/giang-vien/lop-hoc-phan/{id}/**`) đều bắt buộc xác thực:
     `courseClass.getGiangVien().getId().equals(currentLecturer.getId())`.
   - Báo cáo chuyên cần sinh viên (`/api/giang-vien/sinh-vien/{id}/chuyen-can`): Xác thực xem sinh viên có đang theo học ít nhất một lớp do giảng viên này phụ trách hay không thông qua `dangKyHocPhanRepository.existsBySinhVienIdAndGiangVienIdAndTrangThai(...)`. Nếu không, ném `403 Forbidden`.
   - Export CSV danh sách sinh viên lớp: Buộc kiểm tra quyền giảng viên phụ trách trước khi xuất dữ liệu.
3. **Ngăn chặn leo thang đặc quyền (Privilege Escalation)**:
   - Các thao tác phê duyệt bảng điểm (`/phe-duyet-diem`) và công bố bảng điểm (`/cong-bo-diem`) chỉ cho phép `QUAN_TRI` thực hiện, Giảng viên chỉ có quyền gửi duyệt (`/gui-duyet-diem`).

---

## 5. Cơ chế kiểm soát tài khoản bị khóa (Locked Account Enforcement)

1. **Tầng Bộ lọc JWT (`JwtAuthFilter`)**:
   - Khi request gửi kèm token hợp lệ, `JwtAuthFilter` trích xuất username và kiểm tra qua `UserAuthCache.find(username)` (truy vấn DB có caching).
   - Nếu `user.active() == false`:
     - Không cho phép thiết lập `SecurityContext`.
     - Lập tức gửi phản hồi `403 Forbidden` với cấu trúc JSON chuẩn `{"message": "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo"}`.
     - Ngăn chặn triệt để trường hợp token JWT còn hạn (chưa hết 24h) nhưng tài khoản đã bị admin khóa trong DB.
2. **Tầng Service (`CurrentUser`)**:
   - Trong phương thức `requireUser()`, bổ sung kiểm tra kép:
     ```java
     if (!user.isTrangThai()) {
         throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị khóa, vui lòng liên hệ phòng đào tạo");
     }
     ```
   - Khi quản trị viên cập nhật trạng thái người dùng tại `AdminStudentService` và `AdminLecturerService`, đồng thời gọi `userAuthCache.evict(username)` để hủy bỏ cache ngay lập tức.

---

## 6. Danh mục Endpoint đã rà soát toàn diện

### 6.1. Nhóm Xác thực & Người dùng (`/api/auth/**`)
- `POST /api/auth/login`: Public. Kiểm tra đúng tài khoản, mật khẩu, và trạng thái `trangThai == true`.
- `GET /api/auth/me`: Yêu cầu xác thực. Lấy hồ sơ người dùng đang đăng nhập.
- `PUT /api/auth/change-password`: Yêu cầu xác thực. Đổi mật khẩu tài khoản hiện tại.

### 6.2. Nhóm Sinh viên Self-Service (`/api/sinh-vien/**`, `/api/dang-ky/**`)
- `GET /api/sinh-vien/me/thong-tin`: Chỉ `SINH_VIEN`, lấy thông tin cá nhân.
- `GET /api/sinh-vien/me/diem`: Chỉ `SINH_VIEN`, lấy bảng điểm cá nhân.
- `GET /api/sinh-vien/me/diem/pdf`: Chỉ `SINH_VIEN`, xuất PDF bảng điểm cá nhân.
- `GET /api/sinh-vien/me/thoi-khoa-bieu`: Chỉ `SINH_VIEN`, lấy TKB theo học kỳ.
- `GET /api/sinh-vien/me/canh-bao-hoc-vu`: Chỉ `SINH_VIEN`, xem cảnh báo học tập.
- `GET /api/sinh-vien/me/tien-do-hoc-tap`: Chỉ `SINH_VIEN`, xem tiến độ tín chỉ.
- `GET /api/sinh-vien/me/ke-hoach-hoc-tap/**`: Chỉ `SINH_VIEN`, lập và xem kế hoạch học tập.
- `GET /api/sinh-vien/me/dang-ky-hoc-phan/lich-su`: Chỉ `SINH_VIEN`, xem lịch sử đăng ký.
- `POST /api/dang-ky`: Chỉ `SINH_VIEN`, đăng ký lớp học phần trong kỳ.
- `DELETE /api/dang-ky/{id}`: Chỉ `SINH_VIEN`, hủy môn đã đăng ký của chính mình (chống IDOR).
- `GET /api/sinh-vien/lop-hoc-phan/{id}/thong-bao`: Chỉ `SINH_VIEN` đã đăng ký lớp mới được xem thông báo lớp.

### 6.3. Nhóm Giảng viên (`/api/giang-vien/**`)
- `GET /api/giang-vien/me/dashboard`: Chỉ `GIANG_VIEN`, số liệu tổng quan cá nhân.
- `GET /api/giang-vien/lop-hoc-phan`: Chỉ `GIANG_VIEN`, danh sách lớp được phân công.
- `GET /api/giang-vien/lop-hoc-phan/{id}/sinh-vien`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}`.
- `GET /api/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}` (Export CSV).
- `GET /api/giang-vien/lop-hoc-phan/{id}/thong-ke-diem`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}`.
- `POST /api/giang-vien/lop-hoc-phan/{id}/thong-bao`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}`.
- `GET, POST, PUT /api/giang-vien/lop-hoc-phan/{id}/diem-danh/**`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}`.
- `GET /api/giang-vien/sinh-vien/{sinhVienId}/chuyen-can`: Chỉ `GIANG_VIEN` đang phụ trách lớp có sinh viên `{sinhVienId}` theo học (Đã fix IDOR).
- `POST /api/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem`: Chỉ `GIANG_VIEN` phụ trách lớp `{id}` gửi duyệt bảng điểm.

### 6.4. Nhóm Quản trị viên (`/api/admin/**`)
- `GET, POST, PUT, DELETE /api/admin/sinh-vien/**`: Toàn quyền quản trị sinh viên.
- `PATCH /api/admin/sinh-vien/{id}/trang-thai`: Khóa/mở khóa tài khoản sinh viên.
- `POST /api/admin/sinh-vien/import-csv`: Import danh sách sinh viên hàng loạt.
- `GET, POST, PUT, DELETE /api/admin/giang-vien/**`: Toàn quyền quản trị giảng viên.
- `PATCH /api/admin/giang-vien/{id}/trang-thai`: Khóa/mở khóa tài khoản giảng viên.
- `GET, POST, PUT, DELETE /api/admin/lop-hoc-phan/**`: Quản trị toàn bộ lớp học phần.
- `POST /api/admin/lop-hoc-phan/{id}/huy-lop`: Hủy lớp học phần.
- `POST /api/admin/lop-hoc-phan/{id}/chuyen-lop`: Chuyển sinh viên sang lớp học phần khác.
- `POST /api/admin/lop-hoc-phan/{id}/phe-duyet-diem`: Duyệt điểm thi chính thức.
- `POST /api/admin/lop-hoc-phan/{id}/tra-ve-diem`: Trả về điểm thi.
- `POST /api/admin/lop-hoc-phan/{id}/cong-bo-diem`: Công bố điểm thi toàn trường.
- `POST /api/admin/lop-hoc-phan/kiem-tra-xung-dot`: Kiểm tra xung đột phòng và lịch học.
- `GET, POST, PUT /api/admin/phong-hoc/**`: Quản lý danh mục phòng học.
- `GET /api/admin/thong-ke/**`: Báo cáo, thống kê và export KPI đào tạo.
- `GET /api/admin/audit-logs/**`: Tra cứu nhật ký kiểm toán.
- `GET, PATCH /api/admin/support-requests/**`: Tiếp nhận và xử lý yêu cầu hỗ trợ sinh viên.

---

## 7. Các điểm lưu ý và Quyền hạn đặc biệt
- **Không có vai trò trung gian**: Hiện tại hệ thống sử dụng 3 vai trò duy nhất (`QUAN_TRI`, `GIANG_VIEN`, `SINH_VIEN`). Nếu trong tương lai mở rộng thêm vai trò `TRUONG_KHOA` hoặc `GIAO_VU_KHOA`, cần tách quyền phê duyệt điểm cấp Khoa và cấp Phòng Đào tạo.
- **Export / Bulk Data**: Tất cả API xuất dữ liệu (PDF bảng điểm cá nhân, CSV sinh viên lớp, Excel thống kê KPI) đều kế thừa chính xác bộ lọc phân quyền của API truy vấn tương ứng, không có bypass qua query parameters.
