# CHÍNH SÁCH XÓA MỀM VÀ LƯU TRỮ DANH MỤC (CATALOG ARCHIVAL POLICY)
**Mã nghiệp vụ:** QLSV-89 — TASK-OP-02  
**Dự án:** Hệ thống Quản lý Đào tạo & Đăng ký Tín chỉ (QLSV)  
**Ngày ban hành:** 09/11/2026  
**Trạng thái:** Đã áp dụng & Kiểm thử hoàn chỉnh (Passed 100%)

---

## 1. MỤC TIÊU & NGUYÊN TẮC THIẾT KẾ

Trong hệ thống quản lý đào tạo đại học, dữ liệu danh mục (khoa, ngành, lớp hành chính, môn học, phòng học, học kỳ, khóa học) đóng vai trò xương sống cho toàn bộ các giao dịch học tập: đăng ký tín chỉ, xếp thời khóa biểu, nhập và tính điểm tổng kết, xét học vụ, in ấn bằng và bảng điểm quá trình.

Nếu xóa vật lý (hard delete) một danh mục đang có liên kết, cơ sở dữ liệu sẽ gặp lỗi vi phạm khóa ngoại (foreign key constraint violation) hoặc dẫn đến mồ côi dữ liệu (orphaned records), làm sai lệch toàn bộ lịch sử học tập của sinh viên. Ngược lại, nếu áp dụng soft delete máy móc cho tất cả các bảng sẽ làm phình to mô hình dữ liệu, làm phức tạp các câu lệnh truy vấn và tiềm ẩn nguy cơ xung đột khóa unique tự nhiên (như mã khoa, mã môn).

Vì vậy, hệ thống áp dụng nguyên tắc **phân loại thực thể theo quan hệ và nhu cầu lưu vết lịch sử**, chia thành 3 nhóm rõ ràng:
1. **Xóa mềm / Lưu trữ (Soft Delete / Archive)**
2. **Chuyển trạng thái ngừng hoạt động (Inactive / Status-based)**
3. **Chặn xóa khi đang được tham chiếu (Restrict Delete with Guiding Error)**

---

## 2. MA TRẬN PHÂN LOẠI THỰC THỂ DANH MỤC

| Thực thể | Tên bảng | Nhóm chính sách | Ràng buộc kiểm tra trước khi thao tác | Hành vi khi xóa | Hành vi khi tạo lại mã cũ |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Môn học** (`Subject`) | `mon_hoc` | **Nhóm 1: Soft Delete / Archive** | Lớp học phần (`lop_hoc_phan`), Chương trình đào tạo (`khung_chuong_trinh_mon`), Quan hệ môn (`mon_tien_quyet`) | - Nếu **đang có tham chiếu**: Chuyển `dangHieuLuc = false`, lưu `ngayLuuTru`, `lyDoLuuTru`, ghi log `COURSE_ARCHIVED`.<br>- Nếu **chưa có tham chiếu**: Cho phép xóa vật lý (hard delete).<br>- Nếu gửi tham số `permanent=true` mà có tham chiếu: Chặn xóa, trả 409 kèm hướng dẫn. | Trả lỗi 409 thông báo mã đã lưu trữ. Cho phép gửi cờ `khoiPhucNeuDaLuuTru = true` hoặc bấm "Khôi phục" để phục hồi bản ghi gốc mà không tạo trùng dữ liệu. |
| **Quan hệ môn / Tiên quyết** (`SubjectRelation`) | `mon_tien_quyet` | **Nhóm 1: Soft Delete / Inactive** | Đợt đăng ký, lớp học phần | Chuyển `dangHieuLuc = false` hoặc xóa vật lý nếu vừa tạo nhầm và chưa phát sinh ràng buộc thẩm định đăng ký. | Cập nhật lại điều kiện hoặc khôi phục hiệu lực. |
| **Phòng học** (`Classroom` - QLSV-80) | `phong_hoc` | **Nhóm 2: Ngừng hoạt động** | Thời khóa biểu (`thoi_khoa_bieu`), Buổi điểm danh | Chuyển trạng thái sang `NGUNG_SU_DUNG` (hoặc `BAO_TRI`). Không xóa vật lý khi đã có lịch học/lịch thi. | Nếu mã phòng đã tồn tại ở trạng thái `NGUNG_SU_DUNG`, trả thông báo hướng dẫn khôi phục trạng thái hoạt động thay vì tạo mới. |
| **Khóa đào tạo / Niên khóa** (`AcademicCohort`) | `khoa_dao_tao` | **Nhóm 2: Ngừng hoạt động** | Sinh viên (`sinh_vien`), Khung chương trình | Chuyển `dangHieuLuc = false`. Dữ liệu sinh viên các khóa cũ vẫn được bảo lưu trọn vẹn. | Báo lỗi trùng mã niên khóa, yêu cầu tái kích hoạt hoặc chỉnh sửa. |
| **Khoa** (`Faculty`) | `khoa` | **Nhóm 3: Chặn xóa khi có tham chiếu** | Ngành (`nganh`), Giảng viên (`giang_vien`), Sinh viên (`sinh_vien`) | - **Đang có tham chiếu**: Chặn xóa, trả HTTP 409 Conflict nêu rõ số lượng giảng viên, sinh viên, ngành trực thuộc và hướng dẫn điều chuyển.<br>- **Chưa có tham chiếu**: Cho phép xóa vật lý. | Báo lỗi trùng mã nếu mã đang tồn tại. |
| **Ngành học** (`Major`) | `nganh` | **Nhóm 3: Chặn xóa khi có tham chiếu** | Lớp hành chính (`lop_hanh_chinh`), Sinh viên (`sinh_vien`), Khung chương trình | - **Đang có tham chiếu**: Chặn xóa, trả HTTP 409 Conflict nêu rõ số lượng sinh viên, lớp hành chính liên quan.<br>- **Chưa có tham chiếu**: Cho phép xóa vật lý. | Báo lỗi trùng mã nếu mã đang tồn tại. |
| **Lớp hành chính** (`AdministrativeClass`) | `lop_hanh_chinh` | **Nhóm 3: Chặn xóa khi có tham chiếu** | Sinh viên (`sinh_vien`) | - **Đang có sinh viên**: Chặn xóa, trả HTTP 409 Conflict nêu rõ sĩ số sinh viên trong lớp.<br>- **Chưa có sinh viên**: Cho phép xóa vật lý. | Báo lỗi trùng mã nếu mã đang tồn tại. |
| **Học kỳ** (`Semester`) | `hoc_ky` | **Nhóm 3: Chặn xóa khi có tham chiếu** | Lớp học phần (`lop_hoc_phan`), Đợt đăng ký (`dot_dang_ky`) | - **Đang có tham chiếu**: Chặn xóa, trả HTTP 409 Conflict.<br>- **Chưa có tham chiếu**: Cho phép xóa vật lý. | Báo lỗi trùng mã/năm học. |

