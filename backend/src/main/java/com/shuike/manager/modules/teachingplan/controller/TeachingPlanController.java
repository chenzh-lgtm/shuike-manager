package com.shuike.manager.modules.teachingplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlan;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlanFile;
import com.shuike.manager.modules.teachingplan.service.TeachingPlanService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teaching-plans")
@RequiredArgsConstructor
public class TeachingPlanController {
    private final TeachingPlanService service;

    @GetMapping
    public ApiResponse<PageResult<TeachingPlan>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String status) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        IPage<TeachingPlan> result = service.page(new Page<>(page, pageSize), teacherId, courseId, semesterId, status);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<TeachingPlan> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeachingPlan> create(@Valid @RequestBody TeachingPlan plan) {
        return ApiResponse.success(service.create(plan));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeachingPlan> update(@PathVariable Long id, @Valid @RequestBody TeachingPlan plan) {
        return ApiResponse.success(service.update(id, plan));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return ApiResponse.success("提交成功", null);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return ApiResponse.success("撤回成功", null);
    }

    @GetMapping("/{id}/files")
    public ApiResponse<List<TeachingPlanFile>> files(@PathVariable Long id) {
        return ApiResponse.success(service.getFiles(id));
    }

    @PostMapping("/{id}/files")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeachingPlan> addFiles(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        return ApiResponse.success(service.addFiles(id, body.get("fileIds")));
    }
}
