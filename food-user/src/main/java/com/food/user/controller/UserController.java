package com.food.user.controller;

import com.food.common.core.constant.CommonConstant;
import com.food.common.core.result.R;
import com.food.user.dto.UserDTO;
import com.food.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    public R<UserDTO> getUserInfo(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId) {
        return R.ok(userService.getUserInfo(userId));
    }

    @PutMapping("/info")
    public R<Void> updateUserInfo(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                  @RequestBody UserDTO dto) {
        userService.updateUserInfo(userId, dto);
        return R.ok();
    }
}
