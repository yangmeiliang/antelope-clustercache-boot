package com.antelope.clustercache.autoconfigure.memcached;

import com.antelope.clustercache.autoconfigure.annotion.CacheExpire;
import com.antelope.clustercache.autoconfigure.core.CacheWrapper;
import com.antelope.clustercache.autoconfigure.util.CacheUtil;
import com.antelope.clustercache.autoconfigure.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yaml
 * @since 2021/6/29
 */
@Slf4j
public class MemcacheCacheManager extends AbstractTransactionSupportingCacheManager implements ApplicationContextAware, InitializingBean {

    private ApplicationContext applicationContext;
    private final MemcachedClient memcachedClient;
    private final MemCachedConfiguration defaultCacheConfiguration;
    private final Map<String, MemCachedConfiguration> initialCacheConfiguration = new LinkedHashMap<>();


    public MemcacheCacheManager(MemcachedClient memcachedClient, MemCachedConfiguration defaultCacheConfiguration) {
        this.memcachedClient = memcachedClient;
        this.defaultCacheConfiguration = defaultCacheConfiguration;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, CacheExpire> cacheExpireMap = CacheUtil.resolveCacheExpire(applicationContext);
        cacheExpireMap.forEach(this::add);
        super.afterPropertiesSet();
    }

    @Override
    protected Collection<? extends Cache> loadCaches() {
        List<MemCache> memCaches = new ArrayList<>();
        initialCacheConfiguration.forEach((name, config) -> {
            MemCache memCache = new MemCache(name, this.memcachedClient, config);
            memCaches.add(memCache);
        });
        return memCaches;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = super.getCache(name);
        return new CacheWrapper(cache);
    }

    private void add(String cacheName, CacheExpire cacheExpire) {
        if (cacheName == null || "".equals(cacheName.trim())) {
            return;
        }
        MemCachedConfiguration cacheConfiguration = this.defaultCacheConfiguration;
        if (cacheExpire != null) {
            cacheConfiguration = cacheConfiguration.entryTtl(TimeUtil.simpleParse(cacheExpire.expire()));
        }
        initialCacheConfiguration.put(cacheName, cacheConfiguration);
        log.info("cacheName: {}, expire: {}", cacheName, cacheConfiguration.getTtl());
    }
}
