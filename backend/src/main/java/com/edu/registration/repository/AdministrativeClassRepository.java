package com.edu.registration.repository;

import com.edu.registration.entity.AdministrativeClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministrativeClassRepository extends JpaRepository<AdministrativeClass, Long> {

    @EntityGraph(attributePaths = {"nganh", "nganh.khoa"})
    List<AdministrativeClass> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"nganh", "nganh.khoa"})
    List<AdministrativeClass> findByDangHieuLucOrderByMaAsc(boolean dangHieuLuc);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);

    Optional<AdministrativeClass> findByMa(String ma);

    Optional<AdministrativeClass> findByMaIgnoreCase(String ma);

    long countByNganhId(Long nganhId);
}
