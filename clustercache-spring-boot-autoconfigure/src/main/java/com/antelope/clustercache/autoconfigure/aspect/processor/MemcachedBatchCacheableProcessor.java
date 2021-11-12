package com.antelope.clustercache.autoconfigure.aspect.processor;

import com.antelope.clustercache.autoconfigure.core.CacheWrapper;
import com.antelope.clustercache.autoconfigure.memcached.MemCache;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yaml
 * @since 2021/8/5
 */
@Slf4j
@AllArgsConstructor
public class MemcachedBatchCacheableProcessor implements AbstractBatchCacheableProcessor {

    private final CacheManager cacheManager;

    @Override
    public Map<String, Object> mGet(String cacheName, String prefix, List<String> keyCollection) {
        try {
            CacheWrapper cache = (CacheWrapper) cacheManager.getCache(cacheName);
            assert cache != null;
            MemCache memCache = (MemCache) cache.getCache();
            MemcachedClient memcachedClient = memCache.getMemcachedClient();
            // key加前缀 k：方法入参集合原始值 v: 方法入参集合原始值加前缀
            Map<String, String> keyCollectionMap = keyCollection.stream().collect(Collectors.toMap(Function.identity(), prefix::concat, (o1, o2) -> o2));
            // 生成最终的key k：带前缀的方法入参集合原始值 v: 实际缓存key
            Map<String, String> keyMap = memCache.generateCacheKeyCollection(keyCollectionMap.values());
            // 查询缓存 k：实际缓存key v: 缓存值
            Map<String, Object> objectMap = memcachedClient.get(keyMap.values());
            if (objectMap == null) {
                return Collections.emptyMap();
            }
            Map<String, Object> result = new HashMap<>(objectMap.size());
            keyCollectionMap.forEach((key, value) -> {
                String finalKey = keyMap.get(value);
                Optional.ofNullable(objectMap.get(finalKey)).ifPresent(o -> result.put(key, o));
            });
            return result;
        } catch (Exception e) {
            log.warn("MemcachedBatchCacheableProcessor mget error!", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void mSet(String cacheName, String prefix, Map<String, Object> data) {
        CacheWrapper cache = (CacheWrapper) cacheManager.getCache(cacheName);
        assert cache != null;
        MemCache memCache = (MemCache) cache.getCache();
        data.forEach((key, value) -> memCache.put(prefix.concat(key), value));
    }
}
