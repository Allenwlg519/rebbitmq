package com.seckill.aspect;

import com.seckill.annotation.RateLimit;
import com.seckill.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * 限流切面 - 基于Redis + Lua脚本实现滑动窗口限流
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redisTemplate;
    private final HttpServletRequest request;

    /**
     * Lua脚本：滑动窗口限流
     * KEYS[1] = 限流key
     * ARGV[1] = 窗口大小(秒)
     * ARGV[2] = 最大请求数
     * ARGV[3] = 当前时间戳(毫秒)
     */
    private static final String RATE_LIMIT_LUA =
            "local key = KEYS[1]\n" +
            "local window = tonumber(ARGV[1])\n" +
            "local maxCount = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local windowStart = now - window * 1000\n" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)\n" +
            "local count = redis.call('ZCARD', key)\n" +
            "if count < maxCount then\n" +
            "    redis.call('ZADD', key, now, now)\n" +
            "    redis.call('EXPIRE', key, window)\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    @Around("@annotation(com.seckill.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 构建限流Key: 前缀 + 用户ID + 方法名
        String userId = request.getParameter("userId");
        String limitKey = rateLimit.key() + ":" + (userId != null ? userId : request.getRemoteAddr())
                + ":" + method.getName();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
        List<String> keys = Collections.singletonList(limitKey);
        Long result = redisTemplate.execute(redisScript, keys,
                String.valueOf(rateLimit.time()),
                String.valueOf(rateLimit.count()),
                String.valueOf(System.currentTimeMillis()));

        if (result == null || result == 0) {
            log.warn("触发限流: key={}", limitKey);
            return Result.fail(429, rateLimit.msg());
        }

        return joinPoint.proceed();
    }
}
