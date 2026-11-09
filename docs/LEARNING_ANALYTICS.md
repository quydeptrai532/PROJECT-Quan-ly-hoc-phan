# Phân tích Học tập cơ bản - Learning Analytics (QLSV-98 / TASK-OP-11)

Tài liệu này đặc tả toàn bộ kiến trúc, từ điển KPI, công thức tính toán, quy tắc so sánh lớp, chính sách xử lý dữ liệu thiếu, phân quyền và xuất báo cáo của hệ thống **Learning Analytics cơ bản** (mã công việc: `QLSV-98` / `TASK-OP-11`).

---

## 1. Khảo sát & Tái sử dụng Từ điển KPI từ TASK-AD-11 (QLSV-86)

Hệ thống kế thừa 100% định nghĩa KPI chuẩn đã ban hành tại [`docs/KPI_DICTIONARY.md`](KPI_DICTIONARY.md) và cơ chế tính toán tại [`docs/CLASS_GRADE_ANALYTICS.md`](CLASS_GRADE_ANALYTICS.md):

| Tên chỉ số | Mã KPI | Tử số | Mẫu số ($M$) | Trạng thái áp dụng | Đơn vị & Quy chuẩn làm tròn |
|---|---|---|---|---|---|
| **Số SV có điểm công bố (Mẫu số)** | `KPI_SO_SV_CO_DIEM` | Đếm bản ghi có `diemTongKet` hợp lệ $\in [0.0, 10.0]$ | Không áp dụng | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | Sinh viên (nguyên dương) |
| **Số SV chưa có điểm** | `KPI_SV_CHUA_CO_DIEM` | $N_{\text{đk}} - M$ ($N_{\text{đk}}$ là tổng lượt đăng ký hiệu lực `DA_DANG_KY`) | Không áp dụng | Lớp chưa công bố hoặc chưa nhập điểm | Sinh viên (nguyên dương) |
| **Điểm trung bình (Thang 10)** | `KPI_DIEM_TB_10` | $\sum_{i=1}^{M} x_i$ ($x_i$ là điểm tổng kết) | $M$ (Số SV có điểm công bố) | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | Thang 10, làm tròn `HALF_UP` 2 chữ số (`null` nếu $M = 0$) |
| **GPA tương đương (Thang 4)** | `KPI_GPA_4` | $\sum_{i=1}^{M} (\text{quyDoiHe4}(x_i) \times \text{soTinChi}_i)$ | $\sum \text{soTinChi}_i$ hoặc $M$ | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | Thang 4, làm tròn `HALF_UP` 2 chữ số (`null` nếu $M = 0$) |
| **Tỷ lệ Đạt (≥ 5.0)** | `KPI_TY_LE_DAT` | $M_{\text{đạt}} = \sum \mathbb{I}(x_i \ge 5.0)$ | $M$ (Số SV có điểm công bố) | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | Phần trăm (`%`), 2 chữ số thập phân (`0.0%` nếu $M = 0$) |
| **Tỷ lệ Trượt (< 5.0)** | `KPI_TY_LE_TRUOT` | $M_{\text{trượt}} = \sum \mathbb{I}(x_i < 5.0)$ | $M$ (Số SV có điểm công bố) | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | Phần trăm (`%`), 2 chữ số thập phân (`0.0%` nếu $M = 0$) |

### 1.1. Chính sách xử lý dữ liệu thiếu (Missing Data Policy)
- **Quy tắc**: `EXCLUDE_FROM_DENOMINATOR`.
- Bản ghi chưa có điểm tổng kết (`diemTongKet == null`) hoặc thuộc các lớp đang làm việc (`NHAP`, `CHO_DUYET`, `TRA_VE`, `DA_DUYET`) bị **loại hoàn toàn khỏi mẫu số $M$**.
- **Tuyệt đối không tự động đổi điểm thiếu thành `0.0`**.
- Toàn bộ sinh viên chưa có điểm công bố được hiển thị rõ ràng bên cạnh mẫu số dưới chỉ số `soSinhVienChuaCoDiem`.

### 1.2. 5 Khoảng phân bố điểm chuẩn
Hệ thống sử dụng 5 khoảng liên tục, không chồng lấn và không bỏ sót giá trị biên:
1. `[0.0, 4.0)`: Kém ($0.0 \le x < 4.0$) - Chưa đạt
2. `[4.0, 5.0)`: Yếu ($4.0 \le x < 5.0$) - Chưa đạt
3. `[5.0, 6.5)`: Trung bình ($5.0 \le x < 6.5$) - Đạt
4. `[6.5, 8.0)`: Khá ($6.5 \le x < 8.0$) - Đạt
5. `[8.0, 10.0]`: Giỏi / Xuất sắc ($8.0 \le x \le 10.0$) - Đạt

### 1.3. Các điểm lưu ý quy chế đào tạo
- **Học lại / cải thiện**: Hiện tính trên tất cả các lượt học có điểm công bố trong phạm vi lọc; chưa chốt quy chế lấy điểm cao nhất hay lần học cuối cùng.
- **Quy đổi khoảng [5.0, 5.5)**: Đang quy đổi `1.5` (D+) theo `AcademicRules.DIEM_DAT`.
- Các tham số cấu hình được đánh dấu rõ trong DTO/báo cáo để Chờ Hội đồng Đào tạo chuẩn hóa bằng văn bản.

