package com.antelope.clustercache.autoconfigure.support;

import com.antelope.clustercache.autoconfigure.ClusterCacheProperties;
import com.antelope.clustercache.autoconfigure.core.FinallyKeyGenerator;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author yaml
 * @since 2021/8/11
 */
@Slf4j
public abstract class CacheToolTemplate {

    protected final ClusterCacheProperties cacheProperties;

    protected final Integer defaultExpireTime;

    public CacheToolTemplate(ClusterCacheProperties cacheProperties) {
        this.cacheProperties = cacheProperties;
        this.defaultExpireTime = (int) cacheProperties.getDefaultTtl().getSeconds();
    }

    public boolean exist(String key) {
        return !StringUtils.isEmpty(get(key));
    }

    public <T> T get(String key, Class<T> clazz, T defaultValue) {
        T value = get(key, clazz);
        return value == null ? defaultValue : value;
    }

    public <T> T get(String key, Class<T> clazz) {
        String json = get(key);
        try {
            return JSON.parseObject(json, clazz);
        } catch (Exception e) {
            log.error("cache <T> get error, key: {},json: {}", key, json, e);
        }
        return null;
    }

    public <T> List<T> getCollection(String key, Class<T> clazz) {
        return JSON.parseArray(get(key), clazz);
    }

    public boolean set(String key, String value) {
        return set(key, value, defaultExpireTime);
    }

    public boolean set(String key, Object value) {
        return set(key, value, defaultExpireTime);
    }

    public boolean set(String key, Object value, int expireTime) {
        return set(key, JSON.toJSONString(value), expireTime);
    }


    public abstract String get(String key);

    public abstract boolean set(String key, String value, int expireTime);

    public long incr(String key) {
        return incr(key, 1, 1, defaultExpireTime);
    }

    public long incr(String key, int expireTime) {
        return incr(key, 1, 1, expireTime);
    }

    public abstract long incr(String key, int incrValue, int initValue, int expireTime);

    public abstract boolean del(String key);

    public String generateKey(String key) {
        return FinallyKeyGenerator.getInstance(cacheProperties.getKeyPrefix(), cacheProperties.getKeySeparator()).generate(key);
    }

}
