package com.shuike.manager.modules.coursestandard.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.course.entity.Course;
import com.shuike.manager.modules.course.mapper.CourseMapper;
import com.shuike.manager.modules.coursestandard.entity.CourseStandard;
import com.shuike.manager.modules.coursestandard.service.CourseStandardService;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/course-standards")
@RequiredArgsConstructor
public class CourseStandardController {
    private final CourseStandardService service;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long courseId) {
        // 非教务处用户：自动按自己的deanId过滤
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        List<String> currentRoles = SecurityUtils.getCurrentUserRoles();
        Long deanId = null;
        if (!currentRoles.contains("OFFICE")) {
            deanId = currentUserId;  // 院长只看自己创建的课程标准
        }
        IPage<CourseStandard> result = service.page(new Page<>(page, pageSize), courseId, deanId);
        Map<Long, String> courseNameCache = new HashMap<>();
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (CourseStandard cs : result.getRecords()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", cs.getId()); item.put("courseId", cs.getCourseId());
            item.put("tcpId", cs.getTcpId()); item.put("fileUrl", cs.getFileUrl());
            item.put("contentText", cs.getContentText()); item.put("knowledgePoints", cs.getKnowledgePoints());
            item.put("abilityTargets", cs.getAbilityTargets()); item.put("status", cs.getStatus());
            item.put("createdAt", cs.getCreatedAt()); item.put("updatedAt", cs.getUpdatedAt());
            String cn = courseNameCache.computeIfAbsent(cs.getCourseId() != null ? cs.getCourseId() : 0L,
                    k -> { Course c = k > 0 ? courseMapper.selectById(k) : null; return c != null ? c.getName() : "-"; });
            item.put("courseName", cn);
            enriched.add(item);
        }
        return ApiResponse.success(PageResult.of(enriched, result.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<CourseStandard> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<CourseStandard> create(@RequestBody CourseStandard standard) {
        standard.setDeanId(SecurityUtils.getCurrentUserId());
        service.create(standard);
        return ApiResponse.success(standard);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody CourseStandard standard) {
        service.update(id, standard);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
