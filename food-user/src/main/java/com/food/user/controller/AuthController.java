package com.food.user.controller;

import com.food.common.core.result.R;
import com.food.user.dto.LoginDTO;
import com.food.user.dto.RegisterDTO;
import com.food.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public R<Map<String, String>> register(@Valid @RequestBody RegisterDTO dto) {
        return R.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public R<Map<String, String>> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok(userService.login(dto));
    }

    @PostMapping("/refresh")
    public R<Map<String, String>> refresh(@RequestBody Map<String, String> body) {
        return R.ok(userService.refreshToken(body.get("refreshToken")));
    }
}
