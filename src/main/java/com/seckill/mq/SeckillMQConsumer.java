package com.seckill.mq;

import com.seckill.common.RedisKeyConstant;
import com.seckill.service.SeckillOrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMQConsumer {

    private final SeckillOrderService seckillOrderService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 消费秒杀消息，异步创建订单落库
     */
    @RabbitListener(queues = RabbitMQConfig.SECKILL_QUEUE)
    public void consume(SeckillMessage seckillMessage, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            log.info("收到秒杀消息: userId={}, couponId={}", seckillMessage.getUserId(), seckillMessage.getCouponId());

            // 创建订单（MySQL落库）
            seckillOrderService.createOrder(seckillMessage.getUserId(), seckillMessage.getCouponId());

            // 手动ACK
            channel.basicAck(deliveryTag, false);
            log.info("秒杀订单创建成功: userId={}, couponId={}", seckillMessage.getUserId(), seckillMessage.getCouponId());
        } catch (Exception e) {
            log.error("秒杀订单创建失败: userId={}, couponId={}, error={}",
                    seckillMessage.getUserId(), seckillMessage.getCouponId(), e.getMessage());
            try {
                // 消息重试3次后进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("消息NACK失败", ex);
            }

            // 回滚Redis库存和用户去重标记
            rollbackRedis(seckillMessage.getUserId(), seckillMessage.getCouponId());
        }
    }

    /**
     * 订单创建失败时回滚Redis数据
     */
    private void rollbackRedis(Long userId, Long couponId) {
        try {
            // 恢复库存
            String stockKey = RedisKeyConstant.stockKey(couponId);
            redisTemplate.opsForValue().increment(stockKey);

            // 移除用户已购标记
            String boughtSetKey = RedisKeyConstant.boughtSetKey(couponId);
            redisTemplate.opsForSet().remove(boughtSetKey, String.valueOf(userId));

            log.info("Redis数据回滚完成: userId={}, couponId={}", userId, couponId);
        } catch (Exception e) {
            log.error("Redis数据回滚失败", e);
        }
    }
}
