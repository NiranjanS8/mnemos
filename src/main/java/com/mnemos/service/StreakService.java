package com.mnemos.service;

import com.mnemos.model.Streak;
import com.mnemos.repository.StreakRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StreakService {
    private final StreakRepository streakRepository;

    public StreakService() {
        this.streakRepository = new StreakRepository();
    }

    /**
     * Called when a task is completed. Updates the streak based on the completion
     * date.
     */
    public void onTaskCompleted() {
        Streak streak = streakRepository.getCurrentStreak();
        LocalDate today = LocalDate.now();
        LocalDate lastDate = streak.getLastCompletionDate();

        if (lastDate == null) {
            // First ever completion
            streak.setCurrentStreak(1);
            streak.setLastCompletionDate(today);
        } else if (lastDate.equals(today)) {
            // Already completed a task today, no change needed
            return;
        } else {
            long daysBetween = ChronoUnit.DAYS.between(lastDate, today);

            if (daysBetween == 1) {
                // Consecutive day - increment streak
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                streak.setLastCompletionDate(today);
            } else if (daysBetween > 1) {
                // Missed days - reset streak
                streak.setCurrentStreak(1);
                streak.setLastCompletionDate(today);
            }
        }

        streakRepository.updateStreak(streak);
    }

    /**
     * Get the current streak, accounting for missed days.
     */
    public Streak getCurrentStreak() {
        Streak streak = streakRepository.getCurrentStreak();
        LocalDate today = LocalDate.now();
        LocalDate lastDate = streak.getLastCompletionDate();

        // Check if streak should be reset due to missed days
        if (lastDate != null) {
            long daysBetween = ChronoUnit.DAYS.between(lastDate, today);

            // If more than 1 day has passed since last completion, reset streak
            if (daysBetween > 1) {
                streak.setCurrentStreak(0);
            }
        }

        return streak;
    }

    /**
     * Get formatted streak display string.
     */
    public String getStreakDisplay() {
        Streak streak = getCurrentStreak();
        int current = streak.getCurrentStreak();

        if (current == 0) {
            return "🔥 0 Day Streak";
        } else if (current == 1) {
            return "🔥 1 Day Streak";
        } else {
            return "🔥 " + current + " Day Streak";
        }
    }
}
