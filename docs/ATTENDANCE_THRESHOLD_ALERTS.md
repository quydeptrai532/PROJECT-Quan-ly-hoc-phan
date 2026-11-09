# QLSV-71 / TASK-LC-06: Cảnh báo nghỉ quá ngưỡng trong dự án QLSV

## 1. Mục tiêu và bối cảnh nghiệp vụ

Tính năng cảnh báo nghỉ quá ngưỡng chuyên cần (QLSV-71) giúp phát hiện sớm các trường hợp sinh viên có số buổi vắng không phép hoặc tỷ lệ vắng đạt/vượt mức cảnh báo quy định. Cảnh báo được gửi tới Giảng viên phụ trách lớp và Sinh viên thông qua Notification Center nội bộ, đồng thời hiển thị trực quan trên giao diện điểm danh của giảng viên và trang chuyên cần của sinh viên.

---

## 2. Nguyên tắc tính toán và nghiệp vụ cốt lõi

1. **Tính nhất quán từ dữ liệu điểm danh chuẩn (Không dùng bộ đếm độc lập)**:
   - Hệ thống không lưu trữ hay duy trì một cột đếm độc lập có nguy cơ sai lệch số liệu.
   - Khi có bất kỳ thay đổi nào (điểm danh buổi học, sửa buổi học, xóa buổi học, hoặc thông báo nghỉ học), hệ thống tính toán lại trực tiếp từ bảng `diem_danh` kết hợp với `buoi_hoc` của lớp học phần.
   - Số buổi vắng được tính tuân thủ cấu hình chuẩn từ QLSV-70 (`AttendanceProperties`):
     - Vắng không phép (`VANG`).
     - Vắng có phép (`CO_PHEP`) chỉ tính vào vắng khi `app.attendance.co-phep-tinh-vang=true`.
     - Quy đổi muộn sang vắng nếu `app.attendance.so-lan-muon-thanh-mot-vang > 0`.

2. **Loại trừ buổi chưa diễn ra và buổi bị hủy**:
   - Buổi học có ngày học trong tương lai (`buoiHoc.ngayHoc > LocalDate.now()`) chưa diễn ra và không được tính vào tổng số buổi hợp lệ.
   - Buổi học rơi vào ngày có thông báo nghỉ (`ThongBaoNghi`) của lớp học phần được xác định là buổi bị hủy và không tính vào tổng số buổi hợp lệ.
   - `tongBuoiHopLe`: Tổng số buổi học thực tế của lớp đã diễn ra và không bị hủy.
   - Tỷ lệ vắng (%) = `(soBuoiVang / tongBuoiHopLe) * 100%` (nếu `tongBuoiHopLe > 0`).

3. **Chính sách kiểm soát quyền Giảng viên tại Backend**:
   - Giảng viên (`GIANG_VIEN`) chỉ được xem và cấu hình cảnh báo cho các lớp học phần mình trực tiếp phụ trách (`lopHocPhan.giangVien`).
   - Mọi nỗ lực truy cập hoặc cấu hình ngưỡng lớp của giảng viên khác đều bị chặn tại backend và trả về lỗi `403 Forbidden`.
   - Quản trị viên (`QUAN_TRI`) có quyền xem toàn diện và cấu hình cho mọi lớp học phần.

4. **Quản lý trạng thái cảnh báo và bảo toàn lịch sử Audit Log**:
   - Trạng thái cảnh báo:
     - `DANG_CANH_BAO`: Sinh viên có số buổi vắng hoặc tỷ lệ vắng đạt/vượt ngưỡng đã cấu hình.
     - `DA_GIAI_TOA`: Khi kết quả điểm danh được cập nhật lại (ví dụ giảng viên sửa điểm danh, chuyển trạng thái vắng thành có mặt/phép, hoặc bổ sung buổi học hợp lệ) khiến mức vắng trở về dưới ngưỡng quy định.
   - **Tuyệt đối không xóa bản ghi lịch sử**: Khi cảnh báo được giải tỏa, trạng thái bản ghi trong bảng `canh_bao_diem_danh` chuyển sang `DA_GIAI_TOA`, thời điểm cập nhật được ghi nhận và hành động `ATTENDANCE_ALERT_RESOLVED` được ghi vào bảng `audit_log` phục vụ thanh tra đào tạo.

5. **Cơ chế Idempotency và ngăn ngừa trùng lặp cảnh báo**:
   - Khóa duy nhất tại cơ sở dữ liệu: `uk_canh_bao_sv_lop_nguong` trên bộ ba `(sinh_vien_id, lop_hoc_phan_id, loai_nguong)`.
   - Idempotency key cho thông báo Notification Center:
     `attendance-alert:{lopId}:{sinhVienId}:{loaiNguong}:{soBuoiVang}/{tongBuoiHopLe}:{trangThai}`
   - Khi lưu điểm danh lặp lại mà số buổi vắng và buổi hợp lệ không thay đổi: hệ thống không tạo thêm bản ghi, không gửi thông báo trùng lặp và không ghi audit log dư thừa.

