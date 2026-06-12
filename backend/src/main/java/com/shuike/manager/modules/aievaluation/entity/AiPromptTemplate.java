package com.shuike.manager.modules.aievaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_templates")
public class AiPromptTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String scene;
    private String materialType;
    private String dimension;
    private String templateText;
    private String version;
    private Integer isActive;
    private String description;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
