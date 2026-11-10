package com.edu.registration.dto;

import com.edu.registration.entity.AcademicAlertRuleType;
import com.edu.registration.entity.AcademicAlertSeverity;
import com.edu.registration.entity.AcademicAlertStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class AcademicAlertDtos {

    private AcademicAlertDtos() {}

    public record AcademicAlertDto(
            Long id,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            String lopHanhChinh,
            Long lopHocPhanId,
            String maLopHocPhan,
            String tenMonHoc,
            AcademicAlertRuleType loaiLuat,
            String tenLoaiLuat,
            String maLuat,
            String tenLuat,
            AcademicAlertSeverity mucDo,
            String tenMucDo,
            String lyDo,
            String congThucGiaiThich,
            String duLieuDanChung,
            String nguCanh,
            AcademicAlertStatus trangThai,
            String tenTrangThai,
            boolean choPhepSinhVienXem,
            boolean active,
            LocalDateTime ngayTao,
            LocalDateTime capNhatLuc,
            LocalDateTime ngayXuLy,
            String nguoiXuLy,
            String ghiChuXuLy,
            String nguyenNhanGiaiToa
    ) {}

    public record CapNhatTrangThaiRequest(
            AcademicAlertStatus trangThai,
            String ghiChu
    ) {}

    public record ScanAlertsResultDto(
            int soSinhVienQuet,
            int soCanhBaoTaoMoi,
            int soCanhBaoCapNhat,
            int soCanhBaoTuDongGiaiToa,
            long thoiGianThucThiMs,
            String thongBao
    ) {}

    public record AlertRuleInfo(
            AcademicAlertRuleType loaiLuat,
            String maLuat,
            String tenLuat,
            String moTa,
            String mucDoMacDinh,
            boolean kichHoat,
            boolean choPhepSinhVienXem,
            String congThucTinh,
            String nguonDuLieu,
            String ghiChuQuyChe,
            boolean daChotQuyChe
    ) {}

    public record EarlyWarningConfigDto(
            boolean enabled,
            boolean officialRegulation,
            String policyNote,
            List<AlertRuleInfo> danhSachLuat,
            List<String> giaTriChoXacNhan
    ) {}

    public record StudentAlertSummaryDto(
            long tongSoCanhBao,
            long soCanhBaoHoatDong,
            List<AcademicAlertDto> danhSachCanhBao
    ) {}
}
