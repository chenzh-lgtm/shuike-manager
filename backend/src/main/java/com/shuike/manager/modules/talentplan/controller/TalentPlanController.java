package com.shuike.manager.modules.talentplan.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.talentplan.entity.TalentCultivationPlan;
import com.shuike.manager.modules.talentplan.service.TalentPlanService;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/talent-plans")
@RequiredArgsConstructor
public class TalentPlanController {
    private final TalentPlanService service;
    private final UserMapper userMapper;

    @GetMapping
    public ApiResponse<PageResult<TalentCultivationPlan>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String status) {
        // 非教务处用户：自动按自己的学院过滤
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        List<String> currentRoles = SecurityUtils.getCurrentUserRoles();
        if (!currentRoles.contains("OFFICE") && currentUser != null && currentUser.getCollegeId() != null) {
            collegeId = currentUser.getCollegeId();
        }
        IPage<TalentCultivationPlan> result = service.page(new Page<>(page, pageSize), collegeId, status);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<TalentCultivationPlan> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<TalentCultivationPlan> create(@RequestBody TalentCultivationPlan plan) {
        plan.setDeanId(SecurityUtils.getCurrentUserId());
        service.create(plan);
        return ApiResponse.success(plan);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody TalentCultivationPlan plan) {
        service.update(id, plan);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/mapping")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> updateMapping(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TalentCultivationPlan plan = service.getById(id);
        plan.setTargets(body.get("targets"));
        plan.setRequirements(body.get("requirements"));
        plan.setCourseSystem(body.get("courseSystem"));
        plan.setMappingMatrix(body.get("mappingMatrix"));
        service.update(id, plan);
        return ApiResponse.success(null);
    }
}
