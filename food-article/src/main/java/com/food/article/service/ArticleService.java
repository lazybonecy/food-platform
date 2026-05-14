package com.food.article.service;

import com.food.article.dto.ArticleDTO;
import com.food.article.dto.ArticleListDTO;

public interface ArticleService {

    Long publishArticle(Long merchantId, ArticleDTO dto);

    void updateArticle(Long merchantId, Long articleId, ArticleDTO dto);

    void deleteArticle(Long merchantId, Long articleId);

    ArticleListDTO listArticles(int current, int size, String category, String keyword);

    ArticleDTO getArticleDetail(Long articleId, Long userId);

    void like(Long userId, Long articleId);

    void unlike(Long userId, Long articleId);

    void collect(Long userId, Long articleId);

    void uncollect(Long userId, Long articleId);

    ArticleListDTO listHotArticles(int current, int size, String category, String keyword);
}
