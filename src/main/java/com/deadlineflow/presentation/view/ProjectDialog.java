package com.deadlineflow.presentation.view;

import com.deadlineflow.domain.model.Project;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.util.Optional;

public final class ProjectDialog {
    private ProjectDialog() {
    }

    public record Result(String name, String colorHex, int priority) {
    }

    public static Optional<Result> show(Window owner, Project existing) {
        Dialog<Result> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create Project" : "Edit Project");
        dialog.initOwner(owner);

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TextField nameField = new TextField(existing == null ? "" : existing.name());
        ColorPicker colorPicker = new ColorPicker(existing == null ? Color.web("#3A7AFE") : safeColor(existing.color()));
        Spinner<Integer> prioritySpinner = new Spinner<>(1, 5, existing == null ? 3 : existing.priority());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.add(new Label("Name"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Color"), 0, 1);
        grid.add(colorPicker, 1, 1);
        grid.add(new Label("Priority"), 0, 2);
        grid.add(prioritySpinner, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButton) {
                return null;
            }
            return new Result(nameField.getText().trim(), toHex(colorPicker.getValue()), prioritySpinner.getValue());
        });

        return dialog.showAndWait();
    }

    private static String toHex(Color color) {
        int r = (int) Math.round(color.getRed() * 255);
        int g = (int) Math.round(color.getGreen() * 255);
        int b = (int) Math.round(color.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static Color safeColor(String value) {
        try {
            return Color.web(value);
        } catch (Exception ignored) {
            return Color.web("#3A7AFE");
        }
    }
}