6. **Tích hợp Notification Center nội bộ**:
   - Gửi thông báo trong hệ thống (`thong_bao`) tới cả Giảng viên phụ trách và Sinh viên vi phạm.
   - Không tự ý gửi Email/SMS ngoài luồng.

---

## 3. Cấu hình Ngưỡng và Các điểm chờ Chủ dự án quyết định

Do quy chế chính thức về ngưỡng cảnh báo chuyên cần chưa được Hội đồng Đào tạo chốt, hệ thống thiết kế cơ chế cấu hình phân tầng linh hoạt:

1. **Cấu hình tập trung qua `application.yml` / `AttendanceProperties`**:
   - `app.attendance.canh-bao-kich-hoat`: Mặc định `false` (tắt cảnh báo tập trung, chỉ kích hoạt khi có giá trị xác nhận).
   - `app.attendance.nguong-so-buoi-vang`: Ngưỡng số buổi vắng tối đa toàn trường (mặc định để trống).
   - `app.attendance.nguong-ty-le-vang`: Ngưỡng tỷ lệ vắng tối đa toàn trường (%) (mặc định để trống).
   - `app.attendance.ghi-chu-nguong`: Thông điệp chính sách giải thích quy chế.

2. **Cấu hình theo Lớp học phần (`LopHocPhan`)**:
   - Cho phép Giảng viên hoặc Quản trị viên thiết lập ngưỡng riêng cho từng lớp thông qua API:
     - `nguongSoBuoiVang` (Integer, tối thiểu 1 buổi).
     - `nguongTyLeVang` (Double, từ 0.1% đến 100.0%).
   - Cấu hình theo lớp có độ ưu tiên cao hơn cấu hình tập trung.

3. **Nội dung cần Chủ dự án quyết định chính thức**:
   - [ ] **Mức ngưỡng chính thức**: Số buổi vắng tuyệt đối (ví dụ: vắng 3 buổi) hay tỷ lệ % vắng (ví dụ: vắng từ 20% tổng số buổi) sẽ là tiêu chuẩn cấm thi / cảnh cáo học vụ.
   - [ ] **Quy đổi buổi muộn**: Có quy đổi số lần đi muộn thành vắng hay không (ví dụ: 3 lần muộn = 1 lần vắng).
   - [ ] **Xử lý vắng có phép**: Có tính buổi vắng có phép vào giới hạn vắng hay miễn trừ hoàn toàn.
   - [ ] **Phạm vi áp dụng**: Áp dụng chung một ngưỡng cho toàn trường hay phân theo số tín chỉ của từng học phần.

---

## 4. Danh sách Endpoint API bổ sung

| Method | Endpoint | Quyền hạn | Mô tả |
|---|---|---|---|
| `GET` | `/api/giang-vien/lop-hoc-phan/{lopId}/canh-bao` | `GIANG_VIEN`, `QUAN_TRI` | Lấy danh sách cảnh báo và thông tin ngưỡng của lớp (kiểm tra quyền giảng viên). |
| `PUT` | `/api/giang-vien/lop-hoc-phan/{lopId}/cau-hinh-canh-bao` | `GIANG_VIEN`, `QUAN_TRI` | Cập nhật ngưỡng số buổi vắng / tỷ lệ vắng cho lớp học phần. |
| `GET` | `/api/giang-vien/canh-bao-diem-danh` | `GIANG_VIEN`, `QUAN_TRI` | Danh sách tất cả cảnh báo thuộc các lớp giảng viên phụ trách. |
| `GET` | `/api/sinh-vien/me/canh-bao-diem-danh` | `SINH_VIEN` | Sinh viên tự xem các cảnh báo chuyên cần của bản thân. |

---

## 5. Giao diện người dùng

1. **Tab 3 trên trang Quản lý Điểm danh của Giảng viên (`/lop-cua-toi/:id/diem-danh`)**:
   - Huy hiệu (badge) hiển thị số lượng sinh viên đang bị cảnh báo ngay trên tiêu đề tab.
   - Thẻ thống kê: Buổi hợp lệ, Sinh viên đang cảnh báo, Ngưỡng số buổi và Ngưỡng tỷ lệ vắng.
   - Khung thông báo quy chế: nguồn ngưỡng đang áp dụng và ghi chú quy chế chờ duyệt.
   - Nút "Cấu hình ngưỡng lớp" mở modal thiết lập nhanh `nguongSoBuoiVang` và `nguongTyLeVang`.
   - Bảng danh sách cảnh báo: đầy đủ thông tin sinh viên, buổi vắng / buổi hợp lệ, tỷ lệ vắng, mức ngưỡng, trạng thái (Đang cảnh báo / Đã giải tỏa), chi tiết thông điệp và thời điểm cập nhật.
2. **Trang Chuyên cần của Sinh viên (`/chuyen-can-cua-toi`)**:
   - Banner cảnh báo chuyên cần nổi bật khi sinh viên có cảnh báo đang ở trạng thái `DANG_CANH_BAO`.
   - Nêu rõ môn học, lớp học phần, số buổi vắng, tổng buổi hợp lệ và ngưỡng vi phạm.
