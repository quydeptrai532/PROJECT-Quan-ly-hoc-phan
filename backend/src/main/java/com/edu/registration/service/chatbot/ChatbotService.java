package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotAiProperties;
import com.edu.registration.dto.ChatbotDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.LecturerRepository;
import com.edu.registration.repository.StudentRepository;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.RegistrationService;
import com.edu.registration.service.SemesterService;
import com.edu.registration.service.StatisticsService;
import com.edu.registration.service.chatbot.ai.AiChatProvider;
import com.edu.registration.service.chatbot.ai.CommandCodeAiChatProvider;
import com.edu.registration.service.chatbot.ai.GeminiAiChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý hội thoại Chatbot AI tư vấn và hỗ trợ đăng ký học phần (QLSV-106 / TASK-AI-01).
 * Chatbot chỉ là giao diện hội thoại; toàn bộ dữ liệu lớp học phần, sĩ số, lịch học và đăng ký
 * được thực thi qua API/Service nghiệp vụ phân quyền chính thức.
 */
@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Pattern INFORMATION_QUESTION_PATTERN = Pattern.compile(
            "\\b(khi nao|bao gio|lam sao|lam the nao|nhu the nao|the nao|o dau|tai sao|vi sao|"
                    + "la gi|nghia la gi|co phai|huong dan|cach|ai day|giang vien nao|luc nao|ngay nao|may gio)\\b");

    // Chỉ dùng cho các lượt hội thoại xã giao; mọi dữ liệu lớp học phần vẫn do hệ thống tra cứu quyết định.
    private static final String CONVERSATIONAL_SYSTEM_PROMPT = """
            Bạn là "Trợ lý AI Đăng ký Học phần" của một hệ thống quản lý đào tạo đại học.
            Nhiệm vụ chính: hỗ trợ học vụ (tra cứu lớp học phần, số chỗ trống, lịch học, giảng viên, phòng học,
            thời khóa biểu, đăng ký học phần). Tên gọi này KHÔNG giới hạn phạm vi trả lời của bạn.
            Bạn cũng là một trợ lý trò chuyện thân thiện: được phép trả lời ngắn gọn các câu hỏi kiến thức phổ thông
            (lịch sử, địa lý, khoa học, văn hóa, đời sống, công nghệ, toán, tiếng Anh...) bằng kiến thức của bạn.
            Không được nói "tôi chỉ hỗ trợ học vụ" khi người dùng hỏi câu hỏi thông thường.
            Quy tắc:
            - Không bịa ra mã lớp, sĩ số, lịch học hay bất kỳ dữ liệu đào tạo nào; dữ liệu đó do hệ thống tra cứu trả về.
            - Không hứa hẹn thay đổi dữ liệu; mọi thao tác đăng ký phải qua quy trình xác nhận của hệ thống.
            - Trả lời tối đa 4-5 câu, thân thiện, có thể gợi ý 2-3 câu hỏi mẫu nếu phù hợp.
            - Với dữ liệu thời gian thực (thời tiết, giá cả, tin tức...), nói rõ bạn không có dữ liệu thời gian thực
              và hướng dẫn người dùng tra cứu; tuyệt đối không bịa số liệu.
            - Các dữ kiện học vụ dưới đây chỉ áp dụng cho câu hỏi học vụ; câu hỏi kiến thức chung thì dùng kiến thức của bạn.

            Kiến thức học vụ của hệ thống:
            - Sinh viên đăng ký/hủy học phần tại trang Đăng ký; chỉ thực hiện được trong thời gian đăng ký của học kỳ.
            - Điều kiện đăng ký: đạt tất cả môn tiên quyết (điểm tổng kết >= 5.0 đã công bố), không trùng thứ/tiết với lớp đã đăng ký,
              không vượt sĩ số, không vượt giới hạn tín chỉ của học kỳ, không đăng ký hai lớp cùng một môn trong cùng học kỳ.
            - Điểm tổng kết = 0.4 x điểm quá trình + 0.6 x điểm cuối kỳ, làm tròn 1 chữ số, thang 0-10; điểm đạt >= 5.0.
            - GPA hệ 4 quy đổi: >=8.5 -> 4.0; >=8.0 -> 3.5; >=7.0 -> 3.0; >=6.5 -> 2.5; >=5.5 -> 2.0; >=5.0 -> 1.5; >=4.0 -> 1.0; còn lại 0.
              Tín chỉ tích lũy chỉ tính các môn đạt (>= 5.0).
            - Điểm chỉ hiển thị khi bảng điểm lớp học phần đã được công bố.
            - Các trang chức năng: Đăng ký (/dang-ky), Thời khóa biểu (/thoi-khoa-bieu), Điểm & GPA (/diem),
              Lịch sử học tập (/lich-su-hoc-tap), Học lại/cải thiện (/dang-ky-hoc-lai), Thông báo (/thong-bao),
              Phiếu hỗ trợ (/yeu-cau-ho-tro), Cảnh báo học vụ (/canh-bao-hoc-vu).
            - Hệ thống không quản lý học phí; nếu được hỏi, hướng dẫn liên hệ phòng Tài chính - Kế toán của trường.
            """;

    private final ChatbotAiProperties properties;
    private final ChatbotSessionManager sessionManager;
    private final PromptInjectionGuard promptInjectionGuard;
    private final VietnameseNluEngine nluEngine;
    private final ChatbotRateLimiter rateLimiter;
    private final GeminiAiChatProvider geminiAiChatProvider;
    private final CommandCodeAiChatProvider commandCodeAiChatProvider;
    private final WeatherService weatherService;
    private final CourseClassService lopHocPhanService;
    private final RegistrationService dangKyService;
    private final SemesterService hocKyService;
    private final StatisticsService thongKeService;
    private final CurrentUser currentUser;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ChatbotService(
            ChatbotAiProperties properties,
            ChatbotSessionManager sessionManager,
            PromptInjectionGuard promptInjectionGuard,
            VietnameseNluEngine nluEngine,
            ChatbotRateLimiter rateLimiter,
            GeminiAiChatProvider geminiAiChatProvider,
            CommandCodeAiChatProvider commandCodeAiChatProvider,
            WeatherService weatherService,
            CourseClassService lopHocPhanService,
            RegistrationService dangKyService,
            SemesterService hocKyService,
            StatisticsService thongKeService,
            CurrentUser currentUser,
            StudentRepository studentRepository,
            LecturerRepository lecturerRepository) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.promptInjectionGuard = promptInjectionGuard;
        this.nluEngine = nluEngine;
        this.rateLimiter = rateLimiter;
        this.geminiAiChatProvider = geminiAiChatProvider;
        this.commandCodeAiChatProvider = commandCodeAiChatProvider;
        this.weatherService = weatherService;
        this.lopHocPhanService = lopHocPhanService;
        this.dangKyService = dangKyService;
        this.hocKyService = hocKyService;
        this.thongKeService = thongKeService;
        this.currentUser = currentUser;
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
    }

    public ChatbotService(
            ChatbotAiProperties properties,
            ChatbotSessionManager sessionManager,
            PromptInjectionGuard promptInjectionGuard,
            VietnameseNluEngine nluEngine,
            ChatbotRateLimiter rateLimiter,
            GeminiAiChatProvider geminiAiChatProvider,
            CommandCodeAiChatProvider commandCodeAiChatProvider,
            WeatherService weatherService,
            CourseClassService lopHocPhanService,
            RegistrationService dangKyService,
            SemesterService hocKyService,
            StatisticsService thongKeService,
            CurrentUser currentUser) {
        this(properties, sessionManager, promptInjectionGuard, nluEngine, rateLimiter,
                geminiAiChatProvider, commandCodeAiChatProvider, weatherService, lopHocPhanService,
                dangKyService, hocKyService, thongKeService, currentUser, null, null);
    }

    public ChatbotDtos.ChatbotStatusDto getStatus() {
        return activeAiProvider().getStatus();
    }

    private AiChatProvider activeAiProvider() {
        return properties.isCommandCodeProvider() ? commandCodeAiChatProvider : geminiAiChatProvider;
    }

    public ChatbotDtos.ChatSessionSummaryDto getSessionSummary(String sessionId) {
        User user = currentUser.get();
        ChatbotSessionManager.UserSession session = sessionManager.getOrCreateSession(sessionId, user.getUsername(), user.getRole().name());
        return new ChatbotDtos.ChatSessionSummaryDto(
                session.getSessionId(),
                session.getUsername(),
                session.getRole(),
                session.getCurrentHocKyId(),
                session.getCurrentHocKyTen(),
                session.getCurrentMonHocTen(),
                session.getCurrentLopHocPhanMa(),
                session.getPendingRegistrationClass(),
                session.getMessages().size(),
                session.getLastActive()
        );
    }

    public void resetSession(String sessionId) {
        User user = currentUser.get();
        sessionManager.resetUserSession(user.getUsername());
        rateLimiter.clear("user:" + user.getUsername());
    }

    public ChatbotDtos.ChatResponse handleUserMessage(ChatbotDtos.ChatRequest request) {
        User user = currentUser.get();
        String username = user.getUsername();
        Role role = user.getRole();

        // 1. Kiểm tra Rate Limiting & bảo vệ chi phí
        if (!rateLimiter.isAllowed("user:" + username)) {
            return buildErrorResponse(request.sessionId(),
                    "Bạn đã gửi quá nhiều yêu cầu trong thời gian ngắn (vượt giới hạn " + properties.getRateLimitPerMinute()
                            + " yêu cầu/phút). Vui lòng đợi giây lát trước khi gửi câu hỏi tiếp theo.",
                    ChatbotDtos.ChatActionType.ERROR);
        }

        // 2. Kiểm tra Prompt Injection và các chỉ thị phá hoại bảo mật
        PromptInjectionGuard.GuardResult guardResult = promptInjectionGuard.checkInput(request.message());
        if (!guardResult.safe()) {
            return buildSecurityBlockedResponse(request.sessionId(), guardResult.reason());
        }

        // 3. Lấy hoặc tạo phiên hội thoại
        ChatbotSessionManager.UserSession session = sessionManager.getOrCreateSession(request.sessionId(), username, role.name());
        session.addMessage("USER", request.message(), properties.getMaxHistoryMessages());

        // 4. Xác định học kỳ áp dụng
        Semester hocKy;
        try {
            Long hocKyId = request.hocKyId() != null ? request.hocKyId() : session.getCurrentHocKyId();
            hocKy = hocKyService.layHoacHienTai(hocKyId);
            session.setCurrentHocKyId(hocKy.getId());
            session.setCurrentHocKyTen(hocKy.getTen());
        } catch (Exception e) {
            log.warn("Không xác định được học kỳ: {}", e.getMessage());
            return buildErrorResponse(session.getSessionId(), "Không thể xác minh thông tin học kỳ hiện tại trong hệ thống. Vui lòng thử lại sau.", ChatbotDtos.ChatActionType.ERROR);
        }

        String thoiDiem = LocalDateTime.now().format(TIME_FORMATTER);

        // 5. Phân tích NLU ý định người dùng
        VietnameseNluEngine.NluParseResult nlu = nluEngine.parse(request.message());

        // 6. Xử lý logic theo Intent
        ChatbotDtos.ChatResponse response;
        try {
            response = processIntent(session, hocKy, nlu, thoiDiem, role);
        } catch (Exception e) {
            log.error("Lỗi xử lý câu hỏi chatbot: {}", e.getMessage(), e);
            response = buildErrorResponse(session.getSessionId(),
                    "Không thể xác minh dữ liệu lớp học phần vào thời điểm này (" + thoiDiem + "). Vui lòng thử lại sau.",
                    ChatbotDtos.ChatActionType.ERROR);
        }

        session.addMessage("BOT", response.reply(), properties.getMaxHistoryMessages());
        return response;
    }

    private ChatbotDtos.ChatResponse processIntent(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            VietnameseNluEngine.NluParseResult nlu,
            String thoiDiem,
            Role role) {

        if (hocKy == null) {
            return buildErrorResponse(session.getSessionId(),
                    "Không xác định được học kỳ hiện tại trong hệ thống. Vui lòng thử lại sau.",
                    ChatbotDtos.ChatActionType.ERROR);
        }

        // --- Intent: CHÀO HỎI ---
        if (nlu.intent() == VietnameseNluEngine.Intent.GREETING) {
            String fallback = buildGreeting(session, role);
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, greetingSuggestions(role), hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: CẢM ƠN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.THANKS) {
            String fallback = "Rất vui được hỗ trợ bạn! 😊 Nếu còn thắc mắc gì về lớp học phần, lịch học, phòng học hay đăng ký, bạn cứ nhắn mình nhé.";
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, greetingSuggestions(role), hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: TẠM BIỆT ---
        if (nlu.intent() == VietnameseNluEngine.Intent.GOODBYE) {
            String fallback = "Tạm biệt bạn! 👋 Chúc bạn một ngày tốt lành. Khi cần tra cứu lớp học phần hay đăng ký, mình luôn ở đây.";
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: GIỚI THIỆU VỀ TRỢ LÝ ---
        if (nlu.intent() == VietnameseNluEngine.Intent.SMALL_TALK) {
            String fallback = "Mình là **Trợ lý AI Đăng ký Học phần (QLSV Assistant)** — trợ lý ảo của hệ thống quản lý đào tạo.\n\n" +
                    "Mình tra cứu trực tiếp CSDL thời gian thực để giúp bạn tìm lớp học phần, kiểm tra chỗ trống, " +
                    "xem lịch học, tra cứu giảng viên/phòng học và hướng dẫn đăng ký an toàn.\n\n" +
                    "Bạn cần mình giúp gì không?";
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, greetingSuggestions(role), hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: THỜI TIẾT (Open-Meteo) ---
        if (nlu.intent() == VietnameseNluEngine.Intent.THOI_TIET) {
            String report = weatherService.getWeatherReport(nlu.rawMessage())
                    .orElse("Mình chưa lấy được dữ liệu thời tiết lúc này. Bạn thử lại sau ít phút hoặc kiểm tra ứng dụng thời tiết nhé.");
            return buildResponse(session, report, ChatbotDtos.ChatActionType.NONE,
                    null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: THÔNG TIN CÁ NHÂN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.THONG_TIN_CA_NHAN) {
            return handleProfileInfo(session, hocKy, thoiDiem, role, currentUser.get());
        }

        // --- Intent: HƯỚNG DẪN ĐỔI MẬT KHẨU ---
        if (nlu.intent() == VietnameseNluEngine.Intent.DOI_MAT_KHAU) {
            return handlePasswordChangeGuide(session, hocKy, thoiDiem);
        }

        // --- Intent: LỚP CỦA TÔI (Lớp phụ trách của GV hoặc lớp đã đăng ký của SV) ---
        if (nlu.intent() == VietnameseNluEngine.Intent.LOP_CUA_TOI) {
            return handleMyClasses(session, hocKy, thoiDiem, role);
        }

        // --- Intent: THỜI KHÓA BIỂU CÁ NHÂN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.TKB_CA_NHAN) {
            return handlePersonalTimetable(session, hocKy, nlu, thoiDiem, role);
        }

        // --- Câu hỏi mang tính giải thích/hướng dẫn (không có mã lớp cụ thể) ---
        // Ưu tiên trả lời tự nhiên thay vì hiểu nhầm thành lệnh đăng ký/tra cứu.
        if (shouldAnswerConversationally(nlu)) {
            String fallback = "Mình chưa có đủ dữ liệu để trả lời chính xác câu hỏi này. " +
                    "Bạn có thể hỏi mình về đăng ký/hủy học phần, thời khóa biểu, lớp học phần, điểm/GPA, " +
                    "học lại/cải thiện, thông báo hoặc phiếu hỗ trợ nhé.";
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, greetingSuggestions(role), hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: XÁC NHẬN ĐĂNG KÝ ---
        if (nlu.intent() == VietnameseNluEngine.Intent.XAC_NHAN) {
            if (!session.isAwaitingConfirmation() || session.getPendingRegistrationClass() == null) {
                return buildResponse(session,
                        "Hiện tại không có lớp học phần nào đang chờ xác nhận đăng ký. " +
                                "Để đăng ký, bạn vui lòng chỉ định lớp muốn đăng ký (ví dụ: 'Tôi muốn đăng ký lớp LHP01').",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
            return executeConfirmedRegistration(session, session.getPendingRegistrationClass().id(), thoiDiem);
        }

        // --- Intent: HỦY XÁC NHẬN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.HUY_XAC_NHAN) {
            if (session.isAwaitingConfirmation() && session.getPendingRegistrationClass() != null) {
                String maLop = session.getPendingRegistrationClass().ma();
                session.clearPendingRegistration();
                return buildResponse(session,
                        "Đã hủy thao tác đăng ký lớp " + maLop + ". Bạn có thể tiếp tục tra cứu các lớp học phần khác.",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
            return buildResponse(session, "Đã ghi nhận yêu cầu hủy thao tác.", ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        // --- Người dùng chọn số thứ tự từ danh sách trước đó (Disambiguation Selection) ---
        if (nlu.selectionIndex() != null && !session.getCandidateClasses().isEmpty()) {
            int idx = nlu.selectionIndex() - 1;
            if (idx >= 0 && idx < session.getCandidateClasses().size()) {
                ChatbotDtos.ClassCardDto chosen = session.getCandidateClasses().get(idx);
                session.setCurrentLopHocPhanId(chosen.id());
                session.setCurrentLopHocPhanMa(chosen.ma());
                session.setCurrentMonHocId(chosen.monHocId());
                session.setCurrentMonHocTen(chosen.tenMon());

                if (session.isAwaitingConfirmation() || nlu.intent() == VietnameseNluEngine.Intent.DANG_KY) {
                    return prepareRegistrationConfirmation(session, chosen, hocKy, thoiDiem);
                } else {
                    return buildResponse(session,
                            "Bạn đã chọn lớp **" + chosen.ma() + "** môn " + chosen.tenMon() + " (" + chosen.soTinChi() + " tín chỉ).\n" +
                                    "- Giảng viên: " + chosen.giangVien() + "\n" +
                                    "- Lịch học: Thứ " + chosen.thu() + ", tiết " + chosen.tietBatDau() + "-" + (chosen.tietBatDau() + chosen.soTiet() - 1) + "\n" +
                                    "- Phòng: " + chosen.phong() + "\n" +
                                    "- Sĩ số: " + chosen.siSoHienTai() + "/" + chosen.siSoToiDa() + " (còn " + chosen.conLai() + " chỗ trống).\n\n" +
                                    (role == Role.SINH_VIEN ? "Bạn có muốn đăng ký lớp này không? Hãy nói 'Đăng ký lớp này' để chuẩn bị ghi danh." : ""),
                            ChatbotDtos.ChatActionType.NONE, chosen, List.of(chosen), null, hocKy.getTen(), thoiDiem, false);
                }
            }
        }

        // --- Intent: ĐĂNG KÝ HỌC PHẦN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.DANG_KY) {
            if (role != Role.SINH_VIEN) {
                return buildResponse(session,
                        "Quyền hạn bị từ chối: Chức năng đăng ký học phần chỉ dành riêng cho tài khoản Sinh viên.",
                        ChatbotDtos.ChatActionType.SECURITY_BLOCKED, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            // Tìm lớp cụ thể
            List<CourseClassDtos.LopHocPhanDto> matched = resolveCandidateClasses(session, hocKy, nlu);
            if (matched.isEmpty()) {
                return buildResponse(session,
                        "Không tìm thấy lớp học phần phù hợp với yêu cầu đăng ký của bạn trong " + hocKy.getTen() + ". " +
                                "Vui lòng kiểm tra lại mã lớp hoặc tên môn học.",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            if (matched.size() > 1) {
                // Nhiều lớp => hỏi lại, không được tự chọn!
                List<ChatbotDtos.ClassCardDto> cardList = matched.stream().map(l -> toClassCard(l, thoiDiem)).toList();
                session.setCandidateClasses(cardList);
                session.clearPendingRegistration();

                StringBuilder sb = new StringBuilder();
                sb.append("Tìm thấy **").append(matched.size()).append("** lớp học phần phù hợp trong ").append(hocKy.getTen()).append(":\n\n");
                for (int i = 0; i < cardList.size(); i++) {
                    ChatbotDtos.ClassCardDto c = cardList.get(i);
                    sb.append(String.format(Locale.ROOT, "%d. **%s** - Môn: %s | GV: %s | Thứ %d (tiết %d-%d) | Phòng %s | Còn: %d/%d chỗ\n",
                            i + 1, c.ma(), c.tenMon(), c.giangVien(), c.thu(), c.tietBatDau(), c.tietBatDau() + c.soTiet() - 1, c.phong(), c.conLai(), c.siSoToiDa()));
                }
                sb.append("\n**Vui lòng nhập số thứ tự (ví dụ: '1') hoặc mã lớp bạn muốn đăng ký.** Hệ thống không tự ý chọn lớp.");
                return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.SELECTION_REQUIRED, null, cardList, null, hocKy.getTen(), thoiDiem, false);
            }

            // Xác định được duy nhất 1 lớp => hiển thị thông tin và yêu cầu xác nhận rõ ràng
            CourseClassDtos.LopHocPhanDto target = matched.get(0);
            ChatbotDtos.ClassCardDto card = toClassCard(target, thoiDiem);
            return prepareRegistrationConfirmation(session, card, hocKy, thoiDiem);
        }

        // --- Intent: TÌM LỚP THAY THẾ (khi lớp đầy hoặc cần đổi lớp) ---
        if (nlu.intent() == VietnameseNluEngine.Intent.LOP_THAY_THE) {
            String searchSubject = nlu.tenMonOrKeyword() != null ? nlu.tenMonOrKeyword() : session.getCurrentMonHocTen();
            if (searchSubject == null && session.getCurrentLopHocPhanMa() != null) {
                searchSubject = session.getCurrentLopHocPhanMa();
            }

            List<CourseClassDtos.LopHocPhanDto> all = lopHocPhanService.danhSach(hocKy.getId(), searchSubject);
            List<ChatbotDtos.ClassCardDto> alternatives = all.stream()
                    .filter(l -> !l.daHuy() && l.conLai() > 0)
                    .filter(l -> session.getCurrentLopHocPhanId() == null || !l.id().equals(session.getCurrentLopHocPhanId()))
                    .map(l -> toClassCard(l, thoiDiem))
                    .toList();

            if (alternatives.isEmpty()) {
                return buildResponse(session,
                        "Hiện tại không còn lớp học phần thay thế nào còn chỗ cho môn học này trong " + hocKy.getTen() +
                                " (Dữ liệu thời điểm: " + thoiDiem + "). Vui lòng liên hệ phòng đào tạo hoặc chờ đợt đăng ký bổ sung.",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            session.setCandidateClasses(alternatives);
            StringBuilder sb = new StringBuilder();
            sb.append("Tìm thấy **").append(alternatives.size()).append("** lớp học phần thay thế còn chỗ trong ").append(hocKy.getTen()).append(":\n\n");
            for (int i = 0; i < alternatives.size(); i++) {
                ChatbotDtos.ClassCardDto c = alternatives.get(i);
                sb.append(String.format(Locale.ROOT, "%d. **%s** - Môn: %s | GV: %s | Thứ %d (tiết %d-%d) | Phòng %s | **Còn: %d chỗ**\n",
                        i + 1, c.ma(), c.tenMon(), c.giangVien(), c.thu(), c.tietBatDau(), c.tietBatDau() + c.soTiet() - 1, c.phong(), c.conLai()));
            }
            sb.append("\nBạn có thể chọn lớp bằng cách gõ số thứ tự hoặc yêu cầu: 'Đăng ký lớp [Mã lớp]'.");
            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, alternatives, null, hocKy.getTen(), thoiDiem, false);
        }


        // --- Intent: THỐNG KÊ QUẢN TRỊ ---
        if (nlu.intent() == VietnameseNluEngine.Intent.THONG_KE) {
            if (role != Role.QUAN_TRI) {
                return buildResponse(session,
                        "Quyền hạn bị từ chối: Dữ liệu thống kê quản trị chỉ dành cho tài khoản có quyền Quản trị viên (QUAN_TRI).",
                        ChatbotDtos.ChatActionType.SECURITY_BLOCKED, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
            StatDtos.TongQuanDto tq = thongKeService.tongQuan();
            String reply = String.format(Locale.ROOT,
                    "**Thống kê tổng quan đào tạo** (Học kỳ: %s - Cập nhật lúc: %s):\n" +
                            "- Tổng số sinh viên: %d\n" +
                            "- Tổng số giảng viên: %d\n" +
                            "- Tổng số môn học: %d\n" +
                            "- Số lớp học phần đang mở: %d\n" +
                            "- Tổng lượt đăng ký học phần: %d",
                    hocKy.getTen(), thoiDiem, tq.soSinhVien(), tq.soGiangVien(), tq.soMonHoc(), tq.soLopHocPhan(), tq.soDangKy());
            return buildResponse(session, reply, ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: TRỢ GIÚP ---
        if (nlu.intent() == VietnameseNluEngine.Intent.TRO_GIUP) {
            String guide = getHelpText(role);
            return buildResponse(session, guide, ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: KHÔNG XÁC ĐỊNH ---
        // Không tự động liệt kê toàn bộ lớp học phần khi câu hỏi ngoài phạm vi hiểu biết.
        if (nlu.intent() == VietnameseNluEngine.Intent.UNKNOWN) {
            String fallback = "Xin lỗi, mình chưa hiểu rõ ý bạn. Bạn có thể hỏi mình về:\n" +
                    "- Tìm lớp học phần và số chỗ còn trống\n" +
                    "- Xem thời khóa biểu của bạn\n" +
                    "- Tra cứu giảng viên, phòng học, lịch học\n" +
                    "- Hướng dẫn đăng ký học phần\n\n" +
                    "Bạn thử diễn đạt lại giúp mình, hoặc gõ \"Hướng dẫn\" để xem đầy đủ nhé.";
            return buildResponse(session, conversationalReply(session, hocKy, nlu.rawMessage(), fallback),
                    ChatbotDtos.ChatActionType.NONE, null, null, greetingSuggestions(role), hocKy.getTen(), thoiDiem, false);
        }

        // --- Intent: TRA CỨU LỚP / SĨ SỐ / CÒN CHỖ / LỊCH / GIẢNG VIÊN / PHÒNG ---
        List<CourseClassDtos.LopHocPhanDto> classes = queryClasses(session, hocKy, nlu, role);

        if (classes.isEmpty()) {
            return buildResponse(session,
                    "Không tìm thấy lớp học phần nào phù hợp với câu hỏi của bạn trong " + hocKy.getTen() +
                            " (Thời điểm kiểm tra: " + thoiDiem + "). Bạn có thể thử tìm theo tên môn, mã môn hoặc giảng viên.",
                    ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }

        List<ChatbotDtos.ClassCardDto> cards = classes.stream().map(l -> toClassCard(l, thoiDiem)).toList();
        session.setCandidateClasses(cards);

        // Lưu ngữ cảnh môn / lớp gần nhất
        if (!classes.isEmpty()) {
            CourseClassDtos.LopHocPhanDto first = classes.get(0);
            session.setCurrentMonHocId(first.monHocId());
            session.setCurrentMonHocTen(first.tenMon());
            if (classes.size() == 1) {
                session.setCurrentLopHocPhanId(first.id());
                session.setCurrentLopHocPhanMa(first.ma());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Tìm thấy **").append(cards.size()).append("** lớp học phần trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");

        for (int i = 0; i < cards.size(); i++) {
            ChatbotDtos.ClassCardDto c = cards.get(i);
            String status = c.daHuy() ? "[ĐÃ HỦY]" : (c.conLai() > 0 ? "Còn " + c.conLai() + " chỗ" : "**ĐÃ ĐẦY**");
            sb.append(String.format(Locale.ROOT, "%d. **%s** (%s - %d tín chỉ)\n" +
                            "   - Giảng viên: %s | Phòng: %s\n" +
                            "   - Lịch học: Thứ %d, tiết %d-%d\n" +
                            "   - Sĩ số: %d/%d (%s)\n\n",
                    i + 1, c.ma(), c.tenMon(), c.soTinChi(), c.giangVien(), c.phong(), c.thu(), c.tietBatDau(), c.tietBatDau() + c.soTiet() - 1,
                    c.siSoHienTai(), c.siSoToiDa(), status));
        }

        if (role == Role.SINH_VIEN) {
            sb.append("Để đăng ký lớp, bạn có thể nói 'Tôi muốn đăng ký lớp [Mã lớp]' hoặc chọn số thứ tự.");
        }

        return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, cards, null, hocKy.getTen(), thoiDiem, false);
    }

    private ChatbotDtos.ChatResponse prepareRegistrationConfirmation(
            ChatbotSessionManager.UserSession session,
            ChatbotDtos.ClassCardDto card,
            Semester hocKy,
            String thoiDiem) {

        session.setPendingRegistrationClass(card);
        session.setCurrentLopHocPhanId(card.id());
        session.setCurrentLopHocPhanMa(card.ma());
        session.setCurrentMonHocId(card.monHocId());
        session.setCurrentMonHocTen(card.tenMon());

        String message = String.format(Locale.ROOT,
                "### Xác nhận thông tin đăng ký học phần\n\n" +
                        "Hệ thống đã nhận diện lớp học phần bạn yêu cầu:\n" +
                        "- **Môn học:** %s (%s) - %d tín chỉ\n" +
                        "- **Mã lớp:** `%s`\n" +
                        "- **Giảng viên:** %s\n" +
                        "- **Lịch học:** Thứ %d, tiết %d - %d\n" +
                        "- **Phòng học:** %s\n" +
                        "- **Sĩ số hiện tại:** %d/%d (còn %d chỗ trống)\n" +
                        "- **Học kỳ:** %s\n" +
                        "- **Thời điểm kiểm tra dữ liệu:** %s\n\n" +
                        "⚠️ **Lưu ý bảo đảm:** Hệ thống CHƯA gọi API thay đổi dữ liệu cho đến khi bạn xác nhận rõ ràng. " +
                        "Bạn có chắc chắn muốn đăng ký lớp này không?\n\n" +
                        "👉 Vui lòng trả lời **'Xác nhận'** hoặc **'Đồng ý'** (hoặc bấm nút Xác nhận bên dưới).",
                card.tenMon(), card.maMon(), card.soTinChi(),
                card.ma(), card.giangVien(),
                card.thu(), card.tietBatDau(), card.tietBatDau() + card.soTiet() - 1,
                card.phong(), card.siSoHienTai(), card.siSoToiDa(), card.conLai(),
                hocKy.getTen(), thoiDiem);

        return buildResponse(session, message, ChatbotDtos.ChatActionType.CONFIRM_REGISTRATION, card, List.of(card),
                List.of("Xác nhận đăng ký", "Hủy thao tác"), hocKy.getTen(), thoiDiem, true);
    }

    public ChatbotDtos.ChatResponse confirmRegistration(ChatbotDtos.ConfirmRegistrationRequest request) {
        User user = currentUser.get();
        if (user.getRole() != Role.SINH_VIEN) {
            throw ApiException.badRequest("Chức năng chỉ dành cho Sinh viên");
        }
        ChatbotSessionManager.UserSession session = sessionManager.getOrCreateSession(request.sessionId(), user.getUsername(), user.getRole().name());
        String thoiDiem = LocalDateTime.now().format(TIME_FORMATTER);
        return executeConfirmedRegistration(session, request.lopHocPhanId(), thoiDiem);
    }

    private ChatbotDtos.ChatResponse executeConfirmedRegistration(
            ChatbotSessionManager.UserSession session,
            Long lopHocPhanId,
            String thoiDiem) {

        try {
            // TUYỆT ĐỐI GỌI API ĐĂNG KÝ HIỆN CÓ CỦA RegistrationService, KHÔNG VIẾT ĐƯỜNG RIÊNG!
            RegistrationDtos.DangKyDto result = dangKyService.dangKy(lopHocPhanId, null);
            session.clearPendingRegistration();

            String successMsg = String.format(Locale.ROOT,
                    "🎉 **Đăng ký học phần thành công!**\n\n" +
                            "- Môn học: %s (%s) - %d tín chỉ\n" +
                            "- Lớp học phần: `%s`\n" +
                            "- Giảng viên: %s | Phòng: %s\n" +
                            "- Lịch học: Thứ %d, tiết %d-%d\n" +
                            "- Học kỳ: %s\n" +
                            "- Thời gian ghi nhận: %s\n\n" +
                            "Hệ thống đã lưu thông tin vào thời khóa biểu của bạn.",
                    result.tenMon(), result.maMon(), result.soTinChi(),
                    result.maLop(), result.tenGiangVien(), result.phong(),
                    result.thu(), result.tietBatDau(), result.tietBatDau() + result.soTiet() - 1,
                    result.tenHocKy(), thoiDiem);

            return buildResponse(session, successMsg, ChatbotDtos.ChatActionType.REGISTRATION_SUCCESS, null, null, null, result.tenHocKy(), thoiDiem, false);

        } catch (ApiException e) {
            String errorMsg = e.getMessage();
            log.warn("Đăng ký lớp học phần {} thất bại qua Chatbot: {}", lopHocPhanId, errorMsg);
            session.clearPendingRegistration();

            // Nếu thất bại do hết chỗ hoặc xung đột lịch, chủ động gợi ý lớp thay thế
            List<ChatbotDtos.ClassCardDto> suggestions = new ArrayList<>();
            Semester hocKy = hocKyService.layHoacHienTai(session.getCurrentHocKyId());
            if (session.getCurrentMonHocId() != null) {
                List<CourseClassDtos.LopHocPhanDto> alternatives = lopHocPhanService.danhSach(hocKy.getId(), session.getCurrentMonHocTen());
                suggestions = alternatives.stream()
                        .filter(l -> !l.daHuy() && l.conLai() > 0 && !l.id().equals(lopHocPhanId))
                        .map(l -> toClassCard(l, thoiDiem))
                        .toList();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("❌ **Đăng ký không thành công**\n\n");
            sb.append("Lý do từ hệ thống: **").append(errorMsg).append("**\n");
            sb.append("(Thời điểm kiểm tra: ").append(thoiDiem).append(")\n\n");

            if (!suggestions.isEmpty()) {
                sb.append("💡 **Gợi ý các lớp học phần thay thế còn chỗ:**\n");
                for (int i = 0; i < suggestions.size(); i++) {
                    ChatbotDtos.ClassCardDto alt = suggestions.get(i);
                    sb.append(String.format(Locale.ROOT, "%d. **%s** | GV: %s | Thứ %d (tiết %d-%d) | Còn %d chỗ\n",
                            i + 1, alt.ma(), alt.giangVien(), alt.thu(), alt.tietBatDau(), alt.tietBatDau() + alt.soTiet() - 1, alt.conLai()));
                }
                session.setCandidateClasses(suggestions);
                sb.append("\nBạn có thể nhập số thứ tự hoặc mã lớp để đăng ký lớp thay thế.");
            }

            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.REGISTRATION_FAILED, null, suggestions, null, hocKy.getTen(), thoiDiem, false);
        }
    }

    private List<CourseClassDtos.LopHocPhanDto> resolveCandidateClasses(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            VietnameseNluEngine.NluParseResult nlu) {

        // 1. Nếu có mã lớp rõ ràng (vd LHP01)
        if (nlu.maLop() != null) {
            List<CourseClassDtos.LopHocPhanDto> list = lopHocPhanService.danhSach(hocKy.getId(), nlu.maLop());
            List<CourseClassDtos.LopHocPhanDto> exact = list.stream().filter(l -> l.ma().equalsIgnoreCase(nlu.maLop())).toList();
            if (!exact.isEmpty()) return exact;
            return list;
        }

        // 2. Nếu có mã môn (vd IT001)
        if (nlu.maMon() != null) {
            return lopHocPhanService.danhSach(hocKy.getId(), nlu.maMon());
        }

        // 3. Nếu có từ khóa tên môn
        if (nlu.tenMonOrKeyword() != null && !nlu.tenMonOrKeyword().isBlank()) {
            return lopHocPhanService.danhSach(hocKy.getId(), nlu.tenMonOrKeyword());
        }

        // 4. Nếu trong session đã có mã lớp hoặc môn học đang thảo luận
        if (session.getCurrentLopHocPhanMa() != null) {
            List<CourseClassDtos.LopHocPhanDto> list = lopHocPhanService.danhSach(hocKy.getId(), session.getCurrentLopHocPhanMa());
            if (!list.isEmpty()) return list;
        }

        if (session.getCurrentMonHocTen() != null) {
            return lopHocPhanService.danhSach(hocKy.getId(), session.getCurrentMonHocTen());
        }

        return List.of();
    }

    private List<CourseClassDtos.LopHocPhanDto> queryClasses(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            VietnameseNluEngine.NluParseResult nlu,
            Role role) {

        String keyword = null;
        if (nlu.maLop() != null) keyword = nlu.maLop();
        else if (nlu.maMon() != null) keyword = nlu.maMon();
        else if (nlu.tenMonOrKeyword() != null) keyword = nlu.tenMonOrKeyword();
        else if (nlu.tenGiangVien() != null) keyword = nlu.tenGiangVien();
        else if (session.getCurrentMonHocTen() != null && (nlu.thu() != null || nlu.onlyAvailableSeats() || nlu.onlyFullClasses())) {
            keyword = session.getCurrentMonHocTen();
        }

        List<CourseClassDtos.LopHocPhanDto> list;
        if (role == Role.GIANG_VIEN && keyword == null) {
            list = lopHocPhanService.danhSachCuaToi(hocKy.getId());
        } else {
            list = lopHocPhanService.danhSach(hocKy.getId(), keyword);
        }

        return list.stream()
                .filter(l -> !l.daHuy())
                .filter(l -> nlu.thu() == null || l.thu() == nlu.thu())
                .filter(l -> nlu.tietBatDau() == null || l.tietBatDau() == nlu.tietBatDau())
                .filter(l -> !nlu.onlyAvailableSeats() || l.conLai() > 0)
                .filter(l -> !nlu.onlyFullClasses() || l.conLai() <= 0)
                .filter(l -> nlu.tenGiangVien() == null || (l.tenGiangVien() != null && l.tenGiangVien().toLowerCase(Locale.ROOT).contains(nlu.tenGiangVien().toLowerCase(Locale.ROOT))))
                .toList();
    }

    private ChatbotDtos.ClassCardDto toClassCard(CourseClassDtos.LopHocPhanDto dto, String thoiDiem) {
        return new ChatbotDtos.ClassCardDto(
                dto.id(),
                dto.ma(),
                dto.monHocId(),
                dto.maMon(),
                dto.tenMon(),
                dto.soTinChi(),
                dto.hocKyId(),
                dto.tenHocKy(),
                dto.tenGiangVien(),
                dto.phong(),
                dto.thu(),
                dto.tietBatDau(),
                dto.soTiet(),
                dto.siSoToiDa(),
                dto.siSoHienTai(),
                dto.conLai(),
                thoiDiem,
                dto.daHuy()
        );
    }

    private ChatbotDtos.ChatResponse buildResponse(
            ChatbotSessionManager.UserSession session,
            String reply,
            ChatbotDtos.ChatActionType actionType,
            ChatbotDtos.ClassCardDto pendingClass,
            List<ChatbotDtos.ClassCardDto> candidateClasses,
            List<String> suggestedOptions,
            String tenHocKy,
            String thoiDiemLayDuLieu,
            boolean requiresConfirmation) {

        ChatbotDtos.ChatbotStatusDto status = activeAiProvider().getStatus();
        String aiStatus = status.configured() ? "CONFIGURED" : "UNCONFIGURED_SAFE_RULES";
        String missing = status.configured() ? null : String.join(", ", status.missingDependencies());

        return new ChatbotDtos.ChatResponse(
                UUID.randomUUID().toString(),
                session.getSessionId(),
                "BOT",
                reply,
                LocalDateTime.now(),
                actionType,
                pendingClass,
                candidateClasses,
                suggestedOptions,
                tenHocKy,
                thoiDiemLayDuLieu,
                aiStatus,
                missing,
                requiresConfirmation
        );
    }

    private ChatbotDtos.ChatResponse buildSecurityBlockedResponse(String sessionId, String reason) {
        return new ChatbotDtos.ChatResponse(
                UUID.randomUUID().toString(),
                sessionId,
                "BOT",
                "🛡️ **Cảnh báo bảo mật:** " + reason,
                LocalDateTime.now(),
                ChatbotDtos.ChatActionType.SECURITY_BLOCKED,
                null,
                null,
                null,
                null,
                LocalDateTime.now().format(TIME_FORMATTER),
                "SECURITY_GUARD",
                null,
                false
        );
    }

    private ChatbotDtos.ChatResponse buildErrorResponse(String sessionId, String error, ChatbotDtos.ChatActionType actionType) {
        return new ChatbotDtos.ChatResponse(
                UUID.randomUUID().toString(),
                sessionId,
                "BOT",
                "⚠️ " + error,
                LocalDateTime.now(),
                actionType,
                null,
                null,
                null,
                null,
                LocalDateTime.now().format(TIME_FORMATTER),
                "ERROR",
                null,
                false
        );
    }

    /**
     * Với các lượt hội thoại xã giao, ưu tiên phản hồi tự nhiên từ AI Provider nếu đã cấu hình.
     * Không có API key hoặc provider lỗi thì dùng lại câu trả lời quy chuẩn (fallback).
     */
    private String conversationalReply(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            String userMessage,
            String fallback) {
        if (!properties.isEnabled() || !properties.isProviderConfigured()) {
            return fallback;
        }
        List<ChatbotSessionManager.SessionMessage> history = session.getMessages();
        if (!history.isEmpty()) {
            ChatbotSessionManager.SessionMessage last = history.get(history.size() - 1);
            if ("USER".equalsIgnoreCase(last.sender())) {
                history.remove(history.size() - 1);
            }
        }
        return activeAiProvider().generateText(buildConversationalSystemPrompt(hocKy), userMessage, history)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .orElse(fallback);
    }

    public static int toVietnameseThu(LocalDate date) {
        return date.getDayOfWeek().getValue() + 1;
    }

    public static String formatVietnameseThu(int thu) {
        if (thu >= 2 && thu <= 7) {
            return "Thứ " + thu;
        } else if (thu == 8) {
            return "Chủ Nhật";
        }
        return "Thứ " + thu;
    }

    private ChatbotDtos.ChatResponse handleProfileInfo(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            String thoiDiem,
            Role role,
            User user) {

        StringBuilder sb = new StringBuilder();

        if (role == Role.SINH_VIEN) {
            Optional<Student> svOpt = studentRepository != null ? studentRepository.findWithAcademicProfileByUserId(user.getId()) : Optional.empty();
            if (svOpt.isEmpty()) {
                return buildResponse(session,
                        "Không tìm thấy hồ sơ sinh viên tương ứng với tài khoản `" + user.getUsername() + "` trong hệ thống.",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
            Student sv = svOpt.get();
            sb.append("👤 **Thông tin cá nhân của bạn** (Tài khoản đang đăng nhập):\n\n")
                    .append("- **Họ và tên:** ").append(sv.getHoTen()).append("\n")
                    .append("- **Mã sinh viên:** `").append(sv.getMa()).append("`\n")
                    .append("- **Tên đăng nhập:** `").append(user.getUsername()).append("`\n")
                    .append("- **Vai trò:** Sinh viên\n")
                    .append("- **Email:** ").append(sv.getEmail() != null && !sv.getEmail().isBlank() ? sv.getEmail() : "Chưa cập nhật").append("\n")
                    .append("- **Số điện thoại:** ").append(sv.getSoDienThoai() != null && !sv.getSoDienThoai().isBlank() ? sv.getSoDienThoai() : "Chưa cập nhật").append("\n")
                    .append("- **Khoa:** ").append(sv.getKhoa() != null ? sv.getKhoa().getTen() : "Chưa phân khoa").append("\n")
                    .append("- **Ngành đào tạo:** ").append(sv.getNganh() != null ? sv.getNganh().getTen() : "Chưa phân ngành").append("\n")
                    .append("- **Lớp hành chính:** ").append(sv.getLopHanhChinh() != null ? (sv.getLopHanhChinh().getTen() + " (" + sv.getLopHanhChinh().getMa() + ")") : "Chưa phân lớp").append("\n")
                    .append("- **Chương trình đào tạo:** ").append(sv.getChuongTrinh() != null ? (sv.getChuongTrinh().getPhienBan() + " (Khóa " + sv.getChuongTrinh().getKhoaNhapHoc() + ")") : "Chưa phân chương trình").append("\n")
                    .append("- **Trạng thái học tập:** ").append(sv.getTrangThaiHocTap() != null ? sv.getTrangThaiHocTap().getMoTa() : "Đang học").append("\n\n")
                    .append("💡 *Bạn có thể cập nhật email, số điện thoại hoặc đổi mật khẩu tại trang [Hồ sơ cá nhân](/ho-so).*");

        } else if (role == Role.GIANG_VIEN) {
            Optional<Lecturer> gvOpt = lecturerRepository != null ? lecturerRepository.findWithKhoaByUserId(user.getId()) : Optional.empty();
            if (gvOpt.isEmpty()) {
                return buildResponse(session,
                        "Không tìm thấy hồ sơ giảng viên tương ứng với tài khoản `" + user.getUsername() + "` trong hệ thống.",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
            Lecturer gv = gvOpt.get();
            sb.append("👤 **Thông tin cá nhân của Thầy/Cô** (Tài khoản đang đăng nhập):\n\n")
                    .append("- **Họ và tên:** ").append(gv.getHoTen()).append("\n")
                    .append("- **Mã giảng viên:** `").append(gv.getMa()).append("`\n")
                    .append("- **Tên đăng nhập:** `").append(user.getUsername()).append("`\n")
                    .append("- **Vai trò:** Giảng viên\n")
                    .append("- **Email:** ").append(gv.getEmail() != null && !gv.getEmail().isBlank() ? gv.getEmail() : "Chưa cập nhật").append("\n")
                    .append("- **Số điện thoại:** ").append(gv.getSoDienThoai() != null && !gv.getSoDienThoai().isBlank() ? gv.getSoDienThoai() : "Chưa cập nhật").append("\n")
                    .append("- **Khoa / Đơn vị công tác:** ").append(gv.getKhoa() != null ? gv.getKhoa().getTen() : "Chưa phân khoa").append("\n\n")
                    .append("💡 *Thầy/Cô có thể cập nhật thông tin liên hệ hoặc đổi mật khẩu tại trang [Hồ sơ cá nhân](/ho-so).*");

        } else {
            sb.append("👤 **Thông tin tài khoản Quản trị viên** (Tài khoản đang đăng nhập):\n\n")
                    .append("- **Họ và tên:** Quản trị viên hệ thống\n")
                    .append("- **Mã tài khoản:** `ADMIN`\n")
                    .append("- **Tên đăng nhập:** `").append(user.getUsername()).append("`\n")
                    .append("- **Vai trò:** Quản trị viên (QUAN_TRI)\n")
                    .append("- **Quyền hạn:** Toàn quyền quản trị danh mục đào tạo, người dùng, lớp học phần và giám sát hệ thống.\n\n")
                    .append("💡 *Quản trị viên có thể đổi mật khẩu tại trang [Hồ sơ cá nhân](/ho-so).*");
        }

        return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                List.of("Thời khóa biểu hôm nay", "Xem danh sách lớp", "Hướng dẫn đổi mật khẩu"), hocKy.getTen(), thoiDiem, false);
    }

    private ChatbotDtos.ChatResponse handlePasswordChangeGuide(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            String thoiDiem) {

        String reply = """
                🔐 **Hướng dẫn đổi mật khẩu tài khoản:**

                Để đảm bảo an toàn bảo mật tuyệt đối cho tài khoản của bạn, thao tác đổi mật khẩu cần được thực hiện trực tiếp tại trang bảo mật:

                1. **Truy cập:** Mở trang **[Hồ sơ cá nhân](/ho-so)** (bấm vào biểu tượng tài khoản ở góc trên hoặc truy cập menu Hồ sơ).
                2. **Tìm mục:** Cuộn xuống phần **Đổi mật khẩu**.
                3. **Nhập thông tin xác thực:**
                   - **Mật khẩu hiện tại:** Nhập mật khẩu bạn đang sử dụng.
                   - **Mật khẩu mới:** Nhập mật khẩu mới bạn muốn đặt.
                   - **Xác nhận mật khẩu mới:** Nhập lại chính xác mật khẩu mới.
                4. **Lưu thay đổi:** Bấm nút **"Đổi mật khẩu"** để hoàn tất.

                🛡️ *Lưu ý an toàn: Chatbot tuyệt đối không yêu cầu bạn nhập mật khẩu hiện tại hay mật khẩu mới trong khung trò chuyện.*
                """;

        return buildResponse(session, reply, ChatbotDtos.ChatActionType.NONE, null, null,
                List.of("Xem thông tin cá nhân", "Thời khóa biểu hôm nay", "Hướng dẫn trợ giúp"), hocKy.getTen(), thoiDiem, false);
    }

    private ChatbotDtos.ChatResponse handlePersonalTimetable(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            VietnameseNluEngine.NluParseResult nlu,
            String thoiDiem,
            Role role) {

        LocalDate now = LocalDate.now();
        Integer targetThu = nlu.thu();
        String dayLabel = null;

        if (nlu.isToday()) {
            targetThu = toVietnameseThu(now);
            dayLabel = "hôm nay (" + formatVietnameseThu(targetThu) + ", ngày " + now.format(DATE_FORMATTER) + ")";
        } else if (nlu.isTomorrow()) {
            LocalDate tomorrow = now.plusDays(1);
            targetThu = toVietnameseThu(tomorrow);
            dayLabel = "ngày mai (" + formatVietnameseThu(targetThu) + ", ngày " + tomorrow.format(DATE_FORMATTER) + ")";
        } else if (targetThu != null) {
            dayLabel = formatVietnameseThu(targetThu);
        }

        if (role == Role.SINH_VIEN) {
            List<RegistrationDtos.ThoiKhoaBieuDto> tkb = dangKyService.thoiKhoaBieu(hocKy.getId());
            if (tkb.isEmpty()) {
                String emptyMsg = targetThu != null
                        ? "📅 **Thời khóa biểu " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                        + (nlu.isToday() ? "Hôm nay bạn chưa có lịch học nào" : "Bạn chưa có lịch học nào vào " + dayLabel)
                        + " (do bạn chưa đăng ký lớp học phần nào trong " + hocKy.getTen() + ", kiểm tra lúc " + thoiDiem + ")."
                        : "Bạn chưa có lịch học nào được xếp trong " + hocKy.getTen() + " (Thời điểm kiểm tra: " + thoiDiem + ").";
                return buildResponse(session, emptyMsg,
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            if (targetThu != null) {
                final int filterThu = targetThu;
                List<RegistrationDtos.ThoiKhoaBieuDto> daily = tkb.stream()
                        .filter(i -> i.thu() == filterThu)
                        .sorted(Comparator.comparingInt(RegistrationDtos.ThoiKhoaBieuDto::tietBatDau))
                        .toList();

                if (daily.isEmpty()) {
                    String emptyMsg = nlu.isToday()
                            ? "📅 **Thời khóa biểu " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                            + "Hôm nay bạn không có lịch học nào! 🎉 Chúc bạn có một ngày nghỉ ngơi hoặc tự học hiệu quả.\n\n"
                            + "👉 Bạn có thể hỏi: *\"Xem thời khóa biểu của tôi\"* để xem toàn bộ các ngày khác trong tuần."
                            : "📅 **Thời khóa biểu " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                            + "Bạn không có lịch học nào vào " + dayLabel + ".";
                    return buildResponse(session, emptyMsg, ChatbotDtos.ChatActionType.NONE, null, null,
                            List.of("Xem thời khóa biểu của tôi", "Xem thông tin cá nhân"), hocKy.getTen(), thoiDiem, false);
                }

                StringBuilder sb = new StringBuilder("📅 **Thời khóa biểu ").append(dayLabel).append("** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
                sb.append("Bạn có **").append(daily.size()).append("** môn học trong ngày:\n\n");
                for (RegistrationDtos.ThoiKhoaBieuDto item : daily) {
                    sb.append(String.format(Locale.ROOT, "- **%s** (%s): Tiết %d-%d (%d tiết) | Phòng: %s | GV: %s\n",
                            item.maLop(), item.tenMon(), item.tietBatDau(), item.tietBatDau() + item.soTiet() - 1, item.soTiet(), item.phong(), item.giangVien()));
                }
                return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                        List.of("Xem thời khóa biểu của tôi", "Xem thông tin cá nhân"), hocKy.getTen(), thoiDiem, false);
            }

            // Xem toàn bộ TKB học kỳ
            List<RegistrationDtos.ThoiKhoaBieuDto> sorted = tkb.stream()
                    .sorted(Comparator.comparingInt(RegistrationDtos.ThoiKhoaBieuDto::thu)
                            .thenComparingInt(RegistrationDtos.ThoiKhoaBieuDto::tietBatDau))
                    .toList();

            StringBuilder sb = new StringBuilder("📅 **Thời khóa biểu của bạn** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
            int currentThu = -1;
            for (RegistrationDtos.ThoiKhoaBieuDto item : sorted) {
                if (item.thu() != currentThu) {
                    currentThu = item.thu();
                    sb.append("\n**").append(formatVietnameseThu(currentThu)).append(":**\n");
                }
                sb.append(String.format(Locale.ROOT, "- **%s** (%s): Tiết %d-%d (%d tiết) | Phòng: %s | GV: %s\n",
                        item.maLop(), item.tenMon(), item.tietBatDau(), item.tietBatDau() + item.soTiet() - 1, item.soTiet(), item.phong(), item.giangVien()));
            }
            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                    List.of("Thời khóa biểu hôm nay", "Xem thông tin cá nhân"), hocKy.getTen(), thoiDiem, false);

        } else if (role == Role.GIANG_VIEN) {
            List<RegistrationDtos.ThoiKhoaBieuDto> tkb = lopHocPhanService.thoiKhoaBieuCuaToi(hocKy.getId());
            if (tkb.isEmpty()) {
                String emptyMsg = targetThu != null
                        ? "📅 **Lịch giảng dạy " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                        + (nlu.isToday() ? "Hôm nay Thầy/Cô chưa có lịch giảng dạy nào" : "Thầy/Cô chưa có lịch giảng dạy nào vào " + dayLabel)
                        + " (chưa có lớp học phần được phân công trong " + hocKy.getTen() + ", kiểm tra lúc " + thoiDiem + ")."
                        : "Thầy/Cô chưa có lịch giảng dạy nào trong " + hocKy.getTen() + " (Thời điểm kiểm tra: " + thoiDiem + ").";
                return buildResponse(session, emptyMsg,
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            if (targetThu != null) {
                final int filterThu = targetThu;
                List<RegistrationDtos.ThoiKhoaBieuDto> daily = tkb.stream()
                        .filter(i -> i.thu() == filterThu)
                        .sorted(Comparator.comparingInt(RegistrationDtos.ThoiKhoaBieuDto::tietBatDau))
                        .toList();

                if (daily.isEmpty()) {
                    String emptyMsg = nlu.isToday()
                            ? "📅 **Lịch giảng dạy " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                            + "Hôm nay Thầy/Cô không có tiết giảng dạy nào! Chúc Thầy/Cô một ngày làm việc hiệu quả.\n\n"
                            + "👉 Thầy/Cô có thể hỏi: *\"Lịch giảng dạy của tôi\"* hoặc *\"Danh sách lớp tôi phụ trách\"* để xem các ngày khác."
                            : "📅 **Lịch giảng dạy " + dayLabel + "** trong **" + hocKy.getTen() + "**:\n\n"
                            + "Thầy/Cô không có tiết giảng dạy nào vào " + dayLabel + ".";
                    return buildResponse(session, emptyMsg, ChatbotDtos.ChatActionType.NONE, null, null,
                            List.of("Lịch giảng dạy của tôi", "Danh sách lớp tôi phụ trách"), hocKy.getTen(), thoiDiem, false);
                }

                StringBuilder sb = new StringBuilder("📅 **Lịch giảng dạy ").append(dayLabel).append("** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
                sb.append("Thầy/Cô có **").append(daily.size()).append("** lớp giảng dạy trong ngày:\n\n");
                for (RegistrationDtos.ThoiKhoaBieuDto item : daily) {
                    sb.append(String.format(Locale.ROOT, "- **%s** (%s): Tiết %d-%d (%d tiết) | Phòng: %s\n",
                            item.maLop(), item.tenMon(), item.tietBatDau(), item.tietBatDau() + item.soTiet() - 1, item.soTiet(), item.phong()));
                }
                return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                        List.of("Lịch giảng dạy của tôi", "Danh sách lớp tôi phụ trách"), hocKy.getTen(), thoiDiem, false);
            }

            // Toàn bộ lịch giảng dạy
            List<RegistrationDtos.ThoiKhoaBieuDto> sorted = tkb.stream()
                    .sorted(Comparator.comparingInt(RegistrationDtos.ThoiKhoaBieuDto::thu)
                            .thenComparingInt(RegistrationDtos.ThoiKhoaBieuDto::tietBatDau))
                    .toList();

            StringBuilder sb = new StringBuilder("📅 **Lịch giảng dạy của Thầy/Cô** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
            int currentThu = -1;
            for (RegistrationDtos.ThoiKhoaBieuDto item : sorted) {
                if (item.thu() != currentThu) {
                    currentThu = item.thu();
                    sb.append("\n**").append(formatVietnameseThu(currentThu)).append(":**\n");
                }
                sb.append(String.format(Locale.ROOT, "- **%s** (%s): Tiết %d-%d (%d tiết) | Phòng: %s\n",
                        item.maLop(), item.tenMon(), item.tietBatDau(), item.tietBatDau() + item.soTiet() - 1, item.soTiet(), item.phong()));
            }
            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                    List.of("Lịch giảng dạy hôm nay", "Danh sách lớp tôi phụ trách"), hocKy.getTen(), thoiDiem, false);

        } else {
            return buildResponse(session, "Tài khoản Quản trị viên không có thời khóa biểu học tập hoặc giảng dạy cá nhân.",
                    ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }
    }

    private ChatbotDtos.ChatResponse handleMyClasses(
            ChatbotSessionManager.UserSession session,
            Semester hocKy,
            String thoiDiem,
            Role role) {

        if (role == Role.GIANG_VIEN) {
            List<CourseClassDtos.LopHocPhanDto> myClasses = lopHocPhanService.danhSachCuaToi(hocKy.getId());
            if (myClasses.isEmpty()) {
                return buildResponse(session,
                        "Thầy/Cô hiện chưa được phân công phụ trách lớp học phần nào trong " + hocKy.getTen() + " (lấy lúc " + thoiDiem + ").",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            List<ChatbotDtos.ClassCardDto> cards = myClasses.stream().map(l -> toClassCard(l, thoiDiem)).toList();
            session.setCandidateClasses(cards);

            StringBuilder sb = new StringBuilder("📚 **Danh sách các lớp Thầy/Cô phụ trách** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
            sb.append("Tìm thấy **").append(myClasses.size()).append("** lớp học phần:\n\n");
            for (int i = 0; i < cards.size(); i++) {
                ChatbotDtos.ClassCardDto c = cards.get(i);
                sb.append(String.format(Locale.ROOT, "%d. **%s** (%s - %d tín chỉ)\n" +
                                "   - Lịch dạy: Thứ %d, tiết %d-%d | Phòng: %s\n" +
                                "   - Sĩ số: **%d/%d** sinh viên (còn %d chỗ)\n\n",
                        i + 1, c.ma(), c.tenMon(), c.soTinChi(),
                        c.thu(), c.tietBatDau(), c.tietBatDau() + c.soTiet() - 1, c.phong(),
                        c.siSoHienTai(), c.siSoToiDa(), c.conLai()));
            }
            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, cards,
                    List.of("Lịch giảng dạy hôm nay", "Xem thông tin cá nhân"), hocKy.getTen(), thoiDiem, false);

        } else if (role == Role.SINH_VIEN) {
            List<RegistrationDtos.DangKyDto> myClasses = dangKyService.danhSachCuaToi(hocKy.getId());
            if (myClasses.isEmpty()) {
                return buildResponse(session,
                        "Bạn chưa đăng ký lớp học phần nào trong " + hocKy.getTen() + " (lấy lúc " + thoiDiem + ").",
                        ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }

            StringBuilder sb = new StringBuilder("📋 **Danh sách các lớp học phần bạn đã đăng ký** trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
            sb.append("Bạn đã đăng ký thành công **").append(myClasses.size()).append("** lớp học phần:\n\n");
            for (int i = 0; i < myClasses.size(); i++) {
                RegistrationDtos.DangKyDto dk = myClasses.get(i);
                sb.append(String.format(Locale.ROOT, "%d. **%s** (%s - %d tín chỉ)\n" +
                                "   - Giảng viên: %s | Phòng: %s\n" +
                                "   - Lịch học: Thứ %d, tiết %d-%d\n\n",
                        i + 1, dk.maLop(), dk.tenMon(), dk.soTinChi(),
                        dk.tenGiangVien(), dk.phong(),
                        dk.thu(), dk.tietBatDau(), dk.tietBatDau() + dk.soTiet() - 1));
            }
            return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null,
                    List.of("Thời khóa biểu hôm nay", "Xem thông tin cá nhân"), hocKy.getTen(), thoiDiem, false);
        } else {
            return buildResponse(session, "Quản trị viên có thể xem danh sách lớp toàn trường bằng cách hỏi: 'Tra cứu danh sách lớp học phần'.",
                    ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
        }
    }

    private String buildConversationalSystemPrompt(Semester hocKy) {
        StringBuilder sb = new StringBuilder(CONVERSATIONAL_SYSTEM_PROMPT);
        LocalDate today = LocalDate.now();
        int todayThu = toVietnameseThu(today);
        sb.append("\nThời gian hiện tại của hệ thống:\n")
                .append("- Hôm nay là: ").append(formatVietnameseThu(todayThu))
                .append(", ngày ").append(today.format(DATE_FORMATTER)).append("\n");

        try {
            User user = currentUser.get();
            sb.append("\nThông tin người dùng đang trò chuyện:\n")
                    .append("- Tên đăng nhập: ").append(user.getUsername()).append("\n")
                    .append("- Vai trò: ").append(user.getRole().name()).append("\n");
            if (user.getRole() == Role.SINH_VIEN && studentRepository != null) {
                studentRepository.findWithAcademicProfileByUserId(user.getId()).ifPresent(sv -> {
                    sb.append("- Họ và tên: ").append(sv.getHoTen()).append("\n")
                            .append("- Mã sinh viên: ").append(sv.getMa()).append("\n")
                            .append("- Lớp hành chính: ").append(sv.getLopHanhChinh() != null ? sv.getLopHanhChinh().getTen() : "Chưa phân").append("\n")
                            .append("- Khoa: ").append(sv.getKhoa() != null ? sv.getKhoa().getTen() : "Chưa phân").append("\n")
                            .append("- Ngành: ").append(sv.getNganh() != null ? sv.getNganh().getTen() : "Chưa phân").append("\n");
                });
            } else if (user.getRole() == Role.GIANG_VIEN && lecturerRepository != null) {
                lecturerRepository.findWithKhoaByUserId(user.getId()).ifPresent(gv -> {
                    sb.append("- Họ và tên: ").append(gv.getHoTen()).append("\n")
                            .append("- Mã giảng viên: ").append(gv.getMa()).append("\n")
                            .append("- Khoa/Bộ môn: ").append(gv.getKhoa() != null ? gv.getKhoa().getTen() : "Chưa phân").append("\n");
                });
            }
        } catch (Exception ignored) {}

        if (hocKy != null) {
            sb.append("\nThông tin học kỳ đang áp dụng (chỉ dùng đúng các dữ kiện này, không bịa thêm):\n")
                    .append("- Học kỳ: ").append(hocKy.getTen()).append(" (năm học ").append(hocKy.getNamHoc()).append(")\n")
                    .append("- Thời gian đăng ký: ").append(DATE_FORMATTER.format(hocKy.getDangKyTu()))
                    .append(" đến ").append(DATE_FORMATTER.format(hocKy.getDangKyDen()))
                    .append(hocKy.dangMoDangKy() ? " (đang mở đăng ký)" : " (hiện không trong thời gian đăng ký)").append("\n")
                    .append("- Thời gian học: ").append(DATE_FORMATTER.format(hocKy.getNgayBatDau()))
                    .append(" đến ").append(DATE_FORMATTER.format(hocKy.getNgayKetThuc())).append("\n")
                    .append("- Giới hạn tín chỉ: tối thiểu ").append(hocKy.getSoTinChiToiThieu())
                    .append(", tối đa ").append(hocKy.getSoTinChiToiDa()).append("\n");
        }
        return sb.toString();
    }

    private boolean shouldAnswerConversationally(VietnameseNluEngine.NluParseResult nlu) {
        if (nlu.maLop() != null || nlu.maMon() != null
                || (nlu.tenMonOrKeyword() != null && !nlu.tenMonOrKeyword().isBlank())
                || nlu.selectionIndex() != null
                || nlu.onlyAvailableSeats() || nlu.onlyFullClasses()) {
            return false;
        }
        VietnameseNluEngine.Intent intent = nlu.intent();
        if (intent == VietnameseNluEngine.Intent.XAC_NHAN
                || intent == VietnameseNluEngine.Intent.HUY_XAC_NHAN
                || intent == VietnameseNluEngine.Intent.TKB_CA_NHAN
                || intent == VietnameseNluEngine.Intent.LOP_CUA_TOI
                || intent == VietnameseNluEngine.Intent.THONG_TIN_CA_NHAN
                || intent == VietnameseNluEngine.Intent.DOI_MAT_KHAU
                || intent == VietnameseNluEngine.Intent.THONG_KE
                || intent == VietnameseNluEngine.Intent.TRO_GIUP) {
            return false;
        }
        String normalized = nlu.normalizedMessage();
        return INFORMATION_QUESTION_PATTERN.matcher(normalized).find() || normalized.contains("bao nhieu");
    }

    private String buildGreeting(ChatbotSessionManager.UserSession session, Role role) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chào ").append(session.getUsername())
                .append("! Mình là **Trợ lý AI Đăng ký Học phần**, rất vui được trò chuyện với bạn 😊\n\n");
        sb.append("Bạn muốn mình giúp gì hôm nay? Mình có thể:\n");
        if (role == Role.SINH_VIEN) {
            sb.append("- 👤 Xem thông tin cá nhân tài khoản của bạn\n");
            sb.append("- 📅 Xem thời khóa biểu hôm nay hoặc toàn kỳ\n");
            sb.append("- 🔎 Tìm lớp học phần và kiểm tra chỗ trống\n");
            sb.append("- 📝 Hướng dẫn đăng ký học phần và đổi mật khẩu\n");
            sb.append("- 🔁 Gợi ý lớp thay thế khi lớp đã đầy\n\n");
        } else if (role == Role.GIANG_VIEN) {
            sb.append("- 👤 Xem thông tin cá nhân giảng viên\n");
            sb.append("- 📅 Xem lịch giảng dạy hôm nay hoặc toàn kỳ\n");
            sb.append("- 📚 Xem danh sách và sĩ số các lớp phụ trách\n");
            sb.append("- 🔐 Hướng dẫn đổi mật khẩu bảo mật\n\n");
        } else {
            sb.append("- 👤 Xem thông tin tài khoản quản trị\n");
            sb.append("- 📊 Xem thống kê tổng quan đào tạo\n");
            sb.append("- 🔎 Tra cứu danh sách lớp học phần\n\n");
        }
        sb.append("Bạn cứ nhắn tự nhiên nhé, ví dụ: \"Thời khóa biểu hôm nay\" hoặc gõ \"Hướng dẫn\" để xem đầy đủ.");
        return sb.toString();
    }

    private List<String> greetingSuggestions(Role role) {
        if (role == Role.SINH_VIEN) {
            return List.of("Thời khóa biểu hôm nay", "Xem thông tin cá nhân", "Tìm lớp còn chỗ", "Hướng dẫn đổi mật khẩu");
        }
        if (role == Role.GIANG_VIEN) {
            return List.of("Lịch giảng dạy hôm nay", "Xem danh sách lớp tôi phụ trách", "Xem thông tin cá nhân", "Hướng dẫn đổi mật khẩu");
        }
        return List.of("Xem thống kê tổng quan đào tạo", "Xem thông tin tài khoản", "Tra cứu danh sách lớp học phần", "Hướng dẫn trợ giúp");
    }

    private String getHelpText(Role role) {
        if (role == Role.SINH_VIEN) {
            return """
                    🤖 **Trợ lý Đăng ký Học phần (QLSV Assistant)**
                    Tôi có thể hỗ trợ bạn các tác vụ sau theo dữ liệu CSDL thời gian thực:

                    1. **Thông tin cá nhân & Tài khoản:**
                       - *"Xem thông tin cá nhân của tôi"*
                       - *"Tôi muốn đổi mật khẩu bạn hỗ trợ tôi được không?"*
                    2. **Thời khóa biểu & Lịch học:**
                       - *"Thời khóa biểu hôm nay của tôi thế nào?"*
                       - *"Lịch học ngày mai của tôi"*
                       - *"Xem thời khóa biểu của tôi"*
                       - *"Xem các lớp tôi đã đăng ký"*
                    3. **Tra cứu lớp học phần & chỗ trống:**
                       - *"Tìm các lớp môn Lập trình Java còn chỗ"*
                       - *"Lớp LHP01 còn bao nhiêu chỗ?"*
                       - *"Có lớp nào học thứ 4 không?"*
                    4. **Tra cứu giảng viên & phòng học:**
                       - *"Thầy Nguyễn Văn A dạy lớp nào?"*
                       - *"Lớp LHP02 học ở phòng nào?"*
                    5. **Đăng ký học phần có xác nhận an toàn:**
                       - *"Tôi muốn đăng ký lớp LHP01"*
                       - Hệ thống sẽ hiển thị chi tiết lớp và chỉ ghi nhận sau khi bạn xác nhận *"Đồng ý"*.
                    6. **Tìm lớp thay thế khi hết chỗ:**
                       - *"Lớp LHP01 đầy rồi, có lớp nào thay thế không?"*
                    """;
        } else if (role == Role.GIANG_VIEN) {
            return """
                    🤖 **Trợ lý Giảng viên (QLSV Assistant)**
                    Tôi có thể hỗ trợ Thầy/Cô các tác vụ sau:

                    1. **Thông tin cá nhân & Tài khoản:**
                       - *"Xem thông tin cá nhân của tôi"*
                       - *"Hướng dẫn đổi mật khẩu"*
                    2. **Lịch giảng dạy:**
                       - *"Lịch giảng dạy hôm nay của tôi"*
                       - *"Lịch dạy ngày mai của tôi"*
                       - *"Lịch giảng dạy của tôi"*
                    3. **Lớp phụ trách & Sĩ số:**
                       - *"Xem danh sách lớp tôi phụ trách"*
                       - *"Lớp của tôi hiện có bao nhiêu sinh viên?"*
                       - *"Lớp LHP01 hiện có bao nhiêu sinh viên?"*
                    """;
        } else {
            return """
                    🤖 **Trợ lý Quản trị Đào tạo (QLSV Assistant)**
                    - *"Xem thông tin tài khoản"*
                    - *"Xem thống kê tổng quan đào tạo"*
                    - *"Tra cứu danh sách lớp học phần theo môn"*
                    - *"Hướng dẫn đổi mật khẩu"*
                    """;
        }
    }
}
