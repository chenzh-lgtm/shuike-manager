package com.shuike.manager.modules.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 内容安全过滤器
 * 在AI评审前清洗用户注入内容，防止提示词注入攻击
 */
@Slf4j
@Component
public class ContentSanitizer {

    /** 可疑的提示词注入模式 */
    private static final Pattern[] INJECTION_PATTERNS = {
        Pattern.compile("(?i)(忽略|无视|跳过).*(指令|规则|要求|评分标准|评审|prompt|system)"),
        Pattern.compile("(?i)(给我|请给|打|评).*(满分|100分|高分|优秀|通过)"),
        Pattern.compile("(?i)(你是|你是一个|你现在是|作为|act as|you are).*(不是|不要|忘记|ignore|forget)"),
        Pattern.compile("(?i)(override|bypass|指令.*注入|系统.*提示)"),
        Pattern.compile("(?i)(忽略评分|不要评分|直接通过|强制通过)"),
        Pattern.compile("(?i)(repeat|重复).*(above|上面|之前)"),
        Pattern.compile("(?i)([Dd][Aa][Nn]|[Jj]ailbreak|[Pp]rompt.*[Ii]nject)"),
    };

    /**
     * 检测文本中是否包含可疑的提示词注入内容
     * @return 如果包含注入则返回true
     */
    public boolean containsInjection(String text) {
        if (text == null || text.isEmpty()) return false;
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(text).find()) {
                log.warn("[AI安全] 检测到可疑提示词注入: pattern={}", p.pattern());
                return true;
            }
        }
        return false;
    }

    /**
     * 清洗文本中的注入内容
     * 用占位符替换疑似注入段落
     */
    public String sanitize(String text) {
        if (text == null || text.isEmpty()) return text;
        String sanitized = text;
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(sanitized).find()) {
                log.warn("[AI安全] 已过滤注入内容: pattern={}", p.pattern());
                sanitized = p.matcher(sanitized).replaceAll("[内容已过滤]");
            }
        }
        return sanitized;
    }

    /**
     * 检测文本是否很可能是AI生成的内容
     * 通过检测AI生成文本的常见特征来判断
     */
    public int detectAIGeneration(String text) {
        if (text == null || text.isEmpty()) return 0;
        int score = 0;

        // 1. 检测典型的AI生成开头/结尾模式
        if (text.matches("(?s).*(综上所述|总而言之|因此，可以得出结论|基于以上分析|通过上述讨论).*")) score += 20;
        if (text.matches("(?s).*(作为一名|作为一位|我是|我们来|首先，).*")) score += 15;

        // 2. 检测过于规整的结构（编号列表格式）
        int numberedListCount = countMatches(text, "(?m)^\\d+\\.[\\s]+");
        if (numberedListCount >= 3) score += 15;

        // 3. 检测AI常使用的过渡词密度
        String[] aiPhrases = {"此外", "另外", "同时", "值得注意的是", "需要强调的是", "不可忽视的是", "显而易见"};
        int phraseCount = 0;
        for (String phrase : aiPhrases) {
            phraseCount += countMatches(text, phrase);
        }
        if (phraseCount >= 3) score += 15;

        // 4. 检测文本格式过于完美（无错别字、长度均匀、段落规整）
        // 段落长度方差小 → 可能AI生成
        String[] paragraphs = text.split("\n\n|\n");
        if (paragraphs.length >= 3) {
            double avgLen = 0;
            for (String p : paragraphs) avgLen += p.length();
            avgLen /= paragraphs.length;
            double variance = 0;
            for (String p : paragraphs) variance += Math.pow(p.length() - avgLen, 2);
            variance /= paragraphs.length;
            // 方差极小 → 段落长度高度一致
            if (variance < 100) score += 10;
        }

        // 5. 文本长度过于均匀，无碎片化特征
        if (text.length() > 500) {
            // 检查字符多样性
            java.util.Set<Character> chars = new java.util.HashSet<>();
            for (char c : text.toCharArray()) chars.add(c);
            if (chars.size() < 50 && text.length() > 1000) score += 10; // 字符多样性低
        }

        // 归一化到0-15分
        return Math.min(15, score / 5);
    }

    private int countMatches(String text, String regex) {
        return text.split(regex, -1).length - 1;
    }
}
