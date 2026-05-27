package com.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 乐观锁扣减库存
     */
    @Update("UPDATE coupon SET stock = stock - 1 WHERE id = #{couponId} AND stock > 0")
    int deductStock(@Param("couponId") Long couponId);
}
