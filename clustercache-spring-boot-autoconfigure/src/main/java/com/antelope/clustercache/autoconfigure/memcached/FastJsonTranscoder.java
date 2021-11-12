package com.antelope.clustercache.autoconfigure.memcached;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.util.IOUtils;
import lombok.SneakyThrows;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import org.springframework.cache.support.NullValue;

/**
 * @author yaml
 * @since 2021/7/1
 */
public class FastJsonTranscoder extends SerializingTranscoder {

    private static final FastJsonTranscoder JSON_TRANSCODER = new FastJsonTranscoder();

    private static final ParserConfig DEFAULT_REDIS_CONFIG = new ParserConfig();

    static {
        DEFAULT_REDIS_CONFIG.setSafeMode(false);
        DEFAULT_REDIS_CONFIG.setAutoTypeSupport(true);
    }

    public static FastJsonTranscoder getInstance() {
        return JSON_TRANSCODER;
    }

    @Override
    @SneakyThrows
    protected byte[] serialize(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONBytes(object, SerializerFeature.WriteClassName);
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize: " + e.getMessage(), e);
        }
    }

    @Override
    @SneakyThrows
    protected Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            Object object = JSON.parseObject(new String(bytes, IOUtils.UTF8), Object.class, DEFAULT_REDIS_CONFIG);
            if (object instanceof NullValue) {
                return null;
            }
            return object;
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize: " + e.getMessage(), e);
        }
    }
}
