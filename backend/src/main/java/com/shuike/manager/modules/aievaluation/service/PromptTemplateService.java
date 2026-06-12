package com.shuike.manager.modules.aievaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.modules.aievaluation.entity.AiPromptTemplate;
import com.shuike.manager.modules.aievaluation.mapper.AiPromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private final AiPromptTemplateMapper templateMapper;

    // 按场景+材料类型分组查询
    public List<AiPromptTemplate> listByScene(String scene) {
        return templateMapper.selectList(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getScene, scene)
                .orderByAsc(AiPromptTemplate::getMaterialType)
                .orderByAsc(AiPromptTemplate::getDimension));
    }

    public List<AiPromptTemplate> listBySceneAndMaterialType(String scene, String materialType) {
        return templateMapper.selectList(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getScene, scene)
                .eq(AiPromptTemplate::getMaterialType, materialType)
                .orderByAsc(AiPromptTemplate::getDimension));
    }

    public AiPromptTemplate getById(Long id) {
        AiPromptTemplate t = templateMapper.selectById(id);
        if (t == null) throw new BusinessException(ErrorCode.NOT_FOUND, "Prompt模板不存在");
        return t;
    }

    @Transactional
    public AiPromptTemplate update(Long id, String templateText, String description) {
        AiPromptTemplate exist = getById(id);
        // 将旧版本设为非激活
        AiPromptTemplate reset = new AiPromptTemplate();
        reset.setIsActive(0);
        LambdaUpdateWrapper<AiPromptTemplate> uw = new LambdaUpdateWrapper<>();
        uw.eq(AiPromptTemplate::getScene, exist.getScene());
        if (exist.getMaterialType() != null) uw.eq(AiPromptTemplate::getMaterialType, exist.getMaterialType());
        if (exist.getDimension() != null) uw.eq(AiPromptTemplate::getDimension, exist.getDimension());
        uw.eq(AiPromptTemplate::getIsActive, 1);
        uw.set(AiPromptTemplate::getIsActive, 0);
        templateMapper.update(null, uw);

        // 创建新版本
        AiPromptTemplate newVer = new AiPromptTemplate();
        newVer.setScene(exist.getScene());
        newVer.setMaterialType(exist.getMaterialType());
        newVer.setDimension(exist.getDimension());
        newVer.setTemplateText(templateText);
        newVer.setVersion(bumpVersion(exist.getVersion()));
        newVer.setIsActive(1);
        newVer.setDescription(description);
        newVer.setCreatedBy(exist.getCreatedBy());
        templateMapper.insert(newVer);
        return newVer;
    }

    @Transactional
    public void toggleActive(Long id) {
        AiPromptTemplate exist = getById(id);
        // 如果激活，则设为不激活
        if (exist.getIsActive() == 1) {
            exist.setIsActive(0);
            templateMapper.updateById(exist);
            return;
        }
        // 设为激活——先取消同场景同类型同维度的其他激活模板
        AiPromptTemplate reset = new AiPromptTemplate();
        reset.setIsActive(0);
        LambdaUpdateWrapper<AiPromptTemplate> uw2 = new LambdaUpdateWrapper<>();
        uw2.eq(AiPromptTemplate::getScene, exist.getScene());
        if (exist.getMaterialType() != null) uw2.eq(AiPromptTemplate::getMaterialType, exist.getMaterialType());
        if (exist.getDimension() != null) uw2.eq(AiPromptTemplate::getDimension, exist.getDimension());
        uw2.eq(AiPromptTemplate::getIsActive, 1);
        uw2.set(AiPromptTemplate::getIsActive, 0);
        templateMapper.update(null, uw2);
        exist.setIsActive(1);
        templateMapper.updateById(exist);
    }

    // 材料类型中文映射
    public static final Map<String, String> MATERIAL_LABELS = new LinkedHashMap<>();
    static {
        MATERIAL_LABELS.put("TEACHING_PLAN", "授课计划");
        MATERIAL_LABELS.put("LESSON_PLAN", "教案");
        MATERIAL_LABELS.put("COURSEWARE", "课件");
        MATERIAL_LABELS.put("EXAM_PLAN", "考核方案");
    }

    public static final Map<String, String> DIMENSION_LABELS = new LinkedHashMap<>();
    static {
        DIMENSION_LABELS.put("completeness", "内容完整性");
        DIMENSION_LABELS.put("standard_match", "与课程标准匹配度");
        DIMENSION_LABELS.put("format", "格式规范性");
        DIMENSION_LABELS.put("innovation", "创新性");
    }

    private String bumpVersion(String ver) {
        try {
            String[] parts = ver.replace("v", "").split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts.length > 1 ? parts[1] : "0");
            return "v" + major + "." + (minor + 1);
        } catch (Exception e) { return "v1.0"; }
    }
}
