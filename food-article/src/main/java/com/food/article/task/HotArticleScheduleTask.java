package com.food.article.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.article.entity.Article;
import com.food.article.mapper.ArticleMapper;
import com.food.common.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotArticleScheduleTask {

    private static final String HOT_ARTICLE_KEY = "article:hot";
    private static final int TOP_N = 50;
    private static final double VIEW_WEIGHT = 1.0;
    private static final double LIKE_WEIGHT = 3.0;
    private static final double COLLECT_WEIGHT = 5.0;

    private final ArticleMapper articleMapper;
    private final RedisUtil redisUtil;

    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void refreshHotArticles() {
        log.info("Start refreshing hot article cache...");

        // 查询所有已发布的文章
        List<Article> articles = articleMapper.selectList(
                new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1));

        // 清空旧缓存
        redisUtil.delete(HOT_ARTICLE_KEY);

        // 计算热度分值并写入 ZSET
        for (Article article : articles) {
            double score = calculateScore(article);
            redisUtil.zAdd(HOT_ARTICLE_KEY, String.valueOf(article.getId()), score);
        }

        // 只保留 Top N：获取所有成员，移除超出部分
        Long totalSize = redisUtil.zCard(HOT_ARTICLE_KEY);
        if (totalSize != null && totalSize > TOP_N) {
            // reverseRange 返回降序排列，取 start=TOP_N 到 end=-1 就是需要移除的尾部
            Set<String> toRemove = redisUtil.zReverseRange(HOT_ARTICLE_KEY, TOP_N, -1);
            if (toRemove != null && !toRemove.isEmpty()) {
                redisUtil.zRemove(HOT_ARTICLE_KEY, toRemove.toArray());
            }
        }

        log.info("Hot article cache refreshed: {} articles, top {} kept", articles.size(), TOP_N);
    }

    private double calculateScore(Article article) {
        int views = article.getViewCount() != null ? article.getViewCount() : 0;
        int likes = article.getLikeCount() != null ? article.getLikeCount() : 0;
        int collects = article.getCollectCount() != null ? article.getCollectCount() : 0;
        return views * VIEW_WEIGHT + likes * LIKE_WEIGHT + collects * COLLECT_WEIGHT;
    }
}
