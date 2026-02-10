package com.deadlineflow.presentation.view.sections;

import com.deadlineflow.domain.model.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BoardSummaryView extends HBox {
    private final Label dueTodayTitleLabel = new Label();
    private final Label dueInSevenTitleLabel = new Label();
    private final Label overdueTitleLabel = new Label();
    private final Label blockedTitleLabel = new Label();

    private final ListView<Task> dueTodayListView = new ListView<>();
    private final ListView<Task> dueInSevenListView = new ListView<>();
    private final ListView<Task> overdueListView = new ListView<>();
    private final ListView<Task> blockedListView = new ListView<>();

    public BoardSummaryView() {
        getStyleClass().add("summary-row");
        setSpacing(12);

        VBox dueTodayCard = summaryCard(dueTodayTitleLabel, dueTodayListView);
        VBox dueSevenCard = summaryCard(dueInSevenTitleLabel, dueInSevenListView);
        VBox overdueCard = summaryCard(overdueTitleLabel, overdueListView);
        VBox blockedCard = summaryCard(blockedTitleLabel, blockedListView);

        overdueListView.getStyleClass().add("summary-overdue-list");

        getChildren().addAll(dueTodayCard, dueSevenCard, overdueCard, blockedCard);
        for (VBox card : new VBox[]{dueTodayCard, dueSevenCard, overdueCard, blockedCard}) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
    }

    private VBox summaryCard(Label titleLabel, ListView<Task> listView) {
        titleLabel.getStyleClass().add("summary-title");
        listView.getStyleClass().add("summary-list");
        listView.setPrefHeight(82);
        listView.setFixedCellSize(26);

        VBox card = new VBox(6, titleLabel, listView);
        card.getStyleClass().addAll("panel-card", "summary-card");
        card.setPadding(new Insets(10));
        VBox.setVgrow(listView, Priority.ALWAYS);
        return card;
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
}
