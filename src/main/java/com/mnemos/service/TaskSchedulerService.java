package com.mnemos.service;

import com.mnemos.model.Task;
import com.mnemos.model.Task.RecurrenceType;
import com.mnemos.model.Task.RecurrenceUnit;
import com.mnemos.repository.TaskRepository;
import java.time.LocalDate;
import java.util.logging.Logger;

public class TaskSchedulerService {
    private static final Logger logger = Logger.getLogger(TaskSchedulerService.class.getName());
    private final TaskRepository taskRepository;

    public TaskSchedulerService() {
        this.taskRepository = new TaskRepository();
    }

    public void onTaskCompleted(Task completedTask) {
        if (completedTask.getRecurrenceType() == RecurrenceType.NONE) {
            return;
        }

        if (completedTask.getRecurrenceEndDate() != null &&
                completedTask.getRecurrenceEndDate().isBefore(LocalDate.now())) {
            return;
        }

        if (completedTask.getRecurrenceMaxOccurrences() > 0) {
            // Max occurrences reached — don't create a new one
            // (caller is expected to decrement before calling)
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

        nextTask.setRecurrenceType(originalTask.getRecurrenceType());
        nextTask.setRecurrenceInterval(originalTask.getRecurrenceInterval());
        nextTask.setRecurrenceEndDate(originalTask.getRecurrenceEndDate());
        nextTask.setRecurrenceUnit(originalTask.getRecurrenceUnit());
        nextTask.setRecurrenceDays(originalTask.getRecurrenceDays());
        nextTask.setRecurrenceMaxOccurrences(
                originalTask.getRecurrenceMaxOccurrences() > 0
                        ? originalTask.getRecurrenceMaxOccurrences() - 1
                        : 0);

        return nextTask;
    }

    private LocalDate calculateNextDueDate(Task task) {
        LocalDate baseDate = task.getDueDate() != null ? task.getDueDate() : LocalDate.now();
        int interval = task.getRecurrenceInterval() > 0 ? task.getRecurrenceInterval() : 1;

        if (task.getRecurrenceType() == RecurrenceType.DAILY) {
            return baseDate.plusDays(interval);
        } else if (task.getRecurrenceType() == RecurrenceType.WEEKLY) {
            return baseDate.plusWeeks(interval);
        } else if (task.getRecurrenceType() == RecurrenceType.CUSTOM) {
            RecurrenceUnit unit = task.getRecurrenceUnit() != null ? task.getRecurrenceUnit() : RecurrenceUnit.DAYS;
            return switch (unit) {
                case WEEKS -> baseDate.plusWeeks(interval);
                case MONTHS -> baseDate.plusMonths(interval);
                default -> baseDate.plusDays(interval);
            };
        }

        return baseDate.plusDays(1);
    }
}
