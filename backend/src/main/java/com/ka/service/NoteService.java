package com.ka.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ka.dto.NoteDTO;
import com.ka.entity.Note;
import com.ka.entity.Tag;

import java.util.List;

public interface NoteService {
    IPage<Note> page(Long userId, Long categoryId, String keyword, int page, int size);

    Note getById(Long id, Long userId);

    Note create(Long userId, NoteDTO dto);

    Note update(Long userId, Long noteId, NoteDTO dto);

    void delete(Long userId, Long noteId);

    List<Tag> getTags(Long userId, Long noteId);
}
