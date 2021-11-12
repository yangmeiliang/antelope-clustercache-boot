package com.antelope.clustercache.autoconfigure.redis;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author yaml
 * @since 2021/8/6
 */
@Getter
@Setter
@Accessors(chain = true)
public class RedisCacheConfiguration {
    private Duration ttl;
    private String keyPrefix;
    private String keySeparator;
    private boolean cacheNullValues;
    private ConversionService conversionService;

    private RedisSerializationContext.SerializationPair<String> keySerializationPair;
    private RedisSerializationContext.SerializationPair<Object> valueSerializationPair;

    private RedisCacheConfiguration(Duration ttl,
                                    String keyPrefix,
                                    String keySeparator,
                                    Boolean cacheNullValues,
                                    RedisSerializationContext.SerializationPair<String> keySerializationPair,
                                    RedisSerializationContext.SerializationPair<Object> valueSerializationPair,
                                    ConversionService conversionService) {

        this.ttl = ttl;
        this.cacheNullValues = cacheNullValues;
        this.keyPrefix = keyPrefix;
        this.keySeparator = keySeparator;
        this.keySerializationPair = keySerializationPair;
        this.valueSerializationPair = valueSerializationPair;
        this.conversionService = conversionService;
    }

    public static RedisCacheConfiguration defaultCacheConfig() {
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService();
        registerDefaultConverters(conversionService);
        return new RedisCacheConfiguration(Duration.ZERO, "", ":", true,
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()),
                RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()), conversionService);
    }

    public RedisCacheConfiguration entryTtl(Duration ttl) {
        Assert.notNull(ttl, "TTL duration must not be null!");
        return new RedisCacheConfiguration(ttl, keyPrefix, keySeparator, cacheNullValues, keySerializationPair, valueSerializationPair, conversionService);
    }


    public static void registerDefaultConverters(ConverterRegistry registry) {
        Assert.notNull(registry, "ConverterRegistry must not be null!");
        registry.addConverter(String.class, byte[].class, source -> source.getBytes(StandardCharsets.UTF_8));
        registry.addConverter(SimpleKey.class, String.class, SimpleKey::toString);
    }
}
