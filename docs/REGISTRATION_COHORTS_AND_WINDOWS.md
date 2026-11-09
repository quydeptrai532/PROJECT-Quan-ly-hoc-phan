# Cấu hình Đối tượng Đăng ký và Điều chỉnh Đợt (QLSV-77 — TASK-AD-02)

Tài liệu đặc tả kiến trúc dữ liệu, múi giờ, quy tắc phân luồng đối tượng, thứ tự ưu tiên, điều chỉnh đợt đăng ký và cơ chế audit theo yêu cầu của vé **QLSV-77 / TASK-AD-02**.

---

## 1. Múi giờ và Kiểm tra Biên thời gian thống nhất

- **Múi giờ hệ thống quy định**: `Asia/Ho_Chi_Minh` (UTC+7).
- Cấu hình qua biến môi trường hoặc file `backend/src/main/resources/application.yml`:
  ```yaml
  app:
    registration:
      timezone: Asia/Ho_Chi_Minh
      multi-cohort-priority-policy: HIGHEST_PRIORITY
      policy-note: "Chờ quyết định chính thức từ Hội đồng Đào tạo và Quản lý Dự án về chính sách ưu tiên sinh viên thuộc nhiều nhóm."
      official-regulation: false
  ```
- **Kiểm tra biên thời gian**:
  - Thời điểm hiện tại `now` được tính chính xác theo múi giờ `Asia/Ho_Chi_Minh` (`ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime()`).
  - Kiểm tra tính hợp lệ của đợt: `!now.isBefore(dot.getBatDau()) && !now.isAfter(dot.getKetThuc())`.
  - Mọi thao tác ghi danh mới, xác nhận kế hoạch hay hủy học phần đều bắt buộc thỏa mãn biên thời gian của đợt áp dụng.

---

## 2. Mô hình Dữ liệu và Đối tượng Áp dụng

Bảng cơ sở dữ liệu: `dot_dang_ky` (Entity `RegistrationPeriod`):

| Trường | Kiểu dữ liệu | Ý nghĩa |
|---|---|---|
| `id` | `BIGINT` (PK) | Khóa chính tự tăng |
| `hoc_ky_id` | `BIGINT` (FK) | Học kỳ áp dụng đợt đăng ký |
| `ten_dot` | `VARCHAR(150)` | Tên định danh của đợt |
| `bat_dau` | `DATETIME` | Mốc bắt đầu mở đợt (UTC+7) |
| `ket_thuc` | `DATETIME` | Mốc kết thúc đợt (UTC+7) |
| `khoa_hoc_id` | `BIGINT` (FK, nullable) | Giới hạn theo Khóa tuyển sinh / Niên khóa |
| `nganh_id` | `BIGINT` (FK, nullable) | Giới hạn theo Ngành đào tạo |
| `lop_hanh_chinh_id`| `BIGINT` (FK, nullable) | Giới hạn theo Lớp hành chính |
| `nhom_doi_tuong` | `VARCHAR(100)` | Mã nhóm phân loại mở rộng (VD: CHINH_QUY, CHAT_LUONG_CAO) |
| `thu_tu_uu_tien` | `INT` | Thứ tự ưu tiên mở đợt (1 là mức ưu tiên cao nhất) |
| `cho_phep_dang_ky`| `BOOLEAN` | Cờ bật/tắt quyền đăng ký trong đợt |
| `da_gia_han` | `BOOLEAN` | Đánh dấu đợt đã từng được gia hạn thời gian |
| `da_dong_som` | `BOOLEAN` | Đánh dấu đợt bị đóng trước thời hạn kết thúc ban đầu |
| `da_mo_bo_sung` | `BOOLEAN` | Đánh dấu đã phát sinh đợt bổ sung |
| `dot_goc_id` | `BIGINT` (FK, nullable) | Liên kết đợt gốc nếu đây là đợt mở bổ sung |
| `ly_do_dieu_chinh`| `VARCHAR(500)` | Lý do điều chỉnh thời gian/đối tượng gần nhất |
| `nguoi_cap_nhat_cuoi`| `VARCHAR(100)` | Username người thực hiện điều chỉnh cuối |
| `cap_nhat_luc` | `DATETIME` | Thời điểm cập nhật cuối cùng |

