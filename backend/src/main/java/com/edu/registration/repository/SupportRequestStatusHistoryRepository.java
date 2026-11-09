package com.edu.registration.repository;

import com.edu.registration.entity.SupportRequestStatusHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportRequestStatusHistoryRepository extends JpaRepository<SupportRequestStatusHistory, Long> {

    @EntityGraph(attributePaths = {"nguoiThucHien"})
    List<SupportRequestStatusHistory> findByYeuCauIdOrderByThoiDiemAscIdAsc(Long yeuCauId);
}
