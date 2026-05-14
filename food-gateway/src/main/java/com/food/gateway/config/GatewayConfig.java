package com.food.gateway.config;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.HttpProtocol;

@Configuration
public class GatewayConfig {

    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return httpClient -> httpClient.protocol(HttpProtocol.HTTP11);
    }
}