### Quy tắc Phân luồng Đối tượng (Eligibility)
1. **Toàn trường / Mặc định**: Nếu `khoa_hoc_id`, `nganh_id`, `lop_hanh_chinh_id` và `nhom_doi_tuong` đều `null`, đợt đăng ký mở cho toàn bộ sinh viên có trạng thái hoạt động trong học kỳ.
2. **Theo Khóa**: Kiểm tra `student.getKhoaHoc().getId()` hoặc `student.getKhoaNhapHoc()`.
3. **Theo Ngành**: Kiểm tra `student.getNganh().getId()`.
4. **Theo Lớp**: Kiểm tra `student.getLopHanhChinh().getId()`.
5. **Theo Nhóm**: So sánh mã `nhom_doi_tuong` với thông tin phân loại sinh viên.

---

## 3. Thứ tự Ưu tiên và Trường hợp Sinh viên thuộc Nhiều Nhóm

- Mỗi đợt có trọng số `thuTuUuTien` (số nguyên dương, giá trị nhỏ hơn thể hiện độ ưu tiên cao hơn: #1 > #2 > #3).
- **Trường hợp sinh viên thuộc nhiều nhóm/đối tượng**:
  - Codebase trước QLSV-77 chưa có quy định pháp lý/nghiệp vụ chính thức về việc giải quyết xung đột khi sinh viên thuộc nhiều nhóm đối tượng có các khoảng thời gian khác nhau.
  - Theo đúng chỉ đạo của đề bài: **"Nếu sinh viên thuộc nhiều nhóm, áp dụng chính sách ưu tiên đã có; nếu chưa có, không tự đoán và ghi rõ cần quyết định"**.
  - **Quy tắc triển khai**:
    - Hệ thống cấu hình chính sách mặc định: `HIGHEST_PRIORITY` (xếp hạng các đợt thỏa mãn theo `thuTuUuTien ASC`, sau đó theo `batDau ASC` để chọn đợt tốt nhất).
    - Đồng thời hệ thống đánh dấu trạng thái pháp lý: `chinhSachUuTienDaChot: false`.
    - Phản hồi API và giao diện hiển thị rõ thông báo: *"Chờ quyết định chính thức từ Hội đồng Đào tạo và Quản lý Dự án về chính sách ưu tiên sinh viên thuộc nhiều nhóm."*

---

## 4. Các Nghiệp vụ Điều chỉnh Đợt và Bảo toàn Lịch sử

### 4.1 Gia hạn đợt (`POST /api/admin/dot-dang-ky/{id}/gia-han`)
- Tham số bắt buộc: `thoiGianMoi` (mốc kết thúc mới phải sau thời điểm hiện tại và sau mốc bắt đầu), `lyDo` (bắt buộc, không được để trống).
- Hệ thống cập nhật: `ketThuc = thoiGianMoi`, `daGiaHan = true`, `lyDoDieuChinh = lyDo`, `nguoiCapNhatCuoi = actor`, `capNhatLuc = now`.
- Tự động ghi nhận Audit Log hành động `REGISTRATION_PERIOD_EXTENDED`.

### 4.2 Đóng sớm đợt (`POST /api/admin/dot-dang-ky/{id}/dong-som`)
- Tham số bắt buộc: `lyDo` (bắt buộc, giải trình lý do đóng sớm).
- Hệ thống cập nhật: `ketThuc = now`, `daDongSom = true`, `choPhepDangKy = false`, `lyDoDieuChinh = lyDo`, `nguoiCapNhatCuoi = actor`, `capNhatLuc = now`.
- Tự động ghi nhận Audit Log hành động `REGISTRATION_PERIOD_CLOSED_EARLY`.
- **Bảo toàn dữ liệu**: Mọi học phần sinh viên đã đăng ký thành công trước thời điểm đóng sớm được giữ nguyên vẹn 100%, không bị hủy hay vô hiệu hóa.

### 4.3 Mở đợt bổ sung (`POST /api/admin/dot-dang-ky/{id}/mo-bo-sung`)
- Tham số: `batDau`, `ketThuc`, `tenDot`, `lyDo` (bắt buộc).
- Tạo mới một bản ghi `RegistrationPeriod` kế thừa đối tượng áp dụng (`khoaHoc`, `nganh`, `lopHanhChinh`, `nhomDoiTuong`) và `thuTuUuTien` từ đợt gốc.
- Đánh dấu đợt gốc: `daMoBoSung = true`.
- Tự động ghi nhận Audit Log hành động `REGISTRATION_PERIOD_SUPPLEMENTAL_OPENED`.

### 4.4 Bảo toàn lịch sử đăng ký
- Mọi điều chỉnh về thời gian, đối tượng, gia hạn hay đóng sớm **tuyệt đối không xóa hoặc làm sai lệch** bảng `dang_ky_hoc_phan` cũng như bảng nhật ký lịch sử `lich_su_dang_ky`.

---

## 5. Kiểm soát Backend Toàn diện

- Frontend chỉ đóng vai trò hiển thị trạng thái và hỗ trợ trải nghiệm người dùng.
- **Backend là chốt chặn bảo mật tuyệt đối**:
  - `RegistrationService.kiemTraQuyenDangKy`: Xác thực sinh viên có thuộc đối tượng của đợt hiện hành và thời điểm gọi API có nằm trong khoảng `[batDau, ketThuc]` của đợt áp dụng hay không.
  - Nếu không thỏa mãn, ném ngoại lệ `ApiException.forbidden("Tài khoản của bạn hiện không thuộc đối tượng hoặc chưa tới thời gian đăng ký học phần đợt này")` ngay lập tức, chặn đứng mọi request gọi trực tiếp qua Postman hay cURL.

---

## 6. Danh mục API

| Phương thức | Đường dẫn | Quyền | Mục đích |
|---|---|---|---|
| `GET` | `/api/admin/dot-dang-ky?hocKyId=...` | `QUAN_TRI` | Lấy danh sách đợt đăng ký theo học kỳ |
| `POST` | `/api/admin/dot-dang-ky` | `QUAN_TRI` | Tạo mới đợt đăng ký và cấu hình đối tượng |
| `PUT` | `/api/admin/dot-dang-ky/{id}` | `QUAN_TRI` | Chỉnh sửa thông tin đợt (kèm lý do audit) |
| `POST` | `/api/admin/dot-dang-ky/{id}/gia-han` | `QUAN_TRI` | Gia hạn thời gian kết thúc đợt (kèm lý do) |
| `POST` | `/api/admin/dot-dang-ky/{id}/dong-som` | `QUAN_TRI` | Đóng sớm đợt đăng ký ngay lập tức (kèm lý do) |
| `POST` | `/api/admin/dot-dang-ky/{id}/mo-bo-sung` | `QUAN_TRI` | Mở đợt bổ sung kế thừa đối tượng (kèm lý do) |
| `DELETE` | `/api/admin/dot-dang-ky/{id}?lyDo=...` | `QUAN_TRI` | Xóa cấu hình đợt đăng ký (kèm lý do) |
| `GET` | `/api/dang-ky/dot-hien-tai?hocKyId=...` | `SINH_VIEN` | Lấy đợt và trạng thái đăng ký áp dụng cho tài khoản sinh viên |
| `POST` | `/api/dang-ky` | `SINH_VIEN` | Đăng ký học phần (Backend validate chặt chẽ theo đợt) |
