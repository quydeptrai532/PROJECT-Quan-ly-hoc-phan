# Hệ thống Quản lý đào tạo và Đăng ký học phần

Đồ án xây dựng theo tài liệu SRS `SRS_QuanLyDaoTao_DangKyHocPhan`: quản lý chương trình, môn học, lớp học phần; sinh viên đăng ký/hủy học phần trực tuyến; giảng viên nhập điểm; phòng đào tạo thống kê và xuất báo cáo.

## Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Backend | Spring Boot 3.4, Java 21, Spring Security + JWT, Spring Data JPA |
| Cơ sở dữ liệu | MySQL 8.4 (mặc định), H2 (profile `h2` để chạy nhanh) |
| Frontend | React 18, Vite 6, Tailwind CSS 3, React Router, Axios, Recharts |
| Kiểm thử | JUnit 5 + Mockito, Playwright (Chrome), load test bằng Node.js |
| Triển khai & CI/CD | Docker, Docker Compose, Nginx, GitHub Actions, GHCR |

## Chức năng theo vai trò

- **SINH_VIEN**: dashboard tổng quan (tín chỉ, GPA, lịch hôm nay, biểu đồ GPA), cập nhật họ tên/email/SĐT, đổi mật khẩu, lập kế hoạch học tập bằng giỏ lớp và thời khóa biểu dự kiến, xác nhận đăng ký/hủy học phần, xem thời khóa biểu, điểm/GPA, lịch sử học tập & môn chưa đạt, xuất bảng điểm cá nhân PDF và trung tâm thông báo (nhận thông báo kết quả đăng ký/hủy, lớp học phần bị hủy, buổi học tạm nghỉ, thay đổi lịch học/giảng viên và công bố điểm lớp học phần).
- **GIANG_VIEN**: dashboard tổng quan (lớp phụ trách, sĩ số, biểu đồ lấp đầy, lịch dạy hôm nay), thời khóa biểu giảng dạy, danh sách sinh viên, nhập/cập nhật điểm nháp hoặc import từ `.xlsx`, gửi duyệt bảng điểm lớp phụ trách (và chỉnh sửa/gửi duyệt lại khi được Phòng Đào tạo trả về), thống kê điểm lớp và phân bố điểm theo học kỳ/môn/lớp phụ trách, cùng trung tâm thông báo.
- **QUAN_TRI**: quản lý khoa/ngành/lớp hành chính/môn học/học kỳ, cấu hình đợt đăng ký, mở/cập nhật/hủy lớp học phần, phê duyệt/trả về chỉnh sửa/công bố/khóa/mở khóa bảng điểm, thống kê điểm lớp học phần và phân bố khoảng điểm theo học kỳ/khoa/môn/lớp, quản lý sinh viên & giảng viên, khóa/mở tài khoản, nhập CSV, dashboard thống kê có biểu đồ và xuất CSV.

Sinh viên và giảng viên đăng nhập vào `/tong-quan`; quản trị vào `/quan-tri`.

Dashboard và trang điểm của sinh viên chỉ hiển thị điểm tổng kết khi bảng điểm lớp học phần đã được phê duyệt và chuyển sang trạng thái công bố (`DA_CONG_BO` hoặc `DA_KHOA`), dùng chung ngưỡng đạt `AcademicRules.DIEM_DAT` (5.0). Cảnh báo mở/đóng đăng ký dựa trên ngày cấu hình của học kỳ trong cửa sổ sắp tới 14 ngày; chỉnh cửa sổ bằng `app.dashboard.upcoming-window-days` hoặc biến môi trường `DASHBOARD_UPCOMING_WINDOW_DAYS`. Không có dữ liệu đủ căn cứ thì API trả danh sách rỗng.

Góp ý bổ sung cho báo cáo đề cương: [docs/PROPOSAL_CHECKLIST.md](docs/PROPOSAL_CHECKLIST.md).

## Cấu trúc thư mục

