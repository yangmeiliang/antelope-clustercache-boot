package com.antelope.clustercache.autoconfigure.core;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * @author yaml
 * @since 2021/8/25
 */
public class DefaultKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        return "all";
    }
}
