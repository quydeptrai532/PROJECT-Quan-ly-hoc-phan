# Kế hoạch kiểm thử

Tài liệu này tổng hợp test case theo FR/NFR, cách chạy và kết quả thực tế của dự án.

## 1. Môi trường kiểm thử

| Thành phần | Cấu hình                                                                              |
|---|---------------------------------------------------------------------------------------|
| Backend | Spring Boot 3.4, profile `h2` (in-memory), chạy trên JDK 21                                            |
| Frontend | Vite dev server `http://127.0.0.1:5173`                                               |
| Trình duyệt | Google Chrome (tự động), Microsoft Edge (tự động), Firefox/Safari (kiểm thử thủ công) |
| Dữ liệu | Seed tự động: `admin/admin123`, `gv01..gv03/123456`, `sv01..sv20/123456`              |
| Công cụ | JUnit 5 + Mockito, Spring Boot HTTP integration test, Playwright, Node.js load test |

- Máy chạy test: Windows, Intel Core i5-8350U 1,70 GHz, RAM 7,8 GB.
- Java: OpenJDK Corretto 22.0.2; Node.js v22.18.0.
- Backend: Spring Boot, profile h2 (database trong bộ nhớ), chạy cùng máy với công cụ test và IntelliJ.
## 2. Unit test backend

Chạy: `cd backend && ./mvnw test` — **23/23 unit test pass** (19 nghiệp vụ hiện có, 4 audit log).

| Nhóm | Nội dung kiểm thử | Kết quả |
|---|---|---|
| Đăng ký | Đăng ký thành công, tăng sĩ số, tạo bản ghi `DA_DANG_KY` | Pass |
| Đăng ký | Chặn ngoài thời gian đăng ký | Pass |
| Đăng ký | Chặn đăng ký trùng môn trong học kỳ | Pass |
| Đăng ký | Chặn trùng thứ/tiết | Pass |
| Đăng ký | Chặn khi lớp đủ sĩ số | Pass |
| Đăng ký | Chặn vượt tín chỉ tối đa | Pass |
| Đăng ký | Chặn thiếu môn tiên quyết | Pass |
| Đăng ký | Cho phép khi đã đạt môn tiên quyết | Pass |
| Đăng ký | Đăng ký lại sau khi hủy (kích hoạt bản ghi cũ) | Pass |
| Hủy | Hủy thành công, hoàn lại chỗ | Pass |
| Hủy | Chặn hủy khi hết thời gian | Pass |
| Hủy | Chặn hủy nếu tổng tín chỉ còn lại dưới mức tối thiểu | Pass |
| Nhập điểm | Tính điểm tổng kết 40% quá trình + 60% cuối kỳ | Pass |
| Nhập điểm | Chặn giảng viên nhập điểm lớp không phụ trách | Pass |

## 3. Kiểm thử tích hợp đăng ký đồng thời

Chạy `cd backend && ./mvnw test` để chạy cùng suite hoặc xem cấu hình capacity, student-count, concurrency, MySQL và report tại [docs/CONCURRENT_REGISTRATION_TEST.md](CONCURRENT_REGISTRATION_TEST.md). Suite tạo fixture mới cho mỗi test, gửi HTTP request đồng thời, xác nhận tranh chỗ cuối, hai lớp cùng môn, request trùng và hủy trả chỗ. Kiểm tra cả HTTP status, bản ghi đang hoạt động và sĩ số lưu. Report JSON ghi database, đầu vào, correctness và latency.

Kết quả suite hiện tại: **17/17 integration test pass trên H2**. Phần QLSV-48 đã chạy **4/4 integration test trên MySQL 8.4** trước khi thêm thông báo; suite mới chưa được chạy lại trên MySQL.

## 4. Audit log (QLSV-50)

Unit và HTTP integration test bao phủ actor/đối tượng/kết quả, correlation ID, lọc có phân trang, từ chối người dùng thường và request chưa xác thực, lọc bỏ trường nhạy cảm, ghi failure code không lưu nội dung exception, và rollback thay đổi đăng ký khi lưu audit thành công thất bại. Suite backend kiểm tra transaction trên H2; chưa chạy lại trên MySQL.

Các thao tác được audit và kịch bản demo được liệt kê tại [docs/AUDIT_LOG_DEMO.md](AUDIT_LOG_DEMO.md).

