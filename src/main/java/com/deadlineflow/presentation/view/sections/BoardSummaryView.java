package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.domain.model.Task;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class BoardSummaryView extends StackPane {
    private static final double COMPACT_HEIGHT = 38;
    private static final double LIST_VERTICAL_PADDING = 12;
    private static final Duration HOVER_ANIMATION_DURATION = Duration.millis(190);

    private final Label dueTodayTitleLabel = new Label();
    private final Label dueInSevenTitleLabel = new Label();
    private final Label overdueTitleLabel = new Label();
    private final Label blockedTitleLabel = new Label();

    private final ListView<Task> dueTodayListView = new ListView<>();
    private final ListView<Task> dueInSevenListView = new ListView<>();
    private final ListView<Task> overdueListView = new ListView<>();
    private final ListView<Task> blockedListView = new ListView<>();

    private final HBox compactRow = new HBox(12);
    private final Pane overlayLayer = new Pane();
    private final List<HoverSummaryPanel> panels = new ArrayList<>();

    public BoardSummaryView() {
        getStyleClass().add("summary-row-shell");
        setPickOnBounds(false);
        // Render above the timeline card so hover expansions are not obscured.
        setViewOrder(-1);
        setMinHeight(COMPACT_HEIGHT);
        setPrefHeight(COMPACT_HEIGHT);
        setMaxHeight(COMPACT_HEIGHT);

        compactRow.getStyleClass().add("summary-row");
        compactRow.setAlignment(Pos.CENTER_LEFT);
        compactRow.setFillHeight(true);
        compactRow.setPickOnBounds(false);

        overlayLayer.setManaged(false);
        overlayLayer.setPickOnBounds(false);

        HoverSummaryPanel dueTodayPanel = createHoverPanel(dueTodayTitleLabel, dueTodayListView);
        HoverSummaryPanel dueSevenPanel = createHoverPanel(dueInSevenTitleLabel, dueInSevenListView);
        HoverSummaryPanel overduePanel = createHoverPanel(overdueTitleLabel, overdueListView);
        HoverSummaryPanel blockedPanel = createHoverPanel(blockedTitleLabel, blockedListView);

        overdueListView.getStyleClass().add("summary-overdue-list");

        panels.add(dueTodayPanel);
        panels.add(dueSevenPanel);
        panels.add(overduePanel);
        panels.add(blockedPanel);

        for (HoverSummaryPanel panel : panels) {
            compactRow.getChildren().add(panel.compactCard);
            HBox.setHgrow(panel.compactCard, Priority.ALWAYS);
            panel.compactCard.setMaxWidth(Double.MAX_VALUE);
            overlayLayer.getChildren().add(panel.expandedCard);
        }

        getChildren().addAll(compactRow, overlayLayer);
    }

    private HoverSummaryPanel createHoverPanel(Label compactTitleLabel, ListView<Task> listView) {
        compactTitleLabel.getStyleClass().add("summary-title");

        listView.getStyleClass().add("summary-list");
        listView.setFixedCellSize(26);

        VBox compactCard = new VBox(compactTitleLabel);
        compactCard.getStyleClass().addAll("panel-card", "summary-card", "summary-card-compact");
        compactCard.setPadding(new Insets(8, 10, 8, 10));
        compactCard.setAlignment(Pos.CENTER_LEFT);
        compactCard.setMinHeight(COMPACT_HEIGHT);
        compactCard.setPrefHeight(COMPACT_HEIGHT);
        compactCard.setMaxHeight(COMPACT_HEIGHT);

        Label expandedTitleLabel = new Label();
        expandedTitleLabel.getStyleClass().add("summary-title");
        expandedTitleLabel.textProperty().bind(compactTitleLabel.textProperty());

        VBox expandedCard = new VBox(6, expandedTitleLabel, listView);
        expandedCard.getStyleClass().addAll("panel-card", "summary-card", "summary-card-expanded");
        expandedCard.setPadding(new Insets(10));
        expandedCard.setAlignment(Pos.TOP_LEFT);
        expandedCard.setManaged(false);
        expandedCard.setVisible(false);
        expandedCard.setMouseTransparent(true);
        expandedCard.setMinHeight(COMPACT_HEIGHT);
        expandedCard.setPrefHeight(COMPACT_HEIGHT);
        expandedCard.setOpacity(0.0);
        expandedCard.setScaleY(0.97);
        VBox.setVgrow(listView, Priority.ALWAYS);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        expandedCard.setClip(clip);

        HoverSummaryPanel panel = new HoverSummaryPanel(compactCard, expandedCard, listView, clip);
        wireHoverBehavior(panel);
        wireListTracking(panel);
        return panel;
    }

    private void wireHoverBehavior(HoverSummaryPanel panel) {
        panel.compactCard.setOnMouseEntered(event -> {
            panel.anchorHover = true;
            expandPanel(panel);
        });
        panel.compactCard.setOnMouseExited(event -> {
            panel.anchorHover = false;
            queueCollapse(panel);
        });

        panel.expandedCard.setOnMouseEntered(event -> {
            panel.expandedHover = true;
            expandPanel(panel);
        });
        panel.expandedCard.setOnMouseExited(event -> {
            panel.expandedHover = false;
            queueCollapse(panel);
        });

        panel.collapseDelay.setOnFinished(event -> {
            if (!panel.anchorHover && !panel.expandedHover) {
                collapsePanel(panel);
            }
        });
    }

    private void wireListTracking(HoverSummaryPanel panel) {
        panel.itemsListener = change -> {
            if (!panel.expandedCard.isVisible()) {
                return;
            }
            Platform.runLater(() -> {
                if (panel.expandedCard.isVisible()) {
                    positionExpandedCard(panel);
                    animateExpandedHeight(panel, computeExpandedHeight(panel), true);
                }
            });
        };

        ObservableList<Task> items = panel.listView.getItems();
        if (items != null) {
            items.addListener(panel.itemsListener);
        }
        panel.listView.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (oldItems != null && panel.itemsListener != null) {
                oldItems.removeListener(panel.itemsListener);
            }
            if (newItems != null && panel.itemsListener != null) {
                newItems.addListener(panel.itemsListener);
            }
            if (panel.expandedCard.isVisible()) {
                Platform.runLater(() -> {
                    if (panel.expandedCard.isVisible()) {
                        positionExpandedCard(panel);
                        animateExpandedHeight(panel, computeExpandedHeight(panel), true);
                    }
                });
            }
        });
    }

    private void queueCollapse(HoverSummaryPanel panel) {
        panel.collapseDelay.playFromStart();
    }

    private void expandPanel(HoverSummaryPanel panel) {
        panel.collapseDelay.stop();
        for (HoverSummaryPanel other : panels) {
            if (other != panel) {
                other.anchorHover = false;
                other.expandedHover = false;
                collapsePanel(other);
            }
        }
        positionExpandedCard(panel);
        animateExpandedHeight(panel, computeExpandedHeight(panel), true);
    }

    private void collapsePanel(HoverSummaryPanel panel) {
        if (!panel.expandedCard.isVisible() && !panel.expanded) {
            return;
        }
        animateExpandedHeight(panel, COMPACT_HEIGHT, false);
    }

    private void animateExpandedHeight(HoverSummaryPanel panel, double targetHeight, boolean expanding) {
        if (panel.animation != null) {
            panel.animation.stop();
        }

        if (expanding) {
            panel.expandedCard.setVisible(true);
            panel.expandedCard.setMouseTransparent(false);
            panel.expandedCard.toFront();
            panel.expanded = true;
        }

        double startHeight = panel.clip.getHeight() > 0 ? panel.clip.getHeight() : COMPACT_HEIGHT;
        panel.expandedCard.setPrefHeight(startHeight);
        panel.clip.setHeight(startHeight);

        panel.animation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(panel.expandedCard.prefHeightProperty(), startHeight),
                        new KeyValue(panel.clip.heightProperty(), startHeight),
                        new KeyValue(panel.expandedCard.opacityProperty(), panel.expandedCard.getOpacity()),
                        new KeyValue(panel.expandedCard.scaleYProperty(), panel.expandedCard.getScaleY())
                ),
                new KeyFrame(HOVER_ANIMATION_DURATION,
                        new KeyValue(panel.expandedCard.prefHeightProperty(), targetHeight, Interpolator.EASE_BOTH),
                        new KeyValue(panel.clip.heightProperty(), targetHeight, Interpolator.EASE_BOTH),
                        new KeyValue(panel.expandedCard.opacityProperty(), expanding ? 1.0 : 0.84, Interpolator.EASE_OUT),
                        new KeyValue(panel.expandedCard.scaleYProperty(), expanding ? 1.0 : 0.985, Interpolator.EASE_OUT)
                )
        );

        panel.animation.setOnFinished(event -> {
            if (!expanding && !panel.anchorHover && !panel.expandedHover) {
                panel.expandedCard.setVisible(false);
                panel.expandedCard.setMouseTransparent(true);
                panel.expandedCard.setOpacity(0.0);
                panel.expandedCard.setScaleY(0.97);
                panel.expanded = false;
            } else {
                panel.expandedCard.setPrefHeight(targetHeight);
                panel.clip.setHeight(targetHeight);
                panel.expandedCard.setOpacity(1.0);
                panel.expandedCard.setScaleY(1.0);
                panel.expanded = true;
            }
        });

        panel.animation.playFromStart();
    }

    private double computeExpandedHeight(HoverSummaryPanel panel) {
        int count = panel.listView.getItems() == null ? 0 : panel.listView.getItems().size();
        int visibleRows = Math.max(1, count);
        double listHeight = (visibleRows * panel.listView.getFixedCellSize()) + LIST_VERTICAL_PADDING;
        panel.listView.setPrefHeight(listHeight);

        double width = Math.max(panel.compactCard.getWidth(), 140);
        panel.expandedCard.applyCss();
        double prefHeight = panel.expandedCard.prefHeight(width);
        if (Double.isNaN(prefHeight)) {
            prefHeight = COMPACT_HEIGHT + listHeight + 16;
        }
        return Math.max(COMPACT_HEIGHT + 24, prefHeight);
    }

    private void positionExpandedCard(HoverSummaryPanel panel) {
        if (getScene() == null) {
            return;
        }
        Bounds compactBoundsInScene = panel.compactCard.localToScene(panel.compactCard.getBoundsInLocal());
        Point2D anchorInOverlay = overlayLayer.sceneToLocal(compactBoundsInScene.getMinX(), compactBoundsInScene.getMinY());
        double width = compactBoundsInScene.getWidth();

        panel.expandedCard.setLayoutX(anchorInOverlay.getX());
        panel.expandedCard.setLayoutY(anchorInOverlay.getY());
        panel.expandedCard.setMinWidth(width);
        panel.expandedCard.setPrefWidth(width);
        panel.expandedCard.setMaxWidth(width);
        panel.clip.setWidth(width);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        compactRow.resizeRelocate(0, 0, getWidth(), COMPACT_HEIGHT);
        overlayLayer.resizeRelocate(0, 0, getWidth(), COMPACT_HEIGHT);

        for (HoverSummaryPanel panel : panels) {
            if (panel.expandedCard.isVisible()) {
                positionExpandedCard(panel);
            }
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        return COMPACT_HEIGHT;
    }

    @Override
    protected double computeMinHeight(double width) {
        return COMPACT_HEIGHT;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return COMPACT_HEIGHT;
    }

    public Label dueTodayTitleLabel() {
        return dueTodayTitleLabel;
    }

    public Label dueInSevenTitleLabel() {
        return dueInSevenTitleLabel;
    }

    public Label overdueTitleLabel() {
        return overdueTitleLabel;
    }

    public Label blockedTitleLabel() {
        return blockedTitleLabel;
    }

    public ListView<Task> dueTodayListView() {
        return dueTodayListView;
    }

    public ListView<Task> dueInSevenListView() {
        return dueInSevenListView;
    }

    public ListView<Task> overdueListView() {
        return overdueListView;
    }

    public ListView<Task> blockedListView() {
        return blockedListView;
    }

    private static final class HoverSummaryPanel {
        private final VBox compactCard;
        private final VBox expandedCard;
        private final ListView<Task> listView;
        private final Rectangle clip;
        private final PauseTransition collapseDelay = new PauseTransition(Duration.millis(70));
        private Timeline animation;
        private ListChangeListener<Task> itemsListener;
        private boolean anchorHover;
        private boolean expandedHover;
        private boolean expanded;

        private HoverSummaryPanel(VBox compactCard, VBox expandedCard, ListView<Task> listView, Rectangle clip) {
            this.compactCard = compactCard;
            this.expandedCard = expandedCard;
            this.listView = listView;
            this.clip = clip;
        }
    }
}
