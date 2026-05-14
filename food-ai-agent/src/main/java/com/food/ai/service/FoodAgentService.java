package com.food.ai.service;

import com.food.ai.dto.AgentResponse;

public interface FoodAgentService {

    AgentResponse chat(String message, Long userId);

    void clearHistory(Long userId);
}
