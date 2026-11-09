package com.edu.registration.repository;

import com.edu.registration.entity.ClassAbsenceNotice;
import com.edu.registration.entity.RegistrationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClassAbsenceNoticeRepository extends JpaRepository<ClassAbsenceNotice, Long> {

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "giangVien"})
    @Query("""
            select t from ClassAbsenceNotice t
            where t.lopHocPhan.giangVien.id = :giangVienId
              and t.lopHocPhan.hocKy.id = :hocKyId
            order by t.ngayNghi desc, t.lopHocPhan.ma
            """)
    List<ClassAbsenceNotice> cuaGiangVien(@Param("giangVienId") Long giangVienId,
                                    @Param("hocKyId") Long hocKyId);

    @EntityGraph(attributePaths = {"lopHocPhan", "lopHocPhan.monHoc", "lopHocPhan.giangVien", "giangVien"})
    @Query("""
            select t from ClassAbsenceNotice t
            where t.lopHocPhan.hocKy.id = :hocKyId
              and exists (
                select 1 from CourseRegistration d
                where d.lopHocPhan = t.lopHocPhan
                  and d.sinhVien.id = :sinhVienId
                  and d.trangThai = :trangThai
              )
            order by t.ngayNghi desc, t.lopHocPhan.ma
            """)
    List<ClassAbsenceNotice> cuaSinhVien(@Param("sinhVienId") Long sinhVienId,
                                   @Param("hocKyId") Long hocKyId,
                                   @Param("trangThai") RegistrationStatus trangThai);

    boolean existsByLopHocPhanIdAndNgayNghi(Long lopHocPhanId, LocalDate ngayNghi);

    Optional<ClassAbsenceNotice> findByIdAndLopHocPhanGiangVienId(Long id, Long giangVienId);

    List<ClassAbsenceNotice> findByLopHocPhanId(Long lopHocPhanId);

    void deleteByLopHocPhanId(Long lopHocPhanId);
}
