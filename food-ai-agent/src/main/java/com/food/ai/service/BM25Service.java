package com.food.ai.service;

import com.food.ai.entity.Article;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BM25Service {

    private static final double K1 = 1.2;
    private static final double B = 0.75;

    private static final Pattern PUNCTUATION = Pattern.compile("[\\s\\p{Punct}\\u3000-\\u303F\\uFF00-\\uFFEF\\u2018-\\u201F\\u2026\\u2014]+");

    /**
     * BM25 搜索：对 allArticles 按 query 相关性评分并排序
     */
    public List<Article> search(String query, List<Article> allArticles) {
        if (allArticles == null || allArticles.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. query 分词
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("[BM25] query='{}', terms={}", query, queryTerms);

        // 2. 构建倒排索引：term → [articleIndex]
        int N = allArticles.size();
        Map<String, Set<Integer>> invertedIndex = new HashMap<>();
        List<List<String>> docTokens = new ArrayList<>();
        int totalDocLen = 0;

        for (int i = 0; i < N; i++) {
            Article a = allArticles.get(i);
            String text = buildDocText(a);
            List<String> tokens = tokenize(text);
            docTokens.add(tokens);
            totalDocLen += tokens.size();

            Set<String> uniqueTerms = new HashSet<>(tokens);
            for (String term : uniqueTerms) {
                invertedIndex.computeIfAbsent(term, k -> new HashSet<>()).add(i);
            }
        }
        double avgDl = (double) totalDocLen / N;

        // 3. 计算每篇文章的 BM25 分数
        double[] scores = new double[N];
        for (String term : queryTerms) {
            Set<Integer> docsWithTerm = invertedIndex.getOrDefault(term, Collections.emptySet());
            int df = docsWithTerm.size();
            if (df == 0) continue;

            // IDF = log((N - df + 0.5) / (df + 0.5) + 1)
            double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1);

            for (int docIdx : docsWithTerm) {
                int dl = docTokens.get(docIdx).size();
                // TF: 词在文档中出现的次数
                int tf = 0;
                for (String token : docTokens.get(docIdx)) {
                    if (token.equals(term)) tf++;
                }
                // BM25 公式
                double score = idf * (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * dl / avgDl));
                scores[docIdx] += score;
            }
        }

        // 4. 按分数降序排列
        List<Article> ranked = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            if (scores[i] > 0) {
                ranked.add(allArticles.get(i));
            }
        }
        ranked.sort((a, b) -> {
            int idxA = allArticles.indexOf(a);
            int idxB = allArticles.indexOf(b);
            return Double.compare(scores[idxB], scores[idxA]);
        });

        log.info("[BM25] top 5: {}", ranked.stream().limit(5)
                .map(a -> {
                    int idx = allArticles.indexOf(a);
                    return a.getId() + ":" + a.getTitle() + String.format("(%.3f)", scores[idx]);
                })
                .toList());

        return ranked;
    }

    /**
     * 中文分词：标点拆分 + unigram + bigram
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        List<String> terms = new ArrayList<>();
        // 先按标点/空格拆分
        String[] parts = PUNCTUATION.split(text.trim());
        for (String part : parts) {
            if (part.isEmpty()) continue;
            // 去掉非中文非字母数字的字符
            part = part.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
            if (part.isEmpty()) continue;

            // unigram：每个中文字符作为一个 term（保证单字查询也能匹配）
            for (char c : part.toCharArray()) {
                if (c >= '一' && c <= '龥') {
                    terms.add(String.valueOf(c));
                }
            }
            // bigram：2-gram 滑动窗口（捕捉词组语义）
            if (part.length() >= 2) {
                for (int i = 0; i < part.length() - 1; i++) {
                    String gram = part.substring(i, i + 2);
                    if (gram.matches(".*[\\u4e00-\\u9fa5].*")) {
                        terms.add(gram);
                    }
                }
            }
        }
        return terms;
    }

    /**
     * 构建文档文本：标题 + 分类 + 内容(前500字)
     */
    private String buildDocText(Article a) {
        StringBuilder sb = new StringBuilder();
        if (a.getTitle() != null) sb.append(a.getTitle());
        if (a.getCategory() != null) sb.append(" ").append(a.getCategory());
        if (a.getContent() != null) {
            String content = a.getContent();
            sb.append(" ").append(content.length() > 500 ? content.substring(0, 500) : content);
        }
        return sb.toString();
    }
}
