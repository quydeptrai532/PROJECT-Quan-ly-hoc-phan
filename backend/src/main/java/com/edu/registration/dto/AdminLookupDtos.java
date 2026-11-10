package com.edu.registration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** DTO tra cứu lịch sử học tập/giảng dạy của quản trị. Tuyệt đối không chứa mật khẩu/token. */
public final class AdminLookupDtos {

    private AdminLookupDtos() { }

    public record PageDto<T>(List<T> items, int page, int size, long totalElements, int totalPages) { }

    public record StudentSummaryDto(Long id, String ma, String hoTen, String email, String khoa,
                                    String nganh, String lopHanhChinh, boolean dangHoatDong) { }

    public record LecturerSummaryDto(Long id, String ma, String hoTen, String email, String khoa,
                                     boolean dangHoatDong) { }

    public record StudentHistoryItemDto(Long dangKyId, Long hocKyId, String tenHocKy, String namHoc,
                                        LocalDate ngayBatDauHocKy, String maMon, String tenMon, int soTinChi,
                                        Long lopHocPhanId, String maLop, String tenGiangVien,
                                        LocalDateTime thoiGianDangKy, String trangThaiDangKy,
                                        boolean lopDaHuy, String lyDoHuyLop,
                                        Double diemQuaTrinh, Double diemCuoiKy, Double diemTongKet,
                                        String trangThaiKetQua) { }

    public record StudentHistoryDto(StudentSummaryDto sinhVien, String mucDich, PageDto<StudentHistoryItemDto> lichSu) { }

    public record LecturerClassItemDto(Long lopHocPhanId, String maLop, String maMon, String tenMon, int soTinChi,
                                       Long hocKyId, String tenHocKy, String namHoc, LocalDate ngayBatDauHocKy,
                                       int thu, int tietBatDau, int soTiet, String phong,
                                       int siSoToiDa, long soSinhVienDangHoc, String trangThai,
                                       boolean daHuy, LocalDateTime thoiGianHuy, String lyDoHuy) { }

    public record LecturerHistoryDto(LecturerSummaryDto giangVien, String mucDich, PageDto<LecturerClassItemDto> lopGiangDay) { }
}
