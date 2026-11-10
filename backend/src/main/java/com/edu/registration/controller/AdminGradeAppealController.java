package com.edu.registration.controller;

import com.edu.registration.dto.GradeAppealDtos;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.service.GradeAppealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/phuc-khao")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Phúc khảo điểm", description = "Tiếp nhận, xử lý, phê duyệt phúc khảo và điều chỉnh điểm học phần")
public class AdminGradeAppealController {

    private final GradeAppealService appealService;

    public AdminGradeAppealController(GradeAppealService appealService) {
        this.appealService = appealService;
    }

    @GetMapping("/cau-hinh")
    @Operation(summary = "Xem cấu hình chính sách phúc khảo", description = "Xem thời hạn tiếp nhận đơn (SLA) và quy chế phê duyệt phúc khảo điểm")
    public GradeAppealDtos.CauHinhPhucKhaoDto cauHinh() {
        return appealService.layCauHinh();
    }

    @GetMapping
    @Operation(summary = "Danh sách hồ sơ phúc khảo toàn trường", description = "Lọc hồ sơ phúc khảo theo học kỳ, lớp học phần, môn học, mã sinh viên hoặc trạng thái")
    public List<GradeAppealDtos.HoSoPhucKhaoDto> danhSach(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) String maSinhVien,
            @RequestParam(required = false) GradeAppealStatus trangThai,
            @RequestParam(required = false) String keyword) {
        return appealService.danhSachQuanTri(hocKyId, lopHocPhanId, monHocId, maSinhVien, trangThai, keyword);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết hồ sơ phúc khảo (Admin)", description = "Lấy đầy đủ thông tin sinh viên, điểm cũ, điểm đề xuất và lịch sử xử lý")
    public GradeAppealDtos.HoSoPhucKhaoDto chiTiet(@PathVariable Long id) {
        return appealService.chiTietQuanTri(id);
    }

    @PostMapping("/{id}/tiep-nhan")
    @Operation(summary = "Tiếp nhận hồ sơ phúc khảo", description = "Chuyển trạng thái hồ sơ sang Đang xử lý và ghi nhận cán bộ tiếp nhận")
    public GradeAppealDtos.HoSoPhucKhaoDto tiepNhan(
            @PathVariable Long id,
            @RequestBody(required = false) GradeAppealDtos.TiepNhanRequest request) {
        return appealService.tiepNhan(id, request != null ? request.ghiChu() : null);
    }

    @PostMapping("/{id}/xu-ly")
    @Operation(summary = "Xử lý kết quả phúc khảo điểm", description = "Cập nhật quyết định chấp thuận (kèm điểm mới) hoặc bác bỏ đơn phúc khảo")
    public GradeAppealDtos.HoSoPhucKhaoDto xuLy(
            @PathVariable Long id,
            @Valid @RequestBody GradeAppealDtos.XuLyPhucKhaoRequest request) {
        return appealService.xuLy(id, request);
    }
}
