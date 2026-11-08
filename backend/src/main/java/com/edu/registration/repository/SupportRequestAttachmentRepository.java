package com.edu.registration.repository;

import com.edu.registration.entity.SupportRequestAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupportRequestAttachmentRepository extends JpaRepository<SupportRequestAttachment, Long> {

    List<SupportRequestAttachment> findByYeuCauIdOrderByIdAsc(Long yeuCauId);

    Optional<SupportRequestAttachment> findByIdAndYeuCauId(Long id, Long yeuCauId);

    long countByYeuCauId(Long yeuCauId);
}
