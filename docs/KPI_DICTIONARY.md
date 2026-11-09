# Từ điển Định nghĩa KPI Báo cáo và Dashboard Quản trị (QLSV-86 / TASK-AD-11)

Tài liệu này chuẩn hóa toàn bộ từ điển các chỉ số hiệu suất chính (KPI - Key Performance Indicators) được sử dụng trên Bảng điều khiển quản trị (Admin Dashboard), các biểu đồ phân tích và chức năng xuất báo cáo thống kê của hệ thống Quản lý Đào tạo (QLSV).

---

## 1. Nguyên tắc cốt lõi & Tính toàn vẹn dữ liệu

1. **Bộ lọc thống nhất (Unified Filter Scope)**:
   - Toàn bộ thẻ KPI tổng quan, biểu đồ trực quan, bảng dữ liệu chi tiết và file xuất báo cáo (CSV/Excel) đều được truy xuất từ **cùng một nguồn định nghĩa và truy vấn nghiệp vụ (Single Source of Truth)**.
   - Các tiêu chí lọc gồm: Học kỳ (`hocKyId`), Khoa (`khoaId`), Ngành (`nganhId`), Môn học (`monHocId`) và Lớp học phần (`lopHocPhanId`).
   - Mọi tham số bộ lọc đều được backend xác thực tính hợp lệ và quan hệ logic (ví dụ: Ngành phải thuộc Khoa đang chọn, Môn học phải thuộc Khoa/Ngành, Lớp học phần phải thuộc Học kỳ và Môn học tương ứng).

2. **Chính sách sử dụng điểm số (Grade Status Policy)**:
   - **Thống kê chính thức**: Chỉ sử dụng điểm từ các lớp học phần có bảng điểm ở trạng thái **`DA_CONG_BO`** hoặc **`DA_KHOA`** (`CourseClass.isBangDiemDaCongBo() == true`).
   - Tuyệt đối **không trộn** điểm của các lớp đang làm việc (`NHAP`, `CHO_DUYET`, `TRA_VE`, `DA_DUYET`) vào số liệu chính thức chung toàn trường, toàn khoa hoặc toàn môn.
   - **Xem tạm tính**: Chỉ cán bộ quản trị hoặc giảng viên phụ trách mới có quyền xem khối tạm tính cấp từng lớp (`thongKeTamTinhChuaCongBo`) với nhãn cảnh báo vàng riêng biệt, tách rời hoàn toàn khỏi thống kê chính thức.

3. **Xử lý thiếu dữ liệu (Missing Data Handling)**:
   - Sinh viên chưa có điểm tổng kết hoặc thuộc lớp chưa công bố bảng điểm bị **loại khỏi mẫu số ($M$)** tính điểm trung bình và tỷ lệ đạt/trượt; tuyệt đối không bị tính ngầm thành `0.0`.
   - Sinh viên chưa được gán chương trình đào tạo được đánh dấu rõ ràng `configured: false`, không gán tỷ lệ hoàn thành tùy ý.
   - Các đăng ký ở trạng thái đã hủy (`RegistrationStatus.DA_HUY`) bị loại bỏ ngay tại câu truy vấn cơ sở dữ liệu.

4. **Bảo vệ quyền riêng tư & thông tin định danh (Zero Student PII)**:
   - Dashboard quản trị và báo cáo thống kê tổng hợp phục vụ công tác điều hành vĩ mô, **hoàn toàn không chứa và không để lộ thông tin định danh cá nhân của sinh viên** (Mã sinh viên, Họ tên, Email, Số điện thoại).
   - Việc xem danh sách sinh viên cụ thể của từng lớp học phần chỉ được thực hiện qua các màn hình chuyên biệt được bảo vệ bởi phân quyền chi tiết (QLSV-67 / QLSV-75).

---

## 2. Bảng Từ điển KPI Chi tiết

