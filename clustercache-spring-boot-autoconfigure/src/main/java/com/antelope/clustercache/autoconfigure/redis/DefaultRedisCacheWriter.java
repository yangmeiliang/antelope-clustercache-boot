package com.antelope.clustercache.autoconfigure.redis;

import lombok.Setter;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * @author yaml
 * @since 2021/8/6
 */
@Setter
public class DefaultRedisCacheWriter implements RedisCacheWriter {

    private RedisConnectionFactory connectionFactory;

    public static DefaultRedisCacheWriter getInstance(RedisConnectionFactory connectionFactory) {
        Assert.notNull(connectionFactory, "ConnectionFactory must not be null!");
        DefaultRedisCacheWriter defaultRedisCacheWriter = new DefaultRedisCacheWriter();
        defaultRedisCacheWriter.setConnectionFactory(connectionFactory);
        return defaultRedisCacheWriter;
    }

    public void mSet(@NonNull String name, @NonNull Map<byte[], byte[]> keyValues, @Nullable Duration ttl) {
        execute(name, connection -> {
            if (shouldExpireWithin(ttl)) {
                connection.mSet(keyValues);
                keyValues.keySet().forEach(key -> connection.expire(key, ttl.toMillis() / 1000));
            } else {
                connection.mSet(keyValues);
            }
            return "OK";
        });
    }

    public void hmSet(@NonNull String name, @NonNull byte[] key, @NonNull Map<byte[], byte[]> hashes, @Nullable Duration ttl) {
        execute(name, connection -> {

            if (shouldExpireWithin(ttl)) {
                connection.hMSet(key, hashes);
                connection.expire(key, ttl.toMillis() / 1000);
            } else {
                connection.hMSet(key, hashes);
            }
            return "OK";
        });
    }

    @Override
    public void put(@NonNull String name, @NonNull byte[] key, @NonNull byte[] value, @Nullable Duration ttl) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");

        execute(name, connection -> {
            if (shouldExpireWithin(ttl)) {
                connection.set(key, value, Expiration.from(ttl.toMillis(), TimeUnit.MILLISECONDS), RedisStringCommands.SetOption.upsert());
            } else {
                connection.set(key, value);
            }
            return "OK";
        });
    }

    @Override
    public byte[] get(@NonNull String name, @NonNull byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");

        return execute(name, connection -> connection.get(key));
    }

    @Override
    public byte[] putIfAbsent(@NonNull String name, @NonNull byte[] key, @NonNull byte[] value, @Nullable Duration ttl) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");
        Assert.notNull(value, "Value must not be null!");

        return execute(name, connection -> {
            if (connection.setNX(key, value)) {

                if (shouldExpireWithin(ttl)) {
                    connection.pExpire(key, ttl.toMillis());
                }
                return null;
            }

            return connection.get(key);
        });
    }

    @Override
    public void remove(@NonNull String name, @NonNull byte[] key) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(key, "Key must not be null!");

        execute(name, connection -> connection.del(key));
    }

    @Override
    public void clean(@NonNull String name, @NonNull byte[] pattern) {

        Assert.notNull(name, "Name must not be null!");
        Assert.notNull(pattern, "Pattern must not be null!");

        execute(name, connection -> {

            byte[][] keys = Optional.ofNullable(connection.keys(pattern)).orElse(Collections.emptySet()).toArray(new byte[0][]);
            if (keys.length > 0) {
                connection.del(keys);
            }
            return "OK";
        });
    }

    private <T> T execute(String name, Function<RedisConnection, T> callback) {

        RedisConnection connection = connectionFactory.getConnection();
        try {
            return callback.apply(connection);
        } finally {
            connection.close();
        }
    }

    private static boolean shouldExpireWithin(@Nullable Duration ttl) {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }
}
