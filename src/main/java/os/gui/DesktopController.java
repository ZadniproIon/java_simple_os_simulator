package os.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import os.apps.FileExplorerApp;
import os.apps.NotepadApp;
import os.apps.OSApplication;
import os.apps.TaskManagerApp;
import os.core.OSKernel;
import os.core.OSProcess;
import os.core.ProcessLifecycleListener;
import os.fs.VirtualFile;

/**
 * Builds the desktop scene, including icons, taskbar and app windows.
 */
public class DesktopController implements ProcessLifecycleListener {
    private final OSKernel kernel;
    private final BorderPane root = new BorderPane();
    private final Pane desktopArea = new Pane();
    private final TaskbarController taskbar = new TaskbarController();
    private final Map<Integer, OSWindow> openWindows = new HashMap<>();
    private final FlowPane iconPane = new FlowPane(10, 10);

    public DesktopController(OSKernel kernel) {
        this.kernel = kernel;
        this.kernel.addProcessLifecycleListener(this);
        setupLayout();
        setupIcons();
    }

    private void setupLayout() {
        desktopArea.setStyle("-fx-background-color: linear-gradient(#1b2838, #0f2027);");
        desktopArea.setPrefSize(1000, 700);
        iconPane.setPadding(new Insets(15));
        desktopArea.getChildren().add(iconPane);
        root.setCenter(desktopArea);
        root.setBottom(taskbar.getView());
    }

    private void setupIcons() {
        iconPane.getChildren().add(createIcon("Notepad", () ->
                launchApplication("Notepad", () -> new NotepadApp(kernel.getFileSystem()), 64)));
        iconPane.getChildren().add(createIcon("File Explorer", () ->
                launchApplication("File Explorer",
                        () -> new FileExplorerApp(kernel.getFileSystem(), this::openFileInNotepad), 96)));
        iconPane.getChildren().add(createIcon("Task Manager", () ->
                launchApplication("Task Manager", () -> new TaskManagerApp(kernel), 64)));
    }

    private Node createIcon(String title, Runnable action) {
        Button button = new Button(title);
        button.setPrefSize(120, 80);
        button.setWrapText(true);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; -fx-font-weight: bold;");
        button.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                action.run();
            }
        });
        return button;
    }

    private void launchApplication(String processName, Supplier<OSApplication> factory, int memoryRequired) {
        OSApplication app = factory.get();
        Optional<OSProcess> process = kernel.createProcess(processName, app, memoryRequired);
        if (process.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Not enough memory to start " + processName,
                    ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
        OSProcess osProcess = process.get();
        OSWindow window = new OSWindow(app.getName() + " (PID " + osProcess.getPid() + ")",
                app.createContent());
        window.setLayoutX(100 + openWindows.size() * 20);
        window.setLayoutY(80 + openWindows.size() * 20);
        window.setOnCloseRequest(() -> kernel.killProcess(osProcess.getPid()));
        desktopArea.getChildren().add(window);
        openWindows.put(osProcess.getPid(), window);
        taskbar.addProcess(osProcess, () -> focusWindow(osProcess.getPid()));
    }

    private void focusWindow(int pid) {
        OSWindow window = openWindows.get(pid);
        if (window != null) {
            window.toFront();
        }
    }

    private void openFileInNotepad(VirtualFile file) {
        launchApplication("Notepad", () -> new NotepadApp(kernel.getFileSystem(), file), 64);
    }

    public Parent getView() {
        return root;
    }

    @Override
    public void processTerminated(OSProcess process) {
        Platform.runLater(() -> closeWindow(process.getPid()));
    }

    private void closeWindow(int pid) {
        OSWindow window = openWindows.remove(pid);
        if (window != null) {
            desktopArea.getChildren().remove(window);
            taskbar.removeProcess(pid);
        }
    }
}
