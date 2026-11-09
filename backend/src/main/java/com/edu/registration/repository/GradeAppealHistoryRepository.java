package com.edu.registration.repository;

import com.edu.registration.entity.GradeAppealHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeAppealHistoryRepository extends JpaRepository<GradeAppealHistory, Long> {

    @EntityGraph(attributePaths = {"hoSo", "nguoiThucHien"})
    List<GradeAppealHistory> findByHoSoIdOrderByThoiDiemAsc(Long hoSoId);
}
