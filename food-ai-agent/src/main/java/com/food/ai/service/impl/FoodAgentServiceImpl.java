package com.food.ai.service.impl;

import com.food.ai.dto.AgentResponse;
import com.food.ai.dto.ToolResult;
import com.food.ai.service.ConversationMemory;
import com.food.ai.service.FoodAgentService;
import com.food.ai.service.FoodTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class FoodAgentServiceImpl implements FoodAgentService {

    private static final int MAX_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
            你是「美食小助手」，校园美食平台的智能助手。

            ## 工具
            - search_articles(query): 搜索美食文章
            - get_top_liked(limit): 点赞排行
            - get_top_collected(limit): 收藏排行
            - get_my_coupons(): 我的优惠券
            - get_coupon_expiry(): 优惠券过期信息

            ## 调用格式（必须先调工具再回复）
            Thought: 分析意图
            Action: tool_name(arguments)

            ## 回复格式（必须严格遵守）
            Thought: 总结
            FINAL_ANSWER: {type}
            {引导语}
            [文章ID:xxx]推荐理由

            type: chat/recommend/ranking/coupon

            ## 规则
            - 每篇文章必须写一行 [文章ID:xxx]推荐理由
            - 不要在文字中列出菜品名称
            - 不要编造数据，必须通过工具获取
            """;

    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(\\w+)\\((.*?)\\)");
    private static final Pattern ANSWER_PATTERN = Pattern.compile("FINAL_ANSWER:\\s*(.*)", Pattern.DOTALL);
    private static final Pattern REASON_PATTERN = Pattern.compile("\\[文章ID:(\\d+)](.+)");
    private static final Pattern SEARCH_INTENT = Pattern.compile(
            "(想吃|想喝|推荐|来点|来一份|来个|来碗|要吃|要喝|搜|找|有没有|介绍).{1,10}");
    // 泛化查询模式：没有具体食物关键词，只是"推荐美食"/"有什么好吃的"
    private static final Pattern GENERIC_SEARCH = Pattern.compile(
            "^(有没有|推荐|介绍|来点|来一份|来个|给我).{0,4}(美食|好吃的|菜|食物|吃的|喝的|推荐|特色的).{0,4}[？?！!。.]*$");
    private static final Pattern RANKING_INTENT = Pattern.compile(
            "(排行|排名|热门|最火|最多|最受欢迎|点赞最多|收藏最多).{0,10}");
    private static final Pattern COUPON_INTENT = Pattern.compile("(优惠券|券|红包|折扣|过期)");

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private ConversationMemory memory;

    @Autowired
    private FoodTools foodTools;

    @Override
    public AgentResponse chat(String message, Long userId) {
        // 快速路径：命中搜索/排行/优惠券意图 → 跳过 LLM #1，直接调工具
        String intent = detectIntent(message);
        if (intent != null) {
            return fastPath(intent, message, userId);
        }

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM_PROMPT));
        messages.addAll(memory.load(userId));
        messages.add(new UserMessage(message));

        // ReAct 循环，追踪工具调用上下文
        List<Map<String, Object>> lastToolArticles = null;
        List<Map<String, Object>> lastToolCoupons = null;
        String lastToolName = null;
        String finalAnswer = null;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            String output = callLLM(messages);
            log.info("[ReAct] === iteration {} ===\n{}", i + 1, output);

            // 优先检查 Action（防止 LLM 同时输出 Action 和 FINAL_ANSWER 时跳过工具调用）
            Matcher actionMatcher = ACTION_PATTERN.matcher(output);
            if (actionMatcher.find()) {
                log.info("[ReAct] Found Action: {}", actionMatcher.group(0));
                String toolName = actionMatcher.group(1);
                String args = actionMatcher.group(2).trim();
                ToolResult toolResult = executeTool(toolName, args, userId);
                log.info("[ReAct] Tool {}({}) called, result: {}", toolName, args, toolResult.getText());

                lastToolName = toolName;
                if (toolResult.getArticles() != null) {
                    lastToolArticles = toolResult.getArticles();
                }
                if (toolResult.getCoupons() != null) {
                    lastToolCoupons = toolResult.getCoupons();
                }

                messages.add(new AssistantMessage(output));
                messages.add(new UserMessage("Observation: " + toolResult.getText()));
                continue;
            }

            Matcher answerMatcher = ANSWER_PATTERN.matcher(output);
            if (answerMatcher.find()) {
                finalAnswer = answerMatcher.group(1).trim();
                log.info("[ReAct] Found FINAL_ANSWER, ending loop");
                break;
            }

            log.info("[ReAct] No Action/FINAL_ANSWER found, using raw output as final answer");
            finalAnswer = output;
            break;
        }

        if (finalAnswer == null) {
            finalAnswer = "抱歉，处理超时，请重试。";
        }

        // 解析 type：先看 AI 输出，再从工具调用推断
        String type = "chat";
        String reply = finalAnswer;

        String[] lines = finalAnswer.split("\\n", 2);
        String firstLine = lines[0].trim().toLowerCase();
        if (firstLine.equals("chat") || firstLine.equals("recommend")
                || firstLine.equals("ranking") || firstLine.equals("coupon")) {
            type = firstLine;
            reply = lines.length > 1 ? lines[1].trim() : "";
        } else if (lastToolName != null) {
            // AI 没输出 type，从工具调用推断
            type = switch (lastToolName) {
                case "search_articles" -> "recommend";
                case "get_top_liked", "get_top_collected" -> "ranking";
                case "get_my_coupons", "get_coupon_expiry" -> "coupon";
                default -> "chat";
            };
        }

        // 清理 Thought: 行
        reply = reply.replaceAll("(?m)^Thought:.*$", "").trim();

        // 提取推荐理由并合并到文章数据（在清理标记之前）
        List<Map<String, Object>> articles = lastToolArticles;
        if (articles != null && !articles.isEmpty()) {
            Map<Long, String> reasons = new HashMap<>();
            Matcher reasonMatcher = REASON_PATTERN.matcher(reply);
            while (reasonMatcher.find()) {
                long id = Long.parseLong(reasonMatcher.group(1));
                reasons.put(id, reasonMatcher.group(2).trim());
            }
            for (Map<String, Object> article : articles) {
                Long id = ((Number) article.get("id")).longValue();
                if (reasons.containsKey(id)) {
                    article.put("reason", reasons.get(id));
                } else if (!article.containsKey("reason")) {
                    String category = (String) article.get("category");
                    article.put("reason", category != null ? "推荐" + category + "美食" : "值得一试");
                }
            }
            // 提取完理由后再清理标记
            reply = reply.replaceAll("(?m)^\\[文章ID:\\d+].*$", "").trim();
        }

        log.info("[Response] type={}, articles={}, coupons={}", type,
                articles != null ? articles.size() + " items" : "null",
                lastToolCoupons != null ? lastToolCoupons.size() + " items" : "null");

        // 保存记忆（长期记忆异步执行，不阻塞响应）
        memory.save(userId, message, reply);
        final String finalReply = reply;
        final String finalMessage = message;
        CompletableFuture.runAsync(() -> extractLongTermMemory(userId, finalMessage, finalReply));

        return new AgentResponse(type, reply, articles, lastToolCoupons);
    }

    @Override
    public void clearHistory(Long userId) {
        memory.clear(userId);
    }

    private String callLLM(List<Message> messages) {
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .temperature(0.3)
                    .build();
            Prompt prompt = new Prompt(messages, options);
            return chatModel.call(prompt).getResult().getOutput().getText();
        } catch (Exception e) {
            log.error("LLM call failed: {}", e.getMessage(), e);
            return "FINAL_ANSWER: chat\n抱歉，AI服务暂时不可用，请稍后重试。";
        }
    }

    private ToolResult executeTool(String toolName, String args, Long userId) {
        try {
            return switch (toolName) {
                case "search_articles" -> foodTools.searchArticles(stripQuotes(args), userId);
                case "get_top_liked" -> foodTools.getTopLikedArticles(parseLimit(args));
                case "get_top_collected" -> foodTools.getTopCollectedArticles(parseLimit(args));
                case "get_my_coupons" -> foodTools.getMyCoupons(userId);
                case "get_coupon_expiry" -> foodTools.getCouponExpiry(userId);
                default -> new ToolResult("未知工具: " + toolName);
            };
        } catch (Exception e) {
            log.warn("Tool {} execution failed: {}", toolName, e.getMessage());
            return new ToolResult("工具执行失败: " + e.getMessage());
        }
    }

    private int parseLimit(String args) {
        try {
            return Integer.parseInt(stripQuotes(args));
        } catch (Exception e) {
            return 5;
        }
    }

    private String stripQuotes(String s) {
        s = s.trim();
        // 处理 query="鸡肉" 或 query='鸡肉' 格式，提取值部分
        if (s.matches("\\w+\\s*=\\s*\".*\"")) {
            s = s.substring(s.indexOf("\"") + 1, s.lastIndexOf("\""));
        } else if (s.matches("\\w+\\s*=\\s*'.*'")) {
            s = s.substring(s.indexOf("'") + 1, s.lastIndexOf("'"));
        }
        // 处理纯引号包裹
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        if (s.startsWith("'") && s.endsWith("'")) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String detectIntent(String msg) {
        if (SEARCH_INTENT.matcher(msg).find()) return "search";
        if (RANKING_INTENT.matcher(msg).find()) return "ranking";
        if (COUPON_INTENT.matcher(msg).find()) return "coupon";
        return null;
    }

    /**
     * 判断是否为泛化查询（没有具体食物关键词）
     */
    private boolean isGenericQuery(String extractedQuery, String originalMsg) {
        // 提取后的 query 太短或匹配泛化模式
        if (extractedQuery.length() <= 2) return true;
        if (GENERIC_SEARCH.matcher(originalMsg).matches()) return true;
        // query 只包含泛化词
        return extractedQuery.matches("^(美食|好吃的|菜|食物|吃的|喝的|推荐|特色|什么|啥).{0,2}$");
    }

    /**
     * 泛化查询：读取用户偏好 → LLM 生成个性化搜索词
     */
    private String expandWithPreference(String query, Long userId) {
        String preference = memory.getLongTerm(userId);
        if (preference == null || preference.isEmpty() || preference.equals("无")) {
            log.info("[FastPath] No user preference found, using original query");
            return query;
        }

        try {
            String prompt = """
                    你是搜索词生成助手。根据用户偏好，生成一个具体的美食搜索词。

                    ## 规则
                    1. 只输出一个搜索词（2-6个字），不要解释
                    2. 搜索词要具体（如"川菜"、"鸡肉"、"甜品"），不要太泛（如"美食"）
                    3. 优先匹配用户偏好的口味/菜系/食材
                    4. 如果偏好中有多个方向，选最偏好的一个

                    ## 用户偏好
                    %s

                    ## 用户原始查询
                    %s

                    ## 输出搜索词（仅输出搜索词，不要其他内容）
                    """.formatted(preference, query);

            String expanded = chatModel.call(new Prompt(List.of(new UserMessage(prompt))))
                    .getResult().getOutput().getText().trim();

            // 清理：去掉引号、句号等
            expanded = expanded.replaceAll("[\"'。.，,！!？?]", "").trim();

            if (!expanded.isEmpty() && expanded.length() <= 20) {
                log.info("[PreferenceSearch] preference='{}' → query='{}'", preference, expanded);
                return expanded;
            }
        } catch (Exception e) {
            log.warn("[PreferenceSearch] Failed to expand query: {}", e.getMessage());
        }

        return query;
    }

    private AgentResponse fastPath(String intent, String message, Long userId) {
        long t0 = System.currentTimeMillis();
        log.info("[FastPath] intent={}, msg='{}'", intent, message);

        // 1. 直接调工具（跳过 LLM #1）
        ToolResult toolResult;
        String type;
        switch (intent) {
            case "search" -> {
                String query = message.replaceAll(
                        "^(想吃|想喝|推荐|来点|来一份|来个|来碗|要吃|要喝|搜一下|搜|找一下|找|有没有|介绍|给我|帮忙).{0,2}", "")
                        .replaceAll("(的文章|的菜|的做法|推荐|排行|有哪些|吗|呢|吧|啊|呀|~|！)$", "").trim();
                if (query.isEmpty()) query = message;

                // 泛化查询：读取用户偏好，让 LLM 生成个性化搜索词
                if (isGenericQuery(query, message)) {
                    query = expandWithPreference(query, userId);
                    log.info("[FastPath] Generic query expanded to '{}'", query);
                }

                toolResult = foodTools.searchArticles(query, userId);
                type = "recommend";
            }
            case "ranking" -> {
                toolResult = foodTools.getTopLikedArticles(5);
                type = "ranking";
            }
            case "coupon" -> {
                toolResult = foodTools.getMyCoupons(userId);
                type = "coupon";
            }
            default -> { return null; }
        }
        log.info("[FastPath] Tool done in {}ms", System.currentTimeMillis() - t0);

        // 2. 模板生成回复（跳过 LLM，从 ~13s 降到 <10ms）
        List<Map<String, Object>> articles = toolResult.getArticles();
        String reply;
        switch (intent) {
            case "search" -> {
                reply = "为你找到以下美食推荐：";
                if (articles != null) {
                    for (Map<String, Object> a : articles) {
                        String summary = (String) a.get("summary");
                        a.put("reason", summary != null && !summary.isEmpty() ? summary : "值得一试");
                    }
                }
            }
            case "ranking" -> {
                reply = "这是当前最受欢迎的文章：";
                if (articles != null) {
                    for (Map<String, Object> a : articles) {
                        a.put("reason", "人气美食，不容错过");
                    }
                }
            }
            case "coupon" -> {
                reply = "这是你的优惠券信息：";
            }
            default -> { reply = toolResult.getText(); }
        }

        log.info("[FastPath] Total {}ms", System.currentTimeMillis() - t0);

        // 4. 异步保存记忆
        memory.save(userId, message, reply);
        final String r = reply, m2 = message;
        CompletableFuture.runAsync(() -> extractLongTermMemory(userId, m2, r));

        return new AgentResponse(type, reply, articles, toolResult.getCoupons());
    }

    private void extractLongTermMemory(Long userId, String userMsg, String aiReply) {
        if (userId == null) return;
        try {
            String existingMemory = memory.getLongTerm(userId);
            String memoryPrompt = """
                    你是用户画像整理助手。根据对话更新用户的食物偏好档案。

                    ## 规则
                    1. 合并新旧信息，去除重复
                    2. 保留所有不重复的事实
                    3. 用逗号分隔，语言简洁
                    4. 总长度不超过200字
                    5. 如果没有新信息，只输出"无新信息"

                    ## 已有档案
                    %s

                    ## 最新对话
                    用户: %s
                    助手: %s

                    ## 输出（直接输出整理后的档案，不要解释）
                    """.formatted(
                    existingMemory != null ? existingMemory : "无",
                    userMsg, aiReply
            );

            List<Message> msgs = List.of(new UserMessage(memoryPrompt));
            String result = chatModel.call(new Prompt(msgs)).getResult().getOutput().getText();

            if (result != null && !result.contains("无新信息") && !result.isBlank()) {
                String newMemory = result.trim();
                if (newMemory.length() > 500) {
                    newMemory = newMemory.substring(newMemory.length() - 500);
                }
                memory.saveLongTerm(userId, newMemory);
                log.info("[Memory] Updated long-term memory for user {}: {}", userId, newMemory);
            }
        } catch (Exception e) {
            log.debug("Failed to extract long-term memory: {}", e.getMessage());
        }
    }
}
