# Tài liệu Kỹ thuật: Quản lý Trạng thái Học tập Sinh viên (QLSV-84 — TASK-AD-09)

## 1. Tổng quan
Tính năng quản lý trạng thái học tập sinh viên cho phép bộ phận Quản trị Đào tạo theo dõi, cập nhật và lưu vết toàn bộ vòng đời học vụ của sinh viên (đang học, bảo lưu, thôi học, tốt nghiệp). Tính năng này đồng thời liên kết chặt chẽ với hệ thống Đăng ký học phần nhằm đảm bảo tính toàn vẹn nghiệp vụ đào tạo.

---

## 2. Danh mục Trạng thái Học tập

Trạng thái học tập của sinh viên được định nghĩa tập trung qua enum `StudentAcademicStatus`:

| Mã trạng thái | Tên hiển thị | Mô tả nghiệp vụ |
| :--- | :--- | :--- |
| `DANG_HOC` | **Đang học** | Sinh viên đang theo học bình thường tại trường. |
| `BAO_LUU` | **Bảo lưu** | Sinh viên tạm dừng tiến trình học tập theo đơn hoặc quyết định bảo lưu có thời hạn. |
| `THOI_HOC` | **Thôi học** | Sinh viên dừng học theo quyết định buộc thôi học hoặc tự nguyện xin thôi học. |
| `TOT_NGHIEP` | **Tốt nghiệp** | Sinh viên đã hoàn thành chương trình đào tạo và được công nhận tốt nghiệp. |

> [!NOTE]
> Trạng thái học tập (`Student.trangThaiHocTap`) hoàn toàn độc lập với trạng thái khóa/mở khóa tài khoản đăng nhập (`User.trangThai`). Sinh viên bảo lưu vẫn có thể đăng nhập vào hệ thống để tra cứu điểm, xem thông tin học phí, tải bảng điểm và xem lịch sử trạng thái của bản thân.

---

## 3. Ma trận Trạng thái × Quyền Đăng ký Học phần

Hệ thống triển khai cấu hình tập trung tại `StudentAcademicStatusPolicy.java` theo nguyên tắc **Mặc định An toàn (Safe Default)**:

| Trạng thái học tập | Quyền đăng ký môn mới | Hành vi hệ thống khi sinh viên cố gắng đăng ký | Trạng thái chốt quy chế |
| :--- | :---: | :--- | :--- |
| `DANG_HOC` |  **Cho phép** | Tiếp nhận và kiểm tra điều kiện lớp học phần / đợt đăng ký bình thường. | Đã chốt |
| `BAO_LUU` | ❌ **Chặn** | Chặn ở Backend với HTTP 400 và thông báo: *"Sinh viên đang trong thời gian bảo lưu, không đủ điều kiện đăng ký học phần mới."* | ⚠️ **Cần chủ dự án xác nhận** (xem xét có cho phép đăng ký trước cho kỳ tái nhập học hay không) |
| `THOI_HOC` | ❌ **Chặn** | Chặn ở Backend với HTTP 400: *"Sinh viên đã thôi học, không được phép đăng ký học phần."* | Đã chốt |
| `TOT_NGHIEP` | ❌ **Chặn** | Chặn ở Backend với HTTP 400: *"Sinh viên đã tốt nghiệp, không được phép đăng ký học phần mới."* | Đã chốt |

### Nguyên tắc bảo toàn dữ liệu lịch sử
Khi sinh viên thay đổi trạng thái (ví dụ từ `DANG_HOC` sang `BAO_LUU` hoặc `THOI_HOC`):
- Hệ thống **KHÔNG XÓA** và **KHÔNG VÔ HIỆU HÓA** các đăng ký học phần hoặc điểm số đã tích lũy trước đó.
- Lịch sử đăng ký và điểm quá trình / kết thúc học phần vẫn được bảo lưu trọn vẹn để phục vụ báo cáo và xét điều kiện khi sinh viên quay lại học tiếp.

---

## 4. Quản lý Lịch sử Chuyển đổi và Kiểm soát Concurrency

### Cấu trúc Bảng `sinh_vien_lich_su_trang_thai`
Mỗi lần quản trị viên cập nhật trạng thái học tập của sinh viên, hệ thống tự động ghi nhận một bản ghi lịch sử:
- `id` (BIGINT, PK): Định danh bản ghi lịch sử.
- `sinh_vien_id` (BIGINT, FK): Tham chiếu tới sinh viên.
- `trang_thai_truoc` (VARCHAR(30)): Trạng thái trước khi đổi (`DANG_HOC`, `BAO_LUU`, ...).
- `trang_thai_sau` (VARCHAR(30)): Trạng thái mới được áp dụng.
- `ngay_hieu_luc` (DATE): Ngày quyết định hoặc trạng thái có hiệu lực thi hành.
- `ghi_chu` (VARCHAR(500)): Số quyết định, lý do hoặc ghi chú của phòng đào tạo.
- `nguoi_thuc_hien_id` (BIGINT, FK): Tài khoản admin thực hiện thay đổi.
- `thoi_diem` (DATETIME): Thời điểm hệ thống ghi nhận thao tác.

### Kiểm soát Concurrency
Để đảm bảo hai yêu cầu cập nhật đồng thời từ các quản trị viên không làm sai lệch chuỗi lịch sử (lost updates / race conditions):
- Service sử dụng khóa ghi bi quan (Pessimistic Write Lock) trên thực thể sinh viên: `studentRepository.findByIdForUpdate(sinhVienId)` trong cùng transaction `@Transactional`.
- Ràng buộc kiểm tra tính hợp lệ: Không cho phép đổi trạng thái mới trùng với trạng thái hiện tại (`trangThaiMoi != trangThaiHienTai`).
- Ghi nhận Audit Log hành động `STUDENT_ACADEMIC_STATUS_CHANGED` kèm snapshot chi tiết `before`/`after` (trạng thái, ngày hiệu lực, lý do).

---

## 5. Danh sách API Endpoints & Quyền Hạn

| Phương thức | Đường dẫn API | Quyền hạn | Mô tả |
| :--- | :--- | :--- | :--- |
| `PATCH` | `/api/admin/sinh-vien/{id}/trang-thai-hoc-tap` | `QUAN_TRI` | Cập nhật trạng thái học tập của sinh viên kèm ngày hiệu lực và lý do. |
| `GET` | `/api/admin/sinh-vien/{id}/lich-su-trang-thai-hoc-tap` | `QUAN_TRI` | Tra cứu toàn bộ lịch sử biến động trạng thái của sinh viên. |
| `GET` | `/api/sinh-vien/me/lich-su-trang-thai-hoc-tap` | `SINH_VIEN` | Sinh viên tự xem lịch sử thay đổi trạng thái của chính mình. |
| `GET` | `/api/sinh-vien/me/ho-so-dao-tao` | `SINH_VIEN` | Trả về hồ sơ đào tạo tích hợp trường `trangThaiHocTap`. |

Sinh viên **không có quyền** gọi bất kỳ API nào để tự thay đổi trạng thái học tập của mình (đảm bảo bảo mật với `@PreAuthorize("hasRole('QUAN_TRI')")`).
