package com.food.article.dto;

import lombok.Data;

@Data
public class NotificationDTO {

    private Long id;
    private String fromNickname;
    private String fromAvatar;
    private Integer type;
    private String typeDesc;
    private Long articleId;
    private String articleTitle;
    private Long commentId;
    private String content;
    private Integer isRead;
    private String createTime;
}
