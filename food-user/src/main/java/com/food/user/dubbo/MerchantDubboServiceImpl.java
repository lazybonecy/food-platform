package com.food.user.dubbo;

import com.food.api.user.MerchantDubboService;
import com.food.api.user.dto.MerchantDTO;
import com.food.user.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

@DubboService
@RequiredArgsConstructor
public class MerchantDubboServiceImpl implements MerchantDubboService {

    private final MerchantService merchantService;

    @Override
    public MerchantDTO getMerchantByUserId(Long userId) {
        com.food.user.dto.MerchantDTO local = merchantService.getMerchantInfo(userId);
        if (local == null) {
            return null;
        }
        MerchantDTO dto = new MerchantDTO();
        BeanUtils.copyProperties(local, dto);
        return dto;
    }
}