```
.github/workflows/ # CI (build/test/E2E) và build/push Docker image
backend/
  src/main/java/com/edu/registration/
    config/       # Security, CORS, seed dữ liệu mẫu
    controller/   # REST API
    dto/          # request/response records
    entity/       # JPA entities
    exception/    # xử lý lỗi tập trung
    repository/   # Spring Data repositories
    security/     # JWT filter, JwtService, CurrentUser, UserAuthCache
    service/      # nghiệp vụ
  src/test/java/  # unit test nghiệp vụ đăng ký, hủy và nhập điểm
frontend/
  e2e/            # Playwright E2E
  src/
    api/          # axios client, interceptor JWT
    components/   # Layout, Modal, ConfirmDialog, CrudPage, TimetableGrid, UI
    context/      # AuthContext
    lib/          # format, hook useApi
    pages/        # student / lecturer / admin / RoleDashboard
docs/
  LECTURER_STUDENT_SCOPE.md # quy định phạm vi xem thông tin sinh viên cho giảng viên (QLSV-67)
  GRADE_APPROVAL_WORKFLOW.md # máy trạng thái gửi duyệt, phê duyệt, trả về, công bố và khóa bảng điểm (QLSV-74)
  CLASS_GRADE_ANALYTICS.md # định nghĩa chỉ số, mẫu số và khoảng phân bố điểm lớp học phần (QLSV-75)
  TEST_PLAN.md    # test case theo FR/NFR và kết quả
  COURSE_PLANNER_DEMO.md # hướng dẫn demo Course Planner
  NOTIFICATION_DEMO.md # hướng dẫn trung tâm thông báo và cơ chế idempotency
  AUDIT_LOG_DEMO.md # hướng dẫn tra cứu nhật ký kiểm toán
  PROPOSAL_CHECKLIST.md # góp ý bổ sung báo cáo đề cương
  screenshots/    # ảnh giao diện cho báo cáo/Figma
tools/
  load-test.mjs   # load test 50 request đồng thời
docker-compose.yml
```

## Chạy nhanh

### 1. Docker Compose (MySQL + backend + frontend)

```bash
cp .env.example .env
docker compose up --build
```

- Giao diện: http://localhost:3000
- API: http://localhost:8080/api
- MySQL: `localhost:3307`, database `quan_ly_dao_tao` (port host đổi để tránh đụng MySQL cài sẵn trên máy; trong mạng Docker backend vẫn dùng `mysql:3306`)

Dữ liệu mẫu được tạo tự động khi `SEED_ENABLED=true` và database còn trống.

### 2. Chạy local với H2 (không cần MySQL)

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
```

Windows: `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2`

- API: http://localhost:8080/api
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:quan_ly_dao_tao`)

### 3. Chạy local với MySQL

Tạo database `quan_ly_dao_tao`, sau đó đặt biến môi trường (hoặc sửa `application.yml`):

```bash
DB_URL=jdbc:mysql://localhost:3306/quan_ly_dao_tao?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=root
```

```bash
cd backend
./mvnw spring-boot:run
```

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite chạy ở `http://localhost:5173` và proxy `/api` sang `http://localhost:8080`.

## Tài khoản mẫu

| Vai trò | Tên đăng nhập | Mật khẩu |
|---|---|---|
| Quản trị | `admin` | `admin123` |
| Giảng viên | `gv01`, `gv02`, `gv03` | `123456` |
| Sinh viên | `sv01` … `sv20` | `123456` |

Dữ liệu mẫu gồm 2 khoa, 3 ngành, 3 lớp hành chính, 8 môn học (có môn tiên quyết), 3 học kỳ (một học kỳ đang mở đăng ký), 11 lớp học phần và 20 sinh viên với đăng ký/điểm ở các học kỳ trước.

## Quy tắc nghiệp vụ đã triển khai

