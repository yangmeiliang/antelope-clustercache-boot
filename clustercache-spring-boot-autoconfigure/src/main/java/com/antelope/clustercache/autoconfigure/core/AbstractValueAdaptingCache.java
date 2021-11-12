package com.antelope.clustercache.autoconfigure.core;

import lombok.SneakyThrows;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * @author yaml
 * @since 2021/8/6
 */
public abstract class AbstractValueAdaptingCache extends org.springframework.cache.support.AbstractValueAdaptingCache {

    protected final String name;
    protected final Duration ttl;
    protected final ConversionService conversionService;

    protected AbstractValueAdaptingCache(String name, Duration ttl, ConversionService conversionService, boolean allowNullValues) {
        super(allowNullValues);
        this.name = name;
        this.ttl = ttl;
        this.conversionService = conversionService;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T get(@NonNull Object key, @NonNull Callable<T> callable) {
        ValueWrapper valueWrapper = get(key);
        if (valueWrapper != null) {
            return (T) valueWrapper.get();
        }
        T value = valueFromLoader(key, callable);
        put(key, value);
        return value;
    }

    protected String convertKey(Object key) {

        TypeDescriptor source = TypeDescriptor.forObject(key);
        if (conversionService.canConvert(source, TypeDescriptor.valueOf(String.class))) {
            return conversionService.convert(key, String.class);
        }
        Method toString = ReflectionUtils.findMethod(key.getClass(), "toString");
        if (toString != null && !Object.class.equals(toString.getDeclaringClass())) {
            return key.toString();
        }
        throw new IllegalStateException(
                String.format("Cannot convert %s to String. Register a Converter or override toString().", source));
    }

    private static <T> T valueFromLoader(Object key, Callable<T> valueLoader) {
        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }
}
