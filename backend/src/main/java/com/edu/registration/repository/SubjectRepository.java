package com.edu.registration.repository;

import com.edu.registration.entity.Subject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<Subject> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"khoa"})
    List<Subject> findByDangHieuLucOrderByMaAsc(boolean dangHieuLuc);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);

    Optional<Subject> findByMa(String ma);

    Optional<Subject> findByMaIgnoreCase(String ma);

    long countByKhoaId(Long khoaId);
}
