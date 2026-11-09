package com.edu.registration.controller;

import com.edu.registration.dto.GradeAppealDtos;
import com.edu.registration.service.GradeAppealService;
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
public class StudentGradeAppealController {

    private final GradeAppealService appealService;

    public StudentGradeAppealController(GradeAppealService appealService) {
        this.appealService = appealService;
    }

    @GetMapping("/danh-muc")
    public GradeAppealDtos.DanhMucPhucKhaoDto danhMuc() {
        return appealService.danhMuc();
    }

    @GetMapping
    public List<GradeAppealDtos.HoSoPhucKhaoDto> danhSach() {
        return appealService.danhSachCuaSinhVien();
    }

    @GetMapping("/{id}")
    public GradeAppealDtos.HoSoPhucKhaoDto chiTiet(@PathVariable Long id) {
        return appealService.chiTietCuaSinhVien(id);
    }

    @PostMapping
    public GradeAppealDtos.HoSoPhucKhaoDto tao(
            @Valid @RequestBody GradeAppealDtos.TaoHoSoPhucKhaoRequest request) {
        return appealService.taoHoSo(request);
    }

    @PostMapping("/{id}/huy")
    public GradeAppealDtos.HoSoPhucKhaoDto huy(
            @PathVariable Long id,
            @RequestBody(required = false) GradeAppealDtos.HuyPhucKhaoRequest request) {
        return appealService.huyHoSo(id, request != null ? request.lyDo() : null);
    }
}
