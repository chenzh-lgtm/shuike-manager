package com.shuike.manager.modules.teachingplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teaching_plans")
public class TeachingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long courseId;
    private Long semesterId;
    private String classInfo;
    private String textbookInfo;
    private String summary;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime collegeReviewTime;
    private LocalDateTime officeReviewTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
