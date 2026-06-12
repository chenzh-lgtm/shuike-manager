package com.shuike.manager.modules.aievaluation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.config.MinioConfig;
import com.shuike.manager.modules.ai.client.VolcanoEngineClient;
import com.shuike.manager.modules.ai.parser.EvaluationResultParser;
import com.shuike.manager.modules.ai.prompt.PromptBuilder;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.mapper.AiEvaluationMapper;
import com.shuike.manager.modules.file.service.DocumentParserService;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterial;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterialFile;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialMapper;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialFileMapper;
import com.shuike.manager.modules.coursestandard.entity.CourseStandard;
import com.shuike.manager.modules.coursestandard.mapper.CourseStandardMapper;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationService {

    private final AiEvaluationMapper evalMapper;
    private final PhaseMaterialMapper materialMapper;
    private final PhaseMaterialFileMapper fileMapper;
    private final CourseStandardMapper standardMapper;
    private final NotificationMapper notificationMapper;
    private final VolcanoEngineClient volcanoClient;
    private final PromptBuilder promptBuilder;
    private final EvaluationResultParser resultParser;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final DocumentParserService documentParserService;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 0;  // 不重试，失败直接跳过
    private static final long OVERALL_TIMEOUT_SECONDS = 120;

    public IPage<AiEvaluation> pageMine(Page<AiEvaluation> page) {
        return evalMapper.selectPage(page, new LambdaQueryWrapper<AiEvaluation>()
                .orderByDesc(AiEvaluation::getCreatedAt));
    }

    public IPage<AiEvaluation> pageAll(Page<AiEvaluation> page, String status) {
        LambdaQueryWrapper<AiEvaluation> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq(AiEvaluation::getStatus, status);
        qw.orderByDesc(AiEvaluation::getCreatedAt);
        return evalMapper.selectPage(page, qw);
    }

    public AiEvaluation getById(Long id) {
        AiEvaluation eval = evalMapper.selectById(id);
        if (eval == null) throw new BusinessException(ErrorCode.NOT_FOUND, "AI评审不存在");
        return eval;
    }

    @Transactional
    public AiEvaluation submit(Long materialId) {
        PhaseMaterial material = materialMapper.selectById(materialId);
        if (material == null) throw new BusinessException(ErrorCode.NOT_FOUND, "材料不存在");
        material.setStatus("AI_EVALUATING");
        materialMapper.updateById(material);

        AiEvaluation eval = new AiEvaluation();
        eval.setMaterialId(materialId);
        eval.setStatus("PENDING");
        evalMapper.insert(eval);
        final Long evalId = eval.getId();
        // 等当前事务提交后，再异步执行评审
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                new Thread(() -> {
                    log.info("[AI评审] 异步线程启动(事务已提交) evalId={}", evalId);
                    applicationContext.getBean(AiEvaluationService.class).runEvaluation(evalId);
                }, "ai-eval-" + evalId).start();
            }
        });
        return eval;
    }

    @Transactional
    public void runEvaluation(Long evaluationId) {
        log.info("[AI评审] 开始评审 evaluationId={}", evaluationId);
        AiEvaluation eval = evalMapper.selectById(evaluationId);
        if (eval == null) { log.warn("[AI评审] 找不到评审记录 evaluationId={}", evaluationId); return; }

        long startTime = System.currentTimeMillis();
        try {
            eval.setStatus("EVALUATING");
            evalMapper.updateById(eval);

            // 1. 获取材料文件内容（从DB textContent 或 MinIO 下载解析）
            PhaseMaterial material = materialMapper.selectById(eval.getMaterialId());
            if (material == null) throw new RuntimeException("材料不存在");

            List<PhaseMaterialFile> files = fileMapper.selectList(
                    new LambdaQueryWrapper<PhaseMaterialFile>()
                            .eq(PhaseMaterialFile::getMaterialId, eval.getMaterialId()));
            StringBuilder contentBuilder = new StringBuilder();
            for (PhaseMaterialFile f : files) {
                if (f.getTextContent() != null && !f.getTextContent().isEmpty()) {
                    contentBuilder.append(f.getTextContent()).append("\n\n");
                } else if (f.getFileUrl() != null) {
                    // 从 MinIO 下载并解析
                    try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                            .bucket(minioConfig.getBucket())
                            .object(f.getFileUrl())
                            .build())) {
                        String text = documentParserService.extractText(is, f.getFileName());
                        contentBuilder.append(text).append("\n\n");
                        // 缓存解析结果
                        f.setTextContent(text);
                        fileMapper.updateById(f);
                        log.info("[AI评审] 从MinIO解析文件: {} -> {}字符", f.getFileName(), text.length());
                    } catch (Exception ex) {
                        log.warn("[AI评审] MinIO文件读取失败: {}", f.getFileName());
                        contentBuilder.append("[文件: ").append(f.getFileName()).append(" - 无法读取]\n");
                    }
                }
            }
            String materialContent = contentBuilder.toString();
            if (materialContent.trim().isEmpty()) {
                materialContent = "[材料无文本内容]";
            }

            // 2. 获取关联的课程标准
            String courseStandard = "";
            if (material.getCourseId() != null) {
                CourseStandard standard = standardMapper.selectOne(
                        new LambdaQueryWrapper<CourseStandard>()
                                .eq(CourseStandard::getCourseId, material.getCourseId())
                                .eq(CourseStandard::getStatus, "ACTIVE")
                                .orderByDesc(CourseStandard::getCreatedAt)
                                .last("LIMIT 1"));
                if (standard != null && standard.getContentText() != null) {
                    courseStandard = standard.getContentText();
                }
            }

            // 3. 构建 4 个维度的 Prompt
            Map<String, PromptBuilder.PromptPair> prompts = promptBuilder.buildAllDimensionPrompts(
                    material.getMaterialType(), materialContent, courseStandard);

            log.info("[AI评审] evaluationId={}, 材料类型={}, Prompt维度数={}",
                    evaluationId, material.getMaterialType(), prompts.size());

            // 4. 并发调用 4 个维度 LLM
            Map<String, EvaluationResultParser.DimensionResult> dimensionResults = new LinkedHashMap<>();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            String[] dimensions = {"completeness", "standard_match", "format", "innovation"};

            for (String dimension : dimensions) {
                PromptBuilder.PromptPair pair = prompts.get(dimension);
                if (pair == null) continue;
                CompletableFuture<Void> f = CompletableFuture.runAsync(() -> {
                    try {
                        long t0 = System.currentTimeMillis();
                        String llmResponse = volcanoClient.chat(pair.getSystemPrompt(), pair.getUserPrompt());
                        EvaluationResultParser.DimensionResult dr = resultParser.parseDimensionResult(llmResponse, dimension);
                        synchronized (dimensionResults) { dimensionResults.put(dimension, dr); }
                        log.info("[AI评审] 维度{} 完成, 评分={}, 耗时={}ms", dimension, dr.getScore(), System.currentTimeMillis()-t0);
                    } catch (Exception e) {
                        log.warn("[AI评审] 维度{} 调用失败: {}", dimension, e.getMessage());
                        synchronized (dimensionResults) {
                            dimensionResults.put(dimension, new EvaluationResultParser.DimensionResult(dimension, 65, new java.util.ArrayList<>(), "评审超时"));
                        }
                    }
                });
                futures.add(f);
            }

            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(OVERALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("[AI评审] evaluationId={} 部分维度超时", evaluationId);
            }

            // 5. 汇总评分
            EvaluationResultParser.CompositeResult composite = resultParser.aggregateResults(dimensionResults);
            String dimensionScoresJson = objectMapper.writeValueAsString(composite.getDimensionScores());
            String suggestionsJson = objectMapper.writeValueAsString(composite.getSuggestions());

            // 6. 存储结果
            eval.setScore(composite.getOverallScore());
            eval.setDimensionScores(dimensionScoresJson);
            eval.setSuggestions(suggestionsJson);
            eval.setModelVersion("doubao-pro-32k");
            eval.setPromptVersion(prompts.isEmpty() ? "v1" :
                    prompts.values().iterator().next().getPromptVersion());
            eval.setEvalTime(LocalDateTime.now());
            eval.setCostMs((int) (System.currentTimeMillis() - startTime));
            eval.setStatus("COMPLETED");
            evalMapper.updateById(eval);

            // 更新材料状态
            material.setStatus("AI_COMPLETED");
            materialMapper.updateById(material);

            // 发送通知
            Notification noti = new Notification();
            noti.setUserId(material.getTeacherId());
            noti.setType("AI_REVIEW_COMPLETED");
            noti.setTitle("AI 评审已完成");
            noti.setContent("材料「" + (material.getDescription() != null ? material.getDescription() : "无描述")
                    + "」的 AI 评审已完成，综合评分 " + composite.getOverallScore() + " 分，请查看详情。");
            noti.setTargetUrl("/teacher/ai-review/" + evaluationId);
            notificationMapper.insert(noti);

            log.info("[AI评审] evaluationId={} 完成, 评分={}, 耗时={}ms",
                    evaluationId, composite.getOverallScore(), eval.getCostMs());

        } catch (Exception e) {
            log.error("[AI评审] evaluationId={} 失败: {}", evaluationId, e.getMessage(), e);
            eval.setStatus("FAILED");
            eval.setCostMs((int) (System.currentTimeMillis() - startTime));
            evalMapper.updateById(eval);

            PhaseMaterial material = materialMapper.selectById(eval.getMaterialId());
            if (material != null) {
                material.setStatus("SUBMITTED");
                materialMapper.updateById(material);

                Notification noti = new Notification();
                noti.setUserId(material.getTeacherId());
                noti.setType("SYSTEM");
                noti.setTitle("AI 评审失败");
                noti.setContent("AI 评审服务暂时不可用，请稍后重试。");
                noti.setTargetUrl("/teacher/ai-review");
                notificationMapper.insert(noti);
            }
        }
    }
}
