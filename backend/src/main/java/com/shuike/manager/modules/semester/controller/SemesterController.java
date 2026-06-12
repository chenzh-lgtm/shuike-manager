package com.shuike.manager.modules.semester.controller;

import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.semester.entity.Semester;
import com.shuike.manager.modules.semester.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {
    private final SemesterService service;

    @GetMapping
    public ApiResponse<List<Semester>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Semester> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Semester> create(@RequestBody Semester semester) {
        service.create(semester);
        return ApiResponse.success(semester);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Semester semester) {
        service.update(id, semester);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> activate(@PathVariable Long id) {
        service.activate(id);
        return ApiResponse.success(null);
    }
}
