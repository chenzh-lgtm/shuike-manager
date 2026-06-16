package com.shuike.manager.modules.course.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.course.entity.Course;
import com.shuike.manager.modules.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService service;

    @GetMapping
    public ApiResponse<PageResult<Course>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long collegeId) {
        IPage<Course> result = service.page(new Page<>(page, pageSize), collegeId);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Course> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @OperationLog(module = "课程管理", action = "CREATE", targetType = "COURSE")
    @PostMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Course> create(@RequestBody Course course) {
        service.create(course);
        return ApiResponse.success(course);
    }

    @OperationLog(module = "课程管理", action = "UPDATE", targetType = "COURSE")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Course course) {
        service.update(id, course);
        return ApiResponse.success(null);
    }

    @OperationLog(module = "课程管理", action = "DELETE", targetType = "COURSE")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
