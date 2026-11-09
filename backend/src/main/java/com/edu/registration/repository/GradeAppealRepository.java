package com.edu.registration.repository;

import com.edu.registration.entity.GradeAppeal;
import com.edu.registration.entity.GradeAppealStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GradeAppealRepository extends JpaRepository<GradeAppeal, Long> {

    @EntityGraph(attributePaths = {
            "sinhVien", "sinhVien.user", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.hocKy", "monHoc", "dangKy", "diem", "nguoiXuLy"
    })
    Optional<GradeAppeal> findWithDetailsById(Long id);

    Optional<GradeAppeal> findByMaHoSo(String maHoSo);

    @EntityGraph(attributePaths = {
            "sinhVien", "sinhVien.user", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.hocKy", "monHoc", "dangKy", "diem", "nguoiXuLy"
    })
    List<GradeAppeal> findBySinhVienIdOrderByCreatedAtDesc(Long sinhVienId);

    @Query("""
            select a from GradeAppeal a
            where a.dangKy.id = :dangKyId
              and a.trangThai in :statuses
            """)
    List<GradeAppeal> findActiveByDangKyId(
            @Param("dangKyId") Long dangKyId,
            @Param("statuses") Collection<GradeAppealStatus> statuses);

    @EntityGraph(attributePaths = {
            "sinhVien", "sinhVien.user", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.hocKy", "monHoc", "dangKy", "diem", "nguoiXuLy"
    })
    @Query("""
            select a from GradeAppeal a
            where (:hocKyId is null or a.lopHocPhan.hocKy.id = :hocKyId)
              and (:lopHocPhanId is null or a.lopHocPhan.id = :lopHocPhanId)
              and (:monHocId is null or a.monHoc.id = :monHocId)
              and (:trangThai is null or a.trangThai = :trangThai)
              and (:maSinhVien is null or lower(a.sinhVien.ma) = lower(:maSinhVien))
            order by a.createdAt desc
            """)
    List<GradeAppeal> findWithFilters(
            @Param("hocKyId") Long hocKyId,
            @Param("lopHocPhanId") Long lopHocPhanId,
            @Param("monHocId") Long monHocId,
            @Param("trangThai") GradeAppealStatus trangThai,
            @Param("maSinhVien") String maSinhVien);
}
