# Tài liệu Kỹ thuật: Quản lý Phòng học (QLSV-80 — TASK-AD-05)

## 1. Tổng quan & Mục tiêu

- **Mã công việc:** QLSV-80 (TASK-AD-05)
- **Mục tiêu:** Thiết kế và triển khai chức năng Quản lý Phòng học (`phong_hoc` / `Classroom`) trong hệ thống Quản lý Sinh viên & Đào tạo (QLSV).
- **Yêu cầu cốt lõi:**
  1. Mô hình thông tin phòng học: Mã phòng duy nhất, tên phòng, tòa nhà, tầng, vị trí chi tiết, sức chứa (chỗ ngồi), phân loại phòng, trang thiết bị, trạng thái hoạt động và ghi chú.
  2. Tìm kiếm và bộ lọc đa tiêu chí: Lọc theo loại phòng, khoảng sức chứa (min/max), trạng thái hoạt động/bảo trì/ngừng sử dụng, và từ khóa tìm kiếm nhanh.
  3. Ràng buộc & Validation backend chặt chẽ:
     - Chuẩn hóa mã phòng (trim khoảng trắng, uppercase đồng nhất, kiểm tra duy nhất case-insensitive).
     - Sức chứa là số nguyên dương ($> 0$).
     - Chặn tuyệt đối việc mở lớp học phần mới hoặc gán lịch vào phòng đang **Bảo trì** (`BAO_TRI`) hoặc **Ngừng sử dụng** (`NGUNG_SU_DUNG`).
     - Bảo vệ dữ liệu lịch sử: **Không xóa cứng phòng học đã gắn lịch học phần**. Hỗ trợ chính sách chuyển trạng thái sang **Ngừng sử dụng** để lưu trữ lịch sử đào tạo.
     - Bảo đảm tính toàn vẹn tham chiếu lịch sử: Lớp/lịch cũ tiếp tục hiển thị chính xác tên và mã phòng đã xếp lịch; việc đổi tên/cập nhật thông tin phòng không làm sai lệch hay hỏng tham chiếu dữ liệu.
     - Phân quyền quản trị viên (`QUAN_TRI`) và ghi nhận nhật ký kiểm toán (`AuditLogService`) với định danh người thực hiện và mốc thời gian.
  4. Cơ chế phân loại (Taxonomy) mở rộng: Hệ thống sử dụng danh mục phân loại phòng và thiết bị mở rộng, hỗ trợ gợi ý linh hoạt, không gán cứng nghiệp vụ vào mã nguồn.

---

## 2. Thiết kế Database Schema

### 2.1. Bảng `phong_hoc`

```sql
CREATE TABLE IF NOT EXISTS phong_hoc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma VARCHAR(50) NOT NULL,
    ten VARCHAR(150) NOT NULL,
    toa_nha VARCHAR(50),
    tang INT,
    vi_tri VARCHAR(255),
    suc_chua INT NOT NULL,
    loai_phong VARCHAR(100),
    thiet_bi VARCHAR(500),
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'HOAT_DONG',
    ghi_chu VARCHAR(1000),
    tao_luc TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cap_nhat_luc TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    nguoi_tao_id BIGINT,
    nguoi_cap_nhat_id BIGINT,
    CONSTRAINT uk_phong_hoc_ma UNIQUE (ma)
);

CREATE INDEX IF NOT EXISTS idx_phong_hoc_trang_thai ON phong_hoc(trang_thai);
CREATE INDEX IF NOT EXISTS idx_phong_hoc_loai_phong ON phong_hoc(loai_phong);
CREATE INDEX IF NOT EXISTS idx_phong_hoc_suc_chua ON phong_hoc(suc_chua);
CREATE INDEX IF NOT EXISTS idx_phong_hoc_toa_nha ON phong_hoc(toa_nha);
```

### 2.2. Quan hệ với bảng `lop_hoc_phan` (`CourseClass`)

Bảng `lop_hoc_phan` bổ sung khóa ngoại `phong_hoc_id` liên kết với `phong_hoc(id)`:

```sql
ALTER TABLE lop_hoc_phan ADD COLUMN phong_hoc_id BIGINT NULL;
ALTER TABLE lop_hoc_phan ADD CONSTRAINT fk_lhp_phong_hoc FOREIGN KEY (phong_hoc_id) REFERENCES phong_hoc(id);
```

> [!NOTE]
> **Chính sách tương thích ngược và bảo toàn tham chiếu lịch sử:**
> Cột `phong VARCHAR(20)` sẵn có trên bảng `lop_hoc_phan` tiếp tục được duy trì để lưu snapshot mã phòng tại thời điểm mở lớp. Đồng thời trường liên kết quan hệ `phong_hoc_id` (`@ManyToOne Classroom`) được gán khi tạo hoặc cập nhật lớp. Điều này đảm bảo:
> 1. Toàn bộ logic hiển thị thời khóa biểu của sinh viên, giảng viên và báo cáo lịch sử tiếp tục hoạt động 100% trơn tru mà không làm gián đoạn hệ thống.
> 2. Đổi tên hoặc cập nhật vị trí phòng học sau này không làm mất dữ liệu tham chiếu lịch sử của các học kỳ trước.

---

## 3. Quy tắc Nghiệp vụ & Kiểm soát Trạng thái

### 3.1. Các trạng thái phòng học (`ClassroomStatus`)

