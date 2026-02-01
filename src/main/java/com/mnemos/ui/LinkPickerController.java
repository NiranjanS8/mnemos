package com.mnemos.ui;

import com.mnemos.model.*;
import com.mnemos.model.Link.ItemType;
import com.mnemos.service.LinkService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LinkPickerController {
    @FXML
    private TextField searchField;
    @FXML
    private TabPane tabPane;
    @FXML
    private ListView<Task> tasksListView;
    @FXML
    private ListView<Note> notesListView;
    @FXML
    private ListView<FileReference> filesListView;

    private final LinkService linkService = new LinkService();
    private Stage stage;
    private ItemType selectedType;
    private Long selectedId;
    private boolean linkCreated = false;

    // Exclude these from picker (can't link to self)
    private ItemType excludeType;
    private Long excludeId;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setExclude(ItemType type, Long id) {
        this.excludeType = type;
        this.excludeId = id;
    }

    public boolean isLinkCreated() {
        return linkCreated;
    }

    public ItemType getSelectedType() {
        return selectedType;
    }

    public Long getSelectedId() {
        return selectedId;
    }

    @FXML
    public void initialize() {
        loadItems();
        setupCellFactories();
        setupSearch();
    }

    private void loadItems() {
        // Load tasks
        ObservableList<Task> tasks = FXCollections.observableArrayList(linkService.getAllTasks());
        if (excludeType == ItemType.TASK && excludeId != null) {
            tasks.removeIf(t -> t.getId().equals(excludeId));
        }
        tasksListView.setItems(tasks);

        // Load notes
        ObservableList<Note> notes = FXCollections.observableArrayList(linkService.getAllNotes());
        if (excludeType == ItemType.NOTE && excludeId != null) {
            notes.removeIf(n -> n.getId().equals(excludeId));
        }
        notesListView.setItems(notes);

        // Load files
        ObservableList<FileReference> files = FXCollections.observableArrayList(linkService.getAllFiles());
        if (excludeType == ItemType.FILE && excludeId != null) {
            files.removeIf(f -> f.getId().equals(excludeId));
        }
        filesListView.setItems(files);
    }

    private void setupCellFactories() {
        tasksListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("📋 " + item.getTitle());
                    setStyle("-fx-text-fill: white; -fx-padding: 5;");
                }
            }
        });

        notesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("📝 " + item.getTitle());
                    setStyle("-fx-text-fill: white; -fx-padding: 5;");
                }
            }
        });

        filesListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(FileReference item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("📎 " + item.getName());
                    setStyle("-fx-text-fill: white; -fx-padding: 5;");
                }
            }
        });
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.toLowerCase();

            // Filter tasks
            ObservableList<Task> allTasks = FXCollections.observableArrayList(linkService.getAllTasks());
            if (excludeType == ItemType.TASK && excludeId != null) {
                allTasks.removeIf(t -> t.getId().equals(excludeId));
            }
            if (!filter.isEmpty()) {
                allTasks.removeIf(t -> !t.getTitle().toLowerCase().contains(filter));
            }
            tasksListView.setItems(allTasks);

            // Filter notes
            ObservableList<Note> allNotes = FXCollections.observableArrayList(linkService.getAllNotes());
            if (excludeType == ItemType.NOTE && excludeId != null) {
                allNotes.removeIf(n -> n.getId().equals(excludeId));
            }
            if (!filter.isEmpty()) {
                allNotes.removeIf(n -> !n.getTitle().toLowerCase().contains(filter));
            }
            notesListView.setItems(allNotes);

            // Filter files
            ObservableList<FileReference> allFiles = FXCollections.observableArrayList(linkService.getAllFiles());
            if (excludeType == ItemType.FILE && excludeId != null) {
                allFiles.removeIf(f -> f.getId().equals(excludeId));
            }
            if (!filter.isEmpty()) {
                allFiles.removeIf(f -> !f.getName().toLowerCase().contains(filter));
            }
            filesListView.setItems(allFiles);
        });
    }

    @FXML
    private void handleLink() {
        // Get selected item based on current tab
        int tabIndex = tabPane.getSelectionModel().getSelectedIndex();

        switch (tabIndex) {
            case 0 -> { // Tasks
                Task task = tasksListView.getSelectionModel().getSelectedItem();
                if (task != null) {
                    selectedType = ItemType.TASK;
                    selectedId = task.getId();
                    linkCreated = true;
                }
            }
            case 1 -> { // Notes
                Note note = notesListView.getSelectionModel().getSelectedItem();
                if (note != null) {
                    selectedType = ItemType.NOTE;
                    selectedId = note.getId();
                    linkCreated = true;
                }
            }
            case 2 -> { // Files
                FileReference file = filesListView.getSelectionModel().getSelectedItem();
                if (file != null) {
                    selectedType = ItemType.FILE;
                    selectedId = file.getId();
                    linkCreated = true;
                }
            }
        }

        if (linkCreated && stage != null) {
            stage.close();
        }
    }

    @FXML
    private void handleCancel() {
        if (stage != null) {
            stage.close();
        }
    }
}
