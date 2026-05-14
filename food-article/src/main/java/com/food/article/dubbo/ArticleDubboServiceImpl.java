package com.food.article.dubbo;

import com.food.article.dto.ArticleDTO;
import com.food.article.entity.Article;
import com.food.article.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

@DubboService
@RequiredArgsConstructor
public class ArticleDubboServiceImpl implements ArticleDubboService {

    private final ArticleMapper articleMapper;

    @Override
    public ArticleDTO getArticleById(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return null;
        }
        ArticleDTO dto = new ArticleDTO();
        BeanUtils.copyProperties(article, dto);
        return dto;
    }
}
