package com.edu.registration.repository;

import com.edu.registration.entity.AuditAlert;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuditAlertRepository
        extends JpaRepository<AuditAlert, Long> {

    Page<AuditAlert> findAllByStatusOrderByOccurredAtDesc(
            String status,
            Pageable pageable
    );

    Page<AuditAlert> findAllByOrderByOccurredAtDesc(
            Pageable pageable
    );

    Optional<AuditAlert> findByFingerprint(String fingerprint);
}