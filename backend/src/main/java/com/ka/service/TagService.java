package com.ka.service;

import com.ka.entity.Tag;

import java.util.List;

public interface TagService {
    List<Tag> list(Long userId);

    Tag create(Long userId, Tag tag);

    void delete(Long userId, Long id);
}
