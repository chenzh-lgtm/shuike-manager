package com.shuike.manager.modules.aievaluation.controller;

import com.shuike.manager.common.response.ApiResponse;
import com.shuike.manager.modules.aievaluation.entity.AiPromptTemplate;
import com.shuike.manager.modules.aievaluation.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/prompt-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OFFICE')")
public class PromptTemplateController {

    private final PromptTemplateService service;

    /**
     * 按场景查询所有Prompt模板（按材料类型+维度分组）
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam(defaultValue = "MATERIAL_EVALUATION") String scene) {
        List<AiPromptTemplate> all = service.listByScene(scene);
        // 按材料类型分组
        Map<String, List<AiPromptTemplate>> grouped = new LinkedHashMap<>();
        for (AiPromptTemplate t : all) {
            String key = t.getMaterialType() != null ? t.getMaterialType() : "通用";
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("materialLabels", PromptTemplateService.MATERIAL_LABELS);
        result.put("dimensionLabels", PromptTemplateService.DIMENSION_LABELS);
        result.put("templates", grouped);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiPromptTemplate> detail(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    /**
     * 更新Prompt模板（自动创建新版本，旧版本设为不激活）
     */
    @PutMapping("/{id}")
    public ApiResponse<AiPromptTemplate> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ApiResponse.success(service.update(id,
                body.get("templateText"), body.get("description")));
    }

    /**
     * 切换激活状态
     */
    @PutMapping("/{id}/toggle-active")
    public ApiResponse<Void> toggleActive(@PathVariable Long id) {
        service.toggleActive(id);
        return ApiResponse.success("操作成功", null);
    }
}
