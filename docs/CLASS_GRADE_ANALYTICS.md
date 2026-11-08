# Thống kê điểm lớp học phần và phân bố điểm (QLSV-75 / TASK-LC-10)

Tài liệu này mô tả định nghĩa các chỉ số thống kê, mẫu số, khoảng phân bố điểm, quyền truy cập theo vai trò, cách tách biệt dữ liệu đã công bố với dữ liệu chưa công bố, và các quy tắc cấu hình tập trung cần Chủ dự án chốt chính thức.

---

## 1. Phạm vi trạng thái bảng điểm và tách biệt dữ liệu công bố / chưa công bố

Hệ thống tuân thủ chặt chẽ trạng thái bảng điểm của `QLSV-74` (`GradeSheetStatus`):

1. **Thống kê chính thức (`phamViDuLieu = "DA_CONG_BO"`, `daCongBoChinhThuc = true`)**:
   - **Chỉ tính** trên các lớp học phần có bảng điểm ở trạng thái **`DA_CONG_BO`** hoặc **`DA_KHOA`** (`CourseClass.isBangDiemDaCongBo() == true`).
   - Toàn bộ khối tổng hợp chung toàn phạm vi lọc (`tongHopCongBo`) và khối tổng hợp theo môn học (`theoMon[*].thongKeCongBo`) **chỉ tổng hợp từ các lớp đã công bố**.
   - Các lớp đang ở trạng thái làm việc (`NHAP`, `CHO_DUYET`, `TRA_VE`, `DA_DUYET`) có `thongKeCongBo.soBanGhiDuocTinh = 0` và toàn bộ sinh viên đăng ký của lớp đó được xếp vào nhóm ngoài mẫu số (`soSinhVienChuaCoDiem`), tuyệt đối **không trộn** vào thống kê công khai/chính thức.
2. **Thống kê tạm tính theo trạng thái làm việc hiện tại (`phamViDuLieu = "TAM_TINH_THEO_TRANG_THAI_HIEN_TAI"`, `daCongBoChinhThuc = false`)**:
   - Đối với lớp chưa công bố (`NHAP`, `CHO_DUYET`, `TRA_VE`, `DA_DUYET`), giảng viên phụ trách lớp hoặc quản trị viên được cung cấp thêm trường riêng `thongKeTamTinhChuaCongBo` ở cấp từng lớp (`ThongKeDiemLopItemDto`).
   - Trên giao diện, khối này mặc định ẩn sau nút **"Xem tạm tính"** và khi mở ra luôn hiển thị băng cảnh báo màu vàng ghi rõ trạng thái hiện tại của bảng điểm và khẳng định dữ liệu chưa công bố không được gộp vào thống kê chính thức.

---

## 2. Định nghĩa từng chỉ số và mẫu số

Chỉ các bản ghi đăng ký ở trạng thái hiệu lực (`RegistrationStatus.DA_DANG_KY`) mới được xét trong quy mô lớp học phần (`RegistrationStatus.DA_HUY` bị loại hoàn toàn ngay tại truy vấn cơ sở dữ liệu).

