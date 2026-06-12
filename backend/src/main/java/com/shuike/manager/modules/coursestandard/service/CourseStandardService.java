package com.shuike.manager.modules.coursestandard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.common.exception.BusinessException;
import com.shuike.manager.common.exception.ErrorCode;
import com.shuike.manager.modules.coursestandard.entity.CourseStandard;
import com.shuike.manager.modules.coursestandard.mapper.CourseStandardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseStandardService {
    private final CourseStandardMapper mapper;
    public IPage<CourseStandard> page(Page<CourseStandard> page, Long courseId, Long deanId) {
        LambdaQueryWrapper<CourseStandard> qw = new LambdaQueryWrapper<>();
        if (courseId != null) qw.eq(CourseStandard::getCourseId, courseId);
        if (deanId != null) qw.eq(CourseStandard::getDeanId, deanId);
        qw.orderByDesc(CourseStandard::getCreatedAt);
        return mapper.selectPage(page, qw);
    }
    public CourseStandard getById(Long id) {
        CourseStandard cs = mapper.selectById(id);
        if (cs == null) throw new BusinessException(ErrorCode.NOT_FOUND, "课程标准不存在");
        return cs;
    }
    public void create(CourseStandard cs) { mapper.insert(cs); }
    public void update(Long id, CourseStandard cs) { cs.setId(id); mapper.updateById(cs); }
    public void delete(Long id) { mapper.deleteById(id); }
}
