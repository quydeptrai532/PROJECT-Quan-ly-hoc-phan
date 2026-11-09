package com.edu.registration.controller;

import com.edu.registration.dto.VerifiableTranscriptDtos;
import com.edu.registration.service.VerifiableTranscriptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Điểm cuối xác thực công khai bảng điểm điện tử (QLSV-99 / TASK-OP-12).
 * Cho phép nhà tuyển dụng, cơ quan và bên thứ ba đối soát tính hợp lệ của bảng điểm qua mã xác minh / quét mã QR.
 */
@RestController
@RequestMapping("/api/public/bang-diem/xac-thuc")
@Tag(name = "Xác thực Bảng điểm Công khai", description = "Điểm cuối công khai xác thực tính hợp lệ của bảng điểm điện tử qua mã hoặc QR (QLSV-99 / TASK-OP-12)")
public class PublicTranscriptVerificationController {

    private final VerifiableTranscriptService verifiableTranscriptService;

    public PublicTranscriptVerificationController(VerifiableTranscriptService verifiableTranscriptService) {
        this.verifiableTranscriptService = verifiableTranscriptService;
    }

    @GetMapping
    @Operation(summary = "Xác thực bảng điểm qua query parameter", description = "Kiểm tra tính hợp lệ và toàn vẹn của bảng điểm dựa trên mã xác minh.")
    public ResponseEntity<VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto> xacThucQuery(
            @Parameter(description = "Mã xác minh bảng điểm in trên văn bản hoặc quét từ QR")
            @RequestParam(required = false) String code) {
        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result =
                verifiableTranscriptService.xacThucCongKhai(code);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Xác thực bảng điểm qua path parameter", description = "Kiểm tra tính hợp lệ và toàn vẹn của bảng điểm dựa trên mã xác minh trên đường dẫn.")
    public ResponseEntity<VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto> xacThucPath(
            @Parameter(description = "Mã xác minh bảng điểm in trên văn bản hoặc quét từ QR")
            @PathVariable String code) {
        VerifiableTranscriptDtos.VerifiableTranscriptPublicVerifyDto result =
                verifiableTranscriptService.xacThucCongKhai(code);
        return ResponseEntity.ok(result);
    }
}
