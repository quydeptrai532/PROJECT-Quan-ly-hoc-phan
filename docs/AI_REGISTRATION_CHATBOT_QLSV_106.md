# TÀI LIỆU KIẾN TRÚC VÀ ĐẶC TẢ KỸ THUẬT CHATBOT AI ĐĂNG KÝ HỌC PHẦN (QLSV-106 / TASK-AI-01)

## 1. Tổng quan dự án & Nhiệm vụ

Hệ thống **Chatbot AI tiếng Việt tư vấn và hỗ trợ đăng ký học phần** (Mã nhiệm vụ: `QLSV-106` / `TASK-AI-01`) được thiết kế nhằm cung cấp giao diện hội thoại thông minh hỗ trợ sinh viên, giảng viên và cán bộ quản trị tra cứu lịch học, sĩ số, chỗ còn, phòng học và thực hiện đăng ký lớp học phần một cách chuẩn xác, an toàn và có kiểm soát chặt chẽ.

> **Nguyên tắc cốt lõi:**  
> Chatbot chỉ đóng vai trò là giao diện hội thoại (Conversational UI / Intent Interpreter); toàn bộ dữ liệu lớp học, sĩ số, điều kiện tiên quyết, giới hạn tín chỉ và giao dịch đăng ký ghi nhận bắt buộc phải do các API/Service nghiệp vụ có phân quyền và locking giao dịch chính thức của hệ thống quyết định. Chatbot tuyệt đối không tự suy đoán hay bịa đặt dữ liệu (Zero Hallucination).

---

## 2. Kiến trúc hệ thống (System Architecture)

```
[ Frontend: React + TailwindCSS + Floating/Dedicated Chatbot UI ]
                           │
                           │  HTTP REST (Bearer JWT Auth)
                           ▼
             [ Spring Boot: ChatbotController ]
                           │
      ┌────────────────────┴────────────────────┐
      ▼                                         ▼
[ Security Guard ]                      [ Session & Rate Limit ]
• PromptInjectionGuard                  • ChatbotRateLimiter (30 req/min)
• Role Authorization Guard              • ChatbotSessionManager (TTL 60 min, max 20 msgs)
      │                                         │
      └────────────────────┬────────────────────┘
                           ▼
                  [ ChatbotService ]
                           │
          ┌────────────────┴────────────────┐
          ▼                                 ▼
[ VietnameseNluEngine ]           [ External AI Provider Abstraction ]
• Intent Classification           • AiChatProvider (Interface)
• Slot Extraction (Lớp/Môn/GV/Lịch)• GeminiAiChatProvider (LLM REST)
• Disambiguation Resolver         • Unconfigured Fallback to Safe Rules Engine
          │
          ▼
[ Official Business Services & Repositories ]
• CourseClassService: danhSach(hocKyId, keyword), chiTiet(id)
• RegistrationService: dangKy(lopHocPhanId, loaiDangKy), thoiKhoaBieu(), danhSachCuaToi()
• SemesterService: layHoacHienTai()
• StatisticsService: tongQuan(), theoMon() (chỉ cho QUAN_TRI)
```

---

## 3. Nguồn dữ liệu và Phân quyền API (Data Sources & Role Authorization)

Hệ thống truy cập dữ liệu hoàn toàn thông qua Spring Data JPA và Business Services đã tích hợp bảo mật:

| Vai trò (Role) | Dữ liệu được phép tra cứu | Thao tác được phép | Endpoint tương ứng |
| :--- | :--- | :--- | :--- |
| **SINH_VIEN** | Danh sách lớp học phần đang mở trong học kỳ, sĩ số hiện tại, chỗ còn lại, giảng viên, phòng, thời khóa biểu cá nhân, các lớp đã đăng ký của bản thân. | Yêu cầu đăng ký lớp học phần (bắt buộc qua bước xác nhận rõ ràng). | `/api/chatbot/message`<br/>`/api/chatbot/confirm`<br/>`/api/chatbot/reset` |
| **GIANG_VIEN** | Danh sách các lớp học phần được phân công giảng dạy trong kỳ, thời khóa biểu giảng dạy cá nhân. | Tra cứu thông tin phục vụ giảng dạy; không được tự ý đăng ký học phần thay sinh viên. | `/api/chatbot/message`<br/>`/api/chatbot/session` |
| **QUAN_TRI** | Thống kê tổng quan đào tạo (tổng sinh viên, giảng viên, môn học, lớp mở, lượt đăng ký), danh mục toàn trường. | Giám sát và quản lý; từ chối khi sinh viên/giảng viên hỏi dữ liệu thống kê quản trị. | `/api/chatbot/message`<br/>`/api/chatbot/status` |

