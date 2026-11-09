package com.edu.registration.controller;

import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.entity.ClassroomStatus;
import com.edu.registration.service.ClassroomService;
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
public class AdminClassroomController {

    private final ClassroomService classroomService;

    public AdminClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
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
    public List<ClassroomDtos.PhongHocDto> danhSachHoatDong() {
        return classroomService.danhSachHoatDong();
    }

    @GetMapping("/taxonomy")
    public ClassroomDtos.TaxonomyPhongHocDto layTaxonomy() {
        return classroomService.layTaxonomy();
    }

    @GetMapping("/{id}")
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
