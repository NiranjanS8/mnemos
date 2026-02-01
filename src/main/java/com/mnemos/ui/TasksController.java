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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.mnemos.model.Task.Priority;
import com.mnemos.model.Task.Status;
import com.mnemos.App;

public class TasksController {

    @FXML
    private TextField taskTitleField;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private ComboBox<Priority> priorityCombo;
    @FXML
    private ListView<Task> tasksListView;

    @FXML
    private Label streakLabel;
    @FXML
    private VBox linkedItemsSection;
    @FXML
    private ListView<LinkedItem> linkedItemsListView;

    private final TaskService taskService = new TaskService();
    private final StreakService streakService = new StreakService();
    private final LinkService linkService = new LinkService();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    private final ObservableList<LinkedItem> linkedItems = FXCollections.observableArrayList();
    private Status currentFilter = null; // null = ALL
    private Task selectedTask = null;

    @FXML
    public void initialize() {
        priorityCombo.getItems().setAll(Priority.values());
        priorityCombo.getSelectionModel().select(Priority.MEDIUM);

        tasksListView.setItems(tasks);
        tasksListView.setCellFactory(param -> new TaskListCell());

        // Setup linked items list
        linkedItemsListView.setItems(linkedItems);
        linkedItemsListView.setCellFactory(param -> new LinkedItemCell());

        // Listen for task selection to show linked items
        tasksListView.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
            selectedTask = newTask;
            updateLinkedItemsSection();
        });

        loadTasks();
        updateStreakDisplay();
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
        // Sort by priority: HIGH -> MEDIUM -> LOW
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
            taskService.saveTask(newTask);

            taskTitleField.clear();
            dueDatePicker.setValue(null);
            priorityCombo.getSelectionModel().select(Priority.MEDIUM);

            loadTasks();
        }
    }

    @FXML
    private void handleFilterAll() {
        currentFilter = null;
        loadTasks();
    }

    @FXML
    private void handleFilterPending() {
        currentFilter = Status.PENDING;
        loadTasks();
    }

    @FXML
    private void handleFilterCompleted() {
        currentFilter = Status.COMPLETED;
        loadTasks();
    }

    private class TaskListCell extends ListCell<Task> {
        private final javafx.scene.control.CheckBox statusCheckBox = new javafx.scene.control.CheckBox();
        private final javafx.scene.control.Label titleLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Label priorityLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Label dueDateLabel = new javafx.scene.control.Label();
        private final javafx.scene.control.Button timerButton = new javafx.scene.control.Button();
        private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button();
        private final HBox content = new HBox(8);

        public TaskListCell() {
            // Checkbox for status
            statusCheckBox.setCursor(javafx.scene.Cursor.HAND);
            statusCheckBox.setOnAction(e -> {
                Task item = getItem();
                if (item != null) {
                    Status oldStatus = item.getStatus();
                    Status newStatus = statusCheckBox.isSelected() ? Status.COMPLETED : Status.PENDING;
                    item.setStatus(newStatus);
                    taskService.saveTask(item);

                    // Update streak if task was just completed
                    if (oldStatus != Status.COMPLETED && newStatus == Status.COMPLETED) {
                        streakService.onTaskCompleted();
                        updateStreakDisplay();
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

            // Timer button - text based for visibility
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

            // Delete button - text based for visibility
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
                    // Confirmation dialog
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
            content.getChildren().addAll(statusCheckBox, titleLabel, priorityLabel, dueDateLabel, timerButton,
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
                // Update checkbox state
                statusCheckBox.setSelected(item.getStatus() == Status.COMPLETED);

                titleLabel.setText(item.getTitle());

                // Set priority label with color
                priorityLabel.setText(item.getPriority().toString());
                String priorityColor = switch (item.getPriority()) {
                    case HIGH -> "-fx-background-color: rgba(255, 95, 86, 0.2); -fx-text-fill: #ff6b6b;";
                    case MEDIUM -> "-fx-background-color: rgba(255, 189, 46, 0.2); -fx-text-fill: #ffbd2e;";
                    case LOW -> "-fx-background-color: rgba(90, 122, 255, 0.2); -fx-text-fill: #6c8cff;";
                };
                priorityLabel.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4;"
                                + priorityColor);

                // Display due date with color coding
                if (item.getDueDate() != null) {
                    java.time.LocalDate today = java.time.LocalDate.now();
                    java.time.LocalDate dueDate = item.getDueDate();
                    long daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);

                    String dateText;
                    String dateColor;

                    if (daysUntilDue < 0) {
                        // Overdue
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
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/PomodoroDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            com.mnemos.ui.PomodoroController controller = loader.getController();

            javafx.stage.Stage dialog = new javafx.stage.Stage();
            dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialog.initOwner(tasksListView.getScene().getWindow());
            dialog.setTitle("Pomodoro Timer - " + task.getTitle());

            javafx.scene.Scene scene = new javafx.scene.Scene(root);
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

            dialog.show();
        } catch (Exception e) {
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
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(App.class.getResource("/com/mnemos/ui/styles.css").toExternalForm());
            dialog.setScene(scene);

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
