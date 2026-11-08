package com.edu.registration.repository;

import com.edu.registration.entity.CourseRegistration;
import com.edu.registration.entity.Student;
import com.edu.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy",
            "lopHocPhan.giangVien", "sinhVien"})
    @Query("""
            select d from CourseRegistration d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.hocKy.id = :hocKyId
              and d.trangThai = :trangThai
            order by d.lopHocPhan.ma
            """)
    List<CourseRegistration> findTheoHocKy(@Param("sinhVienId") Long sinhVienId,
                                      @Param("hocKyId") Long hocKyId,
                                      @Param("trangThai") RegistrationStatus trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy",
            "lopHocPhan.giangVien", "sinhVien"})
    @Query("""
            select d from CourseRegistration d
            where d.sinhVien.id = :sinhVienId
              and d.trangThai = :trangThai
            order by d.lopHocPhan.hocKy.ngayBatDau desc, d.lopHocPhan.ma
            """)
    List<CourseRegistration> findAllActiveOfStudent(@Param("sinhVienId") Long sinhVienId,
                                               @Param("trangThai") RegistrationStatus trangThai);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "sinhVien"})
    List<CourseRegistration> findBySinhVienIdOrderByLopHocPhanHocKyNgayBatDauDescIdDesc(Long sinhVienId);

    Optional<CourseRegistration> findBySinhVienIdAndLopHocPhanId(Long sinhVienId, Long lopHocPhanId);

    Optional<CourseRegistration> findByIdAndSinhVienId(Long id, Long sinhVienId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.hocKy", "sinhVien"})
    List<CourseRegistration> findBySinhVienIdAndLopHocPhanHocKyIdOrderByIdAsc(Long sinhVienId, Long hocKyId);

    @EntityGraph(attributePaths = {"sinhVien", "sinhVien.user", "lopHocPhan", "lopHocPhan.monHoc",
            "lopHocPhan.giangVien", "lopHocPhan.hocKy"})
    @Query("""
            select d from CourseRegistration d
            where d.lopHocPhan.id = :lopHocPhanId
              and d.trangThai = :trangThai
            order by d.sinhVien.ma
            """)
    List<CourseRegistration> findByLopHocPhan(@Param("lopHocPhanId") Long lopHocPhanId,
                                         @Param("trangThai") RegistrationStatus trangThai);

    @Query("""
            select d from CourseRegistration d
            join fetch d.sinhVien s
            left join fetch s.lopHanhChinh
            where d.lopHocPhan.id = :lopHocPhanId
              and d.trangThai = :trangThai
            order by s.ma
            """)
    List<CourseRegistration> findDanhSachXuat(@Param("lopHocPhanId") Long lopHocPhanId,
                                         @Param("trangThai") RegistrationStatus trangThai);

    long countByLopHocPhanIdAndTrangThai(Long lopHocPhanId, RegistrationStatus trangThai);

    boolean existsByLopHocPhanId(Long lopHocPhanId);

    @Query("""
            select coalesce(sum(d.lopHocPhan.monHoc.soTinChi), 0) from CourseRegistration d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.hocKy.id = :hocKyId
              and d.trangThai = :trangThai
            """)
    long tinhTongTinChi(@Param("sinhVienId") Long sinhVienId,
                        @Param("hocKyId") Long hocKyId,
                        @Param("trangThai") RegistrationStatus trangThai);

    long countByTrangThai(RegistrationStatus trangThai);

    @Query("""
            select count(d) > 0 from CourseRegistration d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.giangVien.id = :giangVienId
              and d.trangThai = :trangThai
            """)
    boolean existsBySinhVienIdAndGiangVienIdAndTrangThai(@Param("sinhVienId") Long sinhVienId,
                                                         @Param("giangVienId") Long giangVienId,
                                                         @Param("trangThai") RegistrationStatus trangThai);

    @Query("""
            select count(d) > 0 from CourseRegistration d
            where d.sinhVien.id = :sinhVienId
              and d.lopHocPhan.id = :lopHocPhanId
              and d.lopHocPhan.giangVien.id = :giangVienId
              and d.trangThai = :trangThai
            """)
    boolean existsBySinhVienIdAndLopHocPhanIdAndGiangVienIdAndTrangThai(@Param("sinhVienId") Long sinhVienId,
                                                                        @Param("lopHocPhanId") Long lopHocPhanId,
                                                                        @Param("giangVienId") Long giangVienId,
                                                                        @Param("trangThai") RegistrationStatus trangThai);

    @Query("""
            select distinct s from CourseRegistration d
            join d.sinhVien s
            left join fetch s.khoa
            left join fetch s.nganh
            left join fetch s.lopHanhChinh
            left join fetch s.chuongTrinh
            where d.lopHocPhan.giangVien.id = :giangVienId
              and d.trangThai = :trangThai
              and (:lopHocPhanId is null or d.lopHocPhan.id = :lopHocPhanId)
            order by s.ma
            """)
    List<Student> findAssignedStudentsOfLecturer(@Param("giangVienId") Long giangVienId,
                                                 @Param("lopHocPhanId") Long lopHocPhanId,
                                                 @Param("trangThai") RegistrationStatus trangThai);
}
