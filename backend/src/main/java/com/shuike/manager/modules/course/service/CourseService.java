package com.shuike.manager.modules.course.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shuike.manager.modules.course.entity.Course;
import com.shuike.manager.modules.course.mapper.CourseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseMapper mapper;
    public IPage<Course> page(Page<Course> page, Long collegeId) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (collegeId != null) qw.eq(Course::getCollegeId, collegeId);
        qw.orderByAsc(Course::getCode);
        return mapper.selectPage(page, qw);
    }
    public Course getById(Long id) { return mapper.selectById(id); }
    public void create(Course c) { mapper.insert(c); }
    public void update(Long id, Course c) { c.setId(id); mapper.updateById(c); }
    public void delete(Long id) { mapper.deleteById(id); }
}
