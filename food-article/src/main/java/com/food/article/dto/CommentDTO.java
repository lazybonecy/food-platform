package com.food.article.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentDTO {

    private Long id;
    private Long articleId;
    private Long userId;
    private String nickname;
    private String content;
    private Long parentId;
    private String createTime;
    private List<CommentDTO> replies;
}
