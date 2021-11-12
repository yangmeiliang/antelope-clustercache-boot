package com.antelope.clustercache.autoconfigure.redis;

import com.antelope.clustercache.autoconfigure.annotion.CacheExpire;
import com.antelope.clustercache.autoconfigure.core.CacheWrapper;
import com.antelope.clustercache.autoconfigure.core.ObjectMapperFactory;
import com.antelope.clustercache.autoconfigure.util.CacheUtil;
import com.antelope.clustercache.autoconfigure.util.TimeUtil;
import com.alibaba.fastjson.support.spring.GenericFastJsonRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class RedisCacheManager extends AbstractTransactionSupportingCacheManager implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;

    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfiguration;
    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration;
    private final boolean allowInFlightCacheCreation;

    public static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.newInstance();

    public static final StringRedisSerializer STRING_SERIALIZER = new StringRedisSerializer();
    public static final GenericFastJsonRedisSerializer FASTJSON_SERIALIZER = new GenericFastJsonRedisSerializer();
    public static final GenericJackson2JsonRedisSerializer JACKSON_SERIALIZER = new GenericJackson2JsonRedisSerializer(OBJECT_MAPPER);
    public static final RedisSerializationContext.SerializationPair<String> STRING_PAIR = RedisSerializationContext.SerializationPair.fromSerializer(STRING_SERIALIZER);
    public static final RedisSerializationContext.SerializationPair<Object> FASTJSON_PAIR = RedisSerializationContext.SerializationPair.fromSerializer(FASTJSON_SERIALIZER);
    public static final RedisSerializationContext.SerializationPair<Object> JACKSON_PAIR = RedisSerializationContext.SerializationPair.fromSerializer(JACKSON_SERIALIZER);


    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private RedisCacheManager(@NonNull RedisCacheWriter cacheWriter,
                              @NonNull RedisCacheConfiguration cacheConfiguration,
                              boolean allowInFlightCacheCreation) {

        this.cacheWriter = cacheWriter;
        this.cacheConfiguration = cacheConfiguration;
        this.initialCacheConfiguration = new LinkedHashMap<>();
        this.allowInFlightCacheCreation = allowInFlightCacheCreation;
    }


    public RedisCacheManager(RedisCacheWriter cacheWriter, RedisCacheConfiguration defaultCacheConfiguration) {
        this(cacheWriter, defaultCacheConfiguration, true);
    }

    @NonNull
    @Override
    protected Collection<RedisCache> loadCaches() {
        List<RedisCache> caches = new LinkedList<>();
        for (Map.Entry<String, RedisCacheConfiguration> entry : initialCacheConfiguration.entrySet()) {
            caches.add(createRedisCache(entry.getKey(), entry.getValue()));
        }
        return caches;
    }

    @Override
    protected RedisCache getMissingCache(@NonNull String name) {
        return allowInFlightCacheCreation ? createRedisCache(name, cacheConfiguration) : null;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, CacheExpire> cacheExpireMap = CacheUtil.resolveCacheExpire(applicationContext);
        cacheExpireMap.forEach((cacheName, cacheExpire) -> {
            RedisCacheConfiguration cacheConfig = this.cacheConfiguration;
            if (cacheExpire != null) {
                cacheConfig = cacheConfig.entryTtl(TimeUtil.simpleParse(cacheExpire.expire()));
            }
            initialCacheConfiguration.put(cacheName, cacheConfig);
            log.info("cacheName: {}, expire: {}", cacheName, cacheConfig.getTtl());
        });

        super.afterPropertiesSet();
    }

    @Override
    public Cache getCache(@NonNull String name) {
        Cache cache = super.getCache(name);
        return new CacheWrapper(cache);
    }

    protected RedisCache createRedisCache(String name, @Nullable RedisCacheConfiguration cacheConfig) {
        return new RedisCache(name, cacheWriter, cacheConfig != null ? cacheConfig : cacheConfiguration);
    }


}
