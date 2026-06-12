package com.shuike.manager.modules.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.entity.UserRole;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.shuike.manager.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public IPage<User> page(Page<User> page, Long collegeId, String keyword) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (collegeId != null) qw.eq(User::getCollegeId, collegeId);
        if (keyword != null && !keyword.isEmpty()) qw.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        qw.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(page, qw);
    }

    public User getById(Long id) { return userMapper.selectById(id); }

    @Transactional
    public User create(User user, List<String> roles) {
        if (userMapper.exists(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()))) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME, "用户名已存在");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        userMapper.insert(user);
        if (roles != null) {
            for (String role : roles) {
                UserRole ur = new UserRole(); ur.setUserId(user.getId()); ur.setRole(role); roleMapper.insert(ur);
            }
        }
        return user;
    }

    @Transactional
    public User update(Long id, User user, List<String> roles) {
        user.setId(id);
        if (user.getPasswordHash() != null && !user.getPasswordHash().isEmpty()) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        } else {
            user.setPasswordHash(null);
        }
        userMapper.updateById(user);
        if (roles != null) {
            roleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
            for (String role : roles) {
                UserRole ur = new UserRole(); ur.setUserId(id); ur.setRole(role); roleMapper.insert(ur);
            }
        }
        return userMapper.selectById(id);
    }

    public List<UserRole> getUserRoles(Long userId) {
        return roleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
    }

    public void updateStatus(Long id, Integer status) {
        User user = new User(); user.setId(id); user.setStatus(status); userMapper.updateById(user);
    }

    public void delete(Long id) {
        roleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        userMapper.deleteById(id);
    }
}
