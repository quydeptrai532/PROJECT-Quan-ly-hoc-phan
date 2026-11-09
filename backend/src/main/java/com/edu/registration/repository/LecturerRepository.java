package com.edu.registration.repository;

import com.edu.registration.entity.Lecturer;
import com.edu.registration.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByKhoaId(Long khoaId);

    @EntityGraph(attributePaths = {"user", "khoa"})

    @Query(value = """
        select gv from Lecturer gv
        where lower(gv.ma) like concat('%', :kw, '%') or lower(gv.hoTen) like concat('%', :kw, '%')
        """, countQuery = """
        select count(gv) from Lecturer gv
        where lower(gv.ma) like concat('%', :kw, '%') or lower(gv.hoTen) like concat('%', :kw, '%')
        """)
    Page<Lecturer> searchForAdminLookup(@Param("kw") String kw, Pageable pageable);
}
