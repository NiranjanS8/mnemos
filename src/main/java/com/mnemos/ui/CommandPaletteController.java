package com.mnemos.ui;

import com.mnemos.command.CommandItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CommandPaletteController {

    @FXML
    private TextField searchField;
    @FXML
    private ListView<CommandItem> commandListView;

    private final ObservableList<CommandItem> allCommands = FXCollections.observableArrayList();
    private final ObservableList<CommandItem> filteredCommands = FXCollections.observableArrayList();
    private Stage stage;
    private Runnable onCloseCallback;

    @FXML
    public void initialize() {
        commandListView.setItems(filteredCommands);
        commandListView.setCellFactory(param -> new CommandListCell());

        // Filter commands as user types
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterCommands(newVal));

        // Handle keyboard navigation
        searchField.setOnKeyPressed(this::handleKeyPress);
        commandListView.setOnKeyPressed(this::handleKeyPress);

        // Handle command selection
        commandListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                executeSelectedCommand();
            }
        });
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void setCommands(List<CommandItem> commands) {
        allCommands.setAll(commands);
        filterCommands("");

        // Auto-select first item
        if (!filteredCommands.isEmpty()) {
            commandListView.getSelectionModel().select(0);
        }
    }

    public void focusSearch() {
        searchField.requestFocus();
    }

    private void filterCommands(String query) {
        if (query == null || query.trim().isEmpty()) {
            filteredCommands.setAll(allCommands);
        } else {
            List<CommandItem> matches = new ArrayList<>();
            for (CommandItem cmd : allCommands) {
                if (cmd.matches(query)) {
                    matches.add(cmd);
                }
            }
            filteredCommands.setAll(matches);
        }

        // Auto-select first item
        if (!filteredCommands.isEmpty()) {
            commandListView.getSelectionModel().select(0);
        }
    }

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            close();
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER) {
            executeSelectedCommand();
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            if (event.getSource() == searchField) {
                commandListView.requestFocus();
                commandListView.getSelectionModel().selectFirst();
                event.consume();
            }
        } else if (event.getCode() == KeyCode.UP) {
            if (event.getSource() == commandListView &&
                    commandListView.getSelectionModel().getSelectedIndex() == 0) {
                searchField.requestFocus();
                event.consume();
            }
        }
    }

    private void executeSelectedCommand() {
        CommandItem selected = commandListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            close();
            // Execute command after closing to avoid UI issues
            javafx.application.Platform.runLater(() -> selected.getCommand().execute());
        }
    }

    @FXML
    private void handleOverlayClick(MouseEvent event) {
        // Close if clicking outside the command palette
        if (event.getTarget() == event.getSource()) {
            close();
        }
    }

    @FXML
    private void consumeClick(MouseEvent event) {
        // Prevent clicks inside the palette from closing it
        event.consume();
    }

    private void close() {
        if (stage != null) {
            stage.close();
        }
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private static class CommandListCell extends ListCell<CommandItem> {
        @Override
        protected void updateItem(CommandItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getDisplayText());
                setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 13px; -fx-padding: 8;");
            }
        }
    }
}