| STT | Tên KPI | Mã chỉ số | Tử số | Mẫu số | Đơn vị | Cách xử lý thiếu dữ liệu | Trạng thái điểm áp dụng | Tình trạng duyệt quy chế |
|:---:|---|---|---|---|:---:|---|---|:---:|
| **1** | **Số đăng ký hiệu lực** | `KPI_SO_DANG_KY` | Tổng số bản ghi đăng ký có `trangThai = DA_DANG_KY` của các lớp trong phạm vi bộ lọc. | Không áp dụng (chỉ số rời rạc). | Lượt đăng ký | Loại bỏ toàn bộ bản ghi `DA_HUY`. Không đếm sinh viên đã rút đăng ký. | Không phụ thuộc điểm | **Đã chốt** |
| **2** | **Tỷ lệ lấp đầy** | `KPI_TY_LE_LAP_DAY` | Tổng số lượt đăng ký hiệu lực ($\sum \text{daDangKy}$) của các lớp trong phạm vi bộ lọc. | Tổng chỉ tiêu mở lớp ($\sum \text{siSoToiDa}$) của các lớp trong phạm vi bộ lọc. | Phần trăm (`%`, 1 chữ số thập phân) | Nếu tổng chỉ tiêu = 0, trả về `0.0%`. | Không phụ thuộc điểm | **Đã chốt** |
| **3** | **Tỷ lệ Đạt / Trượt** | `KPI_TY_LE_DAT_TRUOT` | - Đạt: Số sinh viên có $\text{diemTongKet} \ge 5.0$.<br>- Trượt: Số sinh viên có $\text{diemTongKet} < 5.0$. | $M$ = Số sinh viên có `diemTongKet` hợp lệ trong $[0.0, 10.0]$ thuộc lớp đã công bố điểm. | Số lượng sinh viên & Phần trăm (`%`, 2 chữ số thập phân) | Bản ghi chưa có điểm hoặc bảng điểm chưa công bố bị loại khỏi mẫu số $M$, chuyển sang chỉ số `soSinhVienChuaCoDiem`. | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | **Đã chốt** (Ngưỡng 5.0 theo `AcademicRules.DIEM_DAT`) |
| **4** | **Điểm trung bình (GPA)** | `KPI_GPA` | - Thang 10: $\sum (\text{diemTongKet}_i \times \text{soTinChi}_i)$.<br>- Thang 4: $\sum (\text{quyDoiHe4}_i \times \text{soTinChi}_i)$. | Tổng số tín chỉ của các môn có điểm công bố ($\sum \text{soTinChi}_i$). | Điểm số (Thang 10 & Thang 4, 2 chữ số thập phân) | Môn chưa có điểm hoặc bảng điểm chưa công bố không đưa vào tử số và mẫu số. Không chứa PII sinh viên trên dashboard. | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | ⚠️ **Cần chủ dự án duyệt thêm quy tắc chi tiết** |
| **5** | **Tiến độ học tập** | `KPI_TIEN_DO_HOC_TAP` | Tổng số tín chỉ tích lũy đạt (`earnedCredits`) từ các môn học thuộc CTĐT có $\text{diemTongKet} \ge 5.0$ trong bảng điểm đã công bố. | Tổng số tín chỉ yêu cầu của chương trình đào tạo (`requiredCredits`). | Tín chỉ & Phần trăm (`%`, làm tròn số nguyên) | Sinh viên chưa gán CTĐT được đánh dấu `configured: false`. Môn học lại chỉ tính tín chỉ 1 lần; tín chỉ tự chọn bị chặn trần ở yêu cầu nhóm. | Bắt buộc `DA_CONG_BO` hoặc `DA_KHOA` | ⚠️ **Cần chủ dự án duyệt khung mốc năm học** |

---

## 3. Chi tiết Công thức Toán học & Quy tắc Tính toán

### 3.1. Tỷ lệ lấp đầy (Capacity Utilization)
$$\text{tiLeLapDay} = \begin{cases} 0.0 & \text{nếu } \sum \text{siSoToiDa} = 0 \\ \text{round}_{\text{HALF\_UP}, 1}\left(\frac{\sum \text{daDangKy} \times 100}{\sum \text{siSoToiDa}}\right) & \text{nếu } \sum \text{siSoToiDa} > 0 \end{cases}$$
- **Quy tắc phân loại cảnh báo hành động**:
  - `LOP_GAN_DAY`: Lớp có tỷ lệ $\ge 90\%$.
  - `LOP_IT_DANG_KY`: Lớp có tỷ lệ $\le 25\%$.
  - `MON_HET_CHO`: Môn học có tổng đăng ký / tổng chỉ tiêu các lớp $\ge 100\%$.

