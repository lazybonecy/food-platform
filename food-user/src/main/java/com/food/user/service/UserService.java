package com.food.user.service;

import com.food.user.dto.*;

import java.util.Map;

public interface UserService {

    Map<String, String> register(RegisterDTO dto);

    Map<String, String> login(LoginDTO dto);

    Map<String, String> refreshToken(String refreshToken);

    UserDTO getUserInfo(Long userId);

    void updateUserInfo(Long userId, UserDTO dto);

    UserDTO getUserById(Long userId);
}
