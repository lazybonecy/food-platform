package com.food.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_BUCKET_SCRIPT =
            "local tokens_key = KEYS[1]\n" +
            "local timestamp_key = KEYS[2]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local refill_rate = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = 1\n" +
            "\n" +
            "local fill_time = capacity / refill_rate\n" +
            "local ttl = math.floor(fill_time * 2)\n" +
            "\n" +
            "local last_tokens = tonumber(redis.call('get', tokens_key))\n" +
            "if last_tokens == nil then\n" +
            "    last_tokens = capacity\n" +
            "end\n" +
            "\n" +
            "local last_refreshed = tonumber(redis.call('get', timestamp_key))\n" +
            "if last_refreshed == nil then\n" +
            "    last_refreshed = 0\n" +
            "end\n" +
            "\n" +
            "local delta = math.max(0, now - last_refreshed)\n" +
            "local filled_tokens = math.min(capacity, last_tokens + (delta * refill_rate / 1000))\n" +
            "local allowed = filled_tokens >= requested\n" +
            "local new_tokens = filled_tokens\n" +
            "if allowed then\n" +
            "    new_tokens = filled_tokens - requested\n" +
            "end\n" +
            "\n" +
            "redis.call('setex', tokens_key, ttl, tostring(new_tokens))\n" +
            "redis.call('setex', timestamp_key, ttl, tostring(now))\n" +
            "\n" +
            "if allowed then\n" +
            "    return 1\n" +
            "else\n" +
            "    return 0\n" +
            "end";

    private static final DefaultRedisScript<Long> SCRIPT = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);

    private static final long CAPACITY = 500;
    private static final double REFILL_RATE = 5000.0;

    public RateLimitFilter(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.contains("/claim") && !path.contains("/flash-claim")) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        String tokensKey = "rate_limit:tokens:" + ip;
        String timestampKey = "rate_limit:ts:" + ip;
        long now = System.currentTimeMillis();

        List<String> keys = Arrays.asList(tokensKey, timestampKey);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(TOKEN_BUCKET_SCRIPT, Long.class);

        List<String> args = Arrays.asList(
                String.valueOf(CAPACITY),
                String.valueOf(REFILL_RATE),
                String.valueOf(now));

        return redisTemplate.execute(script, keys, args)
                .next()
                .map(result -> result != null && result == 1L)
                .onErrorResume(e -> {
                    log.error("Rate limit Redis script failed, allowing request: {}", e.getMessage());
                    return Mono.just(true); // fail-open: Redis故障时放行
                })
                .flatMap(allowed -> {
                    if (allowed) {
                        return chain.filter(exchange);
                    } else {
                        ServerHttpResponse response = exchange.getResponse();
                        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
                        String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}";
                        return response.writeWith(
                                Mono.just(new DefaultDataBufferFactory().wrap(body.getBytes())));
                    }
                });
    }

    private String getClientIp(ServerWebExchange exchange) {
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        if (address != null && address.getAddress() != null) {
            return address.getAddress().getHostAddress();
        }
        return "unknown";
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
