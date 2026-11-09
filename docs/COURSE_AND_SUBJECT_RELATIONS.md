# Quản lý Khóa học / Niên khóa và Quan hệ giữa các môn học (`QLSV-76 — TASK-AD-01`)

## 1. Tổng quan

Tài liệu này mô tả thiết kế mô hình dữ liệu, API phân quyền, kiểm tra hợp lệ và phạm vi quy chế áp dụng cho danh mục **Khóa học / Niên khóa**, **Môn học** và **Quan hệ giữa các môn học** trong hệ thống QLSV.

---

## 2. Mô hình dữ liệu đã bổ sung / mở rộng

### 2.1. Khóa học / Niên khóa (`AcademicCohort` — bảng `khoa_dao_tao`)
- `ma` (`unique`, bắt buộc, VD: `K2024`, `K2025`, `K2026`).
- `ten` (bắt buộc, VD: `Khóa 2024`).
- `nienKhoa` (bắt buộc, VD: `2024-2028`).
- `namBatDau`, `namKetThuc` (validate `namKetThuc >= namBatDau`).
- `hocKyBatDau`, `hocKyKetThuc` (liên kết `Semester`, validate thứ tự thời gian học kỳ).
- `hieuLucTu`, `hieuLucDen`, `dangHieuLuc` (thời gian hiệu lực đào tạo).
- `moTa`.
- Metadata người tạo / cập nhật: `nguoiTaoId`, `nguoiTaoUsername`, `taoLuc`, `nguoiCapNhatId`, `nguoiCapNhatUsername`, `capNhatLuc`.

### 2.2. Môn học (`Subject` — bảng `mon_hoc`)
- Giữ nguyên quy tắc tín chỉ và tiết lý thuyết (`1 TC = 15 tiết lý thuyết = 5 buổi`).
- Bổ sung metadata người tạo / cập nhật: `nguoiTaoId`, `nguoiTaoUsername`, `taoLuc`, `nguoiCapNhatId`, `nguoiCapNhatUsername`, `capNhatLuc`.

### 2.3. Quan hệ giữa các môn học (`PrerequisiteSubject` — bảng `mon_tien_quyet`, enum `SubjectRelationType`)
- Hỗ trợ 5 loại quan hệ (`loaiQuanHe`):
  1. `TIEN_QUYET` (Tiên quyết)
  2. `SONG_HANH` (Song hành)
  3. `TUONG_DUONG` (Tương đương)
  4. `THAY_THE` (Thay thế)
  5. `NHOM_TU_CHON` (Nhóm tự chọn)
- Thuộc tính điều kiện đạt & tự chọn:
  - `dieuKienDat` (thang 10, mặc định `5.0`, validate trong khoảng `0.0 - 10.0`).
  - `yeuCauDat` (`boolean`, mặc định `true` đối với các quan hệ yêu cầu đạt điểm tổng kết đã công bố, `false` đối với `SONG_HANH` nếu chỉ yêu cầu học song song).
  - `maNhomTuChon` (bắt buộc khi `loaiQuanHe = NHOM_TU_CHON`) và `tinChiTuChonYeuCau`.
- Thuộc tính phạm vi hiệu lực:
  - `khoaNhapHoc` (giới hạn theo khóa nhập học, hoặc `null` nếu áp dụng toàn trường).
  - `hocKyHieuLucTu`, `hocKyHieuLucDen` (giới hạn theo đợt/học kỳ hiệu lực).
  - `hieuLucTu`, `hieuLucDen`, `dangHieuLuc` (giới hạn theo khoảng ngày và cờ hiệu lực).
  - `ghiChu`.
- Metadata người tạo / cập nhật: `nguoiTaoId`, `nguoiTaoUsername`, `taoLuc`, `nguoiCapNhatId`, `nguoiCapNhatUsername`, `capNhatLuc`.

---

## 3. Ràng buộc toàn vẹn & Bảo vệ dữ liệu

1. **Chống tự tham chiếu (`400 Bad Request`)**:
   - Không cho phép `monHocId == monLienQuanId` ở bất kỳ loại quan hệ nào.
2. **Chống chu trình trong đồ thị môn tiên quyết (`400 Bad Request`)**:
   - Khi thêm mới hoặc cập nhật một quan hệ sang loại `TIEN_QUYET`, hệ thống duyệt BFS trên đồ thị có hướng các cạnh `TIEN_QUYET` để phát hiện mọi chu trình trực tiếp (`A <-> B`) hoặc gián tiếp nhiều bước (`A -> B -> C -> A`).
