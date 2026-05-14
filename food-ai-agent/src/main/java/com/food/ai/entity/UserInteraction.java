package com.food.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_interaction")
public class UserInteraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    private Integer type;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
