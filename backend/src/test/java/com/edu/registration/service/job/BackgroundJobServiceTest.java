package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.exception.ApiException;
import com.edu.registration.repository.BackgroundJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServiceTest {

    @Mock
    private BackgroundJobRepository jobRepository;

    private BackgroundJobProperties properties;
    private BackgroundJobService jobService;

    @BeforeEach
    void setUp() {
        properties = new BackgroundJobProperties();
        properties.setDefaultMaxRetries(4);
        properties.setDefaultBackoffSeconds(45);
        properties.setTimezone("Asia/Ho_Chi_Minh");
        jobService = new BackgroundJobService(jobRepository, properties);
    }

    @Test
    void shouldEnqueueNewJobWithDefaultConfiguration() {
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(invocation -> {
            BackgroundJob job = invocation.getArgument(0);
            job.setId(100L);
            return job;
        });

        BackgroundJob created = jobService.enqueue(
                "TEST_JOB", "key-123", "{\"payload\":\"test\"}", null, null, null);

        assertNotNull(created);
        assertEquals(100L, created.getId());
        assertEquals("TEST_JOB", created.getJobType());
        assertEquals("key-123", created.getIdempotencyKey());
        assertEquals(BackgroundJobStatus.PENDING, created.getStatus());
        assertEquals(0, created.getRetryCount());
        assertEquals(4, created.getMaxRetries());
        assertEquals(45, created.getBackoffSeconds());
        assertNotNull(created.getNextRunAt());
    }

    @Test
    void shouldReturnExistingJobWhenIdempotencyKeyMatches() {
        BackgroundJob existing = new BackgroundJob();
        existing.setId(200L);
        existing.setJobType("TEST_JOB");
        existing.setIdempotencyKey("duplicate-key");
        existing.setStatus(BackgroundJobStatus.PENDING);

        when(jobRepository.findByIdempotencyKey("duplicate-key")).thenReturn(Optional.of(existing));

        BackgroundJob result = jobService.enqueue("TEST_JOB", "duplicate-key", "{}", 3, 30, null);

        assertEquals(200L, result.getId());
        verify(jobRepository, never()).save(any());
    }

    @Test
    void shouldRetryFailedJobSuccessfully() {
        BackgroundJob failedJob = new BackgroundJob();
        failedJob.setId(300L);
        failedJob.setJobType("EMAIL_DISPATCH");
        failedJob.setStatus(BackgroundJobStatus.FAILED);
        failedJob.setRetryCount(3);
        failedJob.setMaxRetries(3);
        failedJob.setLastError("Connection timed out");
        failedJob.setLockedBy("crashed-worker");

        when(jobRepository.findById(300L)).thenReturn(Optional.of(failedJob));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(i -> i.getArgument(0));

        BackgroundJob retried = jobService.retryJob(300L);

        assertEquals(BackgroundJobStatus.PENDING, retried.getStatus());
        assertNull(retried.getLockedBy());
        assertNull(retried.getLeaseExpiresAt());
        assertTrue(retried.getMaxRetries() > 3, "Max retries should be incremented to allow new attempt");
        verify(jobRepository).save(retried);
    }

    @Test
    void shouldRejectRetryWhenJobIsCurrentlyRunning() {
        BackgroundJob runningJob = new BackgroundJob();
        runningJob.setId(400L);
        runningJob.setStatus(BackgroundJobStatus.RUNNING);

        when(jobRepository.findById(400L)).thenReturn(Optional.of(runningJob));

        ApiException ex = assertThrows(ApiException.class, () -> jobService.retryJob(400L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void shouldCancelPendingJob() {
        BackgroundJob pending = new BackgroundJob();
        pending.setId(500L);
        pending.setStatus(BackgroundJobStatus.PENDING);

        when(jobRepository.findById(500L)).thenReturn(Optional.of(pending));
        when(jobRepository.save(any(BackgroundJob.class))).thenAnswer(i -> i.getArgument(0));

        BackgroundJob cancelled = jobService.cancelJob(500L, "Không cần chạy nữa");

        assertEquals(BackgroundJobStatus.FAILED, cancelled.getStatus());
        assertTrue(cancelled.getLastError().contains("Không cần chạy nữa"));
        assertNotNull(cancelled.getCompletedAt());
    }

    @Test
    void shouldReturnAccurateStatistics() {
        when(jobRepository.countByStatus(BackgroundJobStatus.PENDING)).thenReturn(10L);
        when(jobRepository.countByStatus(BackgroundJobStatus.RUNNING)).thenReturn(2L);
        when(jobRepository.countByStatus(BackgroundJobStatus.SUCCEEDED)).thenReturn(50L);
        when(jobRepository.countByStatus(BackgroundJobStatus.FAILED)).thenReturn(3L);

        Map<String, Long> stats = jobService.getStatistics();

        assertEquals(10L, stats.get("pending"));
        assertEquals(2L, stats.get("running"));
        assertEquals(50L, stats.get("succeeded"));
        assertEquals(3L, stats.get("failed"));
        assertEquals(65L, stats.get("total"));
    }
}
