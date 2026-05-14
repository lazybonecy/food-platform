package com.food.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.food.common.core.exception.BusinessException;
import com.food.user.dto.MerchantDTO;
import com.food.user.entity.Merchant;
import com.food.user.entity.User;
import com.food.user.mapper.MerchantMapper;
import com.food.user.mapper.UserMapper;
import com.food.user.service.MerchantService;
import com.food.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Map<String, String> apply(Long userId, MerchantDTO dto) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        if (merchantMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("您已提交过入驻申请");
        }

        Merchant merchant = new Merchant();
        merchant.setUserId(userId);
        merchant.setShopName(dto.getShopName());
        merchant.setShopDesc(dto.getShopDesc());
        merchant.setLogo(dto.getLogo());
        merchant.setAddress(dto.getAddress());
        merchant.setCategory(dto.getCategory());
        merchant.setStatus(1);
        merchantMapper.insert(merchant);

        // Update user role to merchant
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getRole, 1));

        // Return new tokens with updated role
        String accessToken = JwtUtil.generateToken(userId, 1, 2 * 60 * 60 * 1000);
        String refreshToken = JwtUtil.generateToken(userId, 1, 7 * 24 * 60 * 60 * 1000);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    @Override
    public MerchantDTO getMerchantInfo(Long userId) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        Merchant merchant = merchantMapper.selectOne(wrapper);

        if (merchant == null) {
            throw new BusinessException("商家信息不存在");
        }

        MerchantDTO dto = new MerchantDTO();
        BeanUtils.copyProperties(merchant, dto);
        return dto;
    }

    @Override
    public void updateMerchantInfo(Long userId, MerchantDTO dto) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Merchant::getUserId, userId);
        Merchant merchant = merchantMapper.selectOne(wrapper);

        if (merchant == null) {
            throw new BusinessException("商家信息不存在");
        }

        if (dto.getShopName() != null) merchant.setShopName(dto.getShopName());
        if (dto.getShopDesc() != null) merchant.setShopDesc(dto.getShopDesc());
        if (dto.getLogo() != null) merchant.setLogo(dto.getLogo());
        if (dto.getAddress() != null) merchant.setAddress(dto.getAddress());
        if (dto.getCategory() != null) merchant.setCategory(dto.getCategory());

        merchantMapper.updateById(merchant);
    }
}
