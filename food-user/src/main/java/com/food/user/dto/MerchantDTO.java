package com.food.user.dto;

import lombok.Data;

@Data
public class MerchantDTO {
    private Long id;
    private Long userId;
    private String shopName;
    private String shopDesc;
    private String logo;
    private String address;
    private String category;
    private Integer status;
}
