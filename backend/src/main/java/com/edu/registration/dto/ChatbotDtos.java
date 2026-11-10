package com.edu.registration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Các đối tượng DTO phục vụ Chatbot AI tư vấn và hỗ trợ đăng ký học phần (QLSV-106 / TASK-AI-01).
 */
public final class ChatbotDtos {

    private ChatbotDtos() {
    }

    public enum ChatActionType {
        NONE,
        SELECTION_REQUIRED,
        CONFIRM_REGISTRATION,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILED,
        SECURITY_BLOCKED,
        ERROR
    }

    @Schema(description = "Yêu cầu gửi tin nhắn tới Chatbot")
    public record ChatRequest(
            @NotBlank(message = "Nội dung câu hỏi không được để trống")
            @Size(max = 1000, message = "Độ dài câu hỏi tối đa 1000 ký tự")
            String message,

            Long hocKyId,

            String sessionId
    ) {
    }

    @Schema(description = "Yêu cầu xác nhận hoặc hủy thao tác đăng ký")
    public record ConfirmRegistrationRequest(
            @NotNull(message = "Vui lòng chỉ định ID lớp học phần cần xác nhận")
            Long lopHocPhanId,

            String sessionId
    ) {
    }

    @Schema(description = "Thẻ thông tin lớp học phần trả về từ dữ liệu thực tế kèm mốc thời gian")
    public record ClassCardDto(
            Long id,
            String ma,
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long hocKyId,
            String tenHocKy,
            String giangVien,
            String phong,
            int thu,
            int tietBatDau,
            int soTiet,
            int siSoToiDa,
            int siSoHienTai,
            int conLai,
            String thoiDiemLayDuLieu,
            boolean daHuy
    ) {
    }

    @Schema(description = "Phản hồi tin nhắn từ Chatbot")
    public record ChatResponse(
            String id,
            String sessionId,
            String sender,
            String reply,
            LocalDateTime timestamp,
            ChatActionType actionType,
            ClassCardDto pendingClass,
            List<ClassCardDto> candidateClasses,
            List<String> suggestedOptions,
            String tenHocKy,
            String thoiDiemLayDuLieu,
            String aiProviderStatus,
            String missingDependency,
            boolean requiresConfirmation
    ) {
    }

    @Schema(description = "Trạng thái cấu hình của hệ thống Chatbot và External Provider")
    public record ChatbotStatusDto(
            boolean enabled,
            String provider,
            String model,
            boolean configured,
            List<String> missingDependencies,
            String statusMessage,
            String policyNote,
            int rateLimitPerMinute,
            int sessionTtlMinutes
    ) {
    }

    @Schema(description = "Thông tin phiên trò chuyện và ngữ cảnh hiện thời")
    public record ChatSessionSummaryDto(
            String sessionId,
            String username,
            String role,
            Long currentHocKyId,
            String currentHocKyTen,
            String currentMonHocTen,
            String currentLopHocPhanMa,
            ClassCardDto pendingRegistrationClass,
            int messageCount,
            LocalDateTime lastActive
    ) {
    }
}
