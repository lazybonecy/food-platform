package com.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UserCouponDTO {

    private Long id;

    private Long userId;

    private Long couponId;

    private String couponTitle;

    private String couponCode;

    private Integer type;

    private String typeDesc;

    private BigDecimal threshold;

    private BigDecimal discount;

    private BigDecimal originalPrice;

    private Integer status;

    private String statusDesc;

    private BigDecimal payAmount;

    private String merchantName;

    private String usedTime;

    private String createTime;

    private String endTime;
}
