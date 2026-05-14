package com.food.order.dto;

import lombok.Data;

import java.util.List;

@Data
public class CouponListDTO {

    private List<CouponDTO> records;

    private Long total;

    private Long current;

    private Long size;
}
