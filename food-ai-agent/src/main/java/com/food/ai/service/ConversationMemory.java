package com.food.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ConversationMemory {

    private static final int MAX_ROUNDS = 5;
    private static final String LONG_TERM_KEY_PREFIX = "ai:memory:";
    private static final long LONG_TERM_TTL_DAYS = 30;

    private final ConcurrentHashMap<Long, LinkedList<Message>> shortTerm = new ConcurrentHashMap<>();

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 加载用户记忆：长期事实 + 短期对话历史
     */
    public List<Message> load(Long userId) {
        List<Message> messages = new ArrayList<>();

        // 加载长期记忆（用户偏好/习惯）
        String longTerm = getLongTerm(userId);
        if (longTerm != null && !longTerm.isEmpty()) {
            messages.add(new UserMessage("[用户记忆] " + longTerm));
            messages.add(new AssistantMessage("好的，我已记住这些用户偏好。"));
        }

        // 加载短期对话
        if (userId != null) {
            LinkedList<Message> history = shortTerm.get(userId);
            if (history != null) {
                messages.addAll(history);
            }
        }

        return messages;
    }

    /**
     * 保存一轮对话（用户消息 + AI 回复）
     */
    public void save(Long userId, String userMsg, String aiReply) {
        if (userId == null) return;

        LinkedList<Message> history = shortTerm.computeIfAbsent(userId, k -> new LinkedList<>());
        history.addLast(new UserMessage(userMsg));
        history.addLast(new AssistantMessage(aiReply));

        // 保持最多 MAX_ROUNDS 轮（每轮 2 条消息）
        while (history.size() > MAX_ROUNDS * 2) {
            history.removeFirst();
            history.removeFirst();
        }

        log.debug("Saved conversation for user {}, history size: {}", userId, history.size());
    }

    /**
     * 读取长期记忆
     */
    public String getLongTerm(Long userId) {
        if (userId == null) return null;
        try {
            return redisTemplate.opsForValue().get(LONG_TERM_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("Failed to read long-term memory for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 保存长期记忆
     */
    public void saveLongTerm(Long userId, String facts) {
        if (userId == null || facts == null || facts.isEmpty()) return;
        try {
            redisTemplate.opsForValue().set(
                    LONG_TERM_KEY_PREFIX + userId,
                    facts,
                    LONG_TERM_TTL_DAYS,
                    TimeUnit.DAYS
            );
            log.info("Saved long-term memory for user {}: {}", userId, facts);
        } catch (Exception e) {
            log.warn("Failed to save long-term memory for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 清除用户短期记忆
     */
    public void clear(Long userId) {
        if (userId != null) {
            shortTerm.remove(userId);
            log.info("Cleared conversation history for user {}", userId);
        }
    }
}
