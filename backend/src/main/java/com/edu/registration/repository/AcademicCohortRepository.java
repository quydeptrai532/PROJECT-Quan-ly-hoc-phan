package com.edu.registration.repository;

import com.edu.registration.entity.AcademicCohort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademicCohortRepository extends JpaRepository<AcademicCohort, Long> {

    @EntityGraph(attributePaths = {"hocKyBatDau", "hocKyKetThuc"})
    List<AcademicCohort> findAllByOrderByNamBatDauDescMaAsc();

    boolean existsByMaIgnoreCase(String ma);

    Optional<AcademicCohort> findByMaIgnoreCase(String ma);
}
