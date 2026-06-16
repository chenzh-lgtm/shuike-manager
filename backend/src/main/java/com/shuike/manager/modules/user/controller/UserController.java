package com.shuike.manager.modules.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.modules.college.entity.College;
import com.shuike.manager.modules.college.mapper.CollegeMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.entity.UserRole;
import com.shuike.manager.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;
    private final CollegeMapper collegeMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String keyword) {
        IPage<User> result = service.page(new Page<>(page, pageSize), collegeId, keyword);
        Map<Long, String> collegeCache = new HashMap<>();
        List<Map<String, Object>> enriched = result.getRecords().stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId()); item.put("username", u.getUsername());
            item.put("realName", u.getRealName()); item.put("email", u.getEmail());
            item.put("phone", u.getPhone()); item.put("collegeId", u.getCollegeId());
            item.put("status", u.getStatus()); item.put("lastLoginAt", u.getLastLoginAt());
            item.put("createdAt", u.getCreatedAt());
            String cn = collegeCache.computeIfAbsent(u.getCollegeId() != null ? u.getCollegeId() : 0L,
                    k -> { College c = k > 0 ? collegeMapper.selectById(k) : null; return c != null ? c.getName() : "-"; });
            item.put("collegeName", cn);
            // 角色
            List<UserRole> roles = service.getUserRoles(u.getId());
            item.put("roles", roles.stream().map(UserRole::getRole).collect(Collectors.toList()));
            return item;
        }).collect(Collectors.toList());
        return ApiResponse.success(PageResult.of(enriched, result.getTotal(), page, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        User user = service.getById(id);
        List<UserRole> roles = service.getUserRoles(id);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("roles", roles.stream().map(UserRole::getRole).collect(Collectors.toList()));
        if (user.getCollegeId() != null) {
            College c = collegeMapper.selectById(user.getCollegeId());
            data.put("collegeName", c != null ? c.getName() : "");
        }
        return ApiResponse.success(data);
    }

    @OperationLog(module = "用户管理", action = "CREATE", targetType = "USER")
    @PostMapping
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<User> create(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setUsername((String) body.get("username"));
        user.setPasswordHash((String) body.get("password"));
        user.setRealName((String) body.get("realName"));
        user.setEmail((String) body.get("email"));
        user.setPhone((String) body.get("phone"));
        user.setCollegeId(body.get("collegeId") != null ? ((Number) body.get("collegeId")).longValue() : null);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) body.get("roles");
        return ApiResponse.success(service.create(user, roles));
    }

    @OperationLog(module = "用户管理", action = "UPDATE", targetType = "USER")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<User> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = new User();
        user.setUsername((String) body.get("username"));
        if (body.containsKey("password")) user.setPasswordHash((String) body.get("password"));
        user.setRealName((String) body.get("realName"));
        user.setEmail((String) body.get("email"));
        user.setPhone((String) body.get("phone"));
        user.setCollegeId(body.get("collegeId") != null ? ((Number) body.get("collegeId")).longValue() : null);
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) body.get("roles");
        return ApiResponse.success(service.update(id, user, roles));
    }

    @OperationLog(module = "用户管理", action = "UPDATE", targetType = "USER")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        service.updateStatus(id, body.get("status"));
        return ApiResponse.success(null);
    }

    @OperationLog(module = "用户管理", action = "DELETE", targetType = "USER")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    /**
     * 批量导入用户（CSV/TXT格式）
     * 格式：工号,姓名,密码(可选),学院名称或ID,角色1-角色2
     */
    @OperationLog(module = "用户管理", action = "IMPORT", targetType = "USER")
    @PostMapping("/import")
    @PreAuthorize("hasRole('OFFICE')")
    public ApiResponse<Map<String, Object>> batchImport(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine && line.contains("工号")) { isFirstLine = false; continue; }
                isFirstLine = false;
                if (line.trim().isEmpty()) continue;
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 2) { fail++; errors.add(line + " -> 格式错误"); continue; }
                    String username = parts[0].trim();
                    String realName = parts[1].trim();
                    String password = parts.length > 2 && !parts[2].trim().isEmpty() ? parts[2].trim() : ("fzrjxy" + username);
                    Long collegeId = resolveCollegeId(parts.length > 3 ? parts[3].trim() : "");
                    List<String> roles = new ArrayList<>();
                    if (parts.length > 4 && !parts[4].trim().isEmpty()) {
                        for (String r : parts[4].trim().split("-")) {
                            String role = r.trim();
                            if (role.equals("教师")) roles.add("TEACHER");
                            else if (role.equals("主任") || role.equals("审核员")) roles.add("COLLEGE_REVIEWER");
                            else if (role.equals("教务处")) roles.add("OFFICE");
                            else if (role.equals("院长")) roles.add("DEAN");
                        }
                    }
                    if (roles.isEmpty()) roles.add("TEACHER");

                    User user = new User();
                    user.setUsername(username); user.setRealName(realName);
                    user.setPasswordHash(password); user.setCollegeId(collegeId);
                    service.create(user, roles);
                    success++;
                } catch (Exception e) {
                    fail++; errors.add(line + " -> " + e.getMessage());
                }
            }
        } catch (Exception e) {
            return ApiResponse.error(500, "文件解析失败: " + e.getMessage());
        }
        result.put("success", success); result.put("fail", fail); result.put("errors", errors);
        return ApiResponse.success("导入完成: 成功" + success + "条, 失败" + fail + "条", result);
    }

    /**
     * 下载导入模板（CSV格式，Excel可直接打开）
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String content = "工号,姓名,密码(可选留空默认fzrjxy+工号),学院(名称或ID),角色(教师/主任/教务处/院长 用-连接)\n" +
               "T001,张三,,数学学院,教师\n" +
               "T002,李四,,计算机学院,教师-主任\n" +
               "T003,王五,mypass,教务处,教务处\n" +
               "T004,赵六,,数学学院,院长";
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = new String("用户导入模板.csv".getBytes(java.nio.charset.StandardCharsets.UTF_8), java.nio.charset.StandardCharsets.ISO_8859_1);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream; charset=UTF-8")
                .body(bytes);
    }

    private Long resolveCollegeId(String collegeStr) {
        if (collegeStr == null || collegeStr.isEmpty()) return null;
        try { return Long.parseLong(collegeStr); } catch (NumberFormatException e) {}
        // 按学院名称匹配
        College c = collegeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<College>()
                        .eq(College::getName, collegeStr).last("LIMIT 1"));
        return c != null ? c.getId() : null;
    }
}
