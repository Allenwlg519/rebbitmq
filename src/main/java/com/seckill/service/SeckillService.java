package com.seckill.service;

/**
 * 秒杀核心服务
 */
public interface SeckillService {

    /**
     * 执行秒杀: Redis库存扣减 + 用户去重 + 发送MQ消息
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @return 订单号
     */
    String doSeckill(Long userId, Long couponId);
}
