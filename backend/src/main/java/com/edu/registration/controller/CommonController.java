package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.service.ClassroomService;
import com.edu.registration.service.SemesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommonController {

    private final SemesterService hocKyService;
    private final ClassroomService classroomService;

    public CommonController(SemesterService hocKyService) {
        this(hocKyService, null);
    }

    @Autowired
    public CommonController(SemesterService hocKyService,
                            @Autowired(required = false) ClassroomService classroomService) {
        this.hocKyService = hocKyService;
        this.classroomService = classroomService;
    }

    @GetMapping("/hoc-ky")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyService.danhSach();
    }

    @GetMapping("/phong-hoc/hoat-dong")
    public List<ClassroomDtos.PhongHocDto> danhSachPhongHoatDong() {
        if (classroomService == null) {
            return List.of();
        }
        return classroomService.danhSachHoatDong();
    }
}
