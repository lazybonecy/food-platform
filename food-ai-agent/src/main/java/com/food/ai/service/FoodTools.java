package com.food.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.ai.dto.ToolResult;
import com.food.ai.entity.Article;
import com.food.ai.mapper.ArticleMapper;
import com.food.ai.mapper.CommentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FoodTools {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private RerankService rerankService;

    @Autowired
    private BM25Service bm25Service;

    @Autowired
    private QueryExpandService queryExpandService;

    @Value("${food.search.expand-enabled:false}")
    private boolean expandEnabled;

    @Value("${food.search.rerank-enabled:true}")
    private boolean rerankEnabled;

    @Value("${food.order.base-url:http://localhost:8080}")
    private String orderBaseUrl;

    private static final int RRF_K = 60;

    public ToolResult searchArticles(String query, Long userId) {
        long startTime = System.currentTimeMillis();
        try {
            log.info("[SearchArticles] query='{}', BM25 + HNSW + RRF hybrid search", query);

            // 1. 加载所有已发布文章
            LambdaQueryWrapper<Article> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.eq(Article::getStatus, 1);
            List<Article> allArticles = articleMapper.selectList(allWrapper);
            log.info("[SearchArticles] Total published articles: {}", allArticles.size());

            // 2. BM25 + HNSW 并行执行
            List<String> searchKeywords = expandEnabled ? queryExpandService.expand(query) : List.of(query);
            CompletableFuture<List<Article>> bm25Future = CompletableFuture.supplyAsync(() -> {
                List<Article> result = new ArrayList<>();
                Set<Long> seen = new HashSet<>();
                for (String keyword : searchKeywords.subList(0, Math.min(5, searchKeywords.size()))) {
                    for (Article a : bm25Service.search(keyword, allArticles)) {
                        if (seen.add(a.getId())) result.add(a);
                    }
                }
                return result;
            });
            CompletableFuture<List<Article>> hnswFuture = CompletableFuture.supplyAsync(() -> vectorSearch(query, allArticles));

            List<Article> bm25Ranked = bm25Future.join();
            List<Article> hnswRanked = hnswFuture.join();
            log.info("[SearchArticles] BM25={}, HNSW={}, elapsed={}ms", bm25Ranked.size(), hnswRanked.size(), System.currentTimeMillis() - startTime);

            // 3. RRF 融合
            Map<Long, Double> rrfScores = new HashMap<>();
            Map<Long, Article> articleMap = new HashMap<>();
            for (int i = 0; i < bm25Ranked.size(); i++) {
                Long id = bm25Ranked.get(i).getId();
                rrfScores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
                articleMap.put(id, bm25Ranked.get(i));
            }
            for (int i = 0; i < hnswRanked.size(); i++) {
                Long id = hnswRanked.get(i).getId();
                rrfScores.merge(id, 1.0 / (RRF_K + i + 1), Double::sum);
                articleMap.putIfAbsent(id, hnswRanked.get(i));
            }

            // 按 RRF 分数排序，取 top 7 送 rerank
            List<Article> rrfRanked = rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(7)
                    .map(e -> articleMap.get(e.getKey()))
                    .filter(Objects::nonNull)
                    .toList();

            log.info("[RRF] Fused top {}: {}", rrfRanked.size(), rrfRanked.stream()
                    .map(a -> a.getId() + ":" + a.getTitle()
                            + String.format("(%.4f)", rrfScores.getOrDefault(a.getId(), 0.0)))
                    .toList());

            if (rrfRanked.isEmpty()) {
                return new ToolResult("未找到相关文章");
            }

            // 5. Cross-Encoder 精排（可选）→ top 3
            List<Article> ranked;
            if (rerankEnabled) {
                ranked = rerankService.rerank(query, rrfRanked,
                        a -> (a.getTitle() != null ? a.getTitle() : "")
                           + " " + (a.getCategory() != null ? a.getCategory() : "")
                           + " " + (a.getContent() != null ? a.getContent().substring(0, Math.min(a.getContent().length(), 200)) : ""))
                        .stream().limit(3).toList();
            } else {
                ranked = rrfRanked.stream().limit(3).toList();
            }
            log.info("[SearchArticles] Final top {}: {}, elapsed={}ms", ranked.size(),
                    ranked.stream().map(a -> a.getId() + ":" + a.getTitle()).toList(),
                    System.currentTimeMillis() - startTime);

            return new ToolResult(formatArticles(ranked), buildArticleData(ranked), null);
        } catch (Exception e) {
            log.error("[SearchArticles] Search failed, falling back: {}", e.getMessage(), e);
            return searchArticlesFallback(query);
        }
    }

    /**
     * HNSW 向量召回：Milvus 搜索 → 按 articleId 排序
     */
    private List<Article> vectorSearch(String query, List<Article> allArticles) {
        try {
            SearchRequest searchReq = SearchRequest.builder()
                    .query(query)
                    .topK(20)
                    .similarityThreshold(0.3)
                    .build();
            List<Document> docs = vectorStore.similaritySearch(searchReq);
            log.info("[HNSW] Milvus returned {} documents", docs != null ? docs.size() : 0);
            if (docs != null) {
                for (Document doc : docs) {
                    Object aid = doc.getMetadata().get("articleId");
                    String title = (String) doc.getMetadata().get("title");
                    log.info("[HNSW]   doc: articleId={}, title='{}', score={}", aid, title, doc.getScore());
                }
            }

            if (docs == null || docs.isEmpty()) {
                return Collections.emptyList();
            }

            // 按 Milvus 返回顺序（score 降序）映射为 Article 列表
            Map<Long, Article> allMap = new HashMap<>();
            for (Article a : allArticles) {
                allMap.put(a.getId(), a);
            }

            List<Article> result = new ArrayList<>();
            for (Document doc : docs) {
                Object id = doc.getMetadata().get("articleId");
                if (id == null) continue;
                Long articleId = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString());
                Article article = allMap.get(articleId);
                if (article != null) {
                    result.add(article);
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[HNSW] Vector search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 降级方案（Milvus 不可用时）：BM25 + reranker
     */
    private ToolResult searchArticlesFallback(String query) {
        log.info("[Fallback] Using BM25-only fallback for query='{}'", query);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1);
        List<Article> allArticles = articleMapper.selectList(wrapper);

        List<Article> bm25Ranked = bm25Service.search(query, allArticles);
        if (bm25Ranked.isEmpty()) {
            return new ToolResult("未找到相关文章");
        }

        List<Article> ranked = rerankService.rerank(query, bm25Ranked,
                a -> (a.getTitle() != null ? a.getTitle() : "")
                   + " " + (a.getCategory() != null ? a.getCategory() : "")
                   + " " + (a.getContent() != null ? a.getContent().substring(0, Math.min(a.getContent().length(), 200)) : ""))
                .stream().limit(3).toList();

        log.info("[Fallback] Returning {} articles: {}",
                ranked.size(), ranked.stream().map(a -> a.getId() + ":" + a.getTitle()).toList());
        return new ToolResult(formatArticles(ranked), buildArticleData(ranked), null);
    }

    public ToolResult getTopLikedArticles(int limit) {
        log.info("[TopLiked] Querying top {} liked articles via SQL", Math.min(limit, 10));
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
               .orderByDesc(Article::getLikeCount)
               .last("LIMIT " + Math.min(limit, 10));

        List<Article> articles = articleMapper.selectList(wrapper);
        if (articles.isEmpty()) {
            return new ToolResult("暂无文章数据");
        }
        log.info("[TopLiked] Found {} articles: {}", articles.size(),
                articles.stream().map(a -> a.getId() + ":" + a.getTitle() + "(" + a.getLikeCount() + "赞)").toList());
        return new ToolResult(formatRanking(articles, "点赞"), buildArticleData(articles), null);
    }

    public ToolResult getTopCollectedArticles(int limit) {
        log.info("[TopCollected] Querying top {} collected articles via SQL", Math.min(limit, 10));
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Article::getStatus, 1)
               .orderByDesc(Article::getCollectCount)
               .last("LIMIT " + Math.min(limit, 10));

        List<Article> articles = articleMapper.selectList(wrapper);
        if (articles.isEmpty()) {
            return new ToolResult("暂无文章数据");
        }
        log.info("[TopCollected] Found {} articles: {}", articles.size(),
                articles.stream().map(a -> a.getId() + ":" + a.getTitle() + "(" + a.getCollectCount() + "收藏)").toList());
        return new ToolResult(formatRanking(articles, "收藏"), buildArticleData(articles), null);
    }

    public ToolResult getMyCoupons(Long userId) {
        if (userId == null) {
            return new ToolResult("请先登录后查询优惠券");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    orderBaseUrl + "/api/coupon/my?size=20",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = resp.getBody();
            if (body == null || !body.containsKey("data")) {
                return new ToolResult("暂无优惠券数据");
            }

            Object data = body.get("data");
            if (data instanceof List<?> list && !list.isEmpty()) {
                List<Map<String, Object>> coupons = new ArrayList<>();
                StringBuilder sb = new StringBuilder("用户可用优惠券：\n");
                for (Object item : list) {
                    if (item instanceof Map<?, ?> coupon) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("id", coupon.get("id"));
                        c.put("name", coupon.get("name"));
                        c.put("minAmount", coupon.get("minAmount"));
                        c.put("discountAmount", coupon.get("discountAmount"));
                        c.put("endTime", coupon.get("endTime"));
                        coupons.add(c);

                        sb.append("- ").append(coupon.get("name"));
                        sb.append("（满").append(coupon.get("minAmount")).append("减").append(coupon.get("discountAmount")).append("）");
                        sb.append("，有效期至 ").append(coupon.get("endTime"));
                        sb.append("\n");
                    }
                }
                return new ToolResult(sb.toString(), null, coupons);
            }
            return new ToolResult("暂无可用优惠券");
        } catch (Exception e) {
            log.warn("Failed to fetch coupons for user {}: {}", userId, e.getMessage());
            return new ToolResult("查询优惠券失败，请稍后重试");
        }
    }

    public ToolResult getCouponExpiry(Long userId) {
        if (userId == null) {
            return new ToolResult("请先登录后查询优惠券");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    orderBaseUrl + "/api/coupon/my?size=20",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = resp.getBody();
            if (body == null || !body.containsKey("data")) {
                return new ToolResult("暂无优惠券数据");
            }

            Object data = body.get("data");
            if (data instanceof List<?> list && !list.isEmpty()) {
                long now = System.currentTimeMillis();
                List<Map<String, Object>> coupons = new ArrayList<>();
                StringBuilder sb = new StringBuilder("优惠券过期信息：\n");
                for (Object item : list) {
                    if (item instanceof Map<?, ?> coupon) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("id", coupon.get("id"));
                        c.put("name", coupon.get("name"));
                        c.put("minAmount", coupon.get("minAmount"));
                        c.put("discountAmount", coupon.get("discountAmount"));
                        c.put("endTime", coupon.get("endTime"));

                        Object endTimeObj = coupon.get("endTime");
                        if (endTimeObj != null) {
                            long endTime = parseTimestamp(endTimeObj);
                            long daysLeft = (endTime - now) / (1000 * 60 * 60 * 24);
                            c.put("daysLeft", daysLeft);

                            sb.append("- ").append(coupon.get("name"));
                            if (daysLeft < 0) {
                                sb.append("（已过期）");
                            } else if (daysLeft == 0) {
                                sb.append("（今天到期）");
                            } else {
                                sb.append("（还剩 ").append(daysLeft).append(" 天）");
                            }
                            sb.append("\n");
                        }
                        coupons.add(c);
                    }
                }
                return new ToolResult(sb.toString(), null, coupons);
            }
            return new ToolResult("暂无可用优惠券");
        } catch (Exception e) {
            log.warn("Failed to fetch coupon expiry for user {}: {}", userId, e.getMessage());
            return new ToolResult("查询优惠券失败，请稍后重试");
        }
    }

    private long parseTimestamp(Object obj) {
        if (obj instanceof Number n) {
            return n.longValue();
        }
        if (obj instanceof String s) {
            try {
                return java.time.LocalDateTime.parse(s.replace(" ", "T"))
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private List<Map<String, Object>> buildArticleData(List<Article> articles) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Article a : articles) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("title", a.getTitle());
            item.put("category", a.getCategory());
            item.put("summary", a.getContent() != null
                    ? a.getContent().substring(0, Math.min(a.getContent().length(), 50))
                    : "");
            item.put("likeCount", a.getLikeCount());
            item.put("collectCount", a.getCollectCount());
            item.put("viewCount", a.getViewCount());
            list.add(item);
        }
        return list;
    }

    private String formatArticles(List<Article> articles) {
        StringBuilder sb = new StringBuilder("搜索结果：\n");
        for (Article a : articles) {
            sb.append("[文章ID:").append(a.getId()).append("]");
            sb.append(a.getTitle());
            if (a.getCategory() != null) {
                sb.append("（").append(a.getCategory()).append("）");
            }
            sb.append(" | 点赞:").append(a.getLikeCount());
            sb.append(" | 收藏:").append(a.getCollectCount());
            sb.append("\n");
        }
        return sb.toString();
    }

    private String formatRanking(List<Article> articles, String metric) {
        StringBuilder sb = new StringBuilder(metric + "排行：\n");
        int rank = 1;
        for (Article a : articles) {
            sb.append(rank++).append(". [文章ID:").append(a.getId()).append("]");
            sb.append(a.getTitle());
            if ("点赞".equals(metric)) {
                sb.append("（").append(a.getLikeCount()).append(" 赞）");
            } else {
                sb.append("（").append(a.getCollectCount()).append(" 收藏）");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
