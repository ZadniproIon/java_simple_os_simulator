package os.gui;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Simple draggable window used inside the desktop area.
 */
public class OSWindow extends BorderPane {
    private double dragOffsetX;
    private double dragOffsetY;
    private Runnable closeHandler;

    public OSWindow(String title, Node content) {
        setPrefSize(400, 300);
        getStyleClass().add("os-window");
        setStyle("-fx-border-color: #1e1e1e; -fx-border-width: 1; -fx-background-color: #f3f3f3;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");
        Button closeButton = new Button("X");
        closeButton.setOnAction(e -> {
            if (closeHandler != null) {
                closeHandler.run();
            }
        });
        HBox titleBar = new HBox(10, titleLabel, createSpacer(), closeButton);
        titleBar.setPadding(new Insets(5));
        titleBar.setStyle("-fx-background-color: linear-gradient(#4c4c4c, #2b2b2b); -fx-text-fill: white;");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        closeButton.setStyle("-fx-background-color: #aa0000; -fx-text-fill: white;");
        enableDragging(titleBar);
        setTop(titleBar);
        setCenter(content);

        setOnMouseClicked(e -> toFront());
    }

    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private void enableDragging(Node dragHandle) {
        dragHandle.setOnMousePressed(this::startDrag);
        dragHandle.setOnMouseDragged(this::dragWindow);
        dragHandle.setCursor(Cursor.MOVE);
    }

    private void startDrag(MouseEvent event) {
        dragOffsetX = event.getSceneX() - getLayoutX();
        dragOffsetY = event.getSceneY() - getLayoutY();
        toFront();
    }

    private void dragWindow(MouseEvent event) {
        double x = event.getSceneX() - dragOffsetX;
        double y = event.getSceneY() - dragOffsetY;
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        relocate(x, y);
    }

    public void setOnCloseRequest(Runnable handler) {
        this.closeHandler = handler;
    }
}
