package com.edu.registration.controller;

import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.service.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/phong-hoc")
@PreAuthorize("hasRole('QUAN_TRI')")
@Tag(name = "Quản trị - Phòng học", description = "Quản lý cơ sở vật chất phòng học, sức chứa, tòa nhà và trạng thái bảo trì")
public class AdminClassroomController {

    private final ClassroomService classroomService;

    public AdminClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    @Operation(summary = "Danh sách phòng học", description = "Lọc danh sách phòng học theo loại phòng, tòa nhà, sức chứa và trạng thái hoạt động")
    public List<ClassroomDtos.PhongHocDto> danhSach(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String loaiPhong,
            @RequestParam(required = false) ClassroomStatus trangThai,
            @RequestParam(required = false) Integer sucChuaMin,
            @RequestParam(required = false) Integer sucChuaMax,
            @RequestParam(required = false) String toaNha) {
        String term = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return classroomService.danhSach(term, loaiPhong, trangThai, sucChuaMin, sucChuaMax, toaNha);
    }

    @GetMapping("/hoat-dong")
    @Operation(summary = "Danh sách phòng học hoạt động", description = "Tra cứu nhanh các phòng học sẵn sàng phục vụ xếp thời khóa biểu")
    public List<ClassroomDtos.PhongHocDto> danhSachHoatDong() {
        return classroomService.danhSachHoatDong();
    }

    @GetMapping("/taxonomy")
    @Operation(summary = "Lấy phân loại phòng học", description = "Lấy danh mục các loại phòng và danh sách các tòa nhà")
    public ClassroomDtos.TaxonomyPhongHocDto layTaxonomy() {
        return classroomService.layTaxonomy();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết phòng học", description = "Lấy thông tin chi tiết một phòng học theo ID")
    public ClassroomDtos.PhongHocDto chiTiet(@PathVariable Long id) {
        return classroomService.chiTiet(id);
    }

    @PostMapping
    public ClassroomDtos.PhongHocDto tao(@Valid @RequestBody ClassroomDtos.PhongHocRequest request) {
        return classroomService.tao(request);
    }

    @PutMapping("/{id}")
    public ClassroomDtos.PhongHocDto sua(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomDtos.PhongHocRequest request) {
        return classroomService.sua(id, request);
    }

    @PatchMapping("/{id}/trang-thai")
    public ClassroomDtos.PhongHocDto doiTrangThai(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomDtos.DoiTrangThaiRequest request) {
        return classroomService.doiTrangThai(id, request.trangThai(), request.ghiChu());
    }

    @DeleteMapping("/{id}")
    public void xoa(@PathVariable Long id) {
        classroomService.xoa(id);
    }
}
