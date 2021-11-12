package com.antelope.clustercache.autoconfigure.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.lang.NonNull;

import java.util.concurrent.Callable;

/**
 * @author yaml
 * @since 2021/6/30
 */
@Slf4j
public class CacheWrapper implements Cache {

    private final Cache cache;

    public CacheWrapper(Cache cache) {
        this.cache = cache;
    }

    public Cache getCache(){
        return cache;
    }

    @Override
    public @NonNull
    String getName() {
        return cache.getName();
    }

    @Override
    public @NonNull
    Object getNativeCache() {
        return cache.getNativeCache();
    }

    @Override
    public ValueWrapper get(@NonNull Object key) {
        ValueWrapper valueWrapper = cache.get(key);
        if (log.isDebugEnabled()) {
            log.debug("cache get, key:{}, value:{}", key, valueWrapper);
        }
        return valueWrapper;
    }

    @Override
    public <T> T get(@NonNull Object key, Class<T> clazz) {
        T value = cache.get(key, clazz);
        if (log.isDebugEnabled()) {
            log.debug("cache get class, class:{}, key:{}, value:{}", clazz, key, value);
        }
        return value;
    }

    @Override
    public <T> T get(@NonNull Object key, @NonNull Callable<T> callable) {
        T value = cache.get(key, callable);
        if (log.isDebugEnabled()) {
            log.debug("cache get callable, callable:{}, key:{}, value:{}", callable, key, value);
        }
        return value;
    }

    @Override
    public void put(@NonNull Object key, Object value) {
        if (log.isDebugEnabled()) {
            log.debug("cache put, key:{}, value:{}", key, value);
        }
        cache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        if (log.isDebugEnabled()) {
            log.debug("cache putIfAbsent, key:{}, value:{}", key, value);
        }
        return cache.putIfAbsent(key, value);
    }

    @Override
    public void evict(@NonNull Object key) {
        if (log.isDebugEnabled()) {
            log.debug("cache evict, key:{}", key);
        }
        cache.evict(key);
    }

    @Override
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("cache clear, cacheName:{}", getName());
        }
        cache.clear();
    }
}
