package com.mnemos;

import com.mnemos.command.CommandItem;
import com.mnemos.service.AuthService;
import com.mnemos.service.TaskCleanupService;
import com.mnemos.ui.CommandPaletteController;
import com.mnemos.ui.LoginController;
import com.mnemos.ui.SetupPasswordController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private double xOffset = 0;
    private double yOffset = 0;
    private Stage primaryStage;
    private TrayIcon trayIcon;
    private boolean isWindowVisible = false;
    private Stage commandPaletteStage;
    private List<CommandItem> commands = new ArrayList<>();
    private boolean isDragging = false;
    private TaskCleanupService taskCleanupService;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            com.mnemos.util.AlertUtils.showException("Uncaught Exception", throwable);
        });

        try {
            com.mnemos.util.DatabaseManager.initialize();

            AuthService authService = new AuthService();
            if (!authenticateUser(authService)) {
                Platform.exit();
                return;
            }
        } catch (Exception e) {
            com.mnemos.util.AlertUtils.showException("Database Error", e);
        }

        FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/mnemos/ui/MainView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

        var styles = App.class.getResource("/com/mnemos/ui/styles.css");
        if (styles != null) {
            scene.getStylesheets().add(styles.toExternalForm());
        }

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
            isDragging = false;
        });
        root.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
            isDragging = true;
        });
        root.setOnMouseReleased(event -> {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(200));
            pause.setOnFinished(e -> isDragging = false);
            pause.play();
        });

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN),
                this::showCommandPalette);

        stage.setResizable(true);
        loadWindowBounds(stage);

        stage.setOnCloseRequest(event -> {
            event.consume();
            saveWindowBounds(stage);
            hideWindow();
        });

        Platform.setImplicitExit(false);
        setupSystemTray();
        initializeCommands();
        showWindow();
    }

    private void loadWindowBounds(Stage stage) {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
            double x = prefs.getDouble("window.x", 100);
            double y = prefs.getDouble("window.y", 100);
            double width = prefs.getDouble("window.width", 500);
            double height = prefs.getDouble("window.height", 700);

            stage.setX(x);
            stage.setY(y);
            stage.setWidth(width);
            stage.setHeight(height);
        } catch (Exception e) {
            stage.setWidth(500);
            stage.setHeight(700);
        }
    }

    private void saveWindowBounds(Stage stage) {
        try {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(App.class);
            prefs.putDouble("window.x", stage.getX());
            prefs.putDouble("window.y", stage.getY());
            prefs.putDouble("window.width", stage.getWidth());
            prefs.putDouble("window.height", stage.getHeight());
            prefs.flush();
        } catch (Exception e) {
            logger.error("Failed to save window bounds", e);
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            Platform.runLater(this::showWindow);
            return;
        }

        try {
            BufferedImage trayIconImage = ImageIO.read(
                    App.class.getResourceAsStream("/com/mnemos/ui/tray-icon.png"));

            if (trayIconImage == null) {
                Platform.runLater(this::showWindow);
                return;
            }

            SystemTray tray = SystemTray.getSystemTray();

            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("Show Mnemos");
            showItem.addActionListener(e -> Platform.runLater(this::toggleWindow));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });

            popup.add(showItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(trayIconImage, "Mnemos", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                        Platform.runLater(() -> toggleWindow());
                    }
                }
            });
            tray.add(trayIcon);

        } catch (Exception e) {
            logger.error("Error setting up system tray", e);
            Platform.runLater(this::showWindow);
        }
    }

    private void toggleWindow() {
        if (isWindowVisible) {
            hideWindow();
        } else {
            showWindow();
        }
    }

    private void showWindow() {
        if (!isWindowVisible) {
            primaryStage.show();
            primaryStage.requestFocus();
            primaryStage.toFront();
            isWindowVisible = true;
        }
    }

    private void hideWindow() {
        if (isWindowVisible) {
            primaryStage.hide();
            isWindowVisible = false;
        }
    }

    private void initializeCommands() {
        commands.add(new CommandItem("New Note", "Create a new note", "Actions", "📝", () -> {
        }));
        commands.add(new CommandItem("New Task", "Create a new task", "Actions", "✓", () -> {
        }));
        commands.add(new CommandItem("Add File", "Add a file reference", "Actions", "📎", () -> {
        }));
        commands.add(new CommandItem("Go to Notes", "Navigate to Notes view", "Navigation", "📄", () -> {
        }));
        commands.add(new CommandItem("Go to Tasks", "Navigate to Tasks view", "Navigation", "✓", () -> {
        }));
        commands.add(new CommandItem("Go to Files", "Navigate to Files view", "Navigation", "📁", () -> {
        }));
    }

    private void showCommandPalette() {
        try {
            if (commandPaletteStage != null && commandPaletteStage.isShowing()) {
                commandPaletteStage.toFront();
                return;
            }

            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/mnemos/ui/CommandPalette.fxml"));
            Parent root = loader.load();
            CommandPaletteController controller = loader.getController();

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(App.class.getResource("/com/mnemos/ui/styles.css").toExternalForm());

            commandPaletteStage = new Stage();
            commandPaletteStage.initStyle(StageStyle.TRANSPARENT);
            commandPaletteStage.initModality(Modality.APPLICATION_MODAL);
            commandPaletteStage.initOwner(primaryStage);
            commandPaletteStage.setScene(scene);
            commandPaletteStage.setAlwaysOnTop(true);

            controller.setStage(commandPaletteStage);
            controller.setCommands(commands);
            controller.setOnCloseCallback(() -> commandPaletteStage = null);

            commandPaletteStage.show();
            controller.focusSearch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean authenticateUser(AuthService authService) {
        try {
            if (!authService.isPasswordSet()) {
                return showSetupPasswordDialog();
            } else {
                return showLoginDialog();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean showSetupPasswordDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/mnemos/ui/SetupPasswordDialog.fxml"));
            Parent root = loader.load();
            SetupPasswordController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Setup Password");
            dialog.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);

            controller.setStage(dialog);
            dialog.showAndWait();

            return controller.isSetupComplete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean showLoginDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/mnemos/ui/LoginDialog.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Login");
            dialog.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);

            controller.setStage(dialog);
            dialog.showAndWait();

            return controller.isLoginSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void stop() throws Exception {
        if (taskCleanupService != null) {
            taskCleanupService.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}
