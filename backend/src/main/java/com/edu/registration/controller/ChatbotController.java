package com.edu.registration.controller;

import com.edu.registration.dto.ChatbotDtos;
import com.edu.registration.service.chatbot.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp REST API cho Chatbot AI tư vấn và hỗ trợ đăng ký học phần (QLSV-106 / TASK-AI-01).
 */
@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot AI - Tư vấn & Hỗ trợ Đăng ký", description = "Các API giao tiếp với Chatbot AI tiếng Việt: tra cứu lớp, sĩ số, lịch học và đăng ký học phần an toàn")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gửi tin nhắn tới Chatbot AI", description = "Tiếp nhận câu hỏi tiếng Việt, phân tích ngữ cảnh, kiểm tra prompt injection và trả lời từ dữ liệu thực tế kèm mốc thời gian")
    public ChatbotDtos.ChatResponse guiTinNhan(@Valid @RequestBody ChatbotDtos.ChatRequest request) {
        return chatbotService.handleUserMessage(request);
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Xác nhận đăng ký lớp học phần", description = "Sinh viên bấm nút xác nhận lớp đã được Chatbot nhận diện; hệ thống chuyển tiếp gọi RegistrationService chính thức")
    public ChatbotDtos.ChatResponse xacNhanDangKy(@Valid @RequestBody ChatbotDtos.ConfirmRegistrationRequest request) {
        return chatbotService.confirmRegistration(request);
    }

    @GetMapping("/session")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem ngữ cảnh phiên hội thoại hiện tại", description = "Lấy thông tin tóm tắt về học kỳ, môn học, lớp đang xem và số lượng tin nhắn trong phiên")
    public ChatbotDtos.ChatSessionSummaryDto thongTinPhien(@RequestParam(required = false) String sessionId) {
        return chatbotService.getSessionSummary(sessionId);
    }

    @PostMapping("/reset")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Làm mới / Xóa phiên hội thoại", description = "Xóa lịch sử hội thoại và giải phóng ngữ cảnh đã lưu trong bộ nhớ theo chính sách quyền riêng tư")
    public ResponseEntity<Void> lamMoiPhien(@RequestParam(required = false) String sessionId) {
        chatbotService.resetSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Trạng thái cấu hình External AI Provider", description = "Kiểm tra xem mô hình LLM bên ngoài đã được cấu hình khóa API hay chưa và dependency còn thiếu")
    public ChatbotDtos.ChatbotStatusDto trangThaiCauHinh() {
        return chatbotService.getStatus();
    }
}
