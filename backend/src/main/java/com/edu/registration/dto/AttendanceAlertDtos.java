package com.edu.registration.dto;

import com.edu.registration.entity.AlertThresholdType;
import com.edu.registration.entity.AlertStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;
import java.util.List;

public final class AttendanceAlertDtos {

    private AttendanceAlertDtos() { }

    public record CanhBaoDiemDanhDto(
            Long id,
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String lopHanhChinh,
            AlertThresholdType loaiNguong,
            Double giaTriNguong,
            int soBuoiVang,
            int tongBuoiHopLe,
            double tyLeVang,
            AlertStatus trangThai,
            String thongDiep,
            LocalDateTime thoiGianTao,
            LocalDateTime thoiGianCapNhat
    ) { }

    public record CauHinhNguongLopRequest(
            @Min(value = 1, message = "Ngưỡng số buổi vắng phải từ 1 buổi trở lên")
            Integer nguongSoBuoiVang,

            @DecimalMin(value = "0.1", message = "Ngưỡng tỷ lệ vắng phải từ 0.1% trở lên")
            @DecimalMax(value = "100.0", message = "Ngưỡng tỷ lệ vắng tối đa 100%")
            Double nguongTyLeVang
    ) { }

    public record BaoCaoCanhBaoLopDto(
            Long lopHocPhanId,
            String maLop,
            String tenMon,
            Integer nguongSoBuoiVang,
            Double nguongTyLeVang,
            String nguonNguong,
            boolean canhBaoKichHoat,
            String ghiChuQuyChe,
            int tongSoBuoiHopLe,
            int soSinhVienBiCanhBao,
            List<CanhBaoDiemDanhDto> danhSachCanhBao
    ) { }
}
