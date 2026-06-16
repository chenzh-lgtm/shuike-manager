package com.shuike.manager.modules.phasematerial.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.config.MinioConfig;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.mapper.AiEvaluationMapper;
import com.shuike.manager.modules.aievaluation.service.AiEvaluationService;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterial;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterialFile;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialMapper;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialFileMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.entity.UserRole;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.shuike.manager.modules.user.mapper.UserRoleMapper;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhaseMaterialService {
    private final PhaseMaterialMapper materialMapper;
    private final PhaseMaterialFileMapper fileMapper;
    private final AiEvaluationService aiEvaluationService;
    private final AiEvaluationMapper aiEvaluationMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public IPage<PhaseMaterial> page(Page<PhaseMaterial> page, Long teacherId, String status, String keyword) {
        LambdaQueryWrapper<PhaseMaterial> qw = new LambdaQueryWrapper<>();
        if (teacherId != null) qw.eq(PhaseMaterial::getTeacherId, teacherId);
        if (status != null && !status.isEmpty()) qw.eq(PhaseMaterial::getStatus, status);
        // 关键字搜索：按课程名称like匹配（需先查课程ID列表）
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 用子查询方式：先找匹配的课程ID，再用 material description like
            qw.and(w -> w.like(PhaseMaterial::getDescription, keyword).or().like(PhaseMaterial::getMaterialType, keyword));
            // 注意：课程名称搜索需在Controller层做后置过滤
        }
        qw.orderByDesc(PhaseMaterial::getCreatedAt);
        return materialMapper.selectPage(page, qw);
    }

    public PhaseMaterial getById(Long id) {
        PhaseMaterial m = materialMapper.selectById(id);
        if (m == null) throw new BusinessException(ErrorCode.NOT_FOUND, "材料不存在");
        return m;
    }

    @Transactional
    public PhaseMaterial create(PhaseMaterial material) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        material.setTeacherId(teacherId);
        material.setStatus("AI_EVALUATING");  // 直接进入AI评审

        // 冲突检测：同一教师+同一课程+同一学期+同一材料类型，且状态不是OFFICE_APPROVED
        Long existingCount = materialMapper.selectCount(new LambdaQueryWrapper<PhaseMaterial>()
                .eq(PhaseMaterial::getTeacherId, teacherId)
                .eq(PhaseMaterial::getCourseId, material.getCourseId())
                .eq(PhaseMaterial::getSemesterId, material.getSemesterId())
                .eq(PhaseMaterial::getMaterialType, material.getMaterialType())
                .ne(PhaseMaterial::getStatus, "OFFICE_APPROVED"));
        if (existingCount > 0) {
            // 存在未通过的记录，将旧的置为已取消
            materialMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PhaseMaterial>()
                    .eq(PhaseMaterial::getTeacherId, teacherId)
                    .eq(PhaseMaterial::getCourseId, material.getCourseId())
                    .eq(PhaseMaterial::getSemesterId, material.getSemesterId())
                    .eq(PhaseMaterial::getMaterialType, material.getMaterialType())
                    .ne(PhaseMaterial::getStatus, "OFFICE_APPROVED")
                    .set(PhaseMaterial::getStatus, "CANCELLED"));
            log.info("[材料创建] 检测到重复提交，已将旧材料置为CANCELLED teacherId={} courseId={}", teacherId, material.getCourseId());
        }

        materialMapper.insert(material);
        return material;
    }

    /**
     * 重新提交被驳回的材料：重置状态为AI_EVALUATING，重新触发AI评审
     */
    @Transactional
    public PhaseMaterial resubmit(Long id) {
        PhaseMaterial material = getById(id);
        if (!"AI_REJECTED".equals(material.getStatus())) {
            throw new BusinessException(400, "只有已驳回的材料可以重新提交");
        }
        material.setStatus("AI_EVALUATING");
        materialMapper.updateById(material);

        // 重新触发AI评审
        try {
            aiEvaluationService.submit(id);
            log.info("[材料重传] materialId={} AI评审已重新触发", id);
        } catch (Exception e) {
            log.error("[材料重传] materialId={} AI评审触发失败: {}", id, e.getMessage());
        }

        return material;
    }

    @Transactional
    public void delete(Long id) {
        // 1. 先查所有关联文件，从 MinIO 删除
        List<PhaseMaterialFile> files = fileMapper.selectList(
                new LambdaQueryWrapper<PhaseMaterialFile>().eq(PhaseMaterialFile::getMaterialId, id));
        for (PhaseMaterialFile f : files) {
            if (f.getFileUrl() != null && f.getFileUrl().startsWith("phase-materials/")) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucket()).object(f.getFileUrl()).build());
                    log.info("[材料删除] 已从MinIO删除: {}", f.getFileUrl());
                } catch (Exception e) {
                    log.warn("[材料删除] MinIO删除失败: {}", f.getFileUrl());
                }
            }
        }
        // 2. 删除材料附件记录
        fileMapper.delete(new LambdaQueryWrapper<PhaseMaterialFile>().eq(PhaseMaterialFile::getMaterialId, id));
        // 3. 删除 AI 评审记录
        aiEvaluationMapper.delete(new LambdaQueryWrapper<AiEvaluation>().eq(AiEvaluation::getMaterialId, id));
        // 4. 删除材料主记录
        materialMapper.deleteById(id);
        log.info("[材料删除] materialId={} 已完整删除(含MinIO文件+AI评审记录)", id);
    }

    public List<PhaseMaterialFile> getFiles(Long materialId) {
        return fileMapper.selectList(new LambdaQueryWrapper<PhaseMaterialFile>().eq(PhaseMaterialFile::getMaterialId, materialId));
    }

    @Transactional
    public void addFiles(Long materialId, List<Long> fileIds) {
        for (Long fileId : fileIds) {
            PhaseMaterialFile file = fileMapper.selectById(fileId);
            if (file != null) { file.setMaterialId(materialId); fileMapper.updateById(file); }
        }
        // 文件关联完成后，自动触发 AI 评审（submit内部已异步执行）
        try {
            aiEvaluationService.submit(materialId);
            log.info("[材料提交] materialId={} AI评审已触发(异步)", materialId);
        } catch (Exception e) {
            log.error("[材料提交] materialId={} AI评审触发失败: {}", materialId, e.getMessage());
        }

        // 通知本学院审核员有新材料提交
        try {
            PhaseMaterial material = materialMapper.selectById(materialId);
            if (material != null) {
                User teacher = userMapper.selectById(material.getTeacherId());
                String teacherName = teacher != null ? teacher.getRealName() : "教师";
                String typeLabel = material.getMaterialType() != null ? material.getMaterialType() : "材料";
                // 查找本学院审核员
                Long collegeId = teacher != null ? teacher.getCollegeId() : null;
                if (collegeId != null) {
                    List<UserRole> reviewers = userRoleMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                                    .eq(UserRole::getRole, "COLLEGE_REVIEWER"));
                    for (UserRole ur : reviewers) {
                        User reviewer = userMapper.selectById(ur.getUserId());
                        if (reviewer != null && collegeId.equals(reviewer.getCollegeId())) {
                            Notification noti = new Notification();
                            noti.setUserId(reviewer.getId());
                            noti.setType("NEW_MATERIAL");
                            noti.setTitle("新教学材料待审核");
                            noti.setContent(teacherName + " 提交了「" + typeLabel + "」，AI正在自动评审中，完成后需要您审核");
                            noti.setTargetUrl("/college/material-reviews");
                            notificationMapper.insert(noti);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[材料提交] 通知审核员失败: {}", e.getMessage());
        }
    }

    // 学院审核员：获取所有材料列表（含AI评分），按学院过滤
    public IPage<PhaseMaterial> pageForReviewer(Page<PhaseMaterial> page, String status) {
        LambdaQueryWrapper<PhaseMaterial> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            qw.eq(PhaseMaterial::getStatus, status);
        }
        qw.orderByDesc(PhaseMaterial::getCreatedAt);
        return materialMapper.selectPage(page, qw);
    }
}
