package com.food.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long merchantId;

    private String title;

    private String content;

    private String coverImage;

    private String category;

    private Integer status;

    private Integer viewCount;

    private Integer likeCount;

    private Integer collectCount;

    private Long couponId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