## 5. Trung tâm thông báo (QLSV-51)

HTTP integration test bao phủ thông báo phát sinh khi đăng ký/đổi lịch, không phát sinh cho sinh viên khác, lọc danh sách và cập nhật trạng thái chỉ trong phạm vi người nhận, từ chối admin/anonymous, cùng nhắc hạn idempotent cho sinh viên đang hoạt động. Kết quả lần chạy: **40/40 backend test pass trên H2**. Build giao diện bằng `cd frontend && npm run build` (**pass**); E2E `npx playwright test e2e/notifications.spec.js` (**1/1 pass trên Chrome**).

Sự kiện, người nhận và kịch bản demo xem tại [docs/NOTIFICATION_DEMO.md](NOTIFICATION_DEMO.md).

## 6. E2E (Playwright)

Chạy: `cd frontend && npx playwright test`. Có thể chọn trình duyệt bằng biến `BROWSER_CHANNEL` (`chrome` mặc định, `msedge` cho Edge).

| Mã | Kịch bản | Kỳ vọng | Chrome | Edge | Firefox | WebKit |
|---|---|---|---|---|---|---|
| E2E-01 | Mở trang đăng nhập | Hiển thị form và tài khoản dùng thử | Pass | Pass | Pass | Pass |
| E2E-02 | Đăng nhập sai | Hiển thị lỗi, vẫn ở trang login | Pass | Pass | Pass | Pass |
| E2E-03 | Sinh viên đăng nhập | Chuyển đến `/tong-quan`, thấy dashboard | Pass | Pass | Pass | Pass |
| E2E-04 | Chưa đăng nhập vào trang bảo vệ | Chuyển về `/login` | Pass | Pass | Pass | Pass |
| E2E-05 | Cập nhật họ tên hồ sơ | Lưu thành công, tên mới hiển thị | Pass | Pass | Pass | Pass |
| E2E-06 | Sinh viên đăng ký học phần | Đăng ký thành công, học phần xuất hiện | Pass | Pass | Pass | Pass |
| E2E-07 | Hủy đăng ký bằng modal xác nhận | Nút "Giữ lại" không hủy; "Hủy đăng ký" hủy thành công | Pass | Pass | Pass | Pass |
| E2E-08 | Xem thời khóa biểu và điểm | Trang hiển thị đúng, có biểu đồ GPA | Pass | Pass | Pass | Pass |
| E2E-09 | Giảng viên xem tổng quan/lớp/nhập điểm | Dashboard, danh sách lớp và bảng điểm hiển thị | Pass | Pass | Pass | Pass |
| E2E-10 | Quản trị xem dashboard | Có thống kê, biểu đồ đăng ký và tỷ lệ lấp đầy | Pass | Pass | Pass | Pass |
| E2E-11 | Quản trị thêm/xóa môn học | Tạo môn thành công, xóa qua modal xác nhận | Pass | Pass | Pass | Pass |
| E2E-12 | Quản trị khóa/mở khóa tài khoản | Trạng thái đổi và khôi phục đúng | Pass | Pass | Pass | Pass |
| E2E-13 | Course Planner: giỏ lưu qua tải lại, xung đột lịch/môn và không ghi danh trước xác nhận | Cảnh báo rõ hai lớp; giỏ chỉ là kế hoạch | Pass | Chưa chạy | Chưa chạy | Chưa chạy |
| E2E-14 | Course Planner: xác nhận rõ ràng, backend từ chối thiếu tiên quyết | HTTP 400, giỏ còn nguyên, không có ghi danh một phần | Pass | Chưa chạy | Chưa chạy | Chưa chạy |
| E2E-15 | Course Planner: lớp hết chỗ trước khi xác nhận | Cảnh báo hết chỗ và khóa xác nhận | Pass | Chưa chạy | Chưa chạy | Chưa chạy |
| E2E-16 | Quản trị lọc audit theo người thực hiện và hành động | Hiển thị sự kiện khóa/mở tài khoản thành công | Pass | Chưa chạy | Chưa chạy | Chưa chạy |

