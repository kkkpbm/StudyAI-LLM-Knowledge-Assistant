"""
SM-2 Spaced Repetition Algorithm implementation.

Parameters:
- quality: user self-rating 0-5
- ease_factor: starts at 2.5, adjusted based on performance
- interval: days until next review

Returns: (next_interval_days, new_ease_factor)
"""


def calculate_next_review(quality: int, interval_days: int, ease_factor: float) -> tuple[int, float, int]:
    if quality < 0 or quality > 5:
        raise ValueError("Quality must be 0-5")

    if quality >= 3:
        if interval_days == 1:
            next_interval = 1
        elif interval_days == 2:
            next_interval = 6
        else:
            next_interval = round(interval_days * ease_factor)

        new_ef = ease_factor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
        if new_ef < 1.3:
            new_ef = 1.3
    else:
        next_interval = 1
        new_ef = ease_factor

    repetition_count_increment = 1 if quality >= 3 else -1
    return next_interval, new_ef, repetition_count_increment
