package com.ka.module.learning.service;

import com.ka.module.learning.domain.ReviewReminder;

import java.util.List;

public interface ReviewReminderService {
    List<ReviewReminder> getUpcomingReviews(Long userId, int limit);

    List<ReviewReminder> getAllReviews(Long userId);

    ReviewReminder create(Long userId, Long noteId);

    void completeReview(Long userId, Long id, int quality);

    void delete(Long userId, Long id);
}

