package com.antelope.clustercache.autoconfigure.util;

import com.antelope.clustercache.autoconfigure.annotion.BatchCacheable;
import com.antelope.clustercache.autoconfigure.annotion.CacheExpire;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yaml
 * @since 2021/7/2
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheUtil {

    /**
     * @return key: cacheNames value: CacheExpire
     */
    public static Map<String, CacheExpire> resolveCacheExpire(ApplicationContext applicationContext) {
        Map<String, CacheExpire> result = new HashMap<>(64);
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            fillData(applicationContext.getType(beanName), result);
        }
        return result;
    }

    private static void fillData(Class<?> clazz, Map<String, CacheExpire> map) {

        ReflectionUtils.doWithMethods(clazz, method -> {
            ReflectionUtils.makeAccessible(method);
            CacheExpire cacheExpire = AnnotationUtils.findAnnotation(method, CacheExpire.class);

            Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
            if (cacheable != null) {
                Arrays.stream(cacheable.cacheNames()).forEach(cacheName -> map.put(cacheName, cacheExpire));
                return;
            }
            Caching caching = AnnotationUtils.findAnnotation(method, Caching.class);
            if (caching != null) {
                Arrays.stream(caching.cacheable()).forEach(c -> Arrays.stream(c.cacheNames()).forEach(cacheName -> map.put(cacheName, cacheExpire)));
                return;
            }
            BatchCacheable batchCacheable = AnnotationUtils.findAnnotation(method, BatchCacheable.class);
            if (batchCacheable != null) {
                map.put(batchCacheable.cacheName(), cacheExpire);
                return;
            }
            CacheConfig cacheConfig = AnnotationUtils.findAnnotation(clazz, CacheConfig.class);
            if (cacheConfig != null) {
                Arrays.stream(cacheConfig.cacheNames()).forEach(cacheName -> map.put(cacheName, cacheExpire));
            }
        });
    }
}