| Trạng thái | Mã định danh | Ý nghĩa nghiệp vụ | Cho phép xếp lớp mới? |
| :--- | :--- | :--- | :---: |
| **Hoạt động** | `HOAT_DONG` | Phòng học sẵn sàng phục vụ giảng dạy và xếp lịch | **Có** |
| **Bảo trì** | `BAO_TRI` | Phòng đang sửa chữa cơ sở vật chất, hỏng hóc hoặc tân trang thiết bị | **Không** (Chặn 400 Bad Request) |
| **Ngừng sử dụng** | `NGUNG_SU_DUNG` | Phòng bị hủy sử dụng, cải tạo thành chức năng khác | **Không** (Chặn 400 Bad Request) |

### 3.2. Quy tắc Xóa cứng vs. Ngừng sử dụng (Decommissioning Policy)

- **Khi thực hiện thao tác Xóa phòng (`DELETE /api/admin/phong-hoc/{id}`):**
  - Hệ thống kiểm tra trong database:
    $$\text{isReferenced} \iff \exists \text{CourseClass } c \text{ where } (c.\text{phongHoc.id} = id \lor c.\text{phong} = \text{room.ma})$$
  - **Nếu phòng đã được xếp cho ít nhất 1 lớp học phần (`soLopHocPhan > 0`):**
    - Hệ thống **từ chối xóa cứng** và ném lỗi `ApiException.badRequest`:
      `"Không thể xóa phòng học ... vì đã có lớp học phần được xếp lịch phòng này. Vui lòng chuyển trạng thái phòng sang 'Ngừng sử dụng' để bảo toàn lịch sử đào tạo."`
  - **Nếu phòng chưa từng được gán lịch nào (`soLopHocPhan == 0`):**
    - Cho phép xóa cứng bản ghi khỏi database, đồng thời ghi log kiểm toán hành động `CLASSROOM_DELETED`.

- **Chính sách Ngừng sử dụng (Decommissioning):**
  - Quản trị viên cập nhật trạng thái phòng thành `NGUNG_SU_DUNG` (qua API `PATCH /api/admin/phong-hoc/{id}/trang-thai` hoặc form chỉnh sửa).
  - Lịch sử các lớp cũ vẫn lưu giữ thông tin phòng học đầy đủ.
  - Mọi nỗ lực xếp lớp học phần mới vào phòng này sẽ bị backend từ chối với thông báo:
    `"Phòng học [MÃ] đã ngừng sử dụng, không thể xếp lịch cho lớp học phần."`

---

## 4. Danh mục Mở rộng (Taxonomy Architecture)

Danh mục loại phòng và thiết bị chưa bị ấn định cứng mà được thiết kế theo mô hình mở rộng (Extensible Taxonomy):

1. **Loại phòng (`loaiPhong`):**
   - Bộ cấu hình gợi ý mặc định:
     - `Lý thuyết`
     - `Thực hành máy tính`
     - `Hội trường`
     - `Thí nghiệm`
     - `Đa năng`
   - Quản trị viên hoàn toàn có thể nhập loại phòng mới bất kỳ (VD: `Phòng Seminar`, `Studio ghi hình`, `Phòng học ngoại ngữ`) trực tiếp trên giao diện mà không cần chỉnh sửa mã nguồn hay cập nhật database enum.
2. **Trang thiết bị (`thietBi`):**
   - Danh sách tag gợi ý thêm nhanh: `Máy chiếu`, `Micro không dây`, `Máy lạnh`, `Bảng thông minh`, `Dàn âm thanh`, `45 PC Core i7`, `Hệ thống thí nghiệm cơ bản`.
   - Lưu trữ dạng thẻ ngăn cách bởi dấu phẩy, cho phép mở rộng tùy biến theo từng phòng thực tế.
3. **API Cung cấp Taxonomy:**
   - `GET /api/admin/phong-hoc/taxonomy` trả về danh mục động gồm các loại phòng và thiết bị hiện có trong hệ thống kết hợp cùng các gợi ý chuẩn.

---

## 5. API Reference

Tất cả các API quản trị yêu cầu quyền `QUAN_TRI` và token xác thực:

| Method | Đường dẫn API | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/api/admin/phong-hoc` | Lấy danh sách toàn bộ phòng học (hỗ trợ query param `keyword`, `loaiPhong`, `trangThai`, `minSucChua`, `maxSucChua`) |
| `GET` | `/api/admin/phong-hoc/{id}` | Lấy chi tiết thông tin 1 phòng học theo ID |
| `POST` | `/api/admin/phong-hoc` | Thêm mới phòng học (Validate mã duy nhất, sức chứa $> 0$, ghi Audit Log) |
| `PUT` | `/api/admin/phong-hoc/{id}` | Cập nhật thông tin phòng học (Kiểm tra trùng mã ngoại trừ chính nó, ghi Audit Log) |
| `PATCH`| `/api/admin/phong-hoc/{id}/trang-thai` | Chuyển đổi trạng thái nhanh (`HOAT_DONG`, `BAO_TRI`, `NGUNG_SU_DUNG`) kèm ghi chú |
| `DELETE`| `/api/admin/phong-hoc/{id}` | Xóa phòng học (Chặn xóa cứng nếu đã có lớp học phần xếp lịch) |
| `GET` | `/api/admin/phong-hoc/hoat-dong` | Lấy danh sách các phòng đang hoạt động phục vụ gán lịch lớp |
| `GET` | `/api/admin/phong-hoc/taxonomy` | Lấy danh mục taxonomy gợi ý loại phòng, thiết bị, trạng thái |
| `GET` | `/api/phong-hoc/hoat-dong` | Endpoint dùng chung lấy danh sách phòng đang hoạt động cho các module liên quan |
