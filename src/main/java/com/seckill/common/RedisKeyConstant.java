package com.seckill.common;

/**
 * Redis Key 常量定义
 */
public class RedisKeyConstant {

    /** 库存Key: seckill:stock:{couponId} */
    public static final String STOCK_KEY = "seckill:stock:";

    /** 用户已购集合Key: seckill:bought:{couponId} */
    public static final String BOUGHT_SET_KEY = "seckill:bought:";

    /** 限流Key: seckill:limit:{userId} */
    public static final String RATE_LIMIT_KEY = "seckill:limit:";

    /** 库存预热锁Key */
    public static final String STOCK_INIT_LOCK = "seckill:lock:init:";

    public static String stockKey(Long couponId) {
        return STOCK_KEY + couponId;
    }

    public static String boughtSetKey(Long couponId) {
        return BOUGHT_SET_KEY + couponId;
    }

    public static String rateLimitKey(Long userId) {
        return RATE_LIMIT_KEY + userId;
    }

    public static String stockInitLockKey(Long couponId) {
        return STOCK_INIT_LOCK + couponId;
    }
}
