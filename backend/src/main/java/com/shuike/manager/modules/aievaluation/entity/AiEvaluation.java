package com.shuike.manager.modules.aievaluation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_evaluations")
public class AiEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Integer score;
    private String dimensionScores;
    private String suggestions;
    private String rawResponse;
    private String modelVersion;
    private String promptVersion;
    private LocalDateTime evalTime;
    private Integer costMs;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
