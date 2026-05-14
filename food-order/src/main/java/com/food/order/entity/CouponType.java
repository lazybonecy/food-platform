package com.food.order.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponType {

    THRESHOLD(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    FREE(3, "免费券");

    private final int code;
    private final String desc;

    public static CouponType fromCode(int code) {
        for (CouponType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的券类型: " + code);
    }
}
