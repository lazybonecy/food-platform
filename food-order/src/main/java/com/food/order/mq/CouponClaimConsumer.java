package com.food.order.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.order.entity.Coupon;
import com.food.order.entity.CouponOrder;
import com.food.order.entity.UserCoupon;
import com.food.order.mapper.CouponMapper;
import com.food.order.mapper.CouponOrderMapper;
import com.food.order.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponClaimConsumer {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponOrderMapper couponOrderMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_COUPON_CLAIM)
    @Transactional
    public void handleClaim(CouponClaimMessage message) {
        log.info("Processing coupon claim: userId={}, couponId={}",
                message.getUserId(), message.getCouponId());

        // 1. Duplicate check (idempotent)
        LambdaQueryWrapper<UserCoupon> dupCheck = new LambdaQueryWrapper<>();
        dupCheck.eq(UserCoupon::getUserId, message.getUserId())
                .eq(UserCoupon::getCouponId, message.getCouponId());
        Long existing = userCouponMapper.selectCount(dupCheck);
        if (existing > 0) {
            log.warn("Duplicate claim, skipping: userId={}, couponId={}",
                    message.getUserId(), message.getCouponId());
            return;
        }

        // 2. Load coupon
        Coupon coupon = couponMapper.selectById(message.getCouponId());
        if (coupon == null) {
            log.error("Coupon not found: {}", message.getCouponId());
            return;
        }

        // 3. Optimistic lock: update claimed_count with version
        int affected = couponMapper.update(null,
                new LambdaUpdateWrapper<Coupon>()
                        .eq(Coupon::getId, message.getCouponId())
                        .eq(Coupon::getVersion, coupon.getVersion())
                        .set(Coupon::getClaimedCount, coupon.getClaimedCount() + 1)
                        .set(Coupon::getVersion, coupon.getVersion() + 1));

        if (affected == 0) {
            // Version conflict, retry once
            log.warn("Version conflict on coupon {}, retrying...", message.getCouponId());
            coupon = couponMapper.selectById(message.getCouponId());
            if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
                log.warn("Coupon {} sold out during retry", message.getCouponId());
                return;
            }
            couponMapper.update(null,
                    new LambdaUpdateWrapper<Coupon>()
                            .eq(Coupon::getId, message.getCouponId())
                            .eq(Coupon::getVersion, coupon.getVersion())
                            .set(Coupon::getClaimedCount, coupon.getClaimedCount() + 1)
                            .set(Coupon::getVersion, coupon.getVersion() + 1));
        }

        // 4. Insert user_coupon
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(message.getUserId());
        userCoupon.setCouponId(message.getCouponId());
        userCoupon.setCouponCode(message.getCouponCode());
        userCoupon.setStatus(0);
        userCoupon.setPayAmount(BigDecimal.ZERO);
        userCouponMapper.insert(userCoupon);

        // 5. If paid coupon, create order
        if (coupon.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            userCoupon.setPayAmount(coupon.getOriginalPrice());
            userCouponMapper.updateById(userCoupon);

            CouponOrder order = new CouponOrder();
            order.setUserId(message.getUserId());
            order.setCouponId(message.getCouponId());
            order.setAmount(coupon.getOriginalPrice());
            order.setStatus(1);
            order.setPayTime(LocalDateTime.now());
            couponOrderMapper.insert(order);

            order.setUserCouponId(userCoupon.getId());
            couponOrderMapper.updateById(order);
        }

        log.info("Claim processed: userId={}, couponId={}, code={}",
                message.getUserId(), message.getCouponId(), message.getCouponCode());
    }
}
