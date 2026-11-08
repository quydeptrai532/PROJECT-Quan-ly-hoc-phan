package com.edu.registration.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class NotificationDtos {
    private NotificationDtos() { }

    public record ThongBaoDto(Long id, String eventType, String title, String message,
                              String priority, String targetUrl, LocalDateTime createdAt,
                              LocalDateTime readAt, boolean read) { }

    public record ThongBaoPageDto(List<ThongBaoDto> content, int page, int size,
                                  long totalElements, int totalPages, long unreadCount) { }
}
