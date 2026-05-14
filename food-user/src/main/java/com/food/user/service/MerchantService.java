package com.food.user.service;

import com.food.user.dto.MerchantDTO;

import java.util.Map;

public interface MerchantService {

    Map<String, String> apply(Long userId, MerchantDTO dto);

    MerchantDTO getMerchantInfo(Long userId);

    void updateMerchantInfo(Long userId, MerchantDTO dto);
}
