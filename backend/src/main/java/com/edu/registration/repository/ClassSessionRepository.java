package com.edu.registration.repository;

import com.edu.registration.entity.ClassSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "nguoiTao", "nguoiCapNhat", "nguoiKhoa"})
    List<ClassSession> findByLopHocPhanIdOrderByNgayHocAscCaHocAsc(Long lopHocPhanId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "nguoiTao", "nguoiCapNhat", "nguoiKhoa"})
    Optional<ClassSession> findWithDetailsById(Long id);

    Optional<ClassSession> findByLopHocPhanIdAndNgayHocAndCaHoc(Long lopHocPhanId, LocalDate ngayHoc, Integer caHoc);

    boolean existsByLopHocPhanIdAndNgayHocAndCaHoc(Long lopHocPhanId, LocalDate ngayHoc, Integer caHoc);

    @Query("""
            select b from ClassSession b
            where b.lopHocPhan.hocKy.id = :hocKyId
            order by b.ngayHoc asc, b.caHoc asc
            """)
    List<ClassSession> findByHocKyId(@Param("hocKyId") Long hocKyId);

    long countByLopHocPhanId(Long lopHocPhanId);
}
