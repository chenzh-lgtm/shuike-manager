package com.shuike.manager.modules.alignment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.modules.ai.client.VolcanoEngineClient;
import com.shuike.manager.modules.alignment.entity.AlignmentReport;
import com.shuike.manager.modules.alignment.mapper.AlignmentReportMapper;
import com.shuike.manager.modules.talentplan.entity.TalentCultivationPlan;
import com.shuike.manager.modules.talentplan.mapper.TalentCultivationPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlignmentService {
    private final AlignmentReportMapper reportMapper;
    private final TalentCultivationPlanMapper tcpMapper;
    private final VolcanoEngineClient volcanoClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IPage<AlignmentReport> page(Page<AlignmentReport> page, Long collegeId) {
        LambdaQueryWrapper<AlignmentReport> qw = new LambdaQueryWrapper<>();
        if (collegeId != null) qw.eq(AlignmentReport::getCollegeId, collegeId);
        qw.orderByDesc(AlignmentReport::getCreatedAt);
        return reportMapper.selectPage(page, qw);
    }

    public AlignmentReport getById(Long id) {
        AlignmentReport report = reportMapper.selectById(id);
        if (report == null) throw new BusinessException(ErrorCode.NOT_FOUND, "分析报告不存在");
        return report;
    }

    public void delete(Long id) {
        getById(id);
        reportMapper.deleteById(id);
        log.info("[对齐分析] 删除报告 id={}", id);
    }

    /**
     * 异步触发分析（不阻塞HTTP响应）
     * @param tcpId 人培方案ID
     * @param initiatorId 发起用户ID（从Controller传入，避免异步线程丢失SecurityContext）
     */
    @Async("aiEvaluationExecutor")
    public void analyzeAsync(Long tcpId, Long initiatorId) {
        try {
            analyze(tcpId, initiatorId);
        } catch (Exception e) {
            log.error("[对齐分析] 异步执行失败: tcpId={}, err={}", tcpId, e.getMessage(), e);
        }
    }

    @Transactional
    public AlignmentReport analyze(Long tcpId, Long initiatorId) {
        TalentCultivationPlan tcp = tcpMapper.selectById(tcpId);
        if (tcp == null) throw new BusinessException(ErrorCode.NOT_FOUND, "人培方案不存在");

        String contentText = tcp.getContentText() != null ? tcp.getContentText() : "";
        String targets = tcp.getTargets() != null ? tcp.getTargets() : "";
        String courseSystem = tcp.getCourseSystem() != null ? tcp.getCourseSystem() : "";
        // 截断到4000字，避免prompt过长导致LLM响应截断
        if (contentText.length() > 4000) contentText = contentText.substring(0, 4000) + "...(内容已截断)";
        String combinedContent = "## 专业名称\n" + tcp.getMajorName() + "\n\n"
                + "## 人才培养方案全文\n" + contentText + "\n\n"
                + "## 培养目标\n" + (targets.length() > 2000 ? targets.substring(0, 2000) : targets) + "\n\n"
                + "## 课程体系\n" + (courseSystem.length() > 2000 ? courseSystem.substring(0, 2000) : courseSystem);

        String prompt = buildAnalysisPrompt(tcp.getMajorName(), combinedContent);

        log.info("[对齐分析] 开始调用LLM分析专业: {}", tcp.getMajorName());
        long startTime = System.currentTimeMillis();

        String llmResponse;
        try {
            llmResponse = volcanoClient.chatWithMaxTokens(
                "你是中国高等教育领域资深的研究专家和教育评估学者，擅长产业需求分析、课程体系设计、人才培养方案评估。你的回答需要学术化、数据化、结构化。",
                prompt, 6000
            );
        } catch (Exception e) {
            log.error("[对齐分析] LLM调用失败: {}", e.getMessage());
            throw new BusinessException(4001, "AI服务调用失败，请稍后重试: " + e.getMessage());
        }

        long costMs = System.currentTimeMillis() - startTime;
        log.info("[对齐分析] LLM返回成功, 耗时={}ms, 响应长度={}", costMs, llmResponse.length());

        // 解析LLM返回的JSON
        try {
            String jsonStr = extractJson(llmResponse);
            log.info("[对齐分析] 提取JSON长度={}", jsonStr.length());
            if (jsonStr.length() < 50) {
                log.warn("[对齐分析] JSON太短, 原始响应: {}", llmResponse.substring(0, Math.min(500, llmResponse.length())));
            }
            JsonNode root = objectMapper.readTree(jsonStr);

            AlignmentReport report = new AlignmentReport();
            report.setTcpId(tcpId);
            report.setMajorName(tcp.getMajorName());
            report.setCollegeId(tcp.getCollegeId());
            report.setInitiatorId(initiatorId);
            report.setModelVersion("doubao-pro-32k");

            // 产业需求关键词
            report.setIndustryKeywords(root.path("industryKeywords").toString());

            // 覆盖度评分
            report.setCoverageScore(root.path("coverageScore").asInt(70));

            // 各维度评分
            report.setDimensionScores(root.path("dimensionScores").toString());

            // 差距分析
            report.setGapAnalysis(root.path("gapAnalysis").toString());

            // 改进建议
            report.setSuggestions(root.path("suggestions").toString());

            // 完整报告JSON
            report.setReportJson(root.toString());

            reportMapper.insert(report);
            log.info("[对齐分析] 报告生成成功 id={} coverScore={}", report.getId(), report.getCoverageScore());
            return report;
        } catch (Exception e) {
            log.error("[对齐分析] 报告保存失败: {}", e.getMessage(), e);
            throw new BusinessException(4003, "报告保存失败: " + e.getMessage());
        }
    }

    private String buildAnalysisPrompt(String majorName, String content) {
        return "请你作为一位权威的高等教育产业需求分析专家，基于以下人才培养方案文本，撰写一份详细、学术化的**产业需求对齐分析报告**。\n\n"
                + content + "\n\n"
                + "请严格按照以下JSON格式返回分析结果（必须是合法的JSON，不包含任何markdown标记）：\n\n"
                + "{\n"
                + "  \"overview\": \"专业总体概述（100-200字），分析该专业的定位、特色和人才培养方向\",\n"
                + "  \"industryBackground\": {\n"
                + "    \"summary\": \"产业发展背景概述（150-250字），分析当前该专业对应的产业发展趋势、政策环境、市场规模等\",\n"
                + "    \"trends\": [\"趋势1\", \"趋势2\", \"趋势3\", \"趋势4\", \"趋势5\"]\n"
                + "  },\n"
                + "  \"industryKeywords\": [\"关键词1\", \"关键词2\", ..., \"关键词10\"],\n"
                + "  \"demandAnalysis\": {\n"
                + "    \"summary\": \"产业人才需求分析（150-250字），分析当前市场对该专业人才的需求量、薪资水平、岗位分布等\",\n"
                + "    \"jobPositions\": [{\"title\": \"岗位名称\", \"count\": 需求量评分0-100, \"salary\": \"薪资水平描述\"}],\n"
                + "    \"requiredSkills\": [{\"skill\": \"技能名称\", \"level\": \"掌握程度(精通/熟练/了解)\", \"importance\": 重要度评分0-100}],\n"
                + "    \"skillRadar\": {\"技术能力\":70,\"沟通协作\":65,\"创新思维\":60,\"项目管理\":55,\"数据分析\":75,\"行业认知\":80}\n"
                + "  },\n"
                + "  \"courseAnalysis\": {\n"
                + "    \"summary\": \"课程体系分析概述（150-250字）\",\n"
                + "    \"publicCourses\": [{\"name\": \"公共课名称\", \"credits\": 学分, \"relevance\": \"与产业需求的关联度分析(30-50字)\"}],\n"
                + "    \"coreCourses\": [{\"name\": \"专业核心课名称\", \"credits\": 学分, \"importance\": 重要度评分0-100, \"reason\": \"为什么这是核心课(30-50字)\"}],\n"
                + "    \"electiveCourses\": [{\"name\": \"选修课名称\", \"credits\": 学分, \"suggestion\": \"选课建议(30-50字)\"}],\n"
                + "    \"courseScoreRadar\": {\"公共课匹配度\":75,\"核心课匹配度\":80,\"选修课丰富度\":60,\"实践环节占比\":55,\"创新创业融合\":50}\n"
                + "  },\n"
                + "  \"learningOutcomes\": {\n"
                + "    \"summary\": \"学生学习成果分析（150-250字），分析学生毕业时应达到的知识、能力、素质水平\",\n"
                + "    \"knowledgeLevel\": {\n"
                + "      \"score\": 知识掌握度评分0-100,\n"
                + "      \"strengths\": [\"知识优势1\", \"知识优势2\"],\n"
                + "      \"weaknesses\": [\"知识不足1\", \"知识不足2\"]\n"
                + "    },\n"
                + "    \"abilityLevel\": {\n"
                + "      \"score\": 能力培养度评分0-100,\n"
                + "      \"strengths\": [\"能力优势1\", \"能力优势2\"],\n"
                + "      \"weaknesses\": [\"能力不足1\", \"能力不足2\"]\n"
                + "    },\n"
                + "    \"graduateCompetencies\": [{\"competency\": \"毕业能力项\", \"target\": \"目标水平\", \"current\": \"当前达成度评估\"}]\n"
                + "  },\n"
                + "  \"employmentAnalysis\": {\n"
                + "    \"summary\": \"就业前景分析（150-250字）\",\n"
                + "    \"targetIndustries\": [{\"name\": \"目标行业\", \"demand\": \"需求程度(高/中/低)\", \"growth\": \"增长趋势描述\"}],\n"
                + "    \"startingSalary\": \"起薪范围描述\",\n"
                + "    \"employmentRate\": \"预计就业率(百分比)\",\n"
                + "    \"competitivenessScore\": 就业竞争力评分0-100\n"
                + "  },\n"
                + "  \"coverageScore\": 人培方案与产业需求总体覆盖度评分(0-100),\n"
                + "  \"dimensionScores\": {\"课程设置合理性\":75,\"产业需求匹配度\":70,\"实践能力培养\":65,\"创新素质培养\":60,\"就业竞争力\":72},\n"
                + "  \"gapAnalysis\": [\n"
                + "    {\"area\": \"差距领域\", \"gap\": \"具体差距描述(50-80字)\", \"impact\": \"对学生和就业的影响\", \"severity\": \"严重程度(高/中/低)\"}\n"
                + "  ],\n"
                + "  \"suggestions\": [\n"
                + "    {\"category\": \"建议类别(课程设置/教学方法/实践环节/师资建设/校企合作)\", \"content\": \"具体建议内容(50-100字)\", \"priority\": \"优先级(高/中/低)\", \"difficulty\": \"实施难度(高/中/低)\"}\n"
                + "  ],\n"
                + "  \"summary\": \"综合结论与展望（200-300字），总结分析发现，提出改革方向建议\"\n"
                + "}\n\n"
                + "重要提示：\n"
                + "1. 必须返回合法的JSON格式，不要包含```json等标记\n"
                + "2. 所有评分范围为0-100\n"
                + "3. 分析要学术化、具体化，避免空泛\n"
                + "4. 所有中文文本请保证语法正确、表达专业\n"
                + "5. 如果人培方案内容不完整，基于专业名称和行业常识进行合理推断和补充";
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        // 移除 markdown 代码块标记
        String cleaned = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) return cleaned.substring(start, end + 1);
        return "{}";
    }
}
