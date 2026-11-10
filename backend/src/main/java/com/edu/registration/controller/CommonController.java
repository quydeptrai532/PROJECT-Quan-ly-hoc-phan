package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.dto.ClassroomDtos;
import com.edu.registration.service.ClassroomService;
import com.edu.registration.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Danh mục dùng chung (Common)", description = "Các API tra cứu danh mục công khai hoặc dùng chung như học kỳ và phòng học")
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
    @Operation(summary = "Lấy danh sách học kỳ", description = "Lấy toàn bộ danh sách các học kỳ đào tạo trong hệ thống")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return hocKyService.danhSach();
    }

    @GetMapping("/phong-hoc/hoat-dong")
    @Operation(summary = "Lấy danh sách phòng học đang hoạt động", description = "Tra cứu các phòng học có trạng thái hoạt động bình thường để phục vụ xếp lịch")
    public List<ClassroomDtos.PhongHocDto> danhSachPhongHoatDong() {
        if (classroomService == null) {
            return List.of();
        }
        return classroomService.danhSachHoatDong();
    }
}
