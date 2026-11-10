package com.edu.registration.repository;

import com.edu.registration.entity.RegistrationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    List<RegistrationPeriod> findByHocKyIdOrderByThuTuUuTienAscThoiGianBatDauAsc(Long hocKyId);

    List<RegistrationPeriod> findByHocKyIdAndDangHieuLucTrueOrderByThuTuUuTienAscThoiGianBatDauAsc(Long hocKyId);

    boolean existsByHocKyId(Long hocKyId);

    boolean existsByHocKyIdAndMaDotIgnoreCase(Long hocKyId, String maDot);

    boolean existsByKhoaHocId(Long khoaHocId);

    boolean existsByNganhId(Long nganhId);

    boolean existsByLopHanhChinhId(Long lopHanhChinhId);

    @Query("SELECT p FROM RegistrationPeriod p WHERE (:hocKyId IS NULL OR p.hocKy.id = :hocKyId) " +
           "AND (:dangHieuLuc IS NULL OR p.dangHieuLuc = :dangHieuLuc) " +
           "AND (:loaiDot IS NULL OR UPPER(p.loaiDot) = UPPER(:loaiDot)) " +
           "ORDER BY p.hocKy.id DESC, p.thuTuUuTien ASC, p.thoiGianBatDau ASC")
    List<RegistrationPeriod> search(@Param("hocKyId") Long hocKyId,
                                    @Param("dangHieuLuc") Boolean dangHieuLuc,
                                    @Param("loaiDot") String loaiDot);
}
