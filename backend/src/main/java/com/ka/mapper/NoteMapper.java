package com.ka.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ka.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {
    IPage<Note> selectPageWithTags(Page<Note> page, @Param("userId") Long userId,
                                   @Param("categoryId") Long categoryId,
                                   @Param("keyword") String keyword);
}
