package com.antelope.clustercache.autoconfigure;

import com.antelope.clustercache.autoconfigure.aspect.BatchCacheableAspect;
import com.antelope.clustercache.autoconfigure.aspect.processor.MemcachedBatchCacheableProcessor;
import com.antelope.clustercache.autoconfigure.aspect.processor.RedisBatchCacheableProcessor;
import com.antelope.clustercache.autoconfigure.core.DefaultKeyGenerator;
import com.antelope.clustercache.autoconfigure.memcached.FastJsonTranscoder;
import com.antelope.clustercache.autoconfigure.memcached.JacksonJsonTranscoder;
import com.antelope.clustercache.autoconfigure.memcached.MemCachedConfiguration;
import com.antelope.clustercache.autoconfigure.memcached.MemcacheCacheManager;
import com.antelope.clustercache.autoconfigure.redis.DefaultRedisCacheWriter;
import com.antelope.clustercache.autoconfigure.redis.RedisCacheConfiguration;
import com.antelope.clustercache.autoconfigure.redis.RedisCacheManager;
import com.antelope.clustercache.autoconfigure.support.CacheToolTemplate;
import com.antelope.clustercache.autoconfigure.support.MemCacheToolTemplate;
import com.antelope.clustercache.autoconfigure.support.RedisCacheToolTemplate;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClient;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.lang.NonNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static com.antelope.clustercache.autoconfigure.ClusterCacheProperties.SerialType.FASTJSON;
import static com.antelope.clustercache.autoconfigure.redis.RedisCacheManager.FASTJSON_SERIALIZER;
import static com.antelope.clustercache.autoconfigure.redis.RedisCacheManager.STRING_SERIALIZER;

