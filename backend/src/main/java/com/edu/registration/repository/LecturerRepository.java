package com.edu.registration.repository;

import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {

    Optional<Lecturer> findByUser(User user);

    Optional<Lecturer> findByMa(String ma);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"user", "khoa"})
    List<Lecturer> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"user", "khoa"})
    List<Lecturer> findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(String hoTen, String ma);
}
