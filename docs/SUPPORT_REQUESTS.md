# Yêu cầu hỗ trợ / khiếu nại sinh viên (QLSV-64 — TASK-ST-08)

## 1. Tổng quan chức năng

- **Sinh viên (`SINH_VIEN`)**:
  - Tạo phiếu hỗ trợ/khiếu nại với loại yêu cầu thuộc danh mục hợp lệ, tiêu đề (5–200 ký tự), nội dung gốc (10–4000 ký tự).
  - Tùy chọn liên kết đối tượng học vụ: môn học (`monHocId`), học kỳ (`hocKyId`) hoặc bản ghi điểm của chính mình (`diemId`).
  - Tùy chọn đính kèm tệp minh chứng theo giới hạn cấu hình.
  - Xem danh sách phiếu của chính mình (có lọc theo trạng thái, loại yêu cầu, từ khóa và phân trang), xem chi tiết phiếu, lịch sử chuyển trạng thái và trao đổi phản hồi.
  - Nội dung gốc (`tieuDe`, `noiDungGoc`) được khóa sau khi gửi; các phản hồi tiếp theo được lưu tách biệt tại bảng `yeu_cau_ho_tro_trao_doi`.
- **Quản trị viên (`QUAN_TRI`)**:
  - Tra cứu, lọc và phân trang danh sách toàn bộ phiếu hỗ trợ.
  - Xem chi tiết phiếu, tải xuống tệp minh chứng, gửi phản hồi trao đổi và cập nhật trạng thái xử lý theo luồng hợp lệ (ghi nhận người thực hiện, vai trò, thời điểm và ghi chú vào `yeu_cau_ho_tro_lich_su_trang_thai`).

## 2. Kiểm soát truy cập & Bảo mật tệp đính kèm

1. **Xác thực người tạo từ Principal**:
   - Không nhận `studentId`, `sinhVienId`, `nguoiTaoId`, `nguoiXuLyId` từ phía client; luôn truy xuất tài khoản từ `CurrentUser` (`SecurityContextHolder`).
2. **Chống IDOR**:
   - Sinh viên chỉ được xem chi tiết, thêm trao đổi, tải tệp đính kèm trên phiếu của chính mình và chỉ được gắn `diemId` thuộc kết quả học tập của chính mình. Mọi truy cập sang ID của sinh viên khác bị từ chối với `403 Forbidden`.
