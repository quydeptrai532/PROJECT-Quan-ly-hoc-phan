# Hướng dẫn Import Điểm từ Excel (`.xlsx`) — QLSV-73 (TASK-LC-08)

Tài liệu này mô tả chi tiết định dạng template `.xlsx` chính thức, các rào chắn bảo mật khi phân tích tệp ở backend, quy tắc kiểm tra dữ liệu (tái sử dụng `GradeService.validateDiem` từ QLSV-72), cơ chế xem trước (Preview) gắn phiên ngắn hạn, chính sách giao dịch (Transaction Policy), chính sách cập nhật điểm hiện có (Update Policy) và danh mục API cho tính năng Import điểm lớp học phần.

---

## 1. Định dạng Template `.xlsx` Chính thức

Giảng viên phụ trách lớp học phần có thể tải xuống tệp mẫu `.xlsx` đã điền sẵn danh sách sinh viên đang theo học (`DA_DANG_KY`) trực tiếp trên màn hình **Nhập điểm lớp học phần** hoặc qua API:
- `GET /api/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx`

### Cấu trúc Workbook & Sheet
- **Định dạng tệp:** `.xlsx` (OOXML SpreadsheetML). Tuyệt đối **không hỗ trợ `.csv`, `.xls` (BIFF8/OLE2) hoặc `.xlsm` (macro)**.
- **Số lượng sheet tối đa:** `1` sheet (tên sheet mặc định: `BangDiem`).
- **Dòng tiêu đề (Dòng 1):**

| Cột Excel | Mã cột chuẩn | Tên hiển thị chấp nhận | Bắt buộc | Quy tắc dữ liệu |
|---|---|---|---|---|
| `A` | `maSinhVien` | `maSinhVien`, `Mã sinh viên`, `Mã SV` | Có | Duy nhất trong tệp; phải thuộc danh sách sinh viên có đăng ký `DA_DANG_KY` của lớp được chọn. |
| `B` | `hoTen` | `hoTen`, `Họ tên`, `Họ và tên` | Không | Cột thông tin hỗ trợ giảng viên đối chiếu nhanh khi nhập điểm. |
| `C` | `diemQuaTrinh` | `diemQuaTrinh`, `Điểm quá trình`, `Quá trình` | Có | Số thực trong đoạn `[0, 10]`, tối đa `2` chữ số thập phân (trọng số 40%). Có thể để trống khi lưu nháp. |
| `D` | `diemCuoiKy` | `diemCuoiKy`, `Điểm cuối kỳ`, `Cuối kỳ` | Có | Số thực trong đoạn `[0, 10]`, tối đa `2` chữ số thập phân (trọng số 60%). Có thể để trống khi lưu nháp. |

- **Công thức tính điểm tổng kết:** Backend tự động tính `diemTongKet = round((0.4 × diemQuaTrinh + 0.6 × diemCuoiKy) × 10) / 10` khi cả hai cột điểm đều có giá trị hợp lệ (khớp tuyệt đối với QLSV-72).

---

## 2. Giới hạn Tệp & Rào chắn Bảo mật Backend

Hệ thống không tin cậy tên tệp, phần mở rộng hoặc `Content-Type` từ phía client. Mọi tệp tải lên được ghi vào tệp tạm có giới hạn dung lượng, kiểm tra chữ ký nhị phân (magic bytes `PK\x03\x04` và cấu trúc OOXML `[Content_Types].xml`, `xl/workbook.xml`) và luôn được xóa sạch trong khối `finally` kể cả khi xảy ra lỗi.

| Tham số cấu hình (`application.yml`) | Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|---|
| `app.grade-import.max-file-size-bytes` | `GRADE_IMPORT_MAX_FILE_SIZE_BYTES` | `2097152` (2 MB) | Dung lượng tối đa của tệp `.xlsx` tải lên. |
| `app.grade-import.max-rows` | `GRADE_IMPORT_MAX_ROWS` | `500` | Số dòng dữ liệu sinh viên tối đa trong 1 lần import. |
| `app.grade-import.max-sheets` | `GRADE_IMPORT_MAX_SHEETS` | `1` | Số lượng sheet tối đa trong workbook `.xlsx`. |
| `app.grade-import.parse-timeout-ms` | `GRADE_IMPORT_PARSE_TIMEOUT_MS` | `5000` (5 giây) | Thời gian xử lý phân tích tệp tối đa. |
| `app.grade-import.preview-ttl-seconds` | `GRADE_IMPORT_PREVIEW_TTL_SECONDS` | `900` (15 phút) | Thời hạn hiệu lực của mã phiên xem trước (`previewToken`). |
| `app.grade-import.max-zip-entry-size-bytes` | `GRADE_IMPORT_MAX_ZIP_ENTRY_SIZE_BYTES` | `10485760` (10 MB) | Giới hạn tổng dung lượng giải nén chống tấn công Zip Bomb. |
| `app.grade-import.max-zip-entries` | `GRADE_IMPORT_MAX_ZIP_ENTRIES` | `50` | Số lượng thành phần nén tối đa bên trong gói `.xlsx`. |
| `app.grade-import.temp-dir` | `GRADE_IMPORT_TEMP_DIR` | `./data/temp-grade-imports` | Thư mục lưu tệp tạm trong quá trình phân tích, tự động dọn sạch ngay sau khi đọc. |

