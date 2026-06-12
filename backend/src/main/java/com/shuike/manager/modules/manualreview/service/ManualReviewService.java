package com.shuike.manager.modules.manualreview.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.aievaluation.entity.AiEvaluation;
import com.shuike.manager.modules.aievaluation.mapper.AiEvaluationMapper;
import com.shuike.manager.modules.manualreview.entity.ManualReview;
import com.shuike.manager.modules.manualreview.mapper.ManualReviewMapper;
import com.shuike.manager.modules.phasematerial.entity.PhaseMaterial;
import com.shuike.manager.modules.phasematerial.mapper.PhaseMaterialMapper;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import com.shuike.manager.modules.user.entity.User;
import com.shuike.manager.modules.user.entity.UserRole;
import com.shuike.manager.modules.user.mapper.UserMapper;
import com.shuike.manager.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualReviewService {
    private final ManualReviewMapper reviewMapper;
    private final AiEvaluationMapper evalMapper;
    private final PhaseMaterialMapper materialMapper;
    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public IPage<ManualReview> page(Page<ManualReview> page) {
        return reviewMapper.selectPage(page, new LambdaQueryWrapper<ManualReview>().orderByDesc(ManualReview::getReviewTime));
    }

    public List<ManualReview> getByEvaluationId(Long evaluationId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<ManualReview>()
                .eq(ManualReview::getEvaluationId, evaluationId).orderByDesc(ManualReview::getReviewTime));
    }

    @Transactional
    public ManualReview review(Long evaluationId, String action, Integer modifiedScore, String modifyReason,
                                String reviewComment, String revisionRequirements, LocalDate deadline,
                                String reviewLevel) {
        AiEvaluation eval = evalMapper.selectById(evaluationId);
        if (eval == null) throw new BusinessException(ErrorCode.NOT_FOUND, "AI评审不存在");
        if (!"COMPLETED".equals(eval.getStatus())) throw new BusinessException(400, "AI评审未完成，无法复核");

        PhaseMaterial material = materialMapper.selectById(eval.getMaterialId());
        if (material == null) throw new BusinessException(ErrorCode.NOT_FOUND, "材料不存在");

        // 主任审核时检查状态必须是 AI_COMPLETED
        if ("COLLEGE".equals(reviewLevel) && !"AI_COMPLETED".equals(material.getStatus())) {
            throw new BusinessException(400, "当前材料状态不允许主任审核");
        }
        // 教务处终审时检查状态必须是 COLLEGE_APPROVED
        if ("OFFICE".equals(reviewLevel) && !"COLLEGE_APPROVED".equals(material.getStatus())) {
            throw new BusinessException(400, "当前材料状态不允许教务处终审，请等待主任先审核");
        }

        ManualReview review = new ManualReview();
        review.setEvaluationId(evaluationId);
        review.setReviewerId(SecurityUtils.getCurrentUserId());
        review.setAction(action);
        review.setModifiedScore(modifiedScore);
        review.setModifyReason(modifyReason);
        review.setReviewComment(reviewComment);
        review.setRevisionRequirements(revisionRequirements);
        review.setDeadline(deadline);
        review.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(review);

        // 两级审核状态流转
        if ("REJECT".equals(action)) {
            material.setStatus("AI_REJECTED");
        } else if ("COLLEGE".equals(reviewLevel)) {
            material.setStatus("COLLEGE_APPROVED");  // 主任通过 → 待教务处终审
        } else {
            material.setStatus("OFFICE_APPROVED");   // 教务处通过 → 最终通过
        }
        materialMapper.updateById(material);

        // 发送通知给教师
        String levelLabel = "OFFICE".equals(reviewLevel) ? "教务处" : "专业主任";
        boolean isFinal = "OFFICE".equals(reviewLevel) || "REJECT".equals(action);
        String typeLabel = material.getMaterialType() != null ? material.getMaterialType() : "材料";
        Notification noti = new Notification();
        noti.setUserId(material.getTeacherId());
        noti.setType("AI_MANUAL_REVIEW");
        noti.setTitle(levelLabel + ("CONFIRM".equals(action)
                ? (isFinal ? "审核通过" : "审核通过，已提交教务处终审")
                : "驳回修改"));
        noti.setContent(reviewComment != null && !reviewComment.isEmpty() ? reviewComment : "您的「"+typeLabel+"」已被"+levelLabel+("CONFIRM".equals(action)?"通过":"驳回"));
        noti.setTargetUrl("/teacher/review-progress");
        notificationMapper.insert(noti);

        // 主任通过时：通知教务处有新材料待终审
        if ("COLLEGE".equals(reviewLevel) && "CONFIRM".equals(action)) {
            List<UserRole> officeUsers = userRoleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getRole, "OFFICE"));
            for (UserRole ur : officeUsers) {
                Notification officeNoti = new Notification();
                officeNoti.setUserId(ur.getUserId());
                officeNoti.setType("FINAL_REVIEW");
                officeNoti.setTitle("新材料待教务处终审");
                officeNoti.setContent("专业主任已审核通过一份「"+typeLabel+"」，请进行最终审核");
                officeNoti.setTargetUrl("/office/ai-review-manage");
                notificationMapper.insert(officeNoti);
            }
        }

        return review;
    }
}