---

## 2. Xu hướng GPA theo học kỳ (Semester GPA Trend)

- Truy vấn toàn bộ các học kỳ trong hệ thống theo thứ tự thời gian tăng dần (`ngayBatDau asc`).
- Với mỗi học kỳ:
  - Tính tổng số lớp, số lớp công bố, tổng đăng ký, mẫu số $M$, sinh viên chưa có điểm.
  - Điểm trung bình thang 10, GPA thang 4, tỷ lệ Đạt/Trượt.
  - Nếu học kỳ chưa có lớp nào công bố điểm ($M = 0$), Điểm TB và GPA trả về `null` (trên biểu đồ hiển thị trạng thái ngắt quãng/rỗng, **không vẽ dìm về 0.0**).

---

## 3. So sánh các lớp cùng môn học (Course Section Comparison)

Nguyên tắc so sánh khách quan và công bằng:
1. **Cùng phạm vi**: Chỉ so sánh các lớp của **cùng một môn học** trong **cùng một học kỳ**.
2. **Cùng trạng thái công bố**: Chỉ đưa vào so sánh chính thức các lớp đã ở trạng thái **`DA_CONG_BO`** hoặc **`DA_KHOA`**.
3. **Tách riêng lớp chưa công bố**: Các lớp `NHAP`, `CHO_DUYET`, `TRA_VE`, `DA_DUYET` được gắn cờ `duDieuKienSoSanhChinhThuc = false` và kèm nhãn cảnh báo: *"Bảng điểm chưa công bố - Không được tính vào so sánh chính thức"*.
4. **Ngưỡng cỡ mẫu tối thiểu ($M \ge 5$)**:
   - Nếu $M < 5$: Hệ thống gắn cờ `duSoMauToiThieu = false` và hiển thị cảnh báo: *"Cỡ mẫu nhỏ (M = ... < 5): số liệu phân bố có thể biến động lớn, cần thận trọng khi đối chiếu"*.
   - Chỉ khi cả 2 điều kiện thỏa mãn (`duDieuKienSoSanhChinhThuc && duSoMauToiThieu`) lớp mới được tính vào số lớp đủ điều kiện so sánh.
5. **Điều kiện kết luận so sánh**: Phải có tối thiểu 2 lớp đủ điều kiện (`soLopDuDieuKienSoSanh >= 2`) mới hiển thị biểu đồ đối chiếu.

---

## 4. Tuyên bố từ chối kết luận nhân quả (Non-causality Disclaimer)

> [!WARNING]
> **CẢNH BÁO PHÂN TÍCH HỌC THUẬT:**  
> Toàn bộ số liệu trên dashboard, biểu đồ và file xuất CSV thuần túy mô tả thống kê phân bố điểm thực tế.  
> **Tuyệt đối không sử dụng số liệu mô tả này để suy diễn kết luận nhân quả** về năng lực giảng dạy của giảng viên, trình độ sinh viên hay mức độ khó của đề thi mà không qua nghiên cứu thực nghiệm kiểm soát các biến ngoại lai (thành phần sinh viên, hình thức thi cử, điều kiện giảng dạy, thời khóa biểu...).

---

## 5. Phân quyền và Bảo mật dữ liệu (Zero Student PII)

1. **Quản trị viên (`QUAN_TRI`)**:
   - Quyền truy cập toàn trường, lọc linh hoạt theo Học kỳ, Khoa, Ngành, Môn học, Lớp học phần.
   - Endpoint: `/api/admin/learning-analytics/**`.
2. **Giảng viên (`GIANG_VIEN`)**:
   - Chỉ được xem số liệu trong phạm vi các môn học và lớp học phần mà mình được phân công phụ trách.
   - Nếu cố tình truyền `monHocId` của môn không phụ trách, backend trả về `403 Forbidden`.
   - Endpoint: `/api/giang-vien/learning-analytics/**`.
3. **Sinh viên (`SINH_VIEN`)**:
   - Hoàn toàn không có quyền truy cập vào các API phân tích học tập quản trị và giảng viên (`403 Forbidden`).
4. **Bảo vệ danh tính cá nhân (Zero PII)**:
   - Các API và DTO tuyệt đối không nạp hay trả về `sinhVienId`, `maSinhVien`, `hoTen`, `email`, `soDienThoai` của sinh viên.

---

## 6. Quy chuẩn Xuất báo cáo CSV (Export Standard)

1. **Encoding**: UTF-8 có BOM (`\uFEFF`) chống lỗi font tiếng Việt trên Microsoft Excel.
2. **Khối Header & Metadata**:
   - Tên báo cáo: `BÁO CÁO PHÂN TÍCH HỌC TẬP (LEARNING ANALYTICS - QLSV)`
   - Thời điểm xuất, Người thực hiện, Bộ lọc đang áp dụng (Học kỳ, Khoa, Ngành, Môn).
   - Tuyên bố chính sách điểm thiếu và khuyến cáo không suy diễn nhân quả.
3. **Phần 1**: Tổng quan KPI (Tử số, Mẫu số, Giá trị, Đơn vị, Ghi chú).
4. **Phần 2**: Phân bố 5 khoảng điểm chuẩn.
5. **Phần 3**: Bảng xu hướng GPA theo từng học kỳ.
6. **Phần 4**: Bảng so sánh các lớp cùng môn học (khi lọc theo môn).
