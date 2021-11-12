package com.antelope.clustercache.autoconfigure.aspect;

import com.antelope.clustercache.autoconfigure.annotion.BatchCacheable;
import com.antelope.clustercache.autoconfigure.aspect.processor.AbstractBatchCacheableProcessor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.support.NullValue;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author yaml
 * @since 2021/8/4
 */
@Slf4j
@Aspect
public class BatchCacheableAspect {

    private final AbstractBatchCacheableProcessor batchCacheableProcessor;

    public BatchCacheableAspect(AbstractBatchCacheableProcessor batchCacheableProcessor) {
        this.batchCacheableProcessor = batchCacheableProcessor;
    }

    @Pointcut("@annotation(cacheable)")
    public void point(BatchCacheable cacheable) {

    }

    @SuppressWarnings({"rawtypes"})
    @Around(value = "point(cacheable)", argNames = "joinPoint,cacheable")
    public Object around(ProceedingJoinPoint joinPoint, BatchCacheable cacheable) throws Throwable {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class returnType = signature.getReturnType();

        // 返回值类型是否为集合
        boolean isCollection = Collection.class.isAssignableFrom(returnType);
        // 非集合返回
        if (!isCollection) {
            log.warn("@BatchCacheable返回值应当为集合，若非集合请使用@Cacheable");
            return joinPoint.proceed();
        }
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.warn("@BatchCacheable参数列表不能为空，若为空请使用@Cacheable");
            return joinPoint.proceed();
        }
        Object arg = args[0];
        if (arg == null) {
            return joinPoint.proceed();
        }
        if (!Collection.class.isAssignableFrom(arg.getClass())) {
            log.warn("@BatchCacheable第一位参数值应当为集合");
            return joinPoint.proceed();
        }
        List<String> keyCollection = new ArrayList<>();
        for (Object o : ((Collection) arg)) {
            Optional.ofNullable(o).map(Objects::toString).ifPresent(keyCollection::add);
        }
        if (keyCollection.isEmpty()) {
            return joinPoint.proceed();
        }
        try {
            return doProcess(joinPoint, cacheable, returnType, arg, keyCollection);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return joinPoint.proceed();
    }

    private final ExecutorService executorService = new ThreadPoolExecutor(5, 10, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), Executors.defaultThreadFactory());

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection<?> doProcess(ProceedingJoinPoint joinPoint, BatchCacheable cacheable, Class<?> returnType, Object arg, List<String> keyCollection) throws Throwable {
        Map<String, Object> cacheData = batchCacheableProcessor.mGet(cacheable.cacheName(), cacheable.prefix(), keyCollection);

        // 有缓存结果的key
        Set<String> cachedKeys = cacheData.keySet();
        // 无缓存结果的key
        List<String> noCacheKeys = keyCollection.stream().filter(o -> !cachedKeys.contains(o)).collect(Collectors.toList());

        Collection<Object> cachedValues = cacheData.values();
        // 均存在缓存 则直接返回缓存结果
        if (CollectionUtils.isEmpty(noCacheKeys)) {
            return resolve(cachedValues, returnType);
        }

        // 未命中缓存的走代码逻辑
        Collection<Object> noCachedValues = (Collection) joinPoint.proceed(new Object[]{resolve(noCacheKeys, arg.getClass())});
        // 追加缓存
        if (cacheable.cacheAsync()) {
            executorService.execute(() -> addCache(noCachedValues, cacheable.cacheName(), cacheable.prefix(), cacheable.cacheKeyField()));
        } else {
            addCache(noCachedValues, cacheable.cacheName(), cacheable.prefix(), cacheable.cacheKeyField());
        }
        return join(cachedValues, noCachedValues, returnType);
    }

    private void addCache(Collection<Object> noCachedValues, String cacheName, String prefix, String cacheKeyField) {
        if (StringUtils.isEmpty(cacheKeyField) || CollectionUtils.isEmpty(noCachedValues)) {
            return;
        }
        Map<String, Object> data = new HashMap<>(noCachedValues.size());
        for (Object value : noCachedValues) {
            if (value == null) {
                continue;
            }
            Object filedValue = cacheKeyAttribute(value, cacheKeyField);
            Optional.ofNullable(filedValue).ifPresent(v -> data.put(filedValue.toString(), value));
        }
        batchCacheableProcessor.mSet(cacheName, prefix, data);
    }

    private Object cacheKeyAttribute(Object o, String cacheKeyField) {
        try {
            Field declaredField = o.getClass().getDeclaredField(cacheKeyField);
            declaredField.setAccessible(true);
            return declaredField.get(o);
        } catch (Exception e) {
            log.warn("cacheKeyAttribute error", e);
        }
        return null;
    }

    private Collection<?> resolve(Collection<?> data, Class<?> returnType) {
        if (Set.class.isAssignableFrom(returnType)) {
            return data.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).collect(Collectors.toSet());
        }
        if (List.class.isAssignableFrom(returnType)) {
            return data.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).collect(Collectors.toList());
        }
        throw new RuntimeException("返回类型不支持");
    }

    private Collection<Object> join(Collection<Object> data1, Collection<Object> data2, Class<?> returnType) {
        if (Set.class.isAssignableFrom(returnType)) {
            HashSet<Object> objects = new HashSet<>();
            data1.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).forEach(objects::add);
            data2.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).forEach(objects::add);
            return objects;
        }
        if (List.class.isAssignableFrom(returnType)) {
            ArrayList<Object> objects = new ArrayList<>();
            data1.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).forEach(objects::add);
            data2.stream().filter(o -> Objects.nonNull(o) && !(o instanceof NullValue)).forEach(objects::add);
            return objects;
        }
        throw new RuntimeException("数据类型不支持");
    }
}
