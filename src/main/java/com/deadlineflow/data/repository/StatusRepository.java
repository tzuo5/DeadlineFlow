package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.StatusDefinition;
import javafx.collections.ObservableList;

public interface StatusRepository {
    ObservableList<StatusDefinition> getAll();

    void add(String name);

    void rename(String existingName, String newName);

    void delete(String name, String fallbackName);
}
