package com.edu.registration.controller;

import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.service.SupportRequestService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SupportRequestController {

    private final SupportRequestService yeuCauHoTroService;
    private final CurrentUser currentUser;

    public SupportRequestController(SupportRequestService yeuCauHoTroService, CurrentUser currentUser) {
        this.yeuCauHoTroService = yeuCauHoTroService;
        this.currentUser = currentUser;
    }

    @GetMapping("/yeu-cau-ho-tro/danh-muc")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    public SupportRequestDtos.DanhMucHoTroDto danhMuc() {
        return yeuCauHoTroService.danhMuc();
    }

    @PostMapping(value = "/yeu-cau-ho-tro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportRequestDtos.YeuCauChiTietDto taoYeuCauJson(
            @Valid @RequestBody SupportRequestDtos.TaoYeuCauRequest request) {
        return yeuCauHoTroService.taoYeuCau(request, List.of());
    }

    @PostMapping(value = "/yeu-cau-ho-tro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportRequestDtos.YeuCauChiTietDto taoYeuCauMultipart(
            @RequestPart(value = "payload", required = false) @Valid SupportRequestDtos.TaoYeuCauRequest payload,
            @RequestParam(value = "loaiYeuCau", required = false) String loaiYeuCau,
            @RequestParam(value = "tieuDe", required = false) String tieuDe,
            @RequestParam(value = "noiDung", required = false) String noiDung,
            @RequestParam(value = "monHocId", required = false) Long monHocId,
            @RequestParam(value = "diemId", required = false) Long diemId,
            @RequestParam(value = "hocKyId", required = false) Long hocKyId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile singleFile) {

        SupportRequestDtos.TaoYeuCauRequest effectiveRequest = payload != null
                ? payload
                : new SupportRequestDtos.TaoYeuCauRequest(loaiYeuCau, tieuDe, noiDung, monHocId, diemId, hocKyId);

        List<MultipartFile> combinedFiles = new ArrayList<>();
        if (files != null) {
            combinedFiles.addAll(files);
        }
        if (singleFile != null) {
            combinedFiles.add(singleFile);
        }

        return yeuCauHoTroService.taoYeuCau(effectiveRequest, combinedFiles);
    }

    @GetMapping({"/yeu-cau-ho-tro/me", "/sinh-vien/me/yeu-cau-ho-tro"})
    @PreAuthorize("hasRole('SINH_VIEN')")
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachCuaToi(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return yeuCauHoTroService.danhSachCuaToi(trangThai, loaiYeuCau, keyword, page, size);
    }

    @GetMapping("/yeu-cau-ho-tro")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachTheoVaiTro(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User actor = currentUser.get();
        if (actor.getRole() == Role.QUAN_TRI) {
            return yeuCauHoTroService.danhSachQuanTri(trangThai, loaiYeuCau, keyword, page, size);
        }
        return yeuCauHoTroService.danhSachCuaToi(trangThai, loaiYeuCau, keyword, page, size);
    }

    @GetMapping("/admin/yeu-cau-ho-tro")
    @PreAuthorize("hasRole('QUAN_TRI')")
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachQuanTri(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return yeuCauHoTroService.danhSachQuanTri(trangThai, loaiYeuCau, keyword, page, size);
    }

    @GetMapping({"/yeu-cau-ho-tro/{id}", "/admin/yeu-cau-ho-tro/{id}"})
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    public SupportRequestDtos.YeuCauChiTietDto chiTiet(@PathVariable Long id) {
        return yeuCauHoTroService.chiTiet(id);
    }

    @PutMapping("/yeu-cau-ho-tro/{id}")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    public void chanSuaNoiDungGoc(@PathVariable Long id) {
        throw ApiException.badRequest("Nội dung gốc của phiếu hỗ trợ không được phép chỉnh sửa sau khi gửi");
    }

    @PostMapping("/yeu-cau-ho-tro/{id}/trao-doi")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @ResponseStatus(HttpStatus.CREATED)
    public SupportRequestDtos.TraoDoiDto themTraoDoi(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.ThemTraoDoiRequest request) {
        return yeuCauHoTroService.themTraoDoi(id, request);
    }

    @PatchMapping({"/yeu-cau-ho-tro/{id}/trang-thai", "/admin/yeu-cau-ho-tro/{id}/trang-thai"})
    @PreAuthorize("hasRole('QUAN_TRI')")
    public SupportRequestDtos.YeuCauChiTietDto doiTrangThai(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.DoiTrangThaiRequest request) {
        return yeuCauHoTroService.doiTrangThai(id, request);
    }

    @GetMapping("/yeu-cau-ho-tro/{id}/tep-dinh-kem/{attachmentId}")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    public ResponseEntity<byte[]> taiTepDinhKem(@PathVariable Long id, @PathVariable Long attachmentId) {
        SupportRequestService.TepTaiXuong tep = yeuCauHoTroService.taiTepDinhKem(id, attachmentId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(tep.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(tep.tenTepGoc(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", "default-src 'none'; sandbox");
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }
}
