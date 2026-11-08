package com.edu.registration.repository;

import com.edu.registration.entity.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<Subject> findAllByOrderByMaAsc();

    boolean existsByMa(String ma);

    Optional<Subject> findByMa(String ma);
}
