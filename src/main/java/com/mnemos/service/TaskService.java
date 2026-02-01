package com.mnemos.service;

import com.mnemos.model.Task;
import com.mnemos.repository.TaskRepository;
import java.util.List;
import java.util.stream.Collectors;

public class TaskService {
    private final TaskRepository repository;

    public TaskService() {
        this.repository = new TaskRepository();
    }

    public Task saveTask(Task task) {
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
}
