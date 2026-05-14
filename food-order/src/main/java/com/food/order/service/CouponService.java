package com.food.order.service;

import com.food.order.dto.*;

public interface CouponService {

    Long createCoupon(Long merchantId, CouponDTO dto);

    void updateCoupon(Long merchantId, Long couponId, CouponDTO dto);

    CouponListDTO listCoupons(Long merchantId, int current, int size);

    CouponDTO getCouponDetail(Long couponId);

    void updateCouponStatus(Long merchantId, Long couponId, Integer status);

    void claimCoupon(Long userId, Long couponId);

    void flashClaimCoupon(Long userId, Long couponId);

    void syncStockToRedis(Long couponId);

    UserCouponListDTO listMyCoupons(Long userId, int current, int size);

    UserCouponDTO getMyCouponDetail(Long userId, Long userCouponId);

    CouponVerifyDTO verifyCoupon(Long merchantId, String couponCode);

    void confirmVerify(Long merchantId, String couponCode);
}
