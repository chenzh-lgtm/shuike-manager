package com.shuike.manager.modules.alignment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alignment_reports")
public class AlignmentReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tcpId;
    private String majorName;
    private Long collegeId;
    private String industryKeywords;
    private Integer coverageScore;
    private String gapAnalysis;
    private String suggestions;
    private String dimensionScores;
    private String reportJson;
    private String modelVersion;
    private Long initiatorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