Ghi chú: bảng trên hiện gồm 17 kịch bản trong 16 test Playwright; E2E-06 và E2E-07 nằm trong cùng một test đăng ký/hủy. Lần chạy đầy đủ trước QLSV-51: **15/15 test pass trên Chrome**; test trung tâm thông báo mới chạy riêng **1/1 pass trên Chrome**. E2E mới chưa chạy trên Edge, Firefox và WebKit.

Chạy Firefox và WebKit: `cd frontend && npx playwright install firefox webkit && npx playwright test -c playwright.multi.config.js`.

Kết quả chạy ngày 28/10/2026, backend profile H2 khởi động mới:

| Trình duyệt | Cách chạy | Pass / Tổng | Thời gian | Lỗi phát hiện |
|---|---|---|---|---|
| Chrome | Playwright (channel chrome) | 11/11 | 1,2 phút | Không |
| Edge | Playwright (channel msedge) | 11/11 | 1,3 phút | Không |
| Firefox | Playwright (Desktop Firefox) | 11/11 | 3,9 phút (chạy chung với WebKit) | Không |
| Safari (WebKit) | Playwright (Desktop Safari) | 11/11 | 3,9 phút (chạy chung với Firefox) | Không |

Cả bốn trình duyệt đạt 100%, vượt mục tiêu T42 (từ 80% trở lên). Safari được kiểm thử bằng engine WebKit của Playwright, chưa chạy trên thiết bị Apple thật.
## 7. Kiểm thử tải (NFR-01)

Công cụ: `tools/load-test.mjs` (Node.js, không phụ thuộc thư viện ngoài).

```bash
CONCURRENCY=50 REQUESTS=200 WARMUP=40 node tools/load-test.mjs
```

Kết quả trên máy dev (H2, Windows, Node 22):

| Chỉ số | Lần 1 | Lần 2 | Lần 3 | Ngưỡng |
|---|---|---|---|---|
| Số request / đồng thời | 200 / 50 | 200 / 50 | 200 / 50 | 50 đồng thời |
| Thành công / lỗi | 200 / 0 | 200 / 0 | 200 / 0 | 0 lỗi |
| Tổng thời gian | 995,27 ms | 578,06 ms | 675,56 ms | - |
| Throughput | 200,95 req/s | 345,99 req/s | 296,05 req/s | - |
| Min | 51,31 ms | 36,05 ms | 39,23 ms | - |
| P50 | 165,74 ms | 105,49 ms | 112,87 ms | - |
| P95 | 444,29 ms | 320,04 ms | 330,43 ms | <= 500 ms |
| P99 | 565,20 ms | 331,76 ms | 369,85 ms | - |
| Max | 573,91 ms | 360,56 ms | 373,58 ms | - |
| Kết quả | [PASS] | [PASS] | [PASS] | |

Kết luận: đạt mục tiêu P95 ≤ 500 ms và không có lỗi trong môi trường H2. Cần đo lại khi chạy MySQL/Docker và trên hạ tầng thật.
- Lần 1 chậm hơn hai lần sau (P95 444 ms, gần ngưỡng) vì chạy ngay sau khi vừa khởi động backend, Java và bộ nhớ đệm chưa “nóng”. Hai lần sau ổn định quanh 320-330 ms.
- Máy chạy cả backend, client test và IntelliJ cùng lúc nên số liệu có thể thấp hơn môi trường thật.
- Công cụ chỉ đo một endpoint đọc (/api/lop-hoc-phan), chưa đo đăng ký đồng thời. Kết quả chỉ chứng minh hiệu năng đọc với 50 người dùng cùng lúc.
- Môi trường là H2 trong bộ nhớ, cần đo lại trên MySQL/Docker nếu có điều kiện.
## 8. Truy vết NFR

| NFR | Cách kiểm chứng | Trạng thái |
|---|-----------------|---|
| NFR-01 Hiệu năng | Load test 50 đồng thời, 3 lần chạy,  P95 từ 320 đến 444 ms, 0 lỗi | Đạt trên môi trường dev |
| NFR-02 Toàn vẹn dữ liệu | Transaction `READ_COMMITTED`, khóa bi quan sinh viên + lớp, unique `(sinh_vien_id, lop_hoc_phan_id)`, integration test đối chiếu sĩ số và bản ghi | Đạt |
| NFR-03 Bảo mật | JWT, BCrypt, phân quyền theo vai trò, E2E kiểm tra chuyển hướng | Đạt |
| NFR-04 Kiểm tra đầu vào | Bean Validation ở DTO, xử lý lỗi tập trung | Đạt |
| NFR-05 Ghi log | SLF4J cho đăng ký/hủy/nhập điểm/lỗi hệ thống | Đạt |
| NFR-06 Bảo trì | Tách Controller–Service–Repository–DTO; 22 unit + 13 integration test và E2E Course Planner/Audit Log | Đạt |
| NFR-07 Triển khai | Docker Compose, `docker compose config` hợp lệ | Đạt |
| NFR-08 Giao diện | Responsive, E2E chạy tự động trên Chrome, Edge, FireFox, Safari | Một phần |

