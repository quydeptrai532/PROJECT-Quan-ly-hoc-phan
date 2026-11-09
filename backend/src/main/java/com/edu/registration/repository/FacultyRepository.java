package com.edu.registration.repository;

import com.edu.registration.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    List<Faculty> findAllByOrderByMaAsc();

    List<Faculty> findByDangHieuLucOrderByMaAsc(boolean dangHieuLuc);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);

    Optional<Faculty> findByMa(String ma);

    Optional<Faculty> findByMaIgnoreCase(String ma);
}
