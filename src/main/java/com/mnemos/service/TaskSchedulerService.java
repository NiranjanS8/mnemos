package com.mnemos.service;

import com.mnemos.model.Task;
import com.mnemos.model.Task.RecurrenceType;
import com.mnemos.repository.TaskRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

/**
 * Service to handle task scheduling, recurrence, and reminders.
 */
public class TaskSchedulerService {
    private static final Logger logger = Logger.getLogger(TaskSchedulerService.class.getName());
    private final TaskRepository taskRepository;

    public TaskSchedulerService() {
        this.taskRepository = new TaskRepository();
    }

    /**
     * Handle task completion: if recurrent, create the next instance.
     */
    public void onTaskCompleted(Task completedTask) {
        if (completedTask.getRecurrenceType() == RecurrenceType.NONE) {
            return;
        }

        // Check if recurrence end date passed
        if (completedTask.getRecurrenceEndDate() != null &&
                completedTask.getRecurrenceEndDate().isBefore(LocalDate.now())) {
            return;
        }

        Task nextTask = createNextRecurrence(completedTask);
        taskRepository.save(nextTask);
        logger.info("Created next recurring task instance: " + nextTask.getTitle());
    }

    private Task createNextRecurrence(Task originalTask) {
        LocalDate nextDueDate = calculateNextDueDate(originalTask);

        Task nextTask = new Task(
                originalTask.getTitle(),
                originalTask.getPriority(),
                nextDueDate);

        // Copy recurrence settings
        nextTask.setRecurrenceType(originalTask.getRecurrenceType());
        nextTask.setRecurrenceInterval(originalTask.getRecurrenceInterval());
        nextTask.setRecurrenceEndDate(originalTask.getRecurrenceEndDate());

        // Note: We don't copy the reminder, or maybe we should?
        // For now, let's assume the user sets a new reminder or we default to same
        // offset.
        // Let's copy it but adjust the date? The reminder date is a String ISO.
        // Parsing it is complex without knowing if it's relative or absolute.
        // We'll leave reminder null for the new instance for now (MVP).

        return nextTask;
    }

    private LocalDate calculateNextDueDate(Task task) {
        LocalDate baseDate = task.getDueDate() != null ? task.getDueDate() : LocalDate.now();

        if (task.getRecurrenceType() == RecurrenceType.DAILY) {
            int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;
            return baseDate.plusDays(interval);
        } else if (task.getRecurrenceType() == RecurrenceType.WEEKLY) {
            int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;
            return baseDate.plusWeeks(interval);
        } else if (task.getRecurrenceType() == RecurrenceType.CUSTOM) {
            // Assume days for custom for now
            int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;
            return baseDate.plusDays(interval);
        }

        return baseDate.plusDays(1); // Default fallback
    }
}
