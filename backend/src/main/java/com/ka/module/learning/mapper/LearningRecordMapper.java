package com.ka.module.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ka.module.learning.domain.LearningRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {
}

