package com.deadlineflow.application.services;

import com.deadlineflow.domain.exceptions.ValidationException;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;

public class SchedulerEngine {

    public Task shiftTask(Task task, long deltaDays) {
        LocalDate newStart = task.startDate().plusDays(deltaDays);
        LocalDate newDue = task.dueDate().plusDays(deltaDays);
        return task.withDates(newStart, newDue);
    }

    public Task resizeTaskStart(Task task, LocalDate newStart) {
        LocalDate adjustedStart = newStart;
        if (adjustedStart.isAfter(task.dueDate())) {
            adjustedStart = task.dueDate();
        }
        return task.withDates(adjustedStart, task.dueDate());
    }

    public Task resizeTaskDue(Task task, LocalDate newDue) {
        LocalDate adjustedDue = newDue;
        if (adjustedDue.isBefore(task.startDate())) {
            adjustedDue = task.startDate();
        }
        return task.withDates(task.startDate(), adjustedDue);
    }

    public Task validateTaskDates(Task task) {
        if (task.dueDate().isBefore(task.startDate())) {
            throw new ValidationException("Due date cannot be before start date");
        }
        return task;
    }
}
