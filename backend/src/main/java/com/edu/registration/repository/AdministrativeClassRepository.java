package com.edu.registration.repository;

import com.edu.registration.entity.AdministrativeClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministrativeClassRepository extends JpaRepository<AdministrativeClass, Long> {

    @EntityGraph(attributePaths = {"nganh", "nganh.khoa"})
    List<AdministrativeClass> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<AdministrativeClass> findByMa(String ma);
}
