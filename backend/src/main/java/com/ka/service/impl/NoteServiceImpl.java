package com.ka.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ka.common.BusinessException;
import com.ka.dto.NoteDTO;
import com.ka.entity.*;
import com.ka.mapper.*;
import com.ka.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final NoteTagMapper noteTagMapper;
    private final TagMapper tagMapper;
    private final CategoryMapper categoryMapper;
    private final JdbcTemplate jdbc;

    private void validateReferences(Long userId, NoteDTO dto) {
        if (dto.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dto.getCategoryId());
            if (category == null || !userId.equals(category.getUserId())) {
                throw new BusinessException(404, "Category not found");
            }
        }

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            List<Long> distinctTagIds = dto.getTagIds().stream().distinct().toList();
            List<Tag> tags = tagMapper.selectBatchIds(distinctTagIds);
            if (tags.size() != distinctTagIds.size()
                    || tags.stream().anyMatch(tag -> !userId.equals(tag.getUserId()))) {
                throw new BusinessException(404, "Tag not found");
            }
        }
    }

    public IPage<Note> page(Long userId, Long categoryId, String keyword, int page, int size) {
        Page<Note> p = new Page<>(page, size);
        return noteMapper.selectPageWithTags(p, userId, categoryId, keyword);
    }

    public Note getById(Long id, Long userId) {
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(userId)) {
            throw new BusinessException(404, "Note not found");
        }
        return note;
    }

    @Transactional
    public Note create(Long userId, NoteDTO dto) {
        validateReferences(userId, dto);
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setContentMd(dto.getContentMd());
        note.setCategoryId(dto.getCategoryId());
        note.setDifficultyLevel(dto.getDifficultyLevel());
        note.setStatus(1);
        note.setViewCount(0);
        noteMapper.insert(note);

        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                NoteTag nt = new NoteTag();
                nt.setNoteId(note.getId());
                nt.setTagId(tagId);
                noteTagMapper.insert(nt);
            }
        }
        return note;
    }

    @Transactional
    public Note update(Long userId, Long noteId, NoteDTO dto) {
        Note note = getById(noteId, userId);
        validateReferences(userId, dto);
        Integer versionNo = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version_no),0)+1 FROM note_versions WHERE note_id=? AND user_id=?",
                Integer.class, noteId, userId);
        jdbc.update("""
                INSERT INTO note_versions
                (note_id,user_id,title,content_md,category_id,difficulty_level,version_no)
                VALUES(?,?,?,?,?,?,?)
                """, noteId, userId, note.getTitle(), note.getContentMd(), note.getCategoryId(),
                note.getDifficultyLevel(), versionNo);
        note.setTitle(dto.getTitle());
        note.setContentMd(dto.getContentMd());
        note.setCategoryId(dto.getCategoryId());
        note.setDifficultyLevel(dto.getDifficultyLevel());
        noteMapper.updateById(note);

        // rebuild tags
        noteTagMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getNoteId, noteId));
        if (dto.getTagIds() != null) {
            for (Long tagId : dto.getTagIds()) {
                NoteTag nt = new NoteTag();
                nt.setNoteId(noteId);
                nt.setTagId(tagId);
                noteTagMapper.insert(nt);
            }
        }
        return note;
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        Note note = getById(noteId, userId);
        jdbc.update("""
                INSERT INTO note_trash
                (original_note_id,user_id,title,content_md,category_id,difficulty_level)
                VALUES(?,?,?,?,?,?)
                """, noteId, userId, note.getTitle(), note.getContentMd(),
                note.getCategoryId(), note.getDifficultyLevel());
        noteTagMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteTag>()
                .eq(NoteTag::getNoteId, noteId));
        noteMapper.deleteById(noteId);
    }

    public List<Tag> getTags(Long userId, Long noteId) {
        getById(noteId, userId);
        List<NoteTag> noteTags = noteTagMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteTag>()
                        .eq(NoteTag::getNoteId, noteId));
        if (noteTags.isEmpty()) return List.of();
        List<Long> tagIds = noteTags.stream().map(NoteTag::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds);
    }
}
