package com.food.article.dubbo;

import com.food.article.dto.ArticleDTO;

public interface ArticleDubboService {

    ArticleDTO getArticleById(Long articleId);
}
