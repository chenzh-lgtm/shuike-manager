package com.shuike.manager.modules.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationMapper mapper;

    public IPage<Notification> page(Page<Notification> page) {
        Long userId = SecurityUtils.getCurrentUserId();
        return mapper.selectPage(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).orderByDesc(Notification::getCreatedAt));
    }

    public long unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return mapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0));
    }

    @Transactional
    public void markRead(Long id) {
        Notification n = new Notification(); n.setId(id); n.setIsRead(1); n.setReadAt(LocalDateTime.now());
        mapper.updateById(n);
    }

    @Transactional
    public void markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        Notification n = new Notification(); n.setIsRead(1); n.setReadAt(LocalDateTime.now());
        mapper.update(n, new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId).eq(Notification::getIsRead, 0));
    }
}
