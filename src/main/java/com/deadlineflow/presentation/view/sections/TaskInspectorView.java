package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.domain.model.Dependency;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TaskInspectorView extends VBox {
    private final Label inspectorTitleLabel = new Label();
    private final Label selectedTaskLabel = new Label();
    private final Label titleLabel = new Label();
    private final TextField titleField = new TextField();

    private final Label startDateLabel = new Label();
    private final DatePicker startDatePicker = new DatePicker();

    private final Label dueDateLabel = new Label();
    private final DatePicker dueDatePicker = new DatePicker();

    private final Label progressLabel = new Label();
    private final TextField progressField = new TextField("0");
    private final Label progressValueLabel = new Label("%");

    private final Label descriptionLabel = new Label();
    private final TextArea descriptionArea = new TextArea();

    private final Label statusLabel = new Label();
    private final ComboBox<String> statusComboBox = new ComboBox<>();
    private final Button manageStatusesButton = new Button();

    private final Label slackLabel = new Label();
    private final Label dependenciesLabel = new Label();
    private final ListView<Dependency> dependencyListView = new ListView<>();

    private final Button addDependencyButton = new Button();
    private final Button removeDependencyButton = new Button();
    private final Button deleteTaskButton = new Button();

    public TaskInspectorView() {
        getStyleClass().addAll("panel-card", "inspector-card");
        setSpacing(10);
        setPadding(new Insets(14));
        setPrefWidth(320);
        setMinWidth(280);
        setMaxWidth(360);

        inspectorTitleLabel.getStyleClass().add("section-title");
        selectedTaskLabel.getStyleClass().add("inspector-task-title");
        selectedTaskLabel.setText("-");
        titleLabel.getStyleClass().add("field-label");
        startDateLabel.getStyleClass().add("field-label");
        dueDateLabel.getStyleClass().add("field-label");
        progressLabel.getStyleClass().add("field-label");
        descriptionLabel.getStyleClass().add("field-label");
        statusLabel.getStyleClass().add("field-label");
        dependenciesLabel.getStyleClass().add("field-label");
        slackLabel.getStyleClass().add("muted-label");
        progressValueLabel.getStyleClass().add("muted-label");
        titleField.getStyleClass().add("inspector-input-target");
        startDatePicker.getStyleClass().add("inspector-input-target");
        dueDatePicker.getStyleClass().add("inspector-input-target");
        progressField.getStyleClass().add("inspector-input-target");
        descriptionArea.getStyleClass().add("inspector-input-target");
        statusComboBox.getStyleClass().add("inspector-input-target");

        HBox progressRow = new HBox(8, progressField, progressValueLabel);
        progressRow.getStyleClass().add("progress-row");
        progressRow.setAlignment(Pos.CENTER_LEFT);
        progressRow.setFillHeight(true);
        progressRow.setMaxWidth(Double.MAX_VALUE);
        progressRow.setMinHeight(Region.USE_PREF_SIZE);
        progressField.setAlignment(Pos.CENTER_RIGHT);
        progressField.setMinWidth(80);
        progressField.setPrefWidth(104);
        progressField.setMaxWidth(120);
        progressValueLabel.setMinWidth(16);
        progressValueLabel.setMaxWidth(24);
        progressValueLabel.setAlignment(Pos.CENTER_RIGHT);

        descriptionArea.setWrapText(true);
        descriptionArea.setMinHeight(104);
        descriptionArea.setPrefRowCount(6);

        HBox statusRow = new HBox(8, statusComboBox, manageStatusesButton);
        HBox.setHgrow(statusComboBox, Priority.ALWAYS);
        manageStatusesButton.getStyleClass().add("pill-button");

        dependencyListView.getStyleClass().add("dependency-list-view");
        dependencyListView.setPrefHeight(180);
        VBox.setVgrow(dependencyListView, Priority.ALWAYS);

        addDependencyButton.getStyleClass().add("pill-button");
        removeDependencyButton.getStyleClass().add("pill-button");
        deleteTaskButton.getStyleClass().addAll("pill-button", "danger-button");

        addDependencyButton.setMaxWidth(Double.MAX_VALUE);
        removeDependencyButton.setMaxWidth(Double.MAX_VALUE);
        deleteTaskButton.setMaxWidth(Double.MAX_VALUE);

        VBox dependencyActions = new VBox(8, addDependencyButton, removeDependencyButton, deleteTaskButton);

        getChildren().addAll(
                inspectorTitleLabel,
                selectedTaskLabel,
                titleLabel, titleField,
                startDateLabel, startDatePicker,
                dueDateLabel, dueDatePicker,
                progressLabel, progressRow,
                descriptionLabel, descriptionArea,
                statusLabel, statusRow,
                slackLabel,
                dependenciesLabel, dependencyListView,
                dependencyActions
        );
    }

    public Label inspectorTitleLabel() {
        return inspectorTitleLabel;
    }

    public Label selectedTaskLabel() {
        return selectedTaskLabel;
    }

    public Label titleLabel() {
        return titleLabel;
    }

    public TextField titleField() {
        return titleField;
    }

    public Label startDateLabel() {
        return startDateLabel;
    }

    public DatePicker startDatePicker() {
        return startDatePicker;
    }

    public Label dueDateLabel() {
        return dueDateLabel;
    }

    public DatePicker dueDatePicker() {
        return dueDatePicker;
    }

    public Label progressLabel() {
        return progressLabel;
    }

    public TextField progressField() {
        return progressField;
    }

    public Label progressValueLabel() {
        return progressValueLabel;
    }

    public Label descriptionLabel() {
        return descriptionLabel;
    }

    public TextArea descriptionArea() {
        return descriptionArea;
    }

    public Label statusLabel() {
        return statusLabel;
    }

    public ComboBox<String> statusComboBox() {
        return statusComboBox;
    }

    public Button manageStatusesButton() {
        return manageStatusesButton;
    }

    public Label slackLabel() {
        return slackLabel;
    }

    public Label dependenciesLabel() {
        return dependenciesLabel;
    }

    public ListView<Dependency> dependencyListView() {
        return dependencyListView;
    }

    public Button addDependencyButton() {
        return addDependencyButton;
    }

    public Button removeDependencyButton() {
        return removeDependencyButton;
    }

    public Button deleteTaskButton() {
        return deleteTaskButton;
    }

    public void setSelectionActive(boolean active) {
        if (active) {
            if (!getStyleClass().contains("inspector-active")) {
                getStyleClass().add("inspector-active");
            }
        } else {
            getStyleClass().remove("inspector-active");
        }
    }
}
