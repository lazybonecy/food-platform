package com.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponVerifyDTO {

    private Long id;

    private String couponCode;

    private String couponTitle;

    private Integer type;

    private String typeDesc;

    private BigDecimal threshold;

    private BigDecimal discount;

    private Integer status;

    private String statusDesc;

    private String userName;

    private String usedTime;
}
