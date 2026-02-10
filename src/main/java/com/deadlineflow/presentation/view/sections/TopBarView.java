package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.presentation.theme.ThemeManager;
import com.deadlineflow.presentation.viewmodel.LanguageManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TopBarView extends VBox {
    private final Label bannerLabel = new Label();
    private final Label cpmBanner = new Label();

    private final Label scaleLabel = new Label();
    private final ToggleButton hourButton = new ToggleButton();
    private final ToggleButton dayButton = new ToggleButton();
    private final ToggleButton weekButton = new ToggleButton();
    private final ToggleButton yearButton = new ToggleButton();

    private final Button projectsToggleButton = new Button();
    private final Button inspectorToggleButton = new Button();
    private final Label zoomLabel = new Label();
    private final TextField zoomField = new TextField("100");

    private final Label themeLabel = new Label();
    private final ComboBox<ThemeManager.ThemeMode> themeComboBox = new ComboBox<>();

    private final Label languageLabel = new Label();
    private final ComboBox<LanguageManager.Language> languageComboBox = new ComboBox<>();

    private final Button addTaskButton = new Button();
    private final Label finishDateLabel = new Label();

    public TopBarView() {
        getStyleClass().add("toolbar-shell");
        setSpacing(8);
        setFillWidth(true);
        setMinHeight(Region.USE_PREF_SIZE);
        setMaxWidth(Double.MAX_VALUE);

        bannerLabel.getStyleClass().add("status-banner");
        cpmBanner.getStyleClass().addAll("status-banner", "status-banner-error");
        scaleLabel.getStyleClass().add("toolbar-group-label");
        zoomLabel.getStyleClass().add("toolbar-group-label");
        themeLabel.getStyleClass().add("toolbar-group-label");
        languageLabel.getStyleClass().add("toolbar-group-label");

        HBox segmentedScale = new HBox(hourButton, dayButton, weekButton, yearButton);
        segmentedScale.getStyleClass().add("segmented-control");
        hourButton.getStyleClass().add("segmented-button");
        dayButton.getStyleClass().add("segmented-button");
        weekButton.getStyleClass().add("segmented-button");
        yearButton.getStyleClass().add("segmented-button");
        projectsToggleButton.getStyleClass().addAll("pill-button", "toolbar-projects-button");
        inspectorToggleButton.getStyleClass().addAll("pill-button", "toolbar-inspector-button");

        zoomField.getStyleClass().add("inspector-input-target");
        zoomField.setMinWidth(70);
        zoomField.setPrefWidth(84);
        zoomField.setMaxWidth(96);
        zoomField.setPromptText("100");
        zoomField.setAlignment(Pos.CENTER_RIGHT);
        projectsToggleButton.setMinWidth(86);
        projectsToggleButton.setPrefWidth(96);
        projectsToggleButton.setMaxWidth(132);
        inspectorToggleButton.setMinWidth(112);
        inspectorToggleButton.setPrefWidth(136);
        inspectorToggleButton.setMaxWidth(176);
        themeComboBox.setMinWidth(96);
        themeComboBox.setPrefWidth(128);
        themeComboBox.setMaxWidth(180);
        languageComboBox.setMinWidth(96);
        languageComboBox.setPrefWidth(128);
        languageComboBox.setMaxWidth(180);
        addTaskButton.getStyleClass().addAll("pill-button", "primary-button");
        finishDateLabel.getStyleClass().add("finish-date-label");
        finishDateLabel.setMinWidth(Region.USE_PREF_SIZE);

        HBox leftControls = new HBox(10, scaleLabel, segmentedScale);
        leftControls.setAlignment(Pos.CENTER_LEFT);

        HBox themeGroup = new HBox(8, themeLabel, themeComboBox);
        themeGroup.setAlignment(Pos.CENTER_LEFT);

        HBox languageGroup = new HBox(8, languageLabel, languageComboBox);
        languageGroup.setAlignment(Pos.CENTER_LEFT);

        HBox rightControls = new HBox(12, themeGroup, divider(), languageGroup, addTaskButton, divider(), finishDateLabel);
        rightControls.setAlignment(Pos.CENTER_RIGHT);

        Region controlsSpacer = new Region();
        HBox.setHgrow(controlsSpacer, Priority.ALWAYS);

        HBox controlsRow = new HBox(14, leftControls, controlsSpacer, rightControls);
        controlsRow.getStyleClass().addAll("panel-card", "toolbar-row", "toolbar-controls-row");
        controlsRow.setAlignment(Pos.CENTER_LEFT);
        controlsRow.setMinHeight(Region.USE_PREF_SIZE);
        controlsRow.setMaxWidth(Double.MAX_VALUE);

        Region zoomSpacer = new Region();
        HBox.setHgrow(zoomSpacer, Priority.ALWAYS);

        HBox zoomRow = new HBox(10, projectsToggleButton, zoomLabel, zoomField, zoomSpacer, inspectorToggleButton);
        zoomRow.getStyleClass().addAll("panel-card", "toolbar-row", "toolbar-zoom-row");
        zoomRow.setAlignment(Pos.CENTER_LEFT);
        zoomRow.setMinHeight(Region.USE_PREF_SIZE);
        zoomRow.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(bannerLabel, cpmBanner, controlsRow, zoomRow);
    }

    private Region divider() {
        Region divider = new Region();
        divider.getStyleClass().add("toolbar-divider");
        divider.setMinWidth(1);
        divider.setPrefWidth(1);
        divider.setMaxWidth(1);
        return divider;
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

    public ToggleButton hourButton() {
        return hourButton;
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

    public Button projectsToggleButton() {
        return projectsToggleButton;
    }

    public Button inspectorToggleButton() {
        return inspectorToggleButton;
    }

    public TextField zoomField() {
        return zoomField;
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
