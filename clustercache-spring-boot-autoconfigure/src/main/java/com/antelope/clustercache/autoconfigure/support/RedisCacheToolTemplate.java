package com.antelope.clustercache.autoconfigure.support;

import com.antelope.clustercache.autoconfigure.ClusterCacheProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @author yaml
 * @since 2021/8/11
 */
@Slf4j
public class RedisCacheToolTemplate extends CacheToolTemplate {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheToolTemplate(ClusterCacheProperties cacheProperties, StringRedisTemplate stringRedisTemplate) {
        super(cacheProperties);
        this.redisTemplate = stringRedisTemplate;
    }

    @Override
    public String get(String key) {
        try {
            key = generateKey(key);
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("cache get error, key: {}", key, e);
        }
        return null;
    }

    @Override
    public boolean set(String key, String value, int expireTime) {
        try {
            key = generateKey(key);
            redisTemplate.opsForValue().set(key, value);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.error("cache set error, key: {}, value: {}, expireTime: {}", key, value, expireTime, e);
        }
        return false;
    }

    @Override
    public long incr(String key, int incrValue, int initValue, int expireTime) {
        try {
            key = generateKey(key);
            Long increment = redisTemplate.opsForValue().increment(key, incrValue);
            redisTemplate.expire(key, expireTime, TimeUnit.SECONDS);
            return increment == null ? initValue : increment;
        } catch (Exception e) {
            log.error("cache incr error, key: {}", key, e);
        }
        return initValue;
    }

    @Override
    public boolean del(String key) {
        try {
            key = generateKey(key);
            Boolean delete = redisTemplate.delete(key);
            return delete != null && delete;
        } catch (Exception e) {
            log.error("cache del error, key: {}", key, e);
        }
        return false;
    }
}
