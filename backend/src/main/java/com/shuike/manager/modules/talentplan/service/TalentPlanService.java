package com.shuike.manager.modules.talentplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.modules.talentplan.entity.TalentCultivationPlan;
import com.shuike.manager.modules.talentplan.mapper.TalentCultivationPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TalentPlanService {
    private final TalentCultivationPlanMapper mapper;

    public IPage<TalentCultivationPlan> page(Page<TalentCultivationPlan> page, Long collegeId, String status) {
        LambdaQueryWrapper<TalentCultivationPlan> qw = new LambdaQueryWrapper<>();
        if (collegeId != null) qw.eq(TalentCultivationPlan::getCollegeId, collegeId);
        if (status != null && !status.isEmpty()) qw.eq(TalentCultivationPlan::getStatus, status);
        qw.orderByDesc(TalentCultivationPlan::getCreatedAt);
        return mapper.selectPage(page, qw);
    }

    public TalentCultivationPlan getById(Long id) {
        TalentCultivationPlan plan = mapper.selectById(id);
        if (plan == null) throw new BusinessException(ErrorCode.NOT_FOUND, "人培方案不存在");
        return plan;
    }

    public void create(TalentCultivationPlan plan) { mapper.insert(plan); }
    public void update(Long id, TalentCultivationPlan plan) { plan.setId(id); mapper.updateById(plan); }
    public void delete(Long id) { mapper.deleteById(id); }
}
