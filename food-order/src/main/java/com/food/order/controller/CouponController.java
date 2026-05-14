package com.food.order.controller;

import com.food.common.core.constant.CommonConstant;
import com.food.common.core.exception.BusinessException;
import com.food.common.core.result.R;
import com.food.order.dto.*;
import com.food.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    private void checkMerchant(Integer role) {
        if (role == null || role != CommonConstant.ROLE_MERCHANT) {
            throw new BusinessException("仅商家可操作");
        }
    }

    // ---- 商家端 ----

    @PostMapping
    public R<Long> createCoupon(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                                @RequestBody CouponDTO dto) {
        checkMerchant(role);
        return R.ok(couponService.createCoupon(merchantId, dto));
    }

    @PutMapping("/{id}")
    public R<Void> updateCoupon(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                                @PathVariable Long id,
                                @RequestBody CouponDTO dto) {
        checkMerchant(role);
        couponService.updateCoupon(merchantId, id, dto);
        return R.ok();
    }

    @GetMapping("/list")
    public R<CouponListDTO> listCoupons(
            @RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
            @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
            @RequestParam(name = "current", defaultValue = "1") int current,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        checkMerchant(role);
        return R.ok(couponService.listCoupons(merchantId, current, size));
    }

    @GetMapping("/{id}")
    public R<CouponDTO> getCouponDetail(@PathVariable Long id) {
        return R.ok(couponService.getCouponDetail(id));
    }

    @PutMapping("/{id}/status")
    public R<Void> updateCouponStatus(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                      @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, Integer> body) {
        checkMerchant(role);
        couponService.updateCouponStatus(merchantId, id, body.get("status"));
        return R.ok();
    }

    // ---- 核销端 ----

    @PostMapping("/verify")
    public R<CouponVerifyDTO> verifyCoupon(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                           @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                                           @RequestBody Map<String, String> body) {
        checkMerchant(role);
        return R.ok(couponService.verifyCoupon(merchantId, body.get("couponCode")));
    }

    @PostMapping("/verify/confirm")
    public R<Void> confirmVerify(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                                 @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                                 @RequestBody Map<String, String> body) {
        checkMerchant(role);
        couponService.confirmVerify(merchantId, body.get("couponCode"));
        return R.ok();
    }

    // ---- 用户端 ----

    @PostMapping("/{id}/claim")
    public R<Void> claimCoupon(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                               @PathVariable Long id) {
        couponService.claimCoupon(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/flash-claim")
    public R<Void> flashClaimCoupon(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                    @PathVariable Long id) {
        couponService.flashClaimCoupon(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/sync-stock")
    public R<Void> syncStock(@RequestHeader(CommonConstant.USER_ID_HEADER) Long merchantId,
                             @RequestHeader(CommonConstant.USER_ROLE_HEADER) Integer role,
                             @PathVariable Long id) {
        checkMerchant(role);
        couponService.syncStockToRedis(id);
        return R.ok();
    }

    @GetMapping("/my")
    public R<UserCouponListDTO> listMyCoupons(
            @RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
            @RequestParam(name = "current", defaultValue = "1") int current,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return R.ok(couponService.listMyCoupons(userId, current, size));
    }

    @GetMapping("/my/{id}")
    public R<UserCouponDTO> getMyCouponDetail(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                              @PathVariable Long id) {
        return R.ok(couponService.getMyCouponDetail(userId, id));
    }
}
