package com.example;

import com.antelope.clustercache.autoconfigure.annotion.CacheExpire;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author yaml
 * @since 2021/6/28
 */
@Component
public class RedisCache {

    @CacheExpire("10m")
    @Cacheable(cacheNames = "test", key = "#key")
    public String get(String key) {
        return key;
    }
}
