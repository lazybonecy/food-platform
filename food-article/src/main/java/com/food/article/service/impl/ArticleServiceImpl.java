package com.food.article.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.food.article.dto.ArticleDTO;
import com.food.article.dto.ArticleListDTO;
import com.food.article.entity.Article;
import com.food.article.entity.UserInteraction;
import com.food.article.mapper.ArticleMapper;
import com.food.article.mapper.UserInteractionMapper;
import com.food.article.service.ArticleService;
import com.food.common.core.exception.BusinessException;
import com.food.common.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final UserInteractionMapper interactionMapper;
    private final RedisUtil redisUtil;
    private final RestTemplate restTemplate;

    @Value("${food.ai-agent.url:http://localhost:8080}")
    private String aiAgentUrl;

    private static final String HOT_ARTICLE_KEY = "article:hot";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Long publishArticle(Long merchantId, ArticleDTO dto) {
        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setMerchantId(merchantId);
        article.setStatus(1);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setCollectCount(0);
        articleMapper.insert(article);

        // 异步同步到 AI 向量库
        notifyAiIndex(article);

        return article.getId();
    }

    @Override
    public void updateArticle(Long merchantId, Long articleId, ArticleDTO dto) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !article.getMerchantId().equals(merchantId)) {
            throw new BusinessException("文章不存在或无权限修改");
        }

        if (dto.getTitle() != null) article.setTitle(dto.getTitle());
        if (dto.getContent() != null) article.setContent(dto.getContent());
        if (dto.getCoverImage() != null) article.setCoverImage(dto.getCoverImage());
        if (dto.getCategory() != null) article.setCategory(dto.getCategory());
        article.setCouponId(dto.getCouponId());

        articleMapper.updateById(article);

        // 异步同步到 AI 向量库
        notifyAiIndex(article);
    }

    @Override
    public void deleteArticle(Long merchantId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !article.getMerchantId().equals(merchantId)) {
            throw new BusinessException("文章不存在或无权限删除");
        }

        articleMapper.deleteById(articleId);
    }

    @Override
    public ArticleListDTO listArticles(int current, int size, String category, String keyword) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1);
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Article::getCategory, category);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Article::getTitle, keyword);
        }
        wrapper.orderByDesc(Article::getCreateTime);

        Page<Article> page = articleMapper.selectPage(new Page<>(current, size), wrapper);

        ArticleListDTO result = new ArticleListDTO();
        result.setRecords(page.getRecords().stream().map(this::toDTO).collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    @Override
    public ArticleDTO getArticleDetail(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }

        // 增加浏览量
        article.setViewCount(article.getViewCount() + 1);
        articleMapper.updateById(article);

        ArticleDTO dto = toDTO(article);

        // 查询用户交互状态
        if (userId != null) {
            dto.setLiked(hasInteraction(userId, articleId, 1));
            dto.setCollected(hasInteraction(userId, articleId, 2));
        }

        return dto;
    }

    @Override
    @Transactional
    public void like(Long userId, Long articleId) {
        if (hasInteraction(userId, articleId, 1)) {
            return;
        }

        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setArticleId(articleId);
        interaction.setType(1);
        interactionMapper.insert(interaction);

        Article article = articleMapper.selectById(articleId);
        article.setLikeCount(article.getLikeCount() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Transactional
    public void unlike(Long userId, Long articleId) {
        LambdaQueryWrapper<UserInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInteraction::getUserId, userId)
                .eq(UserInteraction::getArticleId, articleId)
                .eq(UserInteraction::getType, 1);
        interactionMapper.delete(wrapper);

        Article article = articleMapper.selectById(articleId);
        if (article.getLikeCount() > 0) {
            article.setLikeCount(article.getLikeCount() - 1);
            articleMapper.updateById(article);
        }
    }

    @Override
    @Transactional
    public void collect(Long userId, Long articleId) {
        if (hasInteraction(userId, articleId, 2)) {
            return;
        }

        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(userId);
        interaction.setArticleId(articleId);
        interaction.setType(2);
        interactionMapper.insert(interaction);

        Article article = articleMapper.selectById(articleId);
        article.setCollectCount(article.getCollectCount() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Transactional
    public void uncollect(Long userId, Long articleId) {
        LambdaQueryWrapper<UserInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInteraction::getUserId, userId)
                .eq(UserInteraction::getArticleId, articleId)
                .eq(UserInteraction::getType, 2);
        interactionMapper.delete(wrapper);

        Article article = articleMapper.selectById(articleId);
        if (article.getCollectCount() > 0) {
            article.setCollectCount(article.getCollectCount() - 1);
            articleMapper.updateById(article);
        }
    }

    @Override
    public ArticleListDTO listHotArticles(int current, int size, String category, String keyword) {
        // 从 Redis ZSET 按热度降序获取全部文章 ID（取足够多以便过滤后分页）
        boolean needFilter = (category != null && !category.isEmpty())
                || (keyword != null && !keyword.isEmpty());
        long fetchStart = 0;
        long fetchEnd = needFilter ? 499 : (long) (current - 1) * size + size - 1;
        Set<String> articleIdStrs = redisUtil.zReverseRange(HOT_ARTICLE_KEY, fetchStart, fetchEnd);

        ArticleListDTO result = new ArticleListDTO();
        if (articleIdStrs == null || articleIdStrs.isEmpty()) {
            result.setRecords(Collections.emptyList());
            result.setTotal(0L);
            result.setCurrent((long) current);
            result.setSize((long) size);
            return result;
        }

        List<Long> articleIds = articleIdStrs.stream()
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Article> articles = articleMapper.selectBatchIds(articleIds);

        // 按 ZSET 顺序排列
        java.util.Map<Long, Article> articleMap = articles.stream()
                .collect(Collectors.toMap(Article::getId, a -> a));
        List<ArticleDTO> orderedList = new ArrayList<>();
        for (Long id : articleIds) {
            Article article = articleMap.get(id);
            if (article == null) continue;
            // 按分类过滤
            if (category != null && !category.isEmpty()
                    && !category.equals(article.getCategory())) {
                continue;
            }
            // 按关键词过滤
            if (keyword != null && !keyword.isEmpty()
                    && (article.getTitle() == null || !article.getTitle().contains(keyword))) {
                continue;
            }
            orderedList.add(toDTO(article));
        }

        // 客户端分页
        int totalFiltered = orderedList.size();
        int fromIndex = Math.min((current - 1) * size, totalFiltered);
        int toIndex = Math.min(fromIndex + size, totalFiltered);
        List<ArticleDTO> pageList = fromIndex < toIndex
                ? orderedList.subList(fromIndex, toIndex)
                : Collections.emptyList();

        result.setRecords(pageList);
        result.setTotal((long) totalFiltered);
        result.setCurrent((long) current);
        result.setSize((long) size);
        return result;
    }

    private boolean hasInteraction(Long userId, Long articleId, Integer type) {
        LambdaQueryWrapper<UserInteraction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInteraction::getUserId, userId)
                .eq(UserInteraction::getArticleId, articleId)
                .eq(UserInteraction::getType, type);
        return interactionMapper.selectCount(wrapper) > 0;
    }

    private ArticleDTO toDTO(Article article) {
        ArticleDTO dto = new ArticleDTO();
        BeanUtils.copyProperties(article, dto);
        if (article.getCreateTime() != null) {
            dto.setCreateTime(article.getCreateTime().format(FORMATTER));
        }
        return dto;
    }

    /**
     * 异步通知 AI Agent 索引文章（fire-and-forget）
     */
    private void notifyAiIndex(Article article) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, java.nio.charset.StandardCharsets.UTF_8));
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("id", article.getId());
            body.put("title", article.getTitle());
            body.put("content", article.getContent());
            body.put("category", article.getCategory());
            body.put("likeCount", article.getLikeCount());
            body.put("collectCount", article.getCollectCount());
            body.put("viewCount", article.getViewCount());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForObject(aiAgentUrl + "/api/ai/index-article", entity, String.class);
        } catch (Exception e) {
            log.warn("Failed to notify AI agent for article {}: {}", article.getId(), e.getMessage());
        }
    }
}
