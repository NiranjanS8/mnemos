package com.mnemos.service;

import com.mnemos.model.Task;
import com.mnemos.repository.TaskRepository;
import java.util.List;
import java.util.stream.Collectors;

public class TaskService {
    private final TaskRepository repository;
    private final TaskSchedulerService scheduler;

    public TaskService() {
        this.repository = new TaskRepository();
        this.scheduler = new TaskSchedulerService();
    }

    public Task saveTask(Task task) {
        // Business Logic: Check dependencies before completion
        if (task.getStatus() == Task.Status.COMPLETED) {
            if (!repository.canStart(task.getId())) {
                // Task is blocked.
                // For now, we revert status or throw exception?
                // Ideally UI should prevent this.
                // But backend should enforce.
                // We'll throw an IllegalStateException for the controller to handle.
                throw new IllegalStateException("Task is blocked by incomplete dependencies.");
            }

            // Handle recurrence
            scheduler.onTaskCompleted(task);
        }

        return repository.save(task);
    }

    public List<Task> getAllTasks() {
        return repository.findAll();
    }

    public List<Task> getTasksByStatus(Task.Status status) {
        return repository.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    public void deleteTask(Long id) {
        repository.deleteById(id);
    }

    public int deleteAllCompletedTasks() {
        return repository.deleteAllCompletedTasks();
    }

    // Dependency Management
    public void addDependency(long predecessorId, long successorId) {
        if (predecessorId == successorId)
            return; // Prevent self-dependency
        repository.addDependency(predecessorId, successorId);
    }

    public void removeDependency(long predecessorId, long successorId) {
        repository.removeDependency(predecessorId, successorId);
    }

    public boolean isBlocked(long taskId) {
        return !repository.canStart(taskId);
    }
}
