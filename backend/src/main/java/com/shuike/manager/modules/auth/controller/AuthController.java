package com.shuike.manager.modules.auth.controller;

import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.auth.dto.ChangePasswordRequest;
import com.shuike.manager.modules.auth.dto.LoginRequest;
import com.shuike.manager.modules.auth.dto.RefreshTokenRequest;
import com.shuike.manager.modules.auth.service.AuthService;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.shuike.manager.common.security.SecurityUtils;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req.getUsername(), req.getPassword()));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ApiResponse.success(authService.refresh(req.getRefreshToken()));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        User user = userMapper.selectById(SecurityUtils.getCurrentUserId());
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            return ApiResponse.error(400, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);
        return ApiResponse.success("密码修改成功", null);
    }
}
