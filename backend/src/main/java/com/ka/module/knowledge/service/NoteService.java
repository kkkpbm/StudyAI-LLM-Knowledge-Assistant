package com.ka.module.knowledge.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ka.module.knowledge.domain.NoteDTO;
import com.ka.module.knowledge.domain.Note;
import com.ka.module.knowledge.domain.Tag;

import java.util.List;

public interface NoteService {
    IPage<Note> page(Long userId, Long categoryId, String keyword, int page, int size);

    Note getById(Long id, Long userId);

    Note create(Long userId, NoteDTO dto);

    Note update(Long userId, Long noteId, NoteDTO dto);

    void delete(Long userId, Long noteId);

    List<Tag> getTags(Long userId, Long noteId);
}

