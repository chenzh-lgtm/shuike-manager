package com.shuike.manager.modules.dashboard.controller;

import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

    @GetMapping("/teacher")
    public ApiResponse<Map<String, Object>> teacherStats() {
        return ApiResponse.success(service.getTeacherStats());
    }

    @GetMapping("/college")
    public ApiResponse<Map<String, Object>> collegeStats() {
        return ApiResponse.success(service.getCollegeStats());
    }

    @GetMapping("/office")
    public ApiResponse<Map<String, Object>> officeStats() {
        return ApiResponse.success(service.getOfficeStats());
    }

    @GetMapping("/dean")
    public ApiResponse<Map<String, Object>> deanStats() {
        return ApiResponse.success(service.getDeanStats());
    }
}
