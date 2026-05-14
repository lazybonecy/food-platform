package com.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CouponDTO {

    private Long id;

    private Long merchantId;

    private Long articleId;

    private String title;

    private String description;

    private Integer type;

    private String typeDesc;

    private BigDecimal threshold;

    private BigDecimal discount;

    private BigDecimal originalPrice;

    private Integer totalCount;

    private Integer claimedCount;

    private Integer limitPerUser;

    private Integer remainCount;

    private String startTime;

    private String endTime;

    private Integer status;

    private String createTime;
}
