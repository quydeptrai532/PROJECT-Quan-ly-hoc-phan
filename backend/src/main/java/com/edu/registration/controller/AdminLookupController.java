package com.edu.registration.controller;

import com.edu.registration.dto.AdminLookupDtos.*;
import com.edu.registration.service.AdminLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tra-cuu")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Tra cứu", description = "Tra cứu nhanh hồ sơ sinh viên, giảng viên và lịch sử hoạt động đào tạo")
public class AdminLookupController {

    private final AdminLookupService service;

    public AdminLookupController(AdminLookupService service) {
        this.service = service;
    }

    @GetMapping("/sinh-vien")
    @Operation(summary = "Tìm kiếm sinh viên", description = "Tìm kiếm nhanh hồ sơ sinh viên theo từ khóa mã SV hoặc họ tên")
    public PageDto<StudentSummaryDto> timSinhVien(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return service.timSinhVien(keyword, page, size);
    }

    @GetMapping("/sinh-vien/{id}/lich-su")
    @Operation(summary = "Tra cứu lịch sử học tập sinh viên", description = "Tra cứu toàn diện lịch sử đăng ký, bảng điểm và biến động trạng thái của một sinh viên")
    public StudentHistoryDto lichSuSinhVien(@PathVariable Long id,
                                            @RequestParam(required = false) Long hocKyId,
                                            @RequestParam(required = false) String trangThai,
                                            @RequestParam(required = false) String mucDich,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.lichSuSinhVien(id, hocKyId, trangThai, mucDich, page, size);
    }

    @GetMapping("/giang-vien")
    @Operation(summary = "Tìm kiếm giảng viên", description = "Tìm kiếm giảng viên theo mã cán bộ hoặc họ tên")
    public PageDto<LecturerSummaryDto> timGiangVien(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.timGiangVien(keyword, page, size);
    }

    @GetMapping("/giang-vien/{id}/lich-su")
    @Operation(summary = "Tra cứu lịch sử giảng dạy", description = "Xem lịch sử phân công giảng dạy và các lớp học phần của giảng viên")
    public LecturerHistoryDto lichSuGiangVien(@PathVariable Long id,
                                              @RequestParam(required = false) Long hocKyId,
                                              @RequestParam(required = false) String mucDich,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return service.lichSuGiangVien(id, hocKyId, mucDich, page, size);
    }
}
