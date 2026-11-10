package com.edu.registration.repository;

import com.edu.registration.entity.Classroom;
import com.edu.registration.entity.ClassroomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    Optional<Classroom> findByMaIgnoreCase(String ma);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Classroom c where upper(c.ma) = upper(:ma)")
    Optional<Classroom> findByMaForUpdate(@Param("ma") String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);

    List<Classroom> findByTrangThaiOrderByMaAsc(ClassroomStatus trangThai);

    List<Classroom> findAllByOrderByMaAsc();

    @Query("""
        SELECT c FROM Classroom c
        WHERE (:keyword IS NULL OR LOWER(c.ma) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.ten) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (c.toaNha IS NOT NULL AND LOWER(c.toaNha) LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (c.viTri IS NOT NULL AND LOWER(c.viTri) LIKE LOWER(CONCAT('%', :keyword, '%'))))
          AND (:loaiPhong IS NULL OR c.loaiPhong = :loaiPhong)
          AND (:trangThai IS NULL OR c.trangThai = :trangThai)
          AND (:sucChuaMin IS NULL OR c.sucChua >= :sucChuaMin)
          AND (:sucChuaMax IS NULL OR c.sucChua <= :sucChuaMax)
          AND (:toaNha IS NULL OR (c.toaNha IS NOT NULL AND LOWER(c.toaNha) = LOWER(:toaNha)))
        ORDER BY c.ma ASC
    """)
    List<Classroom> timKiemPhong(
            @Param("keyword") String keyword,
            @Param("loaiPhong") String loaiPhong,
            @Param("trangThai") ClassroomStatus trangThai,
            @Param("sucChuaMin") Integer sucChuaMin,
            @Param("sucChuaMax") Integer sucChuaMax,
            @Param("toaNha") String toaNha
    );
}