## 9. Kiểm thử hồi quy
Chạy ngày 28/10/2026 trên nhánh `dev` (đã gộp T40 và tính năng thông báo nghỉ học), backend profile H2 khởi động mới. Mục tiêu: xác nhận các luồng nghiệp vụ chính vẫn hoạt động đúng sau khi gộp code.

### 6.1 Kết quả tự động

| Hạng mục | Cách chạy | Kết quả |
|---|---|---|
| Unit test backend | `cd backend && ./mvnw test` | 19/19 pass, 0 lỗi, 0 bỏ qua (DangKyServiceTest 17, DiemServiceTest 2), tổng 8,1 giây, BUILD SUCCESS |
| E2E Chrome | `cd frontend && npx playwright test` | 11/11 pass, 42,9 giây |

### 6.2 Phạm vi hồi quy

| Luồng nghiệp vụ | Cách kiểm | Kết quả |
|---|---|---|
| Đăng nhập, phân quyền, hồ sơ cá nhân | E2E-01 đến E2E-05 | [PASS] |
| Đăng ký, hủy học phần | E2E-06, E2E-07 và 17 unit test | [PASS] |
| Thời khóa biểu, điểm, nhập điểm | E2E-08, E2E-09 và 2 unit test | [PASS] |
| Quản trị: dashboard, môn học, khóa tài khoản | E2E-10 đến E2E-12 | [PASS] |
| Quản lý khoa, ngành, lớp hành chính, lớp học phần | Thủ công | (điền sau) |
| Thêm người dùng, nhập CSV, môn tiên quyết | Thủ công | (điền sau) |
| Thống kê, xuất CSV | Thủ công | (điền sau) |
| Thông báo nghỉ học và ô nghỉ trong thời khóa biểu | Thủ công | (điền sau) |

### 6.3 Test tải 500 request, 50 đồng thời

Lệnh: `CONCURRENCY=50 REQUESTS=500 WARMUP=40 node tools/load-test.mjs`

| Chỉ số | Lần 1 | Lần 2 | Lần 3 | Ngưỡng |
|---|---|---|---|---|
| Số request / đồng thời | 500 / 50 | 500 / 50 | 500 / 50 | - |
| Thành công / lỗi | 500 / 0 | 500 / 0 | 500 / 0 | 0 lỗi |
| Tổng thời gian | 1095,98 ms | 607,99 ms | 575,68 ms | - |
| Throughput | 456,21 req/s | 822,38 req/s | 868,54 req/s | - |
| Min | 10,18 ms | 12,81 ms | 14,90 ms | - |
| P50 | 70,76 ms | 49,43 ms | 56,17 ms | - |
| P95 | 302,55 ms | 129,50 ms | 86,93 ms | <= 500 ms |
| P99 | 485,40 ms | 153,03 ms | 94,59 ms | - |
| Max | 694,36 ms | 156,90 ms | 99,11 ms | - |
| Kết quả | [PASS] | [PASS] | [PASS] | |

Nhận xét:
- Cả 3 lần đạt: 0 lỗi, P95 từ 87 đến 303 ms (ngưỡng 500 ms), mã trả về 100% là 200.
- Lần 1 chậm hơn hai lần sau (P95 303 ms, Max 694 ms) vì JVM chưa tối ưu hết; hai lần sau ổn định hơn nhiều.
- Số liệu tốt hơn đợt 200 request (P95 320 đến 444 ms) nhiều khả năng do máy ít tiến trình chạy kèm và JVM đã nóng, chưa đủ cơ sở để kết luận hiệu năng được cải thiện.
- Hạn chế như mục 4: chỉ đo endpoint đọc `/api/lop-hoc-phan`, môi trường H2, chưa đo đăng ký đồng thời.
## 10. Tư liệu giao diện

