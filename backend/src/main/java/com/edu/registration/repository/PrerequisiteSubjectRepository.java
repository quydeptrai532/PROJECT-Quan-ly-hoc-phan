package com.edu.registration.repository;

import com.edu.registration.entity.PrerequisiteSubject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;

public interface PrerequisiteSubjectRepository extends JpaRepository<PrerequisiteSubject, Long> {

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet"})
    List<PrerequisiteSubject> findByMonHocIdOrderByIdAsc(Long monHocId);

    @EntityGraph(attributePaths = {"monHoc", "monTienQuyet"})
    List<PrerequisiteSubject> findByMonHocIdInOrderByIdAsc(Collection<Long> monHocIds);

    boolean existsByMonHocIdAndMonTienQuyetId(Long monHocId, Long monTienQuyetId);

    boolean existsByMonHocId(Long monHocId);

    long countByMonTienQuyetId(Long monHocId);
}
