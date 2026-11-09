package com.edu.registration.repository;

import com.edu.registration.entity.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, String> {

    @Modifying
    @Query("DELETE FROM RateLimitBucket b WHERE b.resetAt < :cutoff")
    int deleteExpiredBuckets(@Param("cutoff") LocalDateTime cutoff);
}
