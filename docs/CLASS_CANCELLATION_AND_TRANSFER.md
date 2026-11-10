# QLSV-79 (TASK-AD-04): HỦY LỚP HỌC PHẦN VÀ CHUYỂN SINH VIÊN SANG LỚP KHÁC

## 1. Tổng quan
Tài liệu này mô tả chi tiết giải pháp kỹ thuật, cơ chế giao dịch/khóa đồng thời (transaction/locking), chính sách an toàn điểm số, kiểm soát toàn vẹn dữ liệu, audit log và thông báo đa kênh cho tính năng **QLSV-79 — TASK-AD-04: Hủy lớp học phần và chuyển sinh viên sang lớp khác** trong hệ thống Quản lý Đào tạo (QLSV).

---

## 2. Quy trình & Nguyên tắc cốt lõi

### 2.1. Hủy lớp học phần (`HuyLopHocPhan`)
1. **Khảo sát tác động trước khi hủy (`khaoSatHuyLop`):**
   - Admin thực hiện pre-flight khảo sát tác động: hiển thị sĩ số sinh viên bị ảnh hưởng, trạng thái bảng điểm, danh sách sinh viên cụ thể và gợi ý danh sách lớp đích khả dĩ (cùng môn hoặc môn tương đương còn chỗ trong học kỳ).
2. **Không xóa dữ liệu lịch sử:**
   - Tuyệt đối không xóa bản ghi `lop_hoc_phan`, `dang_ky_hoc_phan`, `lich_su_dang_ky` hay `diem`.
   - Lớp nguồn được đánh dấu `da_huy = true`, lưu vết `thoi_gian_huy`, `ly_do_huy`, `nguoi_huy_id`, `nguoi_huy_username`.
   - Toàn bộ đăng ký `DA_DANG_KY` của lớp nguồn được chuyển sang trạng thái `DA_HUY`, ghi nhận bản ghi `RegistrationHistory` tương ứng.
3. **Chặn đăng ký mới:**
   - Phương thức đăng ký học phần của sinh viên kiểm tra ngay `lop.isDaHuy()`. Nếu lớp đã hủy, hệ thống từ chối ngay lập tức với thông báo `400 Bad Request`.

### 2.2. Chuyển sinh viên sang lớp khác (`ChuyenSinhVien`)
1. **Kiểm tra tính hợp lệ của lớp đích:**
   - Lớp đích phải cùng học kỳ với lớp nguồn.
   - Lớp đích chưa bị hủy (`!lopDich.isDaHuy()`).
   - Lớp đích phải có cùng môn học hoặc thuộc danh mục **môn tương đương** (`TUONG_DUONG`) được cấu hình trong bảng `mon_tien_quyet`.
2. **Kiểm tra điều kiện của từng sinh viên:**
   - **Sức chứa (Capacity):** Lớp đích còn chỗ trống (`siSoHienTai < siSoToiDa`).
   - **Trùng lịch học (Schedule Conflict):** Sinh viên không có bất kỳ lớp học phần nào khác trong cùng học kỳ bị trùng thứ và khung tiết học với lớp đích.
   - **Môn học đã đăng ký:** Sinh viên chưa đăng ký môn học đích ở một lớp khác trong cùng kỳ.
   - **Môn tiên quyết (Prerequisite):** Sinh viên đã hoàn thành và đạt các môn tiên quyết bắt buộc của môn học đích.
   - **An toàn điểm số:** Sinh viên chưa có điểm quá trình/cuối kỳ tại lớp nguồn.

---

## 3. Cơ chế Transaction & Concurrency Control (Khóa chống vượt sĩ số & Tránh Deadlock)

### 3.1. Chống vượt sĩ số (Race Condition Prevention)
Khi nhiều admin hoặc tiến trình thực hiện chuyển sinh viên/đăng ký vào lớp đích đồng thời, hệ thống sử dụng **Pessimistic Write Lock (`PESSIMISTIC_WRITE`)**:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM CourseClass c WHERE c.id = :id")
Optional<CourseClass> findByIdForUpdate(@Param("id") Long id);
```
Câu lệnh sinh ra trong cơ sở dữ liệu: `SELECT ... FROM lop_hoc_phan WHERE id = ? FOR UPDATE`.

### 3.2. Chống Deadlock hai chiều (Deadlock Avoidance Pattern)
Trong các kịch bản chuyển sinh viên chéo giữa hai lớp đồng thời (Ví dụ: Request A chuyển SV từ Lớp 1 sang Lớp 2; Request B chuyển SV từ Lớp 2 sang Lớp 1), việc khóa tùy tiện sẽ gây ra vòng tròn chờ tài nguyên (deadlock).
Hệ thống giải quyết triệt để bằng cách luôn khóa tài nguyên theo thứ tự tăng dần cố định của ID:
```java
Long firstLockId = Math.min(lopNguonId, lopDichId);
Long secondLockId = Math.max(lopNguonId, lopDichId);

