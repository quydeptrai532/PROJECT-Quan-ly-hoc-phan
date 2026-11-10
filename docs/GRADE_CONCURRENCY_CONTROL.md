# Kiểm soát Xung đột Chỉnh điểm (Grade Concurrency Control) - QLSV-95 (TASK-OP-08)

Tài liệu mô tả chi tiết cơ chế kiểm soát xung đột đồng thời khi cập nhật điểm trong hệ thống Quản lý Sinh viên (QLSV), ngăn chặn ghi đè âm thầm (lost update), bảo đảm tính toàn vẹn dữ liệu điểm và tích hợp chặt chẽ với quy trình duyệt/khóa bảng điểm.

---

## 1. Nguyên lý và Kiến trúc Concurrency Control

### 1.1. Optimistic Locking hai tầng (Two-Tier Optimistic Locking)

Hệ thống kết hợp kiểm soát xung đột ở hai cấp độ:
1. **Cấp độ Bản ghi Điểm cá nhân (`Grade.version`)**:
   - Sử dụng `@Version` của JPA / Hibernate với cột `version BIGINT NOT NULL DEFAULT 0` trong bảng `diem`.
   - Mỗi lần cập nhật điểm của một sinh viên (`diemQuaTrinh`, `diemCuoiKy`), JPA tự động so khớp và tăng `version`.
   - Bắt và chuyển đổi `ObjectOptimisticLockingFailureException` / `OptimisticLockException` thành phản hồi HTTP 409 Conflict.
2. **Cấp độ Bảng điểm Lớp học phần (`CourseClass.phienBanBangDiem`)**:
   - Trường `phienBanBangDiem` (Integer, default 0) trong bảng `lop_hoc_phan`.
   - Mỗi thao tác cập nhật (đơn lẻ, hàng loạt hoặc import Excel) đều gọi `lop.tangPhienBanBangDiem()` để phát hiện xung đột giữa các thao tác tác động lên cùng bảng điểm.

### 1.2. Quy tắc So khớp và Xử lý Xung đột
- **Phát hiện xung đột trước khi ghi**:
  - Request mang `version` (bản ghi điểm) và `phienBanBangDiem` (bảng điểm).
  - Nếu `request.version != existing.version`: Từ chối ngay bằng `ApiException.gradeConflict(...)`.
  - Nếu `request.phienBanBangDiem != lop.phienBanBangDiem`: Từ chối ngay bằng `ApiException.gradeConflict(...)`.
- **Đồng thời ở mức database**:
  - Nếu hai request cùng đọc một version và gửi tới cùng lúc, câu lệnh SQL `UPDATE ... WHERE id = ? AND version = ?` đảm bảo tối đa một transaction commit thành công. Request đến sau gặp version đã tăng, throw `ObjectOptimisticLockingFailureException`, và được `GlobalExceptionHandler` trả về HTTP 409 Conflict.
  - **Tuyệt đối không tự động retry ghi đè hay âm thầm merge dữ liệu.**

---

## 2. Các Endpoint và Luồng Xử lý được Bảo vệ

| Endpoint | Phương thức | Dữ liệu phiên bản gửi kèm | Cơ chế kiểm tra |
| :--- | :---: | :--- | :--- |
| `/api/diem/{dangKyId}` | PUT | `version`, `phienBanBangDiem` | Kiểm tra cả phiên bản bản ghi điểm và phiên bản bảng điểm; cập nhật tăng `version` và `phienBanBangDiem`. |
| `/api/giang-vien/lop-hoc-phan/{id}/diem` | PUT | `phienBanBangDiem`, danh sách `item.version` | Kiểm tra toàn bộ `item.version` và `phienBanBangDiem`; transaction rollback toàn bộ nếu có bất kỳ xung đột nào. |
| `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview` | POST | Tệp Excel | Trả về `phienBanBangDiem` tại thời điểm xem trước và lưu vào `PreviewSession`. |
| `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm` | POST | `previewToken`, `phienBanBangDiem` | Kiểm tra `phienBanBangDiem` của request và của phiên xem trước với bảng điểm thực tế; từ chối nếu bảng điểm đã đổi trong quá trình preview. |
| `/api/giang-vien/lop-hoc-phan/{id}/*` (Duyệt/Khóa) | POST | Quy trình QLSV-74 | Ngăn chặn mọi hành động chỉnh sửa khi bảng điểm ở trạng thái `CHO_DUYET`, `DA_DUYET`, `DA_CONG_BO`, `DA_KHOA` bất kể version có khớp. |

---

## 3. Định dạng Phản hồi Xung đột (HTTP 409 Conflict)

Khi phát hiện xung đột phiên bản, hệ thống trả về mã trạng thái HTTP 409 với cấu trúc chuẩn:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Điểm của sinh viên đã bị thay đổi bởi người khác (phiên bản hiện tại: 2). Vui lòng tải lại và đối chiếu.",
  "path": "/api/diem/105",
  "timestamp": "2026-11-09T23:50:00Z",
  "currentVersion": 2,
  "currentPhienBanBangDiem": 5
}
```

UI sử dụng `currentVersion` và `currentPhienBanBangDiem` để hiển thị thông tin cảnh báo, ngăn chặn gửi đè và cung cấp nút bấm "Tải lại & đối chiếu" cho giảng viên.

---

## 4. Trải nghiệm Người dùng (Frontend UX)

- **Cảnh báo xung đột trực quan**:
  - Khi API trả về 409, `GradeEntryPage` hiển thị banner cảnh báo màu vàng/cam nổi bật: "Xung đột dữ liệu chỉnh điểm (HTTP 409 Conflict)".
  - Thông báo nêu rõ dữ liệu trên màn hình đã cũ hơn hệ thống do người khác vừa cập nhật.
- **Nút "Tải lại & đối chiếu"**:
  - Reset trạng thái dirty (`edits`), xóa lỗi trường và gọi lại `reloadAll()` để nạp bản ghi mới nhất kèm version mới.
- **Import Excel Modal**:
  - Nếu bảng điểm bị thay đổi trong lúc giảng viên đang xem lại file preview, khi bấm xác nhận import, modal bắt mã 409 và hiển thị thông báo lỗi yêu cầu tải lại và xem trước lại, không tiến hành ghi đè.

---

## 5. Nhật ký Kiểm toán (Audit Logging) và Bảo mật

- **Chi tiết lưu trữ**:
  - Ghi nhận `versionCu`, `versionMoi`, `phienBanBangDiemCu`, `phienBanBangDiemMoi`, `diemQuaTrinhCu`, `diemCuoiKyCu`, `diemTongKetCu`, `diemQuaTrinhMoi`, `diemCuoiKyMoi`, `diemTongKetMoi`.
  - Ghi nhận `nguoiThucHienId`, username, IP và thời điểm thực hiện.
- **Bảo mật và tính bất biến**:
  - Danh mục `SAFE_DETAIL_FIELDS` trong `AuditLogService` loại trừ các thông tin nhạy cảm.
  - Các endpoint xem audit log yêu cầu quyền `QUAN_TRI`. Giảng viên và sinh viên bị cấm truy cập (HTTP 403 Forbidden).
  - Không tồn tại endpoint chỉnh sửa hoặc xóa lịch sử audit, đảm bảo tính toàn vẹn và khả năng truy vết pháp lý.

---

## 6. Migration Dữ liệu An toàn

- Cột `version` được bổ sung vào bảng `diem`:
  ```sql
  ALTER TABLE diem ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
  ```
- Dữ liệu hiện có mặc định mang giá trị `version = 0`, đảm bảo tương thích ngược 100% không làm gián đoạn hệ thống.
