package os.gui;

import java.util.HashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import os.core.OSProcess;

/**
 * Very small taskbar showing running applications.
 */
public class TaskbarController {
    private final HBox container = new HBox(5);
    private final Map<Integer, Button> buttons = new HashMap<>();

    public TaskbarController() {
        container.setPadding(new Insets(5));
        container.setStyle("-fx-background-color: #252526;");
    }

    public Parent getView() {
        return container;
    }

    public void addProcess(OSProcess process, Runnable focusAction) {
        Button button = new Button(process.getName());
        button.setOnAction(e -> focusAction.run());
        button.setStyle("-fx-background-color: #3c3c3c; -fx-text-fill: white;");
        buttons.put(process.getPid(), button);
        container.getChildren().add(button);
    }

    public void removeProcess(int pid) {
        Button button = buttons.remove(pid);
        if (button != null) {
            container.getChildren().remove(button);
        }
    }
}
