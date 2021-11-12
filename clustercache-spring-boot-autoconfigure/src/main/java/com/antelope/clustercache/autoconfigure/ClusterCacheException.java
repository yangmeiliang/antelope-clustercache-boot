package com.antelope.clustercache.autoconfigure;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yaml
 * @since 2021/6/8
 */
@Getter
@Setter
public class ClusterCacheException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "cache error";

    public ClusterCacheException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public static ClusterCacheException create() {
        return create(DEFAULT_MESSAGE);
    }

    public static ClusterCacheException create(String message) {
        return new ClusterCacheException(message);
    }
}
