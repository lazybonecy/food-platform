package com.food.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class RerankService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${food.rerank.provider:local}")
    private String provider;

    @Value("${food.rerank.local.base-url:http://localhost:8100}")
    private String localBaseUrl;

    @Value("${food.rerank.local.model:BAAI/bge-reranker-v2-m3}")
    private String localModel;

    @Value("${food.rerank.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${food.rerank.dashscope.model:qwen3-vl-rerank}")
    private String dashscopeModel;

    @Value("${food.rerank.enabled:true}")
    private boolean enabled;

    /**
     * 用 cross-encoder 模型对 items 按 query 相关性重排序
     * provider=local → 本地 Docker reranker
     * provider=dashscope → 阿里 DashScope API
     */
    public <T> List<T> rerank(String query, List<T> items, Function<T, String> textExtractor) {
        if (!enabled || items == null || items.size() <= 1) {
            return items;
        }

        List<String> documents = new ArrayList<>();
        for (T item : items) {
            documents.add(textExtractor.apply(item));
        }

        try {
            List<Map<String, Object>> results = switch (provider) {
                case "dashscope" -> callDashScope(query, documents);
                default -> callLocal(query, documents);
            };

            if (results == null || results.isEmpty()) {
                log.warn("[Rerank] Empty results from {}, returning original order", provider);
                return items;
            }

            // 按 relevance_score 降序排列
            results.sort((a, b) -> {
                double sa = ((Number) a.get("relevance_score")).doubleValue();
                double sb = ((Number) b.get("relevance_score")).doubleValue();
                return Double.compare(sb, sa);
            });

            List<T> reranked = new ArrayList<>();
            for (Map<String, Object> r : results) {
                int idx = ((Number) r.get("index")).intValue();
                if (idx >= 0 && idx < items.size()) {
                    reranked.add(items.get(idx));
                }
            }

            log.info("[Rerank] provider={}, scores: {}", provider,
                    results.stream().limit(5)
                            .map(r -> String.format("[%d]=%.4f",
                                    ((Number) r.get("index")).intValue(),
                                    ((Number) r.get("relevance_score")).doubleValue()))
                            .toList());

            return reranked;
        } catch (Exception e) {
            log.error("[Rerank] Failed ({}), returning original order: {}", provider, e.getMessage(), e);
            return items;
        }
    }

    /**
     * 本地 Docker reranker（FastAPI + transformers）
     */
    private List<Map<String, Object>> callLocal(String query, List<String> documents) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", localModel);
        body.put("query", query);
        body.put("documents", documents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("[Rerank] Calling local: model={}, docs={}", localModel, documents.size());

        ResponseEntity<Map> resp = restTemplate.exchange(
                localBaseUrl + "/v1/rerank",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> respBody = resp.getBody();
        if (respBody == null || !respBody.containsKey("results")) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) respBody.get("results");
    }

    /**
     * 阿里 DashScope rerank API
     */
    private List<Map<String, Object>> callDashScope(String query, List<String> documents) {
        if (dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
            log.error("[Rerank] DashScope API key not configured");
            return Collections.emptyList();
        }

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", query);
        input.put("documents", documents);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("top_n", documents.size());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", dashscopeModel);
        body.put("input", input);
        body.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + dashscopeApiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.info("[Rerank] Calling DashScope: model={}, docs={}", dashscopeModel, documents.size());

        ResponseEntity<Map> resp = restTemplate.exchange(
                "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> respBody = resp.getBody();
        if (respBody == null) return Collections.emptyList();

        Map<String, Object> output = (Map<String, Object>) respBody.get("output");
        if (output == null || !output.containsKey("results")) return Collections.emptyList();

        return (List<Map<String, Object>>) output.get("results");
    }
}
