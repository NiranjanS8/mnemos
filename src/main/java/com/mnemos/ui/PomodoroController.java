package com.mnemos.ui;

import com.mnemos.model.Task;
import com.mnemos.util.PomodoroTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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
    @FXML
    private Button closeButton;
    @FXML
    private VBox contentPane;
    @FXML
    private VBox settingsPane;
    @FXML
    private Spinner<Integer> workSpinner;
    @FXML
    private Spinner<Integer> breakSpinner;

    private PomodoroTimer timer;
    private Stage stage;
    private Task task;
    private int completedPomodoros = 0;
    private Runnable onPomodoroCompleteCallback;
    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        timer = new PomodoroTimer();
        timer.setOnTickCallback(() -> Platform.runLater(this::updateDisplay));
        timer.setOnCompleteCallback(() -> Platform.runLater(this::handleTimerComplete));

        // Initialize work spinner (5-120 minutes, default 25)
        SpinnerValueFactory.IntegerSpinnerValueFactory workFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                5, 120, 25, 5);
        workSpinner.setValueFactory(workFactory);
        workSpinner.setEditable(true);
        workSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                timer.setWorkDuration(newVal);
                updateTimerDisplay();
            }
        });

        // Initialize break spinner (1-30 minutes, default 5)
        SpinnerValueFactory.IntegerSpinnerValueFactory breakFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 30, 5, 1);
        breakSpinner.setValueFactory(breakFactory);
        breakSpinner.setEditable(true);
        breakSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                timer.setBreakDuration(newVal);
            }
        });

        // Apply dark styling to spinners
        styleSpinner(workSpinner);
        styleSpinner(breakSpinner);
    }

    private void styleSpinner(Spinner<?> spinner) {
        spinner.getEditor().setStyle(
                "-fx-background-color: rgba(255,255,255,0.1); " +
                        "-fx-text-fill: #e0e0e0; " +
                        "-fx-background-radius: 5;");
    }

    private void updateTimerDisplay() {
        if (timer.getCurrentState() == PomodoroTimer.TimerState.STOPPED) {
            int minutes = workSpinner.getValue();
            timerLabel.setText(String.format("%02d:00", minutes));
        }
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        // Enable window dragging
        if (contentPane != null) {
            contentPane.setOnMousePressed(event -> {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            });
            contentPane.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            });
        }
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

    // Preset handlers
    @FXML
    private void handlePreset25() {
        workSpinner.getValueFactory().setValue(25);
        breakSpinner.getValueFactory().setValue(5);
    }

    @FXML
    private void handlePreset50() {
        workSpinner.getValueFactory().setValue(50);
        breakSpinner.getValueFactory().setValue(10);
    }

    @FXML
    private void handlePreset90() {
        workSpinner.getValueFactory().setValue(90);
        breakSpinner.getValueFactory().setValue(20);
    }

    @FXML
    private void handleStart() {
        if (timer.getCurrentState() == PomodoroTimer.TimerState.STOPPED) {
            // Apply spinner values to timer
            timer.setWorkDuration(workSpinner.getValue());
            timer.setBreakDuration(breakSpinner.getValue());

            timer.startWork();
            stateLabel.setText("Focus Time 🎯");
            startButton.setDisable(true);
            pauseButton.setDisable(false);
            stopButton.setDisable(false);

            // Hide settings while running
            settingsPane.setVisible(false);
            settingsPane.setManaged(false);

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
        int minutes = workSpinner.getValue();
        timerLabel.setText(String.format("%02d:00", minutes));
        progressBar.setProgress(0);
        stateLabel.setText("Ready to Focus");
        startButton.setDisable(false);
        pauseButton.setDisable(true);
        stopButton.setDisable(true);
        pauseButton.setText("Pause");

        // Show settings again
        settingsPane.setVisible(true);
        settingsPane.setManaged(true);
    }

    public int getCompletedPomodoros() {
        return completedPomodoros;
    }

    @FXML
    private void handleClose() {
        if (timer != null) {
            timer.stop();
        }
        if (stage != null) {
            stage.close();
        }
    }
}
