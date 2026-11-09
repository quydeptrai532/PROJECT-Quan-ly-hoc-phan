package com.edu.registration.repository;

import com.edu.registration.entity.Major;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<Major> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<Major> findByMa(String ma);
}
