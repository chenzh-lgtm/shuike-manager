package com.shuike.manager.common.aspect;

import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.common.security.UserPrincipal;
import com.shuike.manager.modules.operationlog.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 拦截标注了 @OperationLog 的方法，自动记录操作日志到数据库
 *
 * detail 格式："{操作人姓名} {动作}了 {目标描述}"
 * 例如："张三 提交了「授课计划」"、"教务管理员 通过了「授课计划」审核"
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    /** action 中文映射 */
    private static final java.util.Map<String, String> ACTION_LABELS = new java.util.HashMap<>();
    static {
        ACTION_LABELS.put("LOGIN", "登录系统");
        ACTION_LABELS.put("CREATE", "创建");
        ACTION_LABELS.put("UPDATE", "编辑");
        ACTION_LABELS.put("DELETE", "删除");
        ACTION_LABELS.put("SUBMIT", "提交");
        ACTION_LABELS.put("WITHDRAW", "撤回");
        ACTION_LABELS.put("APPROVE", "通过");
        ACTION_LABELS.put("REJECT", "驳回");
        ACTION_LABELS.put("REVIEW", "审核");
        ACTION_LABELS.put("IMPORT", "批量导入");
        ACTION_LABELS.put("EXPORT", "导出");
        ACTION_LABELS.put("ANALYZE", "发起AI分析");
        ACTION_LABELS.put("UPLOAD", "上传文件");
    }

    /** targetType 中文映射 */
    private static final java.util.Map<String, String> TARGET_LABELS = new java.util.HashMap<>();
    static {
        TARGET_LABELS.put("USER", "用户");
        TARGET_LABELS.put("COLLEGE", "学院");
        TARGET_LABELS.put("COURSE", "课程");
        TARGET_LABELS.put("SEMESTER", "学期");
        TARGET_LABELS.put("PHASE_MATERIAL", "教学材料");
        TARGET_LABELS.put("TEACHING_PLAN", "授课计划");
        TARGET_LABELS.put("LESSON_PLAN", "教案");
        TARGET_LABELS.put("COURSEWARE", "课件");
        TARGET_LABELS.put("EXAM_PLAN", "考核方案");
        TARGET_LABELS.put("MANUAL_REVIEW", "审核记录");
        TARGET_LABELS.put("TALENT_PLAN", "人培方案");
        TARGET_LABELS.put("COURSE_STANDARD", "课程标准");
        TARGET_LABELS.put("PROMPT_TEMPLATE", "Prompt模板");
        TARGET_LABELS.put("ALIGNMENT_REPORT", "对齐分析报告");
        TARGET_LABELS.put("FILE", "文件");
        TARGET_LABELS.put("SYSTEM_CONFIG", "系统配置");
    }

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint point, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = point.proceed();
        try {
            saveOperationLog(point, opLog, start);
        } catch (Exception e) {
            log.error("[OP_LOG] 保存操作日志失败: {}", e.getMessage());
        }
        return result;
    }

    private void saveOperationLog(ProceedingJoinPoint point, OperationLog annotation, long startTime) {
        com.shuike.manager.modules.operationlog.entity.OperationLog entity =
                new com.shuike.manager.modules.operationlog.entity.OperationLog();

        // 操作人信息
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        String realName = null;
        if (currentUser != null) {
            entity.setUserId(currentUser.getUserId());
            entity.setUsername(currentUser.getUsername());
            realName = currentUser.getUsername(); // UserPrincipal.username 存的是登录名
        } else {
            entity.setUsername("anonymous");
        }

        // 注解信息
        entity.setModule(annotation.module());
        entity.setAction(annotation.action());
        entity.setTargetType(annotation.targetType());

        // 自动提取 @PathVariable id 作为 targetId
        entity.setTargetId(extractPathId(point));

        // 耗时
        long cost = System.currentTimeMillis() - startTime;
        entity.setCostMs((int) cost);

        // 构建人类可读的 detail
        entity.setDetail(buildDetail(entity.getUsername(), realName, annotation));

        // 请求信息
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entity.setIpAddress(getClientIp(request));
                String ua = request.getHeader("User-Agent");
                if (ua != null && ua.length() > 500) ua = ua.substring(0, 500);
                entity.setUserAgent(ua);
            }
        } catch (Exception ignored) {}

        entity.setCreatedAt(LocalDateTime.now());
        operationLogService.save(entity);
        log.info("[OP_LOG] {}", entity.getDetail());
    }

    /** 构建可读的操作描述 */
    private String buildDetail(String username, String realName, OperationLog annotation) {
        String userLabel = username != null ? username : "anonymous";
        String actionLabel = ACTION_LABELS.getOrDefault(annotation.action(), annotation.action());
        String targetLabel = TARGET_LABELS.getOrDefault(annotation.targetType(),
                annotation.targetType().isEmpty() ? "" : annotation.targetType());

        StringBuilder sb = new StringBuilder();
        sb.append(userLabel).append(" ").append(actionLabel);

        if (!targetLabel.isEmpty()) {
            sb.append("了「").append(targetLabel).append("」");
        }

        if (!annotation.module().isEmpty()) {
            sb.append("（").append(annotation.module()).append("）");
        }

        return sb.toString();
    }

    /** 从方法参数中提取 @PathVariable 的 ID */
    private Long extractPathId(ProceedingJoinPoint point) {
        try {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Parameter[] params = signature.getMethod().getParameters();
            Object[] args = point.getArgs();
            for (int i = 0; i < params.length; i++) {
                if (params[i].isAnnotationPresent(org.springframework.web.bind.annotation.PathVariable.class)) {
                    Object arg = args[i];
                    if (arg instanceof Long) return (Long) arg;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
