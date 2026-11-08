package com.edu.registration.repository;

import com.edu.registration.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findAllByOccurredAtAfterOrderByOccurredAtAsc(
            LocalDateTime occurredAt
    );

    @Query("""
            select event from AuditLog event
            where (:fromTime is null or event.occurredAt >= :fromTime)
              and (:toTime is null or event.occurredAt <= :toTime)
              and (:actor is null or lower(event.actorUsername) like lower(concat('%', :actor, '%')))
              and (:action is null or event.action = :action)
              and (:entityType is null or event.entityType = :entityType)
              and (:entityId is null or event.entityId = :entityId)
            """)

    Page<AuditLog> search(@Param("fromTime") LocalDateTime fromTime,
                          @Param("toTime") LocalDateTime toTime,
                          @Param("actor") String actor,
                          @Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("entityId") String entityId,
                          Pageable pageable);
}
