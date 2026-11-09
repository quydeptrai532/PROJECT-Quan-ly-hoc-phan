package com.edu.registration.repository;

import com.edu.registration.entity.AcademicAlert;
import com.edu.registration.entity.AcademicAlertRuleType;
import com.edu.registration.entity.AcademicAlertSeverity;
import com.edu.registration.entity.AcademicAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicAlertRepository extends JpaRepository<AcademicAlert, Long> {

    Optional<AcademicAlert> findByKhoaChongTrung(String khoaChongTrung);

    List<AcademicAlert> findBySinhVienId(Long sinhVienId);

    List<AcademicAlert> findBySinhVienIdAndActiveTrue(Long sinhVienId);

    @Query("SELECT a FROM AcademicAlert a WHERE a.sinhVien.id = :sinhVienId AND a.choPhepSinhVienXem = true " +
            "ORDER BY a.active DESC, a.ngayTao DESC")
    Page<AcademicAlert> findForStudent(@Param("sinhVienId") Long sinhVienId, Pageable pageable);

    @Query("SELECT a FROM AcademicAlert a WHERE a.sinhVien.id = :sinhVienId AND a.choPhepSinhVienXem = true " +
            "ORDER BY a.active DESC, a.ngayTao DESC")
    List<AcademicAlert> findForStudentAll(@Param("sinhVienId") Long sinhVienId);

    @Query("SELECT a FROM AcademicAlert a WHERE a.lopHocPhanId IN :lopIds " +
            "ORDER BY a.active DESC, a.capNhatLuc DESC")
    Page<AcademicAlert> findByLopHocPhanIdIn(@Param("lopIds") Collection<Long> lopIds, Pageable pageable);

    @Query("SELECT a FROM AcademicAlert a WHERE a.sinhVien.id IN :studentIds " +
            "ORDER BY a.active DESC, a.capNhatLuc DESC")
    Page<AcademicAlert> findBySinhVienIdIn(@Param("studentIds") Collection<Long> studentIds, Pageable pageable);

    @Query("SELECT a FROM AcademicAlert a " +
            "WHERE (:trangThai IS NULL OR a.trangThai = :trangThai) " +
            "AND (:loaiLuat IS NULL OR a.loaiLuat = :loaiLuat) " +
            "AND (:mucDo IS NULL OR a.mucDo = :mucDo) " +
            "AND (:active IS NULL OR a.active = :active) " +
            "AND (:keyword IS NULL OR LOWER(a.sinhVien.ma) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(a.sinhVien.hoTen) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(a.tenLuat) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "     OR LOWER(a.lyDo) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY a.active DESC, a.capNhatLuc DESC")
    Page<AcademicAlert> filterForAdmin(
            @Param("trangThai") AcademicAlertStatus trangThai,
            @Param("loaiLuat") AcademicAlertRuleType loaiLuat,
            @Param("mucDo") AcademicAlertSeverity mucDo,
            @Param("active") Boolean active,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("SELECT a FROM AcademicAlert a WHERE a.active = true")
    List<AcademicAlert> findAllActive();
}
