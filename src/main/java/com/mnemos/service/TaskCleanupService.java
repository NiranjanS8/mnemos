package com.mnemos.service;

import com.mnemos.repository.TaskRepository;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskCleanupService {
    private static final Logger logger = LoggerFactory.getLogger(TaskCleanupService.class);
    private static final long CLEANUP_DELAY_MINUTES = 5;
    private static final long CHECK_INTERVAL_MINUTES = 1; // Check every minute

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "TaskCleanupService");
        thread.setDaemon(true); // Daemon thread won't prevent app shutdown
        return thread;
    });

    private final TaskRepository taskRepository = new TaskRepository();
    private Runnable onTasksDeletedCallback;

    public void start() {
        logger.info("Starting TaskCleanupService - will delete completed tasks after {} minutes",
                CLEANUP_DELAY_MINUTES);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                int deletedCount = taskRepository.deleteOldCompletedTasks(CLEANUP_DELAY_MINUTES);
                if (deletedCount > 0) {
                    logger.info("Auto-deleted {} completed task(s)", deletedCount);

                    // Notify UI on JavaFX thread
                    if (onTasksDeletedCallback != null) {
                        Platform.runLater(onTasksDeletedCallback);
                    }
                }
            } catch (Exception e) {
                logger.error("Error during task cleanup", e);
            }
        }, CHECK_INTERVAL_MINUTES, CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public void setOnTasksDeletedCallback(Runnable callback) {
        this.onTasksDeletedCallback = callback;
    }

    public void shutdown() {
        logger.info("Shutting down TaskCleanupService");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
