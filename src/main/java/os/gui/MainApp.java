package os.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import os.memory.MemoryManager;
import os.process.OSKernel;
import os.process.Scheduler;
import os.process.SchedulingAlgorithm;
import os.users.AuthManager;
import os.vfs.VirtualFileSystem;

/**
 * JavaFX entry point. Shows login first, then the desktop simulation.
 * <p>
 * This class wires the GUI to the back-end {@link OSKernel} by driving
 * the kernel's {@code tick()} method with a JavaFX {@link Timeline}.
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private OSKernel kernel;
    private Timeline cpuClock;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Construct the core OS components.
        MemoryManager memoryManager = new MemoryManager(1024, 64);
        Scheduler scheduler = new Scheduler(SchedulingAlgorithm.ROUND_ROBIN);
        AuthManager authManager = new AuthManager();
        VirtualFileSystem vfs = new VirtualFileSystem("virtual_fs");
        kernel = new OSKernel(memoryManager, scheduler, authManager, vfs);

        primaryStage.setTitle("Simple OS Simulator");
        showLoginScreen();
        primaryStage.show();
    }

    private void showLoginScreen() {
        VBox loginPane = new VBox(10);
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setPadding(new Insets(20));
        Label title = new Label("Welcome to Mini OS");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        Label status = new Label("Use admin/admin to login");
        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            boolean success = kernel.getAuthManager().login(usernameField.getText(), passwordField.getText());
            if (success) {
                // Ensure that the user's home directory exists before showing the desktop.
                kernel.ensureCurrentUserHomeDirectory();
                showDesktop();
            } else {
                status.setText("Invalid credentials");
            }
        });
        loginPane.getChildren().addAll(title, usernameField, passwordField, loginButton, status);
        Scene loginScene = new Scene(loginPane, 400, 300);
        primaryStage.setScene(loginScene);
    }

    private void showDesktop() {
        DesktopController desktopController = new DesktopController(kernel);
        Scene desktopScene = new Scene(desktopController.getView(), 1200, 800);
        primaryStage.setScene(desktopScene);

        // Start driving the kernel scheduler.
        if (cpuClock != null) {
            cpuClock.stop();
        }
        cpuClock = new Timeline(new KeyFrame(Duration.millis(500), event -> kernel.tick()));
        cpuClock.setCycleCount(Timeline.INDEFINITE);
        cpuClock.play();
    }

    @Override
    public void stop() {
        if (cpuClock != null) {
            cpuClock.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
