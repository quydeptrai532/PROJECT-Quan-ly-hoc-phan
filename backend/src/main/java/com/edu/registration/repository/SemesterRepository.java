package com.edu.registration.repository;

import com.edu.registration.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {

    List<Semester> findAllByOrderByNgayBatDauDesc();

    List<Semester> findAllByOrderByNgayBatDauAsc();

    Optional<Semester> findFirstByDangKyTuLessThanEqualAndDangKyDenGreaterThanEqualOrderByDangKyDenDesc(
            LocalDate ngay, LocalDate cungNgay);

    Optional<Semester> findFirstByOrderByDangKyDenDesc();

    boolean existsByTenAndNamHoc(String ten, String namHoc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Semester h where h.id = :id")
    Optional<Semester> findByIdForUpdate(@Param("id") Long id);
}
