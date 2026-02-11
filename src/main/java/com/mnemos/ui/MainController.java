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
    private javafx.scene.control.ToggleButton notesTabBtn;
    @FXML
    private javafx.scene.control.ToggleButton tasksTabBtn;
    @FXML
    private javafx.scene.control.ToggleButton filesTabBtn;

    @FXML
    public void initialize() {
        try {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(32);
            clip.setArcHeight(32);
            clip.widthProperty().bind(rootPane.widthProperty());
            clip.heightProperty().bind(rootPane.heightProperty());
            rootPane.setClip(clip);

            javafx.fxml.FXMLLoader notesLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/NotesView.fxml"));
            javafx.scene.Node notesView = notesLoader.load();

            AnchorPane.setTopAnchor(notesView, 0.0);
            AnchorPane.setBottomAnchor(notesView, 0.0);
            AnchorPane.setLeftAnchor(notesView, 0.0);
            AnchorPane.setRightAnchor(notesView, 0.0);

            notesContainer.getChildren().add(notesView);

            javafx.fxml.FXMLLoader tasksLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/TasksView.fxml"));
            javafx.scene.Node tasksView = tasksLoader.load();
            AnchorPane.setTopAnchor(tasksView, 0.0);
            AnchorPane.setBottomAnchor(tasksView, 0.0);
            AnchorPane.setLeftAnchor(tasksView, 0.0);
            AnchorPane.setRightAnchor(tasksView, 0.0);
            tasksContainer.getChildren().add(tasksView);

            javafx.fxml.FXMLLoader filesLoader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/com/mnemos/ui/FilesView.fxml"));
            javafx.scene.Node filesView = filesLoader.load();
            AnchorPane.setTopAnchor(filesView, 0.0);
            AnchorPane.setBottomAnchor(filesView, 0.0);
            AnchorPane.setLeftAnchor(filesView, 0.0);
            AnchorPane.setRightAnchor(filesView, 0.0);
            filesContainer.getChildren().add(filesView);

            javafx.scene.control.ToggleGroup tabGroup = new javafx.scene.control.ToggleGroup();
            notesTabBtn.setToggleGroup(tabGroup);
            tasksTabBtn.setToggleGroup(tabGroup);
            filesTabBtn.setToggleGroup(tabGroup);

            notesTabBtn.setOnAction(e -> mainTabPane.getSelectionModel().select(0));
            tasksTabBtn.setOnAction(e -> mainTabPane.getSelectionModel().select(1));
            filesTabBtn.setOnAction(e -> mainTabPane.getSelectionModel().select(2));

            mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
                switch (newVal.intValue()) {
                    case 0:
                        tabGroup.selectToggle(notesTabBtn);
                        break;
                    case 1:
                        tabGroup.selectToggle(tasksTabBtn);
                        break;
                    case 2:
                        tabGroup.selectToggle(filesTabBtn);
                        break;
                }
            });

            tabGroup.selectToggle(notesTabBtn);

        } catch (Throwable e) {
            e.printStackTrace();
            com.mnemos.util.AlertUtils.showError("Initialization Error", "Failed to load application modules.",
                    e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    private void handleMinimize() {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.setIconified(true);
    }
}
