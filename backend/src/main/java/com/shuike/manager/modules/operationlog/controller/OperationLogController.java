package com.shuike.manager.modules.operationlog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.operationlog.entity.OperationLog;
import com.shuike.manager.modules.operationlog.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 操作日志查询接口（仅教务处可访问）
 */
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     *
     * @param page      页码，默认 1
     * @param pageSize  每页大小，默认 10
     * @param module    操作模块筛选
     * @param action    操作类型筛选
     * @param keyword   关键词搜索（用户名/详情）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 分页结果
     */
    @GetMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<PageResult<OperationLog>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        IPage<OperationLog> result = operationLogService.page(page, pageSize, module, action, keyword, startTime, endTime);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }
}
