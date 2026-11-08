package com.edu.registration.controller;

import com.edu.registration.dto.NotificationDtos;
import com.edu.registration.service.NotificationService;
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
public class NotificationController {

    private final NotificationService thongBaoService;

    public NotificationController(NotificationService thongBaoService) {
        this.thongBaoService = thongBaoService;
    }

    @GetMapping
    public NotificationDtos.ThongBaoPageDto list(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return thongBaoService.list(unreadOnly, page, size);
    }

    @PatchMapping("/{id}/read")
    public NotificationDtos.ThongBaoDto markRead(@PathVariable Long id) {
        return thongBaoService.markRead(id);
    }

    @PatchMapping("/read-all")
    public long markAllRead() {
        return thongBaoService.markAllRead();
    }
}