Mọi dữ liệu lớp học phần và kết quả đăng ký trả về từ Chatbot đều bắt buộc đính kèm:
1. **Tên học kỳ đào tạo áp dụng** (`tenHocKy` - ví dụ: "Học kỳ 1 2026-2027").
2. **Thời điểm lấy dữ liệu thực tế** (`thoiDiemLayDuLieu` - định dạng: `dd/MM/yyyy HH:mm:ss`).

---

## 4. Luồng Đăng ký An toàn 2 bước (Two-Phase Registration Flow)

Để ngăn ngừa việc chatbot tự ý kích hoạt ghi danh sai lớp hoặc gây xung đột lịch học:

```
[ Sinh viên ] : "Tôi muốn đăng ký lớp INT201.01"
       │
       ▼
[ Chatbot NLU & Tra cứu CSDL ] : Nhận diện chính xác 1 lớp
       │
       ▼
[ Trả về thẻ xác nhận ] :
  • Môn: Lập trình mạng (INT201) - 3 tín chỉ
  • Mã lớp: INT201.01 | GV: Nguyễn Văn A | Phòng: A101
  • Lịch: Thứ 2, tiết 1-3 | Sĩ số: 35/45 (còn 10 chỗ)
  • Học kỳ: Học kỳ 1 2026-2027 (Thời điểm: 10/11/2026 11:00:00)
  • Trạng thái: AWAITING_CONFIRMATION (Chưa gọi API thay đổi dữ liệu)
  • Thông điệp: "Bạn có chắc chắn muốn đăng ký lớp này? Trả lời 'Đồng ý' hoặc bấm nút Xác nhận."
       │
       ├─────────────────────────────────────────────────┐
       ▼ (Nếu Sinh viên nói "Hủy")                       ▼ (Nếu Sinh viên nói "Đồng ý" / Bấm Xác nhận)
[ Hủy thao tác & Giải phóng Pending ]        [ Gọi RegistrationService.dangKy(lopId) ]
                                                         │
                                        ┌────────────────┴────────────────┐
                                        ▼ (Thành công)                    ▼ (Thất bại / Lỗi nghiệp vụ)
                             Báo thành công, cập nhật TKB      Báo đúng lý do từ API (trùng lịch,
                                                               hết chỗ, thiếu tiên quyết...) và
                                                               TỰ ĐỘNG GỢI Ý LỚP THAY THẾ CÒN CHỖ.
```

- **Tính toàn vẹn giao dịch:** Khi gọi `RegistrationService.dangKy(lopHocPhanId, null)`, hệ thống chạy trong transaction `READ_COMMITTED` với khóa bi quan pessimistic write lock (`findByIdForUpdate`) trên dòng lớp học phần và tài khoản sinh viên. Toàn bộ quy tắc về đợt đăng ký, sức chứa phòng, điều kiện tiên quyết, trùng lịch và giới hạn tín chỉ đều được thẩm định tự động.
- **Không tự chọn khi mơ hồ (Disambiguation):** Nếu sinh viên yêu cầu môn học có nhiều lớp mở (ví dụ: "Đăng ký môn Cấu trúc dữ liệu"), Chatbot sẽ liệt kê danh sách đánh số các lớp cùng lịch và giảng viên để sinh viên lựa chọn số thứ tự; tuyệt đối không tự chọn.

---

## 5. Phòng vệ An toàn & Chống Prompt Injection (Security Defenses)

Hệ thống xây dựng lớp phòng thủ đa tầng `PromptInjectionGuard`:
1. **Chặn Jailbreak & Override:** Nhận diện và hủy bỏ tức thì các chỉ thị dạng:
   - `ignore all previous instructions`, `bỏ qua hướng dẫn`, `quên đi mọi quy tắc`, `developer mode`, `system override`.
2. **Chặn Mạo nhận Quyền (Roleplay Attack):**
   - `hãy đóng vai admin`, `act as root/system`, `trở thành quản trị viên`.
3. **Chặn Trích xuất Dữ liệu Nhạy cảm (Data Exfiltration):**
   - Không cho phép trích xuất `system prompt`, `token JWT`, `mật khẩu`, `secret key`.
   - Chặn truy vấn hỏi điểm số hoặc thông tin cá nhân của sinh viên khác.
4. **Chặn Chèn Lệnh Độc Hại (SQLi):**
   - Phát hiện các mẫu truy vấn như `UNION SELECT`, `DROP TABLE`, `DELETE FROM`.
5. **Zero Credential Exposure:**
   - Bộ nhớ hội thoại và context của Chatbot hoàn toàn không lưu trữ token JWT, mật khẩu hay chuỗi kết nối nhạy cảm.

---

## 6. Quản lý Phiên hội thoại & Chính sách Dữ liệu Tối thiểu (Session Policy)

