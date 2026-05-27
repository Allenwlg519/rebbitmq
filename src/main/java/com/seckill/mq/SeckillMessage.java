package com.seckill.mq;

import lombok.Data;

import java.io.Serializable;

/**
 * 秒杀消息体
 */
@Data
public class SeckillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long couponId;

    public SeckillMessage() {}

    public SeckillMessage(Long userId, Long couponId) {
        this.userId = userId;
        this.couponId = couponId;
    }
}
