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
    private boolean minimized;
    private boolean maximized;
    private double restoreX;
    private double restoreY;
    private double restoreWidth;
    private double restoreHeight;

    public OSWindow(String title, Node content) {
        setPrefSize(400, 300);
        getStyleClass().add("os-window");
        setStyle("-fx-border-color: #1e1e1e; -fx-border-width: 1; -fx-background-color: #f3f3f3;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold;");

        Button minimizeButton = new Button("_");
        minimizeButton.setOnAction(e -> minimize());

        Button maximizeButton = new Button("\u25a1"); // square
        maximizeButton.setOnAction(e -> toggleMaximize());

        Button closeButton = new Button("X");
        closeButton.setOnAction(e -> {
            if (closeHandler != null) {
                closeHandler.run();
            }
        });
        HBox titleBar = new HBox(10, titleLabel, createSpacer(), minimizeButton, maximizeButton, closeButton);
        titleBar.setPadding(new Insets(5));
        titleBar.setStyle("-fx-background-color: linear-gradient(#4c4c4c, #2b2b2b); -fx-text-fill: white;");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        String buttonStyle = "-fx-background-color: #3c3c3c; -fx-text-fill: white; -fx-padding: 2 8 2 8;";
        minimizeButton.setStyle(buttonStyle);
        maximizeButton.setStyle(buttonStyle);
        closeButton.setStyle("-fx-background-color: #aa0000; -fx-text-fill: white; -fx-padding: 2 8 2 8;");
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
        // When maximised, do not allow moving the window via the title bar.
        if (maximized) {
            return;
        }
        dragOffsetX = event.getSceneX() - getLayoutX();
        dragOffsetY = event.getSceneY() - getLayoutY();
        toFront();
    }

    private void dragWindow(MouseEvent event) {
        if (maximized) {
            return;
        }
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

    /**
     * Minimises this window by hiding it from the desktop while leaving
     * the underlying process running.
     */
    public void minimize() {
        minimized = true;
        setVisible(false);
    }

    /**
     * Restores this window if it is currently minimised.
     */
    public void restore() {
        if (minimized) {
            minimized = false;
            setVisible(true);
        }
    }

    /**
     * Toggles between normal and maximised states.
     */
    public void toggleMaximize() {
        if (!maximized) {
            maximize();
        } else {
            restoreFromMaximize();
        }
    }

    private void maximize() {
        Node parent = getParent();
        if (!(parent instanceof Region region)) {
            return;
        }
        restoreX = getLayoutX();
        restoreY = getLayoutY();
        restoreWidth = getWidth() > 0 ? getWidth() : getPrefWidth();
        restoreHeight = getHeight() > 0 ? getHeight() : getPrefHeight();

        setLayoutX(0);
        setLayoutY(0);
        setPrefWidth(region.getWidth());
        setPrefHeight(region.getHeight());
        setMaxWidth(region.getWidth());
        setMaxHeight(region.getHeight());
        maximized = true;
    }

    private void restoreFromMaximize() {
        if (!maximized) {
            return;
        }
        setLayoutX(restoreX);
        setLayoutY(restoreY);
        if (restoreWidth > 0 && restoreHeight > 0) {
            setPrefWidth(restoreWidth);
            setPrefHeight(restoreHeight);
        }
        maximized = false;
    }
}
