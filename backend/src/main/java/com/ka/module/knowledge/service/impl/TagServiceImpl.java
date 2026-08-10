package com.ka.module.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.module.knowledge.domain.Tag;
import com.ka.module.knowledge.mapper.TagMapper;
import com.ka.module.knowledge.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    public List<Tag> list(Long userId) {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getUserId, userId));
    }

    public Tag create(Long userId, Tag tag) {
        tag.setUserId(userId);
        tagMapper.insert(tag);
        return tag;
    }

    public void delete(Long userId, Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || !tag.getUserId().equals(userId)) {
            throw new BusinessException(404, "Tag not found");
        }
        tagMapper.deleteById(id);
    }
}