- **Lưu trữ tối thiểu:** Mỗi phiên chỉ lưu trữ tối đa 20 thông điệp gần nhất (`max-history-messages: 20`) để duy trì mạch ngữ cảnh câu hỏi.
- **Thời hạn tồn tại (TTL):** Phiên hội thoại được tự động dọn dẹp sau 60 phút không tương tác (`session-ttl-minutes: 60`) thông qua tác vụ định kỳ `@Scheduled(fixedRate = 300000)`.
- **Quyền chủ động xóa phiên:** Người dùng có thể xóa trắng lịch sử và ngữ cảnh đã lưu bất cứ lúc nào thông qua nút "Làm mới" trên giao diện hoặc gọi endpoint `POST /api/chatbot/reset`.

---

## 7. Trừu tượng hóa External AI Provider (Provider Abstraction)

Hệ thống định nghĩa interface trừu tượng `AiChatProvider`:
- Khi biến môi trường `CHATBOT_AI_API_KEY` được cung cấp: tích hợp với mô hình LLM bên ngoài (Google Gemini 1.5 Flash).
- Khi **chưa cấu hình API Key (`api-key: ""` hoặc `provider: NONE`)**:
  - Hệ thống **tuyệt đối không giả vờ AI hoạt động ảo giác**.
  - Trạng thái `/api/chatbot/status` báo rõ: `configured: false`, `missingDependencies: ["app.chatbot.ai.api-key (Biến môi trường CHATBOT_AI_API_KEY)"]`.
  - Hệ thống tự động kích hoạt **Động cơ phân tích quy chuẩn CSDL thời gian thực (Deterministic Rule-Based / Tool Calling Engine)**, đảm bảo người dùng vẫn tra cứu lớp, kiểm tra sĩ số, xem thời khóa biểu và thực hiện đăng ký an toàn 100%.

### Cấu hình trong `application.yml`:
```yaml
app:
  chatbot:
    ai:
      enabled: ${CHATBOT_AI_ENABLED:true}
      provider: ${CHATBOT_AI_PROVIDER:GEMINI}
      api-key: ${CHATBOT_AI_API_KEY:}
      model: ${CHATBOT_AI_MODEL:gemini-1.5-flash}
      endpoint: ${CHATBOT_AI_ENDPOINT:}
      temperature: ${CHATBOT_AI_TEMPERATURE:0.2}
      max-tokens: ${CHATBOT_AI_MAX_TOKENS:1000}
      rate-limit-per-minute: ${CHATBOT_AI_RATE_LIMIT_PER_MINUTE:30}
      session-ttl-minutes: ${CHATBOT_AI_SESSION_TTL_MINUTES:60}
      max-history-messages: ${CHATBOT_AI_MAX_HISTORY_MESSAGES:20}
```

---

## 8. Danh mục API REST Chatbot

| Phương thức | Đường dẫn API | Phân quyền | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/chatbot/status` | Public / All | Lấy thông tin trạng thái provider AI và dependency còn thiếu |
| `POST` | `/api/chatbot/message` | Authenticated | Gửi câu hỏi tiếng Việt, nhận phân tích và dữ liệu thực tế |
| `POST` | `/api/chatbot/confirm` | Role `SINH_VIEN` | Xác nhận đăng ký lớp học phần đã nhận diện |
| `GET` | `/api/chatbot/session` | Authenticated | Xem tóm tắt ngữ cảnh môn/học kỳ/lớp trong phiên hiện thời |
| `POST` | `/api/chatbot/reset` | Authenticated | Làm mới và xóa sạch lịch sử phiên hội thoại |

---

## 9. Kiểm thử Tích hợp và Đánh giá (Verification & Test Results)

Hệ thống được kiểm thử toàn diện thông qua bộ test suite tự động:
1. `PromptInjectionGuardTest`: Kiểm thử 5 ca phòng vệ tấn công prompt injection, jailbreak, roleplay bypass và SQL injection.
2. `VietnameseNluEngineTest`: Kiểm thử 8 ca phân tích ý định tiếng Việt (đăng ký, xác nhận, hủy, lớp thay thế, chỗ còn, lịch học, TKB cá nhân, thống kê).
3. `ChatbotIntegrationTest`: Kiểm thử 8 ca tích hợp end-to-end với cơ sở dữ liệu H2 seed data:
   - Kiểm tra endpoint status công khai khi chưa cấu hình API key.
   - Kiểm tra mã lỗi 401 khi chưa đăng nhập.
   - Kiểm tra ngăn chặn sinh viên xem thống kê quản trị.
   - Kiểm tra luồng đăng ký 2 bước bắt buộc xác nhận rõ ràng trước khi ghi danh.
   - Kiểm tra reset session và dọn dẹp ngữ cảnh.

**Kết quả chạy Maven Test:** `21/21 tests passed (0 failures, 0 errors)`.
