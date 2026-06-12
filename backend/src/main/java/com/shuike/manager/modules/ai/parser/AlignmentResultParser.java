package com.shuike.manager.modules.ai.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class AlignmentResultParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlignmentReportData parse(String llmResponse) {
        AlignmentReportData data = new AlignmentReportData();
        try {
            String jsonStr = extractJson(llmResponse);
            if (jsonStr == null) { data.setCoverageScore(50); return data; }

            JsonNode root = objectMapper.readTree(jsonStr);
            data.setCoverageScore(clampScore(root.path("coverageScore").asInt(60)));

            List<String> keywords = new ArrayList<>();
            if (root.has("industryKeywords")) {
                for (JsonNode k : root.get("industryKeywords")) { keywords.add(k.asText()); }
            }
            data.setIndustryKeywords(keywords);

            List<Map<String, String>> gaps = new ArrayList<>();
            if (root.has("gapAnalysis")) {
                for (JsonNode g : root.get("gapAnalysis")) {
                    Map<String, String> gap = new LinkedHashMap<>();
                    gap.put("capability", g.path("capability").asText(""));
                    gap.put("coverage", g.path("coverage").asText("WEAK"));
                    gap.put("suggestion", g.path("suggestion").asText(""));
                    gaps.add(gap);
                }
            }
            data.setGapAnalysis(gaps);

            List<String> suggestions = new ArrayList<>();
            if (root.has("suggestions")) {
                for (JsonNode s : root.get("suggestions")) { suggestions.add(s.asText()); }
            }
            data.setSuggestions(suggestions);

        } catch (Exception e) {
            log.error("[AlignmentParser] 解析失败: {}", e.getMessage());
            data.setCoverageScore(50);
        }
        return data;
    }

    private String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }

    private int clampScore(int score) { return Math.max(0, Math.min(100, score)); }

    public static class AlignmentReportData {
        private int coverageScore;
        private List<String> industryKeywords = new ArrayList<>();
        private List<Map<String, String>> gapAnalysis = new ArrayList<>();
        private List<String> suggestions = new ArrayList<>();
        public int getCoverageScore() { return coverageScore; }
        public void setCoverageScore(int s) { this.coverageScore = s; }
        public List<String> getIndustryKeywords() { return industryKeywords; }
        public void setIndustryKeywords(List<String> k) { this.industryKeywords = k; }
        public List<Map<String, String>> getGapAnalysis() { return gapAnalysis; }
        public void setGapAnalysis(List<Map<String, String>> g) { this.gapAnalysis = g; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> s) { this.suggestions = s; }
    }
}
