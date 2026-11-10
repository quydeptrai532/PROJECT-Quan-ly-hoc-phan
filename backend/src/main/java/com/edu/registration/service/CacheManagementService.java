package com.edu.registration.service;

import com.edu.registration.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Quản lý vòng đời và vô hiệu hóa cache (Invalidation) an toàn (QLSV-94 / TASK-OP-07 Work Package C).
 */
@Service
public class CacheManagementService {

    private static final Logger log = LoggerFactory.getLogger(CacheManagementService.class);

    private final CacheManager cacheManager;

    public CacheManagementService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public List<String> getCacheNames() {
        return new ArrayList<>(cacheManager.getCacheNames());
    }

    public boolean clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Đã xóa toàn bộ bộ đệm cache [{}]", cacheName);
            return true;
        }
        return false;
    }

    public void clearAllCaches() {
        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        log.info("Đã xóa tất cả bộ đệm cache trong hệ thống");
    }

    public void evictStudentProgress(String studentUsername) {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_STUDENT_PROGRESS);
        if (cache != null && studentUsername != null) {
            cache.evict(studentUsername);
            log.debug("Đã xóa cache tiến độ học tập cho sinh viên [{}]", studentUsername);
        }
    }

    public void evictCatalogSubjects() {
        clearCache(CacheConfig.CACHE_CATALOG_SUBJECTS);
    }

    public void evictCatalogSemesters() {
        clearCache(CacheConfig.CACHE_CATALOG_SEMESTERS);
    }

    public void evictCatalogClassrooms() {
        clearCache(CacheConfig.CACHE_CATALOG_CLASSROOMS);
    }
}
