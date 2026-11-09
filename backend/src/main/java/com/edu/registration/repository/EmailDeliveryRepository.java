package com.edu.registration.repository;

import com.edu.registration.entity.EmailDelivery;
import com.edu.registration.entity.EmailDeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {

    Optional<EmailDelivery> findByRecipientIdAndIdempotencyKey(Long recipientId, String idempotencyKey);

    boolean existsByRecipientIdAndIdempotencyKey(Long recipientId, String idempotencyKey);

    @Query("""
        select e from EmailDelivery e
        where e.status in :statuses
          and e.retryCount < :maxRetries
          and (e.nextRetryAt is null or e.nextRetryAt <= :now)
        order by e.nextRetryAt asc, e.id asc
        """)
    List<EmailDelivery> findRetryableEmails(
            @Param("statuses") Collection<EmailDeliveryStatus> statuses,
            @Param("maxRetries") int maxRetries,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    Page<EmailDelivery> findByRecipientId(Long recipientId, Pageable pageable);

    long countByStatus(EmailDeliveryStatus status);
}
