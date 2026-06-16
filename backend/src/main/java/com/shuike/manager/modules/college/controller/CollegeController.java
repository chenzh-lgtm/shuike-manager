package com.shuike.manager.modules.college.controller;

import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.college.entity.College;
import com.shuike.manager.modules.college.service.CollegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {
    private final CollegeService service;

    @GetMapping
    public ApiResponse<List<College>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<College> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @OperationLog(module = "学院管理", action = "CREATE", targetType = "COLLEGE")
    @PostMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<College> create(@RequestBody College college) {
        service.create(college);
        return ApiResponse.success(college);
    }

    @OperationLog(module = "学院管理", action = "UPDATE", targetType = "COLLEGE")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody College college) {
        service.update(id, college);
        return ApiResponse.success(null);
    }

    @OperationLog(module = "学院管理", action = "DELETE", targetType = "COLLEGE")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
