package com.food.order.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class CouponClaimMessage implements Serializable {

    private Long userId;
    private Long couponId;
    private String couponCode;
}
