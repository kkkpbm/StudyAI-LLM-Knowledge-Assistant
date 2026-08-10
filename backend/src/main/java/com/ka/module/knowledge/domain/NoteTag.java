package com.ka.module.knowledge.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("note_tags")
public class NoteTag {
    /** 关联笔记 ID */
    private Long noteId;
    /** 关联标签 ID */
    private Long tagId;
}

