package com.edu.registration.controller;

import com.edu.registration.dto.SupportRequestDtos;
import com.edu.registration.entity.Role;
import com.edu.registration.entity.User;
import com.edu.registration.exception.ApiException;
import com.edu.registration.security.CurrentUser;
import com.edu.registration.service.SupportRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Yêu cầu hỗ trợ sinh viên", description = "Tiếp nhận phiếu hỗ trợ/khiếu nại, trao đổi thông tin, tải tệp minh chứng và xử lý hàng đợi")
public class SupportRequestController {

    private final SupportRequestService yeuCauHoTroService;
    private final CurrentUser currentUser;

    public SupportRequestController(SupportRequestService yeuCauHoTroService, CurrentUser currentUser) {
        this.yeuCauHoTroService = yeuCauHoTroService;
        this.currentUser = currentUser;
    }

    @GetMapping("/yeu-cau-ho-tro/danh-muc")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @Operation(summary = "Lấy danh mục hỗ trợ", description = "Lấy danh sách các loại yêu cầu hỗ trợ, trạng thái và giới hạn đính kèm tệp")
    public SupportRequestDtos.DanhMucHoTroDto danhMuc() {
        return yeuCauHoTroService.danhMuc();
    }

    @PostMapping(value = "/yeu-cau-ho-tro", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo yêu cầu hỗ trợ (JSON)", description = "Sinh viên tạo phiếu yêu cầu hỗ trợ mới qua dữ liệu JSON không đính kèm tệp")
    public SupportRequestDtos.YeuCauChiTietDto taoYeuCauJson(
            @Valid @RequestBody SupportRequestDtos.TaoYeuCauRequest request) {
        return yeuCauHoTroService.taoYeuCau(request, List.of());
    }

    @PostMapping(value = "/yeu-cau-ho-tro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo yêu cầu hỗ trợ kèm tệp đính kèm (Multipart)", description = "Sinh viên gửi phiếu yêu cầu hỗ trợ kèm tệp minh chứng định dạng PDF hoặc hình ảnh")
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
    @Operation(summary = "Xem danh sách yêu cầu hỗ trợ của tôi", description = "Sinh viên xem danh sách các phiếu yêu cầu hỗ trợ đã tạo kèm trạng thái xử lý")
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
    @Operation(summary = "Xem danh sách yêu cầu hỗ trợ theo vai trò", description = "Lấy danh sách yêu cầu hỗ trợ cá nhân (nếu là Sinh viên) hoặc danh sách hàng đợi (nếu là Quản trị)")
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachTheoVaiTro(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String mucUuTien,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Long handlerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User actor = currentUser.get();
        if (actor.getRole() == Role.QUAN_TRI) {
            return yeuCauHoTroService.danhSachHangDoi(scope, trangThai, loaiYeuCau, mucUuTien, slaStatus, handlerUserId, keyword, page, size);
        }
        return yeuCauHoTroService.danhSachCuaToi(trangThai, loaiYeuCau, keyword, page, size);
    }

    @GetMapping("/admin/yeu-cau-ho-tro")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Xem danh sách quản trị yêu cầu hỗ trợ", description = "Quản trị viên lọc danh sách yêu cầu hỗ trợ theo độ ưu tiên, trạng thái SLA và nhân viên phụ trách")
    public Page<SupportRequestDtos.YeuCauTomTatDto> danhSachQuanTri(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String mucUuTien,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Long handlerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return yeuCauHoTroService.danhSachHangDoi(scope, trangThai, loaiYeuCau, mucUuTien, slaStatus, handlerUserId, keyword, page, size);
    }

    @GetMapping("/admin/yeu-cau-ho-tro/queue")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Xem hàng đợi xử lý hỗ trợ", description = "Quản trị viên theo dõi hàng đợi phiếu cần tiếp nhận và phân công xử lý")
    public Page<SupportRequestDtos.YeuCauTomTatDto> hangDoiXuLy(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String loaiYeuCau,
            @RequestParam(required = false) String mucUuTien,
            @RequestParam(required = false) String slaStatus,
            @RequestParam(required = false) Long handlerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return yeuCauHoTroService.danhSachHangDoi(scope, trangThai, loaiYeuCau, mucUuTien, slaStatus, handlerUserId, keyword, page, size);
    }

    @GetMapping("/admin/yeu-cau-ho-tro/queue/summary")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Xem tóm tắt chỉ số SLA hàng đợi", description = "Thống kê tổng số phiếu mới, số phiếu đang xử lý, số phiếu sắp chạm hạn SLA hoặc đã quá hạn")
    public SupportRequestDtos.SupportRequestSlaSummaryDto tomTatHangDoi() {
        return yeuCauHoTroService.tomTatHangDoi();
    }

    @GetMapping("/admin/yeu-cau-ho-tro/handlers")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Danh sách cán bộ xử lý hỗ trợ", description = "Lấy danh sách các cán bộ có quyền tiếp nhận và phân công giải quyết phiếu")
    public List<SupportRequestDtos.HandlerUserDto> danhSachNguoiXuLy() {
        return yeuCauHoTroService.danhSachNhanVienXuLy();
    }

    @PatchMapping("/admin/yeu-cau-ho-tro/{id}/phan-cong")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Phân công cán bộ xử lý phiếu", description = "Gán cán bộ phụ trách trực tiếp xử lý yêu cầu hỗ trợ")
    public SupportRequestDtos.YeuCauChiTietDto phanCong(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.PhanCongRequest request) {
        return yeuCauHoTroService.phanCong(id, request);
    }

    @PatchMapping("/admin/yeu-cau-ho-tro/{id}/triage")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Điều chỉnh phân loại và mức ưu tiên phiếu", description = "Cập nhật loại yêu cầu và mức độ khẩn cấp (thấp, trung bình, cao, khẩn cấp)")
    public SupportRequestDtos.YeuCauChiTietDto dieuChinh(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.DieuChinhYeuCauRequest request) {
        return yeuCauHoTroService.dieuChinh(id, request);
    }

    @PostMapping("/admin/yeu-cau-ho-tro/{id}/yeu-cau-minh-chung")
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Yêu cầu sinh viên bổ sung minh chứng", description = "Chuyển trạng thái phiếu sang Chờ phản hồi sinh viên và gửi tin nhắn yêu cầu giấy tờ")
    public SupportRequestDtos.YeuCauChiTietDto yeuCauMinhChung(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.YeuCauMinhChungRequest request) {
        return yeuCauHoTroService.yeuCauMinhChung(id, request);
    }

    @PostMapping(value = "/yeu-cau-ho-tro/{id}/bo-sung-minh-chung", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Sinh viên bổ sung minh chứng kèm tệp (Multipart)", description = "Đính kèm thêm tệp minh chứng và gửi giải trình bổ sung")
    public SupportRequestDtos.YeuCauChiTietDto boSungMinhChungMultipart(
            @PathVariable Long id,
            @RequestPart(value = "payload", required = false) @Valid SupportRequestDtos.BoSungMinhChungRequest payload,
            @RequestParam(value = "noiDung", required = false) String noiDung,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile singleFile) {

        SupportRequestDtos.BoSungMinhChungRequest effectiveRequest = payload != null
                ? payload
                : new SupportRequestDtos.BoSungMinhChungRequest(noiDung);

        List<MultipartFile> combinedFiles = new ArrayList<>();
        if (files != null) combinedFiles.addAll(files);
        if (singleFile != null) combinedFiles.add(singleFile);

        return yeuCauHoTroService.boSungMinhChung(id, effectiveRequest, combinedFiles);
    }

    @PostMapping(value = "/yeu-cau-ho-tro/{id}/bo-sung-minh-chung", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('SINH_VIEN')")
    @Operation(summary = "Sinh viên bổ sung phản hồi (JSON)", description = "Gửi thêm nội dung giải trình bổ sung dưới dạng JSON")
    public SupportRequestDtos.YeuCauChiTietDto boSungMinhChungJson(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.BoSungMinhChungRequest request) {
        return yeuCauHoTroService.boSungMinhChung(id, request, List.of());
    }

    @GetMapping({"/yeu-cau-ho-tro/{id}", "/admin/yeu-cau-ho-tro/{id}"})
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @Operation(summary = "Xem chi tiết yêu cầu hỗ trợ", description = "Xem nội dung phiếu, lịch sử trao đổi phản hồi, danh sách tệp đính kèm và hạn SLA")
    public SupportRequestDtos.YeuCauChiTietDto chiTiet(@PathVariable Long id) {
        return yeuCauHoTroService.chiTiet(id);
    }

    @PutMapping("/yeu-cau-ho-tro/{id}")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @Operation(summary = "Chặn chỉnh sửa nội dung gốc", description = "Endpoint bảo vệ tính toàn vẹn: nội dung gốc của phiếu hỗ trợ không thể bị sửa đổi sau khi gửi")
    public void chanSuaNoiDungGoc(@PathVariable Long id) {
        throw ApiException.badRequest("Nội dung gốc của phiếu hỗ trợ không được phép chỉnh sửa sau khi gửi");
    }

    @PostMapping("/yeu-cau-ho-tro/{id}/trao-doi")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Gửi tin nhắn trao đổi trong phiếu hỗ trợ", description = "Sinh viên hoặc cán bộ quản trị gửi phản hồi trao đổi trong phiếu")
    public SupportRequestDtos.TraoDoiDto themTraoDoi(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.ThemTraoDoiRequest request) {
        return yeuCauHoTroService.themTraoDoi(id, request);
    }

    @PatchMapping({"/yeu-cau-ho-tro/{id}/trang-thai", "/admin/yeu-cau-ho-tro/{id}/trang-thai"})
    @PreAuthorize("hasRole('QUAN_TRI')")
    @Operation(summary = "Cập nhật trạng thái phiếu hỗ trợ", description = "Quản trị viên chuyển trạng thái phiếu (Đang xử lý, Đã giải quyết, Từ chối, Đã đóng)")
    public SupportRequestDtos.YeuCauChiTietDto doiTrangThai(
            @PathVariable Long id,
            @Valid @RequestBody SupportRequestDtos.DoiTrangThaiRequest request) {
        return yeuCauHoTroService.doiTrangThai(id, request);
    }

    @GetMapping("/yeu-cau-ho-tro/{id}/tep-dinh-kem/{attachmentId}")
    @PreAuthorize("hasAnyRole('SINH_VIEN','QUAN_TRI')")
    @Operation(summary = "Tải tệp đính kèm an toàn", description = "Tải về tệp minh chứng đính kèm với các header bảo mật ngăn chặn thực thi mã độc")
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
