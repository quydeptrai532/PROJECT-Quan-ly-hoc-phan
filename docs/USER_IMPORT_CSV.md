# Hướng dẫn & Đặc tả Kỹ thuật Import Người dùng (Sinh viên & Giảng viên) từ CSV — QLSV-82 (TASK-AD-07)

Tài liệu này mô tả chi tiết giải pháp Import giảng viên, chuẩn hóa quy trình import sinh viên hiện hữu, cơ chế xác thực dữ liệu backend, xem trước (Preview) gắn phiên ngắn hạn, chính sách giao dịch (Transaction Policy), chính sách chống trùng lặp (Idempotency Policy), các rào chắn bảo mật và danh mục API quản trị viên.

---

## 1. Định dạng Template CSV & Quy tắc Cột

Hệ thống hỗ trợ xuất và tải tệp CSV mẫu chuẩn UTF-8 (có gắn BOM `\uFEFF` để phần mềm Microsoft Excel hiển thị đúng tiếng Việt).

### A. Template Import Giảng viên (`mau-import-giang-vien.csv`)
- **API tải mẫu:** `GET /api/admin/giang-vien/import/template`
- **Cấu trúc dòng tiêu đề (Header):** `ma,hoTen,email,soDienThoai,khoaId`

| Cột CSV | Tên hiển thị chấp nhận (Alias) | Bắt buộc | Định dạng & Quy tắc kiểm tra |
|---|---|---|---|
| `ma` | `ma`, `maGiangVien`, `ma_giang_vien`, `magv`, `Mã giảng viên` | **Có** | Tối đa 20 ký tự. Không được trùng trong tệp import và không được trùng với mã/username đã có trong cơ sở dữ liệu. |
| `hoTen` | `hoTen`, `ho_ten`, `hovaten`, `Họ và tên`, `Họ tên` | **Có** | Tối đa 150 ký tự, không được để trống. |
| `email` | `email`, `mail`, `thudientu`, `Thư điện tử` | **Có** | Tối đa 150 ký tự, định dạng email chuẩn. Không được trùng lặp trong tệp hoặc trong hệ thống. |
| `soDienThoai` | `soDienThoai`, `so_dien_thoai`, `sdt`, `sđt`, `Số điện thoại` | Không | Tối đa 20 ký tự, chỉ chứa chữ số và các ký tự điện thoại hợp lệ `+ - ( )`. |
| `khoaId` | `khoaId`, `khoa_id`, `khoa`, `makhoa`, `Mã khoa` | Không | Số nguyên dương (`Long`). Nếu cung cấp thì bắt buộc phải tồn tại trong bảng `khoa`. |

### B. Template Import Sinh viên (`mau-import-sinh-vien.csv`)
- **API tải mẫu:** `GET /api/admin/sinh-vien/import/template`
- **Cấu trúc dòng tiêu đề (Header):** `ma,hoTen,email,khoaId,nganhId,lopId` (hỗ trợ tùy chọn thêm `soDienThoai`)

| Cột CSV | Tên hiển thị chấp nhận (Alias) | Bắt buộc | Định dạng & Quy tắc kiểm tra |
|---|---|---|---|
| `ma` | `ma`, `maSinhVien`, `ma_sinh_vien`, `masv`, `Mã sinh viên` | **Có** | Tối đa 20 ký tự. Không được trùng trong tệp import và không được trùng với mã/username sinh viên đã có. |
| `hoTen` | `hoTen`, `ho_ten`, `hovaten`, `Họ và tên`, `Họ tên` | **Có** | Tối đa 150 ký tự, không được để trống. |
| `email` | `email`, `mail`, `thudientu`, `Thư điện tử` | **Có** | Tối đa 150 ký tự, đúng định dạng email RFC. Không trùng lặp. |
| `khoaId` | `khoaId`, `khoa_id`, `khoa`, `makhoa`, `Mã khoa` | **Có** | Số nguyên dương (`Long`). Bắt buộc phải tồn tại trong bảng `khoa`. |
| `nganhId` | `nganhId`, `nganh_id`, `nganh`, `manganh`, `Mã ngành` | Không | Số nguyên dương (`Long`). Nếu có, phải tồn tại trong bảng `nganh` và **phải thuộc khoa** (`khoaId`) đã chọn. |
| `lopId` | `lopId`, `lop_id`, `lop`, `lophanhchinh`, `Lớp hành chính` | Không | Số nguyên dương (`Long`). Nếu có, phải tồn tại trong bảng `lop_hanh_chinh` và **phải thuộc ngành/khoa** đã chọn. |
| `soDienThoai` | `soDienThoai`, `so_dien_thoai`, `sdt`, `sđt` | Không | Tối đa 20 ký tự, kiểm tra ký tự số điện thoại hợp lệ. |

---

## 2. Rào chắn Bảo mật Backend & Kiểm soát Dữ liệu

1. **Xác thực cấu trúc Header tổng thể:**
   - Nếu tệp tải lên rỗng hoặc thiếu bất kỳ cột bắt buộc nào trong danh sách tiêu đề, hệ thống từ chối ngay lập tức với lỗi `400 Bad Request` tổng quan. Tuyệt đối **không xử lý file một phần**.
