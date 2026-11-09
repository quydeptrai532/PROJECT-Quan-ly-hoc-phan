package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.service.SemesterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommonController {

    private final SemesterService hocKyService;

    public CommonController(SemesterService hocKyService) {
        this.hocKyService = hocKyService;
    }

    @GetMapping("/hoc-ky")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyService.danhSach();
    }
}
