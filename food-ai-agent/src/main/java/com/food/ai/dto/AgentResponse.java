package com.food.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AgentResponse {

    private String type = "chat";

    private String reply;

    private List<Map<String, Object>> articles;

    private List<Map<String, Object>> coupons;

    public AgentResponse() {}

    public AgentResponse(String type, String reply, List<Map<String, Object>> articles, List<Map<String, Object>> coupons) {
        this.type = type;
        this.reply = reply;
        this.articles = articles;
        this.coupons = coupons;
    }
}
