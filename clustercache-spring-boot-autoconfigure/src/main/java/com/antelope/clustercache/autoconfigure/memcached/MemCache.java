package com.antelope.clustercache.autoconfigure.memcached;

import com.antelope.clustercache.autoconfigure.ClusterCacheException;
import com.antelope.clustercache.autoconfigure.core.AbstractValueAdaptingCache;
import com.antelope.clustercache.autoconfigure.core.FinallyKeyGenerator;
import lombok.SneakyThrows;
import net.rubyeye.xmemcached.MemcachedClient;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.antelope.clustercache.autoconfigure.Constants.KEY_PREFIX_NAME_VERSION;
import static net.rubyeye.xmemcached.MemcachedClient.DEFAULT_OP_TIMEOUT;

/**
 * @author yaml
 * @since 2021/7/2
 */
public class MemCache extends AbstractValueAdaptingCache {

    private final MemcachedClient memcachedClient;
    private final FinallyKeyGenerator keyGenerator;

    protected MemCache(@NonNull String name,
                       @NonNull MemcachedClient memcachedClient,
                       @NonNull MemCachedConfiguration cacheConfig) {
        super(name, cacheConfig.getTtl(), cacheConfig.getConversionService(), cacheConfig.isCacheNullValues());
        this.memcachedClient = memcachedClient;
        this.keyGenerator = FinallyKeyGenerator.getInstance(name, cacheConfig.getKeyPrefix(), cacheConfig.getKeySeparator());
    }

    public MemcachedClient getMemcachedClient() {
        return memcachedClient;
    }


    @Override
    @SneakyThrows
    protected Object lookup(@NonNull Object key) {
        return deserializeCacheValue(memcachedClient.get(generateCacheKey(key)));
    }

    private String generateCacheKey(Object key, String nameVersion) {
        return keyGenerator.generate(convertKey(key), nameVersion);
    }

    private String generateCacheKey(Object key) {
        return generateCacheKey(key, getOrInitNameVersion());
    }

    public <T> Map<T, String> generateCacheKeyCollection(Collection<T> keyCollection) {
        String nameVersion = getOrInitNameVersion();
        return keyCollection.stream().collect(Collectors.toMap(Function.identity(), key -> generateCacheKey(key, nameVersion), (o1, o2) -> o2));
    }

    private String generateNameVersionKey() {
        return keyGenerator.generate(KEY_PREFIX_NAME_VERSION);
    }

    @SneakyThrows
    private String getOrInitNameVersion() {
        String nameVersionKey = generateNameVersionKey();
        Object version = memcachedClient.get(nameVersionKey);
        if (version == null) {
            version = "1";
            memcachedClient.incr(nameVersionKey, 0, 1, DEFAULT_OP_TIMEOUT, 0);
        }
        return version.toString();
    }

    @NonNull
    @Override
    public Object getNativeCache() {
        return memcachedClient;
    }


    @Override
    @SneakyThrows
    public void put(@NonNull Object key, Object value) {
        if (value == null && !this.isAllowNullValues()) {
            throw ClusterCacheException.create(String.format("Cache '%s' not allow 'null' values.", name));
        }
        memcachedClient.set(generateCacheKey(key), (int) this.ttl.getSeconds(), serializeCacheValue(value));
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(@NonNull Object key, Object value) {
        throw ClusterCacheException.create("memcached not support this operation");
    }

    @Override
    @SneakyThrows
    public void evict(@NonNull Object key) {
        Assert.notNull(key, "Key must not be null!");
        memcachedClient.delete(generateCacheKey(key));
    }

    @Override
    @SneakyThrows
    public void clear() {
        memcachedClient.incr(generateNameVersionKey(), 1L, 1L, DEFAULT_OP_TIMEOUT, 0);
    }


    private Object deserializeCacheValue(Object value) {
        if (isAllowNullValues() && value instanceof NullValue) {
            return NullValue.INSTANCE;
        }
        return value;
    }

    private Object serializeCacheValue(Object value) {
        if (isAllowNullValues() && value == null) {
            return NullValue.INSTANCE;
        }
        return value;
    }
}