1. Chỉ được đăng ký/hủy khi học kỳ đang trong thời gian cho phép (`dang_ky_tu` ≤ hôm nay ≤ `dang_ky_den`).
2. Phải đạt **tất cả** môn tiên quyết (điểm tổng kết ≥ 5.0 đã được công bố) trước khi đăng ký.
3. Không đăng ký hai lớp của cùng một môn trong cùng học kỳ.
4. Không trùng thứ và khoảng tiết học với học phần đã đăng ký.
5. Không vượt sĩ số tối đa; kiểm tra và tăng sĩ số nằm trong cùng transaction với khóa bi quan (`SELECT ... FOR UPDATE`) trên lớp học phần.
6. Tổng tín chỉ không vượt quá giới hạn tối đa của học kỳ. Khi hủy, hệ thống chặn nếu tổng tín chỉ còn lại rơi vào khoảng (0, tối thiểu); được phép hủy toàn bộ để rút hết học phần. Khi đăng ký thêm, giao diện cảnh báo nếu chưa đạt mức tối thiểu.
7. Hủy đăng ký hoàn lại một chỗ, giữ lại bản ghi với trạng thái `DA_HUY`; đăng ký lại sẽ kích hoạt lại bản ghi cũ (đảm bảo ràng buộc duy nhất `sinh_vien_id + lop_hoc_phan_id`).
8. Giảng viên chỉ được xem/nhập điểm và gửi duyệt cho lớp mình phụ trách khi bảng điểm ở trạng thái `NHAP` hoặc `TRA_VE`. Bảng điểm lớp học phần tuân theo máy trạng thái `NHAP` → `CHO_DUYET` → (`TRA_VE` kèm lý do bắt buộc hoặc `DA_DUYET`) → `DA_CONG_BO` / `DA_KHOA`, có khóa bi quan (`SELECT ... FOR UPDATE`), lưu lịch sử chuyển trạng thái và nhật ký kiểm toán. Hệ thống chỉ hiển thị điểm cho sinh viên khi bảng điểm ở trạng thái `DA_CONG_BO` hoặc `DA_KHOA`, và chỉ phát thông báo nội bộ (`GRADES_PUBLISHED`) sau khi transaction công bố đã commit. Chi tiết: [docs/GRADE_APPROVAL_WORKFLOW.md](docs/GRADE_APPROVAL_WORKFLOW.md).
9. Điểm tổng kết = `0.4 × quá trình + 0.6 × cuối kỳ`, làm tròn 1 chữ số; điểm hợp lệ 0–10.
10. GPA hệ 4 quy đổi: ≥8.5→4.0, ≥8.0→3.5, ≥7.0→3.0, ≥6.5→2.5, ≥5.5→2.0, ≥5.0→1.5, ≥4.0→1.0, còn lại 0. Tín chỉ tích lũy chỉ tính môn đạt (≥5.0).
11. Không xóa dữ liệu danh mục đang được tham chiếu; API trả `409 Conflict` kèm thông báo.
12. Mở lớp học phần có cảnh báo trùng phòng/trùng lịch giảng viên (vẫn cho phép lưu, trả về danh sách `canhBao`).
13. Giảng viên chỉ được gửi thông báo cho các lớp học phần mình phụ trách. Người nhận được xác minh trên server từ các sinh viên đang đăng ký hiệu lực (`DA_DANG_KY`). Thông báo gửi ngay lập tức tới Notification Center của đúng sinh viên, lưu người tạo, thời điểm và ghi audit log (`CLASS_ANNOUNCEMENT_CREATED`). Chi tiết: [docs/LECTURER_CLASS_ANNOUNCEMENTS.md](docs/LECTURER_CLASS_ANNOUNCEMENTS.md).
14. Thống kê điểm lớp học phần và phân bố khoảng điểm (`[0.0, 4.0)`, `[4.0, 5.0)`, `[5.0, 6.5)`, `[6.5, 8.0)`, `[8.0, 10.0]`) chỉ tính thống kê chính thức trên các lớp có bảng điểm đã công bố (`DA_CONG_BO` hoặc `DA_KHOA`), dùng mẫu số là các bản ghi đăng ký hiệu lực đã có điểm tổng kết hợp lệ (không tính sinh viên chưa có điểm công bố thành `0.0`). Dữ liệu lớp chưa công bố được tách riêng ở khối tạm tính có nhãn cảnh báo cho giảng viên phụ trách / quản trị. Chi tiết: [docs/CLASS_GRADE_ANALYTICS.md](docs/CLASS_GRADE_ANALYTICS.md).
15. Quản trị khóa học/niên khóa (`/api/admin/khoa-hoc`) và quan hệ giữa các môn học (`/api/admin/quan-he-mon`: `TIEN_QUYET`, `SONG_HANH`, `TUONG_DUONG`, `THAY_THE`, `NHOM_TU_CHON`) có kiểm tra chống tự tham chiếu, chống chu trình tiên quyết nhiều bước, bảo vệ xóa (`409 Conflict`) khi đang được chương trình đào tạo hoặc lớp học phần sử dụng, và lưu metadata người tạo/cập nhật kèm audit log. Khi kiểm tra đăng ký, hệ thống cưỡng chế `TIEN_QUYET` đang hiệu lực theo ngưỡng đạt cấu hình và trả về thông tin đối chiếu có cấu trúc cho các quan hệ `SONG_HANH`, `TUONG_DUONG`, `THAY_THE`, `NHOM_TU_CHON` đang chờ chủ dự án chốt quy chế. Chi tiết: [docs/COURSE_AND_SUBJECT_RELATIONS.md](docs/COURSE_AND_SUBJECT_RELATIONS.md).

