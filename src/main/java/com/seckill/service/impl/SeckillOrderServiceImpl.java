package com.seckill.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.CouponMapper;
import com.seckill.mapper.SeckillOrderMapper;
import com.seckill.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private final SeckillOrderMapper seckillOrderMapper;
    private final CouponMapper couponMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createOrder(Long userId, Long couponId) {
        // 1. 乐观锁扣减数据库库存（兜底保障）
        int rows = couponMapper.deductStock(couponId);
        if (rows == 0) {
            log.warn("数据库库存扣减失败(库存不足), couponId={}", couponId);
            throw new RuntimeException("库存不足");
        }

        // 2. 创建订单
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setCouponId(couponId);
        order.setOrderNo(generateOrderNo(userId));
        order.setStatus(0); // 待支付
        seckillOrderMapper.insert(order);

        log.info("订单创建成功, orderId={}, userId={}, couponId={}", order.getId(), userId, couponId);
        return order;
    }

    /**
     * 生成订单号: 时间戳 + 用户ID后4位 + 随机数
     */
    private String generateOrderNo(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userIdSuffix = String.valueOf(userId % 10000);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return timestamp + String.format("%04d", Long.parseLong(userIdSuffix)) + random;
    }
}
