package com.food.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserCouponListDTO {

    private List<UserCouponDTO> records;

    private Long total;

    private Long current;

    private Long size;
}
