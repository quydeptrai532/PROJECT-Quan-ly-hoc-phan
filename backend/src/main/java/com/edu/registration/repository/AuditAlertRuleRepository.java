package com.edu.registration.repository;

import com.edu.registration.entity.AuditAlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditAlertRuleRepository
        extends JpaRepository<AuditAlertRule, String> {
}