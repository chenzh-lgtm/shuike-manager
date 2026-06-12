package com.shuike.manager.modules.semester.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shuike.manager.modules.semester.entity.Semester;
import com.shuike.manager.modules.semester.mapper.SemesterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {
    private final SemesterMapper mapper;
    public List<Semester> list() { return mapper.selectList(new LambdaQueryWrapper<Semester>().orderByDesc(Semester::getStartDate)); }
    public Semester getById(Long id) { return mapper.selectById(id); }
    public void create(Semester s) { mapper.insert(s); }
    public void update(Long id, Semester s) { s.setId(id); mapper.updateById(s); }
    public void delete(Long id) { mapper.deleteById(id); }
    @Transactional
    public void activate(Long id) {
        Semester reset = new Semester();
        reset.setIsActive(0);
        mapper.update(reset, new LambdaUpdateWrapper<Semester>().set(Semester::getIsActive, 0));
        Semester s = new Semester(); s.setId(id); s.setIsActive(1); mapper.updateById(s);
    }
}
