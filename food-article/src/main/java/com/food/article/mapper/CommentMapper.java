package com.food.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.article.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
