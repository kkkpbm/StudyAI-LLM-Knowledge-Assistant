package com.ka.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ka.module.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

