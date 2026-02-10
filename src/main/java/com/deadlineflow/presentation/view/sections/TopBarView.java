package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.presentation.theme.ThemeManager;
import com.deadlineflow.presentation.viewmodel.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TopBarView extends VBox {
    private final Label bannerLabel = new Label();
    private final Label cpmBanner = new Label();

    private final Label scaleLabel = new Label();
    private final ToggleButton dayButton = new ToggleButton();
    private final ToggleButton weekButton = new ToggleButton();
    private final ToggleButton yearButton = new ToggleButton();

    private final Label zoomLabel = new Label();
    private final Slider zoomSlider = new Slider(0.5, 3.0, 1.0);

    private final Label themeLabel = new Label();
    private final ComboBox<ThemeManager.ThemeMode> themeComboBox = new ComboBox<>();

    private final Label languageLabel = new Label();
    private final ComboBox<LanguageManager.Language> languageComboBox = new ComboBox<>();

    private final Button addTaskButton = new Button();
    private final Label finishDateLabel = new Label();

    public TopBarView() {
        getStyleClass().add("toolbar-shell");
        setSpacing(8);

        bannerLabel.getStyleClass().add("status-banner");
        cpmBanner.getStyleClass().addAll("status-banner", "status-banner-error");

        HBox segmentedScale = new HBox(dayButton, weekButton, yearButton);
        segmentedScale.getStyleClass().add("segmented-control");
        dayButton.getStyleClass().add("segmented-button");
        weekButton.getStyleClass().add("segmented-button");
        yearButton.getStyleClass().add("segmented-button");

        zoomSlider.getStyleClass().add("zoom-slider");
        addTaskButton.getStyleClass().addAll("pill-button", "primary-button");
        finishDateLabel.getStyleClass().add("finish-date-label");

        HBox left = new HBox(10, scaleLabel, segmentedScale);
        left.setAlignment(Pos.CENTER_LEFT);

        HBox middle = new HBox(10, zoomLabel, zoomSlider);
        middle.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(zoomSlider, Priority.ALWAYS);
        middle.setPrefWidth(320);

        HBox right = new HBox(
                10,
                themeLabel, themeComboBox,
                languageLabel, languageComboBox,
                addTaskButton
        );
        right.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbarRow = new HBox(16, left, middle, spacer, right, finishDateLabel);
        toolbarRow.getStyleClass().addAll("panel-card", "toolbar-row");
        toolbarRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(bannerLabel, cpmBanner, toolbarRow);
    }

    public Label bannerLabel() {
        return bannerLabel;
    }

    public Label cpmBanner() {
        return cpmBanner;
    }

    public Label scaleLabel() {
        return scaleLabel;
    }

    public ToggleButton dayButton() {
        return dayButton;
    }

    public ToggleButton weekButton() {
        return weekButton;
    }

    public ToggleButton yearButton() {
        return yearButton;
    }

    public Label zoomLabel() {
        return zoomLabel;
    }

    public Slider zoomSlider() {
        return zoomSlider;
    }

    public Label themeLabel() {
        return themeLabel;
    }

    public ComboBox<ThemeManager.ThemeMode> themeComboBox() {
        return themeComboBox;
    }

    public Label languageLabel() {
        return languageLabel;
    }

    public ComboBox<LanguageManager.Language> languageComboBox() {
        return languageComboBox;
    }

    public Button addTaskButton() {
        return addTaskButton;
    }

    public Label finishDateLabel() {
        return finishDateLabel;
    }
}
