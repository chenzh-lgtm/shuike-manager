package com.shuike.manager.modules.operationlog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.modules.operationlog.entity.OperationLog;
import com.shuike.manager.modules.operationlog.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 操作日志 Service
 * 提供日志写入和分页查询功能
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 分页查询操作日志，支持多条件筛选
     *
     * @param page      当前页码
     * @param pageSize  每页大小
     * @param module    操作模块（可选）
     * @param action    操作类型（可选）
     * @param keyword   关键词，模糊匹配 username 和 detail（可选）
     * @param startTime 开始时间（可选）
     * @param endTime   结束时间（可选）
     * @return 分页结果
     */
    public IPage<OperationLog> page(long page, long pageSize, String module, String action,
                                     String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(OperationLog::getAction, action);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getDetail, keyword));
        }
        if (startTime != null) {
            wrapper.ge(OperationLog::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLog::getCreatedAt, endTime);
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);
        return operationLogMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 保存操作日志
     *
     * @param log 操作日志实体
     */
    public void save(OperationLog log) {
        operationLogMapper.insert(log);
    }
}