### 3.2. Điểm trung bình và Tỷ lệ Đạt / Trượt
Gọi $M$ là số bản ghi sinh viên có điểm tổng kết hợp lệ ($x_i \in [0.0, 10.0]$) thuộc các lớp đã công bố bảng điểm (`DA_CONG_BO` hoặc `DA_KHOA`).
$$\text{diemTrungBinh} = \begin{cases} \text{null} & \text{nếu } M = 0 \\ \text{round}_{\text{HALF\_UP}, 2}\left(\frac{\sum_{i=1}^{M} x_i}{M}\right) & \text{nếu } M > 0 \end{cases}$$

$$\text{tyLeDat} = \begin{cases} 0.0 & \text{nếu } M = 0 \\ \text{round}_{\text{HALF\_UP}, 2}\left(\frac{M_{\text{đạt}} \times 100}{M}\right) & \text{nếu } M > 0 \end{cases}, \quad M_{\text{đạt}} = \sum_{i=1}^{M} \mathbb{I}(x_i \ge 5.0)$$

$$\text{tyLeTruot} = \begin{cases} 0.0 & \text{nếu } M = 0 \\ \text{round}_{\text{HALF\_UP}, 2}\left(\frac{M_{\text{trượt}} \times 100}{M}\right) & \text{nếu } M > 0 \end{cases}, \quad M_{\text{trượt}} = \sum_{i=1}^{M} \mathbb{I}(x_i < 5.0)$$
Luôn đảm bảo: $M_{\text{đạt}} + M_{\text{trượt}} = M$.

### 3.3. Quy đổi Thang điểm 4 & Phân loại Học lực GPA
Bảng quy đổi theo quy chế đào tạo đại học hiện đang triển khai trong [`GradeService.java`](../backend/src/main/java/com/edu/registration/service/GradeService.java):

| Điểm hệ 10 ($x$) | Điểm chữ | Điểm quy đổi hệ 4 | Xếp loại học lực | Phân loại Dashboard |
|---|:---:|:---:|:---:|:---:|
| $8.5 \le x \le 10.0$ | A | **4.0** | Xuất sắc | `XUAT_SAC` ($\text{GPA} \ge 3.6$) |
| $8.0 \le x < 8.5$ | B+ | **3.5** | Giỏi | `GIOI` ($3.2 \le \text{GPA} < 3.6$) |
| $7.0 \le x < 8.0$ | B | **3.0** | Khá | `KHA` ($2.5 \le \text{GPA} < 3.2$) |
| $6.5 \le x < 7.0$ | C+ | **2.5** | Trung bình khá | `TRUNG_BINH` ($2.0 \le \text{GPA} < 2.5$) |
| $5.5 \le x < 6.5$ | C | **2.0** | Trung bình | `TRUNG_BINH` ($2.0 \le \text{GPA} < 2.5$) |
| $5.0 \le x < 5.5$ | D+ | **1.5** | Trung bình yếu | `YEU_KEM` ($\text{GPA} < 2.0$) |
| $4.0 \le x < 5.0$ | D | **1.0** | Yếu (Chưa đạt học phần) | `YEU_KEM` ($\text{GPA} < 2.0$) |
| $0.0 \le x < 4.0$ | F | **0.0** | Kém (Học lại) | `YEU_KEM` ($\text{GPA} < 2.0$) |

### 3.4. Tiến độ học tập & Hoàn thành tín chỉ
$$\text{completionPercent} = \begin{cases} \text{null} & \text{nếu } \text{requiredCredits} = 0 \\ \min\left(100, \left\lfloor \frac{\text{earnedCredits} \times 100}{\text{requiredCredits}} \right\rfloor \right) & \text{nếu } \text{requiredCredits} > 0 \end{cases}$$
- Mỗi môn học và số tín chỉ của môn chỉ được tính một lần duy nhất (không cộng dồn môn học lại).
- Tín chỉ của nhóm tự chọn chỉ được tính tối đa đến ngưỡng yêu cầu của nhóm môn (`tinChiYeuCau`).

---

## 4. Các mục còn cần Chủ dự án / Hội đồng Đào tạo phê duyệt chính thức

> [!IMPORTANT]
> Toàn bộ các định nghĩa và công thức trên được ánh xạ trực tiếp từ mã nguồn thực tế đang chạy. Hệ thống **tuyệt đối không tự ý tuyên bố** các tham số dưới đây là quy chế chính thức của Nhà trường cho đến khi có quyết định phê duyệt bằng văn bản:

