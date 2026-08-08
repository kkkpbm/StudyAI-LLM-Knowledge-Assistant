package com.ka.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ka.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
