package com.antelope.clustercache.autoconfigure.annotion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 批量缓存注解，作用是将集合入参拆分成一条条数据进行缓存
 * 方法入参和返回类型必须为集合（Collection）
 * 使用此注解 返回的数据顺序会被打乱，所以不要在该注解的方法体内进行数据排序，应该放在外部进行数据排序
 *
 * @author yaml
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BatchCacheable {
    /**
     * cacheName
     */
    String cacheName();

    /**
     * 入参集合的值前缀，和@Cacheable配合使用，达到缓存共享
     * eg.
     *    单个缓存 => @Cacheable(cacheNames="test", key="'k_' + #param.id")
     *    批量缓存 => @BatchCacheable(cacheName = "test", prefix="k_") 此注解下的方法参数类型必须是Collection
     */
    String prefix() default "";

    /**
     * 使用反射获取返回值的字段名 用作添加未命中缓存数据
     */
    String cacheKeyField() default "";

    /**
     * 异步执行追加缓存操作
     */
    boolean cacheAsync() default true;

}
