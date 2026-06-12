package com.shuike.manager.modules.teachingplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teaching_plan_files")
public class TeachingPlanFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
