package com.food.api.user;

import com.food.api.user.dto.MerchantDTO;

public interface MerchantDubboService {

    MerchantDTO getMerchantByUserId(Long userId);
}
