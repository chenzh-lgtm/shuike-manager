package com.shuike.manager.modules.alignment.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.aspect.OperationLog;
import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.common.response.PageResult;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.alignment.entity.AlignmentReport;
import com.shuike.manager.modules.alignment.service.AlignmentService;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alignment")
@RequiredArgsConstructor
public class AlignmentController {
    private final AlignmentService service;
    private final UserMapper userMapper;

    /**
     * 发起AI分析（异步，立即返回）
     */
    @OperationLog(module = "对齐分析", action = "ANALYZE", targetType = "TALENT_PLAN")
    @PostMapping("/analyze")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<String> analyze(@RequestBody Map<String, Long> body) {
        Long tcpId = body.get("tcpId");
        if (tcpId == null) return ApiResponse.error(400, "请选择人培方案");
        Long userId = SecurityUtils.getCurrentUserId();
        service.analyzeAsync(tcpId, userId);
        return ApiResponse.success("AI分析已启动，预计1-2分钟完成，请稍后刷新列表查看");
    }

    @GetMapping("/reports")
    public ApiResponse<PageResult<AlignmentReport>> reports(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long collegeId) {

        // 教师只能看本院院长的分析报告，自动过滤
        Long currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userMapper.selectById(currentUserId);
        List<String> currentRoles = SecurityUtils.getCurrentUserRoles();

        if (!currentRoles.contains("OFFICE")) {
            // 非教务处用户：自动按自己的学院过滤
            collegeId = currentUser != null ? currentUser.getCollegeId() : null;
        }
        // 教务处用户：可选collegeId参数，不传则看全部

        IPage<AlignmentReport> result = service.page(new Page<>(page, pageSize), collegeId);
        return ApiResponse.success(PageResult.of(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<AlignmentReport> reportDetail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @OperationLog(module = "对齐分析", action = "DELETE", targetType = "ALIGNMENT_REPORT")
    @DeleteMapping("/reports/{id}")
    @PreAuthorize("hasRole('DEAN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }

    /** 导出报告为可打印的HTML页面（浏览器打印为PDF） */
    @GetMapping(value = "/reports/{id}/pdf", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String exportPdf(@PathVariable Long id) {
        AlignmentReport report = service.getById(id);
        if (report == null) return "<h1>报告不存在</h1>";
        return service.buildPdfHtml(report);
    }
}
