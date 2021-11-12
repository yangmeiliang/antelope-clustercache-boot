package com.antelope.clustercache.autoconfigure.aspect.processor;

import com.antelope.clustercache.autoconfigure.core.CacheWrapper;
import lombok.AllArgsConstructor;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author yaml
 * @since 2021/8/5
 */
@AllArgsConstructor
public class RedisBatchCacheableProcessor implements AbstractBatchCacheableProcessor {

    private final CacheManager redisCacheManager;

    @Override
    public Map<String, Object> mGet(String cacheName, String prefix, List<String> keyCollection) {
        CacheWrapper cache = (CacheWrapper) redisCacheManager.getCache(cacheName);
        assert cache != null;
        Map<String, Object> result = new HashMap<>(keyCollection.size());
        keyCollection.parallelStream().forEach(key -> Optional.ofNullable(cache.get(prefix.concat(key), Object.class)).ifPresent(o -> result.put(key, o)));
        return result;
    }

    @Override
    public void mSet(String cacheName, String prefix, Map<String, Object> data) {
        CacheWrapper cache = (CacheWrapper) redisCacheManager.getCache(cacheName);
        assert cache != null;
        data.forEach((key, value) -> cache.put(prefix.concat(key), value));
    }
}
