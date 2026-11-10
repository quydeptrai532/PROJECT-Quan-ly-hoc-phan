package com.edu.registration.repository;

import com.edu.registration.entity.RateLimitBucket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM RateLimitBucket b WHERE b.bucketKey = :key")
    Optional<RateLimitBucket> findByIdForUpdate(@Param("key") String key);

    @Modifying
    @Query("DELETE FROM RateLimitBucket b WHERE b.resetAt < :cutoff")
    int deleteExpiredBuckets(@Param("cutoff") LocalDateTime cutoff);
}
