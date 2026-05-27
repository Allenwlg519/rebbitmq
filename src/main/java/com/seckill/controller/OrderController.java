package com.seckill.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.seckill.common.Result;
import com.seckill.entity.SeckillOrder;
import com.seckill.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final SeckillOrderMapper seckillOrderMapper;

    /**
     * 查询用户是否已抢到优惠券
     */
    @GetMapping("/check")
    public Result<Boolean> checkOrder(@RequestParam Long userId, @RequestParam Long couponId) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getUserId, userId)
               .eq(SeckillOrder::getCouponId, couponId);
        Long count = seckillOrderMapper.selectCount(wrapper);
        return Result.success(count > 0);
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderNo}")
    public Result<SeckillOrder> getByOrderNo(@PathVariable String orderNo) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SeckillOrder::getOrderNo, orderNo);
        SeckillOrder order = seckillOrderMapper.selectOne(wrapper);
        if (order == null) {
            return Result.fail("订单不存在");
        }
        return Result.success(order);
    }
}
