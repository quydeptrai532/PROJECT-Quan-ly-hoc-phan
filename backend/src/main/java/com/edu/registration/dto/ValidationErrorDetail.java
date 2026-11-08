package com.edu.registration.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        Long dangKyId,
        Long sinhVienId,
        String maSinhVien,
        String sheet,
        Integer dong,
        String cot,
        String cotExcel,
        String field,
        String giaTriNhap,
        String message
) {
    public ValidationErrorDetail(
            Long dangKyId,
            Long sinhVienId,
            String maSinhVien,
            Integer dong,
            String cot,
            String field,
            String message
    ) {
        this(dangKyId, sinhVienId, maSinhVien, null, dong, cot, null, field, null, message);
    }

    public ValidationErrorDetail(String cot, String message) {
        this(null, null, null, null, null, cot, null, cot, null, message);
    }
}
