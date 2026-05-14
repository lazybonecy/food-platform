package com.food.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.article.entity.UserInteraction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInteractionMapper extends BaseMapper<UserInteraction> {
}
