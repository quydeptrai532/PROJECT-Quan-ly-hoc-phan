package com.edu.registration.repository;

import com.edu.registration.entity.Student;
import com.edu.registration.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser(User user);

    @EntityGraph(attributePaths = {"khoa", "nganh", "lopHanhChinh", "chuongTrinh"})
    Optional<Student> findWithAcademicProfileByUserId(Long userId);

    @EntityGraph(attributePaths = {"khoa", "nganh", "lopHanhChinh", "chuongTrinh"})
    Optional<Student> findWithAcademicProfileById(Long id);

    @Query("select sv from Student sv join fetch sv.user u where u.trangThai = true")
    List<Student> findAllActiveWithUser();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sv from Student sv where sv.user.id = :userId")
    Optional<Student> findByUserIdForUpdate(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sv from Student sv where sv.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);

    Optional<Student> findByMa(String ma);

    boolean existsByMa(String ma);

    boolean existsByMaIgnoreCase(String ma);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<Student> findAllByOrderByMaAsc();

    @EntityGraph(attributePaths = {"user", "khoa", "nganh", "lopHanhChinh"})
    List<Student> findByHoTenContainingIgnoreCaseOrMaContainingIgnoreCaseOrderByMaAsc(String hoTen, String ma);

    @EntityGraph(attributePaths = {"khoa", "nganh", "chuongTrinh"})
    @Query("""
            select sv from Student sv
            where (:khoaId is null or sv.khoa.id = :khoaId)
              and (:nganhId is null or sv.nganh.id = :nganhId)
            """)
    List<Student> findByKhoaIdAndNganhIdForAnalytics(@Param("khoaId") Long khoaId, @Param("nganhId") Long nganhId);

    long countByKhoaId(Long khoaId);

    long countByNganhId(Long nganhId);

    long countByLopHanhChinhId(Long lopHanhChinhId);
}
