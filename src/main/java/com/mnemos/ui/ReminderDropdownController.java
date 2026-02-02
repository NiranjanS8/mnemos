package com.mnemos.ui;

import com.mnemos.model.Task;
import com.mnemos.service.ReminderService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Controller for the Reminder dropdown popup.
 * Uses LocalDateTime as single source of truth for the selected reminder time.
 */
public class ReminderDropdownController {

    @FXML
    private VBox dropdownPane;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Spinner<Integer> hourSpinner;
    @FXML
    private Spinner<Integer> minuteSpinner;

    private Task task;
    private Popup popup;
    private ReminderService reminderService;

    // Single source of truth for the reminder time
    private LocalDateTime selectedDateTime;

    @FXML
    public void initialize() {
        reminderService = ReminderService.getInstance();

        // Initialize with a sensible default (next hour)
        LocalDateTime now = LocalDateTime.now();
        selectedDateTime = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);

        // Setup date picker with today as default
        datePicker.setValue(selectedDateTime.toLocalDate());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateModelFromUI();
            }
        });

        // Setup hour spinner (24-hour format, wrapping)
        SpinnerValueFactory.IntegerSpinnerValueFactory hourFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 23, selectedDateTime.getHour());
        hourFactory.setWrapAround(true);
        hourSpinner.setValueFactory(hourFactory);
        hourSpinner.setEditable(true);
        setupSpinnerValidation(hourSpinner, 0, 23);
        hourSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateModelFromUI();
            }
        });

        // Setup minute spinner (0-59, step 5, wrapping)
        SpinnerValueFactory.IntegerSpinnerValueFactory minuteFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, 59, selectedDateTime.getMinute(), 5);
        minuteFactory.setWrapAround(true);
        minuteSpinner.setValueFactory(minuteFactory);
        minuteSpinner.setEditable(true);
        setupSpinnerValidation(minuteSpinner, 0, 59);
        minuteSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updateModelFromUI();
            }
        });

        System.out.println("ReminderDropdownController initialized: " + selectedDateTime);
    }

    /**
     * Setup text formatter to validate spinner input.
     */
    private void setupSpinnerValidation(Spinner<Integer> spinner, int min, int max) {
        TextField editor = spinner.getEditor();

        // Use a StringConverter that handles invalid input gracefully
        spinner.getValueFactory().setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : String.format("%02d", value);
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return spinner.getValue(); // Keep current value
                }
                try {
                    int val = Integer.parseInt(text.trim());
                    return Math.max(min, Math.min(max, val)); // Clamp to valid range
                } catch (NumberFormatException e) {
                    return spinner.getValue(); // Keep current value on parse error
                }
            }
        });

        // Commit value on focus lost
        editor.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                spinner.increment(0); // Force commit of editor text
            }
        });
    }

    /**
     * Update the model (selectedDateTime) from UI controls.
     */
    private void updateModelFromUI() {
        LocalDate date = datePicker.getValue();
        Integer hour = hourSpinner.getValue();
        Integer minute = minuteSpinner.getValue();

        if (date != null && hour != null && minute != null) {
            selectedDateTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        }
    }

    /**
     * Update UI controls from the model (selectedDateTime).
     */
    private void updateUIFromModel() {
        datePicker.setValue(selectedDateTime.toLocalDate());
        hourSpinner.getValueFactory().setValue(selectedDateTime.getHour());
        minuteSpinner.getValueFactory().setValue(selectedDateTime.getMinute());
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public void setPopup(Popup popup) {
        this.popup = popup;
    }

    @FXML
    private void handleLaterToday() {
        if (task != null) {
            LocalDateTime reminderTime = reminderService.calculateLaterToday();
            setReminderAndClose(reminderTime);
        }
    }

    @FXML
    private void handleTomorrowMorning() {
        if (task != null) {
            LocalDateTime reminderTime = reminderService.calculateTomorrowMorning();
            setReminderAndClose(reminderTime);
        }
    }

    @FXML
    private void handleCustomReminder() {
        if (task == null) {
            closePopup();
            return;
        }

        // Force commit any pending edits
        hourSpinner.increment(0);
        minuteSpinner.increment(0);
        updateModelFromUI();

        // Validate the selected time is in the future
        if (selectedDateTime.isAfter(LocalDateTime.now())) {
            setReminderAndClose(selectedDateTime);
        } else {
            System.out.println("Cannot set reminder in the past: " + selectedDateTime);
            // Don't close - let user correct the time
        }
    }

    /**
     * Set the reminder and close the popup.
     */
    private void setReminderAndClose(LocalDateTime reminderTime) {
        reminderService.setReminder(task, reminderTime);
        System.out.println("Reminder set for: " + reminderTime);
        closePopup();
    }

    @FXML
    private void handleClose() {
        closePopup();
    }

    private void closePopup() {
        if (popup != null) {
            popup.hide();
        }
    }
}
