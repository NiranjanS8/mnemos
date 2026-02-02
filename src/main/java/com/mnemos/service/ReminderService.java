package com.mnemos.service;

import com.mnemos.model.Task;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Service for scheduling and triggering task reminders.
 */
public class ReminderService {
    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);
    private static final ReminderService INSTANCE = new ReminderService();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> scheduledReminders = new ConcurrentHashMap<>();
    private final TaskService taskService = new TaskService();

    private TrayIcon trayIcon;
    private boolean traySupported = false;

    private ReminderService() {
        initializeTray();
    }

    public static ReminderService getInstance() {
        return INSTANCE;
    }

    private void initializeTray() {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage(
                        getClass().getResource("/com/mnemos/ui/icon.png"));

                // Fallback if no icon
                if (image == null) {
                    image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                }

                trayIcon = new TrayIcon(image, "Mnemos Reminders");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
                traySupported = true;
                logger.info("System tray initialized for notifications");
            } catch (Exception e) {
                logger.warn("Failed to initialize system tray", e);
                traySupported = false;
            }
        } else {
            logger.warn("System tray not supported on this platform");
        }
    }

    /**
     * Set a reminder for a task at the specified time.
     */
    public void setReminder(Task task, LocalDateTime reminderTime) {
        if (task == null || task.getId() == null || reminderTime == null) {
            return;
        }

        // Store the reminder timestamp
        String isoTimestamp = reminderTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        task.setReminderDate(isoTimestamp);
        taskService.saveTask(task);

        // Schedule the notification
        scheduleNotification(task, reminderTime);

        logger.info("Reminder set for task '{}' at {}", task.getTitle(), reminderTime);
    }

    /**
     * Schedule a notification for the given task.
     */
    public void scheduleNotification(Task task, LocalDateTime reminderTime) {
        // Cancel any existing reminder for this task
        cancelReminder(task.getId());

        long delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), reminderTime);

        if (delayMs <= 0) {
            // Time already passed, trigger immediately
            triggerNotification(task);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            triggerNotification(task);
            scheduledReminders.remove(task.getId());
        }, delayMs, TimeUnit.MILLISECONDS);

        scheduledReminders.put(task.getId(), future);
    }

    /**
     * Cancel a scheduled reminder.
     */
    public void cancelReminder(Long taskId) {
        ScheduledFuture<?> existing = scheduledReminders.remove(taskId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    /**
     * Trigger a notification for the task.
     */
    public void triggerNotification(Task task) {
        String title = "Task Reminder";
        String message = "📌 " + task.getTitle();

        if (traySupported && trayIcon != null) {
            // Use system tray notification
            trayIcon.displayMessage(title, message, MessageType.INFO);
        } else {
            // Fallback: show JavaFX alert on FX thread
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText("Task Reminder");
                alert.setContentText(message);
                alert.show();
            });
        }

        logger.info("Notification triggered for task: {}", task.getTitle());
    }

    /**
     * Calculate "Later Today" time (next 6:00 PM).
     */
    public LocalDateTime calculateLaterToday() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sixPm = now.withHour(18).withMinute(0).withSecond(0);

        if (now.isAfter(sixPm)) {
            // Already past 6 PM, set for tomorrow
            return sixPm.plusDays(1);
        }
        return sixPm;
    }

    /**
     * Calculate "Tomorrow Morning" time (9:00 AM).
     */
    public LocalDateTime calculateTomorrowMorning() {
        return LocalDateTime.now()
                .plusDays(1)
                .withHour(9)
                .withMinute(0)
                .withSecond(0);
    }

    /**
     * Load and reschedule all pending reminders from the database.
     * Should be called on app startup.
     */
    public void loadPendingReminders() {
        for (Task task : taskService.getAllTasks()) {
            String reminderStr = task.getReminderDate();
            if (reminderStr != null && !reminderStr.isEmpty()) {
                try {
                    LocalDateTime reminderTime = LocalDateTime.parse(reminderStr,
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    if (reminderTime.isAfter(LocalDateTime.now())) {
                        scheduleNotification(task, reminderTime);
                    }
                } catch (Exception e) {
                    logger.warn("Could not parse reminder date for task {}: {}", task.getId(), reminderStr);
                }
            }
        }
        logger.info("Loaded pending reminders");
    }

    /**
     * Shutdown the scheduler.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
