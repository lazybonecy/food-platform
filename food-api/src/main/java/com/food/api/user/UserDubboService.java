package com.food.api.user;

import com.food.api.user.dto.UserDTO;

public interface UserDubboService {

    UserDTO getUserById(Long userId);
}
