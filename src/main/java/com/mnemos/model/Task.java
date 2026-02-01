package com.mnemos.model;

import java.time.Instant;
import java.time.LocalDate;

public class Task {
    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum Status {
        PENDING, COMPLETED
    }

    private Long id;
    private String title;
    private Priority priority;
    private LocalDate dueDate;
    private Status status;
    private int pomodoroCount;

    public Task(Long id, String title, Priority priority, LocalDate dueDate, Status status, int pomodoroCount) {
        this.id = id;
        this.title = title;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = status;
        this.pomodoroCount = pomodoroCount;
    }

    public Task(Long id, String title, Priority priority, LocalDate dueDate, Status status) {
        this(id, title, priority, dueDate, status, 0);
    }

    public Task(String title, Priority priority, LocalDate dueDate) {
        this(null, title, priority, dueDate, Status.PENDING, 0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getPomodoroCount() {
        return pomodoroCount;
    }

    public void setPomodoroCount(int pomodoroCount) {
        this.pomodoroCount = pomodoroCount;
    }
}