---

## 3. CHI TIẾT CÁC QUY TẮC NGHIỆP VỤ

### 3.1. Các trường hợp xóa vật lý (Hard Delete) còn được phép
Hệ thống chỉ cho phép xóa vật lý danh mục khi thỏa mãn đồng thời:
1. Bản ghi vừa được tạo mới hoặc tạo nhầm do sai sót của quản trị viên.
2. Bản ghi **chưa từng phát sinh bất kỳ tham chiếu nào**:
   - Môn học: Chưa từng có lớp học phần, chưa có trong khung chương trình, chưa gắn vào quan hệ tiên quyết.
   - Khoa: Chưa có ngành, chưa có giảng viên, chưa có sinh viên trực thuộc.
   - Ngành: Chưa có lớp hành chính, chưa có sinh viên, chưa có khung chương trình.
   - Lớp hành chính: Sĩ số sinh viên bằng 0.
   - Phòng học: Chưa từng có tiết xếp thời khóa biểu nào.

### 3.2. Mặc định ẩn dữ liệu Inactive / Archived trong các tác vụ thông thường
- **API Danh mục môn học (`GET /api/admin/mon-hoc`)**: Mặc định chỉ trả về các môn có `dangHieuLuc = true`.
  - Tham số lọc `dangHieuLuc=false`: Lấy danh sách các môn đã lưu trữ.
  - Tham số lọc `includeInactive=true`: Lấy toàn bộ môn học trong hệ thống.
- **Mở lớp học phần mới**: API mở lớp chặn hoàn toàn việc chọn môn học đã lưu trữ (`dangHieuLuc = false`), trả thông báo: `"Môn học '...' đã bị lưu trữ/ngừng giảng dạy, không thể mở lớp học phần mới."`
- **Đăng ký tín chỉ sinh viên**: Không xuất hiện các học phần thuộc môn học đã lưu trữ trong các đợt mở mới.

### 3.3. Bảo toàn dữ liệu trên các màn hình lịch sử (Historical Screens)
Ngay cả khi một môn học hoặc phòng học đã bị chuyển sang lưu trữ hoặc ngừng hoạt động:
1. **Bảng điểm sinh viên (Web & PDF Export)**: Vẫn hiển thị nguyên vẹn mã môn, tên môn, số tín chỉ, điểm thành phần và điểm tổng kết đã đạt được.
2. **Lịch sử đăng ký & Học phí**: Vẫn truy xuất được chi tiết lớp học phần và môn học tương ứng.
3. **Thời khóa biểu các học kỳ quá khứ**: Vẫn thể hiện thông tin môn học, mã phòng và giảng viên phụ trách tại thời điểm lớp được tổ chức.
4. **Báo cáo thống kê & Audit Log**: Toàn bộ vết lịch sử lưu trữ (`COURSE_ARCHIVED`) và khôi phục (`COURSE_RESTORED`) được lưu vết vĩnh viễn cùng thông tin người thực hiện, thời điểm và lý do.