3. **Bảo mật tệp minh chứng (`SupportAttachmentStorageService`)**:
   - Kiểm tra phần mở rộng hợp lệ, chặn tên tệp chứa ký tự đường dẫn (`..`, `/`, `\`, `:`, ký tự điều khiển) và chặn phần mở rộng kép chứa định dạng thực thi/kịch bản (`.exe`, `.php`, `.jsp`, `.sh`, `.html`, `.svg`, v.v.).
   - Kiểm tra chữ ký nhị phân (magic bytes) thực tế của tệp (`%PDF-` cho PDF, `89 50 4E 47...` cho PNG, `FF D8 FF` cho JPEG) và đối chiếu khớp với phần mở rộng tệp; không tin `Content-Type` do client gửi.
   - Tên lưu trữ trên đĩa do server sinh bằng `UUID`, lưu tại thư mục riêng (`app.support-request.storage-dir`) ngoài thư mục public, chỉ phục vụ qua API có kiểm tra quyền với header `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: default-src 'none'; sandbox` và `Cache-Control: no-store`.
   - Tự động xóa tệp mồ côi trên đĩa nếu giao dịch cơ sở dữ liệu thất bại hoặc rollback.

## 3. Cấu hình tập trung cần chủ dự án xác nhận

Do mã nguồn hiện có trước đó chưa quy định danh mục loại yêu cầu, trạng thái xử lý, giới hạn tệp và SLA chính thức, hệ thống gom cấu hình tập trung tại `app.support-request` (`SupportRequestProperties` / `application.yml`) với các giá trị mặc định an toàn tạm thời:

| Cấu hình | Biến môi trường | Giá trị mặc định tạm thời | Trạng thái xác nhận |
|---|---|---|---|
| `app.support-request.allowed-types` | `SUPPORT_ALLOWED_TYPES` | `HOC_PHAN,DIEM_SO,HOC_KY,HO_SO_HOC_VU,KHAC` | Cần chủ dự án xác nhận danh mục chính thức |
| `app.support-request.allowed-statuses` | `SUPPORT_ALLOWED_STATUSES` | `MOI_TAO,DANG_XU_LY,CHO_PHAN_HOI_SINH_VIEN,DA_GIAI_QUYET,TU_CHOI,DA_DONG` | Cần chủ dự án xác nhận luồng trạng thái chính thức |
| `app.support-request.comment-allowed-statuses` | `SUPPORT_COMMENT_ALLOWED_STATUSES` | `MOI_TAO,DANG_XU_LY,CHO_PHAN_HOI_SINH_VIEN,DA_GIAI_QUYET` | Cần chủ dự án xác nhận chính sách đóng phản hồi |
| `app.support-request.max-file-size-bytes` | `SUPPORT_MAX_FILE_SIZE_BYTES` | `5242880` (5 MB / tệp) | Cần chủ dự án xác nhận giới hạn dung lượng |
| `app.support-request.max-files-per-request` | `SUPPORT_MAX_FILES_PER_REQUEST` | `3` tệp / phiếu | Cần chủ dự án xác nhận số lượng tệp tối đa |
| `app.support-request.allowed-extensions` | `SUPPORT_ALLOWED_EXTENSIONS` | `pdf,png,jpg,jpeg` | Cần chủ dự án xác nhận loại tệp minh chứng |
| `app.support-request.sla-note` | `SUPPORT_SLA_NOTE` | *(Để trống — không tuyên bố SLA chính thức)* | Cần chủ dự án quyết định SLA xử lý nếu áp dụng |


## 4. Hàng đợi xử lý hỗ trợ sinh viên (QLSV-87 — TASK-AD-12)

### 4.1. Vai trò xử lý & Phạm vi hàng đợi (Scope)
- **Cơ chế phân quyền**:
  - Không mở quyền tự do. Cấu hình vai trò xử lý mặc định là `QUAN_TRI` (`app.support-request.queue.handler-roles: [QUAN_TRI]`).
  - Nhân viên/Cán bộ quản trị truy cập hàng đợi tập trung tại `/admin/yeu-cau-ho-tro/queue` và `/admin/yeu-cau-ho-tro/queue/summary`.
  - Sinh viên truy cập vào API hàng đợi nhân viên sẽ bị từ chối với mã lỗi `403 Forbidden`.
- **Các phạm vi lọc hàng đợi (Queue Scope)**:
  - `ALL`: Toàn bộ các phiếu trong hàng đợi thuộc trạng thái đang xử lý (`MOI_TAO`, `DANG_XU_LY`, `CHO_PHAN_HOI_SINH_VIEN`).
  - `MY_ASSIGNED`: Các phiếu đang được phân công trực tiếp cho tài khoản đăng nhập hiện tại.
  - `UNASSIGNED`: Các phiếu chưa được phân công người xử lý (`nguoiXuLy IS NULL`).
  - `OVERDUE`: Các phiếu đang mở đã vượt quá thời hạn xử lý hoặc quá hạn tính theo SLA lịch làm việc.

### 4.2. Vòng đời xử lý phiếu & Thao tác nghiệp vụ
1. **Phân loại & Tiếp nhận (Triage)**:
   - Cán bộ quản lý có thể điều chỉnh mức độ ưu tiên (`mucUuTien`: `THAP`, `TRUNG_BINH`, `CAO`, `KHAN_CAP`), hạn xử lý (`hanXuLy`), và loại yêu cầu (`loaiYeuCau`).
   - Mọi thao tác điều chỉnh bắt buộc phải cung cấp lý do (`ghiChu`) và được ghi nhận vào lịch sử thay đổi (`SupportRequestStatusHistory`) với loại thay đổi `DIEU_CHINH_TRIAGE`.
2. **Gán người xử lý (Assign Handler)**:
   - Cán bộ có thể gán hoặc chuyển đổi người xử lý từ danh sách cán bộ hợp lệ (`/api/admin/yeu-cau-ho-tro/handlers`).
   - Khi gán cán bộ, nếu phiếu đang ở trạng thái `MOI_TAO`, hệ thống tự động chuyển tiếp sang `DANG_XU_LY`.
   - Lưu vết người thực hiện, cán bộ cũ, cán bộ mới và lý do phân công.
3. **Trao đổi: Phản hồi công khai vs Ghi chú nội bộ**:
   - Trao đổi công khai (`congKhai = true`): Sinh viên và cán bộ đều nhìn thấy.
   - Ghi chú nội bộ (`congKhai = false`, `isNoiBo = true`): Chỉ dành riêng cho cán bộ đào tạo/quản trị trao đổi thông tin nhạy cảm. Sinh viên tuyệt đối không thể xem được ghi chú này qua bất kỳ API nào.
4. **Yêu cầu bổ sung minh chứng**:
   - Khi hồ sơ/minh chứng sinh viên gửi chưa đầy đủ, cán bộ kích hoạt `yeuCauBoSungMinhChung` kèm nội dung hướng dẫn chi tiết và thời hạn bổ sung dự kiến.
   - Trạng thái phiếu tự động chuyển thành `CHO_PHAN_HOI_SINH_VIEN`.
   - **Đồng hồ tính SLA tạm dừng (PAUSED)** cho đến khi sinh viên nộp đủ minh chứng bổ sung.
5. **Sinh viên bổ sung minh chứng**:
   - Sinh viên nộp tài liệu bổ sung qua endpoint riêng `/api/yeu-cau-ho-tro/{id}/bo-sung-minh-chung`.
   - Tệp bổ sung được kiểm tra chặt chẽ về định dạng, dung lượng, chữ ký nhị phân và giới hạn tổng số tệp đính kèm trên phiếu (`max-files-per-request`).
   - Sau khi sinh viên nộp minh chứng, phiếu tự động chuyển về trạng thái `DANG_XU_LY` để cán bộ tiếp tục xử lý, đồng hồ tính SLA tiếp tục hoạt động.
6. **Giải quyết (`DA_GIAI_QUYET`) & Đóng phiếu (`DA_DONG`)**:
   - Cán bộ hoàn tất giải quyết ghi nhận kết quả và lý do. Thời điểm giải quyết (`thoiDiemGiaiQuyet`) được lưu cố định để đối soát SLA.
   - Đóng phiếu (`thoiDiemDong`) khóa khả năng gửi phản hồi bổ sung.

### 4.3. Lịch sử trạng thái & Audit Log (Bất biến)
- Bảng `yeu_cau_ho_tro_lich_su_trang_thai` lưu trữ toàn vẹn toàn bộ các biến động:
  - Loại thay đổi: `KHOI_TAO`, `TRANG_THAI`, `PHAN_CONG`, `MUC_UU_TIEN`, `HAN_XU_LY`, `PHAN_LOAI`, `YEU_CAU_MINH_CHUNG`, `BO_SUNG_MINH_CHUNG`.
  - Giá trị trước và sau khi thay đổi (`giaTriCu`, `giaTriMoi`).
  - Người thực hiện (`User`), vai trò (`Role`), thời điểm (`LocalDateTime`).
  - Lý do thay đổi (`ghiChu`).
- **Nguyên tắc**: Không bao giờ cập nhật ghi đè hoặc xóa bất kỳ dòng lịch sử nào. Lịch sử chỉ được bổ sung nối tiếp (append-only).

### 4.4. Tính toán SLA theo Lịch làm việc (Business Working Calendar)
Hệ thống tích hợp bộ máy tính thời gian làm việc doanh nghiệp `SupportBusinessCalendarService`:
- **Giờ làm việc**: Mặc định 8 giờ/ngày làm việc (buổi sáng `08:00–12:00`, buổi chiều `13:00–17:00`).
- **Nghỉ trưa**: `12:00–13:00` bị loại trừ hoàn toàn khỏi thời gian làm việc.
- **Ngày nghỉ cuối tuần**: Thứ Bảy và Chủ Nhật bị loại trừ khỏi thời gian làm việc.
- **Ngày nghỉ lễ cấu hình**: Danh sách các ngày lễ (`holidays`) được cấu hình tập trung và loại trừ khỏi thời gian làm việc.
- **Thời gian tạm dừng (Paused)**: Toàn bộ thời gian phiếu nằm ở trạng thái `CHO_PHAN_HOI_SINH_VIEN` được trừ ra khỏi tổng thời gian làm việc đã tiêu tốn.
- **Trạng thái SLA**:
  - `ON_TIME`: Đang xử lý, thời gian làm việc còn lại &gt; ngưỡng cảnh báo.
  - `NEARING_DUE`: Đang xử lý, thời gian làm việc còn lại $\le$ ngưỡng cảnh báo (`nearing-due-hours: 4h`).
  - `OVERDUE`: Đang xử lý nhưng đã quá hạn thời gian làm việc SLA hoặc quá hạn deadline tùy chỉnh.
  - `PAUSED`: Đang tạm dừng chờ sinh viên bổ sung minh chứng.
  - `RESOLVED_ON_TIME`: Đã giải quyết trong thời hạn làm việc quy định.
  - `RESOLVED_OVERDUE`: Đã giải quyết nhưng quá hạn thời gian làm việc quy định.

### 4.5. Cấu hình tập trung bổ sung cho QLSV-87 (`application.yml`)
```yaml
app:
  support-request:
    queue:
      default-scope: ALL
      handler-roles:
        - QUAN_TRI
      page-size: 20
    working-calendar:
      zone-id: "Asia/Ho_Chi_Minh"
      morning-start: "08:00"
      morning-end: "12:00"
      afternoon-start: "13:00"
      afternoon-end: "17:00"
      working-days:
        - MONDAY
        - TUESDAY
        - WEDNESDAY
        - THURSDAY
        - FRIDAY
      holidays:
        - "2026-01-01"
        - "2026-04-30"
        - "2026-05-01"
        - "2026-09-02"
    sla:
      official-sla-enabled: false
      nearing-due-hours: 4
      default-resolution-hours:
        THAP: 72
        TRUNG_BINH: 48
        CAO: 24
        KHAN_CAP: 8
```

### 4.6. Các điểm đang chờ Chủ dự án (Project Owner) quyết định
1. **Quyết định vai trò xử lý chính thức (`handler-roles`)**:
   - Hiện tại: Chỉ cấp quyền cho `QUAN_TRI`.
   - Cần quyết định: Có mở rộng quyền xem và xử lý hàng đợi hỗ trợ cho `GIANG_VIEN` (với các phiếu liên quan đến điểm số, lớp học phần phụ trách) hay thành lập một vai trò chuyên biệt mới như `CAN_BO_HO_TRO` / `CHUYEN_VIEN_DAO_TAO` hay không.
2. **Kích hoạt SLA chính thức (`official-sla-enabled`)**:
   - Hiện tại: Đang để `false` (chế độ thử nghiệm, hiển thị số giờ làm việc để đánh giá hiệu suất, không áp dụng chế tài).
   - Cần quyết định: Thời điểm kích hoạt SLA phạt chính thức và chính sách chế tài khi quá hạn.
3. **Thời hạn cam kết SLA cho từng mức ưu tiên (`default-resolution-hours`)**:
   - Đang cấu hình tạm tính: Thấp (72h làm việc - 9 ngày), Trung bình (48h - 6 ngày), Cao (24h - 3 ngày), Khẩn cấp (8h - 1 ngày).
   - Cần quyết định: Chốt khung giờ xử lý cam kết cho từng loại yêu cầu và mức ưu tiên.
4. **Lịch làm việc và Danh sách ngày nghỉ lễ chính thức (`working-calendar`)**:
   - Cần chốt khung giờ làm việc thực tế của Nhà trường và danh sách đầy đủ các ngày nghỉ lễ, Tết hằng năm của đơn vị.
