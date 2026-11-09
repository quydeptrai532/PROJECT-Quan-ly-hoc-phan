package com.edu.registration.repository;

import com.edu.registration.entity.PrerequisiteSubject;
import com.edu.registration.entity.SubjectRelationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PrerequisiteSubjectRepository extends JpaRepository<PrerequisiteSubject, Long> {

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "monTienQuyet", "monTienQuyet.khoa", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    List<PrerequisiteSubject> findByMonHocIdOrderByIdAsc(Long monHocId);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "monTienQuyet", "monTienQuyet.khoa", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    List<PrerequisiteSubject> findByMonHocIdInOrderByIdAsc(Collection<Long> monHocIds);

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "monTienQuyet", "monTienQuyet.khoa", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    List<PrerequisiteSubject> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"monHoc", "monHoc.khoa", "monTienQuyet", "monTienQuyet.khoa", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    @Query("select q from PrerequisiteSubject q where q.id = :id")
    Optional<PrerequisiteSubject> findWithDetailsById(@Param("id") Long id);

    boolean existsByMonHocIdAndMonTienQuyetId(Long monHocId, Long monTienQuyetId);

    boolean existsByMonHocIdAndMonTienQuyetIdAndLoaiQuanHe(Long monHocId, Long monTienQuyetId, SubjectRelationType loaiQuanHe);

    boolean existsByMonHocId(Long monHocId);

    long countByMonTienQuyetId(Long monHocId);

    long countByHocKyHieuLucTuIdOrHocKyHieuLucDenId(Long hocKyTuId, Long hocKyDenId);

    boolean existsByKhoaNhapHocIgnoreCase(String khoaNhapHoc);

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    @Query("""
            select q from PrerequisiteSubject q
            where ((q.monHoc.id = :monA and q.monTienQuyet.id = :monB)
                or (q.monHoc.id = :monB and q.monTienQuyet.id = :monA))
              and q.loaiQuanHe = com.edu.registration.entity.SubjectRelationType.TUONG_DUONG
            """)
    List<PrerequisiteSubject> findQuanHeTuongDuong(@Param("monA") Long monA, @Param("monB") Long monB);

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet", "hocKyHieuLucTu", "hocKyHieuLucDen"})
    @Query("""
            select q from PrerequisiteSubject q
            where (q.monHoc.id = :monHocId or q.monTienQuyet.id = :monHocId)
              and q.loaiQuanHe = com.edu.registration.entity.SubjectRelationType.TUONG_DUONG
            """)
    List<PrerequisiteSubject> findAllQuanHeTuongDuongByMonHocId(@Param("monHocId") Long monHocId);
}
