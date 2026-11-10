# Tài liệu Kỹ thuật: Cảnh báo và Kiểm soát Xung đột Lịch học khi Mở lớp (QLSV-78 - TASK-AD-03)

## 1. Tổng quan & Mục tiêu

- **Mã công việc:** QLSV-78 (TASK-AD-03)
- **Mục tiêu:** Phát hiện và cảnh báo xung đột lịch giảng viên và phòng học khi quản trị viên tạo mới hoặc cập nhật lớp học phần (`CourseClass`), ngăn ngừa tình trạng một phòng hoặc một giảng viên bị xếp trùng lịch trong cùng khung giờ học kỳ.
- **Nguyên tắc thiết kế:**
  - Tái sử dụng triệt để mô hình dữ liệu thực tế sẵn có (`CourseClass`, `thu`, `tietBatDau`, `soTiet`, `phong`, `giangVienId`, `hocKyId`). Tuyệt đối không tạo định dạng lịch học thứ hai.
  - Đồng nhất thuật toán xác định xung đột giữa Backend và Frontend (UI gọi API preview kiểm tra của backend, không tự chạy thuật toán tính toán riêng rẽ ở client-side).
  - Kiểm tra ở cả luồng tạo mới (`POST /api/admin/lop-hoc-phan`) và cập nhật (`PUT /api/admin/lop-hoc-phan/{id}`).
  - Khi cập nhật, loại trừ chính lớp đang sửa (`boQuaId`) để tránh tự báo xung đột với chính nó.
  - Kiểm soát chính sách phân quyền lưu: logic kiểm tra được tách độc lập (`ClassScheduleConflictService`), cho phép cấu hình linh hoạt chính sách xử lý (`EnforcementPolicy`).

---

## 2. Thuật toán Giao thoa Thời gian (Time Intersection Algorithm)

### 2.1. Điều kiện tiên quyết
Chỉ so sánh các lớp học phần thỏa mãn đồng thời:
1. Cùng học kỳ (`lop.hocKy.id == req.hocKyId`).
2. Cùng thứ trong tuần (`lop.thu == req.thu`, giá trị 2..8 với 8 là Chủ nhật).
3. Khác mã ID của lớp đang sửa (nếu là cập nhật: `lop.id != boQuaId`).

### 2.2. Quy tắc giao thoa khoảng tiết
Một buổi học được xác định bởi:
- Tiết bắt đầu: $t_{\text{start}}$
- Số tiết: $n$ ($1 \le n \le 6$)
- Tiết kết thúc: $t_{\text{end}} = t_{\text{start}} + n - 1$ ($1 \le t_{\text{end}} \le 12$)

Hai khoảng tiết $[A_{\text{start}}, A_{\text{end}}]$ và $[B_{\text{start}}, B_{\text{end}}]$ giao nhau khi và chỉ khi:
$$\text{Giao nhau} \iff (A_{\text{start}} \le B_{\text{end}}) \land (B_{\text{start}} \le A_{\text{end}})$$

### 2.3. Quy tắc các tiết liền kề
- Nếu lớp A học tiết 1..3 ($A = [1, 3]$) và lớp B học tiết 4..6 ($B = [4, 6]$):
  - $A_{\text{start}} \le B_{\text{end}} \implies 1 \le 6$ (Đúng)
  - $B_{\text{start}} \le A_{\text{end}} \implies 4 \le 3$ (**Sai**)
  - Kết quả: **Không giao nhau** (hai lớp kết thúc và bắt đầu liền kề mà không dùng chung tiết thì **không** được xem là trùng).
- Nếu lớp A học tiết 2..4 ($A = [2, 4]$) và lớp B học tiết 4..6 ($B = [4, 6]$):
  - $A_{\text{start}} \le B_{\text{end}} \implies 2 \le 6$ (Đúng)
  - $B_{\text{start}} \le A_{\text{end}} \implies 4 \le 4$ (Đúng)
  - Kết quả: **Giao nhau tại tiết 4**.

### 2.4. Xác định khoảng tiết giao thoa
Khoảng tiết giao thoa được trả về cụ thể cho quản trị viên xử lý:
$$t_{\text{giao bắt đầu}} = \max(A_{\text{start}}, B_{\text{start}})$$
$$t_{\text{giao kết thúc}} = \min(A_{\text{end}}, B_{\text{end}})$$

---

## 3. Phân loại Xung đột Lịch học

1. **Xung đột Phòng học (`loaiXungDot: "PHONG"`):**
   - Điều kiện: Cả hai lớp có cùng chuỗi phòng học (so sánh chuẩn hóa `trim().toLowerCase()`).
   - Mô tả: `Trùng phòng {phong} với lớp {maLop} ({tenMon}) vào Thứ {thu}, tiết {giao}`.
2. **Xung đột Giảng viên (`loaiXungDot: "GIANG_VIEN"`):**
   - Điều kiện: Cả hai lớp do cùng một giảng viên đảm nhiệm (`lop.giangVien.id == req.giangVienId`).
   - Mô tả: `Giảng viên {tenGiangVien} đã có lịch phụ trách lớp {maLop} ({tenMon}) vào Thứ {thu}, tiết {giao}`.

---

## 4. Chính sách Xử lý & Thông báo Chủ Dự án (Enforcement Policy)

