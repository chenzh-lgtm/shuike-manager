package com.shuike.manager.modules.phasematerial.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("phase_material_files")
public class PhaseMaterialFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String textContent;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
