package com.edu.registration.repository;

import com.edu.registration.entity.SupportRequestComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportRequestCommentRepository extends JpaRepository<SupportRequestComment, Long> {

    @EntityGraph(attributePaths = {"nguoiGui"})
    List<SupportRequestComment> findByYeuCauIdOrderByCreatedAtAscIdAsc(Long yeuCauId);

    long countByYeuCauId(Long yeuCauId);
}
