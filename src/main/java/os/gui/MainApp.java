package os.gui;

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
import os.core.AuthManager;
import os.core.MemoryManager;
import os.core.OSKernel;
import os.core.Scheduler;
import os.fs.VirtualFileSystem;

/**
 * JavaFX entry point. Shows login first, then the desktop simulation.
 */
public class MainApp extends Application {
    private Stage primaryStage;
    private OSKernel kernel;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        Scheduler scheduler = new Scheduler();
        MemoryManager memoryManager = new MemoryManager(1024, 64);
        VirtualFileSystem vfs = new VirtualFileSystem("virtual_fs");
        AuthManager authManager = new AuthManager();
        kernel = new OSKernel(scheduler, memoryManager, vfs, authManager);

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
        kernel.startScheduler();
    }

    @Override
    public void stop() {
        if (kernel != null) {
            kernel.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
