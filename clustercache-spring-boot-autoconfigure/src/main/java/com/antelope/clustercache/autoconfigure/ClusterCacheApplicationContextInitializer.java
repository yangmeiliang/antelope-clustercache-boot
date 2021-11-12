package com.antelope.clustercache.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yaml
 * @since 2021/8/11
 */
@Slf4j
public class ClusterCacheApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String PROPERTY_KEY_SPRING_REDIS_ENABLE = "spring.redis.enable";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        // 判断是否屏蔽redis
        handleRedisEnable(environment.getProperty(PROPERTY_KEY_SPRING_REDIS_ENABLE), environment);
    }

    @SuppressWarnings("unchecked")
    private void handleRedisEnable(String enable, ConfigurableEnvironment environment) {
        if (!"false".equalsIgnoreCase(enable)) {
            return;
        }
        log.info("redis not enable...");
        List<Object> disables = new ArrayList<>();
        disables.add("org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        List<Object> property = environment.getProperty("spring.autoconfigure.exclude", List.class);
        if (!CollectionUtils.isEmpty(property)) {
            disables.addAll(property);
        }
        System.getProperties().put("spring.autoconfigure.exclude", disables);
    }
}
