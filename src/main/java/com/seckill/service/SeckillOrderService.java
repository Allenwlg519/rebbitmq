package com.seckill.service;

import com.seckill.entity.SeckillOrder;

public interface SeckillOrderService {

    /**
     * 创建秒杀订单（MySQL落库）
     */
    SeckillOrder createOrder(Long userId, Long couponId);
}