CourseClass firstLocked = lopHocPhanRepository.findByIdForUpdate(firstLockId).orElseThrow(...);
CourseClass secondLocked = lopHocPhanRepository.findByIdForUpdate(secondLockId).orElseThrow(...);

CourseClass lopNguon = (firstLockId.equals(lopNguonId)) ? firstLocked : secondLocked;
CourseClass lopDich = (firstLockId.equals(lopDichId)) ? firstLocked : secondLocked;
```
Nhờ đó, mọi transaction luôn tuân theo một thứ tự khóa duy nhất, ngăn chặn hoàn toàn deadlock ở mức database engine.

### 3.3. Tính nguyên tử (Atomicity)
Mỗi thao tác chuyển sinh viên đơn lẻ được bao bọc trong transaction độc lập `@Transactional(isolation = Isolation.READ_COMMITTED)`. Nếu có bất kỳ lỗi nào xảy ra khi ghi nhận vào lớp đích, transaction sẽ rollback toàn bộ: trạng thái đăng ký của sinh viên tại lớp nguồn giữ nguyên vẹn, sĩ số lớp nguồn và đích không bị suy hao.

---

## 4. Chính sách an toàn đối với lớp/sinh viên đã nhập điểm

### 4.1. Quy định bảo vệ an toàn dữ liệu điểm
Hệ thống áp dụng chính sách bảo toàn nghiêm ngặt:
- Nếu lớp học phần **đã có điểm** (tồn tại bản ghi điểm quá trình, kiểm tra hoặc thi cuối kỳ) hoặc bảng điểm **không ở trạng thái NHÁP** (`trangThaiBangDiem != NHAP` như `CHO_DUYET`, `DA_DUYET`, `DA_CONG_BO`, `DA_KHOA`):
  1. Thao tác hủy lớp bị **CHẶN** với mã lỗi `409 Conflict`.
  2. Thao tác chuyển sinh viên đã có điểm tại lớp nguồn bị **CHẶN** với mã lỗi `409 Conflict`.
  3. Giao diện người dùng hiển thị cảnh báo đỏ và khóa các nút xác nhận tương ứng.

### 4.2. Danh mục quyết định cần chốt với Chủ dự án / Trưởng đơn vị đào tạo
Khi có nhu cầu hủy lớp hoặc chuyển sinh viên đối với lớp đã phát sinh dữ liệu điểm, các quyết định sau đây cần được chốt bằng văn bản quy chế chính thức:
1. **Phương án bảo lưu điểm quá trình:**
   - Sinh viên chuyển sang lớp đích cùng môn có được tự động kế thừa điểm thành phần/quá trình đã đạt ở lớp nguồn không?
   - Giảng viên lớp đích có quyền quyết định công nhận hay yêu cầu làm lại các bài kiểm tra đánh giá quá trình?
2. **Quy định đối với môn tương đương:**
   - Trường hợp chuyển sang môn tương đương có số tín chỉ hoặc cơ cấu điểm khác (VD: bài tập lớn vs. thi tự luận), điểm số chuyển đổi theo hệ số hay hủy kết quả cũ?
3. **Quy trình phê duyệt ngoại lệ:**
   - Nếu buộc phải hủy lớp đã nhập điểm vì lý do bất khả kháng, có cần quy trình phê duyệt cấp Trưởng phòng Đào tạo kèm biên bản xác nhận trước khi hệ thống mở khóa hủy?

---

## 5. Audit Logging & Notification Center

### 5.1. Bảng lịch sử chuyển lớp (`ClassTransferHistory`)
Mọi giao dịch chuyển lớp thành công được ghi nhận vĩnh viễn vào bảng `lich_su_chuyen_lop`:
- `lop_nguon_id`, `lop_dich_id`: Lớp nguồn và lớp đích.
- `sinh_vien_id`: Sinh viên được chuyển.
- `nguoi_thuc_hien_id`, `nguoi_thuc_hien_username`: Người quản trị thực hiện.
- `thoi_gian_chuyen`: Thời điểm giao dịch hoàn tất.
- `loai_quan_he_mon`: Phân loại `CUNG_MON` hoặc `TUONG_DUONG`.
- `ly_do`: Lý do chuyển lớp.

### 5.2. Audit Log hệ thống
- Tích hợp `AuditLogService` ghi lại hành động `HUY_LOP_HOC_PHAN`, `CHUYEN_LOP_HOC_PHAN`, `CHUYEN_LOP_HANG_LOAT`.
- Cấu hình whitelist trường dữ liệu an toàn `SAFE_DETAIL_FIELDS`: bổ sung `daHuy`, `thoiGianHuy`, `lopNguonId`, `lopDichId`, `soLuongChuyen`, `soDangKyBiHuy`.

### 5.3. Trung tâm thông báo (Notification Center)
- Sự kiện `CLASS_STUDENT_TRANSFERRED` gửi thông báo tự động tới:
  1. **Sinh viên:** Thông báo chi tiết lớp học cũ, lớp học mới, lịch học và phòng học mới.
  2. **Giảng viên lớp nguồn:** Cập nhật biến động giảm sĩ số lớp do điều chuyển.
  3. **Giảng viên lớp đích:** Nhắc nhở danh sách tiếp nhận sinh viên mới chuyển đến.
- **Idempotency Key chống gửi trùng khi retry:**
  Khóa định danh tất cả thông báo theo cấu trúc:
  `transfer-class:{lopNguonId}->{lopDichId}:student:{sinhVienId}:target:{userId}:{occurredAtEpochMilli}`

---

## 6. Danh sách REST API Backend

| Phương thức | Đường dẫn API | Phân quyền | Mục đích |
|---|---|---|---|
| `GET` | `/api/admin/lop-hoc-phan/{id}/khao-sat-huy` | `ADMIN` | Khảo sát tác động hủy lớp: sĩ số, cảnh báo điểm, gợi ý lớp đích |
| `POST` | `/api/admin/lop-hoc-phan/{id}/huy` | `ADMIN` | Hủy lớp học phần an toàn, chuyển đăng ký sang `DA_HUY` |
| `POST` | `/api/admin/lop-hoc-phan/{id}/kiem-tra-chuyen` | `ADMIN` | Pre-flight kiểm tra điều kiện chuyển sinh viên (sức chứa, trùng lịch, tiên quyết) |
| `POST` | `/api/admin/lop-hoc-phan/{id}/chuyen-sinh-vien` | `ADMIN` | Chuyển 1 sinh viên sang lớp đích (atomic, pessimistic lock) |
| `POST` | `/api/admin/lop-hoc-phan/{id}/chuyen-hang-loat` | `ADMIN` | Chuyển danh sách nhiều sinh viên sang lớp đích |
| `GET` | `/api/admin/lop-hoc-phan/{id}/lich-su-chuyen` | `ADMIN` | Xem lịch sử chuyển lớp của một lớp học phần |
| `GET` | `/api/admin/lop-hoc-phan/lich-su-chuyen` | `ADMIN` | Tra cứu toàn bộ lịch sử chuyển lớp theo học kỳ |

---

## 7. Giao diện Frontend (`CourseClassPage.jsx`)
1. **Huy hiệu lớp đã hủy:** Cột mã lớp hiển thị Badge `Đã hủy` màu đỏ kèm tooltip thời gian và lý do hủy. Khóa các thao tác sửa/xóa/hủy tiếp theo.
2. **Modal Khảo sát tác động & Hủy lớp:** Hiển thị thông tin lớp, cảnh báo điểm số (khóa nút nếu đã có điểm), bảng sinh viên ảnh hưởng, danh sách lớp đích và nút chuyển SV nhanh.
3. **Modal Chuyển sinh viên sang lớp khác:** Cho phép chọn lớp đích, chọn sinh viên theo danh sách (hoặc chọn tất cả), bấm nút "Kiểm tra điều kiện" để xem trước tính hợp lệ của từng sinh viên, nhập lý do và bấm "Thực hiện chuyển".
4. **Modal Lịch sử chuyển lớp:** Cho phép xem lịch sử chuyển cụ thể của từng lớp hoặc toàn bộ học kỳ.
