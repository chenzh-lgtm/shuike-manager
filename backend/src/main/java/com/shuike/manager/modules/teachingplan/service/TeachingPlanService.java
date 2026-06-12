package com.shuike.manager.modules.teachingplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.common.security.SecurityUtils;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlan;
import com.shuike.manager.modules.teachingplan.entity.TeachingPlanFile;
import com.shuike.manager.modules.teachingplan.mapper.TeachingPlanMapper;
import com.shuike.manager.modules.teachingplan.mapper.TeachingPlanFileMapper;
import com.shuike.manager.modules.notification.entity.Notification;
import com.shuike.manager.modules.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeachingPlanService {
    private final TeachingPlanMapper planMapper;
    private final TeachingPlanFileMapper fileMapper;
    private final NotificationMapper notificationMapper;

    public IPage<TeachingPlan> page(Page<TeachingPlan> page, Long teacherId, Long courseId, Long semesterId, String status) {
        LambdaQueryWrapper<TeachingPlan> qw = new LambdaQueryWrapper<>();
        if (teacherId != null) qw.eq(TeachingPlan::getTeacherId, teacherId);
        if (courseId != null) qw.eq(TeachingPlan::getCourseId, courseId);
        if (semesterId != null) qw.eq(TeachingPlan::getSemesterId, semesterId);
        if (status != null && !status.isEmpty()) qw.eq(TeachingPlan::getStatus, status);
        qw.orderByDesc(TeachingPlan::getCreatedAt);
        return planMapper.selectPage(page, qw);
    }

    public TeachingPlan getById(Long id) {
        TeachingPlan plan = planMapper.selectById(id);
        if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "授课计划不存在");
        if (!canView(plan)) throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看");
        return plan;
    }

    @Transactional
    public TeachingPlan create(TeachingPlan plan) {
        plan.setTeacherId(SecurityUtils.getCurrentUserId());
        plan.setStatus("DRAFT");
        planMapper.insert(plan);
        return plan;
    }

    @Transactional
    public TeachingPlan update(Long id, TeachingPlan plan) {
        TeachingPlan exist = getById(id);
        if (!"DRAFT".equals(exist.getStatus()) && !"REJECTED".equals(exist.getStatus())) {
            throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "当前状态不允许修改");
        }
        plan.setId(id);
        plan.setTeacherId(exist.getTeacherId());
        plan.setStatus("DRAFT");
        planMapper.updateById(plan);
        return planMapper.selectById(id);
    }

    @Transactional
    public void delete(Long id) {
        TeachingPlan exist = getById(id);
        if (!"DRAFT".equals(exist.getStatus())) {
            throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "仅草稿状态可删除");
        }
        fileMapper.delete(new LambdaQueryWrapper<TeachingPlanFile>().eq(TeachingPlanFile::getPlanId, id));
        planMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id) {
        TeachingPlan plan = getById(id);
        if (!"DRAFT".equals(plan.getStatus()) && !"REJECTED".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "当前状态不允许提交");
        }
        plan.setStatus("SUBMITTED");
        plan.setSubmitTime(LocalDateTime.now());
        planMapper.updateById(plan);
    }

    @Transactional
    public void withdraw(Long id) {
        TeachingPlan plan = getById(id);
        if (!"SUBMITTED".equals(plan.getStatus())) {
            throw new BusinessException(ErrorCode.PLAN_STATUS_INVALID, "仅待审核状态可撤回");
        }
        plan.setStatus("DRAFT");
        planMapper.updateById(plan);
    }

    public List<TeachingPlanFile> getFiles(Long planId) {
        return fileMapper.selectList(new LambdaQueryWrapper<TeachingPlanFile>().eq(TeachingPlanFile::getPlanId, planId));
    }

    public IPage<TeachingPlan> pageForCollege(Page<TeachingPlan> page, Long collegeId, String status) {
        return planMapper.selectPage(page, new LambdaQueryWrapper<TeachingPlan>()
                .eq(status != null && !status.isEmpty(), TeachingPlan::getStatus, status)
                .orderByDesc(TeachingPlan::getCreatedAt));
    }

    public IPage<TeachingPlan> pageForOffice(Page<TeachingPlan> page, String status) {
        return planMapper.selectPage(page, new LambdaQueryWrapper<TeachingPlan>()
                .eq(status != null && !status.isEmpty(), TeachingPlan::getStatus, status)
                .in(TeachingPlan::getStatus, "COLLEGE_PASSED", "SUBMITTED")
                .orderByDesc(TeachingPlan::getCreatedAt));
    }

    @Transactional
    public TeachingPlan addFiles(Long planId, List<Long> fileIds) {
        TeachingPlan plan = getById(planId);
        for (Long fileId : fileIds) {
            TeachingPlanFile file = fileMapper.selectById(fileId);
            if (file != null) { file.setPlanId(planId); fileMapper.updateById(file); }
        }
        return plan;
    }

    private boolean canView(TeachingPlan plan) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<String> roles = SecurityUtils.getCurrentUserRoles();
        if (roles.contains("OFFICE")) return true;
        if (roles.contains("TEACHER") && plan.getTeacherId().equals(userId)) return true;
        return false;
    }
}