| Chỉ số | Trường JSON (`ChiSoThongKeDiemDto`) | Định nghĩa và công thức |
|---|---|---|
| **Tổng số đăng ký hiệu lực** | `tongSoDangKyHieuLuc` ($N_{\text{đk}}$) | Tổng số bản ghi đăng ký có `trangThai = DA_DANG_KY` thuộc các lớp trong phạm vi đang xét. |
| **Số bản ghi được tính (Mẫu số)** | `soBanGhiDuocTinh` / `soSinhVienCoDiem` / `mauSoTinhTrungBinhVaTyLe` ($M$) | Số bản ghi thuộc lớp đủ điều kiện trạng thái bảng điểm **và** có `diemTongKet != null`, hữu hạn, nằm trong đoạn $[0.0, 10.0]$. Đây là **mẫu số duy nhất** cho điểm trung bình, tỷ lệ đạt/trượt và tỷ lệ phần trăm từng khoảng phân bố. |
| **Số sinh viên chưa có điểm (Ngoài mẫu số)** | `soSinhVienChuaCoDiem` | $N_{\text{đk}} - M$. Sinh viên chưa có điểm công bố **tuyệt đối không bị tính ngầm thành `0.0`**. Chỉ số này được tách chi tiết thành:<ul><li>`soSinhVienChuaNhapDuDiem`: số bản ghi chưa có `diemTongKet` hợp lệ (`null`).</li><li>`soSinhVienChuaCongBoDiem`: số bản ghi đã có `diemTongKet` tạm thời nhưng thuộc lớp chưa công bố bảng điểm.</li></ul> |
| **Điểm trung bình** | `diemTrungBinh` | Nếu $M = 0$ thì trả `null`. Nếu $M > 0$: $$\text{diemTrungBinh} = \text{round}_{\text{HALF\_UP}, 2}\left(\frac{\sum_{i=1}^{M} x_i}{M}\right)$$ tính bằng `BigDecimal` và chỉ làm tròn 2 chữ số thập phân khi xuất kết quả. |
| **Điểm thấp nhất / cao nhất** | `diemThapNhat`, `diemCaoNhat` | Giá trị nhỏ nhất ($\min$) và lớn nhất ($\max$) trong $M$ bản ghi hợp lệ. Trả `null` nếu $M = 0$. |
| **Ngưỡng điểm đạt** | `nguongDat` | Dùng đúng hằng số hiện hành của dự án `AcademicRules.DIEM_DAT = 5.0` (không tự đặt ngưỡng khác). |
| **Số sinh viên Đạt / Trượt** | `soSinhVienDat` ($M_{\text{đạt}}$), `soSinhVienTruot` ($M_{\text{trượt}}$) | Trong $M$ bản ghi hợp lệ:<ul><li>Đạt: $x_i \ge 5.0$</li><li>Trượt: $x_i < 5.0$</li></ul>Luôn thỏa $M_{\text{đạt}} + M_{\text{trượt}} = M$. |
| **Tỷ lệ Đạt / Trượt (%)** | `tyLeDat`, `tyLeTruot` | Nếu $M = 0$ thì trả `0.0`. Nếu $M > 0$: $$\text{tyLeDat} = \text{round}_{\text{HALF\_UP}, 2}\left(\frac{M_{\text{đạt}} \times 100}{M}\right), \quad \text{tyLeTruot} = \text{round}_{\text{HALF\_UP}, 2}\left(\frac{M_{\text{trượt}} \times 100}{M}\right)$$ |
| **Tổng số bản ghi trong phân bố** | `tongSoTrongPhanBo` | Tổng `soLuong` của tất cả các khoảng điểm trong `phanBoDiem`. Luôn khớp 100% với `soBanGhiDuocTinh` ($M$). |

---

## 3. Khoảng phân bố điểm (Không chồng lấn, không bỏ sót giá trị biên)

Các khoảng phân bố được cấu hình tập trung tại `GradeAnalyticsProperties` (`app.grade-analytics.buckets` trong `application.yml`) và được kiểm tra tính liên tục/không chồng lấn ngay khi khởi tạo `GradeAnalyticsService`:

| Mã khoảng (`maKhoang`) | Ký hiệu (`kyHieuKhoang`) | Mô tả (`tenKhoang`) | Điều kiện toán học chính xác | Giá trị biên mẫu | Thuộc nhóm Đạt (`>= 5.0`) |
|---|---|---|---|---|---|
| `KEM` | `[0.0, 4.0)` | Dưới 4.0 | $0.0 \le x < 4.0$ | `0.0`, `3.99` | Không (`false`) |
| `YEU` | `[4.0, 5.0)` | 4.0 đến dưới 5.0 | $4.0 \le x < 5.0$ | `4.0`, `4.99` | Không (`false`) |
| `TRUNG_BINH` | `[5.0, 6.5)` | 5.0 đến dưới 6.5 | $5.0 \le x < 6.5$ | `5.0`, `6.49` | Có (`true`) |
| `KHA` | `[6.5, 8.0)` | 6.5 đến dưới 8.0 | $6.5 \le x < 8.0$ | `6.5`, `7.99` | Có (`true`) |
| `GIOI_XUAT_SAC` | `[8.0, 10.0]` | 8.0 đến 10.0 | $8.0 \le x \le 10.0$ | `8.0`, `10.0` | Có (`true`) |

- Mọi giá trị điểm hợp lệ $x \in [0.0, 10.0]$ luôn rơi vào **đúng 1 và chỉ 1** khoảng.
- Tổng `soLuong` của 5 khoảng luôn bằng đúng `soBanGhiDuocTinh` ($M$).

