package com.food.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ToolResult {

    private String text;
    private List<Map<String, Object>> articles;
    private List<Map<String, Object>> coupons;

    public ToolResult(String text) {
        this(text, null, null);
    }
}
