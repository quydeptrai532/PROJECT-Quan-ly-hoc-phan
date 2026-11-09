package com.edu.registration.controller;

import com.edu.registration.dto.CatalogDtos;
import com.edu.registration.service.CatalogService;
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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('QUAN_TRI')")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/khoa")
    public List<CatalogDtos.KhoaDto> danhSachKhoa() {
        return catalogService.danhSachKhoa();
    }

    @PostMapping("/khoa")
    public CatalogDtos.KhoaDto taoKhoa(@Valid @RequestBody CatalogDtos.KhoaRequest request) {
        return catalogService.taoKhoa(request);
    }

    @PutMapping("/khoa/{id}")
    public CatalogDtos.KhoaDto suaKhoa(@PathVariable Long id, @Valid @RequestBody CatalogDtos.KhoaRequest request) {
        return catalogService.suaKhoa(id, request);
    }

    @DeleteMapping("/khoa/{id}")
    public void xoaKhoa(@PathVariable Long id) {
        catalogService.xoaKhoa(id);
    }

    @GetMapping("/nganh")
    public List<CatalogDtos.NganhDto> danhSachNganh() {
        return catalogService.danhSachNganh();
    }

    @PostMapping("/nganh")
    public CatalogDtos.NganhDto taoNganh(@Valid @RequestBody CatalogDtos.NganhRequest request) {
        return catalogService.taoNganh(request);
    }

    @PutMapping("/nganh/{id}")
    public CatalogDtos.NganhDto suaNganh(@PathVariable Long id,
                                         @Valid @RequestBody CatalogDtos.NganhRequest request) {
        return catalogService.suaNganh(id, request);
    }

    @DeleteMapping("/nganh/{id}")
    public void xoaNganh(@PathVariable Long id) {
        catalogService.xoaNganh(id);
    }

    @GetMapping("/lop-hanh-chinh")
    public List<CatalogDtos.LopHanhChinhDto> danhSachLopHanhChinh() {
        return catalogService.danhSachLopHanhChinh();
    }

    @PostMapping("/lop-hanh-chinh")
    public CatalogDtos.LopHanhChinhDto taoLopHanhChinh(
            @Valid @RequestBody CatalogDtos.LopHanhChinhRequest request) {
        return catalogService.taoLopHanhChinh(request);
    }

    @PutMapping("/lop-hanh-chinh/{id}")
    public CatalogDtos.LopHanhChinhDto suaLopHanhChinh(
            @PathVariable Long id, @Valid @RequestBody CatalogDtos.LopHanhChinhRequest request) {
        return catalogService.suaLopHanhChinh(id, request);
    }

    @DeleteMapping("/lop-hanh-chinh/{id}")
    public void xoaLopHanhChinh(@PathVariable Long id) {
        catalogService.xoaLopHanhChinh(id);
    }

    @GetMapping({"/khoa-hoc", "/nien-khoa"})
    public List<CatalogDtos.KhoaHocDto> danhSachKhoaHoc(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean dangHieuLuc,
            @RequestParam(required = false) Integer namBatDau) {
        String term = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return catalogService.danhSachKhoaHoc(term, dangHieuLuc, namBatDau);
    }

    @PostMapping({"/khoa-hoc", "/nien-khoa"})
    public CatalogDtos.KhoaHocDto taoKhoaHoc(@Valid @RequestBody CatalogDtos.KhoaHocRequest request) {
        return catalogService.taoKhoaHoc(request);
    }

    @PutMapping({"/khoa-hoc/{id}", "/nien-khoa/{id}"})
    public CatalogDtos.KhoaHocDto suaKhoaHoc(@PathVariable Long id,
                                             @Valid @RequestBody CatalogDtos.KhoaHocRequest request) {
        return catalogService.suaKhoaHoc(id, request);
    }

    @DeleteMapping({"/khoa-hoc/{id}", "/nien-khoa/{id}"})
    public void xoaKhoaHoc(@PathVariable Long id) {
        catalogService.xoaKhoaHoc(id);
    }

    @GetMapping("/mon-hoc")
    public List<CatalogDtos.MonHocDto> danhSachMonHoc(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long khoaId) {
        return catalogService.danhSachMonHoc(keyword, khoaId);
    }

    @PostMapping("/mon-hoc")
    public CatalogDtos.MonHocDto taoMonHoc(@Valid @RequestBody CatalogDtos.MonHocRequest request) {
        return catalogService.taoMonHoc(request);
    }

    @PutMapping("/mon-hoc/{id}")
    public CatalogDtos.MonHocDto suaMonHoc(@PathVariable Long id,
                                           @Valid @RequestBody CatalogDtos.MonHocRequest request) {
        return catalogService.suaMonHoc(id, request);
    }

    @DeleteMapping("/mon-hoc/{id}")
    public void xoaMonHoc(@PathVariable Long id) {
        catalogService.xoaMonHoc(id);
    }

    @GetMapping({"/mon-hoc/{id}/tien-quyet", "/mon-hoc/{id}/quan-he"})
    public List<CatalogDtos.MonTienQuyetDto> danhSachTienQuyet(
            @PathVariable Long id,
            @RequestParam(required = false) String loaiQuanHe,
            @RequestParam(required = false) Boolean dangHieuLuc) {
        return catalogService.danhSachTienQuyet(id, loaiQuanHe, dangHieuLuc);
    }

    @PostMapping({"/mon-hoc/{id}/tien-quyet", "/mon-hoc/{id}/quan-he"})
    public CatalogDtos.MonTienQuyetDto themTienQuyet(
            @PathVariable Long id, @Valid @RequestBody CatalogDtos.MonTienQuyetRequest request) {
        return catalogService.themTienQuyet(id, request);
    }

    @PutMapping({"/mon-hoc/{id}/tien-quyet/{lienKetId}", "/mon-hoc/{id}/quan-he/{lienKetId}"})
    public CatalogDtos.MonTienQuyetDto suaTienQuyet(
            @PathVariable Long id,
            @PathVariable Long lienKetId,
            @Valid @RequestBody CatalogDtos.MonTienQuyetRequest request) {
        return catalogService.suaTienQuyet(id, lienKetId, request);
    }

    @DeleteMapping({"/mon-hoc/{id}/tien-quyet/{lienKetId}", "/mon-hoc/{id}/quan-he/{lienKetId}"})
    public void xoaTienQuyet(@PathVariable Long id, @PathVariable Long lienKetId) {
        catalogService.xoaTienQuyet(id, lienKetId);
    }

    @GetMapping({"/quan-he-mon/quy-che", "/loai-quan-he-mon"})
    public List<CatalogDtos.QuyCheQuanHeMonDto> danhSachQuyCheQuanHeMon() {
        return catalogService.danhSachQuyCheQuanHeMon();
    }

    @GetMapping("/quan-he-mon")
    public List<CatalogDtos.MonTienQuyetDto> danhSachQuanHeMon(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long monHocId,
            @RequestParam(required = false) String loaiQuanHe,
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(required = false) String khoaNhapHoc,
            @RequestParam(required = false) Boolean dangHieuLuc) {
        String term = (keyword != null && !keyword.isBlank()) ? keyword : q;
        return catalogService.danhSachQuanHeMon(term, monHocId, loaiQuanHe, hocKyId, khoaNhapHoc, dangHieuLuc);
    }

    @PostMapping("/quan-he-mon")
    public CatalogDtos.MonTienQuyetDto taoQuanHeMon(@Valid @RequestBody CatalogDtos.QuanHeMonRequest request) {
        return catalogService.taoQuanHeMon(request);
    }

    @PutMapping("/quan-he-mon/{id}")
    public CatalogDtos.MonTienQuyetDto suaQuanHeMon(
            @PathVariable Long id,
            @Valid @RequestBody CatalogDtos.QuanHeMonRequest request) {
        return catalogService.suaQuanHeMon(id, request);
    }

    @DeleteMapping("/quan-he-mon/{id}")
    public void xoaQuanHeMon(@PathVariable Long id) {
        catalogService.xoaQuanHeMon(id);
    }

    @GetMapping("/hoc-ky")
    public List<CatalogDtos.HocKyDto> danhSachHocKy() {
        return catalogService.danhSachHocKy();
    }

    @PostMapping("/hoc-ky")
    public CatalogDtos.HocKyDto taoHocKy(@Valid @RequestBody CatalogDtos.HocKyRequest request) {
        return catalogService.taoHocKy(request);
    }

    @PutMapping("/hoc-ky/{id}")
    public CatalogDtos.HocKyDto suaHocKy(@PathVariable Long id,
                                         @Valid @RequestBody CatalogDtos.HocKyRequest request) {
        return catalogService.suaHocKy(id, request);
    }

    @DeleteMapping("/hoc-ky/{id}")
    public void xoaHocKy(@PathVariable Long id) {
        catalogService.xoaHocKy(id);
    }
}
