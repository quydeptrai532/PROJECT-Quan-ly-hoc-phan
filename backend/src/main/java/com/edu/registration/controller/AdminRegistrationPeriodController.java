package com.edu.registration.controller;

import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.service.RegistrationPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/dot-dang-ky")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Đợt đăng ký học phần", description = "Cấu hình khung thời gian mở đợt đăng ký học phần, gia hạn và phân bổ đối tượng sinh viên ưu tiên")
public class AdminRegistrationPeriodController {

    private final RegistrationPeriodService service;

    public AdminRegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Danh sách đợt đăng ký", description = "Lọc các đợt đăng ký học phần theo học kỳ, trạng thái hiệu lực và loại đợt")
    public List<RegistrationPeriodDtos.DotDangKyDto> danhSach(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean dangHieuLuc,
            @RequestParam(required = false) String loaiDot) {
        return service.danhSach(hocKyId, keyword, dangHieuLuc, loaiDot);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết đợt đăng ký", description = "Xem cấu hình chi tiết, thời gian bắt đầu, kết thúc và nhóm đối tượng áp dụng")
    public RegistrationPeriodDtos.DotDangKyDto chiTiet(@PathVariable Long id) {
        return service.chiTiet(id);
    }

    @PostMapping
    @Operation(summary = "Tạo mới đợt đăng ký", description = "Thiết lập đợt đăng ký học phần mới kèm nhóm đối tượng được phép đăng ký")
    public RegistrationPeriodDtos.DotDangKyDto tao(
            @Valid @RequestBody RegistrationPeriodDtos.TaoDotDangKyRequest request) {
        return service.tao(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật đợt đăng ký", description = "Chỉnh sửa thông tin, thời hạn hoặc nhóm sinh viên áp dụng của đợt")
    public RegistrationPeriodDtos.DotDangKyDto sua(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationPeriodDtos.SuaDotDangKyRequest request) {
        return service.sua(id, request);
    }

    @PostMapping("/{id}/gia-han")
    @Operation(summary = "Gia hạn đợt đăng ký", description = "Kéo dài thời gian kết thúc của đợt đăng ký đang mở")
    public RegistrationPeriodDtos.DotDangKyDto giaHan(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationPeriodDtos.GiaHanDotRequest request) {
        return service.giaHan(id, request);
    }

    @PostMapping("/{id}/dong-som")
    public RegistrationPeriodDtos.DotDangKyDto dongSom(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationPeriodDtos.DongSomDotRequest request) {
        return service.dongSom(id, request);
    }

    @PostMapping("/mo-bo-sung")
    public RegistrationPeriodDtos.DotDangKyDto moBoSung(
            @Valid @RequestBody RegistrationPeriodDtos.MoBoSungDotRequest request) {
        return service.moBoSung(request);
    }

    @DeleteMapping("/{id}")
    public void xoa(@PathVariable Long id) {
        service.xoa(id);
    }
}
