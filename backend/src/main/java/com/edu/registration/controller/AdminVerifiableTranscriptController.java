package com.edu.registration.controller;

import com.edu.registration.dto.VerifiableTranscriptDtos;
import com.edu.registration.entity.VerifiableTranscriptStatus;
import com.edu.registration.service.StudentTranscriptPdfService;
import com.edu.registration.service.VerifiableTranscriptService;
import com.edu.registration.util.ExportSecuritySupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản trị bảng điểm điện tử xác thực (QLSV-99 / TASK-OP-12).
 * Cung cấp chức năng tra cứu phiên bản phát hành, xem snapshot dữ liệu gốc, thu hồi và cấp lại bảng điểm.
 */
@RestController
@RequestMapping("/api/admin/bang-diem-xac-thuc")
@PreAuthorize("hasRole('QUAN_TRI')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Quản trị Bảng điểm Xác thực", description = "Quản lý phiên bản bảng điểm điện tử xác thực, thu hồi và cấp lại (QLSV-99 / TASK-OP-12)")
public class AdminVerifiableTranscriptController {

    private final VerifiableTranscriptService verifiableTranscriptService;

    public AdminVerifiableTranscriptController(VerifiableTranscriptService verifiableTranscriptService) {
        this.verifiableTranscriptService = verifiableTranscriptService;
    }

    @GetMapping
    @Operation(summary = "Danh sách bảng điểm điện tử xác thực", description = "Tìm kiếm và phân trang danh sách các bảng điểm điện tử đã phát hành.")
    public ResponseEntity<Page<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto>> danhSach(
            @Parameter(description = "Mã sinh viên cần tìm kiếm")
            @RequestParam(required = false) String maSinhVien,
            @Parameter(description = "Trạng thái bảng điểm (HIEU_LUC, DA_THU_HOI, DA_THAY_THE)")
            @RequestParam(required = false) VerifiableTranscriptStatus trangThai,
            @Parameter(description = "ID học kỳ cụ thể (bỏ trống nếu tìm toàn bộ)")
            @RequestParam(required = false) Long hocKyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> result =
                verifiableTranscriptService.timKiemQuanTri(maSinhVien, trangThai, hocKyId, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết bảng điểm xác thực", description = "Xem chi tiết bảng điểm, ảnh chụp dữ liệu gốc (snapshot) và lịch sử phiên bản liên kết.")
    public ResponseEntity<VerifiableTranscriptDtos.VerifiableTranscriptDetailDto> chiTiet(
            @PathVariable Long id) {
        VerifiableTranscriptDtos.VerifiableTranscriptDetailDto result =
                verifiableTranscriptService.chiTietQuanTri(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/thu-hoi")
    @Operation(summary = "Thu hồi bảng điểm điện tử", description = "Thu hồi hiệu lực của một bảng điểm điện tử kèm lý do bắt buộc.")
    public ResponseEntity<VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto> thuHoi(
            @PathVariable Long id,
            @Valid @RequestBody VerifiableTranscriptDtos.RevokeTranscriptRequest request) {
        VerifiableTranscriptDtos.VerifiableTranscriptSummaryDto result =
                verifiableTranscriptService.thuHoiBangDiem(id, request.lyDo());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/cap-lai")
    @Operation(summary = "Cấp lại bảng điểm điện tử", description = "Thu hồi bản ghi hiện tại và phát hành phiên bản mới tăng tiến (phienBan + 1).")
    public ResponseEntity<byte[]> capLai(
            @PathVariable Long id,
            @Valid @RequestBody VerifiableTranscriptDtos.ReissueTranscriptRequest request) {
        StudentTranscriptPdfService.PdfFile tep =
                verifiableTranscriptService.capLaiBangDiem(id, request.lyDo());
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.APPLICATION_PDF);
        headers.set("X-Verification-Code", tep.maXacMinh());
        headers.set("X-Verification-Url", tep.verificationUrl());
        headers.set("X-Transcript-Version", String.valueOf(tep.phienBan()));
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Tải tệp PDF bảng điểm xác thực", description = "Tải về tệp PDF của phiên bản bảng điểm điện tử đã phát hành.")
    public ResponseEntity<byte[]> taiPdf(@PathVariable Long id) {
        StudentTranscriptPdfService.PdfFile tep = verifiableTranscriptService.taiPdfDaPhatHanh(id);
        HttpHeaders headers = ExportSecuritySupport.createSafeDownloadHeaders(tep.tenTep(), MediaType.APPLICATION_PDF);
        headers.set("X-Transcript-Version", String.valueOf(tep.phienBan()));
        return new ResponseEntity<>(tep.noiDung(), headers, HttpStatus.OK);
    }
}
