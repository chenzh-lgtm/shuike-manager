package com.shuike.manager.modules.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Slf4j
@Component
public class VolcanoEngineClient {

    @Value("${volcano.api-key}")
    private String apiKey;

    @Value("${volcano.endpoint}")
    private String endpoint;

    @Value("${volcano.default-model}")
    private String defaultModel;

    @Value("${volcano.temperature:0.3}")
    private double temperature;

    private final RestTemplate restTemplate;

    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(180000);    // 180秒读取超时（对齐分析报告很长）
        restTemplate = new RestTemplate(factory);
    }

    /**
     * 调用火山引擎大模型聊天接口
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return LLM 返回的文本内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        // 构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", defaultModel);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", 2000);

        String url = endpoint + "/chat/completions";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.info("[AI] 开始调用火山引擎 LLM, model={}, temperature={}", defaultModel, temperature);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long costMs = System.currentTimeMillis() - startTime;

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[AI] LLM 返回空响应, costMs={}", costMs);
                throw new RuntimeException("LLM 返回空响应");
            }

            // 解析 choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.error("[AI] LLM 返回无 choices, body={}", responseBody);
                throw new RuntimeException("LLM 返回格式异常");
            }

            Map<String, Object> choice = choices.get(0);
            Map<String, String> message = (Map<String, String>) choice.get("message");
            String content = message.get("content");

            // 获取 token 使用量
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            int promptTokens = usage != null && usage.get("prompt_tokens") != null
                    ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
            int completionTokens = usage != null && usage.get("completion_tokens") != null
                    ? ((Number) usage.get("completion_tokens")).intValue() : 0;

            log.info("[AI] LLM 调用成功, costMs={}, promptTokens={}, completionTokens={}",
                    costMs, promptTokens, completionTokens);

            return content;

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            log.error("[AI] LLM 调用失败, costMs={}, error={}", costMs, e.getMessage());
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带自定义max_tokens的调用（用于长输出场景如对齐分析报告）
     */
    public String chatWithMaxTokens(String systemPrompt, String userPrompt, int maxTokens) {
        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> sm = new HashMap<>(); sm.put("role","system"); sm.put("content",systemPrompt); messages.add(sm);
        Map<String, String> um = new HashMap<>(); um.put("role","user"); um.put("content",userPrompt); messages.add(um);

        Map<String, Object> body = new HashMap<>();
        body.put("model", defaultModel);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        String url = endpoint + "/chat/completions";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            long costMs = System.currentTimeMillis() - startTime;
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) throw new RuntimeException("LLM 返回空响应");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("LLM 返回无choices");
            Map<String, Object> choice = choices.get(0);
            Map<String, String> message = (Map<String, String>) choice.get("message");
            String content = message.get("content");
            Map<String, Object> usage = (Map<String, Object>) responseBody.get("usage");
            log.info("[AI] LLM longform call success, costMs={}, tokens={}", costMs,
                    usage != null ? usage.get("total_tokens") : "N/A");
            return content;
        } catch (Exception e) {
            log.error("[AI] LLM longform call failed: {}", e.getMessage());
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 带重试的调用
     */
    public String chatWithRetry(String systemPrompt, String userPrompt, int maxRetries) {
        Exception lastException = null;
        for (int i = 0; i <= maxRetries; i++) {
            try {
                return chat(systemPrompt, userPrompt);
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries) {
                    log.warn("[AI] 第{}次调用失败，{}ms后重试...", i + 1, (i + 1) * 2000L);
                    try { Thread.sleep((i + 1) * 2000L); } catch (InterruptedException ignored) {}
                }
            }
        }
        throw new RuntimeException("AI 服务调用失败(已重试" + maxRetries + "次): " +
                (lastException != null ? lastException.getMessage() : "未知错误"));
    }
}
