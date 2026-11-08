package com.edu.registration.repository;

import com.edu.registration.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    List<Faculty> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<Faculty> findByMa(String ma);
}
