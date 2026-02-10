package com.deadlineflow.presentation.view;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.time.LocalDate;
import java.util.Optional;

public final class TaskDialog {
    private TaskDialog() {
    }

    public record Result(String title, LocalDate startDate, LocalDate dueDate) {
    }

    public static Optional<Result> show(Window owner) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle("Create Task");
        dialog.initOwner(owner);

        ButtonType createButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, ButtonType.CANCEL);

        TextField titleField = new TextField();
        titleField.setPromptText("Task title");
        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker dueDatePicker = new DatePicker(LocalDate.now().plusDays(2));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Title"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Start"), 0, 1);
        grid.add(startDatePicker, 1, 1);
        grid.add(new Label("Due"), 0, 2);
        grid.add(dueDatePicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != createButton) {
                return null;
            }
            return new Result(titleField.getText().trim(), startDatePicker.getValue(), dueDatePicker.getValue());
        });

        return dialog.showAndWait();
    }
}
