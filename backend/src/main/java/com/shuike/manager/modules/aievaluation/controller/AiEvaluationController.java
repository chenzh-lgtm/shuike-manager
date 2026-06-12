package com.shuike.manager.modules.aievaluation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.service.AiEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-evaluations")
@RequiredArgsConstructor
public class AiEvaluationController {
    private final AiEvaluationService service;

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<AiEvaluation> submit(@RequestBody Map<String, Long> body) {
        return ApiResponse.success(service.submit(body.get("materialId")));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiEvaluation> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<PageResult<AiEvaluation>> myList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        IPage<AiEvaluation> result = service.pageMine(new Page<>(page, pageSize));
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<PageResult<AiEvaluation>> allList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String status) {
        IPage<AiEvaluation> result = service.pageAll(new Page<>(page, pageSize), status);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }
}
