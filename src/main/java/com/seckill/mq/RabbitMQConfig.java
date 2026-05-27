package com.seckill.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    /** 秒杀订单队列 */
    public static final String SECKILL_QUEUE = "seckill.order.queue";

    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.order.exchange";

    /** 秒杀订单路由键 */
    public static final String SECKILL_ROUTING_KEY = "seckill.order.routing.key";

    /** 死信队列 */
    public static final String SECKILL_DLQ = "seckill.order.dlq";

    /** 死信交换机 */
    public static final String SECKILL_DLX = "seckill.order.dlx";

    /** 死信路由键 */
    public static final String SECKILL_DL_ROUTING_KEY = "seckill.order.dl.routing.key";

    // ========== 正常队列与交换机 ==========

    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE)
                .withArgument("x-dead-letter-exchange", SECKILL_DLX)
                .withArgument("x-dead-letter-routing-key", SECKILL_DL_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }

    // ========== 死信队列 ==========

    @Bean
    public Queue seckillDLQ() {
        return QueueBuilder.durable(SECKILL_DLQ).build();
    }

    @Bean
    public DirectExchange seckillDLX() {
        return new DirectExchange(SECKILL_DLX, true, false);
    }

    @Bean
    public Binding seckillDLQBinding() {
        return BindingBuilder.bind(seckillDLQ()).to(seckillDLX()).with(SECKILL_DL_ROUTING_KEY);
    }
}