/**
 * @author yaml
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ClusterCacheProperties.class)
@ConditionalOnProperty(prefix = "clustercache", value = "enable", havingValue = "true", matchIfMissing = true)
public class ClusterCacheAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("ClusterCacheAutoConfiguration init...");
    }

    @Slf4j
    @Configuration
    @ConditionalOnClass(RedisOperations.class)
    @ConditionalOnProperty(prefix = "clustercache", name = "cache-type", havingValue = "redis")
    public static class RedisCacheManagerConfiguration extends CachingConfigurerSupportAdapter {

        @Resource
        private com.antelope.clustercache.autoconfigure.ClusterCacheProperties cacheProperties;
        @Resource
        private RedisConnectionFactory redisConnectionFactory;

        @PostConstruct
        public void init() {
            log.info("RedisCacheManagerConfiguration init...");
        }

        @Override
        public KeyGenerator keyGenerator() {
            return keyGenerator(cacheProperties.getKeyGenerator());
        }

        /**
         * 配置 RedisCacheManager，使用 cache 注解管理 redis 缓存
         */
        @Bean
        @Override
        public CacheManager cacheManager() {
            log.info("RedisCacheManager init...");
            return new RedisCacheManager(DefaultRedisCacheWriter.getInstance(redisConnectionFactory), defaultRedisCacheConfiguration());
        }

        @Bean
        @ConditionalOnMissingBean(BatchCacheableAspect.class)
        public BatchCacheableAspect batchCacheableAspect() {
            RedisBatchCacheableProcessor batchCacheableProcessor = new RedisBatchCacheableProcessor(cacheManager());
            return new BatchCacheableAspect(batchCacheableProcessor);
        }

        @Bean
        @ConditionalOnMissingBean(CacheToolTemplate.class)
        public CacheToolTemplate cacheToolTemplate(StringRedisTemplate stringRedisTemplate) {
            log.info("RedisCacheToolTemplate init...");
            return new RedisCacheToolTemplate(cacheProperties, stringRedisTemplate);
        }

        private RedisCacheConfiguration defaultRedisCacheConfiguration() {
            RedisSerializationContext.SerializationPair<Object> serializationPair =
                    cacheProperties.getSerialType() == FASTJSON
                            ? RedisCacheManager.FASTJSON_PAIR
                            : RedisCacheManager.JACKSON_PAIR;
            return RedisCacheConfiguration.defaultCacheConfig()
                    .setTtl(cacheProperties.getDefaultTtl())
                    .setKeyPrefix(cacheProperties.getKeyPrefix())
                    .setKeySeparator(cacheProperties.getKeySeparator())
                    .setCacheNullValues(cacheProperties.isCacheNullValues())
                    .setKeySerializationPair(RedisCacheManager.STRING_PAIR)
                    .setValueSerializationPair(serializationPair);
        }

    }

    @Slf4j
    @Configuration
    @ConditionalOnClass(XMemcachedClient.class)
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "clustercache", name = "cache-type", havingValue = "memcached")
    public static class MemcachedCacheManagerConfiguration extends CachingConfigurerSupportAdapter {

        @Resource
        private com.antelope.clustercache.autoconfigure.ClusterCacheProperties cacheProperties;


        @PostConstruct
        public void init() {
            log.info("MemcachedCacheManagerConfiguration init...");
        }

        @Override
        public KeyGenerator keyGenerator() {
            return keyGenerator(cacheProperties.getKeyGenerator());
        }

        @Bean
        @Override
        public CacheManager cacheManager() {
            log.info("MemcacheCacheManager init...");
            MemCachedConfiguration memCachedConfiguration = MemCachedConfiguration.defaultCacheConfig()
                    .setTtl(cacheProperties.getDefaultTtl())
                    .setKeyPrefix(cacheProperties.getKeyPrefix())
                    .setKeySeparator(cacheProperties.getKeySeparator())
                    .setCacheNullValues(cacheProperties.isCacheNullValues());
            return new MemcacheCacheManager(memcachedClient(), memCachedConfiguration);
        }

        @Bean
        @ConditionalOnMissingBean
        public MemcachedClient memcachedClient() {
            com.antelope.clustercache.autoconfigure.ClusterCacheProperties.Memcached properties = cacheProperties.getMemcached();
            List<InetSocketAddress> addresses = AddrUtil.getAddresses(String.join(" ", properties.getHosts()));
            XMemcachedClientBuilder builder = new XMemcachedClientBuilder(addresses, properties.getWeight());

            SerializingTranscoder transcoder = cacheProperties.getSerialType() == FASTJSON
                    ? FastJsonTranscoder.getInstance()
                    : JacksonJsonTranscoder.getInstance();
            builder.setTranscoder(transcoder);
            try {
                return builder.build();
            } catch (IOException e) {
                throw new RuntimeException("memcachedClient build error", e);
            }
        }

        @Bean
        @ConditionalOnMissingBean(BatchCacheableAspect.class)
        public BatchCacheableAspect batchCacheableAspect() {
            log.info("BatchCacheableAspect init...");
            MemcachedBatchCacheableProcessor batchCacheableProcessor = new MemcachedBatchCacheableProcessor(cacheManager());
            return new BatchCacheableAspect(batchCacheableProcessor);
        }

        @Bean
        @ConditionalOnMissingBean(CacheToolTemplate.class)
        public CacheToolTemplate cacheToolTemplate() {
            log.info("MemCacheToolTemplate init...");
            return new MemCacheToolTemplate(cacheProperties, memcachedClient());
        }

    }

    @Slf4j
    @Configuration
    @ConditionalOnClass(RedisOperations.class)
    @ConditionalOnProperty(prefix = "spring.redis", name = "enable", havingValue = "true", matchIfMissing = true)
    public static class RedisTemplateConfiguration {

        @Resource
        private RedisConnectionFactory redisConnectionFactory;

        @PostConstruct
        public void init() {
            log.info("RedisTemplateConfiguration init...");
        }

        @Bean
        @ConditionalOnMissingBean(StringRedisTemplate.class)
        public StringRedisTemplate stringRedisTemplate() {
            log.info("StringRedisTemplate init...");
            return new StringRedisTemplate(redisConnectionFactory);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate() {
            log.info("RedisTemplate<String, Object> init...");
            RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            redisTemplate.setConnectionFactory(redisConnectionFactory);
            redisTemplate.setKeySerializer(STRING_SERIALIZER);
            redisTemplate.setHashKeySerializer(STRING_SERIALIZER);
            redisTemplate.setValueSerializer(FASTJSON_SERIALIZER);
            redisTemplate.setHashValueSerializer(FASTJSON_SERIALIZER);
            redisTemplate.setDefaultSerializer(FASTJSON_SERIALIZER);
            return redisTemplate;
        }
    }

    @Slf4j
    private static abstract class CachingConfigurerSupportAdapter extends CachingConfigurerSupport {

        protected KeyGenerator keyGenerator(Class<? extends KeyGenerator> clazz) {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                log.error("key generator 【{}】 instance fail: {}", clazz, e.getMessage());
            }
            return new DefaultKeyGenerator();
        }

        @Override
        public CacheErrorHandler errorHandler() {
            return new CacheErrorHandler() {
                @Override
                public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                    log.error("cache get error --> cacheName:{}, key:{}, msg:{}", cache.getName(), key, exception.getMessage(), exception);
                }

                @Override
                public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                    log.error("cache put error --> cacheName:{}, key:{}, value:{}, msg:{}", cache.getName(), key, value, exception.getMessage(), exception);
                }

                @Override
                public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                    log.error("cache evict error --> cacheName:{}, key:{}, msg:{}", cache.getName(), key, exception.getMessage(), exception);
                }

                @Override
                public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                    log.error("cache clear error --> cacheName:{}, msg:{}", cache.getName(), exception.getMessage(), exception);
                }
            };
        }
    }
}
