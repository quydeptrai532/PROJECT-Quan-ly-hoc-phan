# Báo cáo Chuẩn hóa Pipeline Import Dữ liệu — QLSV-90 (TASK-OP-03)

Tài liệu này tổng hợp giải pháp chuẩn hóa toàn diện luồng import dữ liệu trong dự án Quản lý Sinh viên (QLSV), tích hợp đồng nhất giữa **Import Sinh viên/Giảng viên (TASK-AD-07 / QLSV-82)** và **Import Điểm Excel (TASK-LC-08 / QLSV-73)**, thiết lập contract chung, rào chắn bảo mật dữ liệu, chính sách giao dịch, cơ chế idempotency và danh mục API.

---

## 1. Kiến trúc Luồng Import Dữ liệu Chuẩn hóa

Hệ thống tuân thủ nghiêm ngặt **chu trình chuẩn hóa 5 bước** thống nhất cho toàn bộ các loại dữ liệu import:

```
[1. Tải Template] ───> [2. Upload & Thẩm định Bảo mật] ───> [3. Phân tích & Validation]
                                                                        │
[6. Xuất File Lỗi] <─── [5. Xác nhận Thực thi (Confirm)] <─── [4. Xem trước (Preview)]
```

1. **Tải Template đúng loại dữ liệu (`exportTemplate`):**
   - Sinh viên & Giảng viên: Tệp CSV UTF-8 kèm ký tự BOM (`\uFEFF`) để Microsoft Excel hiển thị đúng tiếng Việt có dấu.
   - Điểm lớp học phần: Tệp workbook Excel `.xlsx` (OOXML) chứa danh sách sinh viên thực tế đang học (`DA_DANG_KY`).
