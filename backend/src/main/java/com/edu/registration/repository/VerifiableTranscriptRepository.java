package com.edu.registration.repository;

import com.edu.registration.entity.VerifiableTranscript;
import com.edu.registration.entity.VerifiableTranscriptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerifiableTranscriptRepository extends JpaRepository<VerifiableTranscript, Long> {

    Optional<VerifiableTranscript> findByMaXacMinhHash(String maXacMinhHash);

    List<VerifiableTranscript> findBySinhVienIdOrderByThoiDiemPhatHanhDesc(Long sinhVienId);

    List<VerifiableTranscript> findByMaSinhVienOrderByThoiDiemPhatHanhDesc(String maSinhVien);

    Optional<VerifiableTranscript> findFirstBySinhVienIdAndHocKyIdAndTrangThaiOrderByPhienBanDesc(
            Long sinhVienId, Long hocKyId, VerifiableTranscriptStatus trangThai);

    Optional<VerifiableTranscript> findFirstBySinhVienIdAndHocKyIsNullAndTrangThaiOrderByPhienBanDesc(
            Long sinhVienId, VerifiableTranscriptStatus trangThai);

    Optional<VerifiableTranscript> findFirstBySinhVienIdAndHocKyIdOrderByPhienBanDesc(
            Long sinhVienId, Long hocKyId);

    Optional<VerifiableTranscript> findFirstBySinhVienIdAndHocKyIsNullOrderByPhienBanDesc(
            Long sinhVienId);

    @Query("SELECT vt FROM VerifiableTranscript vt WHERE " +
            "(:maSinhVien IS NULL OR LOWER(vt.maSinhVien) LIKE LOWER(CONCAT('%', :maSinhVien, '%')) OR (vt.sinhVien.user IS NOT NULL AND LOWER(vt.sinhVien.user.username) LIKE LOWER(CONCAT('%', :maSinhVien, '%')))) AND " +
            "(:trangThai IS NULL OR vt.trangThai = :trangThai) AND " +
            "(:hocKyId IS NULL OR (vt.hocKy IS NOT NULL AND vt.hocKy.id = :hocKyId)) " +
            "ORDER BY vt.thoiDiemPhatHanh DESC")
    Page<VerifiableTranscript> search(
            @Param("maSinhVien") String maSinhVien,
            @Param("trangThai") VerifiableTranscriptStatus trangThai,
            @Param("hocKyId") Long hocKyId,
            Pageable pageable);
}