1. **Quy chế tính GPA khi học lại hoặc học cải thiện**:
   - *Hiện trạng*: Hệ thống lưu điểm của tất cả các lần học theo từng học kỳ. Khi xét môn tiên quyết hoặc môn đã đạt, hệ thống kiểm tra có bất kỳ lần học nào đạt hay không.
   - *Cần duyệt*: Khi tính GPA tích lũy toàn khóa, Nhà trường áp dụng phương án **Lấy điểm cao nhất giữa các lần học** hay **Lấy điểm của lần học cuối cùng**?
2. **Quy đổi điểm hệ 4 cho khoảng điểm [5.0, 5.5)**:
   - *Hiện trạng*: Đang quy đổi thành `1.5` (D+).
   - *Cần duyệt*: Xác nhận ngưỡng quy đổi này có áp dụng thống nhất cho toàn bộ các khoa hay có khoa quy định $5.0 - 5.4$ là điểm D (1.0).
3. **Chính sách xử lý tín chỉ tự chọn học vượt trần**:
   - *Hiện trạng*: `AcademicProgressService` chặn trần ở số tín chỉ yêu cầu của nhóm môn (`groupEarned = Math.min(gt, ge)`).
   - *Cần duyệt*: Số tín chỉ vượt trần này có được tính vào tổng tín chỉ tích lũy chung và điểm GPA toàn khóa hay chỉ ghi chú ngoài bằng tốt nghiệp?
4. **Khung chuẩn tiến độ học tập theo từng năm học (Milestone SLA)**:
   - *Hiện trạng*: Đang tính theo tỷ lệ phần trăm hoàn thành tổng thể toàn khóa.
   - *Cần duyệt*: Ban hành mốc số tín chỉ tối thiểu sinh viên phải tích lũy sau mỗi năm học:
     - Hết Năm 1: Tối thiểu bao nhiêu tín chỉ (ví dụ $\ge 30$ tín chỉ).
     - Hết Năm 2: Tối thiểu bao nhiêu tín chỉ (ví dụ $\ge 60$ tín chỉ).
     - Hết Năm 3: Tối thiểu bao nhiêu tín chỉ (ví dụ $\ge 90$ tín chỉ).
     để hệ thống tự động gắn nhãn "Đúng tiến độ", "Chậm tiến độ" hoặc "Cảnh báo học vụ".
5. **Khoảng chia phân bố điểm môn học**:
   - *Hiện trạng*: Đang cấu hình 5 khoảng liên tiếp `[0.0, 4.0)`, `[4.0, 5.0)`, `[5.0, 6.5)`, `[6.5, 8.0)`, `[8.0, 10.0]` trong `app.grade-analytics.buckets` kèm cờ `laQuyCheChinhThuc = false`.
   - *Cần duyệt*: Xác nhận giữ 5 khoảng này hay điều chỉnh theo thang điểm A, B, C, D, F chuẩn MOET.

---

## 5. Quy chuẩn File Xuất Báo cáo Thống kê (Export Standard)

File xuất báo cáo (`.csv`) tuân thủ nghiêm ngặt các tiêu chuẩn kỹ thuật sau:
1. **Encoding**: UTF-8 có BOM (`\uFEFF`) để Microsoft Excel trên Windows tự động nhận diện font chữ Tiếng Việt không bị lỗi font.
2. **Khối Metadata đầu file**:
   - Tên báo cáo: `BÁO CÁO THỐNG KÊ VÀ DASHBOARD ĐÀO TẠO (QLSV)`.
   - Thời điểm xuất báo cáo: `YYYY-MM-DD HH:mm:ss`.
   - Cán bộ thực hiện: Username và vai trò quản trị viên (`QUAN_TRI`).
   - Tiêu chí bộ lọc đang áp dụng: Học kỳ, Khoa, Ngành, Môn học, Lớp học phần.
   - Phạm vi dữ liệu điểm: `ĐÃ CÔNG BỐ (Chính thức)`.
3. **Khối Tổng quan KPI**: Liệt kê 5 chỉ số cốt lõi kèm Tử số, Mẫu số, Giá trị, Đơn vị và Ghi chú.
4. **Khối Dữ liệu Chi tiết**: Thống kê chi tiết theo từng môn học / lớp học phần với tiêu đề cột rõ ràng, lấy từ cùng truy vấn với biểu đồ và bảng trên web.
5. **Bảo mật**: Không chứa bất kỳ cột định danh sinh viên nào (Mã SV, Họ tên, Email).
