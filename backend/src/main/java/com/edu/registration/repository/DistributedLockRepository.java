package com.edu.registration.repository;

import com.edu.registration.entity.DistributedLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DistributedLockRepository extends JpaRepository<DistributedLock, String> {

    @Modifying
    @Query("UPDATE DistributedLock l SET l.lockedBy = :workerId, l.lockedAt = :now, l.expiresAt = :expiresAt " +
            "WHERE l.lockName = :lockName AND l.expiresAt <= :now")
    int acquireExpiredLock(@Param("lockName") String lockName,
                           @Param("workerId") String workerId,
                           @Param("now") LocalDateTime now,
                           @Param("expiresAt") LocalDateTime expiresAt);

    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.lockName = :lockName AND l.lockedBy = :workerId")
    int releaseLock(@Param("lockName") String lockName, @Param("workerId") String workerId);

    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.expiresAt <= :cutoff")
    int cleanExpiredLocks(@Param("cutoff") LocalDateTime cutoff);
}