---

## 4. Phân quyền truy cập và bảo vệ danh tính sinh viên (PII)

1. **Giảng viên (`Role.GIANG_VIEN`)**:
   - Endpoint tổng hợp: `GET /api/giang-vien/thong-ke-diem?hocKyId=&monHocId=&lopHocPhanId=` (hoặc `/api/giang-vien/me/thong-ke-diem`).
   - Endpoint theo lớp: `GET /api/giang-vien/lop-hoc-phan/{id}/thong-ke-diem`.
   - Backend tự động gắn phạm vi `giangVien.id` của giảng viên đang đăng nhập. Nếu truyền `lopHocPhanId` của lớp do giảng viên khác phụ trách, backend trả `403 Forbidden`.
   - Giảng viên gọi `/api/admin/thong-ke/diem-lop` bị từ chối `403 Forbidden`.
2. **Quản trị viên (`Role.QUAN_TRI`)**:
   - Endpoint tổng hợp: `GET /api/admin/thong-ke/diem-lop?hocKyId=&khoaId=&monHocId=&lopHocPhanId=` (hoặc `/api/admin/thong-ke-diem`).
   - Endpoint theo lớp: `GET /api/admin/lop-hoc-phan/{id}/thong-ke-diem`.
   - Hỗ trợ lọc theo học kỳ, khoa, môn học và lớp học phần trên toàn trường theo chính sách quản trị hiện có.
3. **Sinh viên (`Role.SINH_VIEN`)**:
   - Không có quyền truy cập các API thống kê điểm lớp/môn của giảng viên và quản trị (`403 Forbidden`).
4. **Không rò rỉ danh tính sinh viên (Zero Student PII) & Drill-down theo QLSV-67**:
   - Truy vấn thống kê tại `CourseRegistrationRepository.findDiemTheoDanhSachLop` chỉ chiếu `(lopHocPhanId, diemQuaTrinh, diemCuoiKy, diemTongKet)` mà không join hay nạp thực thể `Student` / `User`.
   - Toàn bộ DTO trong `GradeAnalyticsDtos` hoàn toàn không chứa `sinhVienId`, `maSinhVien`, `hoTen`, `email` hay `soDienThoai` của sinh viên.
   - Khi người dùng bấm drill-down **"Bảng điểm lớp"** (`duongDanDrillDown = "/lop-cua-toi/{id}/nhap-diem"`), trang đích gọi API `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien` và `/api/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}` vốn được bảo vệ bởi quy tắc phạm vi xem sinh viên của `QLSV-67`.

---

## 5. Các quy tắc cấu hình tập trung cần Chủ dự án chốt chính thức

Do tài liệu SRS gốc chưa quy định cứng các mốc chia khoảng phân bố điểm và chính sách xử lý bản ghi thiếu điểm trong báo cáo thống kê lớp, hệ thống đã gom các tham số này vào cấu hình tập trung `GradeAnalyticsProperties` (`app.grade-analytics.*` trong `application.yml`) kèm cờ `laQuyCheChinhThuc = false` (**không gọi đây là quy chế chính thức**):

1. **Các mốc khoảng phân bố điểm (`app.grade-analytics.buckets`)**: hiện cấu hình 5 khoảng liên tiếp `[0.0, 4.0)`, `[4.0, 5.0)`, `[5.0, 6.5)`, `[6.5, 8.0)`, `[8.0, 10.0]` bám theo các mốc quy đổi điểm hệ 4 hiện có trong `AcademicRules`. Cần Chủ dự án xác nhận giữ 5 khoảng này hay tách riêng khoảng `[5.0, 5.5)`, `[7.0, 8.0)`, `[8.5, 10.0]`.
2. **Chính sách xử lý sinh viên chưa có điểm (`app.grade-analytics.missing-grade-policy = EXCLUDE_FROM_DENOMINATOR`)**: hiện loại bản ghi chưa có điểm công bố khỏi mẫu số tính điểm trung bình và tỷ lệ đạt/trượt, đồng thời hiển thị rõ số lượng `soSinhVienChuaCoDiem` bên cạnh mẫu số. Cần Chủ dự án xác nhận giữ nguyên hay bổ sung thêm cột tỷ lệ đạt tính trên tổng đăng ký hiệu lực.
