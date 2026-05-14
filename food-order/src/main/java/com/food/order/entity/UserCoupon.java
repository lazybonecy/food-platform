package com.food.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    private String couponCode;

    private Integer status;

    private BigDecimal payAmount;

    private LocalDateTime usedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
