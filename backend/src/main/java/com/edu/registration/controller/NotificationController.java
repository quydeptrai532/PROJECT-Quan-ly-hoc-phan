package com.edu.registration.controller;

import com.edu.registration.dto.NotificationDtos;
import com.edu.registration.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('SINH_VIEN','GIANG_VIEN')")
@Tag(name = "Thông báo (Notifications)", description = "Xem và quản lý thông báo hệ thống dành cho Sinh viên và Giảng viên")
public class NotificationController {

    private final NotificationService thongBaoService;

    public NotificationController(NotificationService thongBaoService) {
        this.thongBaoService = thongBaoService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy danh sách thông báo phân trang của người dùng hiện tại, có thể lọc thông báo chưa đọc")
    public NotificationDtos.ThongBaoPageDto list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return thongBaoService.list(unreadOnly, page, size);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Đánh dấu một thông báo đã đọc", description = "Cập nhật trạng thái thông báo thành đã đọc")
    public NotificationDtos.ThongBaoDto markRead(@PathVariable Long id) {
        return thongBaoService.markRead(id);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc", description = "Đánh dấu toàn bộ thông báo chưa đọc của người dùng thành đã đọc")
    public long markAllRead() {
        return thongBaoService.markAllRead();
    }
}
