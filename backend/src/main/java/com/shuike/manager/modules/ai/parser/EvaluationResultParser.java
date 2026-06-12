package com.shuike.manager.modules.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EvaluationResultParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DimensionResult parseDimensionResult(String llmResponse, String dimension) {
        try {
            String jsonStr = extractJson(llmResponse);
            if (jsonStr == null) {
                log.warn("[Parser] 维度{} 未能提取JSON", dimension);
                return createFallbackResult(dimension);
            }
            JsonNode root = objectMapper.readTree(jsonStr);
            int score = clampScore(root.path("score").asInt(80));
            String suggestion = root.path("suggestion").asText("");
            List<String> issues = new ArrayList<>();
            if (root.has("issues") && root.get("issues").isArray()) {
                for (JsonNode issue : root.get("issues")) { issues.add(issue.asText()); }
            }
            return new DimensionResult(dimension, score, issues, suggestion);
        } catch (Exception e) {
            log.error("[Parser] 解析失败: {}", e.getMessage());
            return createFallbackResult(dimension);
        }
    }

    public CompositeResult aggregateResults(Map<String, DimensionResult> dimensionResults) {
        Map<String, Object> dimensionScores = new LinkedHashMap<>();
        List<Map<String, String>> allSuggestions = new ArrayList<>();
        int totalWeightedScore = 0;
        int[] weights = {30, 30, 20, 20};
        String[] dims = {"completeness", "standard_match", "format", "innovation"};
        String[] dimLabels = {"内容完整性", "与课程标准匹配度", "格式规范性", "创新性"};

        for (int i = 0; i < dims.length; i++) {
            DimensionResult dr = dimensionResults.get(dims[i]);
            int score = dr != null ? dr.getScore() : 60;
            dimensionScores.put(dims[i], score);
            totalWeightedScore += score * weights[i];
            // 每个维度只生成一条建议（合并所有 issue）
            if (dr != null && dr.getIssues() != null && !dr.getIssues().isEmpty()) {
                Map<String, String> sug = new LinkedHashMap<>();
                sug.put("dimension", dimLabels[i]);
                sug.put("issue", String.join("；", dr.getIssues()));
                sug.put("direction", dr.getSuggestion() != null ? dr.getSuggestion() : "");
                allSuggestions.add(sug);
            }
        }

        int overallScore = totalWeightedScore / 100;
        CompositeResult result = new CompositeResult();
        result.setOverallScore(overallScore);
        result.setDimensionScores(dimensionScores);
        result.setSuggestions(allSuggestions);
        return result;
    }

    private String extractJson(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("\\{[^{}]*\"score\"[^{}]*\\}", Pattern.DOTALL);
        Matcher m = p.matcher(text);
        if (m.find()) return m.group();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }

    private DimensionResult createFallbackResult(String dimension) {
        return new DimensionResult(dimension, 70, new ArrayList<>(), "AI评审解析异常，建议人工评审");
    }

    private int clampScore(int score) { return Math.max(0, Math.min(100, score)); }

    // ---- Inner classes ----

    public static class DimensionResult {
        private final String dimension;
        private final int score;
        private final List<String> issues;
        private final String suggestion;
        public DimensionResult(String d, int s, List<String> i, String sg) { dimension=d; score=s; issues=i; suggestion=sg; }
        public String getDimension() { return dimension; }
        public int getScore() { return score; }
        public List<String> getIssues() { return issues; }
        public String getSuggestion() { return suggestion; }
    }

    public static class CompositeResult {
        private int overallScore;
        private Map<String, Object> dimensionScores;
        private List<Map<String, String>> suggestions;
        public int getOverallScore() { return overallScore; }
        public void setOverallScore(int s) { this.overallScore = s; }
        public Map<String, Object> getDimensionScores() { return dimensionScores; }
        public void setDimensionScores(Map<String, Object> s) { this.dimensionScores = s; }
        public List<Map<String, String>> getSuggestions() { return suggestions; }
        public void setSuggestions(List<Map<String, String>> s) { this.suggestions = s; }
    }
}
