package com.seckill.init;

import com.seckill.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时自动预热库存到Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CouponService couponService;

    @Override
    public void run(String... args) {
        try {
            // 预热ID为1的优惠券库存
            couponService.preheatStock(1L);
            log.info("========== 库存预热完成 ==========");
        } catch (Exception e) {
            log.warn("库存预热失败（可能Redis未启动）: {}", e.getMessage());
        }
    }
}
