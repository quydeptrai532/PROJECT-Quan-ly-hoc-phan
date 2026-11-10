package com.edu.registration.dto;

import com.edu.registration.entity.ClassroomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class ClassroomDtos {

    public record PhongHocDto(
            Long id,
            String ma,
            String ten,
            String toaNha,
            Integer tang,
            String viTri,
            int sucChua,
            String loaiPhong,
            String loaiPhongHienThi,
            String thietBi,
            ClassroomStatus trangThai,
            String trangThaiHienThi,
            String ghiChu,
            LocalDateTime ngayTao,
            LocalDateTime ngayCapNhat,
            String nguoiCapNhatUsername,
            long soLopDaGanLich
    ) {}

    public record PhongHocRequest(
            @NotBlank(message = "Mã phòng không được để trống")
            @Size(max = 30, message = "Mã phòng tối đa 30 ký tự")
            String ma,

            @NotBlank(message = "Tên phòng không được để trống")
            @Size(max = 150, message = "Tên phòng tối đa 150 ký tự")
            String ten,

            @Size(max = 50, message = "Tòa nhà tối đa 50 ký tự")
            String toaNha,

            Integer tang,

            @Size(max = 150, message = "Vị trí tối đa 150 ký tự")
            String viTri,

            @NotNull(message = "Sức chứa không được để trống")
            @Min(value = 1, message = "Sức chứa phải là số nguyên dương lớn hơn 0")
            Integer sucChua,

            String loaiPhong,

            String thietBi,

            ClassroomStatus trangThai,

            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
            String ghiChu
    ) {}

    public record DoiTrangThaiRequest(
            @NotNull(message = "Trạng thái không được để trống")
            ClassroomStatus trangThai,

            @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
            String ghiChu
    ) {}

    public record TaxonomyItemDto(String ma, String ten, String moTa) {}

    public record TaxonomyPhongHocDto(
            List<TaxonomyItemDto> danhSachLoaiPhong,
            List<TaxonomyItemDto> danhSachThietBiPhoBien,
            List<TaxonomyItemDto> danhSachTrangThai
    ) {}
}
