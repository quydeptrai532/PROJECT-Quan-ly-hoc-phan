package com.edu.registration.controller;

import com.edu.registration.dto.CourseClassDtos;
import com.edu.registration.dto.RegistrationDtos;
import com.edu.registration.service.CourseClassService;
import com.edu.registration.service.GradeService;
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

    @PostMapping("/lop-hoc-phan/kiem-tra-xung-dot")
    public CourseClassDtos.KetQuaKiemTraXungDotDto kiemTraXungDot(
            @Valid @RequestBody CourseClassDtos.KiemTraXungDotRequest request) {
        return lopHocPhanService.kiemTraXungDot(request);
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

    @GetMapping("/lop-hoc-phan/{id}/khao-sat-huy")
    public CourseClassDtos.KhaoSatHuyLopDto khaoSatHuy(@PathVariable Long id) {
        return lopHocPhanService.khaoSatHuyLop(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/huy")
    public CourseClassDtos.HuyLopHocPhanResponse huy(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CourseClassDtos.HuyLopHocPhanRequest request) {
        return lopHocPhanService.huyLopHocPhan(id, request != null ? request.lyDo() : null);
    }

    @PostMapping("/lop-hoc-phan/{id}/kiem-tra-chuyen")
    public CourseClassDtos.KetQuaKiemTraChuyenLopDto kiemTraChuyen(
            @PathVariable Long id,
            @Valid @RequestBody CourseClassDtos.KiemTraChuyenLopRequest request) {
        return lopHocPhanService.kiemTraChuyenLop(id, request.lopDichId(), request.sinhVienIds());
    }

    @PostMapping("/lop-hoc-phan/{id}/chuyen-sinh-vien")
    public CourseClassDtos.ChuyenSinhVienResponse chuyenSinhVien(
            @PathVariable Long id,
            @Valid @RequestBody CourseClassDtos.ChuyenSinhVienRequest request) {
        return lopHocPhanService.chuyenSinhVien(id, request.lopDichId(), request.sinhVienId(), request.lyDo());
    }

    @PostMapping("/lop-hoc-phan/{id}/chuyen-hang-loat")
    public CourseClassDtos.KetQuaChuyenHangLoatDto chuyenHangLoat(
            @PathVariable Long id,
            @Valid @RequestBody CourseClassDtos.ChuyenHangLoatRequest request) {
        return lopHocPhanService.chuyenSinhVienHangLoat(id, request.lopDichId(), request.sinhVienIds(), request.lyDo());
    }

    @GetMapping("/lop-hoc-phan/{id}/lich-su-chuyen")
    public List<CourseClassDtos.LichSuChuyenLopDto> lichSuChuyenTheoLop(@PathVariable Long id) {
        return lopHocPhanService.danhSachLichSuChuyen(id, null);
    }

    @GetMapping("/lop-hoc-phan/lich-su-chuyen")
    public List<CourseClassDtos.LichSuChuyenLopDto> lichSuChuyen(
            @RequestParam(required = false) Long lopHocPhanId,
            @RequestParam(required = false) Long hocKyId) {
        return lopHocPhanService.danhSachLichSuChuyen(lopHocPhanId, hocKyId);
    }

    @GetMapping("/lop-hoc-phan/{id}/sinh-vien")
    public List<RegistrationDtos.DiemDto> danhSachDiemSinhVien(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword) {
        return diemService.danhSachDiemLop(id, keyword);
    }

    @GetMapping("/lop-hoc-phan/{id}/trang-thai-bang-diem")
    public RegistrationDtos.TrangThaiBangDiemDto trangThaiBangDiem(@PathVariable Long id) {
        return diemService.chiTietTrangThaiBangDiem(id);
    }

    @GetMapping("/lop-hoc-phan/{id}/lich-su-bang-diem")
    public List<RegistrationDtos.LichSuTrangThaiBangDiemDto> lichSuTrangThaiBangDiem(@PathVariable Long id) {
        return diemService.lichSuTrangThaiBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/tra-ve-diem")
    public RegistrationDtos.TrangThaiBangDiemDto traVeBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.TraVeBangDiemRequest request) {
        return diemService.traVeBangDiem(id, request != null ? request.lyDo() : null);
    }

    @PostMapping("/lop-hoc-phan/{id}/phe-duyet-diem")
    public RegistrationDtos.TrangThaiBangDiemDto pheDuyetBangDiem(@PathVariable Long id) {
        return diemService.pheDuyetBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/cong-bo-diem")
    public RegistrationDtos.TrangThaiBangDiemDto congBoBangDiem(@PathVariable Long id) {
        return diemService.congBoBangDiem(id);
    }

    @PostMapping("/lop-hoc-phan/{id}/khoa-diem")
    public RegistrationDtos.TrangThaiBangDiemDto khoaBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.KhoaBangDiemRequest request) {
        return diemService.khoaBangDiem(id, request != null ? request.lyDo() : null);
    }

    @PostMapping({"/lop-hoc-phan/{id}/mo-khoa-diem", "/lop-hoc-phan/{id}/thu-hoi-cong-bo-diem"})
    public RegistrationDtos.TrangThaiBangDiemDto moKhoaBangDiem(
            @PathVariable Long id,
            @RequestBody(required = false) RegistrationDtos.MoKhoaBangDiemRequest request) {
        return diemService.moKhoaBangDiem(id, request != null ? request.lyDo() : null);
    }

    @DeleteMapping("/lop-hoc-phan/{id}")
    public void xoa(@PathVariable Long id) {
        lopHocPhanService.xoa(id);
    }
}
