package com.seckill.controller;

import com.seckill.annotation.RateLimit;
import com.seckill.common.RedisKeyConstant;
import com.seckill.common.Result;
import com.seckill.entity.Coupon;
import com.seckill.service.CouponService;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;
    private final CouponService couponService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 秒杀抢券接口
     * 限流: 同一用户1秒内最多5次请求
     */
    @RateLimit(key = "seckill:do", time = 1, count = 5, msg = "抢券太频繁了，请稍后再试")
    @PostMapping("/do")
    public Result<String> doSeckill(@RequestParam Long userId, @RequestParam Long couponId) {
        try {
            String result = seckillService.doSeckill(userId, couponId);
            return Result.success(result);
        } catch (RuntimeException e) {
            log.warn("秒杀失败: userId={}, couponId={}, reason={}", userId, couponId, e.getMessage());
            return Result.fail(e.getMessage());
        }
    }

    /**
     * 查询剩余库存（从Redis读取）
     */
    @GetMapping("/stock")
    public Result<String> getStock(@RequestParam Long couponId) {
        String stockKey = RedisKeyConstant.stockKey(couponId);
        String stock = redisTemplate.opsForValue().get(stockKey);
        return Result.success(stock != null ? stock : "库存数据未预热");
    }

    /**
     * 库存预热接口（将MySQL库存同步到Redis）
     */
    @PostMapping("/preheat")
    public Result<String> preheatStock(@RequestParam Long couponId) {
        couponService.preheatStock(couponId);
        return Result.success("库存预热成功");
    }

    /**
     * 查询优惠券详情
     */
    @GetMapping("/coupon/{couponId}")
    public Result<Coupon> getCoupon(@PathVariable Long couponId) {
        Coupon coupon = couponService.getById(couponId);
        if (coupon == null) {
            return Result.fail("优惠券不存在");
        }
        return Result.success(coupon);
    }
}
