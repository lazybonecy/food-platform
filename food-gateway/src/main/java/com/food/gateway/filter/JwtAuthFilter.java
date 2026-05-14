package com.food.gateway.filter;

import com.food.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/article/list",
            "/api/article/hot",
            "/api/comment/article/",
            "/api/ai/chat",
            "/api/ai/clear-history",
            "/api/ai/reindex",
            "/api/ai/index-article",
            "/api/coupon/my"
    );

    private static final Pattern ARTICLE_DETAIL_PATTERN = Pattern.compile("^/api/article/\\d+$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 判断是否为公开接口
        boolean isPublic = false;
        for (String whitePath : WHITE_LIST) {
            if (path.startsWith(whitePath)) {
                isPublic = true;
                break;
            }
        }
        if (!isPublic && ARTICLE_DETAIL_PATTERN.matcher(path).matches()) {
            isPublic = true;
        }

        // 尝试解析 token
        String token = request.getHeaders().getFirst("Authorization");
        Long userId = null;
        Integer role = null;

        if (token != null && token.startsWith("Bearer ")) {
            String rawToken = token.substring(7);
            userId = JwtUtil.parseToken(rawToken);
            role = JwtUtil.getRole(rawToken);
        }

        // 非公开接口必须登录
        if (!isPublic && userId == null) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 优先使用已有的 X-User-Id（内部服务调用），其次用 JWT 解析的值
        String existingUserId = request.getHeaders().getFirst("X-User-Id");
        String finalUserId = (existingUserId != null && !existingUserId.isEmpty())
                ? existingUserId
                : (userId != null ? String.valueOf(userId) : null);

        if (finalUserId != null) {
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-User-Id", finalUserId)
                    .header("X-User-Role", role != null ? String.valueOf(role) : "")
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
