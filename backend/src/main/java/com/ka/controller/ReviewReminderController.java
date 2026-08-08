package com.ka.controller;

import com.ka.common.Result;
import com.ka.entity.ReviewReminder;
import com.ka.service.ReviewReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewReminderController {

    private final ReviewReminderService reviewService;

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping
    public Result<List<ReviewReminder>> list(Authentication auth) {
        return Result.ok(reviewService.getAllReviews(getUserId(auth)));
    }

    @GetMapping("/upcoming")
    public Result<List<ReviewReminder>> upcoming(Authentication auth,
                                                  @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(reviewService.getUpcomingReviews(getUserId(auth), limit));
    }

    @PostMapping
    public Result<ReviewReminder> create(Authentication auth, @RequestParam Long noteId) {
        return Result.ok(reviewService.create(getUserId(auth), noteId));
    }

    @PutMapping("/{id}/complete")
    public Result<?> complete(Authentication auth, @PathVariable Long id,
                              @RequestParam(defaultValue = "4") int quality) {
        reviewService.completeReview(getUserId(auth), id, quality);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(Authentication auth, @PathVariable Long id) {
        reviewService.delete(getUserId(auth), id);
        return Result.ok();
    }
}
