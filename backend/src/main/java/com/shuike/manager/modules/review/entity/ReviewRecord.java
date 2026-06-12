package com.shuike.manager.modules.review.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("review_records")
public class ReviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long reviewerId;
    private String reviewLevel;
    private String action;
    private String comment;
    private LocalDateTime reviewTime;
}
