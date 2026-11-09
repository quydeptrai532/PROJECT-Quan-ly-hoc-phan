package com.edu.registration.repository;

import com.edu.registration.entity.AttendanceAlert;
import com.edu.registration.entity.AlertThresholdType;
import com.edu.registration.entity.AlertStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceAlertRepository extends JpaRepository<AttendanceAlert, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    Optional<AttendanceAlert> findBySinhVienIdAndLopHocPhanIdAndLoaiNguong(
            Long sinhVienId, Long lopHocPhanId, AlertThresholdType loaiNguong);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    List<AttendanceAlert> findByLopHocPhanIdOrderByThoiGianCapNhatDescThoiGianTaoDesc(Long lopHocPhanId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    List<AttendanceAlert> findByLopHocPhanIdAndTrangThaiOrderByThoiGianCapNhatDesc(
            Long lopHocPhanId, AlertStatus trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    @Query("""
            select c from AttendanceAlert c
            where c.lopHocPhan.giangVien.id = :giangVienId
              and (:trangThai is null or c.trangThai = :trangThai)
            order by coalesce(c.thoiGianCapNhat, c.thoiGianTao) desc
            """)
    List<AttendanceAlert> findByGiangVienIdAndTrangThai(
            @Param("giangVienId") Long giangVienId,
            @Param("trangThai") AlertStatus trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "sinhVien", "sinhVien.user", "sinhVien.lopHanhChinh"})
    @Query("""
            select c from AttendanceAlert c
            where c.sinhVien.id = :sinhVienId
              and (:trangThai is null or c.trangThai = :trangThai)
            order by coalesce(c.thoiGianCapNhat, c.thoiGianTao) desc
            """)
    List<AttendanceAlert> findBySinhVienIdAndTrangThai(
            @Param("sinhVienId") Long sinhVienId,
            @Param("trangThai") AlertStatus trangThai);

    long countByLopHocPhanIdAndTrangThai(Long lopHocPhanId, AlertStatus trangThai);

    void deleteByLopHocPhanId(Long lopHocPhanId);
}