### 4.1. Thực trạng & Quyết định Chờ Chủ Dự án
Chính sách giữa **"Cảnh báo nhưng cho lưu"** (`WARN_ONLY`), **"Cho phép lưu khi có xác nhận ghi đè"** (`ALLOW_OVERRIDE_WITH_CONFIRMATION`) và **"Chặn lưu tuyệt đối khi có xung đột"** (`BLOCK_ON_CONFLICT`) chưa được chốt văn bản quy chế chính thức trong hệ thống đào tạo.

Theo yêu cầu nghiệp vụ:
> "Nếu không có cấu hình/quy ước sẵn, triển khai trả kết quả xung đột và không tự cho phép lưu trái quy định; ghi rõ chính sách cần chủ dự án quyết định. Tách logic kiểm tra để có thể cấu hình enforcement sau này."

### 4.2. Cấu hình triển khai (`application.yml`)
```yaml
app:
  class-schedule-conflict:
    enforcement-policy: BLOCK_ON_CONFLICT
    official-regulation: false
    policy-note: "Chính sách mặc định áp dụng BLOCK_ON_CONFLICT để bảo vệ tính toàn vẹn thời khóa biểu; chờ Chủ dự án phê duyệt quy định chính thức nếu muốn hỗ trợ cơ chế ghi đè hoặc chỉ cảnh báo."
```

- **Mặc định:** `BLOCK_ON_CONFLICT` — Khi có xung đột phòng hoặc giảng viên, backend trả về lỗi HTTP 409 Conflict và từ chối lưu.
- **Frontend UI:** Hiển thị trực quan thẻ cảnh báo xung đột (gồm số lượng, loại xung đột, danh sách lớp liên quan, tiết giao thoa) và vô hiệu hóa nút Lưu kèm ghi chú trạng thái chính sách.

---

## 5. API Reference

### 5.1. Preview kiểm tra xung đột thời gian thực
- **Endpoint:** `POST /api/admin/lop-hoc-phan/kiem-tra-xung-dot`
- **Request Body:**
```json
{
  "lopHocPhanId": 12,
  "hocKyId": 1,
  "monHocId": 5,
  "giangVienId": 2,
  "phong": "A101",
  "thu": 2,
  "tietBatDau": 2,
  "soTiet": 3
}
```
- **Response:**
```json
{
  "coXungDot": true,
  "soLuongXungDot": 1,
  "xungDotGiangVien": false,
  "xungDotPhong": true,
  "danhSachXungDot": [
    {
      "loaiXungDot": "PHONG",
      "moTa": "Trùng phòng A101 với lớp INT201.01 (Lập trình Java) vào Thứ 2, tiết 4",
      "lopLienQuanId": 1,
      "maLopLienQuan": "INT201.01",
      "tenMonLienQuan": "Lập trình Java",
      "phong": "A101",
      "thu": 2,
      "tietBatDau": 4,
      "soTiet": 3,
      "tietKetThuc": 6,
      "giangVienId": 3,
      "tenGiangVien": "Trần Văn B",
      "tietGiaoBatDau": 4,
      "tietGiaoKetThuc": 4
    }
  ],
  "moTaCanhBao": [
    "Trùng phòng A101 với lớp INT201.01 (Lập trình Java) vào Thứ 2, tiết 4"
  ],
  "chinhSachEnforcement": "BLOCK_ON_CONFLICT",
  "choPhepLuu": false,
  "chinhSachDaChot": false,
  "ghiChuChinhSach": "Chính sách mặc định áp dụng BLOCK_ON_CONFLICT để bảo vệ tính toàn vẹn thời khóa biểu; chờ Chủ dự án phê duyệt quy định chính thức nếu muốn hỗ trợ cơ chế ghi đè hoặc chỉ cảnh báo."
}
```

### 5.2. Tạo lớp học phần
- **Endpoint:** `POST /api/admin/lop-hoc-phan`
- **Ngoại lệ:** Trả về HTTP 409 Conflict kèm chi tiết nếu lịch học bị xung đột theo chính sách `BLOCK_ON_CONFLICT`.

### 5.3. Cập nhật lớp học phần
- **Endpoint:** `PUT /api/admin/lop-hoc-phan/{id}`
- **Ngoại lệ:** Tự động bỏ qua so sánh với chính `id` của lớp đang sửa; ném HTTP 409 Conflict nếu xung đột với lớp khác.

---

## 6. Kiểm thử tự động (Integration Testing)

Bộ kiểm thử tích hợp `ClassScheduleConflictIntegrationTest` bao phủ toàn diện:
1. `adjacentPeriods_DoNotConflict`: Tiết 1..3 và 4..6 liền kề cùng thứ/phòng không xung đột.
2. `roomConflict_DetectedWithIntersection`: Tiết 2..4 và 4..6 cùng phòng báo trùng phòng tại tiết 4.
3. `lecturerAndRoomConflict_BothDetected`: Trùng cả phòng và giảng viên cùng lúc.
4. `editingClass_DoesNotConflictWithItself`: Cập nhật lớp với chính ID của mình không tự báo xung đột.
5. `backendBlockOnConflict_ThrowsConflictException`: Gọi API tạo lớp khi có xung đột bị backend chặn và ném HTTP 409 Conflict.
