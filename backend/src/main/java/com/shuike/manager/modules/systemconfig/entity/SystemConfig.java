package com.shuike.manager.modules.systemconfig.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 映射 system_configs 表，用于教务处在线管理系统参数
 */
@Data
@TableName("system_configs")
public class SystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键（如 enable_captcha、ai_temperature） */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String description;

    /** 最后修改人ID */
    private Long updatedBy;

    /** 最后修改时间（自动更新） */
    private LocalDateTime updatedAt;
}
