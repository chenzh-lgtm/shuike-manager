package com.shuike.manager.modules.manualreview.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.manualreview.entity.ManualReview;
import com.shuike.manager.modules.manualreview.service.ManualReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/manual-reviews")
@RequiredArgsConstructor
public class ManualReviewController {
    private final ManualReviewService service;

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('OFFICE','COLLEGE_REVIEWER')")
    public ApiResponse<PageResult<ManualReview>> pending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<ManualReview> result = service.page(new Page<>(page, pageSize));
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('OFFICE','COLLEGE_REVIEWER')")
    public ApiResponse<PageResult<ManualReview>> history(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<ManualReview> result = service.page(new Page<>(page, pageSize));
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/evaluation/{evaluationId}")
    public ApiResponse<List<ManualReview>> byEvaluation(@PathVariable Long evaluationId) {
        return ApiResponse.success(service.getByEvaluationId(evaluationId));
    }

    @OperationLog(module = "审核管理", action = "REVIEW", targetType = "MANUAL_REVIEW")
    @PostMapping("/{evaluationId}")
    @PreAuthorize("hasAnyRole('OFFICE','COLLEGE_REVIEWER')")
    public ApiResponse<ManualReview> review(@PathVariable Long evaluationId, @RequestBody Map<String, Object> body) {
        String action = (String) body.get("action");
        Integer modifiedScore = body.get("modifiedScore") != null ? ((Number) body.get("modifiedScore")).intValue() : null;
        String modifyReason = (String) body.get("modifyReason");
        String reviewComment = (String) body.get("reviewComment");
        String revisionRequirements = (String) body.get("revisionRequirements");
        LocalDate deadline = body.get("deadline") != null ? LocalDate.parse((String) body.get("deadline")) : null;
        String reviewLevel = (String) body.getOrDefault("reviewLevel", "COLLEGE");
        return ApiResponse.success(service.review(evaluationId, action, modifiedScore, modifyReason,
                reviewComment, revisionRequirements, deadline, reviewLevel));
    }
}
