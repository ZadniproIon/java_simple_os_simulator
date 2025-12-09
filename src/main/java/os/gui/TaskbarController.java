package os.gui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import os.process.OSProcess;

/**
 * Taskbar showing a start button, running app buttons and a clock,
 * similar to a simplified Windows taskbar.
 */
public class TaskbarController {
    private final HBox root = new HBox(10);
    private final HBox appButtonsBox = new HBox(5);
    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Label clockLabel = new Label();
    private final Timeline clockTimeline;

    public TaskbarController(Runnable onStartMenuRequested) {
        root.setPadding(new Insets(4, 10, 4, 10));
        root.setStyle("-fx-background-color: #252526;");
        root.setAlignment(Pos.CENTER_LEFT);

        Button startButton = new Button("\u2630"); // hamburger-style icon
        startButton.setOnAction(e -> {
            if (onStartMenuRequested != null) {
                onStartMenuRequested.run();
            }
        });
        startButton.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-font-weight: bold;");

        appButtonsBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(appButtonsBox, Priority.ALWAYS);

        clockLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11px;");

        root.getChildren().addAll(startButton, appButtonsBox, clockLabel);

        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock()));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
        updateClock();
    }

    private void updateClock() {
        LocalDateTime now = LocalDateTime.now();
        String text = now.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  HH:mm"));
        clockLabel.setText(text);
    }

    public Parent getView() {
        return root;
    }

    public void addProcess(OSProcess process, Runnable focusAction) {
        Button button = new Button(process.getName());
        button.setOnAction(e -> focusAction.run());
        button.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: white;");
        buttons.put(process.getPid(), button);
        appButtonsBox.getChildren().add(button);
    }

    public void removeProcess(int pid) {
        Button button = buttons.remove(pid);
        if (button != null) {
            appButtonsBox.getChildren().remove(button);
        }
    }
}
