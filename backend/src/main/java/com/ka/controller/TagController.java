package com.ka.controller;

import com.ka.common.Result;
import com.ka.entity.Tag;
import com.ka.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<Tag>> list(Authentication auth) {
        return Result.ok(tagService.list(getUserId(auth)));
    }

    @PostMapping
    public Result<Tag> create(Authentication auth, @RequestBody Tag tag) {
        return Result.ok(tagService.create(getUserId(auth), tag));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        tagService.delete(getUserId(auth), id);
        return Result.ok();
    }
}
