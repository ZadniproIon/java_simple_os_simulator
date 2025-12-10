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
        setPrefSize(420, 320);
        // Prevent parent layouts (like StackPane) from stretching the window
        // to fill all available space unless we explicitly maximise it.
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getStyleClass().add("os-window");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("os-window-title");

        Button minimizeButton = createControlButton("\u2013", this::minimize);
        Button maximizeButton = createControlButton("\u25A1", this::toggleMaximize);
        Button closeButton = createControlButton("\u2715", () -> {
            if (closeHandler != null) {
                closeHandler.run();
            }
        });
        closeButton.getStyleClass().add("close-control");

        HBox controls = new HBox(8, minimizeButton, maximizeButton, closeButton);
        controls.getStyleClass().add("os-window-controls");

        HBox titleBar = new HBox(10, titleLabel, createSpacer(), controls);
        titleBar.setPadding(new Insets(5, 16, 5, 16));
        titleBar.getStyleClass().add("os-window-titlebar");
        enableDragging(titleBar);
        setTop(titleBar);

        javafx.scene.layout.StackPane contentWrapper = new javafx.scene.layout.StackPane(content);
        contentWrapper.getStyleClass().add("app-surface");
        setCenter(contentWrapper);

        setOnMouseClicked(e -> toFront());
    }

    private Button createControlButton(String symbol, Runnable action) {
        Button button = new Button(symbol);
        button.setOnAction(e -> action.run());
        button.getStyleClass().add("window-control");
        return button;
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
        // Restore max size to match preferred size so layouts don't keep
        // stretching us to the full available area.
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        maximized = false;
    }
}
