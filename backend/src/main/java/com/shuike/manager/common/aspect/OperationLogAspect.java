package com.shuike.manager.common.aspect;

import com.shuike.manager.common.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {
    @Around("@annotation(com.shuike.manager.common.aspect.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = point.proceed();
        long cost = System.currentTimeMillis() - start;
        String method = point.getSignature().toShortString();
        String user = SecurityUtils.getCurrentUser() != null ? SecurityUtils.getCurrentUser().getUsername() : "anonymous";
        log.info("[OP_LOG] user={} method={} cost={}ms", user, method, cost);
        return result;
    }
}
