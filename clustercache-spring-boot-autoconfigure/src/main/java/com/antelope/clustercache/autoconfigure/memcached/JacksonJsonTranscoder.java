package com.antelope.clustercache.autoconfigure.memcached;

import com.antelope.clustercache.autoconfigure.core.ObjectMapperFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import lombok.SneakyThrows;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import org.springframework.cache.support.NullValue;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author yaml
 * @since 2021/7/1
 */
public class JacksonJsonTranscoder extends SerializingTranscoder {

    private static final JacksonJsonTranscoder JSON_TRANSCODER = new JacksonJsonTranscoder();

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = ObjectMapperFactory.newInstance();
        OBJECT_MAPPER.registerModule(new SimpleModule().addSerializer(new NullValueSerializer(null)));
    }

    public static JacksonJsonTranscoder getInstance() {
        return JSON_TRANSCODER;
    }

    @Override
    @SneakyThrows
    protected byte[] serialize(Object object) {
        if (object == null) {
            return new byte[0];
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not write JSON: " + e.getMessage(), e);
        }
    }

    @Override
    @SneakyThrows
    protected Object deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Could not deserialize: " + e.getMessage(), e);
        }
    }

    private static class NullValueSerializer extends StdSerializer<NullValue> {

        private static final long serialVersionUID = 1999052150548658808L;
        private final String classIdentifier;

        /**
         * @param classIdentifier can be {@literal null} and will be defaulted to {@code @class}.
         */
        NullValueSerializer(@Nullable String classIdentifier) {

            super(NullValue.class);
            this.classIdentifier = StringUtils.hasText(classIdentifier) ? classIdentifier : "@class";
        }

        /*
         * (non-Javadoc)
         * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
         */
        @Override
        public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {

            jgen.writeStartObject();
            jgen.writeStringField(classIdentifier, NullValue.class.getName());
            jgen.writeEndObject();
        }


    }
}
