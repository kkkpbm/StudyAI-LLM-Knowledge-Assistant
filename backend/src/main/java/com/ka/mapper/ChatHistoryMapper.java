package com.ka.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ka.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