2. **Upload & Kiểm tra bảo mật/định dạng/kích thước (`validateFileSecurity`):**
   - Chống Path Traversal: Làm sạch và từ chối các chuỗi `../`, `..`, `\`, ký tự điều khiển (`ASCII < 32`), null-byte (`\0`).
   - Kiểm tra dung lượng tối đa và số dòng tối đa qua cấu hình `application.yml`.
   - Chống giả mạo định dạng: Kiểm tra chữ ký nhị phân (Magic Bytes), từ chối tệp thực thi Windows PE (`MZ`), Linux ELF, ZIP, PDF, OLE2 trá hình phần mở rộng `.csv`.
   - Kiểm tra chuẩn mã hóa: Tệp CSV bắt buộc phải giải mã được theo chuẩn UTF-8 nghiêm ngặt. Tệp Excel bắt buộc phải có chữ ký ZIP OOXML (`PK\x03\x04`).
3. **Validate cấu trúc, kiểu dữ liệu, khóa trùng và quan hệ tham chiếu:**
   - Kiểm tra dòng tiêu đề (header mapping) và bắt buộc đủ các cột thiết yếu.
   - Kiểm tra trùng lặp nội bộ trong cùng tệp tải lên (Duplicate in file).
   - Kiểm tra toàn vẹn tham chiếu (Referential Integrity): Kiểm tra ID khoa, ngành, lớp hành chính trong cơ sở dữ liệu và ràng buộc cây quan hệ `Khoa -> Ngành -> Lớp hành chính`.
   - Đối với điểm: Kiểm tra sinh viên có thuộc danh sách lớp học phần; kiểm tra miền giá trị điểm `[0, 10]`, tối đa 2 chữ số thập phân; cấm tuyệt đối công thức tính toán (`=`, `<f>`).
4. **Xem trước (Preview) gắn phiên ngắn hạn:**
   - Chạy ở chế độ chỉ đọc (`readOnly = true`), tuyệt đối không ghi vào cơ sở dữ liệu.
   - Cung cấp mã phiên `previewToken` ngẫu nhiên bảo mật (UUIDv4) kèm chữ ký băm `fileSha256` và thời hạn hiệu lực `expiresAt` (mặc định 15 phút).
   - Thống kê chi tiết: `tongSoDong`, `soDongHopLe`, `soDongLoi`, `soTaoMoiDuKien`, `soCapNhatDuKien`, cờ `coTheXacNhan`, danh sách lỗi chi tiết theo dòng/cột.
5. **Chỉ ghi sau khi người dùng xác nhận (Confirm):**
   - Xác thực `previewToken` và kiểm tra khớp `fileSha256`.
   - Áp dụng nguyên tử một lần (`consumed.compareAndSet(false, true)`); từ chối xác nhận lại phiên đã dùng.
   - Thực thi theo Transaction Policy và Update/Idempotency Policy đã hiển thị trước khi xác nhận.
6. **Báo cáo kết quả theo dòng & Cho phép tải file lỗi (Export Errors):**
   - Trả về danh sách kết quả chi tiết từng dòng (`hopLe`, `hanhDong`, `errors`).
   - Cung cấp API tải tệp CSV báo cáo danh sách các dòng lỗi để người dùng dễ dàng chỉnh sửa ngoại tuyến và upload lại.

---

## 2. Bảng Tổng Hợp Các Loại Import Chuẩn Hóa

| Thuộc tính | Import Sinh viên | Import Giảng viên | Import Điểm lớp học phần |
|---|---|---|---|
| **Mã loại (`ImportType`)** | `SINH_VIEN` | `GIANG_VIEN` | `DIEM_LOPHOCPHAN` |
| **Định dạng tệp** | CSV (UTF-8 có BOM) | CSV (UTF-8 có BOM) | Excel OOXML (`.xlsx`) |
| **MIME Type** | `text/csv; charset=UTF-8` | `text/csv; charset=UTF-8` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| **Cấu trúc cột chuẩn** | `ma,hoTen,email,khoaId,nganhId,lopId` (+ `soDienThoai`) | `ma,hoTen,email,soDienThoai,khoaId` | `maSinhVien,hoTen,diemQuaTrinh,diemCuoiKy` (Sheet `BangDiem`) |
| **Dung lượng tối đa** | `5 MB` (`5,242,880` bytes) | `5 MB` (`5,242,880` bytes) | `2 MB` (`2,097,152` bytes) |
| **Số dòng tối đa** | `2.000` dòng dữ liệu | `2.000` dòng dữ liệu | `500` dòng dữ liệu |
| **Thời hạn phiên Preview** | `900` giây (15 phút) | `900` giây (15 phút) | `900` giây (15 phút) |
| **Transaction Policy** | `ROW_BY_ROW` | `ROW_BY_ROW` | `ALL_OR_NOTHING` |
| **Update / Idempotency Policy** | `SKIP_IF_EXISTS` | `SKIP_IF_EXISTS` | `OVERWRITE_EXISTING` |
| **Phân quyền Backend** | Quản trị viên (`QUAN_TRI`) | Quản trị viên (`QUAN_TRI`) | Giảng viên phụ trách lớp hoặc Quản trị viên |
| **API Tải Template** | `GET /api/admin/sinh-vien/import/template` | `GET /api/admin/giang-vien/import/template` | `GET /api/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx` |
| **API Lấy Cấu hình** | `GET /api/admin/sinh-vien/import/cau-hinh` | `GET /api/admin/giang-vien/import/cau-hinh` | `GET /api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/cau-hinh` |
| **API Xem trước (Preview)** | `POST /api/admin/sinh-vien/import/preview` | `POST /api/admin/giang-vien/import/preview` | `POST /api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview` |
| **API Xác nhận (Confirm)** | `POST /api/admin/sinh-vien/import/confirm` | `POST /api/admin/giang-vien/import/confirm` | `POST /api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm` |
| **API Xuất File Lỗi** | `POST /api/admin/sinh-vien/import/export-errors` | `POST /api/admin/giang-vien/import/export-errors` | `POST /api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/export-errors` |

---

## 3. Chính Sách Giao Dịch & Cập Nhật Dữ Liệu

### A. Transaction Policy
1. **`ROW_BY_ROW` (Áp dụng cho Sinh viên & Giảng viên):**
   - Mỗi dòng dữ liệu hợp lệ được commit trong một transaction độc lập (`transactionTemplate.execute(...)`).
   - Đảm bảo nguyên tử ở cấp độ bản ghi: tài khoản `User` và hồ sơ `Student`/`Lecturer` luôn được ghi cùng lúc hoặc rollback cùng lúc; tuyệt đối không tạo tài khoản mồ côi.
   - Dòng bị lỗi chỉ rollback cục bộ dòng đó, ghi nhận nguyên nhân lỗi chi tiết vào danh sách kết quả, không làm ảnh hưởng đến các dòng hợp lệ khác.
2. **`ALL_OR_NOTHING` (Áp dụng cho Điểm lớp học phần):**
   - Chỉ khi toàn bộ các dòng trong tệp đều hợp lệ (`soDongLoi == 0`) và giảng viên bấm xác nhận, hệ thống mới ghi vào bảng điểm trong một giao dịch nguyên tử (`@Transactional`) kèm khóa bi quan trên lớp học phần.
   - Nếu có bất kỳ lỗi nào, toàn bộ giao dịch được rollback (`0` bản ghi được lưu).

### B. Idempotency & Update Policy
1. **`SKIP_IF_EXISTS` (Áp dụng cho Sinh viên & Giảng viên):**
   - Nếu mã định danh (`ma`), tên đăng nhập (`username`) hoặc thư điện tử (`email`) đã tồn tại trong cơ sở dữ liệu: dòng đó được đánh dấu là `TRUNG_LAP` và từ chối tạo mới.
   - Hệ thống **không tự ý ghi đè** thông tin tài khoản đã có, bảo toàn toàn vẹn lịch sử đào tạo. Tải lên lại tệp nhiều lần không sinh bản ghi trùng.
2. **`OVERWRITE_EXISTING` (Áp dụng cho Điểm lớp học phần):**
   - Bảng `diem` có ràng buộc duy nhất (`1-1`) với `dang_ky_hoc_phan`.
   - Nếu sinh viên chưa có điểm: tạo mới (`TAO_MOI`).
   - Nếu sinh viên đã có điểm nháp: cập nhật ghi đè điểm quá trình/cuối kỳ trên bản ghi hiện có (`CAP_NHAT`). Upload lại nhiều lần không tạo bản ghi trùng lặp.

---

## 4. Rào Chắn Bảo Mật & An Toàn Hệ Thống

1. **Chống Path Traversal & Ký tự nguy hiểm:**
   - Phương thức tập trung `ImportSecurityValidator.sanitizeAndValidateFileName` loại bỏ mọi chuỗi `../`, `..`, `\`, ký tự điều khiển ASCII và null-byte `\0`.
2. **Chống Giả Mạo Định Dạng (Fake File Format Attack):**
   - Kiểm tra magic bytes của các định dạng thực thi/nhị phân (`MZ` cho PE Executable, `\x7fELF` cho Linux ELF, `PK\x03\x04` cho ZIP, `%PDF` cho PDF, `\xD0\xCF\x11\xE0` cho OLE2, GZIP, Java bytecode).
   - Tệp CSV chứa ký tự nhị phân null-byte hoặc chuỗi byte không thể giải mã UTF-8 hợp lệ sẽ bị từ chối ngay lập tức.
3. **Chống Tấn Công Tệp Excel (XLSX):**
   - Tắt hoàn toàn DTD và External Entities trong bộ đọc StAX XML để chống tấn công XXE.
   - Từ chối ngay các tệp chứa Macro/VBA (`vbaProject.bin`, `macroEnabled`).
   - Cấm hoàn toàn ô chứa công thức Excel (`=`, thẻ `<f>`); chỉ nhận giá trị số thực.
   - Giới hạn giải nén Zip Bomb qua `max-zip-entry-size-bytes` (10 MB) và `max-zip-entries` (50 entries).
   - Tệp tạm luôn được dọn dẹp trong khối `finally`.
4. **Bảo mật Dữ liệu Nhạy cảm & Stack Trace:**
   - Mật khẩu mặc định (`123456`) và hash BCrypt tuyệt đối không được đưa vào preview, log hay audit log.
   - Mã phiên `previewToken` và JWT token không bao giờ được ghi vào nhật ký hệ thống.
   - `GlobalExceptionHandler` chặn bắt toàn bộ ngoại lệ chưa xử lý (`Exception.class`), trả về thông báo lỗi 500 an toàn và không làm lộ stack trace ra phía client.
