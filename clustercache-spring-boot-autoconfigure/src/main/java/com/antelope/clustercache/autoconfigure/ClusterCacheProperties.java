package com.antelope.clustercache.autoconfigure;

import com.antelope.clustercache.autoconfigure.core.DefaultKeyGenerator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.interceptor.KeyGenerator;

import java.time.Duration;

/**
 * @author yaml
 * @since 2021/7/2
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "clustercache")
public class ClusterCacheProperties {
    /**
     * 是否启用缓存组件
     */
    private boolean enable = true;
    /**
     * 缓存key前缀
     */
    private String keyPrefix;
    /**
     * 缓存key分隔符
     */
    private String keySeparator = ":";
    /**
     * 是否缓存null值
     */
    private boolean cacheNullValues = true;
    /**
     * 默认过期时间
     */
    private Duration defaultTtl = Duration.ofHours(1L);
    /**
     * 当未指定cache key时，默认生成的key => all
     */
    private Class<? extends KeyGenerator> keyGenerator = DefaultKeyGenerator.class;
    /**
     * 序列化方式 jackson | fastjson
     */
    private SerialType serialType = SerialType.JACKSON;
    /**
     * 缓存类型 redis | memcached
     */
    private CacheType cacheType = CacheType.REDIS;
    private Memcached memcached = new Memcached();

    @SuppressWarnings("AlibabaEnumConstantsMustHaveComment")
    enum CacheType {
        MEMCACHED, REDIS
    }

    @SuppressWarnings("AlibabaEnumConstantsMustHaveComment")
    enum SerialType {
        JACKSON, FASTJSON
    }

    @Getter
    @Setter
    public static class Memcached {

        private String[] hosts = new String[]{"127.0.0.1:11211"};
        private int[] weight;
        private String username;
        private String password;
    }
}