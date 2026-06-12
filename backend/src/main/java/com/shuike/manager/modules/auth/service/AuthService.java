package com.shuike.manager.modules.auth.service;

import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.security.JwtTokenProvider;
import com.shuike.manager.modules.college.entity.College;
import com.shuike.manager.modules.college.mapper.CollegeMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.entity.UserRole;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.shuike.manager.modules.user.mapper.UserRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final CollegeMapper collegeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账户已被禁用");
        }
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        List<String> roles = userRoles.stream().map(UserRole::getRole).collect(Collectors.toList());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("refreshToken", refreshToken);
        result.put("expiresIn", 7200);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("collegeId", user.getCollegeId());
        if (user.getCollegeId() != null) {
            College college = collegeMapper.selectById(user.getCollegeId());
            userInfo.put("collegeName", college != null ? college.getName() : "");
        }
        userInfo.put("roles", roles);
        userInfo.put("avatar", user.getAvatar());
        result.put("userInfo", userInfo);
        return result;
    }

    public Map<String, String> refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "RefreshToken无效或已过期");
        }
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已禁用");
        }
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        List<String> roles = userRoles.stream().map(UserRole::getRole).collect(Collectors.toList());
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), roles);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);
        Map<String, String> result = new HashMap<>();
        result.put("token", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        return result;
    }
}
