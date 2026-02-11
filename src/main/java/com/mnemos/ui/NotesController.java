package com.mnemos.ui;

import com.mnemos.model.Note;
import com.mnemos.model.LinkedItem;
import com.mnemos.model.Link.ItemType;
import com.mnemos.service.NoteService;
import com.mnemos.service.LinkService;
import com.mnemos.App;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.Timer;
import java.util.TimerTask;

public class NotesController {

    @FXML
    private VBox listViewbox;
    @FXML
    private VBox editorViewbox;
    @FXML
    private ListView<Note> notesListView;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;
    @FXML
    private Label statusLabel;
    @FXML
    private VBox linkedItemsSection;
    @FXML
    private ListView<LinkedItem> linkedItemsListView;

    private final NoteService noteService = new NoteService();
    private final LinkService linkService = new LinkService();
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final ObservableList<LinkedItem> linkedItems = FXCollections.observableArrayList();
    private Note currentNote;
    private Timer autoSaveTimer;

    @FXML
    public void initialize() {
        notesListView.setItems(notes);
        notesListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Note item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getTitle());
                }
            }
        });

        notesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                editNote(newVal);
            }
        });

        linkedItemsListView.setItems(linkedItems);
        linkedItemsListView.setCellFactory(param -> new LinkedItemCell());

        loadNotes();
        setupAutoSave();
    }

    private void loadNotes() {
        notes.setAll(noteService.getAllNotes());
    }

    @FXML
    private void handleNewNote() {
        currentNote = new Note("New Note", "");
        currentNote = noteService.saveNote(currentNote);
        notes.add(0, currentNote);
        editNote(currentNote);
    }

    private void editNote(Note note) {
        currentNote = note;
        titleField.setText(note.getTitle());
        contentArea.setText(note.getContent());

        listViewbox.setVisible(false);
        editorViewbox.setVisible(true);
        statusLabel.setText("");
        updateLinkedItemsSection();
    }

    private void updateLinkedItemsSection() {
        if (currentNote != null && currentNote.getId() != null) {
            linkedItems.clear();
            linkedItems.addAll(linkService.getLinkedItems(ItemType.NOTE, currentNote.getId()));
        } else {
            linkedItems.clear();
        }
    }

    @FXML
    private void handleBack() {
        saveCurrentNote();
        editorViewbox.setVisible(false);
        listViewbox.setVisible(true);
        notesListView.getSelectionModel().clearSelection();
        notesListView.refresh();
        loadNotes();
    }

    @FXML
    private void handleDelete() {
        if (currentNote != null && currentNote.getId() != null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            alert.initOwner(titleField.getScene().getWindow());
            alert.getDialogPane().getStylesheets()
                    .add(getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm());
            alert.getDialogPane().getStyleClass().add("dialog-pane");
            alert.setTitle("Delete Note");
            alert.setHeaderText("Delete \"" + currentNote.getTitle() + "\"?");
            alert.setContentText("This action cannot be undone.");

            alert.showAndWait().ifPresent(response -> {
                if (response == javafx.scene.control.ButtonType.OK) {
                    noteService.deleteNote(currentNote.getId());
                    notes.remove(currentNote);
                    handleBack();
                }
            });
        }
    }

    @FXML
    private void handleAddLink() {
        if (currentNote == null || currentNote.getId() == null)
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
            controller.setExclude(ItemType.NOTE, currentNote.getId());
            dialog.showAndWait();

            if (controller.isLinkCreated()) {
                linkService.linkItems(ItemType.NOTE, currentNote.getId(),
                        controller.getSelectedType(), controller.getSelectedId());
                updateLinkedItemsSection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupAutoSave() {
        titleField.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
        contentArea.textProperty().addListener((obs, old, newVal) -> scheduleAutoSave());
    }

    private void scheduleAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
        }
        statusLabel.setText("Unsaved...");
        autoSaveTimer = new Timer();
        autoSaveTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    saveCurrentNote();
                    statusLabel.setText("Saved");
                });
            }
        }, 800);
    }

    private void saveCurrentNote() {
        if (currentNote != null) {
            currentNote.setTitle(titleField.getText());
            currentNote.setContent(contentArea.getText());
            noteService.saveNote(currentNote);
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
