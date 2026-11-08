package com.edu.registration.controller;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.service.GradeService;
import com.edu.registration.service.CourseClassService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminCourseClassController {

    private final CourseClassService lopHocPhanService;
    private final GradeService diemService;

    public AdminCourseClassController(CourseClassService lopHocPhanService) {
        this(lopHocPhanService, null);
    }

    @Autowired
    public AdminCourseClassController(CourseClassService lopHocPhanService, GradeService diemService) {
        this.lopHocPhanService = lopHocPhanService;
        this.diemService = diemService;
    }

    @GetMapping("/lop-hoc-phan")
    public List<CourseClassDtos.LopHocPhanDto> danhSach(@RequestParam(required = false) Long hocKyId,
                                                       @RequestParam(required = false) String keyword) {
        return lopHocPhanService.danhSach(hocKyId, keyword);
    }

    @PostMapping("/lop-hoc-phan")
    public CourseClassDtos.LopHocPhanResponse tao(
            @Valid @RequestBody CourseClassDtos.LopHocPhanRequest request) {
        return lopHocPhanService.tao(request);
    }

    @PutMapping("/lop-hoc-phan/{id}")
    public CourseClassDtos.LopHocPhanResponse sua(@PathVariable Long id,
                                                 @Valid @RequestBody CourseClassDtos.LopHocPhanRequest request) {
        return lopHocPhanService.sua(id, request);
    }

    @PostMapping("/lop-hoc-phan/{id}/huy")
    public CourseClassDtos.HuyLopHocPhanResponse huy(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CourseClassDtos.HuyLopHocPhanRequest request) {
        return lopHocPhanService.huyLopHocPhan(id, request != null ? request.lyDo() : null);
    }

    @GetMapping("/lop-hoc-phan/{id}/trang-thai-bang-diem")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/phe-duyet-diem")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/cong-bo-diem")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem")
    public RegistrationDtos.TrangThaiBangDiemDto thuHoiCongBoBangDiem(@PathVariable Long id) {
        return diemService.thuHoiCongBoBangDiem(id);
    }

    @DeleteMapping("/lop-hoc-phan/{id}")
    public void xoa(@PathVariable Long id) {
        lopHocPhanService.xoa(id);
    }
}
