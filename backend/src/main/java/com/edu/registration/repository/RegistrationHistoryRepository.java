package com.edu.registration.repository;

import com.edu.registration.entity.RegistrationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationHistoryRepository extends JpaRepository<RegistrationHistory, Long> {
    boolean existsByDangKyId(Long dangKyId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "sinhVien"})
    @Query("select h from RegistrationHistory h where h.sinhVien.id = :sinhVienId and h.hocKyId = :hocKyId")
    Page<RegistrationHistory> findLichSu(@Param("sinhVienId") Long sinhVienId,
                                   @Param("hocKyId") Long hocKyId, Pageable pageable);

    @EntityGraph(attributePaths = {"sinhVien", "sinhVien.user", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.hocKy", "lopHocPhan.giangVien", "lopHocPhan.giangVien.user"})
    @Query("select h from RegistrationHistory h where h.sinhVien.user.id = :userId order by h.thoiDiem asc, h.id asc")
    List<RegistrationHistory> findByStudentUserIdWithDetails(@Param("userId") Long userId);
}