2. **Kiểm tra tính toàn vẹn tham chiếu (Referential Integrity):**
   - Mọi khóa ngoại tham chiếu (`khoaId`, `nganhId`, `lopId`) được truy vấn kiểm tra trực tiếp ở backend; từ chối mọi ID không tồn tại hoặc vi phạm cây quan hệ `Khoa -> Ngành -> Lớp hành chính`.
3. **An toàn bảo mật thông tin xác thực:**
   - Mật khẩu mặc định (`123456`) và mã hash mật khẩu BCrypt **tuyệt đối không bao giờ được đưa vào** kết quả preview, nhật ký hệ thống (log), nhật ký kiểm toán (audit trail) hay tệp báo lỗi CSV.
4. **Phân quyền truy cập:**
   - Toàn bộ các API import được bảo vệ ở cấp độ Controller bằng `@PreAuthorize("hasRole('QUAN_TRI')")`. Người dùng vai trò khác hoặc chưa đăng nhập bị từ chối truy cập (401/403).
5. **Giới hạn tệp & Chống DoS:**
   - Kích thước tệp tối đa 5 MB, định dạng bắt buộc `.csv` (hoặc text/plain chứa CSV).

---

## 3. Chính sách Giao dịch & Chống trùng lặp

### Chính sách Giao dịch: `ROW_BY_ROW`
- Mỗi dòng dữ liệu hợp lệ được thực thi trong **một giao dịch độc lập** (`transactionTemplate.execute(...)`).
- Đảm bảo **nguyên tử ở cấp độ dòng**: bản ghi `User` và `Student`/`Lecturer` luôn được ghi cùng lúc hoặc rollback cùng lúc; **không bao giờ ghi nửa dòng**.
- Dòng bị lỗi sẽ được rollback cục bộ và ghi nhận chi tiết lỗi vào danh sách kết quả, không làm ảnh hưởng đến các dòng hợp lệ khác. Báo cáo phản hồi chỉ rõ trạng thái từng dòng: số dòng thành công, số dòng thất bại (không mô tả như import nguyên khối).

### Chính sách Chống trùng lặp (Idempotency): `SKIP_IF_EXISTS`
- Nếu mã định danh (`ma`), tên đăng nhập (`username`) hoặc `email` đã tồn tại trong cơ sở dữ liệu: dòng đó được đánh dấu là `TRUNG_LAP` và từ chối tạo mới.
- Hệ thống **không tự ý ghi đè** (overwrite) thông tin tài khoản đã có để bảo toàn an toàn dữ liệu và lịch sử học tập/giảng dạy.

---

## 4. Các Quy tắc Mới Cần Xác Nhận Chính Thức (Quyết định Chờ Chốt)

1. **Quy tắc sinh tên đăng nhập (Username) giảng viên:**
   - *Hiện tại áp dụng:* `username = ma` (đồng nhất với sinh viên và chức năng tạo thủ công).
   - *Điểm chờ xác nhận:* Trường đào tạo có muốn quy tắc tạo username theo tiền tố (ví dụ `gv.<hoTenKhongDau>` hoặc email prefix) hay giữ nguyên `username = ma`?
2. **Chính sách mật khẩu khởi tạo:**
   - *Hiện tại áp dụng:* Mật khẩu mặc định khởi tạo là `123456` (được băm bằng BCrypt).
   - *Điểm chờ xác nhận:* Có cần bổ sung cờ bắt buộc đổi mật khẩu ở lần đăng nhập đầu tiên (`mustChangePasswordOnFirstLogin`) hoặc gửi email kích hoạt tự động hay không?

---

## 5. Danh mục API Quản trị Import

| Phương thức | Endpoint | Chức năng |
|---|---|---|
| `GET` | `/api/admin/sinh-vien/import/template` | Tải tệp CSV mẫu import sinh viên (UTF-8 có BOM). |
| `POST` | `/api/admin/sinh-vien/import/preview` | Upload file CSV sinh viên, phân tích và trả về preview từng dòng kèm `previewToken` và `fileSha256`. |
| `POST` | `/api/admin/sinh-vien/import/confirm` | Xác nhận thực thi import phiên preview hợp lệ cho sinh viên. |
| `POST` | `/api/admin/sinh-vien/import` | Import trực tiếp tệp CSV sinh viên (tương thích ngược). |
| `POST` | `/api/admin/sinh-vien/import/export-errors` | Xuất danh sách các dòng lỗi của sinh viên thành tệp CSV báo cáo. |
| `GET` | `/api/admin/giang-vien/import/template` | Tải tệp CSV mẫu import giảng viên (UTF-8 có BOM). |
| `POST` | `/api/admin/giang-vien/import/preview` | Upload file CSV giảng viên, phân tích và trả về preview từng dòng kèm `previewToken` và `fileSha256`. |
| `POST` | `/api/admin/giang-vien/import/confirm` | Xác nhận thực thi import phiên preview hợp lệ cho giảng viên. |
| `POST` | `/api/admin/giang-vien/import` | Import trực tiếp tệp CSV giảng viên. |
| `POST` | `/api/admin/giang-vien/import/export-errors` | Xuất danh sách các dòng lỗi của giảng viên thành tệp CSV báo cáo. |
