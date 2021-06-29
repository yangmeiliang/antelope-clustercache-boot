package com.antelope.clustercache.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;

import javax.annotation.Resource;

import static com.antelope.clustercache.autoconfigure.RedisCacheManager.FASTJSON_SERIALIZER;
import static com.antelope.clustercache.autoconfigure.RedisCacheManager.STRING_SERIALIZER;

/**
 * @author yaml
 */
@Slf4j
@Configuration
public class CacheAutoConfiguration extends CachingConfigurerSupport {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(STRING_SERIALIZER);
        redisTemplate.setHashKeySerializer(STRING_SERIALIZER);

        redisTemplate.setValueSerializer(FASTJSON_SERIALIZER);
        redisTemplate.setHashValueSerializer(FASTJSON_SERIALIZER);

        redisTemplate.setDefaultSerializer(FASTJSON_SERIALIZER);
        log.info("初始化 redisTemplate 完成... RedisConnectionFactory:{}", redisConnectionFactory);
        return redisTemplate;
    }

    /**
     * 配置 RedisCacheManager，使用 cache 注解管理 redis 缓存
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        return new RedisCacheManager(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("cache get error --> cacheName:{}, key:{}, msg:{}", cache.getName(), key, exception.getMessage(), exception);
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.error("cache put error --> cacheName:{}, key:{}, value:{}, msg:{}", cache.getName(), key, value, exception.getMessage(), exception);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("cache evict error --> cacheName:{}, key:{}, msg:{}", cache.getName(), key, exception.getMessage(), exception);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.error("cache clear error --> cacheName:{}, msg:{}", cache.getName(), exception.getMessage(), exception);
            }
        };
    }
}
