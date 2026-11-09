package com.edu.registration.repository;

import com.edu.registration.entity.ClassTransferHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassTransferHistoryRepository extends JpaRepository<ClassTransferHistory, Long> {

    @EntityGraph(attributePaths = {
            "lopNguon", "lopNguon.monHoc", "lopNguon.hocKy", "lopNguon.giangVien",
            "lopDich", "lopDich.monHoc", "lopDich.hocKy", "lopDich.giangVien",
            "sinhVien", "sinhVien.user", "nguoiThucHien", "monHocNguon", "monHocDich"
    })
    @Query("""
            select h from ClassTransferHistory h
            where h.lopNguon.id = :lopId or h.lopDich.id = :lopId
            order by h.thoiGianChuyen desc, h.id desc
            """)
    List<ClassTransferHistory> findByLopId(@Param("lopId") Long lopId);

    @EntityGraph(attributePaths = {
            "lopNguon", "lopNguon.monHoc", "lopNguon.hocKy", "lopNguon.giangVien",
            "lopDich", "lopDich.monHoc", "lopDich.hocKy", "lopDich.giangVien",
            "sinhVien", "sinhVien.user", "nguoiThucHien", "monHocNguon", "monHocDich"
    })
    @Query("""
            select h from ClassTransferHistory h
            where (:hocKyId is null or h.lopNguon.hocKy.id = :hocKyId or h.lopDich.hocKy.id = :hocKyId)
            order by h.thoiGianChuyen desc, h.id desc
            """)
    List<ClassTransferHistory> findByHocKyId(@Param("hocKyId") Long hocKyId);

    @EntityGraph(attributePaths = {
            "lopNguon", "lopNguon.monHoc", "lopNguon.hocKy", "lopNguon.giangVien",
            "lopDich", "lopDich.monHoc", "lopDich.hocKy", "lopDich.giangVien",
            "sinhVien", "sinhVien.user", "nguoiThucHien", "monHocNguon", "monHocDich"
    })
    List<ClassTransferHistory> findBySinhVienIdOrderByThoiGianChuyenDesc(Long sinhVienId);
}
