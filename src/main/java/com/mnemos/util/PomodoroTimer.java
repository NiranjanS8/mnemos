package com.mnemos.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class PomodoroTimer {
    public enum TimerState {
        WORK, BREAK, STOPPED
    }

    private static final int WORK_DURATION_MINUTES = 25;
    private static final int BREAK_DURATION_MINUTES = 5;

    private Timeline timeline;
    private int remainingSeconds;
    private TimerState currentState;
    private Runnable onTickCallback;
    private Runnable onCompleteCallback;
    private boolean isPaused;

    public PomodoroTimer() {
        this.currentState = TimerState.STOPPED;
        this.isPaused = false;
    }

    public void startWork() {
        currentState = TimerState.WORK;
        remainingSeconds = WORK_DURATION_MINUTES * 60;
        startTimer();
    }

    public void startBreak() {
        currentState = TimerState.BREAK;
        remainingSeconds = BREAK_DURATION_MINUTES * 60;
        startTimer();
    }

    private void startTimer() {
        if (timeline != null) {
            timeline.stop();
        }

        isPaused = false;
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (!isPaused) {
                remainingSeconds--;

                if (onTickCallback != null) {
                    onTickCallback.run();
                }

                if (remainingSeconds <= 0) {
                    stop();
                    if (onCompleteCallback != null) {
                        onCompleteCallback.run();
                    }
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        currentState = TimerState.STOPPED;
        remainingSeconds = 0;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public String getFormattedTime() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public TimerState getCurrentState() {
        return currentState;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setOnTickCallback(Runnable callback) {
        this.onTickCallback = callback;
    }

    public void setOnCompleteCallback(Runnable callback) {
        this.onCompleteCallback = callback;
    }

    public int getWorkDurationMinutes() {
        return WORK_DURATION_MINUTES;
    }

    public int getBreakDurationMinutes() {
        return BREAK_DURATION_MINUTES;
    }
}
