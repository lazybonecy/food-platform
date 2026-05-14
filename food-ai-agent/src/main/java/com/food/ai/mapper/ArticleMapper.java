package com.food.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.ai.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
