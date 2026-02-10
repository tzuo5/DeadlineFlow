package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.domain.model.Project;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ProjectsSidebarView extends VBox {
    private final Label titleLabel = new Label();
    private final ListView<Project> projectListView = new ListView<>();
    private final Button addProjectButton = new Button();
    private final Button editProjectButton = new Button();
    private final Button deleteProjectButton = new Button();

    public ProjectsSidebarView() {
        getStyleClass().addAll("panel-card", "sidebar-card");
        setSpacing(10);
        setPadding(new Insets(14));
        setPrefWidth(246);
        setMinWidth(220);
        setMaxWidth(270);

        titleLabel.getStyleClass().add("section-title");
        projectListView.getStyleClass().add("project-list-view");

        addProjectButton.getStyleClass().addAll("pill-button", "primary-button");
        editProjectButton.getStyleClass().add("pill-button");
        deleteProjectButton.getStyleClass().addAll("pill-button", "danger-button");

        addProjectButton.setMaxWidth(Double.MAX_VALUE);
        editProjectButton.setMaxWidth(Double.MAX_VALUE);
        deleteProjectButton.setMaxWidth(Double.MAX_VALUE);

        VBox actions = new VBox(8, addProjectButton, editProjectButton, deleteProjectButton);

        getChildren().addAll(titleLabel, projectListView, actions);
        VBox.setVgrow(projectListView, Priority.ALWAYS);
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public ListView<Project> projectListView() {
        return projectListView;
    }

    public Button addProjectButton() {
        return addProjectButton;
    }

    public Button editProjectButton() {
        return editProjectButton;
    }

    public Button deleteProjectButton() {
        return deleteProjectButton;
    }
}