### Chống Công thức, Macro & XXE
- **XXE Prevention:** StAX parser tắt hoàn toàn `SUPPORT_DTD` và `IS_SUPPORTING_EXTERNAL_ENTITIES`.
- **Macro/VBA Rejection:** Từ chối ngay các workbook có `vbaProject.bin` hoặc `macroEnabled`.
- **Formula Rejection:** Không bao giờ thực thi công thức trong workbook. Mọi ô chứa thẻ công thức `<f>` hoặc chuỗi bắt đầu bằng dấu `=` đều bị từ chối và báo lỗi chính xác tại `sheet`, `dong`, `cot` (`cotExcel`).

---

## 3. Chính sách Giao dịch (Transaction Policy) & Cập nhật (Update Policy)

1. **Transaction Policy (`ALL_OR_NOTHING`):**
   - Bước **Preview** (`POST .../import-xlsx/preview`) chạy ở chế độ chỉ đọc (`@Transactional(readOnly = true)`), kiểm tra toàn bộ dòng/cột và trả về thống kê (`tongSoDong`, `soDongHopLe`, `soDongLoi`, `soTaoMoiDuKien`, `soCapNhatDuKien`) cùng danh sách lỗi chi tiết (`sheet`, `dong`, `cot`, `cotExcel`, `maSinhVien`, `giaTriNhap`, `message`). Dữ liệu preview tuyệt đối không ghi vào bảng `diem`.
   - Chỉ khi `soDongLoi == 0` và giảng viên bấm **Xác nhận Import** (`POST .../import-xlsx/confirm`), hệ thống mới thực thi ghi dữ liệu trong một giao dịch nguyên tử (`@Transactional`) có khóa bi quan trên lớp học phần. Nếu có bất kỳ lỗi nào tại thời điểm xác nhận, toàn bộ giao dịch được rollback (`0` bản ghi được ghi).
2. **Ràng buộc Phiên Preview (`previewToken`):**
   - Mỗi phiên xem trước gắn chặt với `(userId, giangVienId, lopHocPhanId, fileSha256, expiresAt)` và chỉ được phép xác nhận **một lần duy nhất** trong thời hạn `900 giây`.
   - Quyền giảng viên phụ trách lớp được kiểm tra trước khi preview và kiểm tra lại lần nữa tại thời điểm xác nhận import.
3. **Update Policy (`OVERWRITE_EXISTING`) & Chống trùng lặp:**
   - Bảng `diem` gắn quan hệ `1-1` (`unique`) với `dang_ky_hoc_phan`.
   - Nếu sinh viên chưa có bản ghi điểm: tạo mới bản ghi (`TAO_MOI`).
   - Nếu sinh viên đã có bản ghi điểm nháp: cập nhật ghi đè `diemQuaTrinh`, `diemCuoiKy` và tính lại `diemTongKet` trên bản ghi hiện có (`CAP_NHAT`). Upload lại nhiều lần không bao giờ tạo dữ liệu trùng.

> **Ghi chú quyết định cần Chủ dự án xác nhận chính thức:** Mẫu cột Excel chuẩn (`maSinhVien`, `hoTen`, `diemQuaTrinh`, `diemCuoiKy` trên 1 sheet `BangDiem`), giới hạn tệp (`2 MB`, `500 dòng`, `1 sheet`, timeout `5 giây`, TTL preview `15 phút`), chính sách giao dịch `ALL_OR_NOTHING` và chính sách cập nhật `OVERWRITE_EXISTING` đang được áp dụng nhất quán qua cấu hình `app.grade-import.*` và sẵn sàng điều chỉnh khi có quyết định phê duyệt chính thức.

---

## 4. Danh mục API Import Điểm (`.xlsx`)

| Phương thức | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/cau-hinh` | Lấy cấu hình giới hạn tệp, danh sách cột mẫu, chính sách giao dịch và cập nhật. |
| `GET` | `/api/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx` | Tải tệp mẫu `.xlsx` có sẵn mã sinh viên đang theo học của lớp phụ trách. |
| `POST` | `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview` | Upload tệp `.xlsx` (`multipart/form-data`, field `file`), kiểm tra bảo mật/validation và trả về dữ liệu preview kèm `previewToken` & `fileSha256`. |
| `POST` | `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm` | Xác nhận ghi dữ liệu từ phiên preview hợp lệ (`{ "previewToken": "...", "fileSha256": "..." }`) vào bảng điểm và ghi nhật ký kiểm toán `GRADE_IMPORTED_XLSX`. |
