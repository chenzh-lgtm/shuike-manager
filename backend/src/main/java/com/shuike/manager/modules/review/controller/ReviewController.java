package com.shuike.manager.modules.review.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.review.entity.ReviewRecord;
import com.shuike.manager.modules.review.service.ReviewService;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlan;
import com.shuike.manager.modules.teachingplan.service.TeachingPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final TeachingPlanService planService;

    @GetMapping("/college/pending")
    @PreAuthorize("hasRole('COLLEGE_REVIEWER')")
    public ApiResponse<PageResult<TeachingPlan>> collegePending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        IPage<TeachingPlan> result = planService.pageForCollege(new Page<>(page, pageSize), null, "SUBMITTED");
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/office/pending")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<PageResult<TeachingPlan>> officePending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<TeachingPlan> result = planService.pageForOffice(new Page<>(page, pageSize), "COLLEGE_PASSED");
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @OperationLog(module = "审核管理", action = "REVIEW", targetType = "TEACHING_PLAN")
    @PostMapping("/college/{planId}")
    @PreAuthorize("hasRole('COLLEGE_REVIEWER')")
    public ApiResponse<Void> collegeReview(@PathVariable Long planId, @RequestBody Map<String, String> body) {
        reviewService.collegeReview(planId, body.get("action"), body.get("comment"));
        return ApiResponse.success("审核完成", null);
    }

    @OperationLog(module = "审核管理", action = "REVIEW", targetType = "TEACHING_PLAN")
    @PostMapping("/office/{planId}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> officeReview(@PathVariable Long planId, @RequestBody Map<String, String> body) {
        reviewService.officeReview(planId, body.get("action"), body.get("comment"), body.get("rejectTarget"));
        return ApiResponse.success("终审完成", null);
    }

    @PostMapping("/college/batch")
    @PreAuthorize("hasRole('COLLEGE_REVIEWER')")
    public ApiResponse<Void> collegeBatchReview(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> planIds = ((List<Integer>) body.get("planIds")).stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
        reviewService.collegeBatchReview(planIds, (String) body.get("action"), (String) body.get("comment"));
        return ApiResponse.success("批量审核完成", null);
    }

    @GetMapping("/history/{planId}")
    public ApiResponse<List<ReviewRecord>> history(@PathVariable Long planId) {
        return ApiResponse.success(reviewService.getHistory(planId));
    }
}
