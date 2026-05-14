package com.food.article.controller;

import com.food.article.dto.CommentDTO;
import com.food.article.service.CommentService;
import com.food.common.core.constant.CommonConstant;
import com.food.common.core.result.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/article/{articleId}")
    public R<List<CommentDTO>> listComments(@PathVariable Long articleId) {
        return R.ok(commentService.listComments(articleId));
    }

    @PostMapping
    public R<CommentDTO> addComment(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                    @RequestBody Map<String, Object> body) {
        Long articleId = Long.valueOf(body.get("articleId").toString());
        String content = (String) body.get("content");
        Long parentId = body.get("parentId") != null ? Long.valueOf(body.get("parentId").toString()) : null;
        Long replyToUserId = body.get("replyToUserId") != null ? Long.valueOf(body.get("replyToUserId").toString()) : null;
        return R.ok(commentService.addComment(userId, articleId, content, parentId, replyToUserId));
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteComment(@RequestHeader(CommonConstant.USER_ID_HEADER) Long userId,
                                 @PathVariable Long id) {
        commentService.deleteComment(userId, id);
        return R.ok();
    }
}
