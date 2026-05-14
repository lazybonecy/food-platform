package com.food.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class QueryExpandService {

    @Autowired
    private OpenAiChatModel chatModel;

    private static final String EXPAND_PROMPT = """
            你是美食搜索助手。用户输入了一个食物相关的查询，请扩展出相关的食物关键词，用于搜索美食文章。

            规则：
            - 输出关键词列表，用逗号分隔
            - 包含原词、同义词、相关菜品名、食材名
            - 不要输出解释，只输出关键词
            - 最多输出10个关键词

            示例：
            输入：想吃鸡肉
            输出：鸡肉,鸡丁,鸡翅,鸡腿,白切鸡,宫保鸡丁,辣子鸡,口水鸡,黄焖鸡,烤鸡

            输入：推荐辣的菜
            输出：辣,辣椒,麻辣,川菜,湘菜,辣子鸡,水煮鱼,麻婆豆腐,回锅肉,剁椒鱼头

            输入：%s
            """;

    /**
     * 用 LLM 扩展查询词，返回扩展后的关键词列表
     */
    public List<String> expand(String query) {
        try {
            String prompt = String.format(EXPAND_PROMPT, query);
            String result = chatModel.call(new Prompt(new UserMessage(prompt)))
                    .getResult().getOutput().getText();

            if (result == null || result.isBlank()) {
                return List.of(query);
            }

            List<String> keywords = new ArrayList<>();
            keywords.add(query); // 原词始终保留
            for (String kw : result.trim().split("[,，\\s]+")) {
                kw = kw.trim();
                if (!kw.isEmpty() && !kw.equals(query)) {
                    keywords.add(kw);
                }
            }
            log.info("[QueryExpand] '{}' → {}", query, keywords);
            return keywords;
        } catch (Exception e) {
            log.warn("[QueryExpand] Failed, using original query: {}", e.getMessage());
            return List.of(query);
        }
    }
}
