package com.edu.registration.repository;

import com.edu.registration.entity.Grade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    Optional<Grade> findByDangKyId(Long dangKyId);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.sinhVien.user"})
    List<Grade> findByDangKyIdIn(Collection<Long> dangKyIds);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.lopHocPhan", "dangKy.lopHocPhan.monHoc", "dangKy.lopHocPhan.hocKy"})
    Optional<Grade> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.lopHocPhan", "dangKy.lopHocPhan.monHoc", "dangKy.lopHocPhan.hocKy"})
    @Query("select d from Grade d where d.dangKy.sinhVien.id = :sinhVienId order by d.dangKy.lopHocPhan.hocKy.ngayBatDau desc, d.id desc")
    List<Grade> findBySinhVienIdWithDetails(@Param("sinhVienId") Long sinhVienId);

    @EntityGraph(attributePaths = {"dangKy", "dangKy.sinhVien", "dangKy.sinhVien.user", "dangKy.lopHocPhan", "dangKy.lopHocPhan.monHoc", "dangKy.lopHocPhan.hocKy"})
    @Query("""
            select d from Grade d
            where d.dangKy.sinhVien.user.id = :userId
              and d.dangKy.trangThai = com.edu.registration.entity.RegistrationStatus.DA_DANG_KY
              and d.dangKy.lopHocPhan.trangThaiBangDiem = com.edu.registration.entity.GradeSheetStatus.DA_CONG_BO
              and d.dangKy.lopHocPhan.congBoBangDiemLuc is not null
              and d.diemTongKet is not null
            order by d.dangKy.lopHocPhan.congBoBangDiemLuc asc, d.id asc
            """)
    List<Grade> findPublishedGradesForStudentUser(@Param("userId") Long userId);

    @Query("""
            select distinct d.dangKy.lopHocPhan.monHoc.id from Grade d
            where d.dangKy.sinhVien.id = :sinhVienId
              and d.diemTongKet >= :diemDat
              and (d.dangKy.lopHocPhan.trangThaiBangDiem is null
                   or d.dangKy.lopHocPhan.trangThaiBangDiem = com.edu.registration.entity.GradeSheetStatus.DA_CONG_BO)
            """)
    List<Long> findMonDaDat(@Param("sinhVienId") Long sinhVienId, @Param("diemDat") double diemDat);
}
