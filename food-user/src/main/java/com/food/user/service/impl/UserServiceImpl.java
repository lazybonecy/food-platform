package com.food.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.common.core.exception.BusinessException;
import com.food.user.dto.*;
import com.food.user.entity.User;
import com.food.user.mapper.UserMapper;
import com.food.user.service.UserService;
import com.food.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Map<String, String> register(RegisterDTO dto) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(dto.getRole());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);

        // 生成 token
        return generateTokens(user);
    }

    @Override
    public Map<String, String> login(LoginDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        return generateTokens(user);
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        Long userId = JwtUtil.parseToken(refreshToken);
        if (userId == null) {
            throw new BusinessException(401, "refresh token 无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(401, "用户不存在或已被禁用");
        }

        return generateTokens(user);
    }

    @Override
    public UserDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toDTO(user);
    }

    @Override
    public void updateUserInfo(Long userId, UserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());

        userMapper.updateById(user);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? toDTO(user) : null;
    }

    private Map<String, String> generateTokens(User user) {
        String accessToken = JwtUtil.generateToken(user.getId(), user.getRole(), 2 * 60 * 60 * 1000);
        String refreshToken = JwtUtil.generateToken(user.getId(), user.getRole(), 7 * 24 * 60 * 60 * 1000);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }
}
