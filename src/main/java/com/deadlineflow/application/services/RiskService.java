package com.deadlineflow.application.services;

import com.deadlineflow.domain.model.RiskLevel;
import com.deadlineflow.domain.model.Task;

import java.time.LocalDate;

public class RiskService {

    public RiskLevel evaluate(Task task, LocalDate today, String doneStatusName) {
        if (task.status().equals(doneStatusName)) {
            return RiskLevel.NONE;
        }
        if (task.dueDate().isBefore(today)) {
            return RiskLevel.OVERDUE;
        }
        if (!task.dueDate().isAfter(today.plusDays(1))) {
            return RiskLevel.DUE_SOON;
        }
        return RiskLevel.NONE;
    }
}
