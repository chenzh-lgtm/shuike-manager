package com.shuike.manager.common.aspect;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，AOP 切面自动记录操作日志到数据库
 *
 * 使用示例：
 * <pre>
 * &#064;OperationLog(module = "材料管理", action = "CREATE", targetType = "TEACHING_PLAN")
 * public ApiResponse create(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块，如：认证/材料管理/审核管理/用户管理/对齐分析/Prompt管理 */
    String module() default "";

    /** 操作类型，如：LOGIN/CREATE/UPDATE/DELETE/APPROVE/REJECT/IMPORT/ANALYZE */
    String action() default "";

    /** 目标类型（可选），如：TEACHING_PLAN/LESSON_PLAN/USER/COLLEGE */
    String targetType() default "";

    /** 兼容旧版 value 属性 */
    String value() default "";
}
