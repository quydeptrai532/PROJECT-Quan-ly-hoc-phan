package com.edu.registration.controller;

import com.edu.registration.dto.GradeAppealDtos;
import com.edu.registration.entity.GradeAppealStatus;
import com.edu.registration.service.GradeAppealService;
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
public class AdminGradeAppealController {

    private final GradeAppealService appealService;

    public AdminGradeAppealController(GradeAppealService appealService) {
        this.appealService = appealService;
    }

    @GetMapping("/cau-hinh")
    public GradeAppealDtos.CauHinhPhucKhaoDto cauHinh() {
        return appealService.layCauHinh();
    }

    @GetMapping
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
    public GradeAppealDtos.HoSoPhucKhaoDto chiTiet(@PathVariable Long id) {
        return appealService.chiTietQuanTri(id);
    }

    @PostMapping("/{id}/tiep-nhan")
    public GradeAppealDtos.HoSoPhucKhaoDto tiepNhan(
            @PathVariable Long id,
            @RequestBody(required = false) GradeAppealDtos.TiepNhanRequest request) {
        return appealService.tiepNhan(id, request != null ? request.ghiChu() : null);
    }

    @PostMapping("/{id}/xu-ly")
    public GradeAppealDtos.HoSoPhucKhaoDto xuLy(
            @PathVariable Long id,
            @Valid @RequestBody GradeAppealDtos.XuLyPhucKhaoRequest request) {
        return appealService.xuLy(id, request);
    }
}
