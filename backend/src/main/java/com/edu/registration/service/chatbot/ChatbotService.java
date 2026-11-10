package com.edu.registration.service.chatbot;

import com.edu.registration.config.ChatbotAiProperties;
import com.edu.registration.dto.ChatbotDtos;
import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.dto.StatDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.Semester;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.RegistrationService;
import com.edu.registration.service.SemesterService;
import com.edu.registration.service.StatisticsService;
import com.edu.registration.service.chatbot.ai.AiChatProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

    private final ChatbotAiProperties properties;
    private final ChatbotSessionManager sessionManager;
    private final PromptInjectionGuard promptInjectionGuard;
    private final VietnameseNluEngine nluEngine;
    private final ChatbotRateLimiter rateLimiter;
    private final AiChatProvider aiChatProvider;
    private final CourseClassService lopHocPhanService;
    private final RegistrationService dangKyService;
    private final SemesterService hocKyService;
    private final StatisticsService thongKeService;
    private final CurrentUser currentUser;

    public ChatbotService(
            ChatbotAiProperties properties,
            ChatbotSessionManager sessionManager,
            PromptInjectionGuard promptInjectionGuard,
            VietnameseNluEngine nluEngine,
            ChatbotRateLimiter rateLimiter,
            AiChatProvider aiChatProvider,
            CourseClassService lopHocPhanService,
            RegistrationService dangKyService,
            SemesterService hocKyService,
            StatisticsService thongKeService,
            CurrentUser currentUser) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.promptInjectionGuard = promptInjectionGuard;
        this.nluEngine = nluEngine;
        this.rateLimiter = rateLimiter;
        this.aiChatProvider = aiChatProvider;
        this.lopHocPhanService = lopHocPhanService;
        this.dangKyService = dangKyService;
        this.hocKyService = hocKyService;
        this.thongKeService = thongKeService;
        this.currentUser = currentUser;
    }

    public ChatbotDtos.ChatbotStatusDto getStatus() {
        return aiChatProvider.getStatus();
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

        // --- Intent: THỜI KHÓA BIỂU CÁ NHÂN ---
        if (nlu.intent() == VietnameseNluEngine.Intent.TKB_CA_NHAN) {
            if (role == Role.SINH_VIEN) {
                List<RegistrationDtos.ThoiKhoaBieuDto> tkb = dangKyService.thoiKhoaBieu(hocKy.getId());
                if (tkb.isEmpty()) {
                    return buildResponse(session,
                            "Bạn chưa có lịch học nào được xếp trong " + hocKy.getTen() + " (Thời điểm kiểm tra: " + thoiDiem + ").",
                            ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
                }
                StringBuilder sb = new StringBuilder("Thời khóa biểu của bạn trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
                for (RegistrationDtos.ThoiKhoaBieuDto item : tkb) {
                    sb.append(String.format(Locale.ROOT, "- **%s** (%s): Thứ %d, tiết %d (số tiết: %d) | Phòng: %s | GV: %s\n",
                            item.maLop(), item.tenMon(), item.thu(), item.tietBatDau(), item.soTiet(), item.phong(), item.giangVien()));
                }
                return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            } else if (role == Role.GIANG_VIEN) {
                List<RegistrationDtos.ThoiKhoaBieuDto> tkb = lopHocPhanService.thoiKhoaBieuCuaToi(hocKy.getId());
                if (tkb.isEmpty()) {
                    return buildResponse(session, "Thầy/Cô chưa có lịch giảng dạy nào trong " + hocKy.getTen() + " (Thời điểm kiểm tra: " + thoiDiem + ").",
                            ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
                }
                StringBuilder sb = new StringBuilder("Lịch giảng dạy của Thầy/Cô trong **").append(hocKy.getTen()).append("** (lấy lúc ").append(thoiDiem).append("):\n\n");
                for (RegistrationDtos.ThoiKhoaBieuDto item : tkb) {
                    sb.append(String.format(Locale.ROOT, "- **%s** (%s): Thứ %d, tiết %d (số tiết: %d) | Phòng: %s\n",
                            item.maLop(), item.tenMon(), item.thu(), item.tietBatDau(), item.soTiet(), item.phong()));
                }
                return buildResponse(session, sb.toString(), ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            } else {
                return buildResponse(session, "Tài khoản Quản trị viên không có thời khóa biểu cá nhân.", ChatbotDtos.ChatActionType.NONE, null, null, null, hocKy.getTen(), thoiDiem, false);
            }
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

        ChatbotDtos.ChatbotStatusDto status = aiChatProvider.getStatus();
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

    private String getHelpText(Role role) {
        if (role == Role.SINH_VIEN) {
            return """
                    🤖 **Trợ lý Đăng ký Học phần (QLSV Assistant)**
                    Tôi có thể hỗ trợ bạn các tác vụ sau theo dữ liệu CSDL thời gian thực:
                    
                    1. **Tra cứu lớp học phần & chỗ trống:**
                       - *"Tìm các lớp môn Lập trình Java còn chỗ"*
                       - *"Lớp LHP01 còn bao nhiêu chỗ?"*
                       - *"Có lớp nào học thứ 4 không?"*
                    2. **Tra cứu giảng viên & phòng học:**
                       - *"Thầy Nguyễn Văn A dạy lớp nào?"*
                       - *"Lớp LHP02 học ở phòng nào?"*
                    3. **Đăng ký học phần có xác nhận:**
                       - *"Tôi muốn đăng ký lớp LHP01"*
                       - Hệ thống sẽ hiển thị chi tiết lớp và chỉ ghi nhận sau khi bạn xác nhận *"Đồng ý"*.
                    4. **Tìm lớp thay thế khi hết chỗ:**
                       - *"Lớp LHP01 đầy rồi, có lớp nào thay thế không?"*
                    5. **Xem thời khóa biểu cá nhân:**
                       - *"Xem thời khóa biểu của tôi"*
                    """;
        } else if (role == Role.GIANG_VIEN) {
            return """
                    🤖 **Trợ lý Giảng viên (QLSV Assistant)**
                    - *"Xem danh sách lớp tôi phụ trách"*
                    - *"Lịch giảng dạy của tôi"*
                    - *"Lớp LHP01 hiện có bao nhiêu sinh viên?"*
                    """;
        } else {
            return """
                    🤖 **Trợ lý Quản trị Đào tạo (QLSV Assistant)**
                    - *"Xem thống kê tổng quan đào tạo"*
                    - *"Tra cứu danh sách lớp học phần theo môn"*
                    """;
        }
    }
}
