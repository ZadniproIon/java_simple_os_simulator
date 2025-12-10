package os.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
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
    private LoginOverlay loginOverlay;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Construct the core OS components.
        MemoryManager memoryManager = new MemoryManager(4096, 64);
        Scheduler scheduler = new Scheduler(SchedulingAlgorithm.ROUND_ROBIN);
        VirtualFileSystem vfs = new VirtualFileSystem("virtual_fs");
        AuthManager authManager = new AuthManager(vfs);
        kernel = new OSKernel(memoryManager, scheduler, authManager, vfs);

        primaryStage.setTitle("Simple OS Simulator");
        loginOverlay = new LoginOverlay(kernel, () -> {
            // nothing extra for now; overlay hides itself
        });
        DesktopController desktopController = new DesktopController(kernel, () -> {
            loginOverlay.refreshUsers();
            loginOverlay.showOverlay();
        });
        loginOverlay.showOverlay();

        StackPane root = new StackPane(desktopController.getView(), loginOverlay);
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();

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
