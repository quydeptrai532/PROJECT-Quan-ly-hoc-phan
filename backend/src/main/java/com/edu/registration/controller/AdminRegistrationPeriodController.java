package com.edu.registration.controller;

import com.edu.registration.dto.RegistrationPeriodDtos;
import com.edu.registration.service.RegistrationPeriodService;
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
public class AdminRegistrationPeriodController {

    private final RegistrationPeriodService service;

    public AdminRegistrationPeriodController(RegistrationPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistrationPeriodDtos.DotDangKyDto> danhSach(
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean dangHieuLuc,
            @RequestParam(required = false) String loaiDot) {
        return service.danhSach(hocKyId, keyword, dangHieuLuc, loaiDot);
    }

    @GetMapping("/{id}")
    public RegistrationPeriodDtos.DotDangKyDto chiTiet(@PathVariable Long id) {
        return service.chiTiet(id);
    }

    @PostMapping
    public RegistrationPeriodDtos.DotDangKyDto tao(
            @Valid @RequestBody RegistrationPeriodDtos.TaoDotDangKyRequest request) {
        return service.tao(request);
    }

    @PutMapping("/{id}")
    public RegistrationPeriodDtos.DotDangKyDto sua(
            @PathVariable Long id,
            @Valid @RequestBody RegistrationPeriodDtos.SuaDotDangKyRequest request) {
        return service.sua(id, request);
    }

    @PostMapping("/{id}/gia-han")
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
