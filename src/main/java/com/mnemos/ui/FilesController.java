package com.mnemos.ui;

import com.mnemos.model.FileReference;
import com.mnemos.model.LinkedItem;
import com.mnemos.model.Link.ItemType;
import com.mnemos.service.FileService;
import com.mnemos.service.LinkService;
import com.mnemos.App;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.util.List;

public class FilesController {

    @FXML
    private VBox dropZone;
    @FXML
    private ListView<FileReference> filesListView;
    @FXML
    private VBox linkedItemsSection;
    @FXML
    private ListView<LinkedItem> linkedItemsListView;

    private final FileService fileService = new FileService();
    private final LinkService linkService = new LinkService();
    private final ObservableList<FileReference> files = FXCollections.observableArrayList();
    private final ObservableList<LinkedItem> linkedItems = FXCollections.observableArrayList();
    private FileReference selectedFile = null;

    @FXML
    public void initialize() {
        filesListView.setItems(files);
        filesListView.setCellFactory(param -> new FileListCell());

        // Setup linked items list
        linkedItemsListView.setItems(linkedItems);
        linkedItemsListView.setCellFactory(param -> new LinkedItemCell());

        // Listen for file selection
        filesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldFile, newFile) -> {
            selectedFile = newFile;
            updateLinkedItemsSection();
        });

        loadFiles();
    }

    private void updateLinkedItemsSection() {
        if (selectedFile != null && selectedFile.getId() != null) {
            linkedItems.clear();
            linkedItems.addAll(linkService.getLinkedItems(ItemType.FILE, selectedFile.getId()));
            linkedItemsSection.setVisible(true);
            linkedItemsSection.setManaged(true);
        } else {
            linkedItems.clear();
            linkedItemsSection.setVisible(false);
            linkedItemsSection.setManaged(false);
        }
    }

    @FXML
    private void handleAddLink() {
        if (selectedFile == null || selectedFile.getId() == null)
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
            controller.setExclude(ItemType.FILE, selectedFile.getId());
            dialog.showAndWait();

            if (controller.isLinkCreated()) {
                linkService.linkItems(ItemType.FILE, selectedFile.getId(),
                        controller.getSelectedType(), controller.getSelectedId());
                updateLinkedItemsSection();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class FileListCell extends ListCell<FileReference> {
        private final Label nameLabel = new Label();
        private final javafx.scene.control.Button deleteButton = new javafx.scene.control.Button();
        private final HBox content = new HBox(10);

        public FileListCell() {
            nameLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);
            nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px;");

            deleteButton.setText("Remove");
            deleteButton.setMinWidth(60);
            deleteButton.setMaxWidth(60);
            deleteButton.setStyle(
                    "-fx-background-color: rgba(255, 107, 107, 0.2); -fx-text-fill: #ff6b6b; -fx-cursor: hand; -fx-font-size: 11px; -fx-background-radius: 4; -fx-padding: 4 8;");
            deleteButton.setOnAction(e -> {
                FileReference item = getItem();
                if (item != null && item.getId() != null) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    alert.initOwner(filesListView.getScene().getWindow());
                    alert.getDialogPane().getStylesheets()
                            .add(getClass().getResource("/com/mnemos/ui/styles.css").toExternalForm());
                    alert.getDialogPane().getStyleClass().add("dialog-pane");
                    alert.setTitle("Remove File Reference");
                    alert.setHeaderText("Remove \"" + item.getName() + "\" from list?");
                    alert.setContentText("The file will remain in its original location.");

                    alert.showAndWait().ifPresent(response -> {
                        if (response == javafx.scene.control.ButtonType.OK) {
                            fileService.deleteFile(item.getId());
                            files.remove(item);
                        }
                    });
                }
            });

            content.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            content.getChildren().addAll(nameLabel, deleteButton);

            setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && getItem() != null) {
                    fileService.openFile(getItem());
                }
            });
        }

        @Override
        protected void updateItem(FileReference item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                nameLabel.setText(item.getName());
                setGraphic(content);
                setText(null);
            }
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

    private void loadFiles() {
        files.setAll(fileService.getAllFiles());
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
        event.consume();
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        var db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            List<File> droppedFiles = db.getFiles();
            for (File file : droppedFiles) {
                fileService.addFile(file);
            }
            loadFiles();
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    private void handleChooseFile() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Choose File");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
                new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
                new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new javafx.stage.FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mkv"));

        File selectedFileChooser = fileChooser.showOpenDialog(filesListView.getScene().getWindow());
        if (selectedFileChooser != null) {
            fileService.addFile(selectedFileChooser);
            loadFiles();
        }
    }
}
