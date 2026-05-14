package com.food.article.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    private String content;

    private Long parentId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
