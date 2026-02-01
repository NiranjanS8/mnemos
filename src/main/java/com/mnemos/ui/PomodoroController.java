package com.mnemos.ui;

import com.mnemos.model.Task;
import com.mnemos.util.PomodoroTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;

public class PomodoroController {

    @FXML
    private Label stateLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button startButton;
    @FXML
    private Button pauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label pomodoroCountLabel;

    private PomodoroTimer timer;
    private Stage stage;
    private Task task;
    private int completedPomodoros = 0;
    private Runnable onPomodoroCompleteCallback;

    @FXML
    public void initialize() {
        timer = new PomodoroTimer();

        timer.setOnTickCallback(() -> Platform.runLater(this::updateDisplay));
        timer.setOnCompleteCallback(() -> Platform.runLater(this::handleTimerComplete));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setTask(Task task) {
        this.task = task;
        if (task != null) {
            stage.setTitle("Pomodoro - " + task.getTitle());
        }
    }

    public void setOnPomodoroCompleteCallback(Runnable callback) {
        this.onPomodoroCompleteCallback = callback;
    }

    @FXML
    private void handleStart() {
        if (timer.getCurrentState() == PomodoroTimer.TimerState.STOPPED) {
            timer.startWork();
            stateLabel.setText("Focus Time 🎯");
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);
        } else if (timer.isPaused()) {
            timer.resume();
            pauseButton.setText("Pause");
        }
    }

    @FXML
    private void handlePause() {
        if (timer.isPaused()) {
            timer.resume();
            pauseButton.setText("Pause");
        } else {
            timer.pause();
            pauseButton.setText("Resume");
        }
    }

    @FXML
    private void handleStop() {
        timer.stop();
        resetUI();
    }

    private void handleTimerComplete() {
        if (timer.getCurrentState() == PomodoroTimer.TimerState.WORK) {
            // Work session completed
            completedPomodoros++;
            updatePomodoroCount();

            if (onPomodoroCompleteCallback != null) {
                onPomodoroCompleteCallback.run();
            }

            // Start break
            timer.startBreak();
            stateLabel.setText("Break Time ☕");

        } else if (timer.getCurrentState() == PomodoroTimer.TimerState.BREAK) {
            // Break completed
            resetUI();
            stateLabel.setText("Ready for next session");
        }
    }

    private void updateDisplay() {
        timerLabel.setText(timer.getFormattedTime());

        // Update progress bar
        double totalSeconds;
        if (timer.getCurrentState() == PomodoroTimer.TimerState.WORK) {
            totalSeconds = timer.getWorkDurationMinutes() * 60.0;
        } else {
            totalSeconds = timer.getBreakDurationMinutes() * 60.0;
        }

        double progress = 1.0 - (timer.getRemainingSeconds() / totalSeconds);
        progressBar.setProgress(progress);
    }

    private void updatePomodoroCount() {
        pomodoroCountLabel.setText("🍅 " + completedPomodoros + " Pomodoro" +
                (completedPomodoros == 1 ? "" : "s") + " completed");
    }

    private void resetUI() {
        timerLabel.setText("25:00");
        progressBar.setProgress(0);
        stateLabel.setText("Ready to Focus");
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        pauseButton.setText("Pause");
    }

    public int getCompletedPomodoros() {
        return completedPomodoros;
    }
}
