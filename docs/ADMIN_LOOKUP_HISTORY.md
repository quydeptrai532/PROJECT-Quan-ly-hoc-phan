# Tra cứu lịch sử học tập và giảng dạy quản trị (TASK-AD-08 / FR-AD-014)

Trang: `/quan-tri/tra-cuu-lich-su` (menu "Tra cứu lịch sử"). Chỉ vai trò `QUAN_TRI` (kiểm tra bằng `@PreAuthorize` ở backend).

## API (`/api/admin/tra-cuu`)

| Endpoint | Mô tả |
|---|---|
| `GET /sinh-vien?keyword&page&size` | Tìm sinh viên theo mã/họ tên (không phân biệt hoa thường) |
| `GET /sinh-vien/{id}/lich-su?mucDich&hocKyId&trangThai&page&size` | Lịch sử học tập |
| `GET /giang-vien?keyword&page&size` | Tìm giảng viên theo mã/họ tên |
| `GET /giang-vien/{id}/lich-su?mucDich&hocKyId&page&size` | Lịch sử các lớp giảng dạy |

- Phân trang: `page >= 0`, `size` 1–100, ngược lại `400`. Danh sách tìm kiếm sắp xếp `ma, id`; lịch sử sắp xếp cố định theo học kỳ mới nhất trước, rồi mã môn/lớp, rồi id → phân trang ổn định.
- `mucDich` bắt buộc (5–200 ký tự) khi xem lịch sử chi tiết; thiếu/sai → `400`.
- `hocKyId` không hợp lệ → `400`, không tồn tại → `404`. `trangThai` (sinh viên): `DAT|CHUA_DAT|CHUA_CO_DIEM`.
- Không tồn tại sinh viên/giảng viên → `404`. Danh sách rỗng trả `items: []` (giao diện hiển thị trạng thái rỗng).
- Response chỉ gồm các DTO riêng (`AdminLookupDtos`): không có mật khẩu, token hay trường của `User`.

## Dữ liệu lịch sử

- Sinh viên: giữ cả đăng ký đã hủy (`trangThaiDangKy = DA_HUY`) và lớp đã hủy (`lopDaHuy`, `lyDoHuyLop`); các lần học lại là các bản ghi riêng. Điểm chỉ hiển thị khi đăng ký còn hiệu lực, bảng điểm đã công bố và có điểm tổng kết (cùng quy tắc với TASK-ST-05).
- Giảng viên: gồm lớp `DANG_MO`, `DA_KET_THUC` (học kỳ đã qua) và `DA_HUY`, kèm sĩ số đang học.

## Audit

Mỗi lần tra cứu/xem ghi `AuditLog` (cả thất bại, qua `AuditLogService.execute`):
`ADMIN_LOOKUP_STUDENT_SEARCH`, `ADMIN_LOOKUP_LECTURER_SEARCH`, `ADMIN_VIEW_STUDENT_HISTORY`, `ADMIN_VIEW_LECTURER_HISTORY`. Chi tiết lưu `mucDich`, `hocKyId`, `soBanGhi`, mã sinh viên/giảng viên hoặc từ khóa (các trường nằm trong danh sách an toàn, không lưu điểm).
