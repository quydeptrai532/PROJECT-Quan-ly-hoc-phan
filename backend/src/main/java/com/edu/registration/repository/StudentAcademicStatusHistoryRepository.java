package com.edu.registration.repository;

import com.edu.registration.entity.StudentAcademicStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAcademicStatusHistoryRepository extends JpaRepository<StudentAcademicStatusHistory, Long> {

    @EntityGraph(attributePaths = {"sinhVien", "nguoiThucHien"})
    List<StudentAcademicStatusHistory> findBySinhVienIdOrderByThoiDiemDescIdDesc(Long sinhVienId);
}
