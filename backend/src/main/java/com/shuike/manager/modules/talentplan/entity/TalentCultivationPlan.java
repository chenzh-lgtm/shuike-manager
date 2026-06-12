package com.shuike.manager.modules.talentplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("talent_cultivation_plans")
public class TalentCultivationPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String majorName;
    private String majorCode;
    private String grade;
    private Long collegeId;
    private String fileUrl;
    private String contentText;
    private String targets;
    private String requirements;
    private String courseSystem;
    private String mappingMatrix;
    private Long deanId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
