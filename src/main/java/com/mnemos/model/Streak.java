package com.mnemos.model;

import java.time.LocalDate;

public class Streak {
    private Long id;
    private int currentStreak;
    private LocalDate lastCompletionDate;
    private int longestStreak;

    public Streak() {
        this.currentStreak = 0;
        this.longestStreak = 0;
    }

    public Streak(Long id, int currentStreak, LocalDate lastCompletionDate, int longestStreak) {
        this.id = id;
        this.currentStreak = currentStreak;
        this.lastCompletionDate = lastCompletionDate;
        this.longestStreak = longestStreak;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
        // Update longest streak if current exceeds it
        if (currentStreak > longestStreak) {
            this.longestStreak = currentStreak;
        }
    }

    public LocalDate getLastCompletionDate() {
        return lastCompletionDate;
    }

    public void setLastCompletionDate(LocalDate lastCompletionDate) {
        this.lastCompletionDate = lastCompletionDate;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }
}
