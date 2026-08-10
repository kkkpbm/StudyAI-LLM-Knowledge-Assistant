package com.ka.module.knowledge.service;

import com.ka.module.knowledge.domain.Tag;

import java.util.List;

public interface TagService {
    List<Tag> list(Long userId);

    Tag create(Long userId, Tag tag);

    void delete(Long userId, Long id);
}

