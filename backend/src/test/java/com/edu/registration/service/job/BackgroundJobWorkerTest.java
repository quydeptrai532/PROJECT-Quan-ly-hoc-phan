package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.BackgroundJob;
import com.edu.registration.entity.BackgroundJobStatus;
import com.edu.registration.repository.BackgroundJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackgroundJobWorkerTest {

    @Mock
    private BackgroundJobRepository jobRepository;
    @Mock
    private DistributedLockService lockService;

    private BackgroundJobProperties properties;
    private BackgroundJobWorker worker;

    @BeforeEach
    void setUp() {
        properties = new BackgroundJobProperties();
        properties.setEnabled(true);
        properties.setWorkerId("worker-node-1");
        properties.setLeaseTimeoutSeconds(180);
        properties.setDefaultBackoffSeconds(30);
        properties.setBackoffMultiplier(2.0);
        properties.setMaxBackoffSeconds(1000);
        properties.setBatchSize(10);
        properties.setTimezone("Asia/Ho_Chi_Minh");
    }

    @Test
    void shouldCalculateExponentialBackoffCorrectly() {
        // base = 30s, multiplier = 2.0, max = 1000
        assertEquals(30, BackgroundJobWorker.calculateBackoff(30, 1, 2.0, 1000));
        assertEquals(60, BackgroundJobWorker.calculateBackoff(30, 2, 2.0, 1000));
        assertEquals(120, BackgroundJobWorker.calculateBackoff(30, 3, 2.0, 1000));
        assertEquals(240, BackgroundJobWorker.calculateBackoff(30, 4, 2.0, 1000));
        assertEquals(1000, BackgroundJobWorker.calculateBackoff(30, 10, 2.0, 1000), "Should cap at maxBackoffSeconds");
    }

    @Test
    void shouldClaimLeaseAndExecuteHandlerSuccessfully() throws Exception {
        BackgroundJob job = new BackgroundJob();
        job.setId(1L);
        job.setJobType("CUSTOM_TASK");
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setNextRunAt(LocalDateTime.now().minusMinutes(1));

        BackgroundJob runningJob = new BackgroundJob();
        runningJob.setId(1L);
        runningJob.setJobType("CUSTOM_TASK");
        runningJob.setStatus(BackgroundJobStatus.RUNNING);

        when(jobRepository.findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(eq(BackgroundJobStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(job));
        when(jobRepository.claimJobLease(eq(1L), eq(BackgroundJobStatus.PENDING), eq(BackgroundJobStatus.RUNNING), eq("worker-node-1"), any(), any()))
                .thenReturn(1);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(runningJob));

        AtomicBoolean handlerExecuted = new AtomicBoolean(false);
        BackgroundJobHandler handler = new BackgroundJobHandler() {
            @Override
            public String getJobType() {
                return "CUSTOM_TASK";
            }

            @Override
            public void execute(BackgroundJob j) {
                handlerExecuted.set(true);
            }
        };

        worker = new BackgroundJobWorker(jobRepository, properties, lockService, List.of(handler), null);
        worker.processPendingJobs();

        assertTrue(handlerExecuted.get());
        verify(jobRepository).save(argThat(savedJob ->
                savedJob.getStatus() == BackgroundJobStatus.SUCCEEDED &&
                        savedJob.getCompletedAt() != null &&
                        savedJob.getLockedBy() == null));
    }

    @Test
    void shouldSkipExecutionWhenAnotherWorkerWinsLeaseContention() {
        BackgroundJob job = new BackgroundJob();
        job.setId(2L);
        job.setJobType("CONCURRENT_TASK");
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setNextRunAt(LocalDateTime.now().minusMinutes(1));

        when(jobRepository.findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(eq(BackgroundJobStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(job));
        // Atomic lease claim trả về 0 do instance khác nhận trước
        when(jobRepository.claimJobLease(eq(2L), any(), any(), any(), any(), any())).thenReturn(0);

        worker = new BackgroundJobWorker(jobRepository, properties, lockService, List.of(), null);
        worker.processPendingJobs();

        verify(jobRepository, never()).findById(2L);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void shouldRetryWithBackoffAndSanitizedErrorOnFailure() {
        BackgroundJob job = new BackgroundJob();
        job.setId(3L);
        job.setJobType("FAILING_TASK");
        job.setStatus(BackgroundJobStatus.PENDING);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setBackoffSeconds(30);

        BackgroundJob runningJob = new BackgroundJob();
        runningJob.setId(3L);
        runningJob.setJobType("FAILING_TASK");
        runningJob.setStatus(BackgroundJobStatus.RUNNING);
        runningJob.setRetryCount(0);
        runningJob.setMaxRetries(3);
        runningJob.setBackoffSeconds(30);

        when(jobRepository.findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(eq(BackgroundJobStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(job));
        when(jobRepository.claimJobLease(eq(3L), any(), any(), any(), any(), any())).thenReturn(1);
        when(jobRepository.findById(3L)).thenReturn(Optional.of(runningJob));

        BackgroundJobHandler failingHandler = new BackgroundJobHandler() {
            @Override
            public String getJobType() {
                return "FAILING_TASK";
            }

            @Override
            public void execute(BackgroundJob j) throws Exception {
                throw new RuntimeException("API error: secret=mySensitiveSecret with password=secretPass123");
            }
        };

        worker = new BackgroundJobWorker(jobRepository, properties, lockService, List.of(failingHandler), null);
        worker.processPendingJobs();

        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository).save(captor.capture());

        BackgroundJob saved = captor.getValue();
        assertEquals(BackgroundJobStatus.PENDING, saved.getStatus());
        assertEquals(1, saved.getRetryCount());
        assertNotNull(saved.getNextRunAt());
        // Lỗi phải được làm sạch, không được chứa password hay secret nhạy cảm
        assertFalse(saved.getLastError().contains("mySensitiveSecret"));
        assertFalse(saved.getLastError().contains("secretPass123"));
        assertTrue(saved.getLastError().contains("password=***REDACTED***"));
    }

    @Test
    void shouldTransitionToFailedWhenMaxRetriesExceeded() {
        BackgroundJob runningJob = new BackgroundJob();
        runningJob.setId(4L);
        runningJob.setJobType("EXHAUSTED_TASK");
        runningJob.setStatus(BackgroundJobStatus.RUNNING);
        runningJob.setRetryCount(2);
        runningJob.setMaxRetries(3);
        runningJob.setBackoffSeconds(30);

        BackgroundJob pendingJob = new BackgroundJob();
        pendingJob.setId(4L);
        pendingJob.setJobType("EXHAUSTED_TASK");
        pendingJob.setStatus(BackgroundJobStatus.PENDING);

        when(jobRepository.findByStatusAndNextRunAtLessThanEqualOrderByNextRunAtAsc(eq(BackgroundJobStatus.PENDING), any(), any(Pageable.class)))
                .thenReturn(List.of(pendingJob));
        when(jobRepository.claimJobLease(eq(4L), any(), any(), any(), any(), any())).thenReturn(1);
        when(jobRepository.findById(4L)).thenReturn(Optional.of(runningJob));

        BackgroundJobHandler failingHandler = new BackgroundJobHandler() {
            @Override
            public String getJobType() {
                return "EXHAUSTED_TASK";
            }

            @Override
            public void execute(BackgroundJob j) {
                throw new IllegalStateException("Service unavailable");
            }
        };

        worker = new BackgroundJobWorker(jobRepository, properties, lockService, List.of(failingHandler), null);
        worker.processPendingJobs();

        ArgumentCaptor<BackgroundJob> captor = ArgumentCaptor.forClass(BackgroundJob.class);
        verify(jobRepository).save(captor.capture());

        BackgroundJob saved = captor.getValue();
        assertEquals(BackgroundJobStatus.FAILED, saved.getStatus());
        assertEquals(3, saved.getRetryCount());
        assertNotNull(saved.getCompletedAt());
        assertNull(saved.getLockedBy());
    }

    @Test
    void shouldRecoverStuckRunningJobsAfterWorkerRestart() {
        BackgroundJob stuckJob = new BackgroundJob();
        stuckJob.setId(5L);
        stuckJob.setJobType("RECOVERABLE_TASK");
        stuckJob.setStatus(BackgroundJobStatus.RUNNING);
        stuckJob.setRetryCount(0);
        stuckJob.setMaxRetries(3);
        stuckJob.setBackoffSeconds(30);
        stuckJob.setLockedBy("dead-worker-pid-9999");
        stuckJob.setLeaseExpiresAt(LocalDateTime.now().minusMinutes(10));

        when(jobRepository.findByStatusAndLeaseExpiresAtLessThanEqualOrderByLeaseExpiresAtAsc(eq(BackgroundJobStatus.RUNNING), any(), any(Pageable.class)))
                .thenReturn(List.of(stuckJob));
        when(jobRepository.recoverStuckJob(eq(5L), eq(BackgroundJobStatus.RUNNING), eq(BackgroundJobStatus.PENDING), any(), anyString(), any()))
                .thenReturn(1);

        worker = new BackgroundJobWorker(jobRepository, properties, lockService, List.of(), null);
        worker.recoverStuckJobs();

        verify(jobRepository).recoverStuckJob(
                eq(5L), eq(BackgroundJobStatus.RUNNING), eq(BackgroundJobStatus.PENDING),
                any(), contains("Quá hạn lease thực thi"), any());
    }
}
