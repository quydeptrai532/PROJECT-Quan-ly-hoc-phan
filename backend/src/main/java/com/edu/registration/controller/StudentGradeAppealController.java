package com.edu.registration.controller;

import com.edu.registration.dto.GradeAppealDtos;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sinh-vien/phuc-khao")
@PreAuthorize("hasRole('SINH_VIEN')")
@Tag(name = "Sinh viên - Phúc khảo điểm", description = "Tạo yêu cầu phúc khảo điểm học phần, theo dõi tiến độ xử lý và hủy yêu cầu phúc khảo")
public class StudentGradeAppealController {

    private final GradeAppealService appealService;

    public StudentGradeAppealController(GradeAppealService appealService) {
        this.appealService = appealService;
    }

    @GetMapping("/danh-muc")
    @Operation(summary = "Lấy danh mục cấu hình phúc khảo", description = "Lấy thời hạn nhận đơn (SLA), trạng thái và quy định phúc khảo điểm")
    public GradeAppealDtos.DanhMucPhucKhaoDto danhMuc() {
        return appealService.danhMuc();
    }

    @GetMapping
    @Operation(summary = "Xem danh sách hồ sơ phúc khảo", description = "Lấy tất cả các hồ sơ phúc khảo điểm đã gửi của sinh viên hiện tại")
    public List<GradeAppealDtos.HoSoPhucKhaoDto> danhSach() {
        return appealService.danhSachCuaSinhVien();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết hồ sơ phúc khảo", description = "Xem tiến độ thẩm định, ghi chú và kết quả giải quyết hồ sơ phúc khảo")
    public GradeAppealDtos.HoSoPhucKhaoDto chiTiet(@PathVariable Long id) {
        return appealService.chiTietCuaSinhVien(id);
    }

    @PostMapping
    @Operation(summary = "Gửi đơn phúc khảo điểm", description = "Tạo hồ sơ phúc khảo điểm quá trình hoặc điểm thi kết thúc cho một môn học đã công bố điểm")
    public GradeAppealDtos.HoSoPhucKhaoDto tao(
            @Valid @RequestBody GradeAppealDtos.TaoHoSoPhucKhaoRequest request) {
        return appealService.taoHoSo(request);
    }

    @PostMapping("/{id}/huy")
    @Operation(summary = "Hủy hồ sơ phúc khảo", description = "Sinh viên rút đơn phúc khảo khi hồ sơ còn đang ở trạng thái tiếp nhận")
    public GradeAppealDtos.HoSoPhucKhaoDto huy(
            @PathVariable Long id,
            @RequestBody(required = false) GradeAppealDtos.HuyPhucKhaoRequest request) {
        return appealService.huyHoSo(id, request != null ? request.lyDo() : null);
    }
}
