package com.food.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.article.dto.CommentDTO;
import com.food.article.entity.Article;
import com.food.article.entity.Comment;
import com.food.article.mapper.ArticleMapper;
import com.food.article.mapper.CommentMapper;
import com.food.article.service.CommentService;
import com.food.article.service.NotificationService;
import com.food.api.user.UserDubboService;
import com.food.api.user.dto.UserDTO;
import com.food.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final NotificationService notificationService;

    @DubboReference
    private UserDubboService userDubboService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public CommentDTO addComment(Long userId, Long articleId, String content, Long parentId, Long replyToUserId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }

        if (content.length() > 500) {
            throw new BusinessException("评论内容不能超过500字");
        }

        // 如果是回复，校验父评论存在且属于同一篇文章
        if (parentId != null) {
            Comment parent = commentMapper.selectById(parentId);
            if (parent == null || !parent.getArticleId().equals(articleId)) {
                throw new BusinessException("父评论不存在");
            }
        }

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setContent(content.trim());
        comment.setParentId(parentId);
        commentMapper.insert(comment);

        // 发送通知
        try {
            if (replyToUserId != null) {
                // 回复某人 → 通知被回复者
                notificationService.createNotification(replyToUserId, userId, 2,
                        articleId, comment.getId(), content.trim());
            } else if (parentId == null) {
                // 顶级评论 → 通知文章作者
                notificationService.createNotification(article.getMerchantId(), userId, 1,
                        articleId, comment.getId(), content.trim());
            } else {
                // 回复顶级评论 → 通知顶级评论者
                Comment parent = commentMapper.selectById(parentId);
                if (parent != null && !parent.getUserId().equals(userId)) {
                    notificationService.createNotification(parent.getUserId(), userId, 2,
                            articleId, comment.getId(), content.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to create notification: {}", e.getMessage());
        }

        // 获取真实昵称填充返回值
        String nickname = getNickname(userId);
        CommentDTO dto = toDTO(comment, nickname);
        dto.setReplies(new ArrayList<>());
        return dto;
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此评论");
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    public List<CommentDTO> listComments(Long articleId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getCreateTime);
        List<Comment> allComments = commentMapper.selectList(wrapper);

        // 批量获取所有评论者的昵称
        Set<Long> userIds = allComments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = batchGetNicknames(userIds);

        // 按 parentId 分组构建树形结构
        Map<Long, List<CommentDTO>> replyMap = new LinkedHashMap<>();
        List<CommentDTO> topComments = new ArrayList<>();

        for (Comment c : allComments) {
            String nickname = nicknameMap.getOrDefault(c.getUserId(), "用户");
            CommentDTO dto = toDTO(c, nickname);
            if (c.getParentId() == null) {
                topComments.add(dto);
            } else {
                replyMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(dto);
            }
        }

        // 设置回复列表
        for (CommentDTO dto : topComments) {
            dto.setReplies(replyMap.getOrDefault(dto.getId(), new ArrayList<>()));
        }

        return topComments;
    }

    private CommentDTO toDTO(Comment comment, String nickname) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setArticleId(comment.getArticleId());
        dto.setUserId(comment.getUserId());
        dto.setNickname(nickname != null ? nickname : "用户");
        dto.setContent(comment.getContent());
        dto.setParentId(comment.getParentId());
        if (comment.getCreateTime() != null) {
            dto.setCreateTime(comment.getCreateTime().format(FORMATTER));
        }
        dto.setReplies(new ArrayList<>());
        return dto;
    }

    private String getNickname(Long userId) {
        try {
            UserDTO user = userDubboService.getUserById(userId);
            if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
                return user.getNickname();
            }
        } catch (Exception e) {
            log.warn("Failed to get nickname for userId={}: {}", userId, e.getMessage());
        }
        return "用户";
    }

    private Map<Long, String> batchGetNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        for (Long uid : userIds) {
            map.put(uid, getNickname(uid));
        }
        return map;
    }
}
