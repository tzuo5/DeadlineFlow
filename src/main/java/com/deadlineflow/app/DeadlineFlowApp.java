package com.deadlineflow.app;

import com.deadlineflow.presentation.view.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DeadlineFlowApp extends Application {
    @Override
    public void start(Stage stage) {
        AppContext appContext = new AppContext();
        MainView mainView = new MainView(
                appContext.mainViewModel(),
                appContext.languageManager(),
                appContext.themeManager()
        );

        Scene scene = new Scene(mainView, 1520, 900);
        appContext.themeManager().apply(scene);

        stage.setTitle("DeadlineFlow — Visual Deadline Planner");
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