## API chính

| Phương thức | Endpoint | Chức năng |
|---|---|---|
| GET | `/api/health` | Health check công khai (dùng cho CI/Docker) |
| POST | `/api/auth/login` | Đăng nhập, nhận JWT |
| GET/PUT | `/api/auth/me` | Xem/cập nhật hồ sơ (họ tên, email, SĐT) |
| PUT | `/api/auth/doi-mat-khau` | Đổi mật khẩu |
| GET | `/api/hoc-ky` | Danh sách học kỳ |
| GET | `/api/lop-hoc-phan?hocKyId=&keyword=` | Tìm lớp học phần |
| POST | `/api/dang-ky` | Sinh viên đăng ký |
| POST | `/api/dang-ky/xac-nhan-ke-hoach` | Xác nhận nhiều lớp trong một transaction, dùng lại toàn bộ kiểm tra đăng ký |
| DELETE | `/api/dang-ky/{id}` | Sinh viên hủy đăng ký |
| GET | `/api/dang-ky/me?hocKyId=` | Học phần đã đăng ký |
| GET | `/api/sinh-vien/me/thoi-khoa-bieu` | Thời khóa biểu |
| GET | `/api/sinh-vien/me/diem` | Điểm, GPA học kỳ và tích lũy |
| GET | `/api/sinh-vien/me/lich-su-hoc-tap?hocKyId=&trangThai=` | Lịch sử học tập theo học kỳ, môn chưa đạt và số lần học của sinh viên đang đăng nhập |
| GET | `/api/sinh-vien/me/ho-so-dao-tao` | Hồ sơ học vụ của sinh viên đăng nhập (khoa, ngành, khóa, lớp, chương trình, trạng thái); trường chưa được gán trả `Chưa cập nhật`, không nhận ID sinh viên |
| GET | `/api/giang-vien/me/ho-so` | Hồ sơ cá nhân của giảng viên đăng nhập (mã, họ tên, email, sđt, khoa, trạng thái tài khoản, danh sách trường được phép tự sửa); không nhận ID từ client |
| GET | `/api/giang-vien/me/dashboard?hocKyId=` | Dashboard giảng dạy theo học kỳ (thống kê lớp phụ trách, sĩ số, lịch dạy, việc cần xử lý như bảng điểm nháp/chờ duyệt); tự động giới hạn theo giảng viên hiện tại |
| GET | `/api/giang-vien/me/lop-hoc-phan` | Lớp giảng viên phụ trách |
| GET | `/api/giang-vien/me/thoi-khoa-bieu` | Thời khóa biểu giảng dạy |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien` | Danh sách sinh viên của lớp phụ trách (hỗ trợ tìm kiếm `tuKhoa`) |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/sinh-vien/csv` | Xuất danh sách sinh viên lớp phụ trách CSV |
| GET | `/api/giang-vien/lop-hoc-phan/{lopId}/sinh-vien/{studentId}` | Hồ sơ học tập tối thiểu của sinh viên trong lớp phụ trách |
| GET | `/api/giang-vien/sinh-vien?lopHocPhanId=&tuKhoa=` | Tìm kiếm sinh viên trong phạm vi lớp giảng viên phụ trách |
| GET | `/api/giang-vien/sinh-vien/{studentId}` | Hồ sơ học tập tối thiểu sinh viên được phân công |
| PUT | `/api/diem/{dangKyId}` | Nhập/cập nhật điểm nháp |
| PUT | `/api/giang-vien/lop-hoc-phan/{id}/diem` | Cập nhật điểm hàng loạt cho lớp phụ trách (ALL_OR_NOTHING) |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/diem/template-xlsx` | Tải tệp mẫu Excel `.xlsx` chứa danh sách sinh viên và các cột điểm chuẩn (`maSinhVien`, `hoTen`, `diemQuaTrinh`, `diemCuoiKy`) |
| POST | `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/preview` | Tải lên tệp `.xlsx`, kiểm tra bảo mật/validation và xem trước kết quả trước khi ghi |
| POST | `/api/giang-vien/lop-hoc-phan/{id}/diem/import-xlsx/confirm` | Xác nhận ghi dữ liệu điểm từ phiên preview hợp lệ vào bảng điểm (`ALL_OR_NOTHING`, chi tiết tại [docs/GRADE_IMPORT_XLSX.md](docs/GRADE_IMPORT_XLSX.md)) |
| POST | `/api/giang-vien/lop-hoc-phan/{id}/gui-duyet-diem` | Giảng viên phụ trách gửi duyệt (hoặc gửi duyệt lại) bảng điểm lớp học phần (`NHAP`/`TRA_VE` → `CHO_DUYET`) |
| POST | `/api/admin/lop-hoc-phan/{id}/phe-duyet-diem`, `/tra-ve-diem`, `/cong-bo-diem`, `/khoa-diem`, `/mo-khoa-diem` | Phòng Đào tạo (`QUAN_TRI`) phê duyệt, trả về chỉnh sửa (kèm lý do), công bố, khóa hoặc mở khóa bảng điểm (kèm lý do) |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/lich-su-bang-diem`, `/api/admin/lop-hoc-phan/{id}/lich-su-bang-diem` | Xem lịch sử chuyển trạng thái bảng điểm của lớp học phần |
| GET | `/api/giang-vien/thong-ke-diem?hocKyId=&monHocId=&lopHocPhanId=` | Thống kê điểm lớp và phân bố khoảng điểm trong phạm vi lớp giảng viên phụ trách (không chứa PII sinh viên) |
| GET | `/api/giang-vien/lop-hoc-phan/{id}/thong-ke-diem` | Thống kê điểm và phân bố của một lớp học phần cụ thể (tách riêng thống kê công bố và tạm tính chưa công bố) |
| GET | `/api/admin/thong-ke/diem-lop?hocKyId=&khoaId=&monHocId=&lopHocPhanId=` | Quản trị viên thống kê điểm lớp học phần và phân bố điểm theo học kỳ, khoa, môn học, lớp học phần |
| CRUD | `/api/admin/khoa`, `/nganh`, `/lop-hanh-chinh`, `/mon-hoc`, `/hoc-ky`, `/khoa-hoc` | Quản trị danh mục (bao gồm Khóa học / Niên khóa) |
| GET/POST/PUT/DELETE | `/api/admin/quan-he-mon`, `/api/admin/mon-hoc/{id}/tien-quyet`, `/api/admin/loai-quan-he-mon` | Cấu hình và tra cứu quan hệ giữa các môn học (tiên quyết, song hành, tương đương, thay thế, nhóm tự chọn) |
| GET | `/api/dang-ky/kiem-tra-quan-he?lopHocPhanId=` | Kiểm tra chi tiết các quan hệ môn học đang hiệu lực của lớp học phần đối với sinh viên đăng nhập |
| CRUD | `/api/admin/lop-hoc-phan` | Mở lớp học phần |
| POST | `/api/admin/lop-hoc-phan/{id}/huy` | Hủy lớp học phần, chuyển các đăng ký đang hoạt động sang `DA_HUY` và thông báo sinh viên bị ảnh hưởng |
| CRUD | `/api/admin/sinh-vien`, `/giang-vien` | Quản lý người dùng |
| POST | `/api/admin/sinh-vien/import` | Nhập sinh viên từ CSV |
| GET | `/api/admin/thong-ke/tong-quan`, `/theo-mon`, `/hanh-dong?hocKyId=&khoaId=` | Thống kê và danh sách việc giáo vụ cần chú ý; chỉ quản trị viên |
| GET | `/api/admin/lop-hoc-phan/{id}/csv` | Xuất danh sách lớp CSV |
| GET | `/api/admin/audit-logs` | Tra cứu nhật ký (lọc thời gian, người dùng, hành động, đối tượng; phân trang; chỉ quản trị viên) |
| GET | `/api/notifications?unreadOnly=&page=&size=` | Danh sách thông báo của người đăng nhập (sinh viên/giảng viên), có lọc chưa đọc và phân trang |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu một thông báo của chính người đăng nhập đã đọc |
| PATCH | `/api/notifications/read-all` | Đánh dấu tất cả thông báo của chính người đăng nhập đã đọc |
| GET | `/api/yeu-cau-ho-tro/danh-muc` | Danh mục loại yêu cầu, trạng thái, chính sách tệp và đối tượng liên quan (môn học, học kỳ, điểm của sinh viên) |
| POST | `/api/yeu-cau-ho-tro` | Sinh viên gửi phiếu hỗ trợ/khiếu nại (JSON hoặc `multipart/form-data` kèm tệp minh chứng) |
| GET | `/api/yeu-cau-ho-tro/me` | Danh sách phiếu hỗ trợ của sinh viên đang đăng nhập (có lọc & phân trang) |
| GET | `/api/yeu-cau-ho-tro/{id}` | Chi tiết phiếu hỗ trợ, nội dung gốc, tệp đính kèm, trao đổi và lịch sử trạng thái |
| POST | `/api/yeu-cau-ho-tro/{id}/trao-doi` | Thêm phản hồi trao đổi trên phiếu hỗ trợ (sinh viên sở hữu phiếu hoặc quản trị viên) |
| GET | `/api/yeu-cau-ho-tro/{id}/tep-dinh-kem/{attachmentId}` | Tải xuống tệp minh chứng sau khi kiểm tra quyền truy cập phiếu |
| GET | `/api/admin/yeu-cau-ho-tro` | Quản trị viên tra cứu danh sách phiếu hỗ trợ (lọc & phân trang) |
| PATCH | `/api/admin/yeu-cau-ho-tro/{id}/trang-thai` | Quản trị viên cập nhật trạng thái xử lý phiếu hỗ trợ và ghi lịch sử |

