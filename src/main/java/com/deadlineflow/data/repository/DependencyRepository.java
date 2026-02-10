package com.deadlineflow.data.repository;

import com.deadlineflow.domain.model.Dependency;
import javafx.collections.ObservableList;

import java.util.Optional;

public interface DependencyRepository {
    ObservableList<Dependency> getAll();

    Optional<Dependency> findById(String dependencyId);

    Dependency save(Dependency dependency);

    void delete(String dependencyId);
}
