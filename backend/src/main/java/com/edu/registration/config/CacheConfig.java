package com.edu.registration.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cấu hình Cache phân vùng và kiểm soát bộ nhớ đệm (QLSV-94 / TASK-OP-07 Work Package C).
 * Nguyên tắc:
 * 1. Danh mục dùng chung (catalog) được lưu bộ đệm với khóa tổng quát.
 * 2. Dữ liệu cá nhân (sinh viên, điểm, tiến độ) BẮT BUỘC dùng cache key phân vùng theo mã sinh viên / người dùng.
 * 3. Tự động hủy cache (eviction/invalidation) ngay khi phát sinh thay đổi dữ liệu.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_CATALOG_SUBJECTS = "catalog_subjects";
    public static final String CACHE_CATALOG_SEMESTERS = "catalog_semesters";
    public static final String CACHE_CATALOG_CLASSROOMS = "catalog_classrooms";
    public static final String CACHE_STUDENT_PROGRESS = "student_academic_progress";
    public static final String CACHE_STUDENT_SCHEDULE = "student_schedule";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_CATALOG_SUBJECTS,
                CACHE_CATALOG_SEMESTERS,
                CACHE_CATALOG_CLASSROOMS,
                CACHE_STUDENT_PROGRESS,
                CACHE_STUDENT_SCHEDULE
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(10000)
                .recordStats());
        return cacheManager;
    }
}