Swagger/OpenAPI chưa tích hợp; có thể bổ sung `springdoc-openapi` nếu cần.

## Định dạng CSV nhập sinh viên

Có thể tải file mẫu trên giao diện quản trị sinh viên. Cột (dòng đầu là header, có thể bỏ qua):

```csv
ma,hoTen,email,khoaId,nganhId,lopId
SV010,Nguyễn Văn A,a@student.edu.vn,1,1,1
```

- `khoaId`, `nganhId`, `lopId` là id số, có thể để trống.
- Tên đăng nhập mặc định bằng `ma`, mật khẩu mặc định `123456`.
- Mỗi dòng được xử lý trong transaction riêng; dòng lỗi được báo lại, các dòng hợp lệ vẫn được tạo.

## Kiểm thử và build

### Backend tests

```bash
cd backend
./mvnw test
```

### E2E với Playwright

Yêu cầu backend đang chạy và frontend dùng Chrome đã cài trên máy:

```bash
cd frontend
npm ci
npx playwright test        # 17 kịch bản trên Chrome
BROWSER_CHANNEL=msedge npx playwright test   # chạy lại trên Edge
npx playwright show-report
```

Windows PowerShell: `$env:BROWSER_CHANNEL="msedge"; npx playwright test`.

Cấu hình dùng `channel: "chrome"` nên không cần tải browser của Playwright. Nếu backend chưa chạy, `globalSetup` sẽ báo lỗi rõ ràng. Chi tiết test case và kết quả xem tại [docs/TEST_PLAN.md](docs/TEST_PLAN.md).

