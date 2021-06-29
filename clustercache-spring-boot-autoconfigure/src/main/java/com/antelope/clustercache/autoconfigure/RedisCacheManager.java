package com.antelope.clustercache.autoconfigure;

import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Slf4j
public class RedisCacheManager extends org.springframework.data.redis.cache.RedisCacheManager implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration = new LinkedHashMap<>();

    public static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    public static final GenericFastJsonRedisSerializer FASTJSON_SERIALIZER = new GenericFastJsonRedisSerializer();
    public static final RedisSerializationContext.SerializationPair<String> STRING_PAIR = RedisSerializationContext.SerializationPair.fromSerializer(STRING_SERIALIZER);
    public static final RedisSerializationContext.SerializationPair<Object> FASTJSON_PAIR = RedisSerializationContext.SerializationPair.fromSerializer(FASTJSON_SERIALIZER);

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public RedisCacheManager(RedisCacheWriter cacheWriter) {
        super(cacheWriter, defaultCacheConfig());
    }

    @Override
    public Cache getCache(@NonNull String name) {
        Cache cache = super.getCache(name);
        return new RedisCacheWrapper(cache);
    }


    @Override
    public void afterPropertiesSet() {
        String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
        for (String beanName : beanNames) {
            final Class<?> clazz = applicationContext.getType(beanName);
            add(clazz);
        }
        super.afterPropertiesSet();
    }

    @Override
    protected @NonNull
    Collection<RedisCache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();
        for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
            caches.add(super.createRedisCache(entry.getKey(), entry.getValue()));
        }
        return caches;
    }

    private void add(final Class<?> clazz) {
        ReflectionUtils.doWithMethods(clazz, method -> {
            ReflectionUtils.makeAccessible(method);
            CacheExpire cacheExpire = AnnotationUtils.findAnnotation(method, CacheExpire.class);
            if (cacheExpire == null) {
                return;
            }
            Cacheable cacheable = AnnotationUtils.findAnnotation(method, Cacheable.class);
            if (cacheable != null) {
                add(cacheable.cacheNames(), cacheExpire);
                return;
            }
            Caching caching = AnnotationUtils.findAnnotation(method, Caching.class);
            if (caching != null) {
                for (Cacheable c : caching.cacheable()) {
                    add(c.cacheNames(), cacheExpire);
                }
                return;
            }
            CacheConfig cacheConfig = AnnotationUtils.findAnnotation(clazz, CacheConfig.class);
            if (cacheConfig != null) {
                add(cacheConfig.cacheNames(), cacheExpire);
            }
        }, method -> null != AnnotationUtils.findAnnotation(method, CacheExpire.class));
    }

    private void add(String[] cacheNames, CacheExpire cacheExpire) {
        for (String cacheName : cacheNames) {
            if (cacheName == null || "".equals(cacheName.trim())) {
                continue;
            }
            String expire = cacheExpire.expire();
            log.info("cacheName: {}, expire: {}", cacheName, expire);
            // 缓存配置
            RedisCacheConfiguration config = defaultCacheConfig().entryTtl(simpleParse(expire));
            initialCacheConfiguration.put(cacheName, config);
        }
    }

    private Duration simpleParse(@NonNull String time) {
        if (time.startsWith("-")) {
            return Duration.ofMillis(-1);
        }
        String timeLower = time.toLowerCase();
        long value = Long.parseLong(timeLower.substring(0, timeLower.length() - 2));
        if (timeLower.endsWith("ns")) {
            return Duration.ofNanos(value);
        }
        if (timeLower.endsWith("ms")) {
            return Duration.ofMillis(value);
        }
        value = Long.parseLong(timeLower.substring(0, timeLower.length() - 1));
        if (timeLower.endsWith("s")) {
            return Duration.ofSeconds(value);
        }
        if (timeLower.endsWith("m")) {
            return Duration.ofMinutes(value);
        }
        if (timeLower.endsWith("h")) {
            return Duration.ofHours(value);
        }
        if (timeLower.endsWith("d")) {
            return Duration.ofDays(value);
        }
        throw new DateTimeParseException("Unable to parse " + time + " into duration", time, 0);
    }

    private static RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(STRING_PAIR)
                .serializeValuesWith(FASTJSON_PAIR);
    }

    protected static class RedisCacheWrapper implements Cache {
        private final Cache cache;

        RedisCacheWrapper(Cache cache) {
            this.cache = cache;
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
                log.debug("cache get: {}", valueWrapper);
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
}
