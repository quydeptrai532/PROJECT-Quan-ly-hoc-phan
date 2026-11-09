package com.edu.registration.repository;

import com.edu.registration.entity.SupportRequest;
import com.edu.registration.entity.SupportRequestPriority;
import com.edu.registration.entity.SupportRequestStatus;
import com.edu.registration.entity.SupportRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    @EntityGraph(attributePaths = {
            "sinhVien", "sinhVien.user", "nguoiTao", "nguoiXuLy",
            "monHoc", "hocKy", "diem", "diem.dangKy", "diem.dangKy.lopHocPhan",
            "diem.dangKy.lopHocPhan.monHoc", "diem.dangKy.lopHocPhan.hocKy"
    })
    Optional<SupportRequest> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {
            "sinhVien", "nguoiXuLy", "monHoc", "hocKy",
            "diem", "diem.dangKy", "diem.dangKy.lopHocPhan", "diem.dangKy.lopHocPhan.monHoc"
    })
    @Query("""
            select y from SupportRequest y
            where y.sinhVien.id = :sinhVienId
              and (:trangThai is null or y.trangThai = :trangThai)
              and (:loaiYeuCau is null or y.loaiYeuCau = :loaiYeuCau)
              and (:keyword is null
                   or lower(y.maPhieu) like lower(concat('%', :keyword, '%'))
                   or lower(y.tieuDe) like lower(concat('%', :keyword, '%')))
            """)
    Page<SupportRequest> searchBySinhVien(@Param("sinhVienId") Long sinhVienId,
                                         @Param("trangThai") SupportRequestStatus trangThai,
                                         @Param("loaiYeuCau") SupportRequestType loaiYeuCau,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    @EntityGraph(attributePaths = {
            "sinhVien", "nguoiXuLy", "monHoc", "hocKy",
            "diem", "diem.dangKy", "diem.dangKy.lopHocPhan", "diem.dangKy.lopHocPhan.monHoc"
    })
    @Query("""
            select y from SupportRequest y
            where (:trangThai is null or y.trangThai = :trangThai)
              and (:loaiYeuCau is null or y.loaiYeuCau = :loaiYeuCau)
              and (:keyword is null
                   or lower(y.maPhieu) like lower(concat('%', :keyword, '%'))
                   or lower(y.tieuDe) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.ma) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.hoTen) like lower(concat('%', :keyword, '%')))
            """)
    Page<SupportRequest> searchAll(@Param("trangThai") SupportRequestStatus trangThai,
                                  @Param("loaiYeuCau") SupportRequestType loaiYeuCau,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {
            "sinhVien", "nguoiXuLy", "monHoc", "hocKy",
            "diem", "diem.dangKy", "diem.dangKy.lopHocPhan", "diem.dangKy.lopHocPhan.monHoc"
    })
    @Query("""
            select y from SupportRequest y
            where (:trangThai is null or y.trangThai = :trangThai)
              and (:loaiYeuCau is null or y.loaiYeuCau = :loaiYeuCau)
              and (:mucUuTien is null or y.mucUuTien = :mucUuTien)
              and (:handlerUserId is null or y.nguoiXuLy.id = :handlerUserId)
              and (:onlyUnassigned = false or y.nguoiXuLy is null)
              and (:onlyAssignedToUserId is null or y.nguoiXuLy.id = :onlyAssignedToUserId)
              and (:keyword is null
                   or lower(y.maPhieu) like lower(concat('%', :keyword, '%'))
                   or lower(y.tieuDe) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.ma) like lower(concat('%', :keyword, '%'))
                   or lower(y.sinhVien.hoTen) like lower(concat('%', :keyword, '%')))
            """)
    Page<SupportRequest> searchQueue(
            @Param("trangThai") SupportRequestStatus trangThai,
            @Param("loaiYeuCau") SupportRequestType loaiYeuCau,
            @Param("mucUuTien") SupportRequestPriority mucUuTien,
            @Param("handlerUserId") Long handlerUserId,
            @Param("onlyUnassigned") boolean onlyUnassigned,
            @Param("onlyAssignedToUserId") Long onlyAssignedToUserId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sinhVien", "nguoiXuLy"})
    List<SupportRequest> findTop500ByOrderByCreatedAtDesc();

    boolean existsByMaPhieu(String maPhieu);

    long countByNguoiXuLyIsNull();

    long countByNguoiXuLyId(Long handlerUserId);
}