### Load test

```bash
# backend đang chạy
node tools/load-test.mjs
# hoặc: cd frontend && npm run test:load
```

Tham số môi trường: `CONCURRENCY` (mặc định 50), `REQUESTS` (200), `WARMUP` (30), `TARGET_P95_MS` (500).

Kết quả chạy trên máy dev (H2 in-memory, Windows, Node 22, có warmup):

| Chỉ số | Giá trị |
|---|---|
| Request / đồng thời | 200 / 50 |
| Thành công / lỗi | 200 / 0 |
| Throughput | ~261 req/s |
| P50 / P95 / P99 | 141 / 332 / 346 ms |

Kết quả thay đổi theo cấu hình máy và database; khi chạy MySQL/Docker nên đo lại trong môi trường đích.

Kiểm chứng đăng ký tranh chỗ cuối bằng request đồng thời, bao gồm cấu hình sức chứa/số sinh viên/độ đồng thời và report kết quả + latency: [docs/CONCURRENT_REGISTRATION_TEST.md](docs/CONCURRENT_REGISTRATION_TEST.md).

### Chụp ảnh giao diện

```bash
cd frontend
npm run screenshots     # cần backend và frontend đang chạy
```

Ảnh được lưu vào `docs/screenshots/` (đăng nhập, dashboard sinh viên/giảng viên/quản trị, đăng ký, điểm, lớp học phần, modal).

