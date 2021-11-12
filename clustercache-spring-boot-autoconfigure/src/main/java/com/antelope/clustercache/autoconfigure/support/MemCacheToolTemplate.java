package com.antelope.clustercache.autoconfigure.support;

import com.antelope.clustercache.autoconfigure.ClusterCacheProperties;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;

import static net.rubyeye.xmemcached.MemcachedClient.DEFAULT_OP_TIMEOUT;

/**
 * @author yaml
 * @since 2021/8/11
 */
@Slf4j
public class MemCacheToolTemplate extends CacheToolTemplate {

    private final MemcachedClient memcachedClient;

    public MemCacheToolTemplate(ClusterCacheProperties cacheProperties, MemcachedClient memcachedClient) {
        super(cacheProperties);
        this.memcachedClient = memcachedClient;
    }

    @Override
    public String get(String key) {
        try {
            key = generateKey(key);
            return memcachedClient.get(key);
        } catch (Exception e) {
            log.error("cache get error, key: {}", key, e);
        }
        return null;
    }

    @Override
    public boolean set(String key, String value, int expireTime) {
        try {
            key = generateKey(key);
            return memcachedClient.set(key, expireTime, value);
        } catch (Exception e) {
            log.error("cache set error, key: {}, value: {}, expireTime: {}", key, value, expireTime, e);
        }
        return false;
    }

    @Override
    public long incr(String key, int incrValue, int initValue, int expireTime) {
        try {
            key = generateKey(key);
            return memcachedClient.incr(key, incrValue, initValue, DEFAULT_OP_TIMEOUT, expireTime);
        } catch (Exception e) {
            log.error("cache incr error, key: {}", key, e);
        }
        return 0;
    }

    @Override
    public boolean del(String key) {
        try {
            key = generateKey(key);
            return memcachedClient.delete(key);
        } catch (Exception e) {
            log.error("cache del error, key: {}", key, e);
        }
        return false;
    }
}
