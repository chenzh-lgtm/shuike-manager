package com.shuike.manager.modules.review.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.review.entity.ReviewRecord;
import com.shuike.manager.modules.review.mapper.ReviewRecordMapper;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlan;
import com.shuike.manager.modules.teachingplan.mapper.TeachingPlanMapper;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRecordMapper reviewMapper;
    private final TeachingPlanMapper planMapper;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void collegeReview(Long planId, String action, String comment) {
        TeachingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "授课计划不存在");
        if (!"SUBMITTED".equals(plan.getStatus())) throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "当前状态不允许学院审核");

        ReviewRecord record = new ReviewRecord();
        record.setPlanId(planId);
        record.setReviewerId(SecurityUtils.getCurrentUserId());
        record.setReviewLevel("COLLEGE");
        record.setAction(action);
        record.setComment(comment);
        record.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(record);

        if ("APPROVE".equals(action)) {
            plan.setStatus("COLLEGE_PASSED");
            plan.setCollegeReviewTime(LocalDateTime.now());
        } else {
            plan.setStatus("REJECTED");
        }
        planMapper.updateById(plan);

        Notification noti = new Notification();
        noti.setUserId(plan.getTeacherId());
        noti.setType("REVIEW_RESULT");
        noti.setTitle("学院审核" + ("APPROVE".equals(action) ? "通过" : "驳回"));
        noti.setContent(comment);
        noti.setTargetUrl("/teacher/teaching-plans/" + planId);
        notificationMapper.insert(noti);
    }

    @Transactional
    public void officeReview(Long planId, String action, String comment, String rejectTarget) {
        TeachingPlan plan = planMapper.selectById(planId);
        if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "授课计划不存在");
        if (!"COLLEGE_PASSED".equals(plan.getStatus())) throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "当前状态不允许教务处审核");

        ReviewRecord record = new ReviewRecord();
        record.setPlanId(planId);
        record.setReviewerId(SecurityUtils.getCurrentUserId());
        record.setReviewLevel("OFFICE");
        record.setAction(action);
        record.setComment(comment);
        record.setReviewTime(LocalDateTime.now());
        reviewMapper.insert(record);

        if ("APPROVE".equals(action)) {
            plan.setStatus("APPROVED");
            plan.setOfficeReviewTime(LocalDateTime.now());
        } else {
            plan.setStatus("REJECTED");
        }
        planMapper.updateById(plan);

        Notification noti = new Notification();
        noti.setUserId(plan.getTeacherId());
        noti.setType("REVIEW_RESULT");
        noti.setTitle("教务处" + ("APPROVE".equals(action) ? "终审通过" : "驳回修改"));
        noti.setContent(comment);
        noti.setTargetUrl("/teacher/teaching-plans/" + planId);
        notificationMapper.insert(noti);
    }

    @Transactional
    public void collegeBatchReview(List<Long> planIds, String action, String comment) {
        for (Long id : planIds) { collegeReview(id, action, comment); }
    }

    public List<ReviewRecord> getHistory(Long planId) {
        return reviewMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                .eq(ReviewRecord::getPlanId, planId).orderByAsc(ReviewRecord::getReviewTime));
    }
}