### Build frontend

```bash
cd frontend
npm run build      # code-split React, Recharts, icons; xuất dist/
```

## CI/CD

- `.github/workflows/ci.yml`: chạy unit test backend, build jar, build frontend và E2E Playwright trên Chrome khi push/PR vào `main` hoặc `dev`.
- `.github/workflows/docker.yml`: build và push image backend/frontend lên GHCR khi push `main`, tag `v*` hoặc chạy thủ công. Dùng `GITHUB_TOKEN` với quyền `packages: write`.
- Để chạy E2E trong CI, workflow khởi động backend profile `h2` và chờ `/api/health` trước khi chạy Playwright.

## Biến môi trường backend

| Biến | Mặc định | Mô tả |
|---|---|---|
| `DB_URL` | MySQL localhost | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | Tài khoản DB |
| `DB_POOL_SIZE` | `20` | Số connection tối đa của HikariCP |
| `JWT_SECRET` | khóa mẫu 64 ký tự | Khóa ký JWT, nên đổi khi triển khai |
| `JWT_EXPIRATION_MS` | `86400000` | Thời hạn token (24 giờ) |
| `CORS_ORIGINS` | `http://localhost:5173,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:3000` | Origin được phép |
| `SEED_ENABLED` | `true` | Tạo dữ liệu mẫu khi DB trống |
| `SUPPORT_UPLOAD_DIR` | `./data/support-attachments` | Thư mục lưu tệp minh chứng ngoài vùng public |
| `SUPPORT_MAX_FILE_SIZE_BYTES` | `5242880` | Dung lượng tối đa mỗi tệp đính kèm (5 MB, cấu hình tạm thời cần chủ dự án xác nhận) |
| `SUPPORT_MAX_FILES_PER_REQUEST` | `3` | Số tệp tối đa trên mỗi phiếu hỗ trợ (cấu hình tạm thời cần chủ dự án xác nhận) |
| `SUPPORT_ALLOWED_EXTENSIONS` | `pdf,png,jpg,jpeg` | Phần mở rộng tệp cho phép (kiểm tra đối chiếu magic bytes nội dung thực tế) |
| `SUPPORT_SLA_NOTE` | *(trống)* | Ghi chú SLA nếu có quyết định chính thức (mặc định để trống, không tuyên bố SLA chính thức) |
| `GRADE_IMPORT_MAX_FILE_SIZE_BYTES` | `2097152` | Dung lượng tối đa tệp `.xlsx` import điểm (2 MB) |
| `GRADE_IMPORT_MAX_ROWS` | `500` | Số dòng dữ liệu sinh viên tối đa trong tệp `.xlsx` import điểm |
| `GRADE_IMPORT_MAX_SHEETS` | `1` | Số sheet tối đa trong workbook `.xlsx` import điểm |
| `GRADE_IMPORT_PARSE_TIMEOUT_MS` | `5000` | Thời gian xử lý phân tích tệp `.xlsx` tối đa (5 giây) |
| `GRADE_IMPORT_PREVIEW_TTL_SECONDS` | `900` | Thời hạn hiệu lực của phiên preview import điểm (15 phút) |

## Ghi chú

- Backend dùng `ddl-auto=update` để tiện phát triển; khi triển khai thật nên dùng migration (Flyway/Liquibase).
- JWT filter dùng cache ngắn hạn (30 giây) cho trạng thái tài khoản để giảm truy vấn DB; khóa/mở khóa tài khoản sẽ xóa cache ngay.
- Đã thêm index cho khóa ngoại của lớp học phần và đăng ký học phần để cải thiện truy vấn danh sách lớp.
- Yêu cầu hiệu năng NFR-01 đã được đo sơ bộ bằng load test ở trên; cần benchmark lại trên MySQL và môi trường triển khai thật.
- CSDL dùng tên bảng/cột tiếng Việt không dấu theo mô hình dữ liệu trong SRS.
