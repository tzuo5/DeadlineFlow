package com.deadlineflow.application.services;

import com.deadlineflow.data.repository.TaskRepository;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;
import java.util.UUID;

public class TaskService {
    private final TaskRepository taskRepository;
    private final SchedulerEngine schedulerEngine;

    public TaskService(TaskRepository taskRepository, SchedulerEngine schedulerEngine) {
        this.taskRepository = taskRepository;
        this.schedulerEngine = schedulerEngine;
    }

    public Task createTask(long projectId, String title, LocalDate startDate, LocalDate dueDate, String status) {
        Task task = new Task(
                UUID.randomUUID().toString(),
                projectId,
                title,
                Task.DEFAULT_DESCRIPTION,
                startDate,
                dueDate,
                0,
                status
        );
        schedulerEngine.validateTaskDates(task);
        taskRepository.save(task);
        return taskRepository.findById(task.id()).orElse(task);
    }
}
