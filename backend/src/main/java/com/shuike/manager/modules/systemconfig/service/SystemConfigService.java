package com.shuike.manager.modules.systemconfig.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuike.manager.modules.systemconfig.entity.SystemConfig;
import com.shuike.manager.modules.systemconfig.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统配置 Service
 * 提供配置读取和更新功能，支持业务代码按 key 获取配置值
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    /**
     * 获取所有配置项列表
     */
    public List<SystemConfig> listAll() {
        return configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getId));
    }

    /**
     * 根据配置键获取配置值（字符串）
     */
    public String getValue(String key) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        return config != null ? config.getConfigValue() : null;
    }

    /**
     * 根据配置键获取配置值（整数），不存在或解析失败返回默认值
     */
    public int getIntValue(String key, int defaultValue) {
        String val = getValue(key);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 根据配置键获取配置值（布尔），不存在返回默认值
     */
    public boolean getBoolValue(String key, boolean defaultValue) {
        String val = getValue(key);
        if (val == null || val.isEmpty()) return defaultValue;
        return "true".equalsIgnoreCase(val) || "1".equals(val);
    }

    /**
     * 更新配置项
     */
    public void update(Long id, String configValue, Long updatedBy) {
        SystemConfig config = new SystemConfig();
        config.setId(id);
        config.setConfigValue(configValue);
        config.setUpdatedBy(updatedBy);
        configMapper.updateById(config);
    }

    /**
     * 根据key更新配置值
     */
    public void updateByKey(String key, String configValue, Long updatedBy) {
        SystemConfig exist = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key));
        if (exist != null) {
            update(exist.getId(), configValue, updatedBy);
        }
    }
}
