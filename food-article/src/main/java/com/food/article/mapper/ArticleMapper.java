package com.food.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.article.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
