package com.edu.registration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class CourseClassDtos {

    public record LopHocPhanDto(Long id, String ma, Long monHocId, String maMon, String tenMon, int soTinChi,
                                Long hocKyId, String tenHocKy, Long giangVienId, String tenGiangVien,
                                String phong, int thu, int tietBatDau, int soTiet,
                                int siSoToiDa, int siSoHienTai, int conLai,
                                String trangThaiBangDiem, int phienBanCongBo,
                                LocalDateTime guiDuyetBangDiemLuc,
                                LocalDateTime traVeBangDiemLuc,
                                String lyDoTraVeBangDiem,
                                LocalDateTime duyetBangDiemLuc,
                                LocalDateTime congBoBangDiemLuc,
                                LocalDateTime khoaBangDiemLuc,
                                LocalDateTime moKhoaBangDiemLuc,
                                String lyDoMoKhoaBangDiem,
                                boolean choPhepSuaDiem,
                                boolean daKhoaBangDiem) {

        public LopHocPhanDto(Long id, String ma, Long monHocId, String maMon, String tenMon, int soTinChi,
                             Long hocKyId, String tenHocKy, Long giangVienId, String tenGiangVien,
                             String phong, int thu, int tietBatDau, int soTiet,
                             int siSoToiDa, int siSoHienTai, int conLai,
                             String trangThaiBangDiem, int phienBanCongBo,
                             LocalDateTime guiDuyetBangDiemLuc,
                             LocalDateTime traVeBangDiemLuc,
                             String lyDoTraVeBangDiem) {
            this(id, ma, monHocId, maMon, tenMon, soTinChi, hocKyId, tenHocKy, giangVienId, tenGiangVien,
                    phong, thu, tietBatDau, soTiet, siSoToiDa, siSoHienTai, conLai,
                    trangThaiBangDiem, phienBanCongBo, guiDuyetBangDiemLuc, traVeBangDiemLuc, lyDoTraVeBangDiem,
                    null, null, null, null, null,
                    "NHAP".equals(trangThaiBangDiem) || "TRA_VE".equals(trangThaiBangDiem),
                    "DA_CONG_BO".equals(trangThaiBangDiem) || "DA_KHOA".equals(trangThaiBangDiem));
        }

        public LopHocPhanDto(Long id, String ma, Long monHocId, String maMon, String tenMon, int soTinChi,
                             Long hocKyId, String tenHocKy, Long giangVienId, String tenGiangVien,
                             String phong, int thu, int tietBatDau, int soTiet,
                             int siSoToiDa, int siSoHienTai, int conLai) {
            this(id, ma, monHocId, maMon, tenMon, soTinChi, hocKyId, tenHocKy, giangVienId, tenGiangVien,
                    phong, thu, tietBatDau, soTiet, siSoToiDa, siSoHienTai, conLai,
                    "NHAP", 1, null, null, null);
        }
    }

    public record LopHocPhanRequest(
            @NotBlank(message = "Mã lớp học phần không được để trống") String ma,
            @NotNull(message = "Vui lòng chọn môn học") Long monHocId,
            @NotNull(message = "Vui lòng chọn học kỳ") Long hocKyId,
            @NotNull(message = "Vui lòng chọn giảng viên") Long giangVienId,
            @NotBlank(message = "Phòng học không được để trống") String phong,
            @Min(value = 2, message = "Thứ học phải từ 2 đến 8")
            @Max(value = 8, message = "Thứ học phải từ 2 đến 8") int thu,
            @Min(value = 1, message = "Tiết bắt đầu phải từ 1 đến 12")
            @Max(value = 12, message = "Tiết bắt đầu phải từ 1 đến 12") int tietBatDau,
            @Min(value = 1, message = "Số tiết phải từ 1 đến 6")
            @Max(value = 6, message = "Số tiết phải từ 1 đến 6") int soTiet,
            @Min(value = 1, message = "Sĩ số tối đa tối thiểu là 1") int siSoToiDa,
            Boolean xacNhanLuuDuXungDot,
            String lyDoXacNhan) {

        public LopHocPhanRequest(String ma, Long monHocId, Long hocKyId, Long giangVienId,
                                 String phong, int thu, int tietBatDau, int soTiet, int siSoToiDa) {
            this(ma, monHocId, hocKyId, giangVienId, phong, thu, tietBatDau, soTiet, siSoToiDa, false, null);
        }
    }

    public record LopHocPhanResponse(
            LopHocPhanDto lopHocPhan,
            List<String> canhBao,
            List<XungDotLichDto> danhSachXungDot,
            boolean coXungDot,
            String chinhSachEnforcement,
            boolean chinhSachDaChot,
            String ghiChuChinhSach) {

        public LopHocPhanResponse(LopHocPhanDto lopHocPhan, List<String> canhBao) {
            this(lopHocPhan, canhBao, List.of(), canhBao != null && !canhBao.isEmpty(),
                    "BLOCK_ON_CONFLICT", false, null);
        }
    }

    public record HuyLopHocPhanRequest(
            @Size(max = 200, message = "Lý do hủy lớp học phần tối đa 200 ký tự") String lyDo) {
    }

    public record HuyLopHocPhanResponse(Long lopHocPhanId, String maLop, int soDangKyBiHuy, String lyDo) {
    }

    // --- QLSV-78: Schedule conflict detection records ---

    public record XungDotLichDto(
            String loaiXungDot, // "GIANG_VIEN" | "PHONG"
            String moTa,
            Long lopLienQuanId,
            String maLopLienQuan,
            String tenMonLienQuan,
            String phong,
            int thu,
            int tietBatDau,
            int soTiet,
            int tietKetThuc,
            Long giangVienId,
            String tenGiangVien,
            int tietGiaoBatDau,
            int tietGiaoKetThuc
    ) {}

    public record KiemTraXungDotRequest(
            Long lopHocPhanId, // null khi tạo mới, ID khi chỉnh sửa (để không tự xung đột với chính nó)
            @NotNull(message = "Vui lòng chọn học kỳ") Long hocKyId,
            Long monHocId,
            @NotNull(message = "Vui lòng chọn giảng viên") Long giangVienId,
            @NotBlank(message = "Vui lòng nhập phòng học") String phong,
            @Min(value = 2, message = "Thứ học phải từ 2 đến 8")
            @Max(value = 8, message = "Thứ học phải từ 2 đến 8") int thu,
            @Min(value = 1, message = "Tiết bắt đầu phải từ 1 đến 12")
            @Max(value = 12, message = "Tiết bắt đầu phải từ 1 đến 12") int tietBatDau,
            @Min(value = 1, message = "Số tiết phải từ 1 đến 6")
            @Max(value = 6, message = "Số tiết phải từ 1 đến 6") int soTiet
    ) {}

    public record KetQuaKiemTraXungDotDto(
            boolean coXungDot,
            int soLuongXungDot,
            boolean xungDotGiangVien,
            boolean xungDotPhong,
            List<XungDotLichDto> danhSachXungDot,
            List<String> moTaCanhBao,
            String chinhSachEnforcement,
            boolean choPhepLuu,
            boolean chinhSachDaChot,
            String ghiChuChinhSach
    ) {}
}
