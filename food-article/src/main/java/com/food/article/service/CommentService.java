package com.food.article.service;

import com.food.article.dto.CommentDTO;

import java.util.List;

public interface CommentService {

    CommentDTO addComment(Long userId, Long articleId, String content, Long parentId, Long replyToUserId);

    void deleteComment(Long userId, Long commentId);

    List<CommentDTO> listComments(Long articleId);
}
