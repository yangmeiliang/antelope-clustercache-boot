package com.antelope.clustercache.autoconfigure.redis;

import com.antelope.clustercache.autoconfigure.core.AbstractValueAdaptingCache;
import com.antelope.clustercache.autoconfigure.core.FinallyKeyGenerator;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NullValue;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.util.ByteUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.nio.ByteBuffer;

/**
 * @author yaml
 * @since 2021/8/6
 */
public class RedisCache extends AbstractValueAdaptingCache {

    private static final byte[] BINARY_NULL_VALUE = new JdkSerializationRedisSerializer().serialize(NullValue.INSTANCE);

    private final RedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration cacheConfig;
    private final FinallyKeyGenerator keyGenerator;

    protected RedisCache(@NonNull String name, @NonNull RedisCacheWriter cacheWriter, @NonNull RedisCacheConfiguration cacheConfig) {

        super(name, cacheConfig.getTtl(), cacheConfig.getConversionService(), cacheConfig.isCacheNullValues());
        this.cacheWriter = cacheWriter;
        this.cacheConfig = cacheConfig;
        this.keyGenerator = FinallyKeyGenerator.getInstance(name, cacheConfig.getKeyPrefix(), cacheConfig.getKeySeparator());
    }

    @Override
    protected Object lookup(@NonNull Object key) {
        byte[] value = cacheWriter.get(name, createAndConvertCacheKey(key));
        if (value == null) {
            return null;
        }
        return deserializeCacheValue(value);
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public RedisCacheWriter getNativeCache() {
        return this.cacheWriter;
    }

    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        Object cacheValue = preProcessCacheValue(value);
        if (!isAllowNullValues() && cacheValue == null) {
            throw new IllegalArgumentException(String.format("Cache '%s' does not allow 'null' values. Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure RedisCache to allow 'null' via RedisCacheConfiguration.", name));
        }
        cacheWriter.put(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue), cacheConfig.getTtl());
    }

    @Override
    public Cache.ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {

        Object cacheValue = preProcessCacheValue(value);
        if (!isAllowNullValues() && cacheValue == null) {
            return get(key);
        }
        byte[] result = cacheWriter.putIfAbsent(name, createAndConvertCacheKey(key), serializeCacheValue(cacheValue), cacheConfig.getTtl());
        if (result == null) {
            return null;
        }
        return new SimpleValueWrapper(fromStoreValue(deserializeCacheValue(result)));
    }


    @Override
    public void evict(@NonNull Object key) {
        cacheWriter.remove(name, createAndConvertCacheKey(key));
    }

    @Override
    public void clear() {
        byte[] pattern = conversionService.convert(createCacheKey("*"), byte[].class);
        cacheWriter.clean(name, pattern);
    }

    @Nullable
    protected Object preProcessCacheValue(@Nullable Object value) {

        if (value != null) {
            return value;
        }

        return isAllowNullValues() ? NullValue.INSTANCE : null;
    }

    protected byte[] serializeCacheKey(String cacheKey) {
        return ByteUtils.getBytes(cacheConfig.getKeySerializationPair().write(cacheKey));
    }

    protected byte[] serializeCacheValue(Object value) {

        if (isAllowNullValues() && value instanceof NullValue) {
            return BINARY_NULL_VALUE;
        }

        return ByteUtils.getBytes(cacheConfig.getValueSerializationPair().write(value));
    }

    @Nullable
    protected Object deserializeCacheValue(byte[] value) {
        if (isAllowNullValues() && ObjectUtils.nullSafeEquals(value, BINARY_NULL_VALUE)) {
            return NullValue.INSTANCE;
        }

        return cacheConfig.getValueSerializationPair().read(ByteBuffer.wrap(value));
    }

    protected String createCacheKey(Object key) {
        return keyGenerator.generate(convertKey(key));
    }

    private byte[] createAndConvertCacheKey(Object key) {
        return serializeCacheKey(createCacheKey(key));
    }
}
