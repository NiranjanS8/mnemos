package com.mnemos.ui;

import com.mnemos.model.Task;
import com.mnemos.model.LinkedItem;
import com.mnemos.model.Link.ItemType;
import com.mnemos.service.TaskService;
import com.mnemos.service.StreakService;
import com.mnemos.service.LinkService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.mnemos.model.Task.Priority;
import com.mnemos.model.Task.Status;
import com.mnemos.model.Task.RecurrenceUnit;
import com.mnemos.App;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasksController {

    @FXML
    private TextField taskTitleField;
    @FXML
    private Label taskTitleLabel;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private ComboBox<Priority> priorityCombo;
    @FXML
    private ComboBox<Task.RecurrenceType> recurrenceCombo;
    @FXML
    private ListView<Task> tasksListView;

    @FXML
    private Label streakLabel;
    @FXML
    private VBox linkedItemsSection;
    @FXML
    private ListView<LinkedItem> linkedItemsListView;
    @FXML
    private javafx.scene.control.Button deleteAllDoneBtn;

    private final TaskService taskService = new TaskService();
    private final StreakService streakService = new StreakService();
    private final LinkService linkService = new LinkService();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    private javafx.stage.Stage activePomodoroDialog = null;
    private final ObservableList<LinkedItem> linkedItems = FXCollections.observableArrayList();
    private Status currentFilter = Status.PENDING;
    private Task selectedTask = null;

    // Custom repeat dialog state
    private int customRepeatInterval = 1;
    private RecurrenceUnit customRepeatUnit = RecurrenceUnit.DAYS;
    private String customRepeatDays = null;
    private String customEndType = "NEVER";
    private LocalDate customEndDate = null;
    private int customMaxOccurrences = 0;
    private Task.RecurrenceType previousRecurrenceSelection = Task.RecurrenceType.NONE;

    @FXML
    public void initialize() {
        priorityCombo.getItems().setAll(Priority.values());
        priorityCombo.getSelectionModel().select(Priority.MEDIUM);

        recurrenceCombo.getItems().setAll(Task.RecurrenceType.values());
        recurrenceCombo.getSelectionModel().select(Task.RecurrenceType.NONE);

        recurrenceCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == Task.RecurrenceType.CUSTOM) {
                previousRecurrenceSelection = oldVal != null ? oldVal : Task.RecurrenceType.NONE;
                showCustomRepeatDialog();
            }
        });

        tasksListView.setItems(tasks);
        tasksListView.setCellFactory(param -> new TaskListCell());

        linkedItemsListView.setItems(linkedItems);
        linkedItemsListView.setCellFactory(param -> new LinkedItemCell());

        Label placeholder = new Label("No linked items. Click '+ Add Link' to connect notes, files, or tasks.");
        placeholder.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-font-style: italic; -fx-wrap-text: true;");
        linkedItemsListView.setPlaceholder(placeholder);

        tasksListView.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            selectedTask = newTask;
            updateLinkedItemsSection();
        });

        loadTasks();
        updateStreakDisplay();

        taskTitleField.focusedProperty().addListener((obs, oldVal, newVal) -> updateFloatingLabel());
        taskTitleField.textProperty().addListener((obs, oldVal, newVal) -> updateFloatingLabel());
        updateFloatingLabel();
    }

    private void updateFloatingLabel() {
        boolean isFocused = taskTitleField.isFocused();
        boolean hasText = taskTitleField.getText() != null && !taskTitleField.getText().isEmpty();

        if (isFocused || hasText) {
            if (!taskTitleLabel.getStyleClass().contains("active")) {
                taskTitleLabel.getStyleClass().add("active");
            }
        } else {
            taskTitleLabel.getStyleClass().remove("active");
        }
    }

    private void updateLinkedItemsSection() {
        if (selectedTask != null) {
            linkedItems.clear();
            linkedItems.addAll(linkService.getLinkedItems(ItemType.TASK, selectedTask.getId()));
            linkedItemsSection.setVisible(true);
            linkedItemsSection.setManaged(true);
        } else {
            linkedItems.clear();
            linkedItemsSection.setVisible(false);
            linkedItemsSection.setManaged(false);
        }
    }

    private void loadTasks() {
        tasks.clear();
        if (currentFilter == null) {
            tasks.addAll(taskService.getAllTasks());
        } else {
            tasks.addAll(taskService.getTasksByStatus(currentFilter));
        }
        tasks.sort((t1, t2) -> {
            int p1 = getPriorityOrder(t1.getPriority());
            int p2 = getPriorityOrder(t2.getPriority());
            return Integer.compare(p1, p2);
        });
    }

    private int getPriorityOrder(Priority priority) {
        return switch (priority) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    @FXML
    private void handleAddTask() {
        String title = taskTitleField.getText();
        if (title != null && !title.isBlank()) {
            Task newTask = new Task(title, priorityCombo.getValue(), dueDatePicker.getValue());

            if (recurrenceCombo.getValue() != null) {
                newTask.setRecurrenceType(recurrenceCombo.getValue());
                if (newTask.getRecurrenceType() == Task.RecurrenceType.CUSTOM) {
                    newTask.setRecurrenceInterval(customRepeatInterval);
                    newTask.setRecurrenceUnit(customRepeatUnit);
                    newTask.setRecurrenceDays(customRepeatDays);
                    if ("ON_DATE".equals(customEndType)) {
                        newTask.setRecurrenceEndDate(customEndDate);
                    } else if ("AFTER".equals(customEndType)) {
                        newTask.setRecurrenceMaxOccurrences(customMaxOccurrences);
                    }
                } else if (newTask.getRecurrenceType() != Task.RecurrenceType.NONE) {
                    newTask.setRecurrenceInterval(1);
                }
            }

            taskService.saveTask(newTask);

            taskTitleField.clear();
            dueDatePicker.setValue(null);
            priorityCombo.getSelectionModel().select(Priority.MEDIUM);
            recurrenceCombo.getSelectionModel().select(Task.RecurrenceType.NONE);

            // Reset custom repeat state
            customRepeatInterval = 1;
            customRepeatUnit = RecurrenceUnit.DAYS;
            customRepeatDays = null;
            customEndType = "NEVER";
            customEndDate = null;
            customMaxOccurrences = 0;

            loadTasks();
        }
    }

    private void showCustomRepeatDialog() {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.initOwner(tasksListView.getScene().getWindow());

        // -- Card content --
        VBox card = new VBox(16);
        card.getStyleClass().add("custom-repeat-dialog");
        card.setPadding(new Insets(24));
        card.setMaxWidth(380);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label titleLabel = new Label("Custom Repeat");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        // == Repeat Every section ==
        Label repeatLabel = new Label("Repeat every");
        repeatLabel.getStyleClass().add("dialog-section-label");

        Spinner<Integer> intervalSpinner = new Spinner<>(1, 99, customRepeatInterval);
        intervalSpinner.setEditable(true);
        intervalSpinner.setPrefWidth(80);
        intervalSpinner.getStyleClass().add("custom-repeat-spinner");

        ComboBox<String> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll("Days", "Weeks", "Months");
        unitCombo.getSelectionModel().select(
                customRepeatUnit == RecurrenceUnit.WEEKS ? 1 : customRepeatUnit == RecurrenceUnit.MONTHS ? 2 : 0);
        unitCombo.getStyleClass().add("modern-combo-box");
        unitCombo.setPrefWidth(120);

        HBox repeatRow = new HBox(10, intervalSpinner, unitCombo);
        repeatRow.setAlignment(Pos.CENTER_LEFT);

        // == Weekday selector (shown only for Weeks) ==
        Label daysLabel = new Label("On these days");
        daysLabel.getStyleClass().add("dialog-section-label");

        String[] dayNames = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        String[] dayKeys = { "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN" };
        List<ToggleButton> dayButtons = new ArrayList<>();
        HBox daysRow = new HBox(6);
        daysRow.setAlignment(Pos.CENTER_LEFT);

        List<String> preselectedDays = customRepeatDays != null ? Arrays.asList(customRepeatDays.split(","))
                : new ArrayList<>();

        for (int i = 0; i < dayNames.length; i++) {
            ToggleButton tb = new ToggleButton(dayNames[i]);
            tb.getStyleClass().add("weekday-toggle");
            tb.setSelected(preselectedDays.contains(dayKeys[i]));
            dayButtons.add(tb);
            daysRow.getChildren().add(tb);
        }

        VBox weekdaySection = new VBox(6, daysLabel, daysRow);
        weekdaySection.setVisible(unitCombo.getSelectionModel().getSelectedIndex() == 1);
        weekdaySection.setManaged(unitCombo.getSelectionModel().getSelectedIndex() == 1);

        unitCombo.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isWeeks = "Weeks".equals(newV);
            weekdaySection.setVisible(isWeeks);
            weekdaySection.setManaged(isWeeks);
        });

        // == End condition ==
        Label endLabel = new Label("Ends");
        endLabel.getStyleClass().add("dialog-section-label");

        ToggleGroup endGroup = new ToggleGroup();

        RadioButton neverRadio = new RadioButton("Never");
        neverRadio.setToggleGroup(endGroup);
        neverRadio.setStyle("-fx-text-fill: #ccc;");

        RadioButton onDateRadio = new RadioButton("On date");
        onDateRadio.setToggleGroup(endGroup);
        onDateRadio.setStyle("-fx-text-fill: #ccc;");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.getStyleClass().add("modern-date-picker");
        endDatePicker.setPrefWidth(150);
        endDatePicker.setVisible(false);
        endDatePicker.setManaged(false);
        if (customEndDate != null)
            endDatePicker.setValue(customEndDate);

        HBox onDateRow = new HBox(8, onDateRadio, endDatePicker);
        onDateRow.setAlignment(Pos.CENTER_LEFT);

        RadioButton afterRadio = new RadioButton("After");
        afterRadio.setToggleGroup(endGroup);
        afterRadio.setStyle("-fx-text-fill: #ccc;");

        Spinner<Integer> occurrencesSpinner = new Spinner<>(1, 999,
                customMaxOccurrences > 0 ? customMaxOccurrences : 5);
        occurrencesSpinner.setEditable(true);
        occurrencesSpinner.setPrefWidth(80);
        occurrencesSpinner.getStyleClass().add("custom-repeat-spinner");
        occurrencesSpinner.setVisible(false);
        occurrencesSpinner.setManaged(false);

        Label occLabel = new Label("occurrences");
        occLabel.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");
        occLabel.setVisible(false);
        occLabel.setManaged(false);

        HBox afterRow = new HBox(8, afterRadio, occurrencesSpinner, occLabel);
        afterRow.setAlignment(Pos.CENTER_LEFT);

        // Pre-select end condition
        if ("ON_DATE".equals(customEndType))
            onDateRadio.setSelected(true);
        else if ("AFTER".equals(customEndType))
            afterRadio.setSelected(true);
        else
            neverRadio.setSelected(true);

        endGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            boolean isDate = newT == onDateRadio;
            boolean isAfter = newT == afterRadio;
            endDatePicker.setVisible(isDate);
            endDatePicker.setManaged(isDate);
            occurrencesSpinner.setVisible(isAfter);
            occurrencesSpinner.setManaged(isAfter);
            occLabel.setVisible(isAfter);
            occLabel.setManaged(isAfter);
        });
        // Trigger initial visibility
        if (onDateRadio.isSelected()) {
            endDatePicker.setVisible(true);
            endDatePicker.setManaged(true);
        }
        if (afterRadio.isSelected()) {
            occurrencesSpinner.setVisible(true);
            occurrencesSpinner.setManaged(true);
            occLabel.setVisible(true);
            occLabel.setManaged(true);
        }

        VBox endSection = new VBox(8, neverRadio, onDateRow, afterRow);

        // == Buttons ==
        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("add-task-btn");
        saveBtn.setOnAction(e -> {
            customRepeatInterval = intervalSpinner.getValue();
            String sel = unitCombo.getValue();
            customRepeatUnit = "Weeks".equals(sel) ? RecurrenceUnit.WEEKS
                    : "Months".equals(sel) ? RecurrenceUnit.MONTHS : RecurrenceUnit.DAYS;

            // Collect selected weekdays
            if (customRepeatUnit == RecurrenceUnit.WEEKS) {
                List<String> selected = new ArrayList<>();
                for (int i = 0; i < dayButtons.size(); i++) {
                    if (dayButtons.get(i).isSelected())
                        selected.add(dayKeys[i]);
                }
                customRepeatDays = selected.isEmpty() ? null : String.join(",", selected);
            } else {
                customRepeatDays = null;
            }

            // End condition
            Toggle endToggle = endGroup.getSelectedToggle();
            if (endToggle == onDateRadio) {
                customEndType = "ON_DATE";
                customEndDate = endDatePicker.getValue();
                customMaxOccurrences = 0;
            } else if (endToggle == afterRadio) {
                customEndType = "AFTER";
                customEndDate = null;
                customMaxOccurrences = occurrencesSpinner.getValue();
            } else {
                customEndType = "NEVER";
                customEndDate = null;
                customMaxOccurrences = 0;
            }

            dialogStage.close();
        });

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("icon-btn");
        cancelBtn.setOnAction(e -> {
            recurrenceCombo.getSelectionModel().select(previousRecurrenceSelection);
            dialogStage.close();
        });

        HBox buttonRow = new HBox(12, saveBtn, cancelBtn);
        buttonRow.setAlignment(Pos.CENTER_RIGHT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.1);");

        card.getChildren().addAll(titleLabel, repeatLabel, repeatRow, weekdaySection, endLabel, endSection, sep,
                buttonRow);

        // -- Overlay --
        StackPane overlay = new StackPane(card);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        overlay.setAlignment(Pos.CENTER);
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                recurrenceCombo.getSelectionModel().select(previousRecurrenceSelection);
                dialogStage.close();
            }
        });

        Scene scene = new Scene(overlay, 450, 420);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    @FXML
    private void handleFilterPending() {
        currentFilter = Status.PENDING;
        deleteAllDoneBtn.setVisible(false);
        deleteAllDoneBtn.setManaged(false);
        loadTasks();
    }

    @FXML
    private void handleFilterCompleted() {
        currentFilter = Status.COMPLETED;
        deleteAllDoneBtn.setVisible(true);
        deleteAllDoneBtn.setManaged(true);
        loadTasks();
    }

    @FXML
    private void handleDeleteAllDone() {
        if (tasks.isEmpty())
            return;

        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.initOwner(tasksListView.getScene().getWindow());
        alert.getDialogPane().getStylesheets()
                .add(getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("dialog-pane");
        alert.setTitle("Delete All Done Tasks");
        alert.setHeaderText("Delete all completed tasks?");
        alert.setContentText(
                "This will permanently delete " + tasks.size() + " completed task(s). This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                taskService.deleteAllCompletedTasks();
                loadTasks();
            }
        });
    }

    private class TaskListCell extends ListCell<Task> {
        private final javafx.scene.control.CheckBox statusCheckBox = new javafx.scene.control.CheckBox();
        private final javafx.scene.control.Label titleLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Label priorityLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Label dueDateLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Button reminderButton = new javafx.scene.control.Button();
        private final javafx.scene.control.Button timerButton = new javafx.scene.control.Button();
        private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button();
        private final HBox content = new HBox(8);

        public TaskListCell() {
            statusCheckBox.setCursor(javafx.scene.Cursor.HAND);
            statusCheckBox.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    Status oldStatus = item.getStatus();
                    Status newStatus = statusCheckBox.isSelected() ? Status.COMPLETED : Status.PENDING;

                    try {
                        item.setStatus(newStatus);
                        taskService.saveTask(item);

                        if (oldStatus != Status.COMPLETED && newStatus == Status.COMPLETED) {
                            streakService.onTaskCompleted();
                            updateStreakDisplay();
                        }
                    } catch (IllegalStateException ex) {
                        statusCheckBox.setSelected(false);
                        item.setStatus(oldStatus);

                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.WARNING);
                        alert.setTitle("Task Blocked");
                        alert.setHeaderText("Cannot complete task");
                        alert.setContentText(ex.getMessage());
                        alert.showAndWait();
                    }

                    updateItem(item, false);
                    if (currentFilter != null) {
                        loadTasks();
                    }
                }
            });

            titleLabel.setStyle("-fx-font-size: 13px;");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

            priorityLabel.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4;");

            dueDateLabel.setStyle(
                    "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4;");

            timerButton.setText("Timer");
            timerButton.setMinWidth(50);
            timerButton.setMaxWidth(50);
            timerButton.setMinHeight(26);
            timerButton.setMaxHeight(26);
            timerButton.setStyle(
                    "-fx-background-color: rgba(108, 140, 255, 0.25); -fx-text-fill: #6c8cff; -fx-cursor: hand; -fx-font-size: 10px; -fx-background-radius: 5; -fx-font-weight: bold;");
            timerButton.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    showPomodoroDialog(item);
                }
            });

            reminderButton.setText("🔔");
            reminderButton.setMinWidth(35);
            reminderButton.setMaxWidth(35);
            reminderButton.setMinHeight(26);
            reminderButton.setMaxHeight(26);
            reminderButton.setStyle(
                    "-fx-background-color: rgba(255, 189, 46, 0.25); -fx-text-fill: #ffbd2e; -fx-cursor: hand; -fx-font-size: 12px; -fx-background-radius: 5;");
            reminderButton.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    TasksController.this.showReminderDropdown(item, reminderButton);
                }
            });

            deleteButton.setText("Delete");
            deleteButton.setMinWidth(60);
            deleteButton.setMaxWidth(60);
            deleteButton.setMinHeight(26);
            deleteButton.setMaxHeight(26);
            deleteButton.setStyle(
                    "-fx-background-color: rgba(255, 107, 107, 0.25); -fx-text-fill: #ff6b6b; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 5; -fx-font-weight: bold;");
            deleteButton.setOnAction(e -> {
                Task item = getItem();
                if (item != null && item.getId() != null) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    alert.initOwner(getListView().getScene().getWindow());
                    alert.getDialogPane().getStylesheets()
                            .add(getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm());
                    alert.getDialogPane().getStyleClass().add("dialog-pane");
                    alert.setTitle("Delete Task");
                    alert.setHeaderText("Delete \"" + item.getTitle() + "\"?");
                    alert.setContentText("This action cannot be undone.");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == javafx.scene.control.ButtonType.OK) {
                            taskService.deleteTask(item.getId());
                            tasks.remove(item);
                        }
                    });
                }
            });

            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            content.getChildren().addAll(statusCheckBox, titleLabel, priorityLabel, dueDateLabel, reminderButton,
                    timerButton,
                    deleteButton);
        }

        @Override
        protected void updateItem(Task item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("");
            } else {
                statusCheckBox.setSelected(item.getStatus() == Status.COMPLETED);

                titleLabel.setText(item.getTitle());

                priorityLabel.setText(item.getPriority().toString());
                String priorityColor = switch (item.getPriority()) {
                    case HIGH -> "-fx-background-color: rgba(255, 95, 86, 0.2); -fx-text-fill: #ff6b6b;";
                    case MEDIUM -> "-fx-background-color: rgba(255, 189, 46, 0.2); -fx-text-fill: #ffbd2e;";
                    case LOW -> "-fx-background-color: rgba(90, 122, 255, 0.2); -fx-text-fill: #6c8cff;";
                };
                priorityLabel.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4;"
                                + priorityColor);

                if (item.getDueDate() != null) {
                    java.time.LocalDate today = java.time.LocalDate.now();
                    java.time.LocalDate dueDate = item.getDueDate();
                    long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);

                    String dateText;
                    String dateColor;

                    if (daysUntilDue < 0) {
                        dateText = "⚠ " + Math.abs(daysUntilDue) + "d overdue";
                        dateColor = "-fx-background-color: rgba(255, 95, 86, 0.3); -fx-text-fill: #ff6b6b;";
                    } else if (daysUntilDue == 0) {
                        // Due today
                        dateText = "📅 Today";
                        dateColor = "-fx-background-color: rgba(255, 189, 46, 0.3); -fx-text-fill: #ffbd2e;";
                    } else if (daysUntilDue == 1) {
                        dateText = "📅 Tomorrow";
                        dateColor = "-fx-background-color: rgba(90, 122, 255, 0.2); -fx-text-fill: #6c8cff;";
                    } else if (daysUntilDue <= 7) {
                        dateText = "📅 " + daysUntilDue + "d";
                        dateColor = "-fx-background-color: rgba(90, 122, 255, 0.2); -fx-text-fill: #6c8cff;";
                    } else {
                        dateText = "📅 " + dueDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"));
                        dateColor = "-fx-background-color: rgba(255, 255, 255, 0.05); -fx-text-fill: #999;";
                    }

                    dueDateLabel.setText(dateText);
                    dueDateLabel.setStyle(
                            "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4;"
                                    + dateColor);
                    dueDateLabel.setVisible(true);
                    dueDateLabel.setManaged(true);
                } else {
                    dueDateLabel.setVisible(false);
                    dueDateLabel.setManaged(false);
                }

                // Strike through if completed
                if (item.getStatus() == Status.COMPLETED) {
                    titleLabel.setStyle("-fx-font-size: 13px; -fx-strikethrough: true; -fx-text-fill: #888;");
                } else {
                    titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0e0e0;");
                }

                setGraphic(content);
                setText(null);
            }
        }
    }

    private void showPomodoroDialog(Task task) {
        // Prevent multiple dialogs - check if one is already showing
        if (activePomodoroDialog != null && activePomodoroDialog.isShowing()) {
            activePomodoroDialog.requestFocus();
            return;
        }

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/PomodoroDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            com.mnemos.ui.PomodoroController controller = loader.getController();

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            dialog.initOwner(tasksListView.getScene().getWindow());
            dialog.setResizable(false);
            dialog.setAlwaysOnTop(true);

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm());
            dialog.setScene(scene);

            controller.setStage(dialog);
            controller.setTask(task);
            controller.setOnPomodoroCompleteCallback(() -> {
                // Update task pomodoro count
                task.setPomodoroCount(task.getPomodoroCount() + 1);
                taskService.saveTask(task);
                loadTasks(); // Refresh list
            });

            // Track this dialog and clear reference when closed
            activePomodoroDialog = dialog;
            dialog.setOnHidden(e -> activePomodoroDialog = null);

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showReminderDropdown(Task task, javafx.scene.control.Button anchorButton) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/ReminderDropdown.fxml"));
            javafx.scene.Parent root = loader.load();
            ReminderDropdownController controller = loader.getController();

            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);

            // Apply stylesheet to the root node directly
            String cssPath = getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm();
            root.getStylesheets().add(cssPath);

            controller.setTask(task);
            controller.setPopup(popup);

            // Position below the anchor button
            javafx.geometry.Bounds bounds = anchorButton.localToScreen(anchorButton.getBoundsInLocal());
            if (bounds != null) {
                popup.show(anchorButton.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 5);
            } else {
                System.err.println("Could not get screen bounds for anchor button");
            }

        } catch (Exception e) {
            System.err.println("Error showing reminder dropdown: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateStreakDisplay() {
        if (streakLabel != null) {
            streakLabel.setText(streakService.getStreakDisplay());
        }
    }

    @FXML
    private void handleAddLink() {
        if (selectedTask == null)
            return;

        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/mnemos/ui/LinkPickerDialog.fxml"));
            Parent root = loader.load();
            LinkPickerController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initOwner(tasksListView.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.setAlwaysOnTop(true);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(App.class.getResource("/com/mnemos/ui/styles.css").toExternalForm());
            dialog.setScene(scene);

            // Center on parent
            dialog.setOnShown(event -> {
                javafx.stage.Window owner = tasksListView.getScene().getWindow();
                dialog.setX(owner.getX() + (owner.getWidth() - dialog.getWidth()) / 2);
                dialog.setY(owner.getY() + (owner.getHeight() - dialog.getHeight()) / 2);
            });

            controller.setStage(dialog);
            controller.setExclude(ItemType.TASK, selectedTask.getId());

            dialog.showAndWait();

            if (controller.isLinkCreated()) {
                linkService.linkItems(ItemType.TASK, selectedTask.getId(),
                        controller.getSelectedType(), controller.getSelectedId());
                updateLinkedItemsSection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class LinkedItemCell extends ListCell<LinkedItem> {
        private final HBox content = new HBox(8);
        private final Label iconLabel = new Label();
        private final Label titleLabel = new Label();
        private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button("✕");

        public LinkedItemCell() {
            iconLabel.setStyle("-fx-font-size: 14px;");
            titleLabel.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

            deleteButton.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #666; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 4;");
            deleteButton.setOnAction(e -> {
                LinkedItem item = getItem();
                if (item != null) {
                    linkService.unlinkItems(item.getLinkId());
                    updateLinkedItemsSection();
                }
            });

            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            content.getChildren().addAll(iconLabel, titleLabel, deleteButton);
        }

        @Override
        protected void updateItem(LinkedItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                iconLabel.setText(item.getIcon());
                titleLabel.setText(item.getTitle());
                setGraphic(content);
            }
        }
    }
}