### 3.4. Xử lý xung đột Unique Constraint khi tạo lại mã đã lưu trữ
Để không làm vi phạm ràng buộc `UNIQUE(ma)` và không tạo bản ghi rác trùng lặp:
1. Khi quản trị viên gửi yêu cầu tạo mới môn học (`POST /api/admin/mon-hoc`) với một mã đã tồn tại ở trạng thái lưu trữ:
   - Hệ thống từ chối tạo mới và trả về HTTP 409 Conflict với thông điệp:
     > *"Mã môn học '...' đã tồn tại trong danh mục lưu trữ. Vui lòng khôi phục môn học này hoặc đặt cờ khoiPhucNeuDaLuuTru=true để khôi phục và cập nhật thông tin."*
2. Nếu quản trị viên xác nhận muốn khôi phục (gửi `khoiPhucNeuDaLuuTru: true` hoặc gọi API `PUT /api/admin/mon-hoc/{id}/khoi-phuc`):
   - Bản ghi cũ được cập nhật lại trạng thái `dangHieuLuc = true`, `ngayLuuTru = null`, `lyDoLuuTru = null`.
   - Các thông tin tên môn, số tín chỉ, khoa được cập nhật theo dữ liệu mới nhất.
   - Toàn bộ dữ liệu lịch sử các lớp trước đây gắn với môn này được liên kết mượt mà mà không sinh bản ghi trùng.

### 3.5. Đồng bộ chính sách giữa API Backend và Giao diện UI
- **Giao diện Môn học (`SubjectPage.jsx`)**:
  - Có bộ lọc trạng thái: **Đang giảng dạy (mặc định)** | **Đã lưu trữ / Ngừng** | **Tất cả trạng thái**.
  - Môn đã lưu trữ hiển thị nhãn màu hổ phách `Đã lưu trữ` kèm lý do lưu trữ.
  - Cung cấp nút thao tác **Khôi phục** để đưa môn trở lại hoạt động với 1 cú click.
  - Khi xóa môn học có liên kết lịch sử, hộp thoại xác nhận giải thích rõ: *"Nếu môn học đã có lớp học phần hoặc chương trình tham chiếu, hệ thống sẽ tự động lưu trữ (soft delete) để bảo toàn lịch sử."*
  - Khi tạo môn trùng mã đã lưu trữ, form hiển thị khối thông báo gợi ý màu vàng kèm nút *"Khôi phục & Cập nhật môn học này"*.
- **Các trang Khoa, Ngành, Lớp hành chính, Học kỳ**:
  - Khi bấm xóa đối tượng đang có liên kết, giao diện hiển thị ngay thông báo lỗi chi tiết có tính chất hướng dẫn từ backend (ví dụ: *"Không thể xóa khoa CNTT vì đang có 12 giảng viên, 250 sinh viên..."*).

---

## 4. SCHEMA & DATABASE MIGRATION

Các cột mới được bổ sung có giá trị mặc định tương thích ngược cho dữ liệu hiện có:
- Bảng `mon_hoc`:
  - `dang_hieu_luc` (BOOLEAN NOT NULL DEFAULT TRUE)
  - `ngay_luu_tru` (TIMESTAMP NULL)
  - `ly_do_luu_tru` (VARCHAR(255) NULL)
- Bảng `khoa`, `nganh`, `lop_hanh_chinh`:
  - `dang_hieu_luc` (BOOLEAN NOT NULL DEFAULT TRUE)

Toàn bộ các file cấu hình `seed_data.sql` (root và backend resources) đã được đồng bộ để phục vụ môi trường MySQL/PostgreSQL cũng như H2 in-memory test.

---

## 5. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG

Bộ kiểm thử tích hợp `CatalogArchivalIntegrationTest` bao gồm 5 kịch bản chính:
1. `testXoaMonHocCoLopHocPhan_ChuyenSangLuuTruVaBaoToanLichSu`: Kiểm tra xóa môn học đang có lớp học phần -> chuyển sang lưu trữ an toàn, lịch sử lớp học phần vẫn trỏ đúng môn học.
2. `testDanhSachMonHoc_MacDinhAnMonLuuTru`: Kiểm tra danh sách mặc định chỉ hiện môn có hiệu lực, và lọc được môn đã lưu trữ khi truyền `dangHieuLuc=false`.
3. `testTaoLaiMonHocDaLuuTru_XulyUniqueConstraintVaKhoiPhuc`: Kiểm tra chặn tạo trùng mã đã lưu trữ và khôi phục thành công khi có cờ `khoiPhucNeuDaLuuTru`.
4. `testXoaMonHocChuaSuDung_XoaVatLyThanhCong`: Kiểm tra môn học mới tạo chưa phát sinh tham chiếu thì được phép xóa vật lý.
5. `testXoaKhoaCoLienKet_BiChanVaBaoLoiHuongDan`: Kiểm tra chặn xóa Khoa có giảng viên/ngành/sinh viên kèm thông điệp hướng dẫn rõ ràng.

Toàn bộ test suite của dự án (176 tests) đã vượt qua 100% không có lỗi.
