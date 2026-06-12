package com.shuike.manager.modules.college.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuike.manager.modules.college.entity.College;
import com.shuike.manager.modules.college.mapper.CollegeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollegeService {
    private final CollegeMapper mapper;
    public List<College> list() { return mapper.selectList(new LambdaQueryWrapper<College>().orderByAsc(College::getSortOrder)); }
    public College getById(Long id) { return mapper.selectById(id); }
    public void create(College c) { mapper.insert(c); }
    public void update(Long id, College c) { c.setId(id); mapper.updateById(c); }
    public void delete(Long id) { mapper.deleteById(id); }
}
