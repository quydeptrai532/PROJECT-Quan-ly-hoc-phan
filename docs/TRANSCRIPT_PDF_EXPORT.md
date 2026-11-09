# Xuất bảng điểm cá nhân PDF (QLSV-62 / TASK-ST-06)

Sinh viên có thể tải bảng điểm cá nhân dưới dạng PDF trực tiếp tại trang `/diem` và `/lich-su-hoc-tap`.

## API

`GET /api/sinh-vien/me/bang-diem/pdf?hocKyId={id}`

- Chỉ cho phép vai trò `SINH_VIEN`; danh tính sinh viên luôn được xác định từ tài khoản đang đăng nhập (`CurrentUser`), không nhận `studentId` từ client.
- `hocKyId` (tùy chọn): nếu truyền vào sẽ chỉ xuất các học phần đã công bố điểm thuộc học kỳ đó (`400 Bad Request` nếu `<= 0`, `404 Not Found` nếu học kỳ không tồn tại).
- Nếu sinh viên chưa có bất kỳ học phần nào có điểm đã công bố (trong phạm vi chọn), API trả `400 Bad Request` với thông báo `"Chưa có kết quả học tập đã công bố để xuất bảng điểm PDF"` và giao diện hiển thị cảnh báo rõ ràng, không sinh tệp rỗng/hỏng.
- Header phản hồi:
  - `Content-Type: application/pdf`
  - `Content-Disposition: attachment; filename="bang-diem-{maSinhVien}.pdf"; filename*=UTF-8''bang-diem-{maSinhVien}.pdf`
  - `Cache-Control: no-store`

## Nội dung bảng điểm PDF và nguồn dữ liệu

- Tái sử dụng trực tiếp `DiemService.lichSuHocTap(...)`, `DiemService.quyDoiHe4(...)`, `AcademicRules.DIEM_DAT` (`5.0`) và `StudentAcademicProfileService.getMyProfile()` để bảo đảm số liệu khớp 100% với trang Điểm và Lịch sử học tập.
- **Chỉ xuất điểm đã công bố**: chỉ các bản ghi có `diemTongKet != null` mới được đưa vào PDF; học phần đang học hoặc mới có điểm thành phần nháp (`diemTongKet == null`) bị loại hoàn toàn khỏi PDF.
- **Thông tin nhận diện & học vụ**: Họ và tên, Mã sinh viên, Khoa, Ngành, Lớp hành chính, Email, Khóa nhập học, Phiên bản CTĐT, Thời điểm phát hành (`dd/MM/yyyy HH:mm:ss`).
- **Tổng hợp toàn khóa & từng học kỳ**: Số học phần đã công bố, Tổng tín chỉ có điểm, Tín chỉ tích lũy (đạt từ `5.0`), GPA hệ 10, GPA hệ 4 và bảng chi tiết môn học theo từng học kỳ (`STT`, `Mã môn`, `Tên môn học`, `Lớp học phần` kèm thứ tự lần học nếu học nhiều lần, `Tín chỉ`, `Quá trình`, `Cuối kỳ`, `Tổng kết`, `Hệ 4`, `Kết quả`).
- **Định dạng & font tiếng Việt**: Khổ A4 dọc, nhúng trực tiếp bộ font TrueType Unicode `LiberationSans` (`IDENTITY_H`, `EMBEDDED`) từ `openpdf-fonts-extra`, lặp lại dòng tiêu đề bảng khi sang trang mới và đánh số trang `Trang X / Y` ở chân trang.

## Các trường và thông tin chưa xuất do dữ liệu/mẫu chính thức chưa có

- **Tên đơn vị phát hành chính thức**: chỉ hiển thị trên đầu bảng điểm khi cấu hình biến `app.export.bang-diem.don-vi-phat-hanh` (hoặc biến môi trường `EXPORT_BANG_DIEM_DON_VI_PHAT_HANH`). Mặc định để trống, không tự bịa tên trường/cơ quan chủ quản.
- **Mẫu hành chính, chữ ký, con dấu, mã xác thực/QR**: Đã được nâng cấp chính thức theo tiêu chuẩn điện tử xác thực trong **QLSV-99 / TASK-OP-12**. PDF hiện nhúng khối xác thực điện tử với mã xác minh ngẫu nhiên entropy cao, mã QR quét đường dẫn HTTPS tra cứu công khai và snapshot bất biến được lưu trữ tại cơ sở dữ liệu (`bang_diem_xac_thuc`). Chi tiết xem tại `docs/VERIFIABLE_TRANSCRIPT_QLSV_99.md`.
- **Thông tin nhân thân bổ sung** (ngày sinh, giới tính, nơi sinh, CCCD, hệ đào tạo, xếp loại học lực toàn khóa, trạng thái học tập): chưa có cột dữ liệu trong bảng `sinh_vien` nên không đưa vào hoặc giữ giá trị `Chưa cập nhật` từ hồ sơ đào tạo.
- **Quy chế thay thế điểm khi học lại/học cải thiện**: chưa được cấu hình chính thức trong hệ thống; PDF liệt kê đầy đủ các lần học đã công bố và ghi chú rõ cách tính hiện hành.
