package com.edu.registration.service.job;

import com.edu.registration.config.BackgroundJobProperties;
import com.edu.registration.entity.DistributedLock;
import com.edu.registration.repository.DistributedLockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Quản lý khóa phân tán bền vững trong cơ sở dữ liệu, chống chạy trùng giữa nhiều instance hoặc worker (QLSV-93 / TASK-OP-06).
 */
@Service
public class DistributedLockService {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockService.class);

    private final DistributedLockRepository lockRepository;
    private final BackgroundJobProperties properties;
    private final TransactionTemplate requiresNewTemplate;

    public DistributedLockService(DistributedLockRepository lockRepository,
                                  BackgroundJobProperties properties,
                                  PlatformTransactionManager transactionManager) {
        this.lockRepository = lockRepository;
        this.properties = properties;
        if (transactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.requiresNewTemplate = template;
        } else {
            this.requiresNewTemplate = null;
        }
    }

    /**
     * Thử xin khóa phân tán với thời hạn nhất định.
     */
    public boolean tryAcquire(String lockName, Duration ttl) {
        if (requiresNewTemplate != null) {
            Boolean result = requiresNewTemplate.execute(status -> doTryAcquire(lockName, ttl));
            return Boolean.TRUE.equals(result);
        }
        return doTryAcquire(lockName, ttl);
    }

    private boolean doTryAcquire(String lockName, Duration ttl) {
        LocalDateTime now = LocalDateTime.now(properties.getZoneId());
        LocalDateTime expiresAt = now.plus(ttl);
        String workerId = properties.getWorkerId();

        try {
            Optional<DistributedLock> existing = lockRepository.findById(lockName);
            if (existing.isEmpty()) {
                DistributedLock newLock = new DistributedLock(lockName, workerId, now, expiresAt);
                lockRepository.saveAndFlush(newLock);
                log.debug("Đã cấp khóa phân tán [{}] cho worker [{}] đến {}", lockName, workerId, expiresAt);
                return true;
            }

            DistributedLock current = existing.get();
            if (current.getExpiresAt().isBefore(now)) {
                int updated = lockRepository.acquireExpiredLock(lockName, workerId, now, expiresAt);
                if (updated > 0) {
                    log.info("Thu hồi khóa phân tán hết hạn [{}] (trước đó giữ bởi [{}]) giao cho [{}]",
                            lockName, current.getLockedBy(), workerId);
                    return true;
                }
            }
            log.debug("Khóa phân tán [{}] đang được nắm giữ bởi [{}], hết hạn {}",
                    lockName, current.getLockedBy(), current.getExpiresAt());
            return false;
        } catch (DataIntegrityViolationException ex) {
            // Trường hợp 2 instance cùng cố gắng INSERT đồng thời
            log.debug("Xung đột đồng thời khi tạo khóa [{}], thử cập nhật nếu đã hết hạn", lockName);
            int updated = lockRepository.acquireExpiredLock(lockName, workerId, now, expiresAt);
            return updated > 0;
        } catch (Exception ex) {
            log.warn("Lỗi khi xin khóa phân tán [{}]: {}", lockName, ex.getMessage());
            return false;
        }
    }

    /**
     * Giải phóng khóa phân tán nếu đang được sở hữu bởi worker hiện tại.
     */
    public void release(String lockName) {
        if (requiresNewTemplate != null) {
            requiresNewTemplate.executeWithoutResult(status -> doRelease(lockName));
        } else {
            doRelease(lockName);
        }
    }

    private void doRelease(String lockName) {
        try {
            String workerId = properties.getWorkerId();
            lockRepository.releaseLock(lockName, workerId);
            log.debug("Đã giải phóng khóa phân tán [{}] bởi worker [{}]", lockName, workerId);
        } catch (Exception ex) {
            log.warn("Không thể giải phóng khóa phân tán [{}]: {}", lockName, ex.getMessage());
        }
    }

    /**
     * Thực thi đoạn mã độc quyền dưới sự bảo vệ của khóa phân tán.
     * Trả về true nếu nhận được khóa và chạy xong, false nếu bị bỏ qua do có instance khác đang chạy.
     */
    public boolean executeWithLock(String lockName, Duration ttl, Runnable task) {
        if (!tryAcquire(lockName, ttl)) {
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            release(lockName);
        }
    }
}
