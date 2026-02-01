package com.mnemos.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private AnchorPane rootPane;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private AnchorPane notesContainer;
    @FXML
    private AnchorPane tasksContainer;
    @FXML
    private AnchorPane filesContainer;

    @FXML
    public void initialize() {
        try {
            javafx.fxml.FXMLLoader notesLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/NotesView.fxml"));
            javafx.scene.Node notesView = notesLoader.load();

            // Anchor to edges
            AnchorPane.setTopAnchor(notesView, 0.0);
            AnchorPane.setBottomAnchor(notesView, 0.0);
            AnchorPane.setLeftAnchor(notesView, 0.0);
            AnchorPane.setRightAnchor(notesView, 0.0);

            notesContainer.getChildren().add(notesView);

            // Load Tasks
            javafx.fxml.FXMLLoader tasksLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/TasksView.fxml"));
            javafx.scene.Node tasksView = tasksLoader.load();
            AnchorPane.setTopAnchor(tasksView, 0.0);
            AnchorPane.setBottomAnchor(tasksView, 0.0);
            AnchorPane.setLeftAnchor(tasksView, 0.0);
            AnchorPane.setRightAnchor(tasksView, 0.0);
            tasksContainer.getChildren().add(tasksView);

            // Load Files
            javafx.fxml.FXMLLoader filesLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/FilesView.fxml"));
            javafx.scene.Node filesView = filesLoader.load();
            AnchorPane.setTopAnchor(filesView, 0.0);
            AnchorPane.setBottomAnchor(filesView, 0.0);
            AnchorPane.setLeftAnchor(filesView, 0.0);
            AnchorPane.setRightAnchor(filesView, 0.0);
            filesContainer.getChildren().add(filesView);

        } catch (Throwable e) {
            e.printStackTrace();
            com.mnemos.util.AlertUtils.showError("Initialization Error", "Failed to load application modules.",
                    e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        // For now, exit app. Later invalid minimize to tray
        System.exit(0);
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }
}
