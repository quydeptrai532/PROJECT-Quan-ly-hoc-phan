package com.edu.registration.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
                                boolean daKhoaBangDiem,
                                boolean daHuy,
                                LocalDateTime thoiGianHuy,
                                String lyDoHuy,
                                Long nguoiHuyId,
                                String nguoiHuyUsername) {

        public LopHocPhanDto(Long id, String ma, Long monHocId, String maMon, String tenMon, int soTinChi,
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
            this(id, ma, monHocId, maMon, tenMon, soTinChi, hocKyId, tenHocKy, giangVienId, tenGiangVien,
                    phong, thu, tietBatDau, soTiet, siSoToiDa, siSoHienTai, conLai,
                    trangThaiBangDiem, phienBanCongBo, guiDuyetBangDiemLuc, traVeBangDiemLuc, lyDoTraVeBangDiem,
                    duyetBangDiemLuc, congBoBangDiemLuc, khoaBangDiemLuc, moKhoaBangDiemLuc, lyDoMoKhoaBangDiem,
                    choPhepSuaDiem, daKhoaBangDiem, false, null, null, null, null);
        }

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

    // --- QLSV-79: Class cancellation impact & transfer records ---

    public record SinhVienLopDto(
            Long sinhVienId,
            String maSinhVien,
            String hoTen,
            String lopHanhChinh,
            boolean coDiem,
            Double diemQuaTrinh,
            Double diemCuoiKy,
            Double diemTongKet,
            LocalDateTime thoiGianDangKy
    ) {}

    public record LopDichKhaDiDto(
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            String tenGiangVien,
            String phong,
            int thu,
            int tietBatDau,
            int soTiet,
            int siSoHienTai,
            int siSoToiDa,
            int conLai,
            String loaiQuanHeMon // "CUNG_MON" hoặc "TUONG_DUONG"
    ) {}

    public record KhaoSatHuyLopDto(
            Long lopHocPhanId,
            String maLop,
            Long monHocId,
            String maMon,
            String tenMon,
            int soTinChi,
            Long giangVienId,
            String tenGiangVien,
            Long hocKyId,
            String tenHocKy,
            int siSoHienTai,
            int siSoToiDa,
            boolean daHuy,
            boolean coDiem,
            int soSinhVienCoDiem,
            String trangThaiBangDiem,
            boolean choPhepPhaoHuy,
            String lyDoKhongChoPhep,
            List<SinhVienLopDto> danhSachSinhVien,
            List<LopDichKhaDiDto> danhSachLopDichKhaDi
    ) {}

    public record KiemTraChuyenLopRequest(
            @NotNull(message = "Vui lòng chọn lớp đích") Long lopDichId,
            List<Long> sinhVienIds
    ) {}

    public record KiemTraChuyenSinhVienItemDto(
            Long sinhVienId,
            String maSinhVien,
            String hoTen,
            boolean duDieuKien,
            boolean trungLich,
            String moTaTrungLich,
            boolean duTienQuyet,
            String moTaTienQuyet,
            boolean coDiemTaiLopNguon,
            String moTaDiem,
            String thongDiepLoi
    ) {}

    public record KetQuaKiemTraChuyenLopDto(
            Long lopNguonId,
            String maLopNguon,
            Long lopDichId,
            String maLopDich,
            String loaiQuanHeMon,
            int soChoConLaiLopDich,
            int tongSinhVienKiemTra,
            int soSinhVienDuDieuKien,
            boolean choPhepChuyenTatCa,
            List<KiemTraChuyenSinhVienItemDto> chiTietSinhVien
    ) {}

    public record ChuyenSinhVienRequest(
            @NotNull(message = "Vui lòng chọn sinh viên cần chuyển") Long sinhVienId,
            @NotNull(message = "Vui lòng chọn lớp đích") Long lopDichId,
            @Size(max = 500, message = "Lý do tối đa 500 ký tự") String lyDo
    ) {}

    public record ChuyenSinhVienResponse(
            Long transferId,
            Long lopNguonId,
            String maLopNguon,
            Long lopDichId,
            String maLopDich,
            Long sinhVienId,
            String maSinhVien,
            String hoTen,
            LocalDateTime thoiGianChuyen,
            String lyDo,
            String loaiQuanHeMon
    ) {}

    public record ChuyenHangLoatRequest(
            @NotNull(message = "Vui lòng chọn lớp đích") Long lopDichId,
            @NotEmpty(message = "Danh sách sinh viên không được để trống") List<Long> sinhVienIds,
            @Size(max = 500, message = "Lý do tối đa 500 ký tự") String lyDo
    ) {}

    public record KetQuaChuyenHangLoatDto(
            Long lopNguonId,
            String maLopNguon,
            Long lopDichId,
            String maLopDich,
            int soLuongYeuCau,
            int soLuongThanhCong,
            int soLuongThatBai,
            List<ChuyenSinhVienResponse> danhSachThanhCong,
            List<String> danhSachLoi
    ) {}

    public record LichSuChuyenLopDto(
            Long id,
            Long lopNguonId,
            String maLopNguon,
            String tenMonNguon,
            Long lopDichId,
            String maLopDich,
            String tenMonDich,
            Long sinhVienId,
            String maSinhVien,
            String hoTenSinhVien,
            Long nguoiThucHienId,
            String nguoiThucHienTen,
            LocalDateTime thoiGianChuyen,
            String lyDo,
            String loaiQuanHeMon,
            String ghiChu
    ) {}
}
