package com.edu.registration.repository;

import com.edu.registration.entity.CourseClass;
import com.edu.registration.entity.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    @Query("""
            select l from CourseClass l
            where (:hocKyId is null or l.hocKy.id = :hocKyId)
              and (:khoaId is null or l.monHoc.khoa.id = :khoaId)
              and (:monHocId is null or l.monHoc.id = :monHocId)
              and (:giangVienId is null or l.giangVien.id = :giangVienId)
              and (:lopHocPhanId is null or l.id = :lopHocPhanId)
            order by l.hocKy.ngayBatDau desc, l.ma asc
            """)
    List<CourseClass> findForGradeAnalytics(@Param("hocKyId") Long hocKyId,
                                            @Param("khoaId") Long khoaId,
                                            @Param("monHocId") Long monHocId,
                                            @Param("giangVienId") Long giangVienId,
                                            @Param("lopHocPhanId") Long lopHocPhanId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("""
            select l from CourseClass l
            where (:hocKyId is null or l.hocKy.id = :hocKyId)
              and (:khoaId is null or l.monHoc.khoa.id = :khoaId)
              and (:monHocId is null or l.monHoc.id = :monHocId)
              and (:lopHocPhanId is null or l.id = :lopHocPhanId)
            order by l.hocKy.ngayBatDau desc, l.ma asc
            """)
    List<CourseClass> findForAdminAnalytics(@Param("hocKyId") Long hocKyId,
                                            @Param("khoaId") Long khoaId,
                                            @Param("monHocId") Long monHocId,
                                            @Param("lopHocPhanId") Long lopHocPhanId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.id = :id")
    Optional<CourseClass> findWithDetailsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.id = :id")
    Optional<CourseClass> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"monHoc", "hocKy", "giangVien"})
    @Query("""
        select l from CourseClass l
        where l.giangVien.id = :giangVienId
          and (:hocKyId is null or l.hocKy.id = :hocKyId)
        """)
    List<CourseClass> findForAdminLecturerHistory(@Param("giangVienId") Long giangVienId,
                                                  @Param("hocKyId") Long hocKyId);

    boolean existsByMa(String ma);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "hocKy", "giangVien"})
    @Query("select l from CourseClass l where l.hocKy.dangKyTu <= :ngay and l.hocKy.dangKyDen >= :ngay and l.hocKy.ngayKetThuc >= :ngay and l.siSoHienTai < l.siSoToiDa")
    List<CourseClass> findOpenForRegistration(@Param("ngay") LocalDate ngay);

    long countByHocKyId(Long hocKyId);

    @EntityGraph(attributePaths = {"monHoc", "hocKy"})
    List<CourseClass> findByMonHocIdOrderByMaAsc(Long monHocId);

    long countByMonHocId(Long monHocId);

    boolean existsByPhongIgnoreCase(String phong);

    boolean existsByPhongHocId(Long phongHocId);

    long countByPhongIgnoreCase(String phong);

    long countByPhongHocId(Long phongHocId);
}
