package com.antelope.clustercache.autoconfigure;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author yaml
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheExpire {
    /**
     * 过期时间，永久(-1) 毫秒(ms) 秒(s) 分(m) 小时(h) 天(d)
     */
    @AliasFor("expire")
    String value() default "1h";

    /**
     * 过期时间，单位：秒，默认：一小时
     */
    @AliasFor("value")
    String expire() default "1h";
}
