package com.food.user.dubbo;

import com.food.api.user.UserDubboService;
import com.food.api.user.dto.UserDTO;
import com.food.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

@DubboService
@RequiredArgsConstructor
public class UserDubboServiceImpl implements UserDubboService {

    private final UserService userService;

    @Override
    public UserDTO getUserById(Long userId) {
        com.food.user.dto.UserDTO local = userService.getUserById(userId);
        if (local == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(local, dto);
        return dto;
    }
}
