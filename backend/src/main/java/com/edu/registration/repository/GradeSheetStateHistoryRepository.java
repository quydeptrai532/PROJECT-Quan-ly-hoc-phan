package com.edu.registration.repository;

import com.edu.registration.entity.GradeSheetStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradeSheetStateHistoryRepository extends JpaRepository<GradeSheetStateHistory, Long> {

    List<GradeSheetStateHistory> findByLopHocPhanIdOrderByThoiDiemDescIdDesc(Long lopHocPhanId);
}
