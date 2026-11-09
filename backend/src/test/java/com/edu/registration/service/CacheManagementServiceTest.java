package com.edu.registration.service;

import com.edu.registration.config.CacheConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CacheManagementServiceTest {

    private CacheManager cacheManager;
    private CacheManagementService cacheManagementService;
    private Cache mockCache;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        mockCache = mock(Cache.class);
        when(cacheManager.getCacheNames()).thenReturn(Set.of(
                CacheConfig.CACHE_CATALOG_SUBJECTS,
                CacheConfig.CACHE_CATALOG_SEMESTERS,
                CacheConfig.CACHE_STUDENT_PROGRESS
        ));
        when(cacheManager.getCache(anyString())).thenReturn(mockCache);

        cacheManagementService = new CacheManagementService(cacheManager);
    }

    @Test
    void shouldReturnAllCacheNames() {
        List<String> names = cacheManagementService.getCacheNames();
        assertEquals(3, names.size());
        assertTrue(names.contains(CacheConfig.CACHE_CATALOG_SUBJECTS));
    }

    @Test
    void shouldClearSpecificCache() {
        boolean cleared = cacheManagementService.clearCache(CacheConfig.CACHE_CATALOG_SUBJECTS);
        assertTrue(cleared);
        verify(mockCache, times(1)).clear();
    }

    @Test
    void shouldClearAllCaches() {
        cacheManagementService.clearAllCaches();
        verify(mockCache, times(3)).clear();
    }

    @Test
    void shouldEvictPersonalStudentProgressCacheByKey() {
        String studentUsername = "SV001";
        cacheManagementService.evictStudentProgress(studentUsername);
        verify(cacheManager).getCache(CacheConfig.CACHE_STUDENT_PROGRESS);
        verify(mockCache).evict(studentUsername);
    }
}
