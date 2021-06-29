package com.antelop.clustercache;

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


    @Test
    public void test() {
        String s = redisCache.get("123");
        System.out.println(s);

    }

}
