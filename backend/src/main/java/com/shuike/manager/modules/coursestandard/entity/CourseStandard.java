package com.shuike.manager.modules.coursestandard.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("course_standards")
public class CourseStandard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long tcpId;
    private String fileUrl;
    private String contentText;
    private String knowledgePoints;
    private String abilityTargets;
    private Long deanId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
