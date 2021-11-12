package com.antelope.clustercache.autoconfigure.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * 默认缓存key生成规则：{keyPrefix}:{cacheName}:{nameVersion}:{key}
 *
 * @author yaml
 * @since 2021/8/6
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FinallyKeyGenerator {

    private final String name;
    private final String keyPrefix;
    private final String keySeparator;

    public static final int KEY_MAX_LENGTH = 50;

    public static FinallyKeyGenerator getInstance(@NonNull String name, @Nullable String keyPrefix, @NonNull String keySeparator) {
        return new FinallyKeyGenerator(name, keyPrefix, keySeparator);
    }

    public static FinallyKeyGenerator getInstance(@Nullable String keyPrefix, @NonNull String keySeparator) {
        return new FinallyKeyGenerator(null, keyPrefix, keySeparator);
    }

    public String generate(@Nullable String key) {
        return generate(key, null);
    }

    public String generate(String key, String nameVersion) {
        StringBuilder stringBuilder = new StringBuilder();
        if (!StringUtils.isEmpty(keyPrefix)) {
            stringBuilder.append(keyPrefix.concat(keySeparator));
        }
        if (!StringUtils.isEmpty(name)) {
            stringBuilder.append(name.concat(keySeparator));
        }
        if (!StringUtils.isEmpty(nameVersion)) {
            stringBuilder.append(nameVersion.concat(keySeparator));
        }
        if (!StringUtils.isEmpty(key)) {
            if (key.length() <= KEY_MAX_LENGTH) {
                stringBuilder.append(key.replace(" ", ""));
            } else {
                stringBuilder.append(DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8)));
            }
        }
        String finalKey = stringBuilder.toString().trim();
        if (finalKey.endsWith(keySeparator)) {
            return finalKey.substring(0, finalKey.length() - 1);
        }
        return finalKey;
    }
}
