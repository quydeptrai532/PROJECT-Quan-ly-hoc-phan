package com.edu.registration.repository;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface CourseClassRepository extends JpaRepository<CourseClass, Long> {

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<CourseClass> findByHocKyIdOrderByMaAsc(Long hocKyId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy"})
    List<CourseClass> findByHocKyIdAndMonHocKhoaIdOrderByMaAsc(Long hocKyId, Long khoaId);

    interface DangKyTheoLop {
        Long getLopHocPhanId();
        long getSoDangKy();
    }

    @Query("""
            select l.id as lopHocPhanId, count(d.id) as soDangKy
            from CourseClass l
            left join CourseRegistration d on d.lopHocPhan = l and d.trangThai = :trangThai
            where l.hocKy.id = :hocKyId
              and (:khoaId is null or l.monHoc.khoa.id = :khoaId)
            group by l.id
            """)
    List<DangKyTheoLop> countDangKyHienHanhTheoLop(@Param("hocKyId") Long hocKyId,
                                                    @Param("khoaId") Long khoaId,
                                                    @Param("trangThai") RegistrationStatus trangThai);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<CourseClass> findByGiangVienIdAndHocKyIdOrderByMaAsc(Long giangVienId, Long hocKyId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    List<CourseClass> findByGiangVienIdOrderByMaAsc(Long giangVienId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.id = :id")
    Optional<CourseClass> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.id = :id")
    Optional<CourseClass> findByIdForUpdate(@Param("id") Long id);

    boolean existsByMa(String ma);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.hocKy.dangKyTu <= :ngay and l.hocKy.dangKyDen >= :ngay and l.hocKy.ngayKetThuc >= :ngay and l.siSoHienTai < l.siSoToiDa")
    List<CourseClass> findOpenForRegistration(@Param("ngay") LocalDate ngay);

    long countByHocKyId(Long hocKyId);
}
