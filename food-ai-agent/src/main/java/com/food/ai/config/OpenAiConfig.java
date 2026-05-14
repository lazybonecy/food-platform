package com.food.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Configuration
public class OpenAiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(60000);

        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("api-key", apiKey)
                .messageConverters(converters -> {
                    // 添加一个能处理 octet-stream 的 converter（兼容代理返回错误 content-type 的情况）
                    converters.add(0, new OctetStreamJsonConverter());
                });
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    /**
     * 兼容 API 代理返回 application/octet-stream 但实际是 JSON 的情况
     */
    static class OctetStreamJsonConverter extends MappingJackson2HttpMessageConverter {
        OctetStreamJsonConverter() {
            super();
            setSupportedMediaTypes(List.of(
                    new MediaType("application", "octet-stream"),
                    new MediaType("application", "*")
            ));
        }
    }
}
