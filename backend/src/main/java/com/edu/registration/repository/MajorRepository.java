package com.edu.registration.repository;

import com.edu.registration.entity.Major;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {

    @EntityGraph(attributePaths = {"khoa"})
    List<Major> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"khoa"})
    List<Major> findByDangHieuLucOrderByMaAsc(boolean dangHieuLuc);

    @EntityGraph(attributePaths = {"khoa"})
    List<Major> findByKhoaIdOrderByMaAsc(Long khoaId);

    @EntityGraph(attributePaths = {"khoa"})
    Optional<Major> findWithKhoaById(Long id);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByMaIgnoreCaseAndIdNot(String ma, Long id);

    Optional<Major> findByMa(String ma);

    Optional<Major> findByMaIgnoreCase(String ma);

    long countByKhoaId(Long khoaId);
}
