package com.edu.registration.repository;

import com.edu.registration.entity.CourseClassAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseClassAnnouncementRepository extends JpaRepository<CourseClassAnnouncement, Long> {

    @Query("SELECT tb FROM CourseClassAnnouncement tb JOIN FETCH tb.lopHocPhan lop JOIN FETCH lop.monHoc JOIN FETCH tb.giangVien WHERE lop.id = :lopHocPhanId ORDER BY tb.thoiGianTao DESC, tb.id DESC")
    List<CourseClassAnnouncement> findByLopHocPhanIdOrderByThoiGianTaoDesc(@Param("lopHocPhanId") Long lopHocPhanId);

    @Query("SELECT tb FROM CourseClassAnnouncement tb JOIN FETCH tb.lopHocPhan lop JOIN FETCH lop.monHoc JOIN FETCH tb.giangVien WHERE tb.giangVien.id = :giangVienId ORDER BY tb.thoiGianTao DESC, tb.id DESC")
    List<CourseClassAnnouncement> findByGiangVienIdOrderByThoiGianTaoDesc(@Param("giangVienId") Long giangVienId);

    Optional<CourseClassAnnouncement> findByLopHocPhanIdAndIdempotencyKey(Long lopHocPhanId, String idempotencyKey);
}
