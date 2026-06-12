package com.shuike.manager.modules.manualreview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("manual_reviews")
public class ManualReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long reviewerId;
    private String action;
    private Integer modifiedScore;
    private String modifyReason;
    private String reviewComment;
    private String revisionRequirements;
    private LocalDate deadline;
    private LocalDateTime reviewTime;
}
