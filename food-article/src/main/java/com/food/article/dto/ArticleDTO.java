package com.food.article.dto;

import lombok.Data;

@Data
public class ArticleDTO {

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
    private String createTime;

    // 用户交互状态
    private Boolean liked;
    private Boolean collected;
}
