package com.seckill.service;

import com.seckill.entity.Coupon;

public interface CouponService {

    /**
     * 根据ID查询优惠券
     */
    Coupon getById(Long couponId);

    /**
     * 将优惠券库存预热到Redis
     */
    void preheatStock(Long couponId);
}
