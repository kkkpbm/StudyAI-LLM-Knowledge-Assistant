package com.ka.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ka.common.BusinessException;
import com.ka.entity.Note;
import com.ka.entity.ReviewReminder;
import com.ka.mapper.NoteMapper;
import com.ka.mapper.ReviewReminderMapper;
import com.ka.service.ReviewReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewReminderServiceImpl implements ReviewReminderService {
    private final ReviewReminderMapper reminderMapper;
    private final NoteMapper noteMapper;

    private ReviewReminder requireOwnedReminder(Long userId, Long id) {
        ReviewReminder reminder = reminderMapper.selectById(id);
        if (reminder == null || !userId.equals(reminder.getUserId())) {
            throw new BusinessException(404, "Review reminder not found");
        }
        return reminder;
    }

    private void requireOwnedNote(Long userId, Long noteId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null || !userId.equals(note.getUserId())) {
            throw new BusinessException(404, "Note not found");
        }
    }

    public List<ReviewReminder> getUpcomingReviews(Long userId, int limit) {
        return reminderMapper.selectList(new LambdaQueryWrapper<ReviewReminder>()
                .eq(ReviewReminder::getUserId, userId)
                .le(ReviewReminder::getNextReviewAt, LocalDate.now())
                .orderByAsc(ReviewReminder::getNextReviewAt)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
    }

    public List<ReviewReminder> getAllReviews(Long userId) {
        return reminderMapper.selectList(new LambdaQueryWrapper<ReviewReminder>()
                .eq(ReviewReminder::getUserId, userId)
                .orderByAsc(ReviewReminder::getNextReviewAt));
    }

    public ReviewReminder create(Long userId, Long noteId) {
        requireOwnedNote(userId, noteId);
        ReviewReminder reminder = new ReviewReminder();
        reminder.setUserId(userId);
        reminder.setNoteId(noteId);
        reminder.setNextReviewAt(LocalDate.now().plusDays(1));
        reminder.setIntervalDays(1);
        reminder.setEaseFactor(2.5);
        reminder.setRepetitionCount(0);
        reminderMapper.insert(reminder);
        return reminder;
    }

    public void completeReview(Long userId, Long id, int quality) {
        ReviewReminder reminder = requireOwnedReminder(userId, id);
        if (quality < 0 || quality > 5) {
            throw new BusinessException(400, "Quality must be between 0 and 5");
        }

        int repetitions = reminder.getRepetitionCount() == null ? 0 : reminder.getRepetitionCount();
        int interval = reminder.getIntervalDays() == null ? 1 : reminder.getIntervalDays();
        double ease = reminder.getEaseFactor() == null ? 2.5 : reminder.getEaseFactor();
        int nextInterval;

        if (quality < 3) {
            repetitions = 0;
            nextInterval = 1;
        } else {
            nextInterval = repetitions == 0 ? 1
                    : repetitions == 1 ? 6
                    : Math.max(1, (int) Math.round(interval * ease));
            ease = Math.max(1.3,
                    ease + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
            repetitions++;
        }

        ReviewReminder update = new ReviewReminder();
        update.setId(id);
        update.setNextReviewAt(LocalDate.now().plusDays(nextInterval));
        update.setIntervalDays(nextInterval);
        update.setRepetitionCount(repetitions);
        update.setEaseFactor(ease);
        reminderMapper.updateById(update);
    }

    public void delete(Long userId, Long id) {
        requireOwnedReminder(userId, id);
        reminderMapper.deleteById(id);
    }
}