3. **Bảo vệ xóa khi đang được sử dụng (`409 Conflict`)**:
   - Không cho phép xóa quan hệ môn học (`PrerequisiteSubject`) nếu môn chính/môn liên quan đang được tham chiếu trong **Chương trình đào tạo (`Curriculum`)** hoặc **Lớp học phần (`CourseClass`)** thuộc phạm vi khóa/học kỳ hiệu lực.
   - Không cho phép xóa **Khóa học / Niên khóa (`AcademicCohort`)** nếu đang được tham chiếu bởi Chương trình đào tạo, Quan hệ môn học, Lớp hành chính hoặc Sinh viên.
   - Khi từ chối xóa, trả về HTTP `409 Conflict` kèm thông báo tiếng Việt rõ ràng và giữ nguyên toàn bộ dữ liệu liên quan.
4. **Ghi vết Audit Log (`AuditLogService`)**:
   - Tự động ghi nhận sự kiện máy chủ cho `COHORT_CREATED`, `COHORT_UPDATED`, `COHORT_DELETED`, `COURSE_CREATED`, `COURSE_UPDATED`, `COURSE_DELETED`, `PREREQUISITE_ADDED`, `SUBJECT_RELATION_UPDATED`, `PREREQUISITE_REMOVED`.

---

## 4. Quy chế áp dụng khi kiểm tra đăng ký & Các phần chờ Chủ dự án chốt

| Loại quan hệ | Trạng thái quy chế | Cách xử lý hiện tại trong `RegistrationService` & `GET /api/dang-ky/kiem-tra-quan-he` | Phần cần Chủ dự án / Phòng Đào tạo chốt trên Jira |
| :--- | :--- | :--- | :--- |
| **`TIEN_QUYET` (Tiên quyết)** | **Đã chốt quy chế** (theo `README.md`) | **Chặn đăng ký học phần** nếu sinh viên chưa có điểm tổng kết đã công bố (`DA_CONG_BO` / `DA_KHOA`) đạt `>= dieuKienDat` (mặc định `5.0`) đối với các quan hệ đang trong thời gian/học kỳ/khóa hiệu lực. | Đã chốt. Có thể chốt thêm trường hợp đặc cách học trước nếu Phòng Đào tạo yêu cầu sau này. |
| **`SONG_HANH` (Song hành)** | **Chờ chốt quy chế** | Lưu cấu hình có cấu trúc; đối chiếu xem sinh viên đã đạt môn song hành ở kỳ trước hoặc đang đăng ký môn song hành trong cùng học kỳ hay chưa; hiển thị rõ trên API `kiem-tra-quan-he`, **không tự ý chặn cứng**. | Cần chốt: (1) Bắt buộc đăng ký cùng học kỳ hay cho phép đã học trước nhưng chưa đạt? (2) Khi hủy môn song hành có tự động hủy môn chính hay chỉ cảnh báo? |
| **`TUONG_DUONG` (Tương đương)** | **Chờ chốt quy chế** | Lưu cấu hình có cấu trúc; đối chiếu điểm tổng kết đã công bố của môn tương đương; trả về cảnh báo và trạng thái trên `kiem-tra-quan-he`, **không tự động quy đổi thay thế môn bắt buộc/tiên quyết**. | Cần chốt: (1) Môn tương đương có tự động thỏa mãn điều kiện tiên quyết thay cho môn gốc không? (2) Quy tắc quy đổi điểm khi số tín chỉ hai môn khác nhau? |
| **`THAY_THE` (Thay thế)** | **Chờ chốt quy chế** | Lưu cấu hình có cấu trúc theo khóa và đợt hiệu lực; đối chiếu kết quả đạt của môn thay thế; hiển thị trong kết quả kiểm tra quan hệ, **không tự động ghi đè học phần gốc**. | Cần chốt: (1) Môn thay thế áp dụng cho mọi khóa hay chỉ các khóa ngừng mở môn cũ? (2) Cách tính GPA tích lũy khi học môn thay thế cho môn đã trượt? |
| **`NHOM_TU_CHON` (Nhóm tự chọn)** | **Chờ chốt quy chế** | Lưu cấu hình mã nhóm (`maNhomTuChon`) và số tín chỉ tự chọn yêu cầu (`tinChiTuChonYeuCau`); trả về thông tin nhóm khi kiểm tra quan hệ, **không tự động chặn vượt trần tín chỉ nhóm khi đăng ký**. | Cần chốt: (1) Chặn đăng ký ngay khi vượt số tín chỉ tự chọn của nhóm hay cho phép học vượt và chỉ chọn môn tính điểm khi xét tốt nghiệp? |