Bộ ảnh giao diện phục vụ báo cáo/Figma nằm trong `docs/screenshots/`, tạo lại bằng:

```bash
cd frontend
npm run screenshots
```

Gồm: đăng nhập, dashboard sinh viên, đăng ký học phần, điểm & GPA, dashboard giảng viên, lớp phụ trách, dashboard quản trị, lớp học phần, modal thêm môn học.

## 11. Hạn chế và việc cần làm
- Safari mới được kiểm thử bằng WebKit của Playwright; chưa kiểm thử trên thiết bị Apple thật (macOS/iOS).
- Load test mới đo endpoint danh sách lớp; nên bổ sung kịch bản đăng ký đồng thời để kiểm chứng khóa bi quan dưới tải.
- E2E dùng dữ liệu seed và có thao tác tạo/xóa/lock/unlock; các test đã được viết để chạy lại an toàn trên database mới.

## 12. QLSV-52: Admin Analytics thành danh sách hành động

- Dữ liệu cảnh báo được đếm từ các đăng ký `DA_DANG_KY`; trạng thái `DA_HUY` không được tính. Bộ lọc học kỳ/khoa chạy ở backend.
- Integration coverage: cảnh báo lớp gần đầy, lớp ít đăng ký, môn đạt tổng chỉ tiêu; sau khi hủy đăng ký cảnh báo được tính lại; lọc khoa không có lớp trả danh sách trống; khoa không tồn tại trả 404; sinh viên bị 403 và request không xác thực bị 401; kỳ đăng ký đang mở và còn 3 ngày sinh cảnh báo sắp đóng.
- Lệnh `cd backend && ./mvnw test`: 42/42 tests đạt (23 unit + 19 integration, H2).
- Lệnh `cd frontend && npm run build`: đạt.
- Chưa chạy E2E Playwright cho thay đổi Analytics hoặc database MySQL; E2E tập trung UI và MySQL không được cấu hình trong lần kiểm tra này.

## 13. QLSV-77: Cấu hình đối tượng đăng ký và điều chỉnh đợt (TASK-AD-02)

- **Phạm vi kiểm thử**:
  - Cấu hình đợt đăng ký theo khóa, ngành, lớp hành chính hoặc nhóm đối tượng.
  - Thứ tự ưu tiên và khoảng thời gian riêng cho từng nhóm đối tượng.
  - Kiểm tra biên thời gian theo múi giờ Asia/Ho_Chi_Minh (UTC+7).
  - Từ chối nghiêm ngặt ở backend đối với sinh viên ngoài đối tượng hoặc ngoài khung giờ mở đăng ký (kể cả gọi API trực tiếp).
  - Thao tác điều chỉnh đợt: Gia hạn (/gia-han), Đóng sớm (/dong-som), Mở đợt bổ sung (/mo-bo-sung), Xóa đợt.
  - Ghi nhận đầy đủ nhật ký Audit Log kèm lý do bắt buộc cho mọi thao tác điều chỉnh (REGISTRATION_PERIOD_EXTENDED, REGISTRATION_PERIOD_CLOSED_EARLY, REGISTRATION_PERIOD_SUPPLEMENTAL_OPENED, REGISTRATION_PERIOD_DELETED).
  - Bảo toàn 100% dữ liệu đăng ký và lịch sử đăng ký của sinh viên qua các lần điều chỉnh cấu hình đợt.
  - Xử lý xung đột đa nhóm đối tượng: áp dụng chính sách cấu hình HIGHEST_PRIORITY và công khai trạng thái chinhSachUuTienDaChot: false chờ quyết định chính thức từ Hội đồng Đào tạo.
- **Tự động hóa**:
  - Integration Test: RegistrationPeriodIntegrationTest bao phủ toàn bộ luồng tạo đợt, giới hạn đối tượng, từ chối backend, đăng ký thành công cho đối tượng hợp lệ, gia hạn, đóng sớm, từ chối sau đóng sớm, mở đợt bổ sung và audit log.
  - Kết quả kiểm thử backend: 106/106 tests PASS trên H2.
  - Kết quả frontend build: Vite build thành công không lỗi (0 warning/error).
