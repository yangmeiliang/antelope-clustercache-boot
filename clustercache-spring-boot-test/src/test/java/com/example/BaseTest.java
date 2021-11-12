package com.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @author yaml
 * @since 2021/6/28
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class BaseTest {

    @Resource
    RedisCache redisCache;

    static {
        System.setProperty("logging.level", "debug");
        System.setProperty("logging.level.root", "debug");
        System.setProperty("logging.level.com.antelope.clustercache", "debug");
    }


    @Test
    public void test() {
        String s = redisCache.get("12313");
        System.out.println(s);

    }

}
