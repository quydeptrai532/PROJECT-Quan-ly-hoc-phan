package com.edu.registration.controller;

import com.edu.registration.dto.AdminLookupDtos.*;
import com.edu.registration.service.AdminLookupService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tra-cuu")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminLookupController {

    private final AdminLookupService service;

    public AdminLookupController(AdminLookupService service) {
        this.service = service;
    }

    @GetMapping("/sinh-vien")
    public PageDto<StudentSummaryDto> timSinhVien(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.timSinhVien(keyword, page, size);
    }

    @GetMapping("/sinh-vien/{id}/lich-su")
    public StudentHistoryDto lichSuSinhVien(@PathVariable Long id,
                                            @RequestParam(required = false) Long hocKyId,
                                            @RequestParam(required = false) String trangThai,
                                            @RequestParam(required = false) String mucDich,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.lichSuSinhVien(id, hocKyId, trangThai, mucDich, page, size);
    }

    @GetMapping("/giang-vien")
    public PageDto<LecturerSummaryDto> timGiangVien(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.timGiangVien(keyword, page, size);
    }

    @GetMapping("/giang-vien/{id}/lich-su")
    public LecturerHistoryDto lichSuGiangVien(@PathVariable Long id,
                                              @RequestParam(required = false) Long hocKyId,
                                              @RequestParam(required = false) String mucDich,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.lichSuGiangVien(id, hocKyId, mucDich, page, size);
    }
}
