package com.food.ai.controller;

import com.food.common.core.result.R;
import com.food.ai.dto.AgentResponse;
import com.food.ai.service.ArticleIndexService;
import com.food.ai.service.FoodAgentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final FoodAgentService foodAgentService;
    private final ArticleIndexService articleIndexService;

    @PostMapping("/chat")
    public R<AgentResponse> chat(@RequestBody Map<String, Object> body,
                                 HttpServletRequest request) {
        String message = (String) body.get("message");
        Long userId = resolveUserId(request, body);
        return R.ok(foodAgentService.chat(message, userId));
    }

    @PostMapping("/clear-history")
    public R<Void> clearHistory(@RequestBody Map<String, Object> body,
                                HttpServletRequest request) {
        Long userId = resolveUserId(request, body);
        if (userId == null) {
            return R.fail("用户未登录");
        }
        foodAgentService.clearHistory(userId);
        return R.ok();
    }

    @PostMapping("/reindex")
    public R<Map<String, Object>> reindex() {
        int count = articleIndexService.reindexAll();
        return R.ok(Map.of("indexed", count));
    }

    @PostMapping("/index-article")
    public R<Void> indexArticle(@RequestBody Map<String, Object> body) {
        Long articleId = Long.valueOf(body.get("id").toString());
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String category = (String) body.get("category");
        Integer likeCount = body.get("likeCount") != null ? Integer.valueOf(body.get("likeCount").toString()) : 0;
        Integer collectCount = body.get("collectCount") != null ? Integer.valueOf(body.get("collectCount").toString()) : 0;
        Integer viewCount = body.get("viewCount") != null ? Integer.valueOf(body.get("viewCount").toString()) : 0;

        com.food.ai.entity.Article article = new com.food.ai.entity.Article();
        article.setId(articleId);
        article.setTitle(title);
        article.setContent(content);
        article.setCategory(category);
        article.setLikeCount(likeCount);
        article.setCollectCount(collectCount);
        article.setViewCount(viewCount);
        article.setStatus(1);

        articleIndexService.indexArticle(article);
        return R.ok();
    }

    private Long resolveUserId(HttpServletRequest request, Map<String, Object> body) {
        // 优先从 gateway 注入的 header 获取
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.isEmpty()) {
            try {
                return Long.valueOf(headerUserId);
            } catch (NumberFormatException ignored) {}
        }
        // 回退到 body
        if (body.get("userId") != null) {
            try {
                return Long.valueOf(body.get("userId").toString());
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}
