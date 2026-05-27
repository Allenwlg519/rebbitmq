package com.seckill.service.impl;

import com.seckill.common.RedisKeyConstant;
import com.seckill.entity.Coupon;
import com.seckill.mq.RabbitMQConfig;
import com.seckill.mq.SeckillMessage;
import com.seckill.service.CouponService;
import com.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final CouponService couponService;

    @Override
    public String doSeckill(Long userId, Long couponId) {
        // 1. 校验秒杀时间
        Coupon coupon = couponService.getById(couponId);
        if (coupon == null) {
            throw new RuntimeException("优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new RuntimeException("不在秒杀时间范围内");
        }

        // 2. Redis原子操作: 用户去重 (SISMEMBER + SADD)
        String boughtSetKey = RedisKeyConstant.boughtSetKey(couponId);
        Boolean isAdded = redisTemplate.opsForSet().add(boughtSetKey, String.valueOf(userId)) == 0 ? false : true;
        if (!isAdded) {
            throw new RuntimeException("您已领取过该优惠券，不可重复领取");
        }

        // 3. Redis原子递减库存 (DECR)
        String stockKey = RedisKeyConstant.stockKey(couponId);
        Long remainStock = redisTemplate.opsForValue().decrement(stockKey);
        if (remainStock == null) {
            // Redis中无库存缓存，需要预热
            throw new RuntimeException("库存数据未预热，请稍后重试");
        }
        if (remainStock < 0) {
            // 库存不足，回滚
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForSet().remove(boughtSetKey, String.valueOf(userId));
            throw new RuntimeException("优惠券已抢光");
        }

        // 4. Redisson分布式锁兜底：防止Redis与MQ之间的极端并发问题
        String lockKey = "seckill:lock:" + couponId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败，说明该用户正在处理中，回滚Redis
                redisTemplate.opsForValue().increment(stockKey);
                redisTemplate.opsForSet().remove(boughtSetKey, String.valueOf(userId));
                throw new RuntimeException("操作过于频繁，请稍后重试");
            }

            // 5. 发送MQ消息，异步创建订单
            SeckillMessage mqMessage = new SeckillMessage(userId, couponId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SECKILL_EXCHANGE,
                    RabbitMQConfig.SECKILL_ROUTING_KEY,
                    mqMessage
            );

            log.info("秒杀请求成功，已发送MQ消息: userId={}, couponId={}, remainStock={}", userId, couponId, remainStock);
            return "排队中，请稍后查询订单结果";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // 回滚Redis
            redisTemplate.opsForValue().increment(stockKey);
            redisTemplate.opsForSet().remove(boughtSetKey, String.valueOf(userId));
            throw new RuntimeException("秒杀被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
