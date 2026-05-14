package com.food.article.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    private static final String SECRET = "food-platform-jwt-secret-key-must-be-at-least-256-bits-long";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        SESSIONS.put(userId, session);
        log.info("WebSocket connected: userId={}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 前端发 ping，服务端回 pong
        if ("ping".equals(message.getPayload())) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            SESSIONS.remove(userId, session);
            log.info("WebSocket disconnected: userId={}", userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket error for userId={}: {}", getUserId(session), exception.getMessage());
        SESSIONS.values().remove(session);
    }

    /**
     * 向指定用户推送通知
     */
    public void sendToUser(Long userId, Object payload) {
        WebSocketSession session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = MAPPER.writeValueAsString(payload);
                session.sendMessage(new TextMessage(json));
                log.debug("Pushed notification to userId={}", userId);
            } catch (IOException e) {
                log.warn("Failed to push notification to userId={}: {}", userId, e.getMessage());
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            Map<String, String> params = UriComponentsBuilder
                    .fromUriString("?" + query).build()
                    .getQueryParams().toSingleValueMap();
            String token = params.get("token");
            if (token == null) return null;

            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return (Long) session.getAttributes().get("userId");
        }
    }
}
