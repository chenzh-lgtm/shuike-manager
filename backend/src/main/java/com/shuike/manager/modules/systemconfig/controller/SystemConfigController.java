package com.shuike.manager.modules.systemconfig.controller;

import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.systemconfig.entity.SystemConfig;
import com.shuike.manager.modules.systemconfig.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置管理接口（仅教务处可访问）
 */
@RestController
@RequestMapping("/api/system-configs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OFFICE')")
public class SystemConfigController {

    private final SystemConfigService configService;

    /**
     * 获取所有系统配置项
     */
    @GetMapping
    public ApiResponse<List<SystemConfig>> list() {
        return ApiResponse.success(configService.listAll());
    }

    /**
     * 更新单个配置项
     */
    @OperationLog(module = "系统配置", action = "UPDATE", targetType = "SYSTEM_CONFIG")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        configService.update(id, body.get("configValue"), userId);
        return ApiResponse.success("配置已更新", null);
    }
}
