package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Project;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface ProjectRepository {
    ObservableList<Project> getAll();

    Optional<Project> findById(long id);

    Project save(Project project);

    void delete(long projectId);
}
