package com.antelope.clustercache.autoconfigure.memcached;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

import java.time.Duration;

/**
 * @author yaml
 * @since 2021/7/1
 */
@Getter
@Setter
@Accessors(chain = true)
public class MemCachedConfiguration {

    private Duration ttl;
    private boolean cacheNullValues;
    private String keyPrefix;
    private String keySeparator;
    private ConversionService conversionService;

    private MemCachedConfiguration(Duration ttl, Boolean cacheNullValues, String keyPrefix, String keySeparator, ConversionService conversionService) {
        this.ttl = ttl;
        this.cacheNullValues = cacheNullValues;
        this.keyPrefix = keyPrefix;
        this.keySeparator = keySeparator;
        this.conversionService = conversionService;
    }

    public static MemCachedConfiguration defaultCacheConfig() {
        return new MemCachedConfiguration(Duration.ZERO, true, "", ":", new DefaultFormattingConversionService());
    }

    public MemCachedConfiguration entryTtl(Duration ttl) {
        return new MemCachedConfiguration(ttl, this.cacheNullValues, this.keyPrefix, this.keySeparator, this.conversionService);
    }
}
