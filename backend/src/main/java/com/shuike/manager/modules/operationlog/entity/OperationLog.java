package com.shuike.manager.modules.operationlog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 记录所有关键操作：登录、创建、审核、删除等
 */
@Data
@TableName("operation_logs")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 操作模块：认证/材料管理/审核管理/用户管理等 */
    private String module;

    /** 操作类型：LOGIN/CREATE/UPDATE/DELETE/APPROVE/REJECT/IMPORT/ANALYZE */
    private String action;

    /** 目标类型（如 TEACHING_PLAN、USER 等） */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    /** 操作详情（JSON 格式，记录请求参数摘要） */
    private String detail;

    /** 客户端 IP */
    private String ipAddress;

    /** 浏览器 User-Agent */
    private String userAgent;

    /** 操作耗时（毫秒） */
    private Integer costMs;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
