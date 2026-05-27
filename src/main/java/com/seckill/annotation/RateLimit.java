package com.seckill.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 基于Redis + Lua脚本实现滑动窗口限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流key前缀
     */
    String key() default "seckill:limit";

    /**
     * 时间窗口(秒)
     */
    int time() default 1;

    /**
     * 限流次数
     */
    int count() default 5;

    /**
     * 限流提示信息
     */
    String msg() default "操作过于频繁，请稍后再试";
}
