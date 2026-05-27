package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.RedisKeyConstant;
import com.seckill.entity.Coupon;
import com.seckill.mapper.CouponMapper;
import com.seckill.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    @Override
    public Coupon getById(Long couponId) {
        return couponMapper.selectById(couponId);
    }

    @Override
    public void preheatStock(Long couponId) {
        String stockKey = RedisKeyConstant.stockKey(couponId);
        String lockKey = RedisKeyConstant.stockInitLockKey(couponId);

        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取分布式锁，防止并发重复预热
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("库存预热锁获取失败，可能其他节点正在预热, couponId={}", couponId);
                return;
            }

            // 检查Redis中是否已有库存缓存
            String stockStr = redisTemplate.opsForValue().get(stockKey);
            if (stockStr != null) {
                log.info("库存已预热，无需重复操作, couponId={}, stock={}", couponId, stockStr);
                return;
            }

            // 从数据库加载库存
            Coupon coupon = couponMapper.selectById(couponId);
            if (coupon == null) {
                log.warn("优惠券不存在, couponId={}", couponId);
                return;
            }

            // 写入Redis
            redisTemplate.opsForValue().set(stockKey, String.valueOf(coupon.getStock()));
            log.info("库存预热完成, couponId={}, stock={}", couponId, coupon.getStock());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("库存预热被中断", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
