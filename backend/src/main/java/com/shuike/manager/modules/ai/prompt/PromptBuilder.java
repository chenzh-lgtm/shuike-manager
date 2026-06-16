package com.shuike.manager.modules.ai.prompt;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuike.manager.modules.aievaluation.entity.AiPromptTemplate;
import com.shuike.manager.modules.aievaluation.mapper.AiPromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final AiPromptTemplateMapper templateMapper;

    // 材料类型中文映射
    private static final Map<String, String> MATERIAL_TYPE_LABELS = new HashMap<>();
    static {
        MATERIAL_TYPE_LABELS.put("TEACHING_PLAN", "授课计划");
        MATERIAL_TYPE_LABELS.put("LESSON_PLAN", "教案");
        MATERIAL_TYPE_LABELS.put("COURSEWARE", "课件");
        MATERIAL_TYPE_LABELS.put("EXAM_PLAN", "考核方案");
        MATERIAL_TYPE_LABELS.put("STUDENT_SAMPLE", "学生成果样本");
    }

    // 维度中文映射
    private static final Map<String, String> DIMENSION_LABELS = new HashMap<>();
    static {
        DIMENSION_LABELS.put("completeness", "内容完整性");
        DIMENSION_LABELS.put("standard_match", "与课程标准匹配度");
        DIMENSION_LABELS.put("format", "格式规范性");
        DIMENSION_LABELS.put("innovation", "创新性");
        DIMENSION_LABELS.put("ai_generated", "AI生成检测");
    }

    // 维度权重（总分加权用）
    private static final Map<String, Double> DIMENSION_WEIGHTS = new HashMap<>();
    static {
        DIMENSION_WEIGHTS.put("completeness", 0.28);
        DIMENSION_WEIGHTS.put("standard_match", 0.28);
        DIMENSION_WEIGHTS.put("format", 0.18);
        DIMENSION_WEIGHTS.put("innovation", 0.16);
        DIMENSION_WEIGHTS.put("ai_generated", 0.10);
    }

    /**
     * 获取所有评审维度的 Prompt
     */
    public Map<String, PromptPair> buildAllDimensionPrompts(
            String materialType, String materialContent, String courseStandard) {

        Map<String, PromptPair> prompts = new HashMap<>();
        String materialTypeLabel = MATERIAL_TYPE_LABELS.getOrDefault(materialType, materialType);

        // 为每个维度加载激活的 Prompt 模板
        for (String dimension : DIMENSION_LABELS.keySet()) {
            AiPromptTemplate template = loadActiveTemplate("MATERIAL_EVALUATION", materialType, dimension);
            if (template == null) {
                log.warn("[PromptBuilder] 未找到材料类型={} 维度={} 的激活模板, 使用通用模板", materialType, dimension);
                template = loadActiveTemplate("MATERIAL_EVALUATION", null, dimension);  // 回退到通用模板
            }
            if (template == null) {
                continue;
            }

            String dimensionLabel = DIMENSION_LABELS.get(dimension);

            // 替换系统 prompt 中的变量
            String systemPrompt = template.getTemplateText()
                    .replace("{{materialType}}", materialTypeLabel)
                    .replace("{{dimension}}", dimensionLabel);

            // 构建用户 prompt
            String userPrompt = buildUserPrompt(materialTypeLabel, materialContent, courseStandard);

            prompts.put(dimension, new PromptPair(systemPrompt, userPrompt, template.getVersion()));
            log.info("[PromptBuilder] 构建维度 {} prompt, version={}", dimensionLabel, template.getVersion());
        }

        return prompts;
    }

    private String buildUserPrompt(String materialTypeLabel, String materialContent, String courseStandard) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 教学材料信息\n");
        sb.append("材料类型：").append(materialTypeLabel).append("\n\n");
        sb.append("## 教学材料内容\n");
        // 限制内容长度避免LLM响应超时（6000字足够评审判断）
        sb.append(truncate(materialContent, 6000)).append("\n\n");

        if (courseStandard != null && !courseStandard.isEmpty()) {
            sb.append("## 对应课程标准（作为评价参考基准）\n");
            sb.append(truncate(courseStandard, 3000)).append("\n");
        }

        sb.append("\n请根据上述材料内容和课程标准（如有），严格按照 JSON 格式返回评审结果：{\"score\":85,\"issues\":[\"具体问题\"],\"suggestion\":\"综合建议\"}");
        return sb.toString();
    }

    /**
     * 截断文本（取前maxLen字符和末尾部分以确保完整性）
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...[内容过长已省略中间部分]...";
    }

    /**
     * 加载指定场景和维度的激活模板
     */
    private AiPromptTemplate loadActiveTemplate(String scene, String materialType, String dimension) {
        return templateMapper.selectOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getScene, scene)
                .eq(materialType != null, AiPromptTemplate::getMaterialType, materialType)
                .eq(dimension != null, AiPromptTemplate::getDimension, dimension)
                .eq(AiPromptTemplate::getIsActive, 1)
                .orderByDesc(AiPromptTemplate::getVersion)
                .last("LIMIT 1"));
    }


    /**
     * Prompt 对（系统 Prompt + 用户 Prompt）
     */
    public static class PromptPair {
        private final String systemPrompt;
        private final String userPrompt;
        private final String promptVersion;

        public PromptPair(String systemPrompt, String userPrompt, String promptVersion) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.promptVersion = promptVersion;
        }

        public String getSystemPrompt() { return systemPrompt; }
        public String getUserPrompt() { return userPrompt; }
        public String getPromptVersion() { return promptVersion; }
    }
}
