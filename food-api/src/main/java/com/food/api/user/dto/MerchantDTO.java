package com.food.api.user.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class MerchantDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String shopName;

    private String shopDesc;

    private String logo;

    private String address;

    private String category;

    private Integer status;
}
