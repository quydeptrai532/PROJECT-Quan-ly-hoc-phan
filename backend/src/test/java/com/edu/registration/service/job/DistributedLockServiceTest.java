package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.DistributedLock;
import com.edu.registration.repository.DistributedLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private DistributedLockRepository lockRepository;

    private BackgroundJobProperties properties;
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        properties = new BackgroundJobProperties();
        properties.setWorkerId("worker-test-1");
        properties.setTimezone("Asia/Ho_Chi_Minh");
        lockService = new DistributedLockService(lockRepository, properties, null);
    }

    @Test
    void shouldAcquireNewLockSuccessfully() {
        when(lockRepository.findById("test-lock")).thenReturn(Optional.empty());

        boolean acquired = lockService.tryAcquire("test-lock", Duration.ofMinutes(5));

        assertTrue(acquired);
        ArgumentCaptor<DistributedLock> captor = ArgumentCaptor.forClass(DistributedLock.class);
        verify(lockRepository).saveAndFlush(captor.capture());
        assertEquals("test-lock", captor.getValue().getLockName());
        assertEquals("worker-test-1", captor.getValue().getLockedBy());
    }

    @Test
    void shouldRejectWhenLockActiveByAnotherWorker() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(5);
        DistributedLock activeLock = new DistributedLock("busy-lock", "worker-other", LocalDateTime.now(), future);
        when(lockRepository.findById("busy-lock")).thenReturn(Optional.of(activeLock));

        boolean acquired = lockService.tryAcquire("busy-lock", Duration.ofMinutes(5));

        assertFalse(acquired);
        verify(lockRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldAcquireWhenPreviousLockExpired() {
        LocalDateTime past = LocalDateTime.now().minusMinutes(5);
        DistributedLock expiredLock = new DistributedLock("expired-lock", "worker-crashed", past.minusMinutes(5), past);
        when(lockRepository.findById("expired-lock")).thenReturn(Optional.of(expiredLock));
        when(lockRepository.acquireExpiredLock(eq("expired-lock"), eq("worker-test-1"), any(), any()))
                .thenReturn(1);

        boolean acquired = lockService.tryAcquire("expired-lock", Duration.ofMinutes(5));

        assertTrue(acquired);
        verify(lockRepository).acquireExpiredLock(eq("expired-lock"), eq("worker-test-1"), any(), any());
    }

    @Test
    void shouldReleaseLockProperly() {
        lockService.release("test-lock");

        verify(lockRepository).releaseLock("test-lock", "worker-test-1");
    }

    @Test
    void shouldExecuteWithLockAndRelease() {
        when(lockRepository.findById("block-lock")).thenReturn(Optional.empty());

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = lockService.executeWithLock("block-lock", Duration.ofMinutes(1), () -> executed.set(true));

        assertTrue(result);
        assertTrue(executed.get());
        verify(lockRepository).releaseLock("block-lock", "worker-test-1");
    }

    @Test
    void shouldSkipExecutionWhenLockHeldByOther() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(2);
        when(lockRepository.findById("held-lock")).thenReturn(
                Optional.of(new DistributedLock("held-lock", "other", LocalDateTime.now(), future)));

        AtomicBoolean executed = new AtomicBoolean(false);
        boolean result = lockService.executeWithLock("held-lock", Duration.ofMinutes(1), () -> executed.set(true));

        assertFalse(result);
        assertFalse(executed.get());
        verify(lockRepository, never()).releaseLock(anyString(), anyString());
    }
}
