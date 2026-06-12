package com.shuike.manager.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shuike.manager.modules.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
